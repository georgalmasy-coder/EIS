import { initMenu } from "../components/menu.js";
import { initHelpDialog } from "../components/help-dialog.js";
import { initTabs } from "../components/tabs.js";
import { createNotesTable } from "../components/notes-table.js";
import { createAttachmentsTable } from "../components/attachments-table.js";
import { setText } from "../core/dom.js";
import {
    getDirectChild,
    getDirectText,
    hasXmlParseError,
    serializeXml
} from "../core/xml.js";
import {
    fieldEditable,
    fieldRequired,
    fieldVisible
} from "../core/field-display.js";
import { escapeHtml } from "../core/html.js";
import { isTruthy } from "../core/utils.js";
import {
    focusFirstInvalidField,
    validateFieldsFromDetailNode
} from "../core/validation.js";

const SAVE_URL = "/project?cmd=save";
const DEFAULT_RETURN_URL = "/web/view?page=myprojects";

const STORAGE_KEYS = {
    tableColumnWidths: "project.edit.tableColumnWidths"
};

const RESIZABLE_TABLES = [
    { tableSelector: ".attachments-table", storageKey: "attachments", defaultMinWidth: 760 },
    { tableSelector: ".notes-table", storageKey: "notes", defaultMinWidth: 640 }
];

const MODES = {
    edit: "edit",
    editVersion: "edit-version",
    create: "create"
};

const notesTable = createNotesTable({
    onAfterRender: initializeResizableEditTables
});

const attachmentsTable = createAttachmentsTable({
    onAfterRender: initializeResizableEditTables
});

const state = {
    mode: MODES.edit,
    id: "",
    version: "",
    returnUrl: DEFAULT_RETURN_URL,
    readOnly: false,
    currentDoc: null,
    detailNode: null,
    topPanel: {
        customerName: "—",
        projectName: "—",
        userName: "—"
    }
};

document.addEventListener("DOMContentLoaded", () => {
    start();
});

function start() {
    initializeShell();
    initializeTabs();
    initializeRouteState();
    initializeEvents();
    applyModeUi();
    loadDetail();
}

function initializeShell() {
    setText("customerName", "—");
    setText("projectName", "—");
    setText("userName", "—");
    setText("loadStatus", "Loading");

    initMenu(document);
    initHelpDialog();
}

function initializeTabs() {
    initTabs([
        { btnId: "tabBtn1", panelId: "tabPanel1" },
        { btnId: "tabBtn2", panelId: "tabPanel2" },
        { btnId: "tabBtn3", panelId: "tabPanel3" }
    ]);

    normalizeStakeholderStyleTabs();
}

function normalizeStakeholderStyleTabs() {
    document.querySelectorAll(".page-tab-btn").forEach((button) => {
        button.addEventListener("click", () => {
            document.querySelectorAll(".page-tab-btn").forEach((tabButton) => {
                const isSelected = tabButton === button;

                tabButton.classList.toggle("is-active", isSelected);
                tabButton.classList.toggle("active", isSelected);
                tabButton.setAttribute("aria-selected", String(isSelected));
            });

            document.querySelectorAll(".tab-panel").forEach((panel) => {
                const isSelected = panel.id === button.getAttribute("aria-controls");

                panel.classList.toggle("is-active", isSelected);
                panel.classList.toggle("active", isSelected);
            });
        });
    });
}

function initializeRouteState() {
    const params = new URLSearchParams(window.location.search);
    const requestedMode = params.get("mode") || MODES.edit;

    state.mode = Object.values(MODES).includes(requestedMode) ? requestedMode : MODES.edit;
    state.id = params.get("id") || "";
    state.version = params.get("version") || "";
    state.returnUrl = params.get("returnUrl") || DEFAULT_RETURN_URL;
    state.readOnly = state.mode === MODES.editVersion && !!state.version;

    notesTable.setReadOnly(state.readOnly, { render: false });
    attachmentsTable.setReadOnly(state.readOnly, { render: false });
}

function initializeEvents() {
    document.getElementById("btnCancel")?.addEventListener("click", () => {
        returnToPreviousPage();
    });

    document.getElementById("btnSave")?.addEventListener("click", async () => {
        await saveCurrentProject();
    });

    notesTable.bind({
        readOnly: state.readOnly
    });

    attachmentsTable.bind({
        readOnly: state.readOnly
    });
}

async function loadDetail() {
    const detailUrl = buildDetailUrl();

    if (!detailUrl) {
        setText("loadStatus", "Error");
        setText("dlgStatus", "Could not determine detail URL.");
        return;
    }

    setText("loadStatus", "Loading");
    setText("dlgStatus", "Loading project details…");

    try {
        const response = await fetch(detailUrl, {
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
            throw new Error("The project endpoint returned invalid XML.");
        }

        state.currentDoc = xmlDocument;
        state.detailNode = findDetailNode(xmlDocument);
        state.topPanel = parseTopPanel(xmlDocument);

        applyTopPanel();
        renderAllFromDoc(xmlDocument);
        applyModeUi();
        initializeResizableEditTables();

        setText("loadStatus", "Loaded");
        setText("dlgStatus", "Loaded.");
    } catch (error) {
        console.error("Failed to load project detail", error);
        setText("loadStatus", "Error");
        setText("dlgStatus", `Could not load project detail. ${error.message}`);
    }
}

function buildDetailUrl() {
    if (state.mode === MODES.edit) {
        if (!state.id) return "";
        return `/project?cmd=edit&id=${encodeURIComponent(state.id)}`;
    }

    if (state.mode === MODES.editVersion) {
        if (!state.id || !state.version) return "";
        return `/project?cmd=edit&id=${encodeURIComponent(state.id)}&version=${encodeURIComponent(state.version)}`;
    }

    if (state.mode === MODES.create) {
        return "/project?cmd=create";
    }

    return "";
}

function applyModeUi() {
    const saveButton = document.getElementById("btnSave");
    const readOnlyBanner = document.getElementById("readOnlyBanner");

    document.body.classList.toggle("project-edit-readonly", state.readOnly);

    notesTable.setReadOnly(state.readOnly);
    attachmentsTable.setReadOnly(state.readOnly);

    if (saveButton) {
        saveButton.disabled = state.readOnly;
        saveButton.title = state.readOnly ? "Save is disabled for historical versions." : "";
    }

    if (readOnlyBanner) {
        readOnlyBanner.hidden = !state.readOnly;
    }

    setText("pageModeLabel", getModeLabel());
    setText("entityMeta", getEntityMetaLabel());

    if (state.readOnly) {
        setFormFieldsReadOnly();
    }
}

function getModeLabel() {
    if (state.mode === MODES.editVersion) return buildHistoricalVersionLabel();
    if (state.mode === MODES.create) return "Create Project";
    return "Edit Project";
}

function buildHistoricalVersionLabel() {
    return state.version
        ? `Historical version ${state.version}`
        : "Historical version";
}

function getEntityMetaLabel() {
    if (state.mode === MODES.create) return "New project";
    if (state.mode === MODES.editVersion) return `Project ID: ${state.id || "—"} · Version: ${state.version || "—"}`;
    return `Project ID: ${state.id || "—"}`;
}

function setFormFieldsReadOnly() {
    document.querySelectorAll("#basisInfoFields input, #basisInfoFields textarea").forEach((field) => {
        if (field.type === "checkbox") {
            field.disabled = true;
        } else {
            field.readOnly = true;
        }
    });

    document.querySelectorAll("#basisInfoFields select").forEach((field) => {
        field.disabled = true;
    });
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
    setText("customerName", state.topPanel.customerName);
    setText("projectName", state.topPanel.projectName);
    setText("userName", state.topPanel.userName);
}

function renderAllFromDoc(doc) {
    renderBasisInfoFromDoc(doc);
    notesTable.loadFromDocument(doc);
    attachmentsTable.loadFromDocument(doc);
}

function findDetailNode(root) {
    return root?.querySelector("projectDocument > project")
        || root?.querySelector("projectDocument project")
        || root?.querySelector("project")
        || null;
}

function renderBasisInfoFromDoc(doc) {
    const detailNode = findDetailNode(doc);
    const basisInfoFields = document.getElementById("basisInfoFields");

    if (!basisInfoFields) {
        return;
    }

    if (!detailNode) {
        basisInfoFields.innerHTML = '<div class="page-empty">No detail XML returned.</div>';
        return;
    }

    basisInfoFields.innerHTML = Array.from(detailNode.children || [])
        .map(renderBasisInfoFieldMarkup)
        .join("");

    if (state.readOnly) {
        setFormFieldsReadOnly();
    }
}

function renderBasisInfoFieldMarkup(field) {
    const name = field.tagName;
    const label = field.getAttribute("header") || field.getAttribute("label") || name;
    const control = (field.getAttribute("control") || "").toLowerCase();
    const editable = fieldEditable(field);
    const visible = fieldVisible(field);
    const required = fieldRequired(field);
    const value = getFieldUiValue(field);

    if (!visible) {
        return "";
    }

    const forceReadOnly = state.readOnly;
    const readonlyAttr = editable && !forceReadOnly ? "" : "readonly";
    const disabledAttr = editable && !forceReadOnly ? "" : "disabled";
    const requiredStar = required ? '<span class="field-required" aria-hidden="true">*</span>' : "";
    const requiredAttr = required ? "required" : "";
    const escapedName = escapeHtml(name);
    const escapedLabel = escapeHtml(label);

    if (control === "hidden") {
        return `<input type="hidden" data-field="${escapedName}" id="fld-${escapedName}" value="${escapeHtml(value)}">`;
    }

    if (control === "checkbox") {
        const checked = isTruthy(value) ? "checked" : "";

        return `
            <div class="page-field checkbox-field">
                <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
                <input id="fld-${escapedName}" data-field="${escapedName}" type="checkbox" ${checked} ${disabledAttr} ${requiredAttr} />
            </div>
        `;
    }

    if (control === "datetime") {
        const normalized = normalizeDateTimeForInput(value);

        return `
            <div class="page-field">
                <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
                <input id="fld-${escapedName}" data-field="${escapedName}" type="datetime-local" value="${escapeHtml(normalized)}" ${readonlyAttr} ${requiredAttr} />
            </div>
        `;
    }

    if (control === "date") {
        const normalized = normalizeDateForInput(value);

        return `
            <div class="page-field">
                <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
                <input id="fld-${escapedName}" data-field="${escapedName}" type="date" value="${escapeHtml(normalized)}" ${readonlyAttr} ${requiredAttr} />
            </div>
        `;
    }

    if (control === "textarea" || name === "Description") {
        return `
            <div class="page-field description-field">
                <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
                <textarea id="fld-${escapedName}" data-field="${escapedName}" ${readonlyAttr} ${requiredAttr}>${escapeHtml(value)}</textarea>
            </div>
        `;
    }

    if (control === "select") {
        const selectedValue = (field.getElementsByTagName("Value")?.[0]?.textContent || "").trim();
        const options = Array.from(field.getElementsByTagName("Option"));

        return `
            <div class="page-field">
                <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
                <select id="fld-${escapedName}" data-field="${escapedName}" ${disabledAttr} ${requiredAttr}>
                    ${options.map((option) => {
            const optionValue = option.getAttribute("value") || "";
            const optionLabel = (option.textContent || "").trim();
            const selected = optionValue === selectedValue ? "selected" : "";

            return `<option value="${escapeHtml(optionValue)}" ${selected}>${escapeHtml(optionLabel)}</option>`;
        }).join("")}
                </select>
            </div>
        `;
    }

    return `
        <div class="page-field">
            <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
            <input id="fld-${escapedName}" data-field="${escapedName}" type="text" value="${escapeHtml(value)}" ${readonlyAttr} ${requiredAttr} />
        </div>
    `;
}

function getFieldUiValue(field) {
    const control = (field.getAttribute("control") || "").toLowerCase();

    if (control === "select") {
        return (field.getElementsByTagName("Value")?.[0]?.textContent || "").trim();
    }

    const valueNode = getDirectChild(field, "Value");

    if (valueNode) {
        return valueNode.textContent || "";
    }

    return getDirectText(field);
}

function normalizeDateForInput(value) {
    const text = String(value || "").trim();

    if (!text) {
        return "";
    }

    if (/^\d{4}-\d{2}-\d{2}/.test(text)) {
        return text.substring(0, 10);
    }

    if (/^\d{8}$/.test(text)) {
        const day = text.substring(0, 2);
        const month = text.substring(2, 4);
        const year = text.substring(4, 8);

        return `${year}-${month}-${day}`;
    }

    return text.substring(0, 10);
}

function normalizeDateTimeForInput(value) {
    const text = String(value || "").trim();

    if (!text) {
        return "";
    }

    return text.replace(" ", "T").substring(0, 16);
}

async function saveCurrentProject() {
    if (state.readOnly) {
        return;
    }

    const validationErrors = validateCurrentProject();

    if (validationErrors.length) {
        setText("dlgStatus", "Validation failed.");
        setText("loadStatus", "Validation error");
        window.alert(validationErrors.join("\n"));
        focusFirstInvalidField(document.getElementById("basisInfoFields") || document);
        return;
    }

    try {
        setText("dlgStatus", "Saving…");
        setText("loadStatus", "Saving");

        const payload = buildSavePayload();

        const response = await fetch(SAVE_URL, {
            method: "POST",
            headers: {
                "Content-Type": "application/xml; charset=UTF-8",
                "Accept": "application/xml,text/xml,*/*"
            },
            body: payload,
            cache: "no-store"
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status} ${response.statusText}`);
        }

        setText("dlgStatus", "Saved.");
        setText("loadStatus", "Saved");

        returnToPreviousPage();
    } catch (error) {
        console.error("Failed to save project", error);
        setText("dlgStatus", `Save failed. ${error.message}`);
        setText("loadStatus", "Error");
    }
}

function validateCurrentProject() {
    const detailNode = state.detailNode || findDetailNode(state.currentDoc);
    const basisInfoFields = document.getElementById("basisInfoFields");

    return validateFieldsFromDetailNode(detailNode, basisInfoFields);
}

function buildSavePayload() {
    const currentDoc = state.currentDoc;

    if (!currentDoc || !currentDoc.documentElement) {
        throw new Error("No XML document loaded.");
    }

    const updatedDoc = currentDoc.cloneNode(true);
    const root = updatedDoc.documentElement;

    const detailContainer = getDirectChild(root, "projectDocument");
    const detailNode = detailContainer
        ? getDirectChild(detailContainer, "project")
        : getDirectChild(root, "project");

    if (!detailNode) {
        throw new Error("No detail XML returned.");
    }

    const basisInfoFields = document.getElementById("basisInfoFields");
    const fields = Array.from(basisInfoFields?.querySelectorAll("[data-field]") || []);

    fields.forEach((uiField) => {
        const name = uiField.getAttribute("data-field");

        if (!name) {
            return;
        }

        const child = ensureChild(updatedDoc, detailNode, name);
        const control = (child.getAttribute("control") || "").toLowerCase();

        if (control === "select") {
            const value = uiField.selectedOptions?.[0]?.value?.trim() || "";
            let valueNode = child.getElementsByTagName("Value")?.[0] || null;

            if (!valueNode) {
                valueNode = updatedDoc.createElement("Value");
                child.insertBefore(valueNode, child.firstChild);
            }

            valueNode.textContent = value;

            Array.from(child.getElementsByTagName("Option")).forEach((option) => {
                if (option.getAttribute("selected") != null) {
                    option.removeAttribute("selected");
                }

                if ((option.getAttribute("value") || "").trim() === value) {
                    option.setAttribute("selected", "true");
                }
            });

            return;
        }

        if (uiField.type === "checkbox") {
            child.textContent = uiField.checked ? "true" : "false";
        } else {
            child.textContent = uiField.value ?? "";
        }
    });

    notesTable.writeToDocument(updatedDoc);
    attachmentsTable.writeToDocument(updatedDoc);

    return serializeXml(updatedDoc);
}

function initializeResizableEditTables() {
    RESIZABLE_TABLES.forEach((config) => {
        const table = document.querySelector(config.tableSelector);

        if (!table) {
            return;
        }

        initializeResizableEditTable(table, config);
    });
}

function initializeResizableEditTable(table, config) {
    const headerCells = Array.from(table.querySelectorAll("thead th"));

    if (!headerCells.length) {
        return;
    }

    ensureTableColGroup(table, headerCells.length);
    applyStoredTableColumnWidths(table, config.storageKey, config.defaultMinWidth);

    headerCells.forEach((headerCell, columnIndex) => {
        headerCell.classList.add("project-edit-resizable-th");

        if (headerCell.querySelector(":scope > .project-edit-column-resizer")) {
            return;
        }

        const handle = document.createElement("span");
        handle.className = "project-edit-column-resizer";
        handle.setAttribute("aria-hidden", "true");

        handle.addEventListener("click", (event) => {
            event.preventDefault();
            event.stopPropagation();
        });

        handle.addEventListener("mousedown", (event) => {
            event.preventDefault();
            event.stopPropagation();

            startEditTableColumnResize(event, table, config, columnIndex, headerCell);
        });

        headerCell.appendChild(handle);
    });
}

function ensureTableColGroup(table, columnCount) {
    let colGroup = table.querySelector(":scope > colgroup");

    if (!colGroup) {
        colGroup = document.createElement("colgroup");
        table.insertBefore(colGroup, table.firstChild);
    }

    while (colGroup.children.length < columnCount) {
        colGroup.appendChild(document.createElement("col"));
    }

    while (colGroup.children.length > columnCount) {
        colGroup.removeChild(colGroup.lastElementChild);
    }
}

function startEditTableColumnResize(event, table, config, columnIndex, headerCell) {
    const startX = event.clientX;
    const startWidth = headerCell.getBoundingClientRect().width;

    document.body.classList.add("project-edit-column-resizing");

    function onMouseMove(moveEvent) {
        const delta = moveEvent.clientX - startX;
        const nextWidth = Math.max(50, startWidth + delta);

        updateEditTableColumnWidth(table, config, columnIndex, nextWidth);
    }

    function onMouseUp(upEvent) {
        const delta = upEvent.clientX - startX;
        const nextWidth = Math.max(50, startWidth + delta);

        updateEditTableColumnWidth(table, config, columnIndex, nextWidth);
        persistEditTableColumnWidth(config.storageKey, columnIndex, nextWidth);

        document.body.classList.remove("project-edit-column-resizing");
        window.removeEventListener("mousemove", onMouseMove);
        window.removeEventListener("mouseup", onMouseUp);
    }

    window.addEventListener("mousemove", onMouseMove);
    window.addEventListener("mouseup", onMouseUp);
}

function updateEditTableColumnWidth(table, config, columnIndex, widthPx) {
    const width = `${Math.max(50, Math.round(widthPx))}px`;
    const colGroup = table.querySelector(":scope > colgroup");
    const col = colGroup?.children?.[columnIndex];

    if (col) {
        col.style.width = width;
    }

    updateEditTableMinWidth(table, config.defaultMinWidth);
}

function applyStoredTableColumnWidths(table, tableKey, defaultMinWidth) {
    const storedWidths = getStoredEditTableColumnWidths();
    const tableWidths = storedWidths[tableKey] || {};
    const colGroup = table.querySelector(":scope > colgroup");

    if (!colGroup) {
        return;
    }

    Array.from(colGroup.children).forEach((col, index) => {
        const storedWidth = tableWidths[String(index)];

        if (storedWidth) {
            col.style.width = storedWidth;
        }
    });

    updateEditTableMinWidth(table, defaultMinWidth);
}

function updateEditTableMinWidth(table, defaultMinWidth) {
    const colGroup = table.querySelector(":scope > colgroup");
    const widths = Array.from(colGroup?.children || []).map((col) => widthToPixels(col.style.width, 0));
    const totalWidth = widths.reduce((sum, width) => sum + width, 0);

    if (totalWidth > 0) {
        table.style.minWidth = `${Math.max(totalWidth, defaultMinWidth)}px`;
    } else {
        table.style.minWidth = `${defaultMinWidth}px`;
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

        return Number.isFinite(parsed) ? Math.max(50, parsed * 10) : fallback;
    }

    return fallback;
}

function getStoredEditTableColumnWidths() {
    try {
        const raw = localStorage.getItem(STORAGE_KEYS.tableColumnWidths);

        if (!raw) {
            return {};
        }

        const parsed = JSON.parse(raw);

        return parsed && typeof parsed === "object" ? parsed : {};
    } catch {
        return {};
    }
}

function persistEditTableColumnWidth(tableKey, columnIndex, widthPx) {
    const widths = getStoredEditTableColumnWidths();

    if (!widths[tableKey] || typeof widths[tableKey] !== "object") {
        widths[tableKey] = {};
    }

    widths[tableKey][String(columnIndex)] = `${Math.max(50, Math.round(widthPx))}px`;

    localStorage.setItem(STORAGE_KEYS.tableColumnWidths, JSON.stringify(widths));
}

function returnToPreviousPage() {
    window.location.href = state.returnUrl || DEFAULT_RETURN_URL;
}

function ensureChild(doc, parent, tagName) {
    let child = getDirectChild(parent, tagName);

    if (!child) {
        child = doc.createElement(tagName);
        parent.appendChild(child);
    }

    return child;
}

function getChildText(parent, tagName, fallback = "") {
    const element = parent?.getElementsByTagName(tagName)?.[0];
    const value = element?.textContent?.trim();

    return value || fallback;
}