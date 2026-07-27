import { initMenu } from "../components/menu.js";
import { mountTopbar, applyTopbarMetadata } from "../components/topbar.js";
import { initHelpDialog } from "../components/help-dialog.js";
import { openEditDialog } from "../components/edit-dialog.js";
import { setText } from "../core/dom.js";
import { getAttribute, getChildText, hasXmlParseError } from "../core/xml.js";

const DASHBOARD_ENDPOINT = "/pro/dashboardirl?cmd=overview";
const SYSTEMS_BREAKDOWN_EDIT_PAGE_URL = "/web/view?page=systemsbreakdown-edit";

const state = {
    document: null,
    filterMode: "all",
    selectedTrlIds: [],
    selectedBlankTrl: false
};

document.addEventListener("DOMContentLoaded", () => {
    initializePageShell();
    initializeEvents();
    loadDashboard();
});

function initializePageShell() {
    mountTopbar();

    setText("customerName", "-", "");
    setText("projectName", "-", "");
    setText("userName", "-", "");
    setText("loadStatus", "Loading", "");

    initMenu();
    initHelpDialog();
}

function initializeEvents() {
    initializeFilterEvents();
    initializeTrlFilterEvents();
}

async function loadDashboard() {
    showEmptyState("Loading dashboard IRL...");
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

        state.document = parseDashboardDocument(xmlDocument);
        applyTopPanel(xmlDocument);
        renderDashboard(state.document);

        setText("loadStatus", "Loaded", "");
    } catch (error) {
        console.error("Failed to load dashboard IRL", error);
        setText("loadStatus", "Error", "");
        showEmptyState(`Could not load dashboard IRL. ${error.message}`);
    }
}

function parseDashboardDocument(xmlDocument) {
    const dashboardElement =
        xmlDocument.querySelector("DashboardIrlDocument > dashboardIrl")
        || xmlDocument.querySelector("dashboardIrl");

    if (!dashboardElement) {
        throw new Error("Missing dashboardIrl element.");
    }

    const lookup = {
        trlById: parseLookupMap(dashboardElement, "trlMeta > trl", "trlId"),
        irlById: parseLookupMap(dashboardElement, "irlMeta > irl", "irlId")
    };

    const structures = parsePhysicalStructures(dashboardElement, lookup);
    const irlTotalsById = buildIrlTotals(structures);

    return {
        title: "Dashboard IRL",
        structures,
        lookup,
        irlTotalsById
    };
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

function parsePhysicalStructures(dashboardElement, lookup) {
    const structureElements = Array.from(dashboardElement.querySelectorAll(":scope > physicalStructures > physicalStructure"));

    return structureElements.map((structureElement) => {
        const irlCounts = parseIrlCounts(structureElement);
        const trlId = getChildText(structureElement, "trlId", "");

        return {
            entityId: getChildText(structureElement, "entityId", ""),
            id: getChildText(structureElement, "id", ""),
            name: getChildText(structureElement, "name", ""),
            description: getChildText(structureElement, "description", ""),
            trlId,
            trlCode: resolveLookupCode(lookup.trlById, trlId),
            trlDescription: resolveLookupDescription(lookup.trlById, trlId),
            trlColor: resolveLookupColor(lookup.trlById, trlId),
            daysNextTrl: getChildText(structureElement, "daysNextTrl", ""),
            irlCounts
        };
    });
}

function parseIrlCounts(structureElement) {
    const counts = new Map();
    const countElements = Array.from(structureElement.querySelectorAll(":scope > irlCounts > irlCount"));

    for (const countElement of countElements) {
        const irlId = getChildText(countElement, "irlId", "");
        const count = Number.parseInt(getChildText(countElement, "count", "0"), 10);

        if (!irlId || Number.isNaN(count) || count <= 0) {
            continue;
        }

        const current = counts.get(irlId) || 0;
        counts.set(irlId, current + count);
    }

    return counts;
}

function buildIrlTotals(structures) {
    const totals = new Map();

    for (const structure of structures) {
        for (const [irlId, count] of structure.irlCounts.entries()) {
            totals.set(irlId, (totals.get(irlId) || 0) + count);
        }
    }

    return totals;
}

function applyTopPanel(xmlDocument) {
    if (!xmlDocument.querySelector("TopPanel")) {
        return;
    }

    applyTopbarMetadata(document, xmlDocument);
}

function renderDashboard(dashboard) {
    setText("dashboardIrlTitle", dashboard.title || "Dashboard IRL", "");

    const filterToggle = document.getElementById("dashboardIrlOverdueOnlyToggle");
    if (filterToggle) {
        filterToggle.checked = state.filterMode === "overdue";
    }

    const notOverdueToggle = document.getElementById("dashboardIrlNotOverdueOnlyToggle");
    if (notOverdueToggle) {
        notOverdueToggle.checked = state.filterMode === "not-overdue";
    }

    renderTrlFilterList(dashboard);

    const view = buildVisibleStructures(
        dashboard,
        state.filterMode,
        state.selectedTrlIds,
        state.selectedBlankTrl
    );

    setText(
        "dashboardIrlStructureCount",
        (state.filterMode === "all" && !state.selectedTrlIds.length && !state.selectedBlankTrl)
            ? String(dashboard.structures.length)
            : `${view.length} / ${dashboard.structures.length}`,
        ""
    );

    const tableHead = document.getElementById("dashboardIrlTableHead");
    const tableBody = document.getElementById("dashboardIrlTableBody");

    if (!tableHead || !tableBody) {
        throw new Error("Missing dashboard table elements.");
    }

    tableHead.innerHTML = "";
    tableBody.innerHTML = "";

    if (!dashboard.structures.length) {
        showEmptyState("No physical structures returned from endpoint.");
        return;
    }

    if (!view.length) {
        showEmptyState("No rows match the current filters.");
        return;
    }

    hideEmptyState();
    renderHeader(tableHead, dashboard);
    renderBody(tableBody, dashboard, view);
}

function renderHeader(tableHead, dashboard) {
    const headerRow = document.createElement("tr");

    const columns = [
        { key: "id", label: "SBS Code", sublabel: "" },
        { key: "name", label: "System Name", sublabel: "" },
        { key: "trl", label: "TRL", sublabel: `[1..${dashboard.lookup.trlById.size}]` },
        { key: "days", label: "Days to next TRL", sublabel: "" },
        { key: "interfaces", label: "Interfaces", sublabel: "" }
    ];

    for (const column of columns) {
        const th = document.createElement("th");
        th.className = `dashboard-irls-header-cell dashboard-irls-header-cell--${column.key}`;
        th.scope = "col";
        if (column.key === "trl" || column.key === "interfaces" || column.key === "days") {
            th.classList.add("dashboard-irls-header-cell--center");
        }
        th.appendChild(buildHeaderContent(column.label, column.sublabel));
        headerRow.appendChild(th);
    }

    for (const irl of dashboard.lookup.irlById.values()) {
        const th = document.createElement("th");
        th.className = "dashboard-irls-header-cell dashboard-irls-header-cell--irl dashboard-irls-header-cell--center";
        th.scope = "col";
        th.title = buildLookupTooltip(irl);

        th.appendChild(buildHeaderContent(`IRL ${irl.code || irl.id || "-"}`, `(${dashboard.irlTotalsById.get(irl.id) || 0})`));
        headerRow.appendChild(th);
    }

    tableHead.replaceChildren(headerRow);
}

function buildHeaderContent(label, sublabel) {
    const container = document.createElement("span");
    container.className = "dashboard-irls-header-content";

    const labelSpan = document.createElement("span");
    labelSpan.className = "dashboard-irls-header-label";
    labelSpan.textContent = label;

    container.appendChild(labelSpan);

    if (sublabel) {
        const sublabelSpan = document.createElement("span");
        sublabelSpan.className = "dashboard-irls-header-sublabel";
        sublabelSpan.textContent = sublabel;
        container.appendChild(sublabelSpan);
    }

    return container;
}

function renderBody(tableBody, dashboard, rows) {
    const fragment = document.createDocumentFragment();

    for (const structure of rows) {
        const tr = document.createElement("tr");
        tr.className = isOverdue(structure) ? "dashboard-irls-row dashboard-irls-row--overdue" : "dashboard-irls-row";
        tr.dataset.entityId = structure.entityId || "";
        if (structure.entityId) {
            tr.title = "Double-click to edit physical structure";
            tr.addEventListener("dblclick", () => {
                openPhysicalStructureEditPage(structure.entityId);
            });
        }

        tr.appendChild(buildTextCell("dashboard-irls-cell dashboard-irls-cell--id", structure.id, structure.description));
        tr.appendChild(buildTextCell("dashboard-irls-cell dashboard-irls-cell--name", structure.name, structure.description));
        tr.appendChild(buildTextCell(
            "dashboard-irls-cell dashboard-irls-cell--trl dashboard-irls-cell--center",
            structure.trlCode,
            structure.trlDescription,
            structure.trlColor
        ));
        tr.appendChild(buildDaysCell(structure.daysNextTrl));
        tr.appendChild(buildTextCell("dashboard-irls-cell dashboard-irls-cell--interfaces dashboard-irls-cell--numeric dashboard-irls-cell--center", String(sumCounts(structure.irlCounts)), "Interfaces"));

        for (const irl of dashboard.lookup.irlById.values()) {
            const count = structure.irlCounts.get(irl.id) || 0;
            tr.appendChild(buildTextCell(
                "dashboard-irls-cell dashboard-irls-cell--irl dashboard-irls-cell--numeric dashboard-irls-cell--center",
                count > 0 ? String(count) : "",
                buildLookupTooltip(irl)
            ));
        }

        fragment.appendChild(tr);
    }

    tableBody.replaceChildren(fragment);
}

function buildTextCell(className, value, title, color) {
    const cell = document.createElement("td");
    cell.className = className;
    const text = document.createElement("span");
    text.className = "dashboard-irls-cell-text";
    text.textContent = value || "";

    if (color) {
        text.classList.add("dashboard-irls-cell-text--pill");
        text.style.setProperty("--dashboard-irls-pill-color", color);
    }

    cell.appendChild(text);

    if (title) {
        cell.title = title;
    }

    return cell;
}

function buildDaysCell(value) {
    const cell = document.createElement("td");
    cell.className = "dashboard-irls-cell dashboard-irls-cell--days";

    const normalizedValue = String(value || "").trim();
    const isOverdueValue = isOverdueText(normalizedValue);

    if (!normalizedValue) {
        cell.textContent = "";
        return cell;
    }

    if (isOverdueValue) {
        const pill = document.createElement("span");
        pill.className = "dashboard-irls-pill dashboard-irls-pill--error";
        pill.textContent = normalizedValue;
        pill.title = normalizedValue;
        cell.appendChild(pill);
        cell.title = normalizedValue;
        return cell;
    }

    const pill = document.createElement("span");
    pill.className = "dashboard-irls-pill dashboard-irls-pill--neutral";
    pill.textContent = normalizedValue;
    pill.title = normalizedValue;
    cell.appendChild(pill);
    cell.title = normalizedValue;
    return cell;
}

function initializeFilterEvents() {
    const overdueToggle = document.getElementById("dashboardIrlOverdueOnlyToggle");
    const notOverdueToggle = document.getElementById("dashboardIrlNotOverdueOnlyToggle");

    overdueToggle?.addEventListener("change", () => {
        state.filterMode = overdueToggle.checked ? "overdue" : "all";

        if (overdueToggle.checked && notOverdueToggle) {
            notOverdueToggle.checked = false;
        }

        if (state.document) {
            renderDashboard(state.document);
        }
    });

    notOverdueToggle?.addEventListener("change", () => {
        state.filterMode = notOverdueToggle.checked ? "not-overdue" : "all";

        if (notOverdueToggle.checked && overdueToggle) {
            overdueToggle.checked = false;
        }

        if (state.document) {
            renderDashboard(state.document);
        }
    });
}

function initializeTrlFilterEvents() {
    const list = document.getElementById("dashboardIrlTrlFilterList");

    list?.addEventListener("change", (event) => {
        const target = event.target;

        if (!(target instanceof HTMLInputElement) || target.type !== "checkbox") {
            return;
        }

        const trlId = target.getAttribute("data-trl-id") || "";
        const isBlank = target.getAttribute("data-trl-blank") === "1";

        if (isBlank) {
            state.selectedBlankTrl = target.checked;
        } else if (trlId) {
            if (target.checked) {
                if (!state.selectedTrlIds.includes(trlId)) {
                    state.selectedTrlIds = [...state.selectedTrlIds, trlId];
                }
            } else {
                state.selectedTrlIds = state.selectedTrlIds.filter((value) => value !== trlId);
            }
        }

        if (state.document) {
            renderDashboard(state.document);
        }
    });
}

function renderTrlFilterList(dashboard) {
    const list = document.getElementById("dashboardIrlTrlFilterList");

    if (!list) {
        return;
    }

    list.innerHTML = "";

    list.appendChild(createFilterItem({
        label: "(blank)",
        checked: state.selectedBlankTrl,
        tooltip: "No TRL value",
        blank: true
    }));

    for (const trl of dashboard.lookup.trlById.values()) {
        list.appendChild(createFilterItem({
            label: trl.code || trl.id || "-",
            checked: state.selectedTrlIds.includes(trl.id),
            tooltip: trl.description || trl.code || trl.id || "",
            trlId: trl.id
        }));
    }
}

function createFilterItem({ label, checked, tooltip, trlId = "", blank = false }) {
    const item = document.createElement("label");
    item.className = "dashboard-irls-trl-filter-item";
    item.title = tooltip || label || "";

    const checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    checkbox.checked = Boolean(checked);

    if (blank) {
        checkbox.setAttribute("data-trl-blank", "1");
    } else {
        checkbox.setAttribute("data-trl-id", trlId);
    }

    const code = document.createElement("span");
    code.className = "dashboard-irls-trl-filter-code";
    code.textContent = label;

    item.append(code, checkbox);
    return item;
}

function buildVisibleStructures(dashboard, filterMode, selectedTrlIds = [], selectedBlankTrl = false) {
    const selectedTrlSet = new Set(selectedTrlIds || []);
    const hasTrlSelection = selectedTrlSet.size > 0 || selectedBlankTrl;

    return dashboard.structures.filter((structure) => {
        if (filterMode === "overdue" && !isOverdue(structure)) {
            return false;
        }

        if (filterMode === "not-overdue" && isOverdue(structure)) {
            return false;
        }

        if (!hasTrlSelection) {
            return true;
        }

        const trlId = String(structure?.trlId || "").trim();

        if (!trlId) {
            return selectedBlankTrl;
        }

        return selectedTrlSet.has(trlId);
    });
}

function isOverdue(structure) {
    const value = String(structure?.daysNextTrl || "").trim().toLowerCase();
    return value.startsWith("over due") || value.startsWith("overdue");
}

function isOverdueText(value) {
    const normalized = String(value || "").trim().toLowerCase();
    return normalized.startsWith("over due") || normalized.startsWith("overdue");
}

function sumCounts(countMap) {
    let total = 0;

    for (const count of countMap.values()) {
        total += count;
    }

    return total;
}

function resolveLookupCode(map, id) {
    if (!id) {
        return "";
    }

    return map?.get(id)?.code || id;
}

function resolveLookupDescription(map, id) {
    if (!id) {
        return "";
    }

    return map?.get(id)?.description || "";
}

function resolveLookupColor(map, id) {
    if (!id) {
        return "";
    }

    return map?.get(id)?.color || "";
}

function buildLookupTooltip(lookup) {
    if (!lookup) {
        return "";
    }

    return lookup.description || "";
}

function openPhysicalStructureEditPage(entityId) {
    const normalizedEntityId = String(entityId || "").trim();

    if (!normalizedEntityId) {
        return;
    }

    const editPage = new URL(SYSTEMS_BREAKDOWN_EDIT_PAGE_URL, window.location.href).searchParams.get("page") || "systemsbreakdown-edit";

    openEditDialog({
        page: editPage,
        mode: "edit",
        id: normalizedEntityId,
        title: "Edit Physical Structure",
        onSaved: () => window.location.reload()
    });
}

function showEmptyState(message) {
    const emptyState = document.getElementById("dashboardIrlEmptyState");

    if (!emptyState) {
        return;
    }

    emptyState.textContent = message;
    emptyState.classList.add("is-visible");
}

function hideEmptyState() {
    const emptyState = document.getElementById("dashboardIrlEmptyState");

    if (!emptyState) {
        return;
    }

    emptyState.classList.remove("is-visible");
}
