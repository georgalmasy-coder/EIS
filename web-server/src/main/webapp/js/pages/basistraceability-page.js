import {initMenu} from "../components/menu.js";
import {userPreferences} from "../core/user-preferences.js";
import {createTraceabilityMatrixView, getTraceabilityCell} from "../components/traceability-matrix-view.js?rev=traceability-blocks-4";
import {createTraceabilityBlockLoader} from "../components/traceability-block-loader.js?rev=traceability-blocks-4";
import {initHelpDialog} from "../components/help-dialog.js";
import {mountTopbar} from "../components/topbar.js";
import {openEditDialog} from "../components/edit-dialog.js";
import {highlightMatchingWords} from "../components/traceability-word-highlights.js";
import {closeDialogElement, setInputValue, setText, showDialog} from "../core/dom.js";
import {applyTopPanelFromDocument as applyPageHeaderFromDocument} from "../core/page-header.js";
import {getAttribute, getBooleanAttribute, getChildText, getNumberAttribute, hasXmlParseError} from "../core/xml.js";
import {getDynamicStyleClass, sanitizeCssColor} from "../core/css.js";

const TRACEABILITY_ENDPOINT = "/basis/basistraceability?cmd=overview";
const TRACEABILITY_SNAPSHOT_ENDPOINT = "/basis/basistraceability?cmd=snapshot";
let blockLoader;
let matrixLoadSequence = 0;
let refreshFrame = 0;
let lastSnapshotRecovery = 0;
let requirementDialogRequest = 0;
let matrixView;

const CONFIRM_RELATION_ENDPOINT = "/basis/basistraceability/confirmrelation";
const REMOVE_CONFIRMED_RELATION_ENDPOINT = "/basis/basistraceability/removeconfirmedrelation";
const MARK_RELATION_NOT_RELEVANT_ENDPOINT = "/basis/basistraceability/relationnotrelevant";
const REMOVE_NOT_RELEVANT_RELATION_ENDPOINT = "/basis/basistraceability/removenotrelevantrelation";

const SYSTEM_REQUIREMENT_EDIT_PAGE_URL = "/web/view?page=systemrequirement-edit";
const STAKEHOLDER_REQUIREMENT_EDIT_PAGE_URL = "/web/view?page=stakeholderrequirement-edit";
const RETURN_URL = "/web/view?page=basistraceabilitymatrix";

const FALLBACK_STYLE_ID = "normal";
const VIEW_MODE_KEY = "traceability.matrix.viewMode";
const STATUS_ICONS = {
    green: {label: "Confirmed relations", symbol: "✓", color: "#287A45", path: "m9 12 2 2 4-4"},
    yellow: {label: "Possible relations", symbol: "?", color: "#996600", path: "M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3M12 17h.01"},
    grayitalic: {label: "Not relevant", symbol: "⊘", color: "#62666B", path: "m4.9 4.9 14.2 14.2"}
};

const CELL_ACTIONS = {
    confirmRelation: "confirmRelation",
    removeConfirmedRelation: "removeConfirmedRelation",
    markRelationNotRelevant: "markRelationNotRelevant",
    removeNotRelevantRelation: "removeNotRelevantRelation"
};

const state = {
    matrix: null,
    viewMode: userPreferences.getItem(VIEW_MODE_KEY) === "colors" ? "colors" : "icons",
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
    matrixView = createTraceabilityMatrixView({
        root: document.getElementById("traceabilityGrid"),
        scroll: document.getElementById("traceabilityTableScroll"),
        createIcon: createStatusIcon,
        cellTooltip: buildCellTooltip,
        rowTooltip: buildRowTooltip,
        columnTooltip: buildHeaderTooltip,
        onCellDoubleClick: openTraceabilityRequirementDialog,
        onCellContextMenu: (event, row, column, cell, element) =>
            openTraceabilityContextMenu(event.clientX, event.clientY, row, column, cell, element),
        onRowEdit: openStakeholderRequirementEditPage,
        onColumnEdit: openSystemRequirementEditPage,
        onRangeChange: range => blockLoader?.requestRange(range)
    });
    updateViewModeControl();
    document.getElementById("traceabilityViewToggle")?.addEventListener("click", () => {
        state.viewMode = state.viewMode === "icons" ? "colors" : "icons";
        userPreferences.setItem(VIEW_MODE_KEY, state.viewMode);
        updateViewModeControl();
        closeTraceabilityContextMenu();
        clearContextState();
        if (state.matrix) {
            renderLegend(state.matrix.styles);
            renderTraceabilityMatrix(state.matrix);
        }
    });
    document.getElementById("traceabilityExcelButton")?.addEventListener("click", async (event) => {
        const button = event.currentTarget;
        const matrix = state.matrix;
        if (button.disabled || !matrix || state.relationUpdating) return;
        if (!canExportTraceabilityMatrix(matrix)) {
            window.alert("The matrix exceeds the export limit of 1,000,000 cells or Excel's row/column limits and cannot be exported.");
            return;
        }
        const count = matrix.rows.length * matrix.columns.length;
        if (count > 22_500 && !window.confirm(`The matrix contains ${count.toLocaleString()} data cells. Export requires calculating the entire matrix and may take a long time. Continue?`)) return;
        state.exporting = true;
        button.disabled = true;
        try {
            if (matrix.snapshotId) {
                await blockLoader.ensureAll((done, total) => setText("loadStatus", `Export ${Math.round(100 * done / total)}%`, ""));
                if (state.matrix !== matrix) throw new Error("The matrix changed during export. Please try again.");
            }
            const snapshot = await snapshotTraceabilityMatrix(matrix);
            const {downloadInterfaceXlsx} = await import("../components/interface-xlsx-export.js?rev=matrix-snapshot-1");
            await downloadInterfaceXlsx({snapshot, endpoint: "/basis/basistraceability",
                title: "Traceability Matrix",
                project: document.getElementById("projectName")?.textContent || ""});
        } catch (error) {
            window.alert(error.message);
        } finally {
            state.exporting = false;
            setText("loadStatus", "Loaded", "");
            updateTraceabilityExportButton(state.matrix);
        }
    });
    initializeContextMenuEvents();
    initializeRequirementDialogEvents();
}

async function loadTraceabilityMatrix() {
    const sequence = ++matrixLoadSequence;
    blockLoader?.dispose();
    blockLoader = null;
    const excelButton = document.getElementById("traceabilityExcelButton");
    if (excelButton) excelButton.disabled = true;
    showEmptyState("Loading traceability matrix…");
    setText("loadStatus", "Loading", "");

    try {
        const response = await fetch(TRACEABILITY_SNAPSHOT_ENDPOINT, {
            method: "POST",
            headers: {
                "Accept": "application/xml,text/xml,*/*"
            },
            cache: "no-store"
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status} ${response.statusText}`);
        }

        const xmlText = await response.text();
        if (sequence !== matrixLoadSequence) return;
        const xmlDocument = new DOMParser().parseFromString(xmlText, "application/xml");

        if (hasXmlParseError(xmlDocument)) {
            throw new Error("The traceability endpoint returned invalid XML.");
        }

        const matrix = parseTraceabilityMatrixDocument(xmlDocument);
        const matrixElement = xmlDocument.querySelector("traceabilityMatrix");
        matrix.snapshotId = matrixElement.getAttribute("snapshotId");
        matrix.blockSize = Number(matrixElement.getAttribute("blockSize"));
        if (!matrix.snapshotId || !Number.isInteger(matrix.blockSize) || matrix.blockSize < 1) throw new Error("Invalid traceability snapshot metadata");
        matrix.loadedBlocks = new Set();
        state.matrix = matrix;
        let blockErrorReported = false;
        blockLoader = createTraceabilityBlockLoader({matrix,
            fetchBlock: async (rowStart, columnStart) => {
                const response = await fetch("/basis/basistraceability?cmd=block", {
                    method: "POST", cache: "no-store", credentials: "same-origin",
                    body: new URLSearchParams({snapshotId: matrix.snapshotId, rowStart, columnStart})
                });
                if (!response.ok) {
                    const error = new Error(`Could not load traceability cells (HTTP ${response.status}). Reload the matrix to retry.`);
                    error.status = response.status;
                    throw error;
                }
                return response.json();
            },
            onUpdate: () => {
                if (state.matrix !== matrix || refreshFrame) return;
                refreshFrame = requestAnimationFrame(() => { refreshFrame = 0; matrixView.refresh(); });
            },
            onError: error => {
                if (state.matrix !== matrix || blockErrorReported) return;
                blockErrorReported = true;
                setText("loadStatus", "Error", "");
                window.alert(error.message);
            },
            onExpired: () => {
                if (state.matrix !== matrix) return;
                if (Date.now() - lastSnapshotRecovery < 30_000) {
                    setText("loadStatus", "Error", "");
                    window.alert("The traceability snapshot is no longer available. Reload the page to try again.");
                    return;
                }
                lastSnapshotRecovery = Date.now();
                loadTraceabilityMatrix();
            }
        });

        applyTopPanel(xmlDocument);
        injectTraceabilityStyles(matrix.styles);
        renderLegend(matrix.styles);
        renderTraceabilityMatrix(matrix);

        setText("loadStatus", "Loaded", "");
    } catch (error) {
        if (sequence !== matrixLoadSequence) return;
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
        columnGroupLabel: getChildText(metaElement, "columnGroupLabel", "Systems Requirement"),
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
    const defaultStyle = getAttribute(matrixElement, "defaultCellStyle", FALLBACK_STYLE_ID);
    const defaultValue = getAttribute(matrixElement, "defaultCellValue", "");

    function readCell(element, rowIndex) {
        const columnIndex = getNumberAttribute(element, "col", -1);
        if (rowIndex < 0 || columnIndex < 0) return;
        const value = element.hasAttribute("value") ? element.getAttribute("value").trim() : defaultValue;
        const style = getAttribute(element, "style", defaultStyle);
        if (value === defaultValue && style === defaultStyle) {
            const rowCells = cellsByRow.get(rowIndex);
            rowCells?.delete(columnIndex);
            if (rowCells?.size === 0) cellsByRow.delete(rowIndex);
            return;
        }
        let rowCells = cellsByRow.get(rowIndex);
        if (!rowCells) {
            rowCells = new Map();
            cellsByRow.set(rowIndex, rowCells);
        }
        rowCells.set(columnIndex, {rowIndex, columnIndex, value, style});
    }

    // Iterate siblings instead of retaining a NodeList for millions of cells.
    const flatCells = matrixElement.querySelector(":scope > cells");
    for (let cell = flatCells?.firstElementChild; cell; cell = cell.nextElementSibling) {
        if (cell.tagName === "cell") readCell(cell, getNumberAttribute(cell, "row", -1));
    }
    const groupedRows = matrixElement.querySelector(":scope > cellRows");
    for (let row = groupedRows?.firstElementChild; row; row = row.nextElementSibling) {
        if (row.tagName !== "cellRow") continue;
        const rowIndex = getNumberAttribute(row, "row", -1);
        for (let cell = row.firstElementChild; cell; cell = cell.nextElementSibling) {
            if (cell.tagName === "cell") readCell(cell, rowIndex);
        }
    }
    return cellsByRow;
}

function applyTopPanel(xmlDocument) {
    applyPageHeaderFromDocument(xmlDocument, {
        customerName: "customerName",
        projectName: "projectName",
        userName: "userName",
        workspaceEyebrow: "pageEyebrow",
        workspaceHeading: "pageHeading",
        workspaceHelpText: "pageHelpText"
    });
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
        const icon = state.viewMode === "icons" ? createStatusIcon(style.id) : null;
        if (icon) {
            swatch.className = "traceability-legend-icon";
            swatch.appendChild(icon);
        } else {
            swatch.className = `traceability-legend-swatch ${getDynamicStyleClass(style.id, "traceability-xml-style")}`;
        }

        const label = document.createElement("span");
        label.textContent = style.name || style.id;

        item.append(swatch, label);
        legend.appendChild(item);
    }
}

function updateViewModeControl() {
    const icons = state.viewMode === "icons";
    document.body.classList.toggle("traceability-icon-view", icons);
    const toggle = document.getElementById("traceabilityViewToggle");
    if (toggle) {
        toggle.setAttribute("aria-pressed", String(icons));
        toggle.title = icons ? "Switch to Color" : "Switch to Icons";
        toggle.querySelector(".traceability-view-label").textContent = icons ? "Icons" : "Color";
    }
}

function createStatusIcon(styleId) {
    const definition = STATUS_ICONS[String(styleId || "").toLowerCase()];
    if (!definition) return null;
    const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    svg.classList.add("traceability-status-icon");
    svg.setAttribute("viewBox", "0 0 24 24");
    svg.setAttribute("fill", "none");
    svg.setAttribute("stroke", "currentColor");
    svg.setAttribute("stroke-width", "2");
    svg.setAttribute("stroke-linecap", "round");
    svg.setAttribute("stroke-linejoin", "round");
    svg.setAttribute("role", "img");
    svg.setAttribute("aria-label", definition.label);
    svg.dataset.exportSymbol = definition.symbol;
    svg.style.color = definition.color;
    const circle = document.createElementNS(svg.namespaceURI, "circle");
    circle.setAttribute("cx", "12");
    circle.setAttribute("cy", "12");
    circle.setAttribute("r", "10");
    const path = document.createElementNS(svg.namespaceURI, "path");
    path.setAttribute("d", definition.path);
    svg.append(circle, path);
    return svg;
}

function renderTraceabilityMatrix(matrix) {
    updateViewModeControl();
    updateTraceabilityExportButton(matrix);
    setText("traceabilityRowCount", String(matrix.rows.length || matrix.rowCount), "");
    setText("traceabilityColumnCount", String(matrix.columns.length || matrix.columnCount), "");
    setText("traceabilityColumnGroupLabel", matrix.columnGroupLabel || "Systems Requirement", "");

    matrixView.setMatrix(matrix, state.viewMode);
    if (!matrix.rows.length || !matrix.columns.length) {
        showEmptyState("No traceability rows or columns returned from endpoint.");
    } else {
        hideEmptyState();
    }
}

async function snapshotTraceabilityMatrix(matrix) {
    if (!canExportTraceabilityMatrix(matrix)) {
        throw new Error("The matrix exceeds the export limit of 1,000,000 cells or Excel's row/column limits and cannot be exported.");
    }
    // Match the existing export endpoint's limits before allocating a snapshot.
    if ((matrix.rows.length + 1) * (matrix.columns.length + 2) > 1_000_000
        || matrix.columns.length + 2 > 16384 || matrix.rows.length + 1 > 1048573) {
        throw new Error("The Excel export endpoint supports matrices up to 1,000,000 cells. This matrix exceeds that limit.");
    }
    const sizes = matrixView.getSizes();
    const snapshot = {
        title: "Traceability Matrix",
        project: document.getElementById("projectName")?.textContent || "",
        widths: [sizes.axis, sizes.rowHeader, ...matrix.columns.map(() => sizes.cell)],
        heights: [sizes.header * 0.75, ...matrix.rows.map(() => sizes.row * 0.75)],
        cells: []
    };
    const samples = document.createElement("div");
    samples.className = "traceability-virtual-grid";
    Object.assign(samples.style, {position: "fixed", visibility: "hidden", width: "0", height: "0", overflow: "hidden"});
    document.body.appendChild(samples);
    const appearances = new Map();
    const icons = state.viewMode === "icons";
    function color(value, fallback) {
        const channels = value.match(/[\d.]+/g)?.map(Number);
        if (!channels || channels.length < 3 || channels[3] === 0) return fallback;
        return channels.slice(0, 3).map(channel => Math.round(channel).toString(16).padStart(2, "0")).join("").toUpperCase();
    }
    function appearance(kind, styleId) {
        const key = `${kind}:${styleId}`;
        if (appearances.has(key)) return appearances.get(key);
        const sample = document.createElement("div");
        const styled = ["traceability-cell", "traceability-row-header", "traceability-column-header"].includes(kind);
        sample.className = `${kind}${styled ? ` ${getDynamicStyleClass(styleId, "traceability-xml-style")}` : ""}`;
        samples.appendChild(sample);
        const css = getComputedStyle(sample);
        const definition = kind === "traceability-cell" && icons ? STATUS_ICONS[String(styleId).toLowerCase()] : null;
        const result = {background: color(css.backgroundColor, "FFFFFF"),
            foreground: definition ? definition.color.slice(1).toUpperCase() : color(css.color, "111827"),
            bold: kind !== "traceability-cell", centered: Boolean(definition),
            text: definition?.symbol || ""};
        appearances.set(key, result);
        sample.remove();
        return result;
    }
    function add(row, column, text, kind, styleId, rowSpan = 1, rotation = 0) {
        snapshot.cells.push({...appearance(kind, styleId), row, column, rowSpan, colSpan: 1,
            ...(text === null ? {} : {text}), rotation});
    }
    try {
        add(0, 0, "", "traceability-second-corner-axis", "normal");
        add(0, 1, "", "traceability-second-corner-row", "normal");
        matrix.columns.forEach((column, c) => add(0, c + 2, column.label || column.code || column.id,
            "traceability-column-header", column.style));
        add(1, 0, matrix.rowGroupLabel, "traceability-axis-cell", "normal", matrix.rows.length, 90);
        for (let r = 0; r < matrix.rows.length; r++) {
            const row = matrix.rows[r];
            add(r + 1, 1, row.label || row.code || row.id, "traceability-row-header", row.style);
            matrix.columns.forEach((column, c) => {
                const cell = getTraceabilityCell(matrix, row, column);
                add(r + 1, c + 2, null, "traceability-cell", cell.style);
            });
            // Let the browser paint the disabled export button and remain responsive.
            if (r % 32 === 0) await new Promise(resolve => setTimeout(resolve, 0));
        }
        return snapshot;
    } finally {
        samples.remove();
    }
}

function canExportTraceabilityMatrix(matrix) {
    return Boolean(matrix?.rows.length && matrix.columns.length
        && (matrix.rows.length + 1) * (matrix.columns.length + 2) <= 1_000_000
        && matrix.columns.length + 2 <= 16384 && matrix.rows.length + 1 <= 1048573);
}

function updateTraceabilityExportButton(matrix) {
    const button = document.getElementById("traceabilityExcelButton");
    if (!button) return;
    button.disabled = state.exporting || state.relationUpdating || !matrix?.rows.length || !matrix.columns.length;
    button.title = "Export traceability matrix to Excel";
}

function openSystemRequirementEditPage(column) {
    const id = column?.id || "";

    if (!id) {
        window.alert("Systems Requirement has no entity id.");
        return;
    }

    openEditDialog({
        page: "systemrequirement-edit",
        mode: "edit",
        id,
        title: "Edit Systems Requirement",
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

async function openTraceabilityRequirementDialog(row, column) {
    const dialog = document.getElementById("traceabilityRequirementDialog");

    if (!dialog) {
        return;
    }

    const requestId = ++requirementDialogRequest;
    const stakeholderName = row?.name || "";
    const stakeholderDescription = row?.description || "";
    const systemName = column?.name || "";
    const systemDescription = column?.description || "";
    const render = (matches = []) => {
        const sourceWords = matches.map(match => match.source);
        const targetWords = matches.map(match => match.target);
        highlightMatchingWords(document.getElementById("traceabilityStakeholderName"), stakeholderName, sourceWords);
        highlightMatchingWords(document.getElementById("traceabilityStakeholderDescription"), stakeholderDescription, sourceWords);
        highlightMatchingWords(document.getElementById("traceabilitySystemName"), systemName, targetWords);
        highlightMatchingWords(document.getElementById("traceabilitySystemDescription"), systemDescription, targetWords);
    };
    setInputValue("traceabilityStakeholderId", row?.code || row?.id || "");
    setInputValue("traceabilitySystemId", column?.code || column?.id || "");
    render();
    setText("traceabilityMatchStatus", "Finding matching words…");
    showDialog(dialog);

    try {
        const response = await fetch("/basis/basistraceability/matchingwords", {
            method: "POST",
            headers: {
                "Accept": "application/json",
                "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8"
            },
            body: new URLSearchParams({ stakeholderName, stakeholderDescription, systemName, systemDescription }),
            cache: "no-store",
            credentials: "same-origin"
        });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const matches = await response.json();
        if (requestId !== requirementDialogRequest) return;
        render(matches);
        setText("traceabilityMatchStatus", matches.length
            ? `${matches.length} matching word pairs shown in bold`
            : "No matching words");
    } catch (error) {
        if (requestId !== requirementDialogRequest) return;
        console.error("Failed to retrieve matching words", error);
        setText("traceabilityMatchStatus", "Matching words could not be loaded");
    }
}

function closeRequirementDialog(dialog) {
    ++requirementDialogRequest;
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
    if (state.relationUpdating || state.exporting) return;
    const matrix = state.matrix;
    const previousSnapshotId = matrix?.snapshotId;
    state.relationUpdating = true;
    blockLoader?.pause();
    updateTraceabilityExportButton(matrix);
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
            if (response.status === 409) {
                if (state.matrix === matrix) await loadTraceabilityMatrix();
                return; // Never retry a write: it may already have been committed.
            }
            throw new Error(`HTTP ${response.status} ${response.statusText}`);
        }

        const update = await response.json();
        if (state.matrix !== matrix || matrix.snapshotId !== previousSnapshotId) return;
        const changed = update.cell;
        if (!update.snapshotId || changed?.rowIndex !== row.index || changed.columnIndex !== column.index) {
            throw new Error("Invalid traceability relation response");
        }
        blockLoader.rebase(update.snapshotId);
        let rowCells = matrix.cellsByRow.get(row.index);
        if (!rowCells) matrix.cellsByRow.set(row.index, rowCells = new Map());
        rowCells.set(column.index, {rowIndex: row.index, columnIndex: column.index, style: changed.style, value: changed.value});
        row.style = changed.rowStyle;
        column.style = changed.columnStyle;
        setText("loadStatus", "Loaded", "");
    } catch (error) {
        console.error("Failed to update traceability relation", error);
        setText("loadStatus", "Error", "");
        window.alert(`Could not update relation. ${error.message}`);
    } finally {
        state.relationUpdating = false;
        if (state.matrix === matrix) {
            blockLoader?.resume();
            matrixView.refresh();
        }
        updateTraceabilityExportButton(state.matrix);
        setCellBusy(cellElement, false);
        clearContextState();
    }
}

function buildRelationActionPayload(row, column, cell) {
    const payload = new URLSearchParams();
    payload.set("snapshotId", state.matrix?.snapshotId || "");

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

    if (style.includes("yellow") || style.includes("normal") || style.includes("white")) {
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
        "Double-click to edit Systems Requirement"
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
