import { initMenu } from "../components/menu.js";
import { initHelpDialog } from "../components/help-dialog.js";
import { mountTopbar } from "../components/topbar.js";
import { applyTopbarMetadata } from "../components/topbar.js";
import { createExportDialog } from "../components/export-dialog.js";
import { createImportDialog } from "../components/import-dialog.js";
import { downloadSystemsBreakdownDiagramPdf } from "./systemsbreakdown-diagram-pdf.js";
import { setText } from "../core/dom.js";
import {
    getDirectChild,
    getDirectText,
    hasXmlParseError
} from "../core/xml.js";
import { sanitizeClassPart } from "../core/css.js";
import {
    isFalsy,
    isTruthy
} from "../core/utils.js";

const LIST_URL = "/project/systembreakdown?cmd=list";
const EDIT_PAGE_URL = "/web/view?page=systemsbreakdown-edit";

const EMPTY_FILTER_MESSAGE = "No physical structure match the current filters.";

const STORAGE_KEYS = {
    selectedView: "basis.systemsbreakdown.main.selectedView",
    filterText: "basis.systemsbreakdown.main.filterText",
    activeOnly: "basis.systemsbreakdown.main.activeOnly",
    sortKey: "basis.systemsbreakdown.main.sortKey",
    sortDirection: "basis.systemsbreakdown.main.sortDirection",
    columnWidths: "basis.systemsbreakdown.main.columnWidths"
};

const VIEW_TYPES = {
    list: "list",
    horizontal: "horizontal",
    vertical: "vertical"
};

const VIEW_LABELS = {
    list: "Liste-visning",
    horizontal: "Horisontal diagram",
    vertical: "Vertikal diagram"
};

/*
 * KOPIERET FRA basissystemrequirementdiagramV2-page.js:
 * Disse mål bruges til at få project- og child-kasserne i det vertikale diagram
 * til at være visuelt identiske med kasserne i basissystemrequirementdiagramV2-page.js.
 */
const V2_NODE_WIDTH = 190;
const V2_NODE_HEIGHT = 92;
const V2_ROOT_WIDTH = 230;
const V2_ROOT_HEIGHT = 96;
const V2_HORIZONTAL_GAP = 96;
const V2_VERTICAL_GAP = 72;

const state = {
    xmlDocument: null,
    topPanel: {
        customerName: "—",
        projectName: "—",
        userName: "—"
    },
    systems: [],
    filteredSystems: [],
    listColumns: [],
    selectedView: VIEW_TYPES.list,
    sortKey: "",
    sortDirection: "asc",
    contextTargetType: "",
    contextSystem: null,
    lastRenderedTree: null,
    lastRenderedLayout: null,
    lastRenderedOrientation: ""
};

document.addEventListener("DOMContentLoaded", () => {
    start();
});

function start() {
    initializeShell();
    initializeStateFromStorage();
    initializeEvents();
    initializeImportExportDialogs();
    injectCopiedV2DiagramStyles();
    applyView(state.selectedView, { persist: false });
    loadSystemsBreakdown();
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

    if (Object.values(VIEW_TYPES).includes(storedView)) {
        state.selectedView = storedView;
    }

    state.sortKey = localStorage.getItem(STORAGE_KEYS.sortKey) || "";
    state.sortDirection = localStorage.getItem(STORAGE_KEYS.sortDirection) || "asc";

    const filterText = sessionStorage.getItem(STORAGE_KEYS.filterText) || "";
    const activeOnly = sessionStorage.getItem(STORAGE_KEYS.activeOnly);

    const filterInput = document.getElementById("filterSystemText");
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
            applyView(viewType, { persist: true });
            applyFiltersAndRender();
        });
    });

    const filterInput = document.getElementById("filterSystemText");
    const activeOnlyInput = document.getElementById("filterActiveOnly");
    const clearFilterButton = document.getElementById("btnClearFilter");
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

    addRootButton?.addEventListener("click", () => {
        openCreateRootSystem();
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
        entityName: "Physical Structure",
        exportUrl: "/project/systembreakdown"
    });

    const importDialog = createImportDialog({
        dialogId: "importDialog",
        openButtonId: "btnImport",
        importUrl: "/project/systembreakdown?cmd=import",
        onImportComplete: async () => {
            await loadSystemsBreakdown();
        }
    });

    exportDialog.bind();
    importDialog.bind();
}

async function loadSystemsBreakdown() {
    showListEmptyState("Loading physical structure…");
    showDiagramEmptyState("horizontal", "Loading horizontal physical structure diagram…");
    showDiagramEmptyState("vertical", "Loading vertical physical structure diagram…");
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
            throw new Error("The physical structure endpoint returned invalid XML.");
        }

        state.xmlDocument = xmlDocument;
        state.currentDoc = xmlDocument;
        state.topPanel = parseTopPanel(xmlDocument);
        state.systems = parseSystemsBreakdown(xmlDocument);
        state.listColumns = buildListColumns(state.systems);

        if (state.sortKey && !state.listColumns.some((column) => column.key === state.sortKey)) {
            state.sortKey = "";
            state.sortDirection = "asc";
            persistSorting();
        }

        applyTopPanel();
        applyFiltersAndRender();

        setText("loadStatus", "Loaded", "");
    } catch (error) {
        console.error("Failed to load physical structure", error);
        setText("loadStatus", "Error", "");
        showListEmptyState(`Could not load physical structure. ${error.message}`);
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

function parseSystemsBreakdown(xmlDocument) {
    const systemNodes = Array.from(
        xmlDocument.querySelectorAll("systembreakdowns > systembreakdown")
    );

    return systemNodes.map((node, index) => {
        const fields = parseVisibleFields(node);

        const entityId = getFirstFieldRawValue(node, [
            "EntityId",
            "SystemBreakdownId",
            "SystemBreakdownPK",
            "SystemBreakdownEntityId"
        ], "");

        const id = normalizeSystemCode(getFirstFieldDisplayText(node, [
            "SBSCode",
            "SystemCode",
            "SystemBreakdownCode",
            "SystemId",
            "Code"
        ], ""));

        const name = getFirstFieldDisplayText(node, [
            "SystemName",
            "SystemBreakdownName",
            "Name",
            "Title"
        ], "—");

        const description = getFirstFieldDisplayText(node, [
            "SystemDescription",
            "SystemBreakdownDescription",
            "Description"
        ], "");

        const status = getFirstFieldDisplayText(node, [
            "SystemStatusId",
            "SystemStatus",
            "Status"
        ], "—");

        const active = parseActiveFlag(node, fields);
        const parentCode = getParentCodeFromFields(node, id);
        const level = calculateLevelFromCode(id);

        return {
            node,
            index,
            entityId,
            id,
            name,
            description,
            status,
            active,
            fields,
            level,
            parentCode
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
                value,
                rawValue,
                displayOrder,
                tableWidth,
                originalIndex: index
            };
        });
}

function buildListColumns(systems) {
    const byName = new Map();

    systems.forEach((system) => {
        system.fields.forEach((field) => {
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
        width: storedWidths[column.key] || column.tableWidth || calculateFallbackColumnWidth(column)
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
    if (key.includes("status")) return "190px";
    if (key.includes("owner")) return "190px";
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
        "SystemActive",
        "SystemBreakdownActive"
    ], "");

    const displayValue = getFirstFieldDisplayText(node, [
        "Active",
        "IsActive",
        "SystemActive",
        "SystemBreakdownActive"
    ], "");

    return parseBooleanValue(rawValue || displayValue, true);
}

function isActiveFieldName(name) {
    const normalized = String(name || "").toLowerCase();

    return [
        "active",
        "isactive",
        "systemactive",
        "systembreakdownactive"
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

function getParentCodeFromFields(node, code) {
    const explicitParent = getFirstFieldDisplayText(node, [
        "ParentSystemCode",
        "ParentSystemBreakdownCode",
        "ParentCode"
    ], "");

    if (explicitParent) {
        return normalizeSystemCode(explicitParent);
    }

    return calculateParentCodeFromCode(code);
}

function normalizeSystemCode(code) {
    return String(code || "").trim();
}

function calculateLevelFromCode(code) {
    const normalized = normalizeSystemCode(code);

    if (!normalized) {
        return 1;
    }

    const codeWithoutFunctionPrefix = stripFunctionPrefix(normalized);

    if (!codeWithoutFunctionPrefix) {
        return 1;
    }

    return codeWithoutFunctionPrefix.split(/[.\-_]/).filter(Boolean).length || 1;
}

function calculateParentCodeFromCode(code) {
    const normalized = normalizeSystemCode(code);

    if (!normalized) {
        return "";
    }

    const functionPrefix = getFunctionPrefix(normalized);
    const codeWithoutFunctionPrefix = stripFunctionPrefix(normalized);

    if (!codeWithoutFunctionPrefix) {
        return "";
    }

    const separators = [".", "-", "_"];

    for (const separator of separators) {
        if (codeWithoutFunctionPrefix.includes(separator)) {
            const parts = codeWithoutFunctionPrefix.split(separator).filter(Boolean);

            if (parts.length > 1) {
                parts.pop();
                return `${functionPrefix}${parts.join(separator)}`;
            }
        }
    }

    return "";
}

function getFunctionPrefix(code) {
    const normalized = normalizeSystemCode(code);

    if (!normalized) {
        return "";
    }

    return characterIsDigit(normalized.charAt(0)) ? "" : normalized.charAt(0);
}

function stripFunctionPrefix(code) {
    const normalized = normalizeSystemCode(code);

    if (!normalized) {
        return "";
    }

    return characterIsDigit(normalized.charAt(0)) ? normalized : normalized.slice(1);
}

function characterIsDigit(value) {
    return /^[0-9]$/.test(String(value || ""));
}

function applyFiltersAndRender() {
    const filterText = (document.getElementById("filterSystemText")?.value || "").trim().toLowerCase();
    const activeOnly = document.getElementById("filterActiveOnly")?.checked !== false;

    state.filteredSystems = state.systems.filter((system) => {
        if (activeOnly && !system.active) {
            return false;
        }

        if (!filterText) {
            return true;
        }

        return [
            system.id,
            system.name,
            system.description,
            system.status
        ].some((value) => String(value || "").toLowerCase().includes(filterText));
    });

    setText("systemsBreakdownCount", String(state.filteredSystems.length), "");
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

    setElementHidden("btnImport", !isListView);
    setElementHidden("btnExport", !isListView);
    setElementHidden("btnAddRoot", !isListView);
    setElementHidden("btnDownloadDiagramPdf", isListView);
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
    const filterText = document.getElementById("filterSystemText")?.value || "";
    const activeOnly = document.getElementById("filterActiveOnly")?.checked !== false;

    sessionStorage.setItem(STORAGE_KEYS.filterText, filterText);
    sessionStorage.setItem(STORAGE_KEYS.activeOnly, String(activeOnly));
}

function persistSorting() {
    localStorage.setItem(STORAGE_KEYS.sortKey, state.sortKey || "");
    localStorage.setItem(STORAGE_KEYS.sortDirection, state.sortDirection || "asc");
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
    const table = document.querySelector(".systemsbreakdown-table");

    if (!colGroup || !headerRow || !tbody) {
        return;
    }

    const columns = state.listColumns;
    const rows = getSortedSystems(state.filteredSystems);
    const totalWidth = columns.reduce((sum, column) => sum + widthToPixels(column.width, 180), 0);

    if (table) {
        table.style.minWidth = `${Math.max(totalWidth, 1220)}px`;
    }

    colGroup.innerHTML = columns.map((column) => `<col style="width: ${escapeHtml(column.width)};">`).join("");

    headerRow.innerHTML = columns.map((column) => {
        const activeSort = state.sortKey === column.key;
        const indicator = activeSort ? (state.sortDirection === "asc" ? "▲" : "▼") : "";

        return `
            <th data-key="${escapeHtml(column.key)}" class="systemsbreakdown-resizable-th">
                <span class="sort">${escapeHtml(column.label)} <span class="sort-indicator">${indicator}</span></span>
                <span class="systemsbreakdown-column-resizer" data-resize-column="${escapeHtml(column.key)}" aria-hidden="true"></span>
            </th>
        `;
    }).join("");

    headerRow.querySelectorAll("th[data-key]").forEach((header) => {
        header.addEventListener("click", (event) => {
            if (event.target.closest(".systemsbreakdown-column-resizer")) {
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
    });

    initializeColumnResize(headerRow, colGroup, table);

    tbody.innerHTML = rows.map((system) => `
        <tr data-entity-id="${escapeHtml(system.entityId)}" data-system-index="${system.index}">
            ${columns.map((column) => renderListCell(system, column)).join("")}
        </tr>
    `).join("");

    tbody.querySelectorAll("tr[data-system-index]").forEach((row) => {
        const system = getSystemFromRow(row);

        row.addEventListener("dblclick", () => {
            if (system) {
                openEditSystem(system);
            }
        });

        row.addEventListener("contextmenu", (event) => {
            event.preventDefault();

            if (system) {
                openContextMenu(event.clientX, event.clientY, "system", system);
            }
        });
    });

    if (rows.length === 0) {
        showListEmptyState(EMPTY_FILTER_MESSAGE);
    } else {
        hideListEmptyState();
    }
}

function initializeColumnResize(headerRow, colGroup, table) {
    const handles = Array.from(headerRow.querySelectorAll(".systemsbreakdown-column-resizer"));

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

            document.body.classList.add("systemsbreakdown-column-resizing");

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

                document.body.classList.remove("systemsbreakdown-column-resizing");
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
        return Number.isFinite(parsed) ? Math.max(80, parsed * 18) : fallback;
    }

    return fallback;
}

function renderListCell(system, column) {
    const field = system.fields.find((item) => item.name === column.key);
    const value = field?.value ?? "";
    const displayValue = formatListCellValue(value, column.control);

    if (column.isActiveColumn) {
        return `
            <td title="${system.active ? "Active" : "Inactive"}">
                <span class="systemsbreakdown-active-state" aria-label="${system.active ? "Active" : "Inactive"}">
                    <span class="systemsbreakdown-active-dot ${system.active ? "is-active" : "is-inactive"}" aria-hidden="true"></span>
                </span>
            </td>
        `;
    }

    return `<td title="${escapeHtml(displayValue)}">${escapeHtml(displayValue)}</td>`;
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

function getSortedSystems(systems) {
    const rows = [...systems];

    if (!state.sortKey) {
        return rows;
    }

    rows.sort((a, b) => {
        const av = getSystemSortValue(a, state.sortKey);
        const bv = getSystemSortValue(b, state.sortKey);

        return compareValues(av, bv);
    });

    if (state.sortDirection === "desc") {
        rows.reverse();
    }

    return rows;
}

function getSystemSortValue(system, key) {
    const field = system.fields.find((item) => item.name === key);

    if (!field) {
        return "";
    }

    if (isActiveFieldName(key)) {
        return system.active ? "1" : "0";
    }

    return field.value || field.rawValue || "";
}

function compareValues(a, b) {
    return String(a || "").localeCompare(String(b || ""), "da", {
        numeric: true,
        sensitivity: "base"
    });
}

function getSystemFromRow(row) {
    const index = Number(row.getAttribute("data-system-index"));
    return state.systems.find((system) => system.index === index) || null;
}

/* ------------------------------------------------------------------ */
/* Diagram rendering                                                   */
/* ------------------------------------------------------------------ */

function renderDiagramView(orientation) {
    const nodesContainer = document.getElementById(`${orientation}DiagramNodes`);
    const svg = document.getElementById(`${orientation}DiagramSvg`);
    const canvas = document.getElementById(`${orientation}DiagramCanvas`);

    if (!nodesContainer || !svg || !canvas) {
        return;
    }

    nodesContainer.innerHTML = "";
    svg.innerHTML = "";

    state.lastRenderedTree = null;
    state.lastRenderedLayout = null;
    state.lastRenderedOrientation = "";

    if (state.filteredSystems.length === 0) {
        canvas.style.width = "";
        canvas.style.height = "";
        svg.removeAttribute("width");
        svg.removeAttribute("height");
        svg.removeAttribute("viewBox");
        showDiagramEmptyState(orientation, EMPTY_FILTER_MESSAGE);
        return;
    }

    const tree = buildVisibleTree();
    const layout = layoutTree(tree, orientation);

    state.lastRenderedTree = tree;
    state.lastRenderedLayout = layout;
    state.lastRenderedOrientation = orientation;

    canvas.style.width = `${layout.width}px`;
    canvas.style.height = `${layout.height}px`;
    svg.setAttribute("width", String(layout.width));
    svg.setAttribute("height", String(layout.height));
    svg.setAttribute("viewBox", `0 0 ${layout.width} ${layout.height}`);

    layout.edges.forEach((edge) => {
        svg.appendChild(createSvgPath(edge, orientation));
    });

    layout.nodes.forEach((node) => {
        nodesContainer.appendChild(createDiagramNode(node, orientation));
    });

    hideDiagramEmptyState(orientation);
}

function buildVisibleTree() {
    const projectNode = {
        type: "project",
        id: "project-root",
        code: "Project",
        name: state.topPanel.projectName || "Project",
        description: state.topPanel.customerName || "Customer",
        status: "",
        system: null,
        children: []
    };

    const byCode = new Map();

    state.filteredSystems.forEach((system) => {
        if (!system.id) {
            return;
        }

        byCode.set(system.id, {
            type: "system",
            id: system.entityId || system.id,
            code: system.id,
            name: system.name,
            description: system.description,
            status: system.status,
            system,
            children: []
        });
    });

    byCode.forEach((node) => {
        const parentCode = node.system?.parentCode || "";
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
    const nodeWidth = orientation === "vertical" ? V2_NODE_WIDTH : 260;
    const nodeHeight = orientation === "vertical" ? V2_NODE_HEIGHT : 140;
    const rootWidth = orientation === "vertical" ? V2_ROOT_WIDTH : 260;
    const rootHeight = orientation === "vertical" ? V2_ROOT_HEIGHT : 140;

    const horizontalGap = orientation === "vertical" ? V2_HORIZONTAL_GAP : 110;
    const verticalGap = orientation === "vertical" ? V2_VERTICAL_GAP : 52;

    const margin = orientation === "vertical" ? 48 : 28;
    const nodes = [];
    const edges = [];

    if (orientation === "horizontal") {
        layoutHorizontal(root, 0, margin, {
            nodeWidth,
            nodeHeight,
            rootWidth,
            rootHeight,
            horizontalGap,
            verticalGap,
            nodes,
            edges,
            margin
        });
    } else {
        layoutVerticalCopiedFromV2(root, 0, margin, {
            nodeWidth,
            nodeHeight,
            rootWidth,
            rootHeight,
            horizontalGap,
            verticalGap,
            nodes,
            edges,
            margin
        });
    }

    const bounds = calculateBounds(nodes, margin);

    return {
        nodes,
        edges,
        width: bounds.width,
        height: bounds.height,
        nodeWidth,
        nodeHeight,
        rootWidth,
        rootHeight
    };
}

function layoutHorizontal(node, depth, nextY, context) {
    const {
        nodeWidth,
        nodeHeight,
        rootWidth,
        rootHeight,
        horizontalGap,
        verticalGap,
        nodes,
        edges,
        margin
    } = context;

    const currentWidth = node.type === "project" ? rootWidth : nodeWidth;
    const currentHeight = node.type === "project" ? rootHeight : nodeHeight;
    const x = margin + depth * (nodeWidth + horizontalGap);

    if (!node.children.length) {
        nodes.push({
            ...node,
            x,
            y: nextY,
            width: currentWidth,
            height: currentHeight
        });

        return nextY + currentHeight + verticalGap;
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
        width: currentWidth,
        height: currentHeight
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

/*
 * KOPIERET FRA basissystemrequirementdiagramV2-page.js:
 * Det vertikale physical structure diagram bruger samme visuelle layout-principper:
 * - Project-kassen har samme størrelse/udtryk som V2 project-kassen.
 * - Child-kasser har samme størrelse/udtryk som V2 requirement-kasser.
 * - Forbindelser tegnes med samme lige/knækkede path-princip som V2.
 */
function layoutVerticalCopiedFromV2(root, depth, nextX, context) {
    const {
        nodeWidth,
        nodeHeight,
        rootWidth,
        rootHeight,
        horizontalGap,
        verticalGap,
        nodes,
        edges,
        margin
    } = context;

    const currentWidth = root.type === "project" ? rootWidth : nodeWidth;
    const currentHeight = root.type === "project" ? rootHeight : nodeHeight;
    const y = margin + depth * (nodeHeight + verticalGap);

    if (!root.children.length) {
        nodes.push({
            ...root,
            x: nextX,
            y,
            width: currentWidth,
            height: currentHeight
        });

        return nextX + currentWidth + horizontalGap;
    }

    let childX = nextX;

    root.children.forEach((child) => {
        childX = layoutVerticalCopiedFromV2(child, depth + 1, childX, context);
    });

    const childNodes = nodes.filter((item) => root.children.some((child) => child.id === item.id));
    const firstChild = childNodes[0];
    const lastChild = childNodes[childNodes.length - 1];

    const childrenLeft = firstChild ? firstChild.x : nextX;
    const childrenRight = lastChild ? lastChild.x + lastChild.width : nextX + currentWidth;
    const x = childrenLeft + ((childrenRight - childrenLeft) / 2) - currentWidth / 2;

    const positioned = {
        ...root,
        x,
        y,
        width: currentWidth,
        height: currentHeight
    };

    nodes.push(positioned);

    root.children.forEach((child) => {
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

function calculateBounds(nodes, margin) {
    const maxX = Math.max(...nodes.map((node) => node.x + node.width), 800);
    const maxY = Math.max(...nodes.map((node) => node.y + node.height), 480);

    return {
        width: maxX + margin,
        height: maxY + margin
    };
}

/*
 * KOPIERET FRA basissystemrequirementdiagramV2-page.js:
 * Samme type forbindelsesstreg som V2: M -> H/V -> H/V, altså lige knækkede streger.
 */
function createSvgPath(edge, orientation) {
    const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
    const from = edge.from;
    const to = edge.to;

    if (orientation === "vertical") {
        const startX = from.x + from.width / 2;
        const startY = from.y + from.height;
        const endX = to.x + to.width / 2;
        const endY = to.y;
        const midY = startY + (endY - startY) / 2;

        path.setAttribute("d", `M ${startX} ${startY} V ${midY} H ${endX} V ${endY}`);
        path.classList.add("systemsbreakdown-diagram-edge", "systemsbreakdown-v2-copied-connection");
        return path;
    }

    const startX = from.x + from.width;
    const startY = from.y + from.height / 2;
    const endX = to.x;
    const endY = to.y + to.height / 2;
    const midX = startX + (endX - startX) / 2;

    path.setAttribute("d", `M ${startX} ${startY} H ${midX} V ${endY} H ${endX}`);
    path.setAttribute("class", "systemsbreakdown-diagram-edge");

    return path;
}

/*
 * KOPIERET FRA basissystemrequirementdiagramV2-page.js:
 * createDiagramNode bruger i vertical mode samme DOM-struktur og samme class-navne
 * som V2-kasserne: systemrequirementdiagram-node, project-node og requirement-node.
 */
function createDiagramNode(node, orientation) {
    if (orientation === "vertical") {
        return createCopiedV2DiagramNode(node);
    }

    return createLegacySystemsBreakdownDiagramNode(node);
}

function createCopiedV2DiagramNode(node) {
    const element = document.createElement("button");
    element.type = "button";
    element.className = node.type === "project"
        ? "systemrequirementdiagram-node systemrequirementdiagram-project-node systemsbreakdown-v2-copied-node"
        : `systemrequirementdiagram-node systemrequirementdiagram-requirement-node systemsbreakdown-v2-copied-node ${getStatusClass(node.status || node.system?.status)}`;

    element.style.left = `${node.x}px`;
    element.style.top = `${node.y}px`;
    element.style.width = `${node.width}px`;
    element.style.height = `${node.height}px`;

    element.setAttribute("data-node-type", node.type);

    if (node.system) {
        element.setAttribute("data-entity-id", node.system.entityId || node.system.id);
    }

    if (node.type === "project") {
        renderCopiedV2ProjectNode(element);

        element.addEventListener("contextmenu", (event) => {
            event.preventDefault();
            event.stopPropagation();
            openContextMenu(event.clientX, event.clientY, "project", null);
        });
    } else {
        renderCopiedV2SystemNode(element, node);

        element.addEventListener("dblclick", () => {
            openEditSystem(node.system);
        });

        element.addEventListener("contextmenu", (event) => {
            event.preventDefault();
            event.stopPropagation();
            openContextMenu(event.clientX, event.clientY, "system", node.system);
        });
    }

    return element;
}

function renderCopiedV2ProjectNode(element) {
    const code = document.createElement("span");
    code.className = "systemrequirementdiagram-node-code";
    code.textContent = "";

    const name = document.createElement("span");
    name.className = "systemrequirementdiagram-node-name";
    name.textContent = state.topPanel.projectName || "Project";

    const footer = document.createElement("span");
    footer.className = "systemrequirementdiagram-node-footer";
    footer.textContent = state.topPanel.customerName || "Customer";

    element.append(code, name, footer);
}

function renderCopiedV2SystemNode(element, node) {
    element.title = buildSystemTooltip(node.system);

    const code = document.createElement("span");
    code.className = "systemrequirementdiagram-node-code";
    code.textContent = node.code || "—";

    const name = document.createElement("span");
    name.className = "systemrequirementdiagram-node-name";
    name.textContent = node.name || "—";

    const footer = document.createElement("span");
    footer.className = "systemrequirementdiagram-node-footer";
    footer.textContent = node.status || "—";

    element.append(code, name, footer);
}

function createLegacySystemsBreakdownDiagramNode(node) {
    const element = document.createElement("article");
    element.className = `systemsbreakdown-diagram-node${node.type === "project" ? " is-project-node" : ""}`;
    element.style.left = `${node.x}px`;
    element.style.top = `${node.y}px`;
    element.style.width = `${node.width}px`;
    element.style.minHeight = `${node.height}px`;
    element.setAttribute("data-node-type", node.type);

    if (node.system) {
        element.setAttribute("data-entity-id", node.system.entityId || node.system.id);
    }

    element.innerHTML = `
        <div class="systemsbreakdown-diagram-node-head">
            <div class="systemsbreakdown-diagram-node-code">${escapeHtml(node.code)}</div>
            <div class="systemsbreakdown-diagram-node-title">${escapeHtml(node.name)}</div>
        </div>
        <div class="systemsbreakdown-diagram-node-body">${escapeHtml(node.description || "")}</div>
        ${node.type === "system" ? renderStatusBar(node.system) : ""}
    `;

    if (node.type === "system") {
        element.addEventListener("dblclick", () => {
            openEditSystem(node.system);
        });

        element.addEventListener("contextmenu", (event) => {
            event.preventDefault();
            event.stopPropagation();
            openContextMenu(event.clientX, event.clientY, "system", node.system);
        });
    } else {
        element.addEventListener("contextmenu", (event) => {
            event.preventDefault();
            event.stopPropagation();
            openContextMenu(event.clientX, event.clientY, "project", null);
        });
    }

    return element;
}

function buildSystemTooltip(system) {
    if (!system) {
        return "";
    }

    return [
        `ID: ${system.id || "—"}`,
        `Name: ${system.name || "—"}`,
        system.description ? `Description: ${system.description}` : "",
        `Status: ${system.status || "—"}`
    ].filter(Boolean).join("\n");
}

function renderStatusBar(system) {
    return `
        <div class="systemsbreakdown-diagram-status-bars">
            <div class="systemsbreakdown-diagram-status-bar system-status ${statusClass(system.status)}">
                <span class="systemsbreakdown-diagram-status-label">Status</span>
                <span class="systemsbreakdown-diagram-status-value">${escapeHtml(system.status || "—")}</span>
            </div>
        </div>
    `;
}

function statusClass(value) {
    const normalized = normalizeCssName(value);

    if (!normalized) {
        return "";
    }

    return `status-${normalized}`;
}

function normalizeCssName(value) {
    return sanitizeClassPart(String(value || "").replace(/&/g, "and"));
}

function getStatusClass(status) {
    const normalized = normalizeStatus(status);

    if (normalized === "new") return "status-new";
    if (normalized === "changed") return "status-changed";
    if (normalized === "validated") return "status-validated";
    if (normalized === "approved") return "status-approved";
    if (normalized === "deprecated") return "status-deprecated";
    if (normalized === "potential duplicate") return "status-potential-duplicate";
    if (normalized === "incomplete") return "status-incomplete";
    if (normalized === "sample") return "status-sample";
    if (normalized === "out of scope") return "status-out-of-scope";
    if (normalized === "active") return "status-approved";
    if (normalized === "inactive") return "status-sample";

    return "status-unknown";
}

function normalizeStatus(status) {
    return String(status || "")
        .trim()
        .replace(/\s+/g, " ")
        .toLowerCase();
}

/*
 * KOPIERET FRA basissystemrequirementdiagramV2-page.js:
 * Da systemsbreakdown-main.html ikke loader basissystemrequirementdiagramV2.css,
 * injiceres de nødvendige V2-styles her, så vertical diagrammet får identiske kasser og streger.
 */
function injectCopiedV2DiagramStyles() {
    if (document.getElementById("systemsbreakdownCopiedV2DiagramStyles")) {
        return;
    }

    const style = document.createElement("style");
    style.id = "systemsbreakdownCopiedV2DiagramStyles";
    style.textContent = `
        .systemsbreakdown-vertical-diagram-canvas .systemrequirementdiagram-node {
            position: absolute;
            border: 1px solid #565f73;
            border-radius: 0;
            background: #3f3f46;
            color: #ffffff;
            box-sizing: border-box;
            cursor: pointer;
            display: flex;
            flex-direction: column;
            overflow: hidden;
            padding: 0;
            text-align: center;
            box-shadow: 0 12px 24px rgba(15, 23, 42, 0.18);
            font-family: inherit;
        }

        .systemsbreakdown-vertical-diagram-canvas .systemrequirementdiagram-project-node {
            background: #4b5563;
            border-color: #7c8798;
        }

        .systemsbreakdown-vertical-diagram-canvas .systemrequirementdiagram-node-code {
            display: block;
            min-height: 20px;
            padding: 7px 8px 2px;
            color: #e5e7eb;
            font-size: 12px;
            font-weight: 800;
            line-height: 1.1;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }

        .systemsbreakdown-vertical-diagram-canvas .systemrequirementdiagram-node-name {
            display: flex;
            align-items: center;
            justify-content: center;
            flex: 1;
            padding: 4px 10px;
            color: #ffffff;
            font-size: 13px;
            font-weight: 800;
            line-height: 1.16;
            overflow: hidden;
            word-break: break-word;
        }

        .systemsbreakdown-vertical-diagram-canvas .systemrequirementdiagram-node-footer {
            display: flex;
            align-items: center;
            justify-content: center;
            min-height: 18px;
            padding: 2px 7px;
            color: #ffffff;
            background: #475569;
            font-size: 11px;
            font-weight: 800;
            line-height: 1.1;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }

        .systemsbreakdown-vertical-diagram-canvas .systemrequirementdiagram-project-node .systemrequirementdiagram-node-footer {
            background: #6d28d9;
        }

        .systemsbreakdown-vertical-diagram-canvas .systemrequirementdiagram-requirement-node.status-new .systemrequirementdiagram-node-footer {
            background: #2563eb;
        }

        .systemsbreakdown-vertical-diagram-canvas .systemrequirementdiagram-requirement-node.status-changed .systemrequirementdiagram-node-footer {
            background: #f97316;
        }

        .systemsbreakdown-vertical-diagram-canvas .systemrequirementdiagram-requirement-node.status-validated .systemrequirementdiagram-node-footer {
            background: #0891b2;
        }

        .systemsbreakdown-vertical-diagram-canvas .systemrequirementdiagram-requirement-node.status-approved .systemrequirementdiagram-node-footer {
            background: #16a34a;
        }

        .systemsbreakdown-vertical-diagram-canvas .systemrequirementdiagram-requirement-node.status-deprecated .systemrequirementdiagram-node-footer {
            background: #7f1d1d;
        }

        .systemsbreakdown-vertical-diagram-canvas .systemrequirementdiagram-requirement-node.status-potential-duplicate .systemrequirementdiagram-node-footer {
            background: #9333ea;
        }

        .systemsbreakdown-vertical-diagram-canvas .systemrequirementdiagram-requirement-node.status-incomplete .systemrequirementdiagram-node-footer {
            background: #dc2626;
        }

        .systemsbreakdown-vertical-diagram-canvas .systemrequirementdiagram-requirement-node.status-sample .systemrequirementdiagram-node-footer {
            background: #64748b;
        }

        .systemsbreakdown-vertical-diagram-canvas .systemrequirementdiagram-requirement-node.status-out-of-scope .systemrequirementdiagram-node-footer {
            background: #a16207;
        }

        .systemsbreakdown-vertical-diagram-canvas .systemrequirementdiagram-requirement-node.status-unknown .systemrequirementdiagram-node-footer {
            background: #475569;
        }

        .systemsbreakdown-v2-copied-connection {
            fill: none;
            stroke: #aeb8c8;
            stroke-width: 1.4;
            stroke-linecap: square;
            stroke-linejoin: miter;
        }
    `;

    document.head.appendChild(style);
}

/* ------------------------------------------------------------------ */
/* Context menu and navigation                                         */
/* ------------------------------------------------------------------ */

function initializeContextMenuEvents() {
    const menu = document.getElementById("systemsBreakdownContextMenu");

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
    const system = state.contextSystem;

    closeContextMenu();

    if (action === "edit-system") {
        if (!system) {
            window.alert("No System selected.");
            return;
        }

        openEditSystem(system);
        return;
    }

    if (action === "create-sub-system") {
        if (!system) {
            window.alert("No System selected.");
            return;
        }

        openCreateSubSystem(system);
        return;
    }

    if (action === "create-root-system") {
        openCreateRootSystem();
    }
}

function openContextMenu(x, y, targetType, system) {
    const menu = document.getElementById("systemsBreakdownContextMenu");
    const editButton = document.getElementById("contextMenuEditSystem");
    const createSubButton = document.getElementById("contextMenuCreateSubSystem");
    const createRootButton = document.getElementById("contextMenuCreateRootSystem");

    if (!menu) {
        return;
    }

    state.contextTargetType = targetType;
    state.contextSystem = system || null;

    const isSystem = targetType === "system";
    const isProject = targetType === "project";

    if (editButton) {
        editButton.hidden = !isSystem;
        editButton.toggleAttribute("hidden", !isSystem);
    }

    if (createSubButton) {
        createSubButton.hidden = !isSystem;
        createSubButton.toggleAttribute("hidden", !isSystem);
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
    const menu = document.getElementById("systemsBreakdownContextMenu");

    if (!menu) {
        return;
    }

    menu.classList.remove("is-open");
    menu.setAttribute("aria-hidden", "true");
    state.contextTargetType = "";
    state.contextSystem = null;
}

function getSystemNavigationId(system) {
    return system?.entityId || system?.id || "";
}

function openEditSystem(system) {
    const id = getSystemNavigationId(system);

    if (!id) {
        window.alert("System has no entity id.");
        return;
    }

    const url = buildEditPageUrl({
        mode: "edit",
        id
    });

    window.location.href = url;
}

function openCreateSubSystem(system) {
    const id = getSystemNavigationId(system);

    if (!id) {
        window.alert("System has no entity id.");
        return;
    }

    const url = buildEditPageUrl({
        mode: "create-child",
        id
    });

    window.location.href = url;
}

function openCreateRootSystem() {
    const url = buildEditPageUrl({
        mode: "create-root"
    });

    window.location.href = url;
}

function buildEditPageUrl(params) {
    const url = new URL(EDIT_PAGE_URL, window.location.href);

    url.searchParams.set("mode", params.mode);

    if (params.id) {
        url.searchParams.set("id", params.id);
    }

    url.searchParams.set("returnUrl", "/web/view?page=systemsbreakdown-main");

    return url.toString();
}

function downloadCurrentDiagramPdf() {
    if (state.selectedView === VIEW_TYPES.list || state.filteredSystems.length === 0) {
        return;
    }

    const orientation = state.selectedView === VIEW_TYPES.horizontal ? "horizontal" : "vertical";
    const tree = buildVisibleTree();
    const layout = layoutTree(tree, orientation);

    downloadSystemsBreakdownDiagramPdf({
        tree,
        layout,
        orientation,
        topPanel: state.topPanel,
        systemCount: state.filteredSystems.length
    });
}

/* ------------------------------------------------------------------ */
/* Empty states and utilities                                          */
/* ------------------------------------------------------------------ */

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

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#039;");
}

function debounce(fn, delay) {
    let timeoutId = null;

    return (...args) => {
        window.clearTimeout(timeoutId);
        timeoutId = window.setTimeout(() => fn(...args), delay);
    };
}
