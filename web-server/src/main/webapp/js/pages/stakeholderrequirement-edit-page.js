import { initMenu } from "../components/menu.js";
import { initHelpDialog } from "../components/help-dialog.js";
import { initTabs } from "../components/tabs.js";
import { applyTopbarMetadata } from "../components/topbar.js";
import { createHistoryTable } from "../components/history-table.js";
import { createNotesTable } from "../components/notes-table.js";
import { createAttachmentsTable } from "../components/attachments-table.js";
import { createLinksTable } from "../components/links-table.js";
import { createEntityRelationsTable } from "../components/entity-relations-table.js";
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

const SAVE_URL = "/basis/stakeholderrequirement?cmd=save";
const EDIT_PAGE_URL = "/web/view?page=stakeholderrequirement-edit";
const DEFAULT_RETURN_URL = "/web/view?page=stakeholderrequirement-main";
const RELATION_LIST_URL = "/basis/stakeholderrequirement/relationlist";
const RELATION_ENTITY_TYPE_ID = 5;

const STORAGE_KEYS = {
    tableColumnWidths: "basis.stakeholderrequirement.edit.tableColumnWidths"
};

const RESIZABLE_TABLES = [
    { tableSelector: ".history-table", storageKey: "history", defaultMinWidth: 520 },
    { tableSelector: ".attachments-table", storageKey: "attachments", defaultMinWidth: 760 },
    { tableSelector: ".notes-table", storageKey: "notes", defaultMinWidth: 640 },
    { tableSelector: ".relations-table", storageKey: "relations", defaultMinWidth: 760 }
];

const MODES = {
    edit: "edit",
    editVersion: "edit-version",
    createChild: "create-child",
    createRoot: "create-root"
};

const historyTable = createHistoryTable({
    editPageUrl: EDIT_PAGE_URL,
    defaultReturnUrl: DEFAULT_RETURN_URL,
    onAfterRender: initializeResizableEditTables
});

const notesTable = createNotesTable({
    onAfterRender: initializeResizableEditTables
});

const attachmentsTable = createAttachmentsTable({
    onAfterRender: initializeResizableEditTables
});

const linksTable = createLinksTable();

const relationsTable = createEntityRelationsTable({
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
        { btnId: "tabBtn3", panelId: "tabPanel3" },
        { btnId: "tabBtn4", panelId: "tabPanel4" },
        { btnId: "tabBtn5", panelId: "tabPanel5" },
        { btnId: "tabBtn6", panelId: "tabPanel6" }
    ]);
}

function initializeRouteState() {
    const params = new URLSearchParams(window.location.search);
    const requestedMode = params.get("mode") || MODES.edit;

    state.mode = Object.values(MODES).includes(requestedMode) ? requestedMode : MODES.edit;
    state.id = params.get("id") || "";
    state.version = params.get("version") || "";
    state.returnUrl = params.get("returnUrl") || DEFAULT_RETURN_URL;
    state.readOnly = state.mode === MODES.editVersion && !!state.version;

    historyTable.setContext({
        id: state.id,
        returnUrl: buildCurrentEditReturnUrl(),
        readOnly: state.readOnly
    }, {
        render: false
    });

    notesTable.setReadOnly(state.readOnly, { render: false });
    attachmentsTable.setReadOnly(state.readOnly, { render: false });
    linksTable.setReadOnly(state.readOnly, { render: false });
    relationsTable.setReadOnly(state.readOnly, { render: false });
    relationsTable.setEntityContext({
        entityTypeId: RELATION_ENTITY_TYPE_ID,
        entityId: state.id
    }, { render: false });
}

function initializeEvents() {
    document.getElementById("btnCancel")?.addEventListener("click", () => {
        returnToPreviousPage();
    });

    document.getElementById("btnSave")?.addEventListener("click", async () => {
        await saveCurrentRequirement();
    });

    historyTable.bind({
        id: state.id,
        returnUrl: buildCurrentEditReturnUrl(),
        readOnly: state.readOnly
    });

    notesTable.bind({
        readOnly: state.readOnly
    });

    attachmentsTable.bind({
        readOnly: state.readOnly
    });

    linksTable.bind({
        readOnly: state.readOnly
    });

    relationsTable.bind({
        readOnly: state.readOnly,
        relationRequestUrl: RELATION_LIST_URL
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
    setText("dlgStatus", "Loading stakeholder requirement details…");

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
            throw new Error("The stakeholder requirement endpoint returned invalid XML.");
        }

        state.currentDoc = xmlDocument;
        state.detailNode = findDetailNode(xmlDocument);
        state.topPanel = parseTopPanel(xmlDocument);

        const entityId = state.id || getEntityIdFromCurrentDoc();

        historyTable.setContext({
            id: entityId,
            returnUrl: buildCurrentEditReturnUrl(entityId),
            readOnly: state.readOnly
        }, {
            render: false
        });

        relationsTable.setEntityContext({
            entityTypeId: RELATION_ENTITY_TYPE_ID,
            entityId
        }, { render: false });

        applyTopPanel();
        renderAllFromDoc(xmlDocument);
        applyModeUi();
        initializeResizableEditTables();

        setText("loadStatus", "Loaded");
        setText("dlgStatus", "Loaded.");
    } catch (error) {
        console.error("Failed to load stakeholder requirement detail", error);
        setText("loadStatus", "Error");
        setText("dlgStatus", `Could not load stakeholder requirement detail. ${error.message}`);
    }
}

function buildDetailUrl() {
    if (state.mode === MODES.edit) {
        if (!state.id) return "";
        return `/basis/stakeholderrequirement?cmd=edit&id=${encodeURIComponent(state.id)}`;
    }

    if (state.mode === MODES.editVersion) {
        if (!state.id || !state.version) return "";
        return `/basis/stakeholderrequirement?cmd=edit&id=${encodeURIComponent(state.id)}&version=${encodeURIComponent(state.version)}`;
    }

    if (state.mode === MODES.createChild) {
        if (!state.id) return "";
        return `/basis/stakeholderrequirement?cmd=create&id=${encodeURIComponent(state.id)}`;
    }

    if (state.mode === MODES.createRoot) {
        return "/basis/stakeholderrequirement?cmd=create";
    }

    return "";
}

function buildCurrentEditReturnUrl(entityId = "") {
    const id = entityId || state.id || getEntityIdFromCurrentDoc();
    const url = new URL(EDIT_PAGE_URL, window.location.href);

    url.searchParams.set("mode", MODES.edit);

    if (id) {
        url.searchParams.set("id", id);
    }

    url.searchParams.set("returnUrl", state.returnUrl || DEFAULT_RETURN_URL);

    return url.toString();
}

function applyModeUi() {
    const saveButton = document.getElementById("btnSave");
    const readOnlyBanner = document.getElementById("readOnlyBanner");

    document.body.classList.toggle("stakeholderrequirement-edit-readonly", state.readOnly);

    historyTable.setReadOnly(state.readOnly);
    notesTable.setReadOnly(state.readOnly);
    attachmentsTable.setReadOnly(state.readOnly);
    linksTable.setReadOnly(state.readOnly);
    relationsTable.setReadOnly(state.readOnly);

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
    if (state.mode === MODES.createChild) return "Create Sub Stakeholder Requirement";
    if (state.mode === MODES.createRoot) return "Create Root Stakeholder Requirement";
    return "Edit Stakeholder Requirement";
}

function buildHistoricalVersionLabel() {
    return state.version
        ? `Historical version ${state.version}`
        : "Historical version";
}

function getEntityMetaLabel() {
    if (state.mode === MODES.createRoot) return "New root requirement";
    if (state.mode === MODES.createChild) return state.id ? `Parent Entity ID: ${state.id}` : "New child requirement";
    if (state.mode === MODES.editVersion) return `Entity ID: ${state.id || "—"} · Version: ${state.version || "—"}`;
    return `Entity ID: ${state.id || "—"}`;
}

function getEntityIdFromCurrentDoc() {
    const detailNode = state.detailNode || findDetailNode(state.currentDoc);

    if (!detailNode) {
        return "";
    }

    return getFirstFieldRawValue(detailNode, ["EntityId"], "");
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
    applyTopbarMetadata(document, state.currentDoc || state.topPanel);
}

function renderAllFromDoc(doc) {
    renderBasisInfoFromDoc(doc);
    historyTable.loadFromDocument(doc);
    notesTable.loadFromDocument(doc);
    attachmentsTable.loadFromDocument(doc);
    linksTable.loadFromDocument(doc);
    relationsTable.loadFromDocument(doc);
}

function findDetailNode(root) {
    return root?.querySelector("stakeholderRequirementDocument > stakeholderRequirement")
        || root?.querySelector("stakeholderRequirementDocument stakeholderRequirement")
        || root?.querySelector("stakeholderRequirement")
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
        const normalized = value ? value.replace(" ", "T") : "";

        return `
            <div class="page-field">
                <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
                <input id="fld-${escapedName}" data-field="${escapedName}" type="datetime-local" step="1" value="${escapeHtml(normalized)}" ${readonlyAttr} ${requiredAttr} />
            </div>
        `;
    }

    if (control === "date") {
        const normalized = value ? value.substring(0, 10) : "";

        return `
            <div class="page-field">
                <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
                <input id="fld-${escapedName}" data-field="${escapedName}" type="date" value="${escapeHtml(normalized)}" ${readonlyAttr} ${requiredAttr} />
            </div>
        `;
    }

    if (control === "textarea"
        || name === "RequirementDescription"
        || name === "StakeholderRequirementDescription"
        || name === "ReqDescription"
        || name === "Description") {
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

async function saveCurrentRequirement() {
    if (state.readOnly) {
        return;
    }

    const validationErrors = validateCurrentRequirement();

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
        console.error("Failed to save stakeholder requirement", error);
        setText("dlgStatus", `Save failed. ${error.message}`);
        setText("loadStatus", "Error");
    }
}

function validateCurrentRequirement() {
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

    const detailContainer = getDirectChild(root, "stakeholderRequirementDocument");
    const detailNode = detailContainer
        ? getDirectChild(detailContainer, "stakeholderRequirement")
        : getDirectChild(root, "stakeholderRequirement");

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
    linksTable.writeToDocument(updatedDoc);
    relationsTable.writeToDocument(updatedDoc);

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
        headerCell.classList.add("stakeholderrequirement-edit-resizable-th");

        if (headerCell.querySelector(":scope > .stakeholderrequirement-edit-column-resizer")) {
            return;
        }

        const handle = document.createElement("span");
        handle.className = "stakeholderrequirement-edit-column-resizer";
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

    document.body.classList.add("stakeholderrequirement-edit-column-resizing");

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

        document.body.classList.remove("stakeholderrequirement-edit-column-resizing");
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

function getFirstFieldRawValue(node, fieldNames, fallback = "") {
    for (const fieldName of fieldNames) {
        const field = getDirectChild(node, fieldName);

        if (!field) {
            continue;
        }

        const valueText = field.querySelector(":scope > Value")?.textContent?.trim();
        const directText = getDirectText(field).trim();

        if (valueText) {
            return valueText;
        }

        if (directText) {
            return directText;
        }
    }

    return fallback;
}
