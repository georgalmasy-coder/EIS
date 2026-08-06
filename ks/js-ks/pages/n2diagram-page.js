import { initMenu } from "../components/menu.js";
import { mountTopbar } from "../components/topbar.js";
import { applyTopbarMetadata } from "../components/topbar.js";
import { setText } from "../core/dom.js";
import {
    getAttribute,
    getBooleanAttribute,
    getChildText,
    getNumberAttribute,
    hasXmlParseError
} from "../core/xml.js";
import {
    getDynamicStyleClass,
    sanitizeClassPart,
    sanitizeCssColor
} from "../core/css.js";

const N2_ENDPOINT = "/basis/basistraceability?cmd=overview";

const FALLBACK_STYLE_ID = "normal";
const DEFAULT_RELATION_TYPE = "relation";
const DEFAULT_DIRECTION = "rowToColumn";

document.addEventListener("DOMContentLoaded", () => {
    initializePageShell();
    loadN2Diagram();
});

function initializePageShell() {
    mountTopbar();

    setText("customerName", "—", "");
    setText("projectName", "—", "");
    setText("userName", "—", "");
    setText("loadStatus", "Loading", "");

    initMenu();
}

async function loadN2Diagram() {
    showEmptyState("Loading N² diagram…");
    setText("loadStatus", "Loading", "");

    try {
        const response = await fetch(N2_ENDPOINT, {
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
            throw new Error("The endpoint returned invalid XML.");
        }

        const matrix = parseTraceabilityMatrixDocument(xmlDocument);

        applyTopPanel(xmlDocument);
        injectN2Styles(matrix.styles);
        renderLegend(matrix.styles);
        renderN2Diagram(matrix);

        setText("loadStatus", "Loaded", "");
    } catch (error) {
        console.error("Failed to load N² diagram", error);
        setText("loadStatus", "Error", "");
        showEmptyState(`Could not load N² diagram. ${error.message}`);
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

    const relationTypes = collectRelationTypes(cellsByRow);
    const directions = collectDirections(cellsByRow);

    return {
        title: getChildText(metaElement, "title", "Traceability Matrix"),
        columnGroupLabel: getChildText(metaElement, "columnGroupLabel", "System requirement"),
        rowGroupLabel: getChildText(metaElement, "rowGroupLabel", "Stakeholder requirement"),
        generatedAt: getChildText(metaElement, "generatedAt", ""),
        rowCount: declaredRowCount,
        columnCount: declaredColumnCount,
        defaultCellStyle,
        defaultCellValue,
        styles,
        columns,
        rows,
        cellsByRow,
        relationTypes,
        directions
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
            backgroundColor: getAttribute(styleElement, "backgroundColor", "#FFFFFF"),
            textColor: getAttribute(styleElement, "textColor", "#000000"),
            bold: getBooleanAttribute(styleElement, "bold", false),
            italic: getBooleanAttribute(styleElement, "italic", false)
        });
    }

    if (!styles.has(FALLBACK_STYLE_ID)) {
        styles.set(FALLBACK_STYLE_ID, {
            id: FALLBACK_STYLE_ID,
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
            entityType: getAttribute(columnElement, "entityType", ""),
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
            entityType: getAttribute(rowElement, "entityType", ""),
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

        addCell(cellsByRow, rowIndex, columnIndex, parseCellElement(cellElement, rowIndex, columnIndex));
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

            addCell(cellsByRow, rowIndex, columnIndex, parseCellElement(cellElement, rowIndex, columnIndex));
        }
    }

    return cellsByRow;
}

function parseCellElement(cellElement, rowIndex, columnIndex) {
    return {
        rowIndex,
        columnIndex,
        value: getAttribute(cellElement, "value", ""),
        style: getAttribute(cellElement, "style", FALLBACK_STYLE_ID),
        relationType: getAttribute(cellElement, "relationType", DEFAULT_RELATION_TYPE),
        direction: getAttribute(cellElement, "direction", DEFAULT_DIRECTION)
    };
}

function addCell(cellsByRow, rowIndex, columnIndex, cell) {
    let rowCells = cellsByRow.get(rowIndex);

    if (!rowCells) {
        rowCells = new Map();
        cellsByRow.set(rowIndex, rowCells);
    }

    rowCells.set(columnIndex, cell);
}

function collectRelationTypes(cellsByRow) {
    const relationTypes = new Set();

    for (const rowCells of cellsByRow.values()) {
        for (const cell of rowCells.values()) {
            if (cell.relationType) {
                relationTypes.add(cell.relationType);
            }
        }
    }

    return Array.from(relationTypes);
}

function collectDirections(cellsByRow) {
    const directions = new Set();

    for (const rowCells of cellsByRow.values()) {
        for (const cell of rowCells.values()) {
            if (cell.direction) {
                directions.add(cell.direction);
            }
        }
    }

    return Array.from(directions);
}

function applyTopPanel(xmlDocument) {
    const topPanelElement = xmlDocument.querySelector("TopPanel");

    if (!topPanelElement) {
        return;
    }

    applyTopbarMetadata(document, xmlDocument);
}

function injectN2Styles(styles) {
    const existingStyleElement = document.getElementById("n2DynamicStyles");

    if (existingStyleElement) {
        existingStyleElement.remove();
    }

    const styleElement = document.createElement("style");
    styleElement.id = "n2DynamicStyles";

    const cssRules = [];

    for (const style of styles.values()) {
        const cssClassName = getDynamicStyleClass(style.id, "n2-xml-style");

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
    const legend = document.getElementById("n2Legend");

    if (!legend) {
        return;
    }

    legend.innerHTML = "";

    for (const style of styles.values()) {
        const item = document.createElement("span");
        item.className = "n2-legend-item";
        item.title = style.id;

        const swatch = document.createElement("span");
        swatch.className = `n2-legend-swatch ${getDynamicStyleClass(style.id, "n2-xml-style")}`;

        const label = document.createElement("span");
        label.textContent = style.id;

        item.append(swatch, label);
        legend.appendChild(item);
    }
}

function renderN2Diagram(matrix) {
    const rowEntityType = getDominantEntityType(matrix.rows);
    const columnEntityType = getDominantEntityType(matrix.columns);

    setText("n2Title", "N² Diagram", "");
    setText("n2RowCount", String(matrix.rows.length || matrix.rowCount), "");
    setText("n2ColumnCount", String(matrix.columns.length || matrix.columnCount), "");
    setText("n2AxisLabel", "N² Diagram", "");
    setText("n2Description", `${rowEntityType || matrix.rowGroupLabel} → ${columnEntityType || matrix.columnGroupLabel}`, "");
    setText("n2RowEntityType", rowEntityType || "—", "");
    setText("n2ColumnEntityType", columnEntityType || "—", "");
    setText("n2RelationType", matrix.relationTypes.length ? matrix.relationTypes.join(", ") : "—", "");
    setText("n2Direction", matrix.directions.length ? matrix.directions.join(", ") : "—", "");

    const tableHead = document.getElementById("n2TableHead");
    const tableBody = document.getElementById("n2TableBody");

    if (!tableHead || !tableBody) {
        throw new Error("Missing N² table elements.");
    }

    tableHead.innerHTML = "";
    tableBody.innerHTML = "";

    if (!matrix.rows.length || !matrix.columns.length) {
        showEmptyState("No rows or columns returned from endpoint.");
        return;
    }

    hideEmptyState();

    renderHeader(tableHead, matrix);
    renderBody(tableBody, matrix);
}

function renderHeader(tableHead, matrix) {
    const headerRow = document.createElement("tr");

    const axisCorner = document.createElement("th");
    axisCorner.className = "n2-corner-axis";
    axisCorner.scope = "col";
    axisCorner.title = matrix.rowGroupLabel;
    headerRow.appendChild(axisCorner);

    const rowHeaderCorner = document.createElement("th");
    rowHeaderCorner.className = "n2-corner-row";
    rowHeaderCorner.scope = "col";
    rowHeaderCorner.title = matrix.rowGroupLabel;
    rowHeaderCorner.textContent = "";
    headerRow.appendChild(rowHeaderCorner);

    for (const column of matrix.columns) {
        const th = document.createElement("th");
        th.className = `n2-column-header ${getDynamicStyleClass(column.style, "n2-xml-style")}`;
        th.scope = "col";
        th.title = buildHeaderTooltip(matrix.columnGroupLabel, column);

        if (column.entityType) {
            const entityTypeSpan = document.createElement("span");
            entityTypeSpan.className = "n2-column-entity-type";
            entityTypeSpan.textContent = column.entityType;
            th.appendChild(entityTypeSpan);
        }

        const span = document.createElement("span");
        span.className = "n2-column-header-text";
        span.textContent = column.label || column.code || column.id;

        th.appendChild(span);
        headerRow.appendChild(th);
    }

    tableHead.appendChild(headerRow);
}

function renderBody(tableBody, matrix) {
    const defaultCell = {
        value: matrix.defaultCellValue || "",
        style: matrix.defaultCellStyle || FALLBACK_STYLE_ID,
        relationType: "",
        direction: ""
    };

    matrix.rows.forEach((row, rowIndexInArray) => {
        const rowIndex = Number.isFinite(row.index) ? row.index : rowIndexInArray;
        const tr = document.createElement("tr");

        if (rowIndexInArray === 0) {
            const axisCell = document.createElement("th");
            axisCell.className = "n2-axis-cell";
            axisCell.rowSpan = matrix.rows.length;
            axisCell.scope = "rowgroup";
            axisCell.title = matrix.rowGroupLabel;

            const axisLabel = document.createElement("div");
            axisLabel.className = "n2-axis-cell-label";

            const axisText = document.createElement("span");
            axisText.textContent = row.entityType || matrix.rowGroupLabel;

            axisLabel.appendChild(axisText);
            axisCell.appendChild(axisLabel);
            tr.appendChild(axisCell);
        }

        const rowHeader = document.createElement("th");
        rowHeader.className = `n2-row-header ${getDynamicStyleClass(row.style, "n2-xml-style")}`;
        rowHeader.scope = "row";
        rowHeader.title = buildRowTooltip(row);

        if (row.entityType) {
            const rowEntityType = document.createElement("span");
            rowEntityType.className = "n2-row-entity-type";
            rowEntityType.textContent = row.entityType;
            rowHeader.appendChild(rowEntityType);
        }

        const rowHeaderText = document.createElement("span");
        rowHeaderText.className = "n2-row-header-text";
        rowHeaderText.textContent = row.label || row.code || row.id;

        rowHeader.appendChild(rowHeaderText);
        tr.appendChild(rowHeader);

        const rowCells = matrix.cellsByRow.get(rowIndex);

        for (let columnIndexInArray = 0; columnIndexInArray < matrix.columns.length; columnIndexInArray += 1) {
            const column = matrix.columns[columnIndexInArray];
            const columnIndex = Number.isFinite(column.index) ? column.index : columnIndexInArray;
            const explicitCell = rowCells?.get(columnIndex);
            const cell = explicitCell || defaultCell;
            const isDiagonal = rowIndex === columnIndex;
            const hasRelation = !!explicitCell && (
                !!explicitCell.value ||
                explicitCell.style !== matrix.defaultCellStyle ||
                !!explicitCell.relationType ||
                !!explicitCell.direction
            );

            const td = document.createElement("td");
            td.className = buildN2CellClass(cell, isDiagonal, hasRelation);
            td.title = buildCellTooltip(row, column, cell, isDiagonal, hasRelation);

            if (cell.value) {
                const valueSpan = document.createElement("span");
                valueSpan.className = "n2-cell-value";
                valueSpan.textContent = cell.value;
                td.appendChild(valueSpan);
            } else if (isDiagonal) {
                const diagonalSpan = document.createElement("span");
                diagonalSpan.className = "n2-cell-value";
                diagonalSpan.textContent = "—";
                td.appendChild(diagonalSpan);
            }

            if (hasRelation) {
                const directionMarker = document.createElement("span");
                directionMarker.className = "n2-direction-marker";
                directionMarker.textContent = getDirectionSymbol(cell.direction);
                td.appendChild(directionMarker);

                if (cell.relationType) {
                    const relationBadge = document.createElement("span");
                    relationBadge.className = "n2-relation-badge";
                    relationBadge.textContent = abbreviateRelationType(cell.relationType);
                    td.appendChild(relationBadge);
                }
            }

            tr.appendChild(td);
        }

        tableBody.appendChild(tr);
    });
}

function buildN2CellClass(cell, isDiagonal, hasRelation) {
    const classes = [
        "n2-cell",
        getDynamicStyleClass(cell.style, "n2-xml-style")
    ];

    if (!hasRelation) {
        classes.push("n2-cell-empty");
    }

    if (hasRelation) {
        classes.push("n2-cell-has-relation");
    }

    if (isDiagonal) {
        classes.push("n2-cell-diagonal");
    }

    if (cell.direction) {
        classes.push(`n2-direction-${sanitizeClassPart(cell.direction)}`);
    }

    if (cell.relationType) {
        classes.push(`n2-relation-${sanitizeClassPart(cell.relationType)}`);
    }

    return classes.join(" ");
}

function buildHeaderTooltip(columnGroupLabel, column) {
    return [
        columnGroupLabel,
        column.entityType ? `Entity type: ${column.entityType}` : "",
        column.code ? `Code: ${column.code}` : "",
        column.label ? `Label: ${column.label}` : "",
        column.id ? `Id: ${column.id}` : ""
    ].filter(Boolean).join("\n");
}

function buildRowTooltip(row) {
    return [
        row.entityType ? `Entity type: ${row.entityType}` : "",
        row.code ? `Code: ${row.code}` : "",
        row.label ? `Label: ${row.label}` : "",
        row.id ? `Id: ${row.id}` : ""
    ].filter(Boolean).join("\n");
}

function buildCellTooltip(row, column, cell, isDiagonal, hasRelation) {
    const value = cell.value ? `Value: ${cell.value}` : "Value: empty";

    return [
        `From row: ${row.label || row.code || row.id}`,
        row.entityType ? `From entity type: ${row.entityType}` : "",
        `To column: ${column.label || column.code || column.id}`,
        column.entityType ? `To entity type: ${column.entityType}` : "",
        value,
        `Style: ${cell.style || FALLBACK_STYLE_ID}`,
        cell.relationType ? `Relation type: ${cell.relationType}` : "",
        cell.direction ? `Direction: ${cell.direction}` : "",
        isDiagonal ? "N² diagonal candidate" : "",
        hasRelation ? "Relation/traceability cell" : "Default/empty cell"
    ].filter(Boolean).join("\n");
}

function getDominantEntityType(items) {
    const counts = new Map();

    for (const item of items) {
        if (!item.entityType) {
            continue;
        }

        counts.set(item.entityType, (counts.get(item.entityType) || 0) + 1);
    }

    let selected = "";
    let selectedCount = 0;

    for (const [entityType, count] of counts.entries()) {
        if (count > selectedCount) {
            selected = entityType;
            selectedCount = count;
        }
    }

    return selected;
}

function getDirectionSymbol(direction) {
    switch (direction) {
        case "rowToColumn":
            return "→";
        case "columnToRow":
            return "←";
        case "bidirectional":
            return "↔";
        default:
            return "•";
    }
}

function abbreviateRelationType(relationType) {
    if (!relationType) {
        return "";
    }

    if (relationType.length <= 8) {
        return relationType;
    }

    if (relationType === "tracesTo") {
        return "trace";
    }

    return relationType.substring(0, 8);
}

function showEmptyState(message) {
    const emptyState = document.getElementById("n2EmptyState");

    if (!emptyState) {
        return;
    }

    emptyState.textContent = message;
    emptyState.classList.add("is-visible");
}

function hideEmptyState() {
    const emptyState = document.getElementById("n2EmptyState");

    if (!emptyState) {
        return;
    }

    emptyState.classList.remove("is-visible");
}
