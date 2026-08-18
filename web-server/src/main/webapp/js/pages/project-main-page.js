import { initMenu } from "../components/menu.js";
import { initHelpDialog } from "../components/help-dialog.js";
import { mountTopbar } from "../components/topbar.js";
import { applyTopbarMetadata } from "../components/topbar.js";
import { openEditDialog } from "../components/edit-dialog.js";
import { setText } from "../core/dom.js";
import { escapeHtml } from "../core/html.js";
import { sanitizeCssColor } from "../core/css.js";
import { applyTopPanel as applyPageHeader, parseTopPanel as parsePageTopPanel } from "../core/page-header.js";
import {
    getChildText,
    getDirectChild,
    getDirectChildren,
    getDirectText,
    hasXmlParseError
} from "../core/xml.js";

const DATA_URL = "/project?cmd=list";
const EDIT_PROJECT_URL = "/web/view?page=project-edit&mode=edit&id=";
const CREATE_PROJECT_URL = "/web/view?page=project-edit&mode=create";
const RETURN_URL = "/web/view?page=project-main";

const STORAGE_KEYS = {
    filterText: "project.main.filterText",
    sortKey: "project.main.sortKey",
    sortDirection: "project.main.sortDirection",
    columnWidths: "project.main.columnWidths"
};

const state = {
    currentDoc: null,
    topPanel: {
        customerName: "—",
        projectName: "—",
        userName: "—",
        workspaceEyebrow: "",
        workspaceHeading: "",
        workspaceHelpText: ""
    },
    projects: [],
    filteredProjects: [],
    listColumns: [],
    sortKey: "",
    sortDirection: "asc"
};

document.addEventListener("DOMContentLoaded", () => {
    start();
});

function start() {
    initializeShell();
    initializeStateFromStorage();
    initializeEvents();
    loadProjects();
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
    state.sortKey = localStorage.getItem(STORAGE_KEYS.sortKey) || "";
    state.sortDirection = localStorage.getItem(STORAGE_KEYS.sortDirection) || "asc";

    const filterInput = document.getElementById("filterProjectText");
    const filterText = sessionStorage.getItem(STORAGE_KEYS.filterText) || "";

    if (filterInput) {
        filterInput.value = filterText;
        syncFilterClearButton(filterInput);
    }
}

function initializeEvents() {
    const filterInput = document.getElementById("filterProjectText");
    const clearButton = document.getElementById("btnClearFilter");
    const addButton = document.getElementById("btnAddProject");

    filterInput?.addEventListener("input", () => {
        syncFilterClearButton(filterInput);
        persistFilters();
        applyFiltersAndRender();
    });

    filterInput?.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            filterInput.value = "";
            syncFilterClearButton(filterInput);
            persistFilters();
            applyFiltersAndRender();
            filterInput.blur();
        }
    });

    clearButton?.addEventListener("click", () => {
        if (filterInput) {
            filterInput.value = "";
            syncFilterClearButton(filterInput);
            filterInput.focus();
        }

        persistFilters();
        applyFiltersAndRender();
    });

    addButton?.addEventListener("click", () => {
        openEditDialog({
            page: "project-edit",
            mode: "create",
            title: "Create Project",
            onSaved: () => window.location.reload()
        });
    });
}

async function loadProjects() {
    showEmptyState("Loading projects...");
    setText("loadStatus", "Loading", "");

    try {
        const response = await fetch(DATA_URL, {
            method: "GET",
            headers: {
                Accept: "application/xml,text/xml,*/*"
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

        const root = xmlDocument.getElementsByTagName("ProjectList")[0] || xmlDocument.documentElement;

        state.currentDoc = xmlDocument;
        state.topPanel = parsePageTopPanel(xmlDocument);
        state.projects = parseProjects(root);
        state.listColumns = buildListColumns(state.projects);

        if (state.sortKey && !state.listColumns.some((column) => column.key === state.sortKey)) {
            state.sortKey = "";
            state.sortDirection = "asc";
            persistSorting();
        }

        applyTopPanel();
        applyFiltersAndRender();

        setText("loadStatus", "Loaded", "");
    } catch (error) {
        console.error("Failed to load projects", error);
        setText("loadStatus", "Error", "");
        showEmptyState(`Could not load projects. ${error.message}`);
    }
}

function applyTopPanel() {
    applyTopbarMetadata(document, state.currentDoc || state.topPanel);
    applyPageHeader(state.topPanel, {
        customerName: "customerName",
        projectName: "projectName",
        userName: "userName",
        workspaceEyebrow: "pageEyebrow",
        workspaceHeading: "pageHeading",
        workspaceHelpText: "pageHelpText"
    });
}

function parseProjects(root) {
    const projectsElement = getDirectChild(root, "projects");
    const projectNodes = getDirectChildren(projectsElement, "project");

    return projectNodes.map((node, index) => {
        const fields = parseVisibleFields(node);
        const projectId = getFirstFieldRawValue(node, [
            "ProjectId",
            "projectid",
            "EntityId"
        ], "");

        return {
            node,
            index,
            projectId,
            fields
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

function buildListColumns(projects) {
    const byName = new Map();

    projects.forEach((project) => {
        project.fields.forEach((field) => {
            if (!byName.has(field.name)) {
                byName.set(field.name, {
                    key: field.name,
                    label: field.label,
                    control: field.control,
                    displayOrder: field.displayOrder,
                    tableWidth: field.tableWidth,
                    originalIndex: field.originalIndex
                });
            }
        });
    });

    return Array.from(byName.values())
        .sort((a, b) => {
            if (a.displayOrder !== b.displayOrder) {
                return a.displayOrder - b.displayOrder;
            }

            return a.originalIndex - b.originalIndex;
        })
        .map((column) => ({
            ...column,
            width: getStoredColumnWidths()[column.key] || column.tableWidth || calculateFallbackColumnWidth(column)
        }));
}

function applyFiltersAndRender() {
    const query = getFilterText();

    state.filteredProjects = state.projects.filter((project) => {
        if (!query) {
            return true;
        }

        return buildSearchText(project).includes(query);
    });

    renderTable();
}

function getFilterText() {
    const filterInput = document.getElementById("filterProjectText");

    return String(filterInput?.value || "").trim().toLowerCase();
}

function buildSearchText(project) {
    return project.fields
        .map((field) => `${field.label} ${field.value} ${field.rawValue}`)
        .join(" ")
        .toLowerCase();
}

function renderTable() {
    const colGroup = document.getElementById("mainColGroup");
    const headerRow = document.getElementById("mainHeaderRow");
    const body = document.getElementById("tbody");
    const count = document.getElementById("projectTableCount");
    const table = document.querySelector(".project-main-table");

    if (!colGroup || !headerRow || !body) {
        return;
    }

    const columns = state.listColumns;
    const rows = getSortedProjects(state.filteredProjects);
    const totalWidth = columns.reduce((sum, column) => sum + widthToPixels(column.width, 180), 0);

    if (table) {
        table.style.minWidth = `${Math.max(totalWidth, 900)}px`;
    }

    colGroup.innerHTML = columns.map((column) => `<col style="width: ${escapeHtml(column.width)};">`).join("");

    headerRow.innerHTML = columns.map((column) => {
        const activeSort = state.sortKey === column.key;
        const indicator = activeSort ? (state.sortDirection === "asc" ? "\u25B2" : "\u25BC") : "";

        return `
            <th data-key="${escapeHtml(column.key)}" class="project-main-resizable-th">
                <span class="sort">${escapeHtml(column.label)} <span class="sort-indicator">${indicator}</span></span>
                <span class="project-main-column-resizer" data-resize-column="${escapeHtml(column.key)}" aria-hidden="true"></span>
            </th>
        `;
    }).join("");

    headerRow.querySelectorAll("th[data-key]").forEach((header) => {
        header.addEventListener("click", (event) => {
            if (event.target.closest(".project-main-column-resizer")) {
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
            renderTable();
        });
    });

    initializeColumnResize(headerRow, colGroup, table);

    body.innerHTML = rows.map((project) => `
        <tr data-project-id="${escapeHtml(project.projectId)}" data-project-index="${project.index}">
            ${columns.map((column) => renderListCell(project, column)).join("")}
        </tr>
    `).join("");

    body.querySelectorAll("tr[data-project-index]").forEach((row) => {
        const project = getProjectFromRow(row);

        row.addEventListener("dblclick", () => {
            if (project) {
                openEditProject(project);
            }
        });

        row.addEventListener("keydown", (event) => {
            if ((event.key === "Enter" || event.key === " ") && project) {
                event.preventDefault();
                openEditProject(project);
            }
        });

        row.tabIndex = 0;
    });

    if (rows.length === 0) {
        showEmptyState("No projects found.");
        if (count) {
            count.textContent = "0 of 0";
        }
    } else {
        hideEmptyState();
        if (count) {
            count.textContent = `${rows.length} of ${state.filteredProjects.length}`;
        }
    }
}

function initializeColumnResize(headerRow, colGroup, table) {
    const handles = Array.from(headerRow.querySelectorAll(".project-main-column-resizer"));

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

            document.body.classList.add("project-main-column-resizing");

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

                document.body.classList.remove("project-main-column-resizing");
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

        table.style.minWidth = `${Math.max(totalWidth, 900)}px`;
    }
}

function persistColumnWidth(columnKey, widthPx) {
    const widths = getStoredColumnWidths();

    widths[columnKey] = `${Math.max(50, Math.round(widthPx))}px`;

    localStorage.setItem(STORAGE_KEYS.columnWidths, JSON.stringify(widths));
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

function persistFilters() {
    const filterText = document.getElementById("filterProjectText")?.value || "";

    sessionStorage.setItem(STORAGE_KEYS.filterText, filterText);
}

function syncFilterClearButton(filterInput) {
    const clearButton = document.getElementById("btnClearFilter");

    if (!clearButton || !filterInput) {
        return;
    }

    clearButton.hidden = String(filterInput.value || "") === "";
}

function persistSorting() {
    localStorage.setItem(STORAGE_KEYS.sortKey, state.sortKey || "");
    localStorage.setItem(STORAGE_KEYS.sortDirection, state.sortDirection || "asc");
}

function getSortedProjects(projects) {
    const rows = [...projects];

    if (!state.sortKey) {
        return rows;
    }

    rows.sort((left, right) => {
        const leftField = left.fields.find((field) => field.name === state.sortKey);
        const rightField = right.fields.find((field) => field.name === state.sortKey);

        const leftValue = getFieldSortValue(leftField);
        const rightValue = getFieldSortValue(rightField);
        const direction = state.sortDirection === "desc" ? -1 : 1;

        return compareValues(leftValue, rightValue) * direction;
    });

    return rows;
}

function getFieldSortValue(field) {
    if (!field) {
        return "";
    }

    if (isDateLikeControl(field.control)) {
        return parseDateLike(field.rawValue || field.value || "");
    }

    return String(field.rawValue || field.value || "").toLowerCase();
}

function renderListCell(project, column) {
    const field = project.fields.find((item) => item.name === column.key);
    const value = formatListCellValue(field?.value ?? "", field?.control ?? column.control);
    const color = (field?.control || column.control) === "select" ? sanitizeCssColor(field?.color, "") : "";
    const colorStyle = color ? ` style="${escapeHtml(buildSelectChipStyle(color))}"` : "";
    const colorClass = color ? " select-chip" : "";

    return `<td title="${escapeHtml(value)}"><span class="data-table-cell-value${colorClass}"${colorStyle}>${escapeHtml(value)}</span></td>`;
}

function buildSelectChipStyle(color) {
    const normalized = String(color || "").trim();

    if (!normalized) {
        return "";
    }

    return `background-color: ${buildSelectChipBackground(normalized, 0.18)}; border-color: ${normalized};`;
}

function buildSelectChipBackground(color, alpha) {
    const normalized = String(color || "").trim();

    if (/^#[0-9a-fA-F]{3}$/.test(normalized)) {
        const hex = normalized.slice(1);
        const r = Number.parseInt(hex[0] + hex[0], 16);
        const g = Number.parseInt(hex[1] + hex[1], 16);
        const b = Number.parseInt(hex[2] + hex[2], 16);
        return `rgba(${r}, ${g}, ${b}, ${alpha})`;
    }

    if (/^#[0-9a-fA-F]{6}$/.test(normalized)) {
        const hex = normalized.slice(1);
        const r = Number.parseInt(hex.slice(0, 2), 16);
        const g = Number.parseInt(hex.slice(2, 4), 16);
        const b = Number.parseInt(hex.slice(4, 6), 16);
        return `rgba(${r}, ${g}, ${b}, ${alpha})`;
    }

    if (/^rgba?\(/i.test(normalized)) {
        return normalized;
    }

    return normalized;
}

function getProjectFromRow(row) {
    const projectIndex = Number(row?.getAttribute("data-project-index"));

    return Number.isFinite(projectIndex)
        ? state.projects.find((project) => project.index === projectIndex) || null
        : null;
}

function buildEditProjectUrl(projectId) {
    return `${EDIT_PROJECT_URL}${encodeURIComponent(projectId || "")}&returnUrl=${encodeURIComponent(RETURN_URL)}`;
}

function openEditProject(project) {
    const projectId = project?.projectId || "";

    if (!projectId) {
        return;
    }

    openEditDialog({
        page: "project-edit",
        mode: "edit",
        id: projectId,
        title: "Edit Project",
        onSaved: () => window.location.reload()
    });
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

function parseDateLike(value) {
    const normalized = String(value || "").trim();

    if (!normalized) {
        return new Date(0);
    }

    const timestampMatch = normalized.match(/^(\d{4})-(\d{2})-(\d{2})(?:[T\s](\d{2}):(\d{2})(?::(\d{2}))?)?/);

    if (timestampMatch) {
        const [, year, month, day, hour = "00", minute = "00", second = "00"] = timestampMatch;
        return new Date(Number(year), Number(month) - 1, Number(day), Number(hour), Number(minute), Number(second));
    }

    const compactDateMatch = normalized.match(/^(\d{2})(\d{2})(\d{4})$/);

    if (compactDateMatch) {
        const [, day, month, year] = compactDateMatch;
        return new Date(Number(year), Number(month) - 1, Number(day));
    }

    const parsed = Date.parse(normalized);

    return Number.isFinite(parsed) ? new Date(parsed) : new Date(0);
}

function compareValues(left, right) {
    if (left instanceof Date && right instanceof Date) {
        return left.getTime() - right.getTime();
    }

    return String(left ?? "").localeCompare(String(right ?? ""), undefined, {
        numeric: true,
        sensitivity: "base"
    });
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

function parseNumberAttribute(value, fallback) {
    const parsed = Number(value);

    return Number.isFinite(parsed) ? parsed : fallback;
}

function parseBooleanAttribute(value, fallback) {
    if (value === null || value === undefined || value === "") {
        return fallback;
    }

    const normalized = String(value).trim().toLowerCase();

    if (["true", "1", "yes", "ja", "y", "on"].includes(normalized)) {
        return true;
    }

    if (["false", "0", "no", "nej", "n", "off"].includes(normalized)) {
        return false;
    }

    return fallback;
}

function normalizeTableWidth(value) {
    const raw = String(value || "").trim();

    if (!raw || raw.toLowerCase() === "auto") {
        return "";
    }

    if (/^\d+$/.test(raw)) {
        return `${raw}px`;
    }

    if (/^\d+(?:\.\d+)?px$/i.test(raw)) {
        return raw.toLowerCase();
    }

    if (/^\d+(?:\.\d+)?%$/.test(raw)) {
        return raw;
    }

    return "";
}

function calculateFallbackColumnWidth(column) {
    const key = String(column.key || "").toLowerCase();

    if (key.includes("description")) return "360px";
    if (key.includes("name")) return "220px";
    if (key.includes("code")) return "140px";
    if (key.endsWith("id")) return "120px";
    if (key.includes("status")) return "160px";
    if (key.includes("date") || key.includes("time")) return "180px";
    if (key.includes("owner") || key.includes("createdby") || key.includes("changedby")) return "160px";

    return "180px";
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

function showEmptyState(message) {
    const emptyState = document.getElementById("listEmptyState");
    const body = document.getElementById("tbody");

    if (emptyState) {
        emptyState.textContent = message;
        emptyState.classList.add("is-visible");
    }

    if (body) {
        body.innerHTML = "";
    }
}

function hideEmptyState() {
    const emptyState = document.getElementById("listEmptyState");

    if (emptyState) {
        emptyState.classList.remove("is-visible");
    }
}

