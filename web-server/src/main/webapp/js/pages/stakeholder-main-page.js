import { initMenu } from "../components/menu.js";
import { initHelpDialog } from "../components/help-dialog.js";
import { mountTopbar, applyTopbarMetadata } from "../components/topbar.js";
import { openEditDialog } from "../components/edit-dialog.js";
import { setText } from "../core/dom.js";
import { escapeHtml } from "../core/html.js";
import { fieldDisplayValue, fieldHeader, fieldVisible, fieldControl, fieldValue } from "../core/field-display.js";
import { compareSortableValues, applySortIndicators } from "../components/sortable-table.js";
import { getDirectChild, getDirectChildren, hasXmlParseError } from "../core/xml.js";
import { isTruthy } from "../core/utils.js";

const LIST_URL = "/basis/stakeholder?cmd=list";
const ROW_TAGS = ["stakeholder", "Stakeholder"];
const LIST_CONTAINER_TAGS = ["stakeholders", "Stakeholders", "stakeholderList", "StakeholderList"];
const TOP_PANEL_TAG = "TopPanel";
const STORAGE_KEYS = {
    sortKey: "stakeholder.main.sortKey",
    sortDirection: "stakeholder.main.sortDirection",
    columnWidths: "stakeholder.main.columnWidths"
};

const state = {
    xmlDocument: null,
    topPanel: {
        customerName: "-",
        projectName: "-",
        userName: "-"
    },
    rows: [],
    columns: [],
    sortState: {
        key: "",
        dir: "asc"
    },
    storedWidths: {}
};

document.addEventListener("DOMContentLoaded", start);

function start() {
    initializeShell();
    initializeStateFromStorage();
    initializeEvents();
    loadStakeholders();
}

function initializeShell() {
    setText("customerName", "-", "-");
    setText("projectName", "-", "-");
    setText("userName", "-", "-");
    setText("loadStatus", "Loading", "-");

    const addButton = document.getElementById("btnAddNew");

    if (addButton) {
        addButton.classList.add("stakeholder-main-icon-button");
        addButton.innerHTML = `
            <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                <path d="M12 5v14M5 12h14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"></path>
            </svg>
        `;
        addButton.setAttribute("aria-label", "Add stakeholder");
        addButton.title = "Add stakeholder";
    }

    initMenu(document);
    mountTopbar(document);
    initHelpDialog();
}

function initializeEvents() {
    document.getElementById("btnAddNew")?.addEventListener("click", () => {
        openStakeholderEditor({ mode: "create" });
    });
}

async function loadStakeholders() {
    setText("loadStatus", "Loading", "-");
    setText("emptyState", "Loading stakeholders...", "Loading stakeholders...");

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
            throw new Error("The stakeholder endpoint returned invalid XML.");
        }

        const root = xmlDocument.getElementsByTagName("StakeholderList")[0]
            || xmlDocument.getElementsByTagName("stakeholderList")[0]
            || xmlDocument.documentElement;

        state.xmlDocument = xmlDocument;
        state.topPanel = parseTopPanel(root);
        state.rows = parseRows(root);
        state.columns = buildColumns(state.rows);

        if (state.sortState.key && !state.columns.some((column) => column.key === state.sortState.key)) {
            state.sortState = {
                key: "",
                dir: "asc"
            };
            persistSorting();
        }

        renderTopPanel();
        renderTable();

        setText("loadStatus", "Loaded", "-");
    } catch (error) {
        console.error("Failed to load stakeholders", error);
        setText("loadStatus", "Error", "-");
        setText("emptyState", `Could not load stakeholders. ${error.message}`, "");
    }
}

function parseTopPanel(xmlDocument) {
    const topPanelElement = xmlDocument.querySelector(TOP_PANEL_TAG);

    if (!topPanelElement) {
        return {
            customerName: "-",
            projectName: "-",
            userName: "-"
        };
    }

    return {
        customerName: getChildText(topPanelElement, "CustomerName", "-"),
        projectName: getChildText(topPanelElement, "ProjectName", "-"),
        userName: getChildText(topPanelElement, ["UserName", "Name"], "-")
    };
}

function renderTopPanel() {
    applyTopbarMetadata(document, state.topPanel);
}

function parseRows(root) {
    const listContainer = getDirectChild(root, "stakeholders")
        || getDirectChild(root, "Stakeholders")
        || getDirectChild(root, "stakeholderList")
        || getDirectChild(root, "StakeholderList")
        || root;

    const directRows = getDirectChildren(listContainer, "stakeholder");
    const capitalRows = getDirectChildren(listContainer, "Stakeholder");
    const rowNodes = directRows.length ? directRows : capitalRows.length ? capitalRows : findRowNodes(listContainer || root);

    return rowNodes.map((node, index) => {
        const row = {
            index,
            node,
            entityId: getRowEntityId(node),
            active: parseActiveFlag(node),
            values: {},
            rawValues: {}
        };

        Array.from(node.children || []).forEach((field) => {
            const name = field.tagName;
            row.values[name] = fieldDisplayValue(field);
            row.rawValues[name] = fieldValue(field);
        });

        return row;
    });
}

function buildColumns(rows) {
    const byName = new Map();

    rows.forEach((row) => {
        Array.from(row.node.children || []).forEach((field, index) => {
            if (!fieldVisible(field) || fieldControl(field) === "hidden") {
                return;
            }

            const name = field.tagName;

            if (!byName.has(name)) {
                byName.set(name, {
                    key: name,
                    label: fieldHeader(field, name),
                    control: fieldControl(field),
                    displayOrder: parseNumber(field.getAttribute("displayOrder"), parseNumber(field.getAttribute("visibleOrder"), index)),
                    width: state.storedWidths[name] || normalizeWidth(field.getAttribute("tableWidth")) || "180px",
                    originalIndex: index,
                    isActiveColumn: isActiveFieldName(name)
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

    return columns;
}

function renderTable() {
    const body = document.getElementById("tbody");
    const headerRow = document.getElementById("mainHeaderRow");
    const colGroup = document.getElementById("mainColGroup");
    const emptyState = document.getElementById("emptyState");

    if (!body || !headerRow || !colGroup || !emptyState) {
        return;
    }

    if (!state.rows.length) {
        body.innerHTML = "";
        headerRow.innerHTML = "";
        colGroup.innerHTML = "";
        emptyState.hidden = false;
        setText(emptyState, "No stakeholders returned from the web service.", "");
        return;
    }

    const totalWidth = state.columns.reduce((sum, column) => sum + widthToPixels(column.width, 180), 0);
    const table = document.querySelector(".stakeholder-main-table");

    if (table) {
        table.style.minWidth = `${Math.max(totalWidth, 960)}px`;
    }

    colGroup.innerHTML = state.columns.map((column) => {
        return column.width ? `<col style="width:${escapeHtml(column.width)};">` : "<col>";
    }).join("");

    headerRow.innerHTML = state.columns.map((column) => `
        <th data-key="${escapeHtml(column.key)}" class="stakeholder-main-resizable-th">
            <span class="sort">${escapeHtml(column.label)} <span class="sort-indicator" id="si-h-${escapeHtml(column.key)}"></span></span>
            <span class="stakeholder-main-column-resizer" data-resize-column="${escapeHtml(column.key)}" aria-hidden="true"></span>
        </th>
    `).join("");

    headerRow.querySelectorAll("th[data-key]").forEach((header) => {
        header.addEventListener("click", (event) => {
            if (event.target.closest(".stakeholder-main-column-resizer")) {
                return;
            }

            const key = header.getAttribute("data-key");

            if (state.sortState.key === key) {
                state.sortState.dir = state.sortState.dir === "asc" ? "desc" : "asc";
            } else {
                state.sortState.key = key || "";
                state.sortState.dir = "asc";
            }

            persistSorting();
            renderTable();
        });
    });

    applySortIndicators(state.columns.map((column) => column.key), state.sortState, "si-h-");
    initializeColumnResize(headerRow, colGroup, table);

    const rows = [...state.rows].sort(compareRows);

    body.innerHTML = rows.map((row) => createRowMarkup(row)).join("");
    emptyState.hidden = true;
    setText(emptyState, "", "");

    body.querySelectorAll("tr[data-entity-id]").forEach((rowElement) => {
        rowElement.addEventListener("dblclick", () => {
            const entityId = rowElement.getAttribute("data-entity-id") || "";

            if (entityId) {
                openStakeholderEditor({ mode: "edit", id: entityId });
            }
        });
    });
}

function createRowMarkup(row) {
    return `
        <tr data-entity-id="${escapeHtml(row.entityId)}" title="Double-click to edit stakeholder">
            ${state.columns.map((column) => {
        const value = renderCellValue(column, row);

        return column.isActiveColumn
            ? `<td>${value}</td>`
            : `<td>${escapeHtml(value)}</td>`;
    }).join("")}
        </tr>
    `;
}

function renderCellValue(column, row) {
    if (column.isActiveColumn) {
        return row.active
            ? '<span class="stakeholder-active-state"><span class="stakeholder-active-dot is-active" aria-hidden="true"></span></span>'
            : '<span class="stakeholder-active-state"><span class="stakeholder-active-dot is-inactive" aria-hidden="true"></span></span>';
    }

    if (isDateLikeControl(column.control)) {
        return formatListCellValue(row.rawValues[column.key] || row.values[column.key] || "", column.control);
    }

    return row.values[column.key] || row.rawValues[column.key] || "";
}

function formatListCellValue(value, control) {
    const raw = String(value ?? "").trim();

    if (!raw) {
        return "";
    }

    if (isDateLikeControl(control)) {
        return formatDateLike(raw);
    }

    return raw;
}

function isDateLikeControl(control) {
    const normalized = String(control || "").toLowerCase();

    return normalized.includes("date") || normalized.includes("time") || normalized.includes("timestamp");
}

function formatDateLike(value) {
    const normalized = String(value || "").trim();

    if (!normalized) {
        return "";
    }

    const timestampMatch = normalized.match(/^(\d{4})-(\d{2})-(\d{2})(?:[T\s](\d{2}):(\d{2})(?::(\d{2}))?)?/);

    if (timestampMatch) {
        const [, year, month, day, hour, minute] = timestampMatch;

        if (hour && minute) {
            return `${day}-${month}-${year} ${hour}:${minute}`;
        }

        return `${day}-${month}-${year}`;
    }

    const compactDateMatch = normalized.match(/^(\d{2})(\d{2})(\d{4})$/);

    if (compactDateMatch) {
        const [, day, month, year] = compactDateMatch;
        return `${day}-${month}-${year}`;
    }

    return normalized;
}

function sortBy(key) {
    if (state.sortState.key === key) {
        state.sortState.dir = state.sortState.dir === "asc" ? "desc" : "asc";
    } else {
        state.sortState.key = key;
        state.sortState.dir = "asc";
    }

    persistSorting();
    renderTable();
}

function compareRows(left, right) {
    const key = state.sortState.key || "";
    const direction = state.sortState.dir === "desc" ? -1 : 1;

    if (!key) {
        return compareSortableValues(left.entityId || left.index, right.entityId || right.index, { locale: "en" });
    }

    const leftValue = getSortValue(left, key);
    const rightValue = getSortValue(right, key);
    const comparison = compareSortableValues(leftValue, rightValue, { locale: "en" });

    if (comparison !== 0) {
        return comparison * direction;
    }

    return compareSortableValues(left.entityId || left.index, right.entityId || right.index, { locale: "en" });
}

function getSortValue(row, key) {
    if (isActiveFieldName(key)) {
        return row.active ? "1" : "0";
    }

    return row.rawValues[key] || row.values[key] || "";
}

function openStakeholderEditor({ mode, id }) {
    openEditDialog({
        page: "stakeholder-edit",
        mode: mode || "edit",
        id: id || "",
        title: mode === "create" ? "Create Stakeholder" : "Edit Stakeholder",
        onSaved: () => window.location.reload()
    });
}

function findRowNodes(root) {
    for (const tagName of ROW_TAGS) {
        const directRows = Array.from(root?.getElementsByTagName?.(tagName) || []);

        if (directRows.length) {
            return directRows.filter((node) => node.parentElement !== null);
        }
    }

    return [];
}

function getRowEntityId(node) {
    return getFirstFieldRawValue(node, [
        "EntityId",
        "StakeholderId",
        "ID",
        "Id"
    ], "");
}

function parseActiveFlag(node) {
    const value = getFirstFieldRawValue(node, ["Active", "IsActive"], "");

    return isTruthy(value);
}

function getChildText(parent, tagNames, fallback = "") {
    const names = Array.isArray(tagNames) ? tagNames : [tagNames];

    for (const tagName of names) {
        const element = parent?.getElementsByTagName?.(tagName)?.[0];
        const value = element?.textContent?.trim();

        if (value) {
            return value;
        }
    }

    return fallback;
}

function getFirstFieldRawValue(node, fieldNames, fallback = "") {
    for (const fieldName of fieldNames) {
        const field = node?.getElementsByTagName?.(fieldName)?.[0];

        if (!field) {
            continue;
        }

        const valueNode = field.getElementsByTagName("Value")?.[0];
        const value = valueNode?.textContent?.trim() || field.textContent?.trim() || "";

        if (value) {
            return value;
        }
    }

    return fallback;
}

function findFirstByTagNames(root, tagNames) {
    for (const tagName of tagNames) {
        const direct = root?.getElementsByTagName?.(tagName)?.[0];

        if (direct) {
            return direct;
        }

        const query = root?.querySelector?.(tagName);

        if (query) {
            return query;
        }
    }

    return null;
}

function normalizeWidth(value) {
    const raw = String(value || "").trim();

    if (!raw || raw.toLowerCase() === "auto") {
        return "";
    }

    if (/^\d+$/.test(raw)) {
        return `${raw}px`;
    }

    return raw;
}

function parseNumber(value, fallback) {
    const parsed = Number(value);

    return Number.isFinite(parsed) ? parsed : fallback;
}

function isActiveFieldName(name) {
    return ["active", "isactive"].includes(String(name || "").toLowerCase());
}

function initializeStateFromStorage() {
    state.sortState.key = localStorage.getItem(STORAGE_KEYS.sortKey) || "";
    state.sortState.dir = localStorage.getItem(STORAGE_KEYS.sortDirection) || "asc";

    try {
        const raw = localStorage.getItem(STORAGE_KEYS.columnWidths);
        state.storedWidths = raw ? JSON.parse(raw) : {};
    } catch {
        state.storedWidths = {};
    }
}

function persistSorting() {
    localStorage.setItem(STORAGE_KEYS.sortKey, state.sortState.key || "");
    localStorage.setItem(STORAGE_KEYS.sortDirection, state.sortState.dir || "asc");
}

function getStoredColumnWidths() {
    try {
        const raw = localStorage.getItem(STORAGE_KEYS.columnWidths);
        return raw ? JSON.parse(raw) : {};
    } catch {
        return {};
    }
}

function persistColumnWidth(columnKey, widthPx) {
    const widths = getStoredColumnWidths();
    widths[columnKey] = `${Math.max(50, Math.round(widthPx))}px`;
    localStorage.setItem(STORAGE_KEYS.columnWidths, JSON.stringify(widths));
    state.storedWidths = widths;
}

function widthToPixels(value, fallback = 180) {
    const raw = String(value || "").trim();

    if (/^\d+(?:\.\d+)?px$/i.test(raw)) {
        return Number.parseFloat(raw);
    }

    if (/^\d+$/.test(raw)) {
        return Number(raw);
    }

    return fallback;
}

function initializeColumnResize(headerRow, colGroup, table) {
    const handles = Array.from(headerRow.querySelectorAll(".stakeholder-main-column-resizer"));

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

            document.body.classList.add("stakeholder-main-column-resizing");

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
                document.body.classList.remove("stakeholder-main-column-resizing");
                window.removeEventListener("mousemove", onMouseMove);
                window.removeEventListener("mouseup", onMouseUp);
            }

            window.addEventListener("mousemove", onMouseMove);
            window.addEventListener("mouseup", onMouseUp);
        });
    });
}

function updateColumnWidth(columnKey, widthPx, colGroup, table) {
    const columnIndex = state.columns.findIndex((column) => column.key === columnKey);

    if (columnIndex < 0) {
        return;
    }

    const safeWidth = `${Math.max(50, Math.round(widthPx))}px`;
    const cols = Array.from(colGroup.querySelectorAll("col"));

    state.columns[columnIndex].width = safeWidth;

    if (cols[columnIndex]) {
        cols[columnIndex].style.width = safeWidth;
    }

    if (table) {
        const totalWidth = state.columns.reduce((sum, column) => sum + widthToPixels(column.width, 180), 0);
        table.style.minWidth = `${Math.max(totalWidth, 960)}px`;
    }
}
