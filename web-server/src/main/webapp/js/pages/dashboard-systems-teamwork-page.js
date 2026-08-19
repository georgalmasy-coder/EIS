import { initMenu } from "../components/menu.js";
import { initHelpDialog } from "../components/help-dialog.js";
import { mountTopbar } from "../components/topbar.js";
import { openEditDialog } from "../components/edit-dialog.js";
import { downloadDashboardSystemsTeamworkPdf } from "./dashboard-systems-teamwork-pdf.js";
import { setText } from "../core/dom.js";
import { applyTopPanelFromDocument as applyPageHeaderFromDocument } from "../core/page-header.js";
import { getAttribute, getChildText, hasXmlParseError } from "../core/xml.js";

const DASHBOARD_ENDPOINT = "/pro/systemsteamwork?cmd=overview";
const EDIT_PAGE_URL = "/web/view?page=systemsbreakdown-edit";
const STORAGE_KEY = "basis.dashboard.systems.teamwork.tableColumnWidths";
const FILTER_TYPES = ["trl", "owner", "department"];

const DEFAULT_COLUMN_WIDTHS = [84, 62, 170, 120, 112, 150, 140, 84, 62, 170, 120, 112];
const STICKY_COLUMN_COUNT = 5;
const MIN_COLUMN_WIDTH = 60;

const COLUMN_DEFINITIONS = [
    { key: "fromSbsCode", label: "SBS Code", source: "fromSbsCode", sticky: true, widthIndex: 0, editable: true },
    { key: "fromTrlId", label: "TRL", source: "fromTrlId", sticky: true, widthIndex: 1, lookup: "trl", editable: true, colored: true, center: true },
    { key: "fromSystemName", label: "System Name", source: "fromSystemName", sticky: true, widthIndex: 2, editable: true },
    { key: "fromSystemOwnerId", label: "System Owner", source: "fromSystemOwnerId", sticky: true, widthIndex: 3, lookup: "user", editable: true },
    { key: "fromSystemDepartmentId", label: "Department", source: "fromSystemDepartmentId", sticky: true, widthIndex: 4, lookup: "department", editable: true },
    { key: "interfaceClass", label: "Class", sublabel: "From -> To", widthIndex: 5, dual: true, type: "classification" },
    { key: "interfaceIrl", label: "IRL", sublabel: "From -> To", widthIndex: 6, dual: true, type: "irl" },
    { key: "toSbsCode", label: "SBS Code", source: "toSbsCode", widthIndex: 7, editable: true },
    { key: "toTrlId", label: "TRL", source: "toTrlId", widthIndex: 8, lookup: "trl", editable: true, colored: true, center: true },
    { key: "toSystemName", label: "System Name", source: "toSystemName", widthIndex: 9, editable: true },
    { key: "toSystemOwnerId", label: "System Owner", source: "toSystemOwnerId", widthIndex: 10, lookup: "user", editable: true },
    { key: "toSystemDepartmentId", label: "Department", source: "toSystemDepartmentId", widthIndex: 11, lookup: "department", editable: true }
];

const state = {
    document: null,
    topPanel: {
        customerName: "—",
        projectName: "—",
        userName: "—"
    },
    dashboard: null,
    columnWidths: [...DEFAULT_COLUMN_WIDTHS],
    filters: {
        trl: new Map(),
        owner: new Map(),
        department: new Map()
    }
};

document.addEventListener("DOMContentLoaded", () => {
    initializePageShell();
    loadDashboard();
});

function initializePageShell() {
    state.columnWidths = loadStoredColumnWidths();
    setText("customerName", "-", "");
    setText("projectName", "-", "");
    setText("userName", "-", "");
    setText("loadStatus", "Loading", "");

    mountTopbar(document);
    initMenu(document);
    initHelpDialog();

    const pdfButton = document.getElementById("btnDownloadPdf");
    pdfButton?.addEventListener("click", handleDownloadPdf);
}

async function loadDashboard() {
    showEmptyState("Loading systems teamwork...");
    setText("loadStatus", "Loading", "");

    try {
        const response = await fetch(DASHBOARD_ENDPOINT, {
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
            throw new Error("The dashboard endpoint returned invalid XML.");
        }

        state.document = xmlDocument;
        state.topPanel = parseTopPanel(xmlDocument);
        state.dashboard = parseDashboardDocument(xmlDocument);

        applyTopPanel(xmlDocument);
        renderDashboard(state.dashboard);
        setText("loadStatus", "Loaded", "");
    } catch (error) {
        console.error("Failed to load systems teamwork dashboard", error);
        setText("loadStatus", "Error", "");
        showEmptyState(`Could not load systems teamwork dashboard. ${error.message}`);
    }
}

function parseDashboardDocument(xmlDocument) {
    const dashboardElement =
        xmlDocument.querySelector("DashboardSystemsTeamworkDocument > dashboardIrl")
        || xmlDocument.querySelector("dashboardIrl");

    if (!dashboardElement) {
        throw new Error("Missing dashboardIrl element.");
    }

    const lookup = {
        trlById: parseLookupMap(dashboardElement, "trlMeta > trl", "trlId"),
        irlById: parseLookupMap(dashboardElement, "irlMeta > irl", "irlId"),
        classificationById: parseLookupMap(dashboardElement, "classificationMeta > classification", "classId"),
        userById: parseLookupMap(dashboardElement, "userMeta > user", "userId"),
        departmentById: parseLookupMap(dashboardElement, "departmentMeta > user", "userId")
    };

    const interfaces = parseInterfaces(dashboardElement);

    return {
        title: "Systems Teamwork",
        interfaces,
        lookup
    };
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
        userName: getChildText(topPanelElement, "Name", "—"),
        workspaceEyebrow: getChildText(topPanelElement, "WorkspaceEyebrow", ""),
        workspaceHeading: getChildText(topPanelElement, "WorkspaceHeading", ""),
        workspaceHelpText: getChildText(topPanelElement, "WorkspaceHelpText", "")
    };
}

function applyTopPanel(xmlDocument) {
    state.topPanel = applyPageHeaderFromDocument(xmlDocument, {
        customerName: "customerName",
        projectName: "projectName",
        userName: "userName",
        workspaceEyebrow: "pageEyebrow",
        workspaceHeading: "pageHeading",
        workspaceHelpText: "pageHelpText"
    }, { userTagNames: ["Name", "UserName"] });
}

function parseLookupMap(parentElement, selector, idAttribute) {
    const map = new Map();
    const elements = parentElement.querySelectorAll(`:scope > ${selector}`);

    for (const element of elements) {
        const id = getAttribute(element, idAttribute, "");

        if (!id) {
            continue;
        }

        map.set(id, {
            id,
            code: getAttribute(element, "code", id),
            description: getAttribute(element, "description", ""),
            color: getAttribute(element, "color", "")
        });
    }

    return map;
}

function parseInterfaces(dashboardElement) {
    const elements = Array.from(dashboardElement.querySelectorAll(":scope > interfaces > interface"));

    return elements.map((element) => ({
        fromEntityId: getChildText(element, "fromEntityId", ""),
        fromSbsCode: getChildText(element, "fromSbsCode", ""),
        fromSystemName: getChildText(element, "fromSystemName", ""),
        fromTrlId: getChildText(element, "fromTrlId", ""),
        fromSystemOwnerId: getChildText(element, "fromSystemOwnerId", ""),
        fromSystemDepartmentId: getChildText(element, "fromSystemDepartmentId", ""),
        fromIrlId: getChildText(element, "fromIrlId", ""),
        fromClassificationIds: getChildText(element, "fromClassificationIds", ""),
        toEntityId: getChildText(element, "toEntityId", ""),
        toSbsCode: getChildText(element, "toSbsCode", ""),
        toSystemName: getChildText(element, "toSystemName", ""),
        toTrlId: getChildText(element, "toTrlId", ""),
        toSystemOwnerId: getChildText(element, "toSystemOwnerId", ""),
        toSystemDepartmentId: getChildText(element, "toSystemDepartmentId", ""),
        toIrlId: getChildText(element, "toIrlId", ""),
        toClassificationIds: getChildText(element, "toClassificationIds", "")
    }));
}

function renderDashboard(dashboard) {
    const header = document.getElementById("dashboardSystemsTeamworkHeader");
    const tableBody = document.getElementById("dashboardSystemsTeamworkTableBody");
    const colGroup = document.getElementById("dashboardSystemsTeamworkColGroup");

    if (!header || !tableBody || !colGroup) {
        throw new Error("Missing systems teamwork table elements.");
    }

    header.innerHTML = "";
    tableBody.innerHTML = "";
    colGroup.innerHTML = "";

    renderColGroup(colGroup);
    renderHeader(header);
    renderFilterBar();

    const visibleRecords = filterInterfaces(dashboard.interfaces);
    renderBody(tableBody, dashboard, visibleRecords);
    updateTableFooter(visibleRecords.length, dashboard.interfaces.length);
    applyColumnWidths();
    initializeColumnResizers();
    applyHeaderLayout();

    if (!dashboard.interfaces.length) {
        showEmptyState("No interfaces returned from endpoint.");
    } else if (!visibleRecords.length) {
        showEmptyState("No rows match the current filters.");
    } else {
        hideEmptyState();
    }
}

function updateTableFooter(visibleCount, totalCount) {
    const count = document.getElementById("dashboardSystemsTeamworkTableCount");
    const visible = Number.isFinite(Number(visibleCount)) ? Number(visibleCount) : 0;
    const total = Number.isFinite(Number(totalCount)) ? Number(totalCount) : visible;

    setText(count, `${visible} of ${total}`, "");
}

function handleDownloadPdf() {
    if (!state.dashboard) {
        return;
    }

    const visibleRecords = filterInterfaces(state.dashboard.interfaces);
    const hasFiltersApplied = hasActiveFilters();

    downloadDashboardSystemsTeamworkPdf({
        topPanel: state.topPanel,
        dashboard: state.dashboard,
        records: visibleRecords,
        isFiltered: hasFiltersApplied
    });
}

function renderColGroup(colGroup) {
    for (const width of state.columnWidths) {
        const col = document.createElement("col");
        col.style.width = `${width}px`;
        colGroup.appendChild(col);
    }
}

function renderHeader(header) {
    const groupRow = document.createElement("div");
    groupRow.className = "dashboard-systems-teamwork-group-row";
    groupRow.appendChild(createGroupHeaderCell("From", 5));
    groupRow.appendChild(createGroupHeaderCell("Interface", 2));
    groupRow.appendChild(createGroupHeaderCell("To", 5));

    const columnRow = document.createElement("div");
    columnRow.className = "dashboard-systems-teamwork-column-row";

    COLUMN_DEFINITIONS.forEach((column, index) => {
        const cell = document.createElement("div");
        cell.className = `dashboard-systems-teamwork-header-cell dashboard-systems-teamwork-header-cell--${column.key}`;
        cell.style.width = `${state.columnWidths[index]}px`;
        cell.style.minWidth = `${state.columnWidths[index]}px`;
        cell.style.maxWidth = `${state.columnWidths[index]}px`;
        cell.classList.add("dashboard-systems-teamwork-resizable-th");

        if (column.dual) {
            cell.appendChild(buildHeaderContent(column.label, column.sublabel || ""));
        } else {
            cell.appendChild(buildHeaderContent(column.label, ""));
        }

        if (column.center) {
            cell.classList.add("dashboard-systems-teamwork-header-cell--center");
        }

        columnRow.appendChild(cell);
    });

    const groupRowCells = [
        createGroupHeaderCell("From", 5),
        createGroupHeaderCell("Interface", 2),
        createGroupHeaderCell("To", 5)
    ];

    groupRow.replaceChildren(...groupRowCells);
    header.replaceChildren(groupRow, columnRow);
}

function createGroupHeaderCell(label, colspan) {
    const cell = document.createElement("div");
    cell.className = "dashboard-systems-teamwork-group-header";
    cell.textContent = label;
    cell.style.gridColumn = `span ${colspan}`;
    return cell;
}

function buildHeaderContent(label, sublabel) {
    const container = document.createElement("span");
    container.className = "dashboard-systems-teamwork-header-content";

    const labelSpan = document.createElement("span");
    labelSpan.className = "dashboard-systems-teamwork-header-label";
    labelSpan.textContent = label;
    container.appendChild(labelSpan);

    if (sublabel) {
        const sublabelSpan = document.createElement("span");
        sublabelSpan.className = "dashboard-systems-teamwork-header-sublabel";
        sublabelSpan.textContent = sublabel;
        container.appendChild(sublabelSpan);
    }

    return container;
}

function renderBody(tableBody, dashboard, records) {
    const fragment = document.createDocumentFragment();

    for (const record of records) {
        const tr = document.createElement("tr");
        tr.className = "dashboard-systems-teamwork-row";

        COLUMN_DEFINITIONS.forEach((column, index) => {
            const td = column.dual
                ? buildDualCell(record, column, dashboard.lookup)
                : buildSingleCell(record, column, dashboard.lookup);

            tr.appendChild(td);
        });

        fragment.appendChild(tr);
    }

    tableBody.replaceChildren(fragment);
}

function buildSingleCell(record, column, lookup) {
    const td = document.createElement("td");
    td.className = `dashboard-systems-teamwork-cell dashboard-systems-teamwork-cell--${column.key}`;

    const rawValue = getRecordValue(record, column.source);
    const resolved = column.lookup
        ? resolveLookupValue(lookup[column.lookup === "department" ? "departmentById" : `${column.lookup}ById`], rawValue)
        : { label: rawValue || "--", title: rawValue || "--", color: "" };

    if (column.center) {
        td.classList.add("dashboard-systems-teamwork-cell--center");
    }

    if (column.editable) {
        td.classList.add("dashboard-systems-teamwork-cell--interactive");
        td.title = "Double-click to open edit page";
        td.addEventListener("dblclick", () => {
            openSystemsBreakdownEditPage(column.key.startsWith("to") ? record.toEntityId : record.fromEntityId);
        });
    }
    const filterType = getFilterTypeForColumn(column);
    if (filterType && rawValue) {
        td.classList.add("dashboard-systems-teamwork-cell--draggable");
        td.draggable = true;
        td.addEventListener("dragstart", (event) => {
            td.classList.add("is-dragging");
            event.dataTransfer.effectAllowed = "copy";
            event.dataTransfer.setData("text/plain", JSON.stringify({
                filterType,
                id: rawValue,
                label: resolved.label || rawValue,
                title: resolved.title || resolved.label || rawValue,
                color: resolved.color || ""
            }));
        });
        td.addEventListener("dragend", () => {
            td.classList.remove("is-dragging");
        });
    }

    if (column.lookup && resolved.color) {
        td.appendChild(buildLookupPill(resolved));
        td.title = resolved.title;
        return td;
    }

    const text = document.createElement("span");
    text.className = "dashboard-systems-teamwork-cell-text";
    text.textContent = resolved.label || "--";
    td.appendChild(text);
    td.title = resolved.title || resolved.label || "--";

    return td;
}

function buildDualCell(record, column, lookup) {
    const td = document.createElement("td");
    td.className = `dashboard-systems-teamwork-cell dashboard-systems-teamwork-cell--dual dashboard-systems-teamwork-cell--${column.key}`;

    const topRaw = getRecordValue(record, column.type === "classification" ? "fromClassificationIds" : "fromIrlId");
    const bottomRaw = getRecordValue(record, column.type === "classification" ? "toClassificationIds" : "toIrlId");

    const topResolved = column.type === "classification"
        ? resolveLookupList(lookup.classificationById, topRaw)
        : resolveLookupValue(lookup.irlById, topRaw);

    const bottomResolved = column.type === "classification"
        ? resolveLookupList(lookup.classificationById, bottomRaw)
        : resolveLookupValue(lookup.irlById, bottomRaw);

    const topLine = buildDualLine(topResolved, "right");
    const bottomLine = buildDualLine(bottomResolved, "left");

    const wrapper = document.createElement("div");
    wrapper.className = "dashboard-systems-teamwork-dual";

    wrapper.appendChild(topLine);
    wrapper.appendChild(bottomLine);
    td.appendChild(wrapper);

    const topTitle = topResolved.title || topResolved.label || "--";
    const bottomTitle = bottomResolved.title || bottomResolved.label || "--";
    td.title = `${topTitle}\n${bottomTitle}`;

    return td;
}

function buildDualLine(resolved, direction) {
    const line = document.createElement("div");
    line.className = "dashboard-systems-teamwork-dual-line";

    line.appendChild(buildArrow(direction));
    line.appendChild(resolved.color ? buildLookupPill(resolved) : buildDualText(resolved));
    return line;
}

function buildDualText(resolved) {
    const text = document.createElement("span");
    text.className = "dashboard-systems-teamwork-dual-text";
    text.textContent = resolved.label || "--";
    text.title = resolved.title || resolved.label || "--";
    return text;
}

function buildLookupPill(resolved) {
    const pill = document.createElement("span");
    pill.className = "dashboard-systems-teamwork-pill";
    pill.textContent = resolved.label || "--";
    pill.title = resolved.title || resolved.label || "--";
    if (resolved.color) {
        pill.style.setProperty("--dashboard-systems-teamwork-pill-color", resolved.color);
    }
    return pill;
}

function buildArrow(direction = "right") {
    const arrow = document.createElement("span");
    arrow.className = `dashboard-systems-teamwork-dual-arrow dashboard-systems-teamwork-dual-arrow--${direction}`;
    arrow.textContent = direction === "left" ? "\u2190" : "\u2192";
    return arrow;
}

function getRecordValue(record, key) {
    return String(record?.[key] || "").trim();
}

function renderFilterBar() {
    const zones = [
        {
            key: "trl",
            elementId: "dashboardSystemsTeamworkFilterTrl",
            placeholder: "Drop TRL here"
        },
        {
            key: "owner",
            elementId: "dashboardSystemsTeamworkFilterOwner",
            placeholder: "Drop System Owner here"
        },
        {
            key: "department",
            elementId: "dashboardSystemsTeamworkFilterDepartment",
            placeholder: "Drop Department here"
        }
    ];

    zones.forEach((zone) => {
        const zoneElement = document.getElementById(zone.elementId);
        if (!zoneElement) {
            return;
        }

        const selected = state.filters[zone.key];
        zoneElement.innerHTML = "";
        zoneElement.classList.toggle("is-active", selected.size > 0);
        zoneElement.classList.remove("is-drop-target");

        if (!selected.size) {
            const placeholder = document.createElement("span");
            placeholder.className = "dashboard-systems-teamwork-filter-placeholder";
            placeholder.textContent = zone.placeholder;
            zoneElement.appendChild(placeholder);
        } else {
            for (const [id, item] of selected.entries()) {
                zoneElement.appendChild(buildFilterChip(zone.key, id, item));
            }
        }

        zoneElement.ondragenter = handleFilterZoneDragEnter;
        zoneElement.ondragover = handleFilterZoneDragOver;
        zoneElement.ondragleave = handleFilterZoneDragLeave;
        zoneElement.ondrop = handleFilterZoneDrop;
    });
}

function buildFilterChip(filterType, id, item) {
    const chip = document.createElement("span");
    chip.className = "dashboard-systems-teamwork-filter-chip";
    chip.draggable = true;
    chip.dataset.filterType = filterType;
    chip.dataset.filterId = id;

    if (item?.color) {
        chip.style.setProperty("--dashboard-systems-teamwork-filter-chip-color", item.color);
    }

    const label = document.createElement("span");
    label.className = "dashboard-systems-teamwork-filter-chip-label";
    label.textContent = item?.label || id || "--";
    chip.appendChild(label);

    const removeButton = document.createElement("button");
    removeButton.type = "button";
    removeButton.className = "dashboard-systems-teamwork-filter-chip-remove";
    removeButton.setAttribute("aria-label", `Remove ${item?.label || id || "filter"}`);
    removeButton.textContent = "x";
    removeButton.addEventListener("click", () => {
        removeFilterValue(filterType, id);
    });
    chip.appendChild(removeButton);

    chip.addEventListener("dragstart", (event) => {
        chip.classList.add("is-dragging");
        event.dataTransfer.effectAllowed = "move";
        event.dataTransfer.setData("text/plain", JSON.stringify({
            filterType,
            id,
            label: item?.label || id || "--",
            title: item?.title || item?.label || id || "--",
            color: item?.color || ""
        }));
    });

    chip.addEventListener("dragend", () => {
        chip.classList.remove("is-dragging");
    });

    return chip;
}
function handleFilterZoneDragEnter(event) {
    event.preventDefault();
    event.currentTarget?.classList.add("is-drop-target");
}

function handleFilterZoneDragOver(event) {
    event.preventDefault();

    if (event.dataTransfer) {
        event.dataTransfer.dropEffect = "copy";
    }

    event.currentTarget?.classList.add("is-drop-target");
}

function handleFilterZoneDragLeave(event) {
    const zone = event.currentTarget;

    if (!zone) {
        return;
    }

    if (event.relatedTarget && zone.contains(event.relatedTarget)) {
        return;
    }

    zone.classList.remove("is-drop-target");
}

function handleFilterZoneDrop(event) {
    event.preventDefault();

    const zone = event.currentTarget;
    zone?.classList.remove("is-drop-target");

    const filterType = zone?.dataset.filterType || "";
    const payload = parseDragPayload(event.dataTransfer?.getData("text/plain") || "");

    if (!FILTER_TYPES.includes(filterType) || payload.filterType !== filterType || !payload.id) {
        return;
    }

    addFilterValue(filterType, payload.id, payload.label, payload.title, payload.color);
}

function parseDragPayload(rawValue) {
    if (!rawValue) {
        return { filterType: "", id: "", label: "", title: "", color: "" };
    }

    try {
        const parsed = JSON.parse(rawValue);
        return {
            filterType: String(parsed.filterType || ""),
            id: String(parsed.id || ""),
            label: String(parsed.label || ""),
            title: String(parsed.title || ""),
            color: String(parsed.color || "")
        };
    } catch {
        return { filterType: "", id: "", label: "", title: "", color: "" };
    }
}

function addFilterValue(filterType, id, label, title, color) {
    if (!FILTER_TYPES.includes(filterType) || !id) {
        return;
    }

    const current = state.filters[filterType];
    if (current.has(id)) {
        return;
    }

    current.set(id, {
        label: label || id,
        title: title || label || id,
        color: color || ""
    });

    if (state.dashboard) {
        renderDashboard(state.dashboard);
    }
}

function removeFilterValue(filterType, id) {
    if (!FILTER_TYPES.includes(filterType)) {
        return;
    }

    state.filters[filterType].delete(id);
    if (state.dashboard) {
        renderDashboard(state.dashboard);
    }
}

function clearAllFilters() {
    FILTER_TYPES.forEach((filterType) => {
        state.filters[filterType].clear();
    });

    if (state.dashboard) {
        renderDashboard(state.dashboard);
    }
}

function filterInterfaces(records) {
    return records.filter((record) => {
        return matchesFilter(record, "trl", "fromTrlId", "toTrlId")
            && matchesFilter(record, "owner", "fromSystemOwnerId", "toSystemOwnerId")
            && matchesFilter(record, "department", "fromSystemDepartmentId", "toSystemDepartmentId");
    });
}

function hasActiveFilters() {
    return FILTER_TYPES.some((filterType) => {
        const selected = state.filters[filterType];
        return Boolean(selected && selected.size > 0);
    });
}

function matchesFilter(record, filterType, fromKey, toKey) {
    const selected = state.filters[filterType];

    if (!selected || !selected.size) {
        return true;
    }

    const fromValue = getRecordValue(record, fromKey);
    const toValue = getRecordValue(record, toKey);

    return Boolean(fromValue && selected.has(fromValue)) || Boolean(toValue && selected.has(toValue));
}

function getFilterTypeForColumn(column) {
    if (!column?.lookup) {
        return "";
    }

    if (column.lookup === "trl") {
        return "trl";
    }

    if (column.lookup === "user") {
        return "owner";
    }

    if (column.lookup === "department") {
        return "department";
    }

    return "";
}
function resolveLookupValue(map, id) {
    const normalizedId = String(id || "").trim();

    if (!normalizedId) {
        return { label: "--", title: "--", color: "" };
    }

    const lookup = map?.get(normalizedId);

    return {
        label: lookup?.code || normalizedId,
        title: lookup?.description || lookup?.code || normalizedId,
        color: lookup?.color || ""
    };
}

function resolveLookupList(map, rawValue) {
    const ids = String(rawValue || "")
        .split(",")
        .map((value) => value.trim())
        .filter(Boolean);

    if (!ids.length) {
        return { label: "--", title: "--", color: "" };
    }

    const items = ids.map((id) => resolveLookupValue(map, id));

    return {
        label: items.map((item) => item.label).join(", "),
        title: items.map((item) => item.title).join(", "),
        color: ""
    };
}

function applyHeaderLayout() {
    const header = document.getElementById("dashboardSystemsTeamworkHeader");
    const groupRow = header?.querySelector(".dashboard-systems-teamwork-group-row");
    const columnRow = header?.querySelector(".dashboard-systems-teamwork-column-row");

    if (!header || !groupRow || !columnRow) {
        return;
    }

    const template = state.columnWidths.map((width) => `${width}px`).join(" ");
    const totalWidth = state.columnWidths.reduce((sum, width) => sum + width, 0);

    header.style.width = `${totalWidth}px`;
    header.style.minWidth = `${totalWidth}px`;
    header.style.maxWidth = `${totalWidth}px`;
    groupRow.style.gridTemplateColumns = template;
    columnRow.style.gridTemplateColumns = template;
}

function initializeColumnResizers() {
    document.querySelectorAll(".dashboard-systems-teamwork-resizer").forEach((node) => node.remove());

    COLUMN_DEFINITIONS.forEach((column, index) => {
        const headerCell = document.querySelector(`.dashboard-systems-teamwork-header-cell--${column.key}`);

        if (!headerCell) {
            return;
        }

        if (headerCell.querySelector(":scope > .dashboard-systems-teamwork-resizer")) {
            return;
        }

        const handle = document.createElement("span");
        handle.className = "dashboard-systems-teamwork-resizer";
        handle.setAttribute("aria-hidden", "true");

        handle.addEventListener("click", (event) => {
            event.preventDefault();
            event.stopPropagation();
        });

        handle.addEventListener("mousedown", (event) => {
            event.preventDefault();
            event.stopPropagation();

            startColumnResize(event, index, headerCell);
        });

        headerCell.appendChild(handle);
    });
}

function startColumnResize(event, columnIndex, headerCell) {
    const startX = event.clientX;
    const startWidth = state.columnWidths[columnIndex];

    document.body.classList.add("dashboard-systems-teamwork-column-resizing");

    function onMouseMove(moveEvent) {
        const delta = moveEvent.clientX - startX;
        const nextWidth = Math.max(MIN_COLUMN_WIDTH, startWidth + delta);
        updateColumnWidth(columnIndex, nextWidth);
    }

    function onMouseUp(upEvent) {
        const delta = upEvent.clientX - startX;
        const nextWidth = Math.max(MIN_COLUMN_WIDTH, startWidth + delta);

        updateColumnWidth(columnIndex, nextWidth);
        persistColumnWidths();

        document.body.classList.remove("dashboard-systems-teamwork-column-resizing");
        window.removeEventListener("mousemove", onMouseMove);
        window.removeEventListener("mouseup", onMouseUp);
    }

    window.addEventListener("mousemove", onMouseMove);
    window.addEventListener("mouseup", onMouseUp);
}

function updateColumnWidth(columnIndex, widthPx) {
    state.columnWidths = state.columnWidths.map((value, index) => index === columnIndex ? Math.max(MIN_COLUMN_WIDTH, Math.round(widthPx)) : value);

    const col = document.querySelector(`#dashboardSystemsTeamworkColGroup col:nth-child(${columnIndex + 1})`);
    if (col) {
        col.style.width = `${state.columnWidths[columnIndex]}px`;
    }

    const table = document.querySelector(".dashboard-systems-teamwork-table");
    if (table) {
        const totalWidth = state.columnWidths.reduce((sum, value) => sum + value, 0);
        table.style.width = `${totalWidth}px`;
        table.style.minWidth = `${totalWidth}px`;
    }

    applyColumnWidths();
    applyHeaderLayout();
}

function loadStoredColumnWidths() {
    const stored = getStoredColumnWidths();

    if (!Array.isArray(stored) || stored.length !== DEFAULT_COLUMN_WIDTHS.length) {
        return [...DEFAULT_COLUMN_WIDTHS];
    }

    return DEFAULT_COLUMN_WIDTHS.map((fallback, index) => {
        const parsed = Number(stored[index]);
        return Number.isFinite(parsed) && parsed >= MIN_COLUMN_WIDTH ? Math.round(parsed) : fallback;
    });
}

function applyColumnWidths() {
    COLUMN_DEFINITIONS.forEach((column, index) => {
        const width = `${state.columnWidths[index]}px`;
        document.querySelectorAll(`.dashboard-systems-teamwork-header-cell--${column.key}`).forEach((cell) => {
            cell.style.width = width;
            cell.style.minWidth = width;
            cell.style.maxWidth = width;
        });

        document.querySelectorAll(`.dashboard-systems-teamwork-cell--${column.key}`).forEach((cell) => {
            cell.style.width = width;
            cell.style.minWidth = width;
            cell.style.maxWidth = width;
        });

        const col = document.querySelector(`#dashboardSystemsTeamworkColGroup col:nth-child(${index + 1})`);
        if (col) {
            col.style.width = width;
        }
    });

    const totalWidth = state.columnWidths.reduce((sum, width) => sum + width, 0);
    const table = document.querySelector(".dashboard-systems-teamwork-table");
    if (table) {
        table.style.width = `${totalWidth}px`;
        table.style.minWidth = `${totalWidth}px`;
    }

    const header = document.getElementById("dashboardSystemsTeamworkHeader");
    if (header) {
        header.style.width = `${totalWidth}px`;
        header.style.minWidth = `${totalWidth}px`;
        header.style.maxWidth = `${totalWidth}px`;
    }
}

function getStoredColumnWidths() {
    try {
        const raw = localStorage.getItem(STORAGE_KEY);

        if (!raw) {
            return null;
        }

        const parsed = JSON.parse(raw);

        return Array.isArray(parsed) ? parsed : null;
    } catch {
        return null;
    }
}

function persistColumnWidths() {
    try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(state.columnWidths));
    } catch {
        // Ignore persistence errors.
    }
}

function openSystemsBreakdownEditPage(entityId) {
    const normalizedEntityId = String(entityId || "").trim();

    if (!normalizedEntityId) {
        return;
    }

    openEditDialog({
        page: "systemsbreakdown-edit",
        mode: "edit",
        id: normalizedEntityId,
        title: "Edit System Breakdown",
        onSaved: () => window.location.reload()
    });
}

function showEmptyState(message) {
    const emptyState = document.getElementById("dashboardSystemsTeamworkEmptyState");

    if (!emptyState) {
        return;
    }

    emptyState.textContent = message;
    emptyState.classList.add("is-visible");
}

function hideEmptyState() {
    const emptyState = document.getElementById("dashboardSystemsTeamworkEmptyState");

    if (!emptyState) {
        return;
    }

    emptyState.classList.remove("is-visible");
}

