import { initMenu } from "../components/menu.js";
import { mountTopbar } from "../components/topbar.js";
import { setText } from "../core/dom.js";
import { fetchXml } from "../core/http.js";
import { escapeHtml } from "../core/html.js";
import { applyTopPanelFromDocument } from "../core/page-header.js";
import { getChildText, getDirectChild, getDirectChildren, hasXmlParseError } from "../core/xml.js";

const API_URL = "/api/admin/incidents";
const DEFAULT_LIMIT = 100;
const MAX_LIMIT = 1000;
const STORAGE_KEY_FILTER_TEXT = "adminIncidents.filterText";
const STORAGE_KEY_COLUMN_WIDTHS = "adminIncidents.table.columnWidths";
const MIN_COLUMN_WIDTH = 70;
const DEFAULT_COLUMN_WIDTHS = {
    incidentCreated: "165px",
    customer: "170px",
    project: "170px",
    user: "150px",
    service: "130px",
    severity: "120px",
    module: "160px",
    message: "260px"
};

const state = {
    currentDoc: null,
    topPanel: {
        customerName: "-",
        projectName: "-",
        userName: "-"
    },
    incidents: [],
    filteredIncidents: [],
    hoveredIncidentIndex: -1,
    columnWidths: loadColumnWidths(),
    loading: false
};

document.addEventListener("DOMContentLoaded", start);

function start() {
    initializeShell();
    initializeStateFromStorage();
    initializeEvents();
    loadIncidents();
}

function initializeShell() {
    setText("customerName", "-", "");
    setText("projectName", "-", "");
    setText("userName", "-", "");
    setText("loadStatus", "Loading", "");

    initMenu();
    mountTopbar(document);
}

function initializeStateFromStorage() {
    const filterInput = byId("filterIncidentText");

    if (filterInput) {
        filterInput.value = sessionStorage.getItem(STORAGE_KEY_FILTER_TEXT) || "";
    }
}

function initializeEvents() {
    const refreshButton = byId("btnRefreshIncidents");
    const countInput = byId("incidentCountInput");
    const filterInput = byId("filterIncidentText");
    const clearFilterButton = byId("btnClearFilter");
    const traceDialog = byId("incidentTraceDialog");
    const traceDialogClose = byId("incidentTraceDialogClose");
    const traceDialogOk = byId("incidentTraceDialogOk");

    refreshButton?.addEventListener("click", () => {
        loadIncidents();
    });

    countInput?.addEventListener("keydown", (event) => {
        if (event.key === "Enter") {
            event.preventDefault();
            loadIncidents();
        }
    });

    filterInput?.addEventListener("input", debounce(() => {
        persistFilter();
        applyFiltersAndRender();
    }, 120));

    filterInput?.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            filterInput.value = "";
            persistFilter();
            applyFiltersAndRender();
            filterInput.blur();
        }
    });

    clearFilterButton?.addEventListener("click", () => {
        if (filterInput) {
            filterInput.value = "";
        }

        persistFilter();
        applyFiltersAndRender();
    });

    traceDialogClose?.addEventListener("click", closeTraceDialog);
    traceDialogOk?.addEventListener("click", closeTraceDialog);

    traceDialog?.addEventListener("cancel", (event) => {
        event.preventDefault();
        closeTraceDialog();
    });

    traceDialog?.addEventListener("close", () => {
        closeTraceDialog();
    });
}

function isTraceDialogOpen() {
    const dialog = byId("incidentTraceDialog");

    return Boolean(dialog?.open);
}

async function loadIncidents() {
    if (state.loading) {
        return;
    }

    const limit = normalizeLimit();

    setLoading(true);
    showEmptyState("Loading incidents...");

    try {
        const url = new URL(API_URL, window.location.origin);
        url.searchParams.set("cmd", "list");
        url.searchParams.set("count", String(limit));

        const xmlDocument = await fetchXml(url.toString(), {
            cache: "no-store",
            credentials: "same-origin"
        });

        if (hasXmlParseError(xmlDocument)) {
            throw new Error("Incident endpoint returned invalid XML.");
        }

        state.currentDoc = xmlDocument;
        state.topPanel = parseTopPanel(xmlDocument);
        state.incidents = parseIncidents(xmlDocument);
        state.filteredIncidents = filterIncidents(state.incidents);
        state.hoveredIncidentIndex = -1;

        applyTopPanel();
        renderTable();
        updateRowCount();

        setText("loadStatus", "Loaded", "");
    } catch (error) {
        console.error("Failed to load incidents", error);
        state.currentDoc = null;
        state.incidents = [];
        state.filteredIncidents = [];
        state.hoveredIncidentIndex = -1;
        renderTable();
        updateRowCount();
        clearHoverState();
        setText("loadStatus", "Error", "");
        showEmptyState(`Could not load incidents. ${error.message || "Unknown error."}`);
    } finally {
        setLoading(false);
    }
}

function setLoading(isLoading) {
    state.loading = isLoading;

    const refreshButton = byId("btnRefreshIncidents");
    const countInput = byId("incidentCountInput");

    if (refreshButton) {
        refreshButton.disabled = isLoading;
    }

    if (countInput) {
        countInput.disabled = isLoading;
    }

    if (isLoading) {
        setText("loadStatus", "Loading", "");
    }
}

function normalizeLimit() {
    const countInput = byId("incidentCountInput");
    const parsed = Number.parseInt(String(countInput?.value || "").trim(), 10);
    const normalized = Number.isFinite(parsed) ? parsed : DEFAULT_LIMIT;
    const limit = Math.max(1, Math.min(normalized, MAX_LIMIT));

    if (countInput) {
        countInput.value = String(limit);
    }

    return limit;
}

function parseTopPanel(xmlDocument) {
    const topPanel = getDirectChild(xmlDocument.documentElement, "TopPanel");

    if (!topPanel) {
        return {
            customerName: "-",
            projectName: "-",
            userName: "-"
        };
    }

    return {
        customerName: getChildText(topPanel, "CustomerName", "-"),
        projectName: getChildText(topPanel, "ProjectName", "-"),
        userName: getChildText(topPanel, "Name", getChildText(topPanel, "UserName", "-")),
        workspaceEyebrow: getChildText(topPanel, "WorkspaceEyebrow", ""),
        workspaceHeading: getChildText(topPanel, "WorkspaceHeading", ""),
        workspaceHelpText: getChildText(topPanel, "WorkspaceHelpText", "")
    };
}

function persistFilter() {
    sessionStorage.setItem(STORAGE_KEY_FILTER_TEXT, byId("filterIncidentText")?.value || "");
}

function applyFiltersAndRender() {
    state.filteredIncidents = filterIncidents(state.incidents);
    state.hoveredIncidentIndex = -1;
    renderTable();
    updateRowCount();
}

function filterIncidents(incidents) {
    const filterText = String(byId("filterIncidentText")?.value || "").trim().toLowerCase();

    if (!filterText) {
        return [...incidents];
    }

    return incidents.filter((incident) => getIncidentFilterValues(incident)
        .some((value) => String(value || "").toLowerCase().includes(filterText)));
}

function getIncidentFilterValues(incident) {
    return [
        incident.incidentId,
        incident.logCreated,
        incident.customer,
        incident.project,
        incident.user,
        incident.serviceType,
        incident.severityType,
        incident.module,
        incident.message,
        incident.trace
    ];
}

function applyTopPanel() {
    if (state.currentDoc) {
        state.topPanel = applyTopPanelFromDocument(state.currentDoc, {
            customerName: "customerName",
            projectName: "projectName",
            userName: "userName",
            workspaceEyebrow: "pageEyebrow",
            workspaceHeading: "pageHeading",
            workspaceHelpText: "pageHelpText"
        }, { userTagNames: ["Name", "UserName"] });
        return;
    }

    setText("customerName", state.topPanel.customerName, "");
    setText("projectName", state.topPanel.projectName, "");
    setText("userName", state.topPanel.userName, "");
    setText("pageEyebrow", state.topPanel.workspaceEyebrow, "");
    setText("pageHeading", state.topPanel.workspaceHeading, "");
    setText("pageHelpText", state.topPanel.workspaceHelpText, "");
}

function parseIncidents(xmlDocument) {
    const incidentsElement = getDirectChild(xmlDocument.documentElement, "incidents");
    const incidentNodes = incidentsElement
        ? getDirectChildren(incidentsElement, "incident")
        : Array.from(xmlDocument.getElementsByTagName("incident"));

    return incidentNodes
        .map((node) => ({
            incidentId: parseNullableInt(getChildText(node, "IncidentId", "")),
            logCreated: getChildText(node, "LogCreated", ""),
            customer: getChildText(node, "Customer", ""),
            project: getChildText(node, "Project", ""),
            user: getChildText(node, "User", ""),
            serviceType: getChildText(node, "ServiceType", ""),
            severityType: getChildText(node, "SeverityType", ""),
            module: getChildText(node, "Module", ""),
            message: getChildText(node, "Message", ""),
            trace: getChildText(node, "Trace", "")
        }))
        .filter((incident) => incident.incidentId !== null);
}

function renderTable() {
    const tbody = byId("tbody");
    const colGroup = byId("incidentColGroup");
    const headerRow = byId("incidentHeaderRow");
    const table = byId("incidentTable");

    if (!tbody || !colGroup || !headerRow || !table) {
        return;
    }

    const columns = getIncidentColumns();
    const incidents = state.filteredIncidents;
    const totalWidth = columns.reduce((sum, column) => sum + widthToPixels(column.width, 180), 0);

    table.style.minWidth = `${Math.max(totalWidth, 1180)}px`;
    colGroup.innerHTML = columns.map((column) => `<col style="width: ${escapeHtml(column.width)};">`).join("");
    headerRow.innerHTML = columns.map((column) => `
        <th data-key="${escapeHtml(column.key)}" class="incident-main-resizable-th">
            <span class="incident-main-header-label">${escapeHtml(column.label)}</span>
            <span class="incident-main-column-resizer" data-resize-column="${escapeHtml(column.key)}" aria-hidden="true"></span>
        </th>
    `).join("");

    initializeColumnResize(headerRow, colGroup, table);

    tbody.innerHTML = incidents.map((incident, index) => renderRow(incident, index)).join("");

    tbody.querySelectorAll("tr[data-incident-index]").forEach((row) => {
        const incidentIndex = Number(row.getAttribute("data-incident-index"));
        const incident = Number.isFinite(incidentIndex) ? state.filteredIncidents[incidentIndex] : null;

        if (!incident) {
            return;
        }

        row.addEventListener("mouseenter", () => {
            state.hoveredIncidentIndex = incidentIndex;
            syncHoveredRow();
        });

        row.addEventListener("mouseleave", clearHoverState);

        row.addEventListener("dblclick", () => openTraceDialog(incidentIndex));
        row.addEventListener("keydown", (event) => {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                openTraceDialog(incidentIndex);
            }
        });

        row.tabIndex = 0;
    });

    syncHoveredRow();

    if (incidents.length === 0) {
        showEmptyState(state.incidents.length ? "No incidents match the current filter." : "No incidents found.");
        clearHoverState();
    } else {
        hideEmptyState();
    }

    updateRowCount();
}

function renderRow(incident, index) {
    const created = formatIncidentTimestamp(incident.logCreated, incident.logCreated || "-");
    const trace = incident.trace || "";

    return `
        <tr data-incident-index="${index}">
            <td title="${escapeHtml(trace)}">${escapeHtml(created)}</td>
            <td title="${escapeHtml(trace)}">${escapeHtml(incident.customer || "-")}</td>
            <td title="${escapeHtml(trace)}">${escapeHtml(incident.project || "-")}</td>
            <td title="${escapeHtml(trace)}">${escapeHtml(incident.user || "-")}</td>
            <td title="${escapeHtml(trace)}">${escapeHtml(incident.serviceType || "-")}</td>
            <td title="${escapeHtml(trace)}">${escapeHtml(incident.severityType || "-")}</td>
            <td title="${escapeHtml(trace)}">${escapeHtml(incident.module || "-")}</td>
            <td title="${escapeHtml(trace)}">${escapeHtml(incident.message || "-")}</td>
        </tr>
    `;
}

function formatIncidentTimestamp(rawValue, fallback = "—") {
    if (!rawValue) {
        return fallback;
    }

    const trimmed = String(rawValue).trim();

    if (!trimmed) {
        return fallback;
    }

    return trimmed
        .replace("T", " ")
        .replace(/\.\d+$/, "");
}

function getIncidentColumns() {
    return [
        { key: "incidentCreated", label: "Incident Created", width: state.columnWidths.incidentCreated || DEFAULT_COLUMN_WIDTHS.incidentCreated },
        { key: "customer", label: "Customer", width: state.columnWidths.customer || DEFAULT_COLUMN_WIDTHS.customer },
        { key: "project", label: "Project", width: state.columnWidths.project || DEFAULT_COLUMN_WIDTHS.project },
        { key: "user", label: "User", width: state.columnWidths.user || DEFAULT_COLUMN_WIDTHS.user },
        { key: "service", label: "Service", width: state.columnWidths.service || DEFAULT_COLUMN_WIDTHS.service },
        { key: "severity", label: "Severity", width: state.columnWidths.severity || DEFAULT_COLUMN_WIDTHS.severity },
        { key: "module", label: "Module", width: state.columnWidths.module || DEFAULT_COLUMN_WIDTHS.module },
        { key: "message", label: "Message", width: state.columnWidths.message || DEFAULT_COLUMN_WIDTHS.message }
    ];
}

function syncHoveredRow() {
    const tbody = byId("tbody");

    if (!tbody) {
        return;
    }

    tbody.querySelectorAll("tr[data-incident-index]").forEach((row) => {
        const rowIndex = Number(row.getAttribute("data-incident-index"));
        const isActive = rowIndex === state.hoveredIncidentIndex;
        row.classList.toggle("is-previewed", isActive);
        row.setAttribute("aria-selected", isActive ? "true" : "false");
    });
}

function clearHoverState() {
    if (state.hoveredIncidentIndex !== -1) {
        state.hoveredIncidentIndex = -1;
        syncHoveredRow();
    }

}

function closeTraceDialog() {
    const dialog = byId("incidentTraceDialog");

    if (!dialog) {
        return;
    }

    if (dialog.open && typeof dialog.close === "function") {
        dialog.close();
        return;
    }

    dialog.removeAttribute("open");
}

function openTraceDialog(index) {
    if (!Number.isFinite(index) || index < 0 || index >= state.filteredIncidents.length) {
        return;
    }

    const incident = state.filteredIncidents[index];
    const dialog = byId("incidentTraceDialog");
    const title = byId("incidentTraceDialogTitle");
    const meta = byId("incidentTraceDialogMeta");
    const body = byId("incidentTraceDialogBody");

    if (!dialog || !title || !meta || !body) {
        return;
    }

    title.textContent = `Incident ${incident.incidentId || "-"}`;
    meta.textContent = [
        incident.logCreated || "-",
        incident.customer || "-",
        incident.project || "-",
        incident.user || "-",
        incident.serviceType || "-",
        incident.severityType || "-",
        incident.module || "-"
    ].join(" | ");
    renderTraceDialogBody(body, incident.trace || "No trace available.");

    if (typeof dialog.showModal === "function") {
        dialog.showModal();
    } else {
        dialog.setAttribute("open", "open");
    }

    scrollTraceDialogToMatch(body);
}

function renderTraceDialogBody(body, traceText) {
    const filterText = String(byId("filterIncidentText")?.value || "").trim();

    if (!filterText) {
        body.textContent = traceText;
        return;
    }

    const lowerTrace = String(traceText || "").toLowerCase();
    const lowerFilter = filterText.toLowerCase();

    if (!lowerTrace.includes(lowerFilter)) {
        body.textContent = traceText;
        return;
    }

    body.innerHTML = highlightText(traceText, filterText);
}

function highlightText(text, term) {
    const source = String(text || "");
    const needle = String(term || "");

    if (!needle) {
        return escapeHtml(source);
    }

    const lowerSource = source.toLowerCase();
    const lowerNeedle = needle.toLowerCase();
    let cursor = 0;
    let html = "";

    while (cursor < source.length) {
        const index = lowerSource.indexOf(lowerNeedle, cursor);

        if (index < 0) {
            html += escapeHtml(source.slice(cursor));
            break;
        }

        html += escapeHtml(source.slice(cursor, index));
        html += `<mark class="incident-main-trace-match">${escapeHtml(source.slice(index, index + needle.length))}</mark>`;
        cursor = index + needle.length;
    }

    return html;
}

function scrollTraceDialogToMatch(body) {
    const match = body?.querySelector?.(".incident-main-trace-match");

    if (!match) {
        body.scrollTop = 0;
        return;
    }

    requestAnimationFrame(() => {
        match.scrollIntoView({ block: "center", inline: "nearest" });
    });
}

function initializeColumnResize(headerRow, colGroup, table) {
    const handles = Array.from(headerRow.querySelectorAll(".incident-main-column-resizer"));

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

            document.body.classList.add("incident-main-column-resizing");

            function onMouseMove(moveEvent) {
                const delta = moveEvent.clientX - startX;
                const nextWidth = Math.max(MIN_COLUMN_WIDTH, startWidth + delta);

                updateColumnWidth(columnKey, nextWidth, colGroup, table);
            }

            function onMouseUp(upEvent) {
                const delta = upEvent.clientX - startX;
                const nextWidth = Math.max(MIN_COLUMN_WIDTH, startWidth + delta);

                updateColumnWidth(columnKey, nextWidth, colGroup, table);
                persistColumnWidth(columnKey, nextWidth);

                document.body.classList.remove("incident-main-column-resizing");
                window.removeEventListener("mousemove", onMouseMove);
                window.removeEventListener("mouseup", onMouseUp);
            }

            window.addEventListener("mousemove", onMouseMove);
            window.addEventListener("mouseup", onMouseUp);
        });
    });
}

function updateColumnWidth(columnKey, widthPx, colGroup, table) {
    const width = `${Math.max(MIN_COLUMN_WIDTH, Math.round(widthPx))}px`;
    state.columnWidths[columnKey] = width;

    const columns = getIncidentColumns();
    const columnIndex = columns.findIndex((column) => column.key === columnKey);

    if (columnIndex < 0) {
        return;
    }

    const col = colGroup?.children?.[columnIndex];

    if (col) {
        col.style.width = width;
    }

    if (table) {
        const totalWidth = columns.reduce((sum, column) => {
            const nextWidth = column.key === columnKey ? width : column.width;
            return sum + widthToPixels(nextWidth, 180);
        }, 0);

        table.style.minWidth = `${Math.max(totalWidth, 1180)}px`;
    }
}

function persistColumnWidth(columnKey, widthPx) {
    const widths = loadColumnWidths();
    widths[columnKey] = `${Math.max(MIN_COLUMN_WIDTH, Math.round(widthPx))}px`;

    try {
        localStorage.setItem(STORAGE_KEY_COLUMN_WIDTHS, JSON.stringify(widths));
    } catch {
        // Ignore storage failures.
    }
}

function loadColumnWidths() {
    try {
        const raw = localStorage.getItem(STORAGE_KEY_COLUMN_WIDTHS);

        if (!raw) {
            return { ...DEFAULT_COLUMN_WIDTHS };
        }

        const parsed = JSON.parse(raw);

        if (!parsed || typeof parsed !== "object") {
            return { ...DEFAULT_COLUMN_WIDTHS };
        }

        return {
            ...DEFAULT_COLUMN_WIDTHS,
            ...parsed
        };
    } catch {
        return { ...DEFAULT_COLUMN_WIDTHS };
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

    const parsed = Number(raw);
    return Number.isFinite(parsed) ? parsed : fallback;
}

function showEmptyState(message) {
    const emptyState = byId("listEmptyState");

    if (!emptyState) {
        return;
    }

    emptyState.textContent = message;
    emptyState.classList.add("is-visible");
}

function hideEmptyState() {
    const emptyState = byId("listEmptyState");

    if (!emptyState) {
        return;
    }

    emptyState.classList.remove("is-visible");
}

function updateRowCount() {
    const tableCount = byId("incidentTableCount");
    const tbody = byId("tbody");

    const renderedRows = tbody ? tbody.querySelectorAll("tr[data-incident-index]").length : state.filteredIncidents.length;
    const totalRows = state.incidents.length;

    setText(tableCount, `${renderedRows} of ${totalRows}`, "");

    const table = byId("incidentTable");
    if (table) {
        table.dataset.filteredRowCount = String(renderedRows);
        table.dataset.totalRowCount = String(totalRows);
    }

    if (window.syncDataTableFooters) {
        window.syncDataTableFooters(document);
    }
}

function byId(id) {
    return document.getElementById(id);
}

function parseNullableInt(value) {
    const normalized = String(value || "").trim();

    if (!normalized) {
        return null;
    }

    const parsed = Number.parseInt(normalized, 10);
    return Number.isFinite(parsed) ? parsed : null;
}

function debounce(fn, delay) {
    let timeoutId = null;

    return (...args) => {
        window.clearTimeout(timeoutId);
        timeoutId = window.setTimeout(() => fn(...args), delay);
    };
}
