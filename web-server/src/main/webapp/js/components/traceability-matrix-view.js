import {getDynamicStyleClass} from "../core/css.js";

export function getTraceabilityCell(matrix, row, column) {
    if (matrix.snapshotId && !matrix.loadedBlocks.has(
        `${Math.floor(row.index / matrix.blockSize) * matrix.blockSize}:${Math.floor(column.index / matrix.blockSize) * matrix.blockSize}`)) {
        return {rowIndex: row.index, columnIndex: column.index, value: "", style: "loading", loading: true};
    }
    return matrix.cellsByRow.get(row.index)?.get(column.index) || {
        rowIndex: row.index,
        columnIndex: column.index,
        value: matrix.defaultCellValue,
        style: matrix.defaultCellStyle
    };
}

// Only the viewport and a small buffer have DOM nodes. Positions are array
// positions; XML indices can be non-contiguous and are used only for data lookup.
export function createTraceabilityMatrixView({root, scroll, createIcon, cellTooltip,
    rowTooltip, columnTooltip, onCellDoubleClick, onCellContextMenu, onRowEdit, onColumnEdit, onRangeChange}) {
    let matrix;
    let mode;
    let frame = 0;
    let lastRange = "";
    let sizes;
    const cells = layer("traceability-virtual-cells");
    const rows = layer("traceability-virtual-rows");
    const columns = layer("traceability-virtual-columns");
    const axis = layer("traceability-virtual-axis traceability-axis-cell");
    const corner = layer("traceability-virtual-corner traceability-second-corner-row");
    root.replaceChildren(cells, rows, columns, axis, corner);
    root.classList.add("traceability-virtual-grid");

    let hoveredHeaders = [];
    function clearHeaderHover() {
        hoveredHeaders.forEach(header => header.classList.remove("traceability-header-hover"));
        hoveredHeaders = [];
    }
    root.addEventListener("mouseover", event => {
        clearHeaderHover();
        const hit = target(event);
        if (!hit) return;
        if (hit.element.matches(".traceability-row-header, .traceability-column-header")) {
            hoveredHeaders = [hit.element];
        } else {
            hoveredHeaders = Array.from(root.querySelectorAll(".traceability-row-header, .traceability-column-header"))
                .filter(header => header.classList.contains("traceability-row-header")
                    ? header.dataset.rowPosition === hit.element.dataset.rowPosition
                    : header.dataset.columnPosition === hit.element.dataset.columnPosition);
        }
        hoveredHeaders.forEach(header => header.classList.add("traceability-header-hover"));
    });
    root.addEventListener("mouseleave", clearHeaderHover);
    scroll.addEventListener("scroll", clearHeaderHover, {passive: true});
    window.addEventListener("blur", clearHeaderHover);

    function layer(className) {
        const element = document.createElement("div");
        element.className = className;
        return element;
    }

    function measure() {
        const css = getComputedStyle(root);
        const size = (name, fallback) => Number.parseFloat(css.getPropertyValue(name)) || fallback;
        sizes = {axis: size("--traceability-axis-width", 44),
            rowHeader: size("--traceability-row-header-width", 192),
            cell: size("--traceability-cell-width", 90),
            header: size("--traceability-header-row-2-height", 104),
            row: size("--traceability-row-height", 54)};
        sizes.left = sizes.axis + sizes.rowHeader;
    }

    function position(element, left, top, width, height) {
        Object.assign(element.style, {left: `${left}px`, top: `${top}px`,
            width: `${width}px`, height: `${height}px`});
    }

    function render(force = false) {
        if (!matrix) return;
        const x = scroll.scrollLeft;
        const y = scroll.scrollTop;
        columns.style.transform = `translateY(${y}px)`;
        rows.style.transform = `translateX(${x}px)`;
        corner.style.transform = `translate(${x}px, ${y}px)`;
        axis.style.transform = `translate(${x}px, ${y}px)`;
        axis.style.height = `${Math.max(0, scroll.clientHeight - sizes.header)}px`;
        const rowStart = Math.max(0, Math.floor(y / sizes.row) - 3);
        const colStart = Math.max(0, Math.floor(x / sizes.cell) - 3);
        const rowEnd = Math.min(matrix.rows.length,
            Math.ceil((y + Math.max(0, scroll.clientHeight - sizes.header)) / sizes.row) + 3);
        const colEnd = Math.min(matrix.columns.length,
            Math.ceil((x + Math.max(0, scroll.clientWidth - sizes.left)) / sizes.cell) + 3);
        const range = `${rowStart}:${rowEnd}:${colStart}:${colEnd}`;
        if (!force && range === lastRange) return;
        lastRange = range;
        onRangeChange?.({rowStart, rowEnd, colStart, colEnd});
        const cellFragment = document.createDocumentFragment();
        const rowFragment = document.createDocumentFragment();
        const columnFragment = document.createDocumentFragment();
        for (let c = colStart; c < colEnd; c++) {
            const column = matrix.columns[c];
            const header = document.createElement("div");
            header.className = `traceability-column-header ${getDynamicStyleClass(column.style, "traceability-xml-style")}`;
            header.setAttribute("role", "columnheader");
            header.setAttribute("aria-colindex", String(c + 2));
            header.dataset.columnPosition = c;
            header.title = columnTooltip(matrix.columnGroupLabel, column);
            const text = document.createElement("span");
            text.className = "traceability-header-text";
            text.textContent = column.label || column.code || column.id;
            header.appendChild(text);
            position(header, sizes.left + c * sizes.cell, 0, sizes.cell, sizes.header);
            columnFragment.appendChild(header);
        }
        for (let r = rowStart; r < rowEnd; r++) {
            const row = matrix.rows[r];
            const header = document.createElement("div");
            header.className = `traceability-row-header ${getDynamicStyleClass(row.style, "traceability-xml-style")}`;
            header.setAttribute("role", "rowheader");
            header.dataset.rowPosition = r;
            header.title = `${rowTooltip(row)}\nDouble-click to edit Stakeholder Requirement`;
            const text = document.createElement("span");
            text.className = "traceability-row-header-text";
            text.textContent = row.label || row.code || row.id;
            header.appendChild(text);
            position(header, 0, sizes.header + r * sizes.row, sizes.rowHeader, sizes.row);
            rowFragment.appendChild(header);
            const rowElement = document.createElement("div");
            rowElement.setAttribute("role", "row");
            rowElement.setAttribute("aria-rowindex", String(r + 2));
            for (let c = colStart; c < colEnd; c++) {
                const cell = getTraceabilityCell(matrix, row, matrix.columns[c]);
                const element = document.createElement("div");
                element.className = `traceability-cell ${getDynamicStyleClass(cell.style, "traceability-xml-style")}`;
                element.setAttribute("role", "gridcell");
                element.setAttribute("aria-colindex", String(c + 2));
                element.dataset.rowPosition = r;
                element.dataset.columnPosition = c;
                element.classList.toggle("is-loading", cell.loading === true);
                if (cell.loading) {
                    element.setAttribute("aria-busy", "true");
                    element.appendChild(createLoadingIcon());
                } else if (mode === "icons") {
                    const icon = createIcon(cell.style);
                    if (icon) element.appendChild(icon);
                }
                position(element, sizes.left + c * sizes.cell, sizes.header + r * sizes.row,
                    sizes.cell, sizes.row);
                rowElement.appendChild(element);
            }
            cellFragment.appendChild(rowElement);
        }
        clearHeaderHover();
        cells.replaceChildren(cellFragment);
        rows.replaceChildren(rowFragment);
        columns.replaceChildren(columnFragment);
    }

    function schedule() {
        if (!frame) frame = requestAnimationFrame(() => {frame = 0; render();});
    }
    scroll.addEventListener("scroll", schedule, {passive: true});
    const resize = new ResizeObserver(() => {
        if (matrix) {measure(); layout(); render(true);}
    });
    resize.observe(scroll);

    function target(event) {
        const element = event.target.closest("[data-row-position], [data-column-position]");
        if (!element || !root.contains(element) || !matrix) return null;
        return {element, row: matrix.rows[element.dataset.rowPosition],
            column: matrix.columns[element.dataset.columnPosition]};
    }
    root.addEventListener("dblclick", event => {
        const hit = target(event);
        if (!hit) return;
        event.preventDefault();
        if (hit.row && hit.column) {
            if (getTraceabilityCell(matrix, hit.row, hit.column).loading) return;
            event.stopPropagation();
            onCellDoubleClick(hit.row, hit.column);
        }
        else if (hit.row) onRowEdit(hit.row);
        else if (hit.column) onColumnEdit(hit.column);
    });
    root.addEventListener("contextmenu", event => {
        const hit = target(event);
        if (!hit?.row || !hit.column) return;
        event.preventDefault();
        if (getTraceabilityCell(matrix, hit.row, hit.column).loading) return;
        event.stopPropagation();
        onCellContextMenu(event, hit.row, hit.column,
            getTraceabilityCell(matrix, hit.row, hit.column), hit.element);
    });
    root.addEventListener("mouseover", event => {
        const hit = target(event);
        if (!hit?.row || !hit.column) return;
        const icon = hit.element.querySelector(".traceability-status-icon");
        hit.element.title = cellTooltip(hit.row, hit.column)
            + (icon ? `\n${icon.getAttribute("aria-label")}` : "");
    });

    function layout() {
        root.style.width = `${sizes.left + matrix.columns.length * sizes.cell}px`;
        root.style.height = `${sizes.header + matrix.rows.length * sizes.row}px`;
        position(rows, sizes.axis, 0, sizes.rowHeader, 0);
        position(columns, 0, 0, 0, sizes.header);
        position(corner, 0, 0, sizes.left, sizes.header);
        position(axis, 0, sizes.header, sizes.axis, 0);
    }

    return {
        setMatrix(value, viewMode) {
            matrix = value;
            mode = viewMode;
            measure();
            root.setAttribute("aria-rowcount", String(matrix.rows.length + 1));
            root.setAttribute("aria-colcount", String(matrix.columns.length + 1));
            corner.title = matrix.rowGroupLabel;
            const label = document.createElement("div");
            label.className = "traceability-axis-label";
            const text = document.createElement("span");
            text.textContent = matrix.rowGroupLabel;
            label.appendChild(text);
            axis.replaceChildren(label);
            layout();
            render(true);
        },
        getSizes() {return {...sizes};},
        refresh() { render(true); }
    };
}

function createLoadingIcon() {
    const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    svg.classList.add("traceability-loading-icon");
    svg.setAttribute("viewBox", "0 0 24 24");
    svg.setAttribute("fill", "none");
    svg.setAttribute("stroke", "currentColor");
    svg.setAttribute("stroke-width", "1.7");
    svg.setAttribute("stroke-linecap", "round");
    svg.setAttribute("stroke-linejoin", "round");
    svg.setAttribute("role", "img");
    svg.setAttribute("aria-label", "Loading");
    const path = document.createElementNS(svg.namespaceURI, "path");
    path.setAttribute("d", "M5 3h14M5 21h14M7 3v4c0 2 3 4 5 5-2 1-5 3-5 5v4M17 3v4c0 2-3 4-5 5 2 1 5 3 5 5v4M9 18h6");
    svg.appendChild(path);
    return svg;
}
