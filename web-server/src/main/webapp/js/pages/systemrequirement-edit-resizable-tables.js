const STORAGE_KEY = "basis.systemrequirement.edit.tableColumnWidths";

const RESIZABLE_TABLES = [
    {
        tableSelector: ".history-table",
        storageKey: "history",
        defaultMinWidth: 520
    },
    {
        tableSelector: ".attachments-table",
        storageKey: "attachments",
        defaultMinWidth: 760
    },
    {
        tableSelector: ".notes-table",
        storageKey: "notes",
        defaultMinWidth: 640
    },
    {
        tableSelector: ".relations-table",
        storageKey: "relations",
        defaultMinWidth: 760
    }
];

export function initializeSystemRequirementEditResizableTables() {
    RESIZABLE_TABLES.forEach((config) => {
        const table = document.querySelector(config.tableSelector);

        if (!table) {
            return;
        }

        initializeResizableTable(table, config);
    });
}

function initializeResizableTable(table, config) {
    const headerCells = Array.from(table.querySelectorAll("thead th"));

    if (!headerCells.length) {
        return;
    }

    ensureColGroup(table, headerCells.length);
    applyStoredColumnWidths(table, config.storageKey, config.defaultMinWidth);

    headerCells.forEach((headerCell, columnIndex) => {
        headerCell.classList.add("systemrequirement-edit-resizable-th");

        if (headerCell.querySelector(":scope > .systemrequirement-edit-column-resizer")) {
            return;
        }

        const handle = document.createElement("span");
        handle.className = "systemrequirement-edit-column-resizer";
        handle.setAttribute("aria-hidden", "true");

        handle.addEventListener("click", (event) => {
            event.preventDefault();
            event.stopPropagation();
        });

        handle.addEventListener("mousedown", (event) => {
            event.preventDefault();
            event.stopPropagation();

            startColumnResize(event, table, config, columnIndex, headerCell);
        });

        headerCell.appendChild(handle);
    });
}

function ensureColGroup(table, columnCount) {
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

function startColumnResize(event, table, config, columnIndex, headerCell) {
    const startX = event.clientX;
    const startWidth = headerCell.getBoundingClientRect().width;

    document.body.classList.add("systemrequirement-edit-column-resizing");

    function onMouseMove(moveEvent) {
        const delta = moveEvent.clientX - startX;
        const nextWidth = Math.max(50, startWidth + delta);

        updateColumnWidth(table, config, columnIndex, nextWidth);
    }

    function onMouseUp(upEvent) {
        const delta = upEvent.clientX - startX;
        const nextWidth = Math.max(50, startWidth + delta);

        updateColumnWidth(table, config, columnIndex, nextWidth);
        persistColumnWidth(config.storageKey, columnIndex, nextWidth);

        document.body.classList.remove("systemrequirement-edit-column-resizing");
        window.removeEventListener("mousemove", onMouseMove);
        window.removeEventListener("mouseup", onMouseUp);
    }

    window.addEventListener("mousemove", onMouseMove);
    window.addEventListener("mouseup", onMouseUp);
}

function updateColumnWidth(table, config, columnIndex, widthPx) {
    const width = `${Math.max(50, Math.round(widthPx))}px`;
    const colGroup = table.querySelector(":scope > colgroup");
    const col = colGroup?.children?.[columnIndex];

    if (col) {
        col.style.width = width;
    }

    updateTableMinWidth(table, config.defaultMinWidth);
}

function applyStoredColumnWidths(table, tableKey, defaultMinWidth) {
    const storedWidths = getStoredColumnWidths();
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

    updateTableMinWidth(table, defaultMinWidth);
}

function updateTableMinWidth(table, defaultMinWidth) {
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

function getStoredColumnWidths() {
    try {
        const raw = localStorage.getItem(STORAGE_KEY);

        if (!raw) {
            return {};
        }

        const parsed = JSON.parse(raw);

        return parsed && typeof parsed === "object" ? parsed : {};
    } catch {
        return {};
    }
}

function persistColumnWidth(tableKey, columnIndex, widthPx) {
    const widths = getStoredColumnWidths();

    if (!widths[tableKey] || typeof widths[tableKey] !== "object") {
        widths[tableKey] = {};
    }

    widths[tableKey][String(columnIndex)] = `${Math.max(50, Math.round(widthPx))}px`;

    localStorage.setItem(STORAGE_KEY, JSON.stringify(widths));
}