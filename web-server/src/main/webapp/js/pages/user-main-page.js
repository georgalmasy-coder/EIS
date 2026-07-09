import { initMenu } from "../components/menu.js";
import { initHelpDialog } from "../components/help-dialog.js";
import { mountTopbar } from "../components/topbar.js";
import { applyTopbarMetadata } from "../components/topbar.js";
import { openEditDialog } from "../components/edit-dialog.js";
import { setText } from "../core/dom.js";
import { escapeHtml } from "../core/html.js";
import {
    getChildText,
    getDirectChild,
    getDirectChildren,
    getDirectText,
    hasXmlParseError
} from "../core/xml.js";

const DATA_URL = "/api/user-main?cmd=list";
const EDIT_USER_URL = "/web/view?page=user-edit&mode=edit&id=";
const CREATE_USER_URL = "/web/view?page=user-edit&mode=create";
const RETURN_URL = "/web/view?page=user-main";
const STORAGE_KEYS = {
    filterText: "user.main.filterText",
    sortKey: "user.main.sortKey",
    sortDirection: "user.main.sortDirection",
    columnWidths: "user.main.columnWidths"
};

const DEFAULT_COLUMNS = [
    { key: "name", label: "Name", width: "220px" },
    { key: "userRole", label: "Role", width: "180px" },
    { key: "email", label: "Email", width: "240px" },
    { key: "phone", label: "Phone", width: "150px" },
    { key: "departmentDescription", label: "Department", width: "220px" },
    { key: "lastLoginAt", label: "Last login", width: "180px" },
    { key: "active", label: "Active", width: "90px" }
];

const state = {
    currentDoc: null,
    topPanel: {
        customerName: "—",
        projectName: "—",
        userName: "—"
    },
    users: [],
    filteredUsers: [],
    columns: DEFAULT_COLUMNS.map((column) => ({ ...column })),
    sortKey: localStorage.getItem(STORAGE_KEYS.sortKey) || "name",
    sortDirection: localStorage.getItem(STORAGE_KEYS.sortDirection) || "asc"
};

document.addEventListener("DOMContentLoaded", () => {
    start();
});

function start() {
    initializeShell();
    initializeStateFromStorage();
    initializeEvents();
    loadUsers();
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
    const storedWidths = getStoredColumnWidths();

    state.columns = DEFAULT_COLUMNS.map((column) => ({
        ...column,
        width: storedWidths[column.key] || column.width
    }));

    const filterInput = document.getElementById("filterUserText");
    const filterText = localStorage.getItem(STORAGE_KEYS.filterText) || "";

    if (filterInput) {
        filterInput.value = filterText;
        syncFilterClearButton(filterInput);
    }
}

function initializeEvents() {
    const filterInput = document.getElementById("filterUserText");
    const addButton = document.getElementById("btnAddUser");
    const clearButton = document.getElementById("btnClearFilter");

    filterInput?.addEventListener("input", () => {
        syncFilterClearButton(filterInput);
        persistFilterText();
        applyFiltersAndRender();
    });

    filterInput?.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            filterInput.value = "";
            syncFilterClearButton(filterInput);
            persistFilterText();
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

        persistFilterText();
        applyFiltersAndRender();
    });

    addButton?.addEventListener("click", () => {
        openEditDialog({
            page: "user-edit",
            mode: "create",
            title: "Create User",
            onSaved: () => window.location.reload()
        });
    });
}

async function loadUsers() {
    showEmptyState("Loading users...");
    setText("loadStatus", "Loading", "");

    try {
        const response = await fetch(DATA_URL, {
            method: "GET",
            headers: {
                Accept: "application/xml,text/xml,*/*"
            },
            cache: "no-store",
            credentials: "same-origin"
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status} ${response.statusText}`);
        }

        const xmlText = await response.text();
        const xmlDocument = new DOMParser().parseFromString(xmlText, "application/xml");

        if (hasXmlParseError(xmlDocument)) {
            throw new Error("The user endpoint returned invalid XML.");
        }

        const root = xmlDocument.getElementsByTagName("UserMain")[0] || xmlDocument.documentElement;
        state.currentDoc = xmlDocument;
        state.topPanel = parseTopPanel(root);
        state.users = parseUsers(root);

        if (state.sortKey && !state.columns.some((column) => column.key === state.sortKey)) {
            state.sortKey = "name";
            state.sortDirection = "asc";
            persistSorting();
        }

        applyTopPanel();
        applyFiltersAndRender();

        setText("loadStatus", "Loaded", "");
    } catch (error) {
        console.error("Failed to load users", error);
        setText("loadStatus", "Error", "");
        showEmptyState(`Could not load users. ${error.message}`);
    }
}

function parseTopPanel(root) {
    const topPanel = getDirectChild(root, "TopPanel");

    if (!topPanel) {
        return {
            customerName: "—",
            projectName: "—",
            userName: "—"
        };
    }

    return {
        customerName: getChildText(topPanel, "CustomerName", "—"),
        projectName: getChildText(topPanel, "ProjectName", "—"),
        userName: getChildText(topPanel, "UserName", getChildText(topPanel, "Name", "—"))
    };
}

function applyTopPanel() {
    applyTopbarMetadata(document, state.currentDoc || state.topPanel);
}

function parseUsers(root) {
    const usersElement = getDirectChild(root, "users");
    const userNodes = getDirectChildren(usersElement, "user");

    return userNodes.map((node, index) => ({
        node,
        index,
        userId: getFieldValue(node, "userId"),
        name: getFieldValue(node, "name"),
        userRole: getFieldValue(node, "userRole"),
        userRoleLabel: getFieldValue(node, "userRoleLabel"),
        email: getFieldValue(node, "email"),
        phone: getFieldValue(node, "phone"),
        departmentDescription: getFieldValue(node, "departmentDescription"),
        lastLoginAt: getFieldValue(node, "lastLoginAt"),
        active: parseBoolean(getFieldValue(node, "active"))
    }));
}

function applyFiltersAndRender() {
    const query = getFilterText();

    state.filteredUsers = state.users.filter((user) => {
        if (!query) {
            return true;
        }

        return buildSearchText(user).includes(query);
    });

    renderTable();
}

function getFilterText() {
    const filterInput = document.getElementById("filterUserText");

    return String(filterInput?.value || "").trim().toLowerCase();
}

function buildSearchText(user) {
    return [
        user.name,
        user.userRoleLabel || labelForRoleId(user.userRole),
        user.email,
        user.phone,
        user.departmentDescription,
        user.lastLoginAt,
        user.active ? "active yes" : "inactive no"
    ]
        .join(" ")
        .toLowerCase();
}

function renderTable() {
    const colGroup = document.getElementById("mainColGroup");
    const headerRow = document.getElementById("mainHeaderRow");
    const body = document.getElementById("tbody");
    const table = document.querySelector(".user-main-table");

    if (!colGroup || !headerRow || !body) {
        return;
    }

    const columns = state.columns;
    const rows = getSortedUsers(state.filteredUsers);
    const totalWidth = columns.reduce((sum, column) => sum + widthToPixels(column.width, 180), 0);

    if (table) {
        table.style.minWidth = `${Math.max(totalWidth, 1040)}px`;
    }

    colGroup.innerHTML = columns.map((column) => `<col style="width: ${escapeHtml(column.width)};">`).join("");

    headerRow.innerHTML = columns.map((column) => {
        const activeSort = state.sortKey === column.key;
        const indicator = activeSort ? (state.sortDirection === "asc" ? "^" : "v") : "";

        return `
            <th data-key="${escapeHtml(column.key)}" class="user-main-resizable-th">
                <span class="sort">${escapeHtml(column.label)} <span class="sort-indicator">${indicator}</span></span>
                <span class="user-main-column-resizer" data-resize-column="${escapeHtml(column.key)}" aria-hidden="true"></span>
            </th>
        `;
    }).join("");

    headerRow.querySelectorAll("th[data-key]").forEach((header) => {
        header.addEventListener("click", (event) => {
            if (event.target.closest(".user-main-column-resizer")) {
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

    body.innerHTML = rows.map((user) => `
        <tr data-user-id="${escapeHtml(user.userId)}" tabindex="0">
            ${columns.map((column) => renderListCell(user, column)).join("")}
        </tr>
    `).join("");

    body.querySelectorAll("tr[data-user-id]").forEach((row) => {
        const userId = row.getAttribute("data-user-id");

        row.addEventListener("dblclick", () => {
            openEditUser(userId);
        });

        row.addEventListener("keydown", (event) => {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                openEditUser(userId);
            }
        });
    });

    if (rows.length === 0) {
        showEmptyState("No users found.");
    } else {
        hideEmptyState();
    }
}

function initializeColumnResize(headerRow, colGroup, table) {
    const handles = Array.from(headerRow.querySelectorAll(".user-main-column-resizer"));

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

            document.body.classList.add("user-main-column-resizing");

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

                document.body.classList.remove("user-main-column-resizing");
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
    const columnIndex = state.columns.findIndex((column) => column.key === columnKey);

    if (columnIndex < 0) {
        return;
    }

    state.columns[columnIndex].width = width;

    const col = colGroup?.children?.[columnIndex];

    if (col) {
        col.style.width = width;
    }

    if (table) {
        const totalWidth = state.columns.reduce((sum, column) => sum + widthToPixels(column.width, 180), 0);
        table.style.minWidth = `${Math.max(totalWidth, 1040)}px`;
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

function persistFilterText() {
    const filterText = document.getElementById("filterUserText")?.value || "";
    localStorage.setItem(STORAGE_KEYS.filterText, filterText);
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

function getSortedUsers(users) {
    const rows = [...users];

    if (!state.sortKey) {
        return rows;
    }

    rows.sort((left, right) => {
        const leftValue = getSortValue(left, state.sortKey);
        const rightValue = getSortValue(right, state.sortKey);
        const direction = state.sortDirection === "desc" ? -1 : 1;

        return compareValues(leftValue, rightValue) * direction;
    });

    return rows;
}

function getSortValue(user, key) {
    if (!user) {
        return "";
    }

    switch (key) {
        case "name":
            return String(user.name || "").toLowerCase();
        case "userRole":
            return String(user.userRoleLabel || labelForRoleId(user.userRole) || "").toLowerCase();
        case "email":
            return String(user.email || "").toLowerCase();
        case "phone":
            return String(user.phone || "").toLowerCase();
        case "departmentDescription":
            return String(user.departmentDescription || "").toLowerCase();
        case "lastLoginAt":
            return parseDateLike(user.lastLoginAt);
        case "active":
            return user.active ? 1 : 0;
        default:
            return String(user[key] || "").toLowerCase();
    }
}

function renderListCell(user, column) {
    switch (column.key) {
        case "name":
            return `<td title="${escapeHtml(user.name || "")}">${escapeHtml(user.name || "-")}</td>`;
        case "userRole":
            return `<td>${renderRoleChip(user.userRole, user.userRoleLabel)}</td>`;
        case "email":
            return `<td title="${escapeHtml(user.email || "")}">${escapeHtml(user.email || "-")}</td>`;
        case "phone":
            return `<td title="${escapeHtml(user.phone || "")}">${escapeHtml(user.phone || "-")}</td>`;
        case "departmentDescription":
            return `<td title="${escapeHtml(user.departmentDescription || "")}">${escapeHtml(user.departmentDescription || "-")}</td>`;
        case "lastLoginAt": {
            const formatted = formatDateTime(user.lastLoginAt);
            return `<td title="${escapeHtml(formatted)}">${escapeHtml(formatted || "-")}</td>`;
        }
        case "active":
            return `<td class="user-main-active-cell">${renderActiveIcon(user.active)}</td>`;
        default:
            return `<td>${escapeHtml(user[column.key] ?? "")}</td>`;
    }
}

function renderRoleChip(roleId, roleLabel) {
    const label = roleLabel || labelForRoleId(roleId) || "Unknown";
    const roleClass = `role-${normalizeRoleClass(label)}`;
    return `<span class="user-main-role-pill ${escapeHtml(roleClass)}" title="${escapeHtml(label)}">${escapeHtml(label)}</span>`;
}

function renderActiveIcon(value) {
    return value
        ? `<span class="user-administration-active-state" title="Active" aria-label="Active"><span class="user-administration-active-dot is-active" aria-hidden="true"></span></span>`
        : `<span class="user-administration-active-state" title="Inactive" aria-label="Inactive"><span class="user-administration-active-dot is-inactive" aria-hidden="true"></span></span>`;
}

function getFieldValue(node, fieldName) {
    return getChildText(node, fieldName, "") || getDirectText(getDirectChild(node, fieldName)).trim();
}

function labelForRoleId(roleId) {
    switch (String(roleId || "").trim()) {
        case "1":
            return "Bepa system administrator";
        case "2":
            return "Customer Administrator";
        case "3":
            return "Project Member";
        case "4":
            return "Project viewer";
        default:
            return "Invalid User Role";
    }
}

function normalizeRoleClass(value) {
    return String(value || "")
        .trim()
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, "-")
        .replace(/^-+|-+$/g, "");
}

function parseBoolean(value) {
    const normalized = String(value || "").trim().toLowerCase();
    return ["true", "1", "yes", "ja", "y", "on"].includes(normalized);
}

function compareValues(left, right) {
    if (left instanceof Date && right instanceof Date) {
        return left.getTime() - right.getTime();
    }

    if (typeof left === "number" && typeof right === "number") {
        return left - right;
    }

    return String(left ?? "").localeCompare(String(right ?? ""), undefined, {
        numeric: true,
        sensitivity: "base"
    });
}

function formatDateTime(value) {
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

    return normalized.replace("T", " ");
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

    const parsed = Date.parse(normalized);

    return Number.isFinite(parsed) ? new Date(parsed) : new Date(0);
}

function widthToPixels(width, fallback) {
    const raw = String(width || "").trim();

    if (!raw) {
        return fallback;
    }

    if (/^\d+(?:\.\d+)?px$/i.test(raw)) {
        return Number.parseFloat(raw);
    }

    if (/^\d+$/.test(raw)) {
        return Number(raw);
    }

    return fallback;
}

function showEmptyState(message) {
    const empty = document.getElementById("listEmptyState");

    if (!empty) {
        return;
    }

    empty.textContent = message;
    empty.classList.add("is-visible");
}

function hideEmptyState() {
    const empty = document.getElementById("listEmptyState");

    empty?.classList.remove("is-visible");
}

function openEditUser(userId) {
    if (!userId) {
        return;
    }

    openEditDialog({
        page: "user-edit",
        mode: "edit",
        id: userId,
        title: "Edit User",
        onSaved: () => window.location.reload()
    });
}

