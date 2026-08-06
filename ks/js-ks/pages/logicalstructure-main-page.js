import { initMenu } from "../components/menu.js";
import { initHelpDialog } from "../components/help-dialog.js";
import { mountTopbar } from "../components/topbar.js";
import { applyTopbarMetadata } from "../components/topbar.js";
import { openEditDialog } from "../components/edit-dialog.js";
import { createExportDialog } from "../components/export-dialog.js";
import { createImportDialog } from "../components/import-dialog.js";
import { downloadLogicalStructureDiagramPdf } from "./logicalstructure-diagram-pdf.js";
import { setText } from "../core/dom.js";
import {
    getDirectChild,
    getDirectText,
    hasXmlParseError
} from "../core/xml.js";
import { buildColorChipStyle, sanitizeCssColor } from "../core/css.js";
import {
    calculateLevelFromRequirementCode,
    getParentRequirementCode,
    normalizeRequirementCode
} from "../core/requirement-code.js";
import { escapeHtml } from "../core/html.js";
import { isFalsy, isTruthy } from "../core/utils.js";

const LIST_URL = "/pro/logicalstructure?cmd=list";
const EDIT_PAGE_URL = "/web/view?page=logicalstructure-edit";

const STORAGE_KEYS = {
    selectedView: "pro.logicalstructure.main.selectedView",
    filterText: "pro.logicalstructure.main.filterText",
    activeOnly: "pro.logicalstructure.main.activeOnly",
    sortKey: "pro.logicalstructure.main.sortKey",
    sortDirection: "pro.logicalstructure.main.sortDirection",
    columnWidths: "pro.logicalstructure.main.columnWidths",
    groupBy: "pro.logicalstructure.main.groupBy",
    groupCollapsed: "pro.logicalstructure.main.groupCollapsed"
};

const VIEW_TYPES = {
    list: "list",
    horizontal: "horizontal",
    vertical: "vertical"
};

const VIEW_PAGE_URLS = {
    list: "/web/view?page=logicalstructure-main-list",
    horizontal: "/web/view?page=logicalstructure-main-horizontal",
    vertical: "/web/view?page=logicalstructure-main-vertical"
};

const FIXED_VIEW = document.body?.dataset?.fixedView || "";

const VIEW_LABELS = {
    list: "Liste-visning",
    horizontal: "Horisontal diagram",
    vertical: "Vertikal diagram"
};

const MAX_REQUIREMENT_LEVEL = 4;
const DIAGRAM_NODE_WIDTH = 260;
const PROJECT_NODE_HEIGHT = 124;
const REQUIREMENT_NODE_HEIGHT = 124;

const state = {
    xmlDocument: null,
    topPanel: {
        customerName: "—",
        projectName: "—",
        userName: "—"
    },
    requirements: [],
    filteredRequirements: [],
    listColumns: [],
    selectedView: VIEW_TYPES.list,
    sortKey: "",
    sortDirection: "asc",
    groupBy: loadGroupBy(),
    collapsedGroupPaths: loadCollapsedGroupPaths(),
    contextTargetType: "",
    contextRequirement: null,
    fixedView: Object.values(VIEW_TYPES).includes(FIXED_VIEW) ? FIXED_VIEW : ""
};

document.addEventListener("DOMContentLoaded", () => {
    start();
});

function start() {
    initializeShell();
    initializeStateFromStorage();
    initializeEvents();
    initializeImportExportDialogs();
    renderGroupByZone();
    applyView(state.selectedView, { persist: false });
    loadLogicalStructures();
}

function initializeShell() {
    setText("customerName", "—", "");
    setText("projectName", "—", "");
    setText("userName", "—", "");
    setText("loadStatus", "Loading", "");

    initMenu(document);
    initHelpDialog();
    mountTopbar(document);
}

function initializeStateFromStorage() {
    const storedView = localStorage.getItem(STORAGE_KEYS.selectedView);

    if (state.fixedView) {
        state.selectedView = state.fixedView;
    } else if (Object.values(VIEW_TYPES).includes(storedView)) {
        state.selectedView = storedView;
    }

    state.sortKey = localStorage.getItem(STORAGE_KEYS.sortKey) || "";
    state.sortDirection = localStorage.getItem(STORAGE_KEYS.sortDirection) || "asc";

    const filterText = sessionStorage.getItem(STORAGE_KEYS.filterText) || "";
    const activeOnly = sessionStorage.getItem(STORAGE_KEYS.activeOnly);

    const filterInput = document.getElementById("filterRequirementText");
    const activeOnlyInput = document.getElementById("filterActiveOnly");

    if (filterInput) {
        filterInput.value = filterText;
    }

    if (activeOnlyInput) {
        activeOnlyInput.checked = activeOnly === null ? true : activeOnly === "true";
    }
}

function initializeEvents() {
    document.querySelectorAll("[data-view-type]").forEach((button) => {
        button.addEventListener("click", () => {
            const viewType = button.getAttribute("data-view-type");
            const targetUrl = VIEW_PAGE_URLS[viewType];

            if (Object.values(VIEW_TYPES).includes(viewType)) {
                localStorage.setItem(STORAGE_KEYS.selectedView, viewType);
            }

            if (targetUrl) {
                window.location.href = targetUrl;
                return;
            }

            applyView(viewType, { persist: true });
            applyFiltersAndRender();
        });
    });

    const filterInput = document.getElementById("filterRequirementText");
    const activeOnlyInput = document.getElementById("filterActiveOnly");
    const clearFilterButton = document.getElementById("btnClearFilter");
    const groupByZone = document.getElementById("groupByZone");
    const addRootButton = document.getElementById("btnAddRoot");
    const pdfButton = document.getElementById("btnDownloadDiagramPdf");

    filterInput?.addEventListener("input", debounce(() => {
        persistFilters();
        applyFiltersAndRender();
    }, 120));

    filterInput?.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            filterInput.value = "";
            persistFilters();
            applyFiltersAndRender();
            filterInput.blur();
        }
    });

    activeOnlyInput?.addEventListener("change", () => {
        persistFilters();
        applyFiltersAndRender();
    });

    clearFilterButton?.addEventListener("click", () => {
        if (filterInput) {
            filterInput.value = "";
        }

        if (activeOnlyInput) {
            activeOnlyInput.checked = true;
        }

        persistFilters();
        applyFiltersAndRender();
    });

    groupByZone?.addEventListener("dragenter", () => {
        groupByZone.classList.add("is-drag-over");
    });

    groupByZone?.addEventListener("dragover", (event) => {
        event.preventDefault();
        event.dataTransfer.dropEffect = "copy";
    });

    groupByZone?.addEventListener("dragleave", (event) => {
        if (event.target === groupByZone) {
            groupByZone.classList.remove("is-drag-over");
        }
    });

    groupByZone?.addEventListener("drop", (event) => {
        event.preventDefault();
        groupByZone.classList.remove("is-drag-over");
        addGroupByKey(event.dataTransfer.getData("text/plain"));
    });

    document.addEventListener("dragend", () => {
        groupByZone?.classList.remove("is-drag-over");
    });

    addRootButton?.addEventListener("click", () => {
        openCreateRootRequirement();
    });

    pdfButton?.addEventListener("click", () => {
        downloadCurrentDiagramPdf();
    });

    initializeContextMenuEvents();

    window.addEventListener("resize", debounce(() => {
        renderCurrentView();
    }, 120));
}

function initializeImportExportDialogs() {
    const exportDialog = createExportDialog({
        dialogId: "exportDialog",
        openButtonId: "btnExport",
        entityName: "Logical Structure",
        exportUrl: "/pro/logicalstructure?cmd=export",
        baseFileName: () => buildExportBaseFileName("Logical Structure", state.topPanel?.projectName)
    });

    const importDialog = createImportDialog({
        dialogId: "importDialog",
        openButtonId: "btnImport",
        importUrl: "/pro/logicalstructure?cmd=import",
        onImportComplete: async () => {
            await loadLogicalStructures();
        }
    });

    exportDialog.bind();
    importDialog.bind();
}

function buildExportBaseFileName(entityName, projectName) {
    const entity = String(entityName || "").trim();
    const project = String(projectName || "").trim();
    return project ? `${entity} - ${project}` : entity;
}

async function loadLogicalStructures() {
    showListEmptyState("Loading logical structures…");
    showDiagramEmptyState("horizontal", "Loading horizontal logical structure diagram…");
    showDiagramEmptyState("vertical", "Loading vertical logical structure diagram…");
    setText("loadStatus", "Loading", "");

    try {
        const response = await fetch(LIST_URL, {
            method: "GET",
            headers: {
                "Accept": "application/xml,text/xml,*/*"
            },
            cache: "no-store"
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status} ${response.statusText}`);
        }

        const xmlText = await response.text();
        const xmlDocument = new DOMParser().parseFromString(xmlText, "application/xml");

        if (hasXmlParseError(xmlDocument)) {
            throw new Error("The logical structure endpoint returned invalid XML.");
        }

        state.xmlDocument = xmlDocument;
        state.currentDoc = xmlDocument;
        state.topPanel = parseTopPanel(xmlDocument);
        state.requirements = parseLogicalStructures(xmlDocument)
            .filter((requirement) => requirement.level <= MAX_REQUIREMENT_LEVEL);
        state.listColumns = buildListColumns(state.requirements);

        if (state.sortKey && !state.listColumns.some((column) => column.key === state.sortKey)) {
            state.sortKey = "";
            state.sortDirection = "asc";
            persistSorting();
        }

        sanitizeGroupByKeys();
        renderGroupByZone();

        applyTopPanel();
        applyFiltersAndRender();

        setText("loadStatus", "Loaded", "");
    } catch (error) {
        console.error("Failed to load logical structures", error);
        setText("loadStatus", "Error", "");
        showListEmptyState(`Could not load logical structures. ${error.message}`);
        showDiagramEmptyState("horizontal", `Could not load horizontal diagram. ${error.message}`);
        showDiagramEmptyState("vertical", `Could not load vertical diagram. ${error.message}`);
    }
}

function parseTopPanel(xmlDocument) {
    const topPanelElement = xmlDocument.querySelector("TopPanel");

    if (!topPanelElement) {
        return {
            customerName: "—",
            projectName: "—",
            userName: "—"
        };
    }

    return {
        customerName: getChildText(topPanelElement, "CustomerName", "—"),
        projectName: getChildText(topPanelElement, "ProjectName", "—"),
        userName: getChildText(topPanelElement, "Name", "—")
    };
}

function applyTopPanel() {
    applyTopbarMetadata(document, state.currentDoc || state.topPanel);
}

function parseLogicalStructures(xmlDocument) {
    const requirementNodes = Array.from(
        xmlDocument.querySelectorAll("logicalStructures > logicalDocument, logicalStructures > logicalStructure, logicalDocument, logicalStructure")
    );

    return requirementNodes.map((node, index) => {
        const fields = parseVisibleFields(node);

        const entityId = getFirstFieldRawValue(node, [
            "EntityId",
            "LogicalStructureId",
            "LogicalStructurePK"
        ], "");

        const id = normalizeRequirementCode(getFirstFieldDisplayText(node, [
            "LogicalCode"
        ], `LSR-${index + 1}`));

        const name = getFirstFieldDisplayText(node, [
            "LogicalName"
        ], "—");

        const description = getFirstFieldDisplayText(node, [
            "LogicalDescription",
            "ReqDescription",
            "Description"
        ], "");

        const active = parseActiveFlag(node, fields);

        return {
            node,
            index,
            entityId,
            id,
            name,
            description,
            active,
            fields,
            level: calculateLevelFromRequirementCode(id) || 1,
            parentCode: calculateParentCode(id)
        };
    });
}

function parseVisibleFields(node) {
    return Array.from(node.children || [])
        .filter((field) => {
            const control = (field.getAttribute("control") || "").toLowerCase();
            const visible = parseBooleanAttribute(field.getAttribute("visible"), true);

            return control !== "hidden" && visible;
        })
        .map((field, index) => {
            const name = field.tagName;
            const label = field.getAttribute("header") || field.getAttribute("label") || name;
            const control = (field.getAttribute("control") || "").toLowerCase();
            const color = sanitizeCssColor(field.getAttribute("color"), "");
            const value = getFieldDisplayValue(field);
            const rawValue = getFieldRawValue(field);

            const displayOrder = parseNumberAttribute(
                field.getAttribute("displayOrder"),
                parseNumberAttribute(field.getAttribute("visibleOrder"), index)
            );

            const tableWidth = normalizeTableWidth(field.getAttribute("tableWidth"));

            return {
                name,
                label,
                control,
                color,
                value,
                rawValue,
                displayOrder,
                tableWidth,
                originalIndex: index
            };
        });
}

function buildListColumns(requirements) {
    const byName = new Map();

    requirements.forEach((requirement) => {
        requirement.fields.forEach((field) => {
            if (!byName.has(field.name)) {
                byName.set(field.name, {
                    key: field.name,
                    label: field.label,
                    control: field.control,
                    displayOrder: field.displayOrder,
                    tableWidth: field.tableWidth,
                    originalIndex: field.originalIndex,
                    isActiveColumn: isActiveFieldName(field.name)
                });
            }
        });
    });

    const columns = Array.from(byName.values())
        .sort((a, b) => {
            if (a.displayOrder !== b.displayOrder) {
                return a.displayOrder - b.displayOrder;
            }

            return a.originalIndex - b.originalIndex;
        });

    const activeIndex = columns.findIndex((column) => column.isActiveColumn);

    if (activeIndex >= 0 && activeIndex !== columns.length - 1) {
        const [activeColumn] = columns.splice(activeIndex, 1);
        columns.push(activeColumn);
    }

    const storedWidths = getStoredColumnWidths();

    return columns.map((column) => ({
        ...column,
        width: normalizeStoredColumnWidth(
            storedWidths[column.key],
            column.tableWidth || calculateFallbackColumnWidth(column)
        )
    }));
}

function normalizeTableWidth(value) {
    const raw = String(value || "").trim();

    if (!raw || raw.toLowerCase() === "auto") {
        return "";
    }

    if (/^\d+$/.test(raw)) {
        return `${raw}px`;
    }

    return raw;
}

function calculateFallbackColumnWidth(column) {
    const key = String(column.key || "").toLowerCase();

    if (column.isActiveColumn) return "70px";
    if (key.includes("description")) return "420px";
    if (key.includes("name")) return "260px";
    if (key.includes("code")) return "150px";
    if (key === "entityid" || key.endsWith("id")) return "120px";
    if (key.includes("priority")) return "170px";
    if (key.includes("date") || key.includes("time")) return "180px";

    return "180px";
}

function parseNumberAttribute(value, fallback) {
    const parsed = Number(value);

    return Number.isFinite(parsed) ? parsed : fallback;
}

function parseBooleanAttribute(value, fallback) {
    if (value === null || value === undefined || value === "") {
        return fallback;
    }

    if (isTruthy(value)) {
        return true;
    }

    if (isFalsy(value)) {
        return false;
    }

    return fallback;
}

function parseActiveFlag(node, fields = []) {
    const activeField = fields.find((field) => isActiveFieldName(field.name));

    if (activeField) {
        return parseBooleanValue(activeField.rawValue || activeField.value, true);
    }

    const rawValue = getFirstFieldRawValue(node, [
        "Active",
        "IsActive",
        "RequirementActive",
        "LogicalStructureActive"
    ], "");

    const displayValue = getFirstFieldDisplayText(node, [
        "Active",
        "IsActive",
        "RequirementActive",
        "LogicalStructureActive"
    ], "");

    return parseBooleanValue(rawValue || displayValue, true);
}

function isActiveFieldName(name) {
    const normalized = String(name || "").toLowerCase();

    return [
        "active",
        "isactive",
        "requirementactive",
        "logicalstructureactive"
    ].includes(normalized);
}

function parseBooleanValue(value, fallback) {
    const normalized = String(value ?? "").trim().toLowerCase();

    if (!normalized) {
        return fallback;
    }

    if (["true", "1", "yes", "ja", "y", "active", "aktiv"].includes(normalized)) {
        return true;
    }

    if (["false", "0", "no", "nej", "n", "inactive", "inaktiv"].includes(normalized)) {
        return false;
    }

    return fallback;
}

function getFieldDisplayValue(field) {
    const selectedOption = Array.from(field.querySelectorAll(":scope > Option"))
        .find((option) => parseBooleanAttribute(option.getAttribute("selected"), false));

    if (selectedOption) {
        return selectedOption.textContent?.trim() || "";
    }

    const valueText = field.querySelector(":scope > Value")?.textContent?.trim();

    if (valueText) {
        const matchingOption = Array.from(field.querySelectorAll(":scope > Option"))
            .find((option) => (option.getAttribute("value") || "").trim() === valueText);

        if (matchingOption) {
            return matchingOption.textContent?.trim() || valueText;
        }

        return valueText;
    }

    return getDirectText(field).trim();
}

function getFieldRawValue(field) {
    const valueText = field.querySelector(":scope > Value")?.textContent?.trim();

    if (valueText) {
        return valueText;
    }

    return getDirectText(field).trim();
}

function getFirstFieldDisplayText(node, fieldNames, fallback = "") {
    for (const fieldName of fieldNames) {
        const field = getDirectChild(node, fieldName);

        if (!field) {
            continue;
        }

        const value = getFieldDisplayValue(field);

        if (value) {
            return value;
        }
    }

    return fallback;
}

function getFirstFieldRawValue(node, fieldNames, fallback = "") {
    for (const fieldName of fieldNames) {
        const field = getDirectChild(node, fieldName);

        if (!field) {
            continue;
        }

        const value = getFieldRawValue(field);

        if (value) {
            return value;
        }
    }

    return fallback;
}

function getChildText(parent, tagName, fallback = "") {
    const element = parent?.getElementsByTagName(tagName)?.[0];
    const value = element?.textContent?.trim();

    return value || fallback;
}

function calculateParentCode(code) {
    return getParentRequirementCode(code);
}

function applyFiltersAndRender() {
    const filterText = (document.getElementById("filterRequirementText")?.value || "").trim().toLowerCase();
    const activeOnly = document.getElementById("filterActiveOnly")?.checked !== false;

    state.filteredRequirements = state.requirements.filter((requirement) => {
        if (activeOnly && !requirement.active) {
            return false;
        }

        if (!filterText) {
            return true;
        }

        return [
            requirement.id,
            requirement.name,
            requirement.description
        ].some((value) => String(value || "").toLowerCase().includes(filterText));
    });

    sanitizeCollapsedGroupPaths();
    setText("logicalStructureCount", String(state.filteredRequirements.length), "");
    updateActionButtonsForView(state.selectedView);
    renderCurrentView();
}

function renderCurrentView() {
    if (state.selectedView === VIEW_TYPES.list) {
        renderListView();
        return;
    }

    if (state.selectedView === VIEW_TYPES.horizontal) {
        renderDiagramView("horizontal");
        return;
    }

    if (state.selectedView === VIEW_TYPES.vertical) {
        renderDiagramView("vertical");
    }
}

function applyView(viewType, options = {}) {
    const safeViewType = Object.values(VIEW_TYPES).includes(viewType) ? viewType : VIEW_TYPES.list;

    state.selectedView = safeViewType;

    if (options.persist) {
        localStorage.setItem(STORAGE_KEYS.selectedView, safeViewType);
    }

    document.querySelectorAll("[data-view-type]").forEach((button) => {
        const active = button.getAttribute("data-view-type") === safeViewType;
        button.classList.toggle("is-active", active);
        button.setAttribute("aria-pressed", active ? "true" : "false");
    });

    document.querySelectorAll("[data-view-panel]").forEach((panel) => {
        const active = panel.getAttribute("data-view-panel") === safeViewType;
        panel.classList.toggle("is-active", active);
        panel.hidden = !active;
    });

    updateActionButtonsForView(safeViewType);
    setText("currentViewLabel", VIEW_LABELS[safeViewType] || VIEW_LABELS.list, "");
}

function updateActionButtonsForView(viewType) {
    const isListView = viewType === VIEW_TYPES.list;
    const hasRequirements = state.requirements.length > 0;

    setElementHidden("btnImport", !isListView || hasRequirements);
    setElementHidden("btnExport", !isListView || !hasRequirements);
    setElementHidden("btnAddRoot", !isListView);
    setElementHidden("btnDownloadDiagramPdf", isListView);
    setElementHidden("groupByBar", !isListView);
    setElementHidden("btnHelp", false);
}

function setElementHidden(id, hidden) {
    const element = document.getElementById(id);

    if (!element) {
        return;
    }

    element.hidden = hidden;
    element.toggleAttribute("hidden", hidden);
    element.style.display = hidden ? "none" : "";
    element.setAttribute("aria-hidden", hidden ? "true" : "false");
}

function persistFilters() {
    const filterText = document.getElementById("filterRequirementText")?.value || "";
    const activeOnly = document.getElementById("filterActiveOnly")?.checked !== false;

    sessionStorage.setItem(STORAGE_KEYS.filterText, filterText);
    sessionStorage.setItem(STORAGE_KEYS.activeOnly, String(activeOnly));
}

function persistSorting() {
    localStorage.setItem(STORAGE_KEYS.sortKey, state.sortKey || "");
    localStorage.setItem(STORAGE_KEYS.sortDirection, state.sortDirection || "asc");
}

function loadGroupBy() {
    try {
        const raw = localStorage.getItem(STORAGE_KEYS.groupBy);
        const parsed = raw ? JSON.parse(raw) : [];

        return Array.isArray(parsed) ? parsed.filter((value) => typeof value === "string" && value) : [];
    } catch {
        return [];
    }
}

function persistGroupBy() {
    localStorage.setItem(STORAGE_KEYS.groupBy, JSON.stringify(state.groupBy));
}

function loadCollapsedGroupPaths() {
    try {
        const raw = localStorage.getItem(STORAGE_KEYS.groupCollapsed);
        const parsed = raw ? JSON.parse(raw) : [];

        return Array.isArray(parsed) ? parsed.filter((value) => typeof value === "string" && value) : [];
    } catch {
        return [];
    }
}

function persistCollapsedGroupPaths() {
    localStorage.setItem(STORAGE_KEYS.groupCollapsed, JSON.stringify(state.collapsedGroupPaths));
}

function sanitizeGroupByKeys() {
    const validKeys = new Set(state.listColumns.map((column) => column.key));
    const nextGroupBy = state.groupBy.filter((key) => validKeys.has(key));

    if (nextGroupBy.length !== state.groupBy.length) {
        state.groupBy = nextGroupBy;
        persistGroupBy();
    }
}

function sanitizeCollapsedGroupPaths() {
    const validPaths = new Set();
    collectGroupPaths(state.filteredRequirements, 0, [], validPaths);

    const nextCollapsed = state.collapsedGroupPaths.filter((path) => validPaths.has(path));

    if (nextCollapsed.length !== state.collapsedGroupPaths.length) {
        state.collapsedGroupPaths = nextCollapsed;
        persistCollapsedGroupPaths();
    }
}

function collectGroupPaths(rows, depth, pathParts, validPaths) {
    if (depth >= state.groupBy.length || !rows.length) {
        return;
    }

    const groupKey = state.groupBy[depth];
    const groups = new Map();

    rows.forEach((row) => {
        const groupValue = groupValueText(row, groupKey) || "—";

        if (!groups.has(groupValue)) {
            groups.set(groupValue, []);
        }

        groups.get(groupValue).push(row);
    });

    groups.forEach((groupRows, groupValue) => {
        const nextPathParts = [...pathParts, `${groupKey}:${groupValue}`];
        const groupPath = JSON.stringify(nextPathParts);

        validPaths.add(groupPath);
        collectGroupPaths(groupRows, depth + 1, nextPathParts, validPaths);
    });
}

function renderGroupByZone() {
    const groupZone = document.getElementById("groupByZone");

    if (!groupZone) {
        return;
    }

    const clearButtonMarkup = `
        <button
                id="btnClearGrouping"
                class="logicalstructure-grouping-clear"
                type="button"
                aria-label="Clear grouping"
                title="Clear grouping"
                ${state.groupBy.length ? "" : "hidden"}
        >
            <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                <path d="M18.3 5.71 12 12l6.3 6.29-1.41 1.42L10.59 13.4 4.29 19.71 2.88 18.3 9.17 12 2.88 5.71 4.29 4.29l6.3 6.3 6.29-6.3z"></path>
            </svg>
        </button>
    `;

    if (!state.groupBy.length) {
        groupZone.innerHTML = `<span class="empty">Drop a column here.</span>${clearButtonMarkup}`;
    } else {
        groupZone.innerHTML = `${state.groupBy.map((key) => `
            <span class="logicalstructure-group-chip" draggable="true" data-group-key="${escapeHtml(key)}">
                <span>${escapeHtml(labelForGroupKey(key))}</span>
                <button type="button" aria-label="Remove group">x</button>
            </span>
        `).join("")}${clearButtonMarkup}`;
    }

    const clearButton = document.getElementById("btnClearGrouping");

    clearButton?.addEventListener("click", () => {
        clearGrouping();
    });

    groupZone.querySelectorAll(".logicalstructure-group-chip").forEach((chip) => {
        chip.addEventListener("dragstart", (event) => {
            chip.classList.add("is-dragging");
            event.dataTransfer.setData("text/plain", chip.getAttribute("data-group-key") || "");
            event.dataTransfer.effectAllowed = "move";
        });

        chip.addEventListener("dragend", () => {
            chip.classList.remove("is-dragging");
        });

        chip.addEventListener("dragover", (event) => {
            event.preventDefault();
        });

        chip.addEventListener("drop", (event) => {
            event.preventDefault();
            reorderGroupBy(event.dataTransfer.getData("text/plain"), chip.getAttribute("data-group-key") || "");
        });

        chip.querySelector("button")?.addEventListener("click", () => {
            removeGroupByKey(chip.getAttribute("data-group-key") || "");
        });
    });
}

function addGroupByKey(key) {
    if (!key || state.groupBy.includes(key) || !state.listColumns.some((column) => column.key === key)) {
        return;
    }

    state.groupBy = [...state.groupBy, key];
    state.collapsedGroupPaths = [];
    persistGroupBy();
    persistCollapsedGroupPaths();
    renderGroupByZone();
    applyFiltersAndRender();
}

function removeGroupByKey(key) {
    state.groupBy = state.groupBy.filter((item) => item !== key);
    state.collapsedGroupPaths = [];
    persistGroupBy();
    persistCollapsedGroupPaths();
    renderGroupByZone();
    applyFiltersAndRender();
}

function reorderGroupBy(sourceKey, targetKey) {
    if (!sourceKey || !targetKey || sourceKey === targetKey) {
        return;
    }

    const next = state.groupBy.filter((item) => item !== sourceKey);
    const targetIndex = next.indexOf(targetKey);

    if (targetIndex < 0) {
        next.push(sourceKey);
    } else {
        next.splice(targetIndex, 0, sourceKey);
    }

    state.groupBy = next;
    state.collapsedGroupPaths = [];
    persistGroupBy();
    persistCollapsedGroupPaths();
    renderGroupByZone();
    applyFiltersAndRender();
}

function clearGrouping() {
    if (!state.groupBy.length) {
        renderGroupByZone();
        return;
    }

    state.groupBy = [];
    state.collapsedGroupPaths = [];
    persistGroupBy();
    persistCollapsedGroupPaths();
    renderGroupByZone();
    applyFiltersAndRender();
}

function isGroupCollapsed(groupPath) {
    return state.collapsedGroupPaths.includes(groupPath);
}

function toggleGroupCollapse(groupPath) {
    if (!groupPath) {
        return;
    }

    if (isGroupCollapsed(groupPath)) {
        state.collapsedGroupPaths = state.collapsedGroupPaths.filter((path) => path !== groupPath);
    } else {
        state.collapsedGroupPaths = [...state.collapsedGroupPaths, groupPath];
    }

    persistCollapsedGroupPaths();
    renderListView();
}

function labelForGroupKey(key) {
    const column = state.listColumns.find((item) => item.key === key);

    return column?.label || key;
}

function groupValueText(requirement, key) {
    if (!requirement) {
        return "";
    }

    if (key === "active") {
        return requirement.active ? "Active" : "Inactive";
    }

    const field = requirement.fields.find((item) => item.name === key);

    if (!field) {
        return String(requirement[key] || "").trim();
    }

    if (field.control === "datetime") {
        return formatDateTimeValue(field.value || field.rawValue || "");
    }

    if (field.control === "date") {
        return formatDateValue(field.value || field.rawValue || "");
    }

    return String(field.value || field.rawValue || "").trim();
}

function getStoredColumnWidths() {
    try {
        const raw = localStorage.getItem(STORAGE_KEYS.columnWidths);

        if (!raw) {
            return {};
        }

        const parsed = JSON.parse(raw);

        return parsed && typeof parsed === "object" ? parsed : {};
    } catch {
        return {};
    }
}

function persistColumnWidth(columnKey, widthPx) {
    const widths = getStoredColumnWidths();

    widths[columnKey] = `${Math.max(50, Math.round(widthPx))}px`;

    localStorage.setItem(STORAGE_KEYS.columnWidths, JSON.stringify(widths));
}

function renderListView() {
    const colGroup = document.getElementById("mainColGroup");
    const headerRow = document.getElementById("mainHeaderRow");
    const tbody = document.getElementById("tbody");
    const table = document.querySelector(".logicalstructure-table");

    if (!colGroup || !headerRow || !tbody) {
        return;
    }

    const columns = state.listColumns;
    const rows = getSortedRequirements(state.filteredRequirements);
    const responsiveWidths = getResponsiveColumnWidths(columns);
    const totalWidth = columns.reduce((sum, column) => sum + widthToPixels(column.width, 180), 0);

    if (table) {
        table.style.width = "100%";
        table.style.minWidth = `${Math.max(totalWidth, 1220)}px`;
    }

    colGroup.innerHTML = columns.map((column, index) => `<col style="width: ${escapeHtml(responsiveWidths[index])};">`).join("");

    headerRow.innerHTML = columns.map((column) => {
        const activeSort = state.sortKey === column.key;
        const indicator = activeSort ? (state.sortDirection === "asc" ? "▲" : "▼") : "";

        return `
            <th data-key="${escapeHtml(column.key)}" draggable="true" class="logicalstructure-resizable-th">
                <span class="sort">${escapeHtml(column.label)} <span class="sort-indicator">${indicator}</span></span>
                <span class="logicalstructure-column-resizer" data-resize-column="${escapeHtml(column.key)}" aria-hidden="true"></span>
            </th>
        `;
    }).join("");

    headerRow.querySelectorAll("th[data-key]").forEach((header) => {
        header.addEventListener("click", (event) => {
            if (event.target.closest(".logicalstructure-column-resizer")) {
                return;
            }

            const key = header.getAttribute("data-key");

            if (state.sortKey === key) {
                state.sortDirection = state.sortDirection === "asc" ? "desc" : "asc";
            } else {
                state.sortKey = key;
                state.sortDirection = "asc";
            }

            persistSorting();
            renderListView();
        });

        header.addEventListener("dragstart", (event) => {
            const key = header.getAttribute("data-key");

            if (!key) {
                return;
            }

            event.dataTransfer.setData("text/plain", key);
            event.dataTransfer.effectAllowed = "copy";
        });
    });

    initializeColumnResize(headerRow, colGroup, table);

    tbody.innerHTML = state.groupBy.length
        ? renderGroupedRows(rows, columns)
        : rows.map((requirement) => `
            <tr data-entity-id="${escapeHtml(requirement.entityId)}" data-requirement-index="${requirement.index}">
                ${columns.map((column) => renderListCell(requirement, column)).join("")}
            </tr>
        `).join("");

    tbody.querySelectorAll("tr[data-requirement-index]").forEach((row) => {
        const requirement = getRequirementFromRow(row);

        row.addEventListener("dblclick", () => {
            if (requirement) {
                openEditRequirement(requirement);
            }
        });

        row.addEventListener("contextmenu", (event) => {
            event.preventDefault();

            if (requirement) {
                openContextMenu(event.clientX, event.clientY, "requirement", requirement);
            }
        });
    });

    tbody.querySelectorAll(".logicalstructure-group-toggle").forEach((button) => {
        button.addEventListener("click", () => {
            toggleGroupCollapse(button.getAttribute("data-group-path") || "");
        });
    });

    if (rows.length === 0) {
        showListEmptyState("No logical structures match the current filters.");
    } else {
        hideListEmptyState();
    }
}

function renderGroupedRows(rows, columns, depth = 0, pathParts = []) {
    if (depth >= state.groupBy.length) {
        return rows.map((requirement) => renderRequirementRow(requirement, columns)).join("");
    }

    const groupKey = state.groupBy[depth];
    const groups = new Map();

    rows.forEach((row) => {
        const groupValue = groupValueText(row, groupKey) || "—";

        if (!groups.has(groupValue)) {
            groups.set(groupValue, []);
        }

        groups.get(groupValue).push(row);
    });

    return Array.from(groups.entries())
        .sort((a, b) => compareValues(a[0], b[0]))
        .map(([groupValue, groupRows]) => {
            const nextPathParts = [...pathParts, `${groupKey}:${groupValue}`];
            const groupPath = JSON.stringify(nextPathParts);
            const collapsed = isGroupCollapsed(groupPath);
            const nextRows = collapsed ? "" : renderGroupedRows(groupRows, columns, depth + 1, nextPathParts);

            return `
                <tr class="logicalstructure-group-row ${collapsed ? "is-collapsed" : ""}" data-group-path="${escapeHtml(groupPath)}">
                    <td colspan="${columns.length}">
                        <div class="logicalstructure-group-row-inner">
                            <button
                                    type="button"
                                    class="logicalstructure-group-toggle"
                                    data-group-path="${escapeHtml(groupPath)}"
                                    aria-label="${collapsed ? "Expand group" : "Collapse group"}"
                                    aria-expanded="${collapsed ? "false" : "true"}"
                            >${collapsed ? "+" : "-"}</button>
                            <span class="logicalstructure-group-label">${escapeHtml(labelForGroupKey(groupKey))}: ${escapeHtml(groupValue)}</span>
                            <span class="logicalstructure-group-count">(${groupRows.length})</span>
                        </div>
                    </td>
                </tr>
                ${nextRows}
            `;
        })
        .join("");
}

function renderRequirementRow(requirement, columns) {
    return `
        <tr data-entity-id="${escapeHtml(requirement.entityId)}" data-requirement-index="${requirement.index}">
            ${columns.map((column) => renderListCell(requirement, column)).join("")}
        </tr>
    `;
}

function initializeColumnResize(headerRow, colGroup, table) {
    const handles = Array.from(headerRow.querySelectorAll(".logicalstructure-column-resizer"));

    handles.forEach((handle) => {
        handle.addEventListener("mousedown", (event) => {
            event.preventDefault();
            event.stopPropagation();

            const th = handle.closest("th[data-key]");
            const columnKey = th?.getAttribute("data-key");

            if (!th || !columnKey) {
                return;
            }

            const startX = event.clientX;
            const startWidth = th.getBoundingClientRect().width;

            document.body.classList.add("logicalstructure-column-resizing");

            function onMouseMove(moveEvent) {
                const delta = moveEvent.clientX - startX;
                const nextWidth = Math.max(50, startWidth + delta);

                updateColumnWidth(columnKey, nextWidth, colGroup, table);
            }

            function onMouseUp(upEvent) {
                const delta = upEvent.clientX - startX;
                const nextWidth = Math.max(50, startWidth + delta);

                updateColumnWidth(columnKey, nextWidth, colGroup, table);
                persistColumnWidth(columnKey, nextWidth);
                renderListView();

                document.body.classList.remove("logicalstructure-column-resizing");
                window.removeEventListener("mousemove", onMouseMove);
                window.removeEventListener("mouseup", onMouseUp);
            }

            window.addEventListener("mousemove", onMouseMove);
            window.addEventListener("mouseup", onMouseUp);
        });
    });
}

function updateColumnWidth(columnKey, widthPx, colGroup, table) {
    const width = `${Math.max(50, Math.round(widthPx))}px`;
    const columnIndex = state.listColumns.findIndex((column) => column.key === columnKey);

    if (columnIndex < 0) {
        return;
    }

    state.listColumns[columnIndex].width = width;

    const col = colGroup?.children?.[columnIndex];

    if (col) {
        col.style.width = width;
    }

    if (table) {
        const totalWidth = state.listColumns.reduce((sum, column) => {
            return sum + widthToPixels(column.width, 180);
        }, 0);

        table.style.width = "100%";
        table.style.minWidth = `${Math.max(totalWidth, 1220)}px`;
    }
}

function widthToPixels(width, fallback) {
    const raw = String(width || "").trim();

    if (!raw) {
        return fallback;
    }

    if (raw.endsWith("px")) {
        const parsed = Number(raw.replace("px", ""));
        return Number.isFinite(parsed) ? parsed : fallback;
    }

    if (/^\d+$/.test(raw)) {
        const parsed = Number(raw);
        return Number.isFinite(parsed) ? parsed : fallback;
    }

    if (raw.endsWith("%")) {
        const parsed = Number(raw.replace("%", ""));
        return Number.isFinite(parsed) ? parsed : fallback;
    }

    return fallback;
}

function normalizeStoredColumnWidth(value, fallback) {
    const raw = String(value || "").trim();

    if (!raw) {
        return fallback;
    }

    if (raw.endsWith("%")) {
        return fallback;
    }

    return raw;
}

function getResponsiveColumnWidths(columns) {
    const weights = columns.map((column) => widthToPixels(column.width, 180));
    const totalWeight = weights.reduce((sum, value) => sum + value, 0) || 1;

    return weights.map((weight) => `${((weight / totalWeight) * 100).toFixed(4)}%`);
}

function renderListCell(requirement, column) {
    const field = requirement.fields.find((item) => item.name === column.key);
    const value = field?.value ?? "";
    const displayValue = formatListCellValue(value, column.control);
    const displayColor = column.control === "select" ? sanitizeCssColor(field?.color, "") : "";

    if (column.isActiveColumn) {
        return `
            <td title="${requirement.active ? "Active" : "Inactive"}">
                <span class="logicalstructure-active-state" aria-label="${requirement.active ? "Active" : "Inactive"}">
                    <span class="logicalstructure-active-dot ${requirement.active ? "is-active" : "is-inactive"}" aria-hidden="true"></span>
                </span>
            </td>
        `;
    }

    const colorStyle = displayColor ? ` style="${escapeHtml(buildColorChipStyle(displayColor))}"` : "";
    const colorClass = displayColor ? " select-chip" : "";

    return `<td title="${escapeHtml(displayValue)}"><span class="data-table-cell-value${colorClass}"${colorStyle}>${escapeHtml(displayValue)}</span></td>`;
}

function formatListCellValue(value, control) {
    const raw = String(value ?? "").trim();

    if (!raw) {
        return "";
    }

    if (control === "datetime") {
        return formatDateTimeValue(raw);
    }

    if (control === "date") {
        return formatDateValue(raw);
    }

    return raw;
}

function formatDateTimeValue(value) {
    const normalized = value.replace(" ", "T");
    const match = normalized.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?/);

    if (!match) {
        return value;
    }

    const [, year, month, day, hour, minute] = match;

    return `${day}-${month}-${year} ${hour}:${minute}`;
}

function formatDateValue(value) {
    const match = value.match(/^(\d{4})-(\d{2})-(\d{2})/);

    if (!match) {
        return value;
    }

    const [, year, month, day] = match;

    return `${day}-${month}-${year}`;
}

function getSortedRequirements(requirements) {
    const rows = [...requirements];

    rows.sort((a, b) => {
        for (const groupKey of state.groupBy) {
            const groupComparison = compareValues(groupValueText(a, groupKey), groupValueText(b, groupKey));

            if (groupComparison !== 0) {
                if (groupKey === state.sortKey && state.sortDirection === "desc") {
                    return -groupComparison;
                }

                return groupComparison;
            }
        }

        if (!state.sortKey) {
            return compareValues(a.id, b.id);
        }

        const av = getRequirementSortValue(a, state.sortKey);
        const bv = getRequirementSortValue(b, state.sortKey);
        const comparison = compareValues(av, bv);

        if (comparison !== 0) {
            return state.sortDirection === "desc" ? -comparison : comparison;
        }

        return compareValues(a.id, b.id);
    });

    return rows;
}

function getRequirementSortValue(requirement, key) {
    const field = requirement.fields.find((item) => item.name === key);

    if (!field) {
        return "";
    }

    if (isActiveFieldName(key)) {
        return requirement.active ? "1" : "0";
    }

    return field.value || field.rawValue || "";
}

function compareValues(a, b) {
    return String(a || "").localeCompare(String(b || ""), "da", {
        numeric: true,
        sensitivity: "base"
    });
}

function getRequirementFromRow(row) {
    const index = Number(row.getAttribute("data-requirement-index"));
    return state.requirements.find((requirement) => requirement.index === index) || null;
}

function renderDiagramView(orientation) {
    const nodesContainer = document.getElementById(`${orientation}DiagramNodes`);
    const svg = document.getElementById(`${orientation}DiagramSvg`);
    const canvas = document.getElementById(`${orientation}DiagramCanvas`);

    if (!nodesContainer || !svg || !canvas) {
        return;
    }

    nodesContainer.innerHTML = "";
    svg.innerHTML = "";

    const tree = buildVisibleTree();
    const layout = layoutTree(tree, orientation);

    canvas.style.width = `${layout.width}px`;
    canvas.style.height = `${layout.height}px`;
    svg.setAttribute("width", String(layout.width));
    svg.setAttribute("height", String(layout.height));
    svg.setAttribute("viewBox", `0 0 ${layout.width} ${layout.height}`);

    layout.edges.forEach((edge) => {
        svg.appendChild(createSvgPath(edge, orientation));
    });

    layout.nodes.forEach((node) => {
        nodesContainer.appendChild(createDiagramNode(node));
    });

    if (state.filteredRequirements.length === 0) {
        showDiagramEmptyState(orientation, "No logical structures match the current filters.");
    } else {
        hideDiagramEmptyState(orientation);
    }
}

function buildVisibleTree() {
    const projectNode = {
        type: "project",
        id: "project-root",
        code: "Project",
        name: state.topPanel.projectName || "Project",
        description: state.topPanel.customerName || "",
        requirement: null,
        children: []
    };

    const byCode = new Map();

    state.filteredRequirements.forEach((requirement) => {
        byCode.set(requirement.id, {
            type: "requirement",
            id: requirement.entityId || requirement.id,
            code: requirement.id,
            name: requirement.name,
            description: requirement.description,
            requirement,
            children: []
        });
    });

    byCode.forEach((node) => {
        const parentCode = node.requirement?.parentCode || "";
        const parent = parentCode ? byCode.get(parentCode) : null;

        if (parent) {
            parent.children.push(node);
        } else {
            projectNode.children.push(node);
        }
    });

    sortTree(projectNode);

    return projectNode;
}

function sortTree(node) {
    node.children.sort((a, b) => compareValues(a.code, b.code));
    node.children.forEach(sortTree);
}

function layoutTree(root, orientation) {
    const horizontalGap = orientation === "horizontal" ? 110 : 70;
    const verticalGap = orientation === "horizontal" ? 52 : 92;
    const margin = 28;
    const nodes = [];
    const edges = [];

    if (orientation === "horizontal") {
        layoutHorizontal(root, 0, margin, {
            horizontalGap,
            verticalGap,
            nodes,
            edges
        });
    } else {
        layoutVertical(root, 0, margin, {
            horizontalGap,
            verticalGap,
            nodes,
            edges
        });
    }

    const bounds = calculateBounds(nodes, margin);

    return {
        nodes,
        edges,
        width: bounds.width,
        height: bounds.height,
        nodeWidth: DIAGRAM_NODE_WIDTH,
        nodeHeight: REQUIREMENT_NODE_HEIGHT
    };
}

function layoutHorizontal(node, depth, nextY, context) {
    const { horizontalGap, verticalGap, nodes, edges } = context;
    const dimensions = getDiagramNodeDimensions(node);
    const x = 28 + depth * (dimensions.width + horizontalGap);

    if (!node.children.length) {
        nodes.push({
            ...node,
            x,
            y: nextY,
            width: dimensions.width,
            height: dimensions.height
        });

        return nextY + dimensions.height + verticalGap;
    }

    let childY = nextY;

    node.children.forEach((child) => {
        childY = layoutHorizontal(child, depth + 1, childY, context);
    });

    const childNodes = nodes.filter((item) => node.children.some((child) => child.id === item.id));
    const firstChild = childNodes[0];
    const lastChild = childNodes[childNodes.length - 1];

    const y = firstChild && lastChild
        ? firstChild.y + ((lastChild.y - firstChild.y) / 2)
        : nextY;

    const positioned = {
        ...node,
        x,
        y,
        width: dimensions.width,
        height: dimensions.height
    };

    nodes.push(positioned);

    node.children.forEach((child) => {
        const childPosition = nodes.find((item) => item.id === child.id);

        if (childPosition) {
            edges.push({
                from: positioned,
                to: childPosition
            });
        }
    });

    return childY;
}

function layoutVertical(node, depth, nextX, context) {
    const { horizontalGap, verticalGap, nodes, edges } = context;
    const dimensions = getDiagramNodeDimensions(node);
    const y = 28 + depth * (dimensions.height + verticalGap);

    if (!node.children.length) {
        nodes.push({
            ...node,
            x: nextX,
            y,
            width: dimensions.width,
            height: dimensions.height
        });

        return nextX + dimensions.width + horizontalGap;
    }

    let childX = nextX;

    node.children.forEach((child) => {
        childX = layoutVertical(child, depth + 1, childX, context);
    });

    const childNodes = nodes.filter((item) => node.children.some((child) => child.id === item.id));
    const firstChild = childNodes[0];
    const lastChild = childNodes[childNodes.length - 1];

    const x = firstChild && lastChild
        ? firstChild.x + ((lastChild.x - firstChild.x) / 2)
        : nextX;

    const positioned = {
        ...node,
        x,
        y,
        width: dimensions.width,
        height: dimensions.height
    };

    nodes.push(positioned);

    node.children.forEach((child) => {
        const childPosition = nodes.find((item) => item.id === child.id);

        if (childPosition) {
            edges.push({
                from: positioned,
                to: childPosition
            });
        }
    });

    return childX;
}

function getDiagramNodeDimensions(node) {
    return {
        width: DIAGRAM_NODE_WIDTH,
        height: node.type === "project" ? PROJECT_NODE_HEIGHT : REQUIREMENT_NODE_HEIGHT
    };
}

function calculateBounds(nodes, margin) {
    const maxX = Math.max(...nodes.map((node) => node.x + node.width), 800);
    const maxY = Math.max(...nodes.map((node) => node.y + node.height), 480);

    return {
        width: maxX + margin,
        height: maxY + margin
    };
}

function createSvgPath(edge, orientation) {
    const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
    const from = edge.from;
    const to = edge.to;

    let d;

    if (orientation === "horizontal") {
        const x1 = from.x + from.width;
        const y1 = from.y + from.height / 2;
        const x2 = to.x;
        const y2 = to.y + to.height / 2;
        const midX = x1 + ((x2 - x1) / 2);

        d = `M ${x1} ${y1} H ${midX} V ${y2} H ${x2}`;
    } else {
        const x1 = from.x + from.width / 2;
        const y1 = from.y + from.height;
        const x2 = to.x + to.width / 2;
        const y2 = to.y;
        const midY = y1 + ((y2 - y1) / 2);

        d = `M ${x1} ${y1} V ${midY} H ${x2} V ${y2}`;
    }

    path.setAttribute("d", d);
    path.setAttribute("class", "logicalstructure-diagram-edge");

    return path;
}

function createDiagramNode(node) {
    const element = document.createElement("article");
    element.className = `logicalstructure-diagram-node${node.type === "project" ? " is-project-node" : ""}`;
    element.style.left = `${node.x}px`;
    element.style.top = `${node.y}px`;
    element.style.width = `${node.width}px`;
    element.style.height = `${node.height}px`;
    element.setAttribute("data-node-type", node.type);

    if (node.requirement) {
        element.setAttribute("data-entity-id", node.requirement.entityId || node.requirement.id);
    }

    if (node.type === "project") {
        element.title = buildProjectTooltip(node);
        element.innerHTML = `
            <div class="logicalstructure-diagram-node-code"></div>
            <div class="logicalstructure-diagram-node-name">${escapeHtml(node.name)}</div>
            <div class="logicalstructure-diagram-node-footer">${escapeHtml(node.description || "â€”")}</div>
        `;
    } else {
        element.title = buildRequirementTooltip(node.requirement);
        element.innerHTML = `
            <div class="logicalstructure-diagram-node-code">${escapeHtml(node.code)}</div>
            <div class="logicalstructure-diagram-node-name">${escapeHtml(node.name)}</div>
            ${renderStatusBars()}
        `;
    }

    if (node.type === "requirement") {
        element.addEventListener("dblclick", () => {
            openEditRequirement(node.requirement);
        });

        element.addEventListener("contextmenu", (event) => {
            event.preventDefault();
            event.stopPropagation();
            openContextMenu(event.clientX, event.clientY, "requirement", node.requirement);
        });
    } else {
        element.addEventListener("dblclick", (event) => {
            event.preventDefault();
            event.stopPropagation();
        });

        element.addEventListener("contextmenu", (event) => {
            event.preventDefault();
            event.stopPropagation();
            openContextMenu(event.clientX, event.clientY, "project", null);
        });
    }

    return element;
}

function buildProjectTooltip(node) {
    return [
        `Project: ${node?.name || "â€”"}`,
        node?.description ? `Customer: ${node.description}` : ""
    ].filter(Boolean).join("\n");
}

function buildRequirementTooltip(requirement) {
    return [
        `ID: ${requirement?.id || "â€”"}`,
        `Name: ${requirement?.name || "â€”"}`,
        requirement?.description ? `Description: ${requirement.description}` : ""
    ].filter(Boolean).join("\n");
}

function renderStatusBars() {
    return `
        <div class="logicalstructure-diagram-status-bars">
            <div class="logicalstructure-diagram-status-bar">
                <span class="logicalstructure-diagram-status-value">&nbsp;</span>
            </div>
            <div class="logicalstructure-diagram-status-bar">
                <span class="logicalstructure-diagram-status-value">&nbsp;</span>
            </div>
        </div>
    `;
}

function initializeContextMenuEvents() {
    const menu = document.getElementById("logicalStructureContextMenu");

    menu?.addEventListener("click", (event) => {
        const button = event.target.closest("button[data-context-action]");

        if (!button) {
            return;
        }

        event.preventDefault();
        event.stopPropagation();

        const action = button.getAttribute("data-context-action");
        handleContextMenuAction(action);
    });

    menu?.addEventListener("contextmenu", (event) => {
        event.preventDefault();
    });

    document.addEventListener("click", (event) => {
        if (!menu?.contains(event.target)) {
            closeContextMenu();
        }
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            closeContextMenu();
        }
    });

    window.addEventListener("scroll", closeContextMenu, true);
    window.addEventListener("resize", closeContextMenu);
}

function handleContextMenuAction(action) {
    const requirement = state.contextRequirement;

    closeContextMenu();

    if (action === "edit-requirement") {
        if (!requirement) {
            window.alert("No Logical Structure selected.");
            return;
        }

        openEditRequirement(requirement);
        return;
    }

    if (action === "create-sub-requirement") {
        if (!requirement) {
            window.alert("No Logical Structure selected.");
            return;
        }

        openCreateSubRequirement(requirement);
        return;
    }

    if (action === "create-root-requirement") {
        openCreateRootRequirement();
    }
}

function openContextMenu(x, y, targetType, requirement) {
    const menu = document.getElementById("logicalStructureContextMenu");
    const editButton = document.getElementById("contextMenuEditRequirement");
    const createSubButton = document.getElementById("contextMenuCreateSubRequirement");
    const createRootButton = document.getElementById("contextMenuCreateRootRequirement");

    if (!menu) {
        return;
    }

    state.contextTargetType = targetType;
    state.contextRequirement = requirement || null;

    const isRequirement = targetType === "requirement";
    const isProject = targetType === "project";

    if (editButton) {
        editButton.hidden = !isRequirement;
        editButton.toggleAttribute("hidden", !isRequirement);
    }

    if (createSubButton) {
        createSubButton.hidden = !isRequirement;
        createSubButton.toggleAttribute("hidden", !isRequirement);
    }

    if (createRootButton) {
        createRootButton.hidden = !isProject;
        createRootButton.toggleAttribute("hidden", !isProject);
    }

    menu.style.left = "0px";
    menu.style.top = "0px";
    menu.classList.add("is-open");
    menu.setAttribute("aria-hidden", "false");

    const menuRect = menu.getBoundingClientRect();
    const margin = 8;

    let left = x;
    let top = y;

    if (left + menuRect.width + margin > window.innerWidth) {
        left = window.innerWidth - menuRect.width - margin;
    }

    if (top + menuRect.height + margin > window.innerHeight) {
        top = window.innerHeight - menuRect.height - margin;
    }

    menu.style.left = `${Math.max(margin, left)}px`;
    menu.style.top = `${Math.max(margin, top)}px`;

    const firstVisibleButton = Array.from(menu.querySelectorAll("button"))
        .find((button) => !button.hidden);

    firstVisibleButton?.focus?.();
}

function closeContextMenu() {
    const menu = document.getElementById("logicalStructureContextMenu");

    if (!menu) {
        return;
    }

    menu.classList.remove("is-open");
    menu.setAttribute("aria-hidden", "true");
    state.contextTargetType = "";
    state.contextRequirement = null;
}

function getRequirementNavigationId(requirement) {
    return requirement?.entityId || requirement?.id || "";
}

function openEditRequirement(requirement) {
    const id = getRequirementNavigationId(requirement);

    if (!id) {
        window.alert("Logical Structure has no entity id.");
        return;
    }

    openEditDialog({
        page: "logicalstructure-edit",
        mode: "edit",
        id,
        title: "Edit Logical Structure",
        onSaved: () => window.location.reload()
    });
}

function openCreateSubRequirement(requirement) {
    const id = getRequirementNavigationId(requirement);

    if (!id) {
        window.alert("Logical Structure has no entity id.");
        return;
    }

    openEditDialog({
        page: "logicalstructure-edit",
        mode: "create-child",
        id,
        title: "Create Sub Logical Structure",
        onSaved: () => window.location.reload()
    });
}

function openCreateRootRequirement() {
    openEditDialog({
        page: "logicalstructure-edit",
        mode: "create-root",
        title: "Create Root Logical Structure",
        onSaved: () => window.location.reload()
    });
}

function buildEditPageUrl(params) {
    const url = new URL(EDIT_PAGE_URL, window.location.href);

    url.searchParams.set("mode", params.mode);

    if (params.id) {
        url.searchParams.set("id", params.id);
    }

    url.searchParams.set("returnUrl", "/web/view?page=logicalstructure-main");

    return url.toString();
}

function downloadCurrentDiagramPdf() {
    if (state.selectedView === VIEW_TYPES.list) {
        return;
    }

    const orientation = state.selectedView === VIEW_TYPES.horizontal ? "horizontal" : "vertical";
    const tree = buildVisibleTree();
    const layout = layoutTree(tree, orientation);

    downloadLogicalStructureDiagramPdf({
        tree,
        layout,
        orientation,
        topPanel: state.topPanel,
        requirementCount: state.filteredRequirements.length
    });
}

function showListEmptyState(message) {
    const emptyState = document.getElementById("listEmptyState");

    if (emptyState) {
        emptyState.textContent = message;
        emptyState.classList.add("is-visible");
        emptyState.hidden = false;
    }
}

function hideListEmptyState() {
    const emptyState = document.getElementById("listEmptyState");

    if (emptyState) {
        emptyState.classList.remove("is-visible");
        emptyState.hidden = true;
    }
}

function showDiagramEmptyState(orientation, message) {
    const emptyState = document.getElementById(`${orientation}DiagramEmptyState`);

    if (emptyState) {
        emptyState.textContent = message;
        emptyState.classList.add("is-visible");
        emptyState.hidden = false;
    }
}

function hideDiagramEmptyState(orientation) {
    const emptyState = document.getElementById(`${orientation}DiagramEmptyState`);

    if (emptyState) {
        emptyState.classList.remove("is-visible");
        emptyState.hidden = true;
    }
}

function debounce(fn, delay) {
    let timeoutId = null;

    return (...args) => {
        window.clearTimeout(timeoutId);
        timeoutId = window.setTimeout(() => fn(...args), delay);
    };
}
