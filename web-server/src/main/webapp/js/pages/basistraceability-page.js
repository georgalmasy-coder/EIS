import { initMenu } from "../components/menu.js";
import { initHelpDialog } from "../components/help-dialog.js";
import { mountTopbar } from "../components/topbar.js";
import { applyTopbarMetadata } from "../components/topbar.js";
import { openEditDialog } from "../components/edit-dialog.js";
import {
    closeDialogElement,
    setInputValue,
    setText,
    showDialog
} from "../core/dom.js";
import {
    getAttribute,
    getBooleanAttribute,
    getChildText,
    getNumberAttribute,
    hasXmlParseError
} from "../core/xml.js";
import {
    getDynamicStyleClass,
    sanitizeCssColor
} from "../core/css.js";

const TRACEABILITY_ENDPOINT = "/basis/basistraceability?cmd=overview";

const CONFIRM_RELATION_ENDPOINT = "/basis/basistraceability/confirmrelation";
const REMOVE_CONFIRMED_RELATION_ENDPOINT = "/basis/basistraceability/removeconfirmedrelation";
const MARK_RELATION_NOT_RELEVANT_ENDPOINT = "/basis/basistraceability/relationnotrelevant";
const REMOVE_NOT_RELEVANT_RELATION_ENDPOINT = "/basis/basistraceability/removenotrelevantrelation";

const SYSTEM_REQUIREMENT_EDIT_PAGE_URL = "/web/view?page=systemrequirement-edit";
const STAKEHOLDER_REQUIREMENT_EDIT_PAGE_URL = "/web/view?page=stakeholderrequirement-edit";
const RETURN_URL = "/web/view?page=basistraceabilitymatrix";

const FALLBACK_STYLE_ID = "normal";

const CELL_ACTIONS = {
    confirmRelation: "confirmRelation",
    removeConfirmedRelation: "removeConfirmedRelation",
    markRelationNotRelevant: "markRelationNotRelevant",
    removeNotRelevantRelation: "removeNotRelevantRelation"
};

const state = {
    contextCell: null,
    contextRow: null,
    contextColumn: null,
    contextCellElement: null
};

document.addEventListener("DOMContentLoaded", () => {
    initializePageShell();
    initializeEvents();
    loadTraceabilityMatrix();
});

function initializePageShell() {
    mountTopbar();

    setText("customerName", "—", "");
    setText("projectName", "—", "");
    setText("userName", "—", "");
    setText("loadStatus", "Loading", "");

    initMenu();
    initHelpDialog();
}

function initializeEvents() {
    initializeContextMenuEvents();
    initializeRequirementDialogEvents();
}

async function loadTraceabilityMatrix() {
    showEmptyState("Loading traceability matrix…");
    setText("loadStatus", "Loading", "");

    try {
        const response = await fetch(TRACEABILITY_ENDPOINT, {
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
            throw new Error("The traceability endpoint returned invalid XML.");
        }

        const matrix = parseTraceabilityMatrixDocument(xmlDocument);

        applyTopPanel(xmlDocument);
        injectTraceabilityStyles(matrix.styles);
        renderLegend(matrix.styles);
        renderTraceabilityMatrix(matrix);

        setText("loadStatus", "Loaded", "");
    } catch (error) {
        console.error("Failed to load traceability matrix", error);
        setText("loadStatus", "Error", "");
        showEmptyState(`Could not load traceability matrix. ${error.message}`);
    }
}

function parseTraceabilityMatrixDocument(xmlDocument) {
    const matrixElement = xmlDocument.querySelector("traceabilityMatrix");

    if (!matrixElement) {
        throw new Error("Missing traceabilityMatrix element.");
    }

    const metaElement = matrixElement.querySelector(":scope > meta");

    const defaultCellStyle = getAttribute(matrixElement, "defaultCellStyle", FALLBACK_STYLE_ID);
    const defaultCellValue = getAttribute(matrixElement, "defaultCellValue", "");

    const styles = parseStyles(matrixElement);
    const columns = parseColumns(matrixElement);
    const rows = parseRows(matrixElement);
    const cellsByRow = parseCells(matrixElement);

    const declaredRowCount = getNumberAttribute(matrixElement, "rowCount", rows.length);
    const declaredColumnCount = getNumberAttribute(matrixElement, "columnCount", columns.length);

    return {
        title: getChildText(metaElement, "title", "Traceability Matrix"),
        columnGroupLabel: getChildText(metaElement, "columnGroupLabel", "System Requirement"),
        rowGroupLabel: getChildText(metaElement, "rowGroupLabel", "Stakeholder requirement"),
        generatedAt: getChildText(metaElement, "generatedAt", ""),
        rowCount: declaredRowCount,
        columnCount: declaredColumnCount,
        defaultCellStyle,
        defaultCellValue,
        styles,
        columns,
        rows,
        cellsByRow
    };
}

function parseStyles(matrixElement) {
    const styles = new Map();
    const styleElements = matrixElement.querySelectorAll(":scope > styles > style");

    for (const styleElement of styleElements) {
        const id = getAttribute(styleElement, "id", "");

        if (!id) {
            continue;
        }

        styles.set(id, {
            id,
            name: getAttribute(styleElement, "name", id),
            backgroundColor: getAttribute(styleElement, "backgroundColor", "#FFFFFF"),
            textColor: getAttribute(styleElement, "textColor", "#000000"),
            bold: getBooleanAttribute(styleElement, "bold", false),
            italic: getBooleanAttribute(styleElement, "italic", false)
        });
    }

    if (!styles.has(FALLBACK_STYLE_ID)) {
        styles.set(FALLBACK_STYLE_ID, {
            id: FALLBACK_STYLE_ID,
            name: "Normal",
            backgroundColor: "#FFFFFF",
            textColor: "#000000",
            bold: false,
            italic: false
        });
    }

    return styles;
}

function parseColumns(matrixElement) {
    const columnElements = Array.from(matrixElement.querySelectorAll(":scope > columns > column"));

    return columnElements
        .map((columnElement, fallbackIndex) => ({
            index: getNumberAttribute(columnElement, "index", fallbackIndex),
            id: getAttribute(columnElement, "id", String(fallbackIndex)),
            code: getAttribute(columnElement, "code", ""),
            label: getAttribute(columnElement, "label", ""),
            name: getAttribute(columnElement, "name", ""),
            description: getAttribute(columnElement, "description", ""),
            style: getAttribute(columnElement, "style", FALLBACK_STYLE_ID)
        }))
        .sort((a, b) => a.index - b.index);
}

function parseRows(matrixElement) {
    const rowElements = Array.from(matrixElement.querySelectorAll(":scope > rows > row"));

    return rowElements
        .map((rowElement, fallbackIndex) => ({
            index: getNumberAttribute(rowElement, "index", fallbackIndex),
            id: getAttribute(rowElement, "id", String(fallbackIndex)),
            code: getAttribute(rowElement, "code", ""),
            label: getAttribute(rowElement, "label", ""),
            name: getAttribute(rowElement, "name", ""),
            description: getAttribute(rowElement, "description", ""),
            style: getAttribute(rowElement, "style", FALLBACK_STYLE_ID)
        }))
        .sort((a, b) => a.index - b.index);
}

function parseCells(matrixElement) {
    const cellsByRow = new Map();

    const flatCellElements = matrixElement.querySelectorAll(":scope > cells > cell");

    for (const cellElement of flatCellElements) {
        const rowIndex = getNumberAttribute(cellElement, "row", -1);
        const columnIndex = getNumberAttribute(cellElement, "col", -1);

        if (rowIndex < 0 || columnIndex < 0) {
            continue;
        }

        addCell(cellsByRow, rowIndex, columnIndex, {
            rowIndex,
            columnIndex,
            value: getAttribute(cellElement, "value", ""),
            style: getAttribute(cellElement, "style", FALLBACK_STYLE_ID)
        });
    }

    const groupedCellRows = matrixElement.querySelectorAll(":scope > cellRows > cellRow");

    for (const cellRowElement of groupedCellRows) {
        const rowIndex = getNumberAttribute(cellRowElement, "row", -1);

        if (rowIndex < 0) {
            continue;
        }

        const cellElements = cellRowElement.querySelectorAll(":scope > cell");

        for (const cellElement of cellElements) {
            const columnIndex = getNumberAttribute(cellElement, "col", -1);

            if (columnIndex < 0) {
                continue;
            }

            addCell(cellsByRow, rowIndex, columnIndex, {
                rowIndex,
                columnIndex,
                value: getAttribute(cellElement, "value", ""),
                style: getAttribute(cellElement, "style", FALLBACK_STYLE_ID)
            });
        }
    }

    return cellsByRow;
}

function addCell(cellsByRow, rowIndex, columnIndex, cell) {
    let rowCells = cellsByRow.get(rowIndex);

    if (!rowCells) {
        rowCells = new Map();
        cellsByRow.set(rowIndex, rowCells);
    }

    rowCells.set(columnIndex, cell);
}

function applyTopPanel(xmlDocument) {
    const topPanelElement = xmlDocument.querySelector("TopPanel");

    if (!topPanelElement) {
        return;
    }

    applyTopbarMetadata(document, xmlDocument);
}

function injectTraceabilityStyles(styles) {
    const existingStyleElement = document.getElementById("traceabilityDynamicStyles");

    if (existingStyleElement) {
        existingStyleElement.remove();
    }

    const styleElement = document.createElement("style");
    styleElement.id = "traceabilityDynamicStyles";

    const cssRules = [];

    for (const style of styles.values()) {
        const cssClassName = getDynamicStyleClass(style.id, "traceability-xml-style");

        cssRules.push(`
.${cssClassName} {
    background-color: ${sanitizeCssColor(style.backgroundColor)};
    color: ${sanitizeCssColor(style.textColor)};
    font-weight: ${style.bold ? "700" : "inherit"};
    font-style: ${style.italic ? "italic" : "normal"};
}`);
    }

    styleElement.textContent = cssRules.join("\n");
    document.head.appendChild(styleElement);
}

function renderLegend(styles) {
    const legend = document.getElementById("traceabilityLegend");

    if (!legend) {
        return;
    }

    legend.innerHTML = "";

    for (const style of styles.values()) {
        const item = document.createElement("span");
        item.className = "traceability-legend-item";
        item.title = style.id;

        const swatch = document.createElement("span");
        swatch.className = `traceability-legend-swatch ${getDynamicStyleClass(style.id, "traceability-xml-style")}`;

        const label = document.createElement("span");
        label.textContent = style.name || style.id;

        item.append(swatch, label);
        legend.appendChild(item);
    }
}

function renderTraceabilityMatrix(matrix) {
    setText("traceabilityTitle", matrix.title || "Traceability Matrix", "");
    setText("traceabilityRowCount", String(matrix.rows.length || matrix.rowCount), "");
    setText("traceabilityColumnCount", String(matrix.columns.length || matrix.columnCount), "");
    setText("traceabilityColumnGroupLabel", matrix.columnGroupLabel || "System Requirement", "");

    const tableHead = document.getElementById("traceabilityTableHead");
    const tableBody = document.getElementById("traceabilityTableBody");

    if (!tableHead || !tableBody) {
        throw new Error("Missing table elements.");
    }

    tableHead.innerHTML = "";
    tableBody.innerHTML = "";

    if (!matrix.rows.length || !matrix.columns.length) {
        showEmptyState("No traceability rows or columns returned from endpoint.");
        return;
    }

    hideEmptyState();

    renderHeader(tableHead, matrix);
    renderBody(tableBody, matrix);
}

function renderHeader(tableHead, matrix) {
    const headerRow = document.createElement("tr");

    const axisCorner = document.createElement("th");
    axisCorner.className = "traceability-second-corner-axis";
    axisCorner.scope = "col";
    axisCorner.title = matrix.rowGroupLabel;
    headerRow.appendChild(axisCorner);

    const rowHeaderCorner = document.createElement("th");
    rowHeaderCorner.className = "traceability-second-corner-row";
    rowHeaderCorner.scope = "col";
    rowHeaderCorner.textContent = "";
    rowHeaderCorner.title = matrix.rowGroupLabel;
    headerRow.appendChild(rowHeaderCorner);

    for (const column of matrix.columns) {
        const th = document.createElement("th");
        th.className = `traceability-column-header ${getDynamicStyleClass(column.style, "traceability-xml-style")}`;
        th.scope = "col";
        th.title = buildHeaderTooltip(matrix.columnGroupLabel, column);
        th.setAttribute("data-column-id", column.id || "");
        th.setAttribute("data-column-code", column.code || "");
        th.setAttribute("data-column-index", String(column.index));

        const span = document.createElement("span");
        span.className = "traceability-header-text";
        span.textContent = column.label || column.code || column.id;

        th.appendChild(span);

        th.addEventListener("dblclick", () => {
            openSystemRequirementEditPage(column);
        });

        headerRow.appendChild(th);
    }

    tableHead.appendChild(headerRow);
}

function renderBody(tableBody, matrix) {
    const defaultCell = {
        value: matrix.defaultCellValue || "",
        style: matrix.defaultCellStyle || FALLBACK_STYLE_ID
    };

    matrix.rows.forEach((row, rowIndexInArray) => {
        const rowIndex = Number.isFinite(row.index) ? row.index : rowIndexInArray;
        const tr = document.createElement("tr");

        if (rowIndexInArray === 0) {
            const axisCell = document.createElement("th");
            axisCell.className = "traceability-axis-cell";
            axisCell.rowSpan = matrix.rows.length;
            axisCell.scope = "rowgroup";
            axisCell.title = matrix.rowGroupLabel;

            const axisLabel = document.createElement("div");
            axisLabel.className = "traceability-axis-label";

            const axisText = document.createElement("span");
            axisText.textContent = matrix.rowGroupLabel;

            axisLabel.appendChild(axisText);
            axisCell.appendChild(axisLabel);
            tr.appendChild(axisCell);
        }

        const rowHeader = document.createElement("th");
        rowHeader.className = `traceability-row-header ${getDynamicStyleClass(row.style, "traceability-xml-style")}`;
        rowHeader.scope = "row";
        rowHeader.title = `${buildRowTooltip(row)}\nDouble-click to edit Stakeholder Requirement`;
        rowHeader.setAttribute("data-row-id", row.id || "");
        rowHeader.setAttribute("data-row-code", row.code || "");
        rowHeader.setAttribute("data-row-index", String(rowIndex));

        const rowHeaderText = document.createElement("span");
        rowHeaderText.className = "traceability-row-header-text";
        rowHeaderText.textContent = row.label || row.code || row.id;

        rowHeader.appendChild(rowHeaderText);

        rowHeader.addEventListener("dblclick", () => {
            openStakeholderRequirementEditPage(row);
        });

        tr.appendChild(rowHeader);

        const rowCells = matrix.cellsByRow.get(rowIndex);

        for (let columnIndexInArray = 0; columnIndexInArray < matrix.columns.length; columnIndexInArray += 1) {
            const column = matrix.columns[columnIndexInArray];
            const columnIndex = Number.isFinite(column.index) ? column.index : columnIndexInArray;
            const cell = rowCells?.get(columnIndex) || { ...defaultCell, rowIndex, columnIndex };

            const td = document.createElement("td");
            td.className = `traceability-cell ${getDynamicStyleClass(cell.style, "traceability-xml-style")}`;
            td.title = buildCellTooltip(row, column);
            td.setAttribute("data-row-id", row.id || "");
            td.setAttribute("data-row-code", row.code || "");
            td.setAttribute("data-row-index", String(rowIndex));
            td.setAttribute("data-column-id", column.id || "");
            td.setAttribute("data-column-code", column.code || "");
            td.setAttribute("data-column-index", String(columnIndex));
            td.setAttribute("data-cell-style", cell.style || FALLBACK_STYLE_ID);
            td.setAttribute("data-cell-value", cell.value || "");

            td.addEventListener("dblclick", (event) => {
                event.preventDefault();
                event.stopPropagation();
                openTraceabilityRequirementDialog(row, column);
            });

            td.addEventListener("contextmenu", (event) => {
                event.preventDefault();
                event.stopPropagation();
                openTraceabilityContextMenu(event.clientX, event.clientY, row, column, cell, td);
            });

            renderCellValue(td, cell.value);

            tr.appendChild(td);
        }

        tableBody.appendChild(tr);
    });
}

function renderCellValue(cellElement, value) {
    cellElement.innerHTML = "";

    if (value) {
        const valueSpan = document.createElement("span");
        valueSpan.className = "traceability-cell-value";
        valueSpan.textContent = value;
        cellElement.appendChild(valueSpan);
    }
}

function openSystemRequirementEditPage(column) {
    const id = column?.id || "";

    if (!id) {
        window.alert("System Requirement has no entity id.");
        return;
    }

    openEditDialog({
        page: "systemrequirement-edit",
        mode: "edit",
        id,
        title: "Edit System Requirement",
        onSaved: () => window.location.reload()
    });
}

function openStakeholderRequirementEditPage(row) {
    const id = row?.id || "";

    if (!id) {
        window.alert("Stakeholder Requirement has no entity id.");
        return;
    }

    openEditDialog({
        page: "stakeholderrequirement-edit",
        mode: "edit",
        id,
        title: "Edit Stakeholder Requirement",
        onSaved: () => window.location.reload()
    });
}

function initializeRequirementDialogEvents() {
    const dialog = document.getElementById("traceabilityRequirementDialog");
    const closeButton = document.getElementById("traceabilityRequirementDialogCloseButton");
    const okButton = document.getElementById("traceabilityRequirementDialogOkButton");

    closeButton?.addEventListener("click", () => {
        closeRequirementDialog(dialog);
    });

    okButton?.addEventListener("click", () => {
        closeRequirementDialog(dialog);
    });

    dialog?.addEventListener("cancel", (event) => {
        event.preventDefault();
        closeRequirementDialog(dialog);
    });
}

function openTraceabilityRequirementDialog(row, column) {
    const dialog = document.getElementById("traceabilityRequirementDialog");

    if (!dialog) {
        return;
    }

    setInputValue("traceabilityStakeholderId", row?.code || row?.id || "");
    setInputValue("traceabilityStakeholderName", row?.name || row?.label || "");
    setInputValue("traceabilityStakeholderDescription", row?.description || "");

    setInputValue("traceabilitySystemId", column?.code || column?.id || "");
    setInputValue("traceabilitySystemName", column?.name || column?.label || "");
    setInputValue("traceabilitySystemDescription", column?.description || "");

    showDialog(dialog);
}

function closeRequirementDialog(dialog) {
    closeDialogElement(dialog);
}

function initializeContextMenuEvents() {
    const menu = document.getElementById("traceabilityContextMenu");

    menu?.addEventListener("click", async (event) => {
        const button = event.target.closest("button[data-context-action]");

        if (!button) {
            return;
        }

        event.preventDefault();
        event.stopPropagation();

        const action = button.getAttribute("data-context-action");
        await handleTraceabilityContextAction(action);
    });

    menu?.addEventListener("contextmenu", (event) => {
        event.preventDefault();
    });

    document.addEventListener("click", (event) => {
        if (!menu?.contains(event.target)) {
            closeTraceabilityContextMenu();
        }
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            closeTraceabilityContextMenu();
        }
    });

    window.addEventListener("scroll", closeTraceabilityContextMenu, true);
    window.addEventListener("resize", closeTraceabilityContextMenu);
}

function openTraceabilityContextMenu(x, y, row, column, cell, cellElement) {
    const menu = document.getElementById("traceabilityContextMenu");

    if (!menu) {
        return;
    }

    const actions = getTraceabilityCellActions(cell);

    if (!actions.length) {
        closeTraceabilityContextMenu();
        return;
    }

    state.contextCell = cell;
    state.contextRow = row;
    state.contextColumn = column;
    state.contextCellElement = cellElement;

    menu.innerHTML = "";

    actions.forEach((action) => {
        const button = document.createElement("button");
        button.type = "button";
        button.setAttribute("role", "menuitem");
        button.setAttribute("data-context-action", action.action);
        button.textContent = action.label;

        menu.appendChild(button);
    });

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

    const firstButton = menu.querySelector("button[data-context-action]");
    firstButton?.focus?.();
}

function closeTraceabilityContextMenu() {
    const menu = document.getElementById("traceabilityContextMenu");

    if (!menu) {
        return;
    }

    menu.classList.remove("is-open");
    menu.setAttribute("aria-hidden", "true");
    menu.innerHTML = "";
}

async function handleTraceabilityContextAction(action) {
    const cell = state.contextCell;
    const row = state.contextRow;
    const column = state.contextColumn;
    const cellElement = state.contextCellElement;

    closeTraceabilityContextMenu();

    if (!cell || !row || !column || !cellElement) {
        window.alert("No traceability cell selected.");
        clearContextState();
        return;
    }

    if (action === CELL_ACTIONS.confirmRelation) {
        await executeTraceabilityRelationAction(CONFIRM_RELATION_ENDPOINT, row, column, cell, cellElement);
        return;
    }

    if (action === CELL_ACTIONS.removeConfirmedRelation) {
        await executeTraceabilityRelationAction(REMOVE_CONFIRMED_RELATION_ENDPOINT, row, column, cell, cellElement);
        return;
    }

    if (action === CELL_ACTIONS.markRelationNotRelevant) {
        await executeTraceabilityRelationAction(MARK_RELATION_NOT_RELEVANT_ENDPOINT, row, column, cell, cellElement);
        return;
    }

    if (action === CELL_ACTIONS.removeNotRelevantRelation) {
        await executeTraceabilityRelationAction(REMOVE_NOT_RELEVANT_RELATION_ENDPOINT, row, column, cell, cellElement);
        return;
    }

    clearContextState();
}

async function executeTraceabilityRelationAction(endpoint, row, column, cell, cellElement) {
    setCellBusy(cellElement, true);
    setText("loadStatus", "Updating", "");

    try {
        const payload = buildRelationActionPayload(row, column, cell);

        const response = await fetch(endpoint, {
            method: "POST",
            headers: {
                "Accept": "application/json",
                "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8"
            },
            body: payload,
            cache: "no-store",
            credentials: "same-origin"
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status} ${response.statusText}`);
        }

        const result = await response.json();
        const updatedCell = normalizeCellUpdateResponse(result);

        updateTraceabilityCell(cell, cellElement, row, column, updatedCell);

        setText("loadStatus", "Loaded", "");
    } catch (error) {
        console.error("Failed to update traceability relation", error);
        setText("loadStatus", "Error", "");
        window.alert(`Could not update relation. ${error.message}`);
    } finally {
        setCellBusy(cellElement, false);
        clearContextState();
    }
}

function buildRelationActionPayload(row, column, cell) {
    const payload = new URLSearchParams();

    payload.set("rowId", row?.id || "");
    payload.set("rowCode", row?.code || "");
    payload.set("rowIndex", String(row?.index ?? cell?.rowIndex ?? ""));
    payload.set("columnId", column?.id || "");
    payload.set("columnCode", column?.code || "");
    payload.set("columnIndex", String(column?.index ?? cell?.columnIndex ?? ""));
    payload.set("style", cell?.style || FALLBACK_STYLE_ID);
    payload.set("value", cell?.value || "");

    return payload;
}

function normalizeCellUpdateResponse(result) {
    return {
        style: String(result?.style || FALLBACK_STYLE_ID),
        value: String(result?.value || "")
    };
}

function updateTraceabilityCell(cell, cellElement, row, column, updatedCell) {
    const previousStyle = cell.style || FALLBACK_STYLE_ID;
    const nextStyle = updatedCell.style || FALLBACK_STYLE_ID;
    const nextValue = updatedCell.value || "";

    cell.style = nextStyle;
    cell.value = nextValue;

    cellElement.classList.remove(getDynamicStyleClass(previousStyle, "traceability-xml-style"));
    cellElement.classList.add(getDynamicStyleClass(nextStyle, "traceability-xml-style"));

    cellElement.setAttribute("data-cell-style", nextStyle);
    cellElement.setAttribute("data-cell-value", nextValue);
    cellElement.title = buildCellTooltip(row, column);

    renderCellValue(cellElement, nextValue);
}

function setCellBusy(cellElement, busy) {
    if (!cellElement) {
        return;
    }

    cellElement.classList.toggle("is-updating", busy);
    cellElement.setAttribute("aria-busy", busy ? "true" : "false");
}

function clearContextState() {
    state.contextCell = null;
    state.contextRow = null;
    state.contextColumn = null;
    state.contextCellElement = null;
}

function getTraceabilityCellActions(cell) {
    const style = String(cell?.style || "").trim().toLowerCase();

    if (style.includes("yellow")) {
        return [
            {
                action: CELL_ACTIONS.confirmRelation,
                label: "Mark Relation As Confirmed"
            },
            {
                action: CELL_ACTIONS.markRelationNotRelevant,
                label: "Mark Relation As Not Relevant"
            }
        ];
    }

    if (style.includes("green")) {
        return [
            {
                action: CELL_ACTIONS.removeConfirmedRelation,
                label: "Remove Confirmed Relation"
            },
            {
                action: CELL_ACTIONS.markRelationNotRelevant,
                label: "Mark Relation As Not Relevant"
            }
        ];
    }

    if (style.includes("grayitalic") || style.includes("gray")) {
        return [
            {
                action: CELL_ACTIONS.confirmRelation,
                label: "Mark Relation As Confirmed"
            },
            {
                action: CELL_ACTIONS.removeNotRelevantRelation,
                label: "Remove Not Relevant Relation"
            }
        ];
    }

    return [];
}

function buildHeaderTooltip(columnGroupLabel, column) {
    return [
        columnGroupLabel,
        column.code ? `Code: ${column.code}` : "",
        column.name ? `Name: ${column.name}` : "",
        column.label ? `Label: ${column.label}` : "",
        column.id ? `Id: ${column.id}` : "",
        "Double-click to edit System Requirement"
    ].filter(Boolean).join("\n");
}

function buildRowTooltip(row) {
    return [
        row.code ? `Code: ${row.code}` : "",
        row.name ? `Name: ${row.name}` : "",
        row.label ? `Label: ${row.label}` : "",
        row.id ? `Id: ${row.id}` : ""
    ].filter(Boolean).join("\n");
}

function buildCellTooltip(row, column) {
    return [
        `Row: ${row.label || row.code || row.id}`,
        `Column: ${column.label || column.code || column.id}`,
        "Double-click to view traceability details"
    ].join("\n");
}

function showEmptyState(message) {
    const emptyState = document.getElementById("traceabilityEmptyState");

    if (!emptyState) {
        return;
    }

    emptyState.textContent = message;
    emptyState.classList.add("is-visible");
}

function hideEmptyState() {
    const emptyState = document.getElementById("traceabilityEmptyState");

    if (!emptyState) {
        return;
    }

    emptyState.classList.remove("is-visible");
}
