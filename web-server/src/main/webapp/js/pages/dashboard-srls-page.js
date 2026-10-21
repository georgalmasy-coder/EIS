import {userPreferences} from "../core/user-preferences.js";
import {initMenu, menuHasRoute} from "../components/menu.js";
import {mountTopbar} from "../components/topbar.js";
import {initHelpDialog} from "../components/help-dialog.js";
import {openEditDialog} from "../components/edit-dialog.js";
import {downloadDashboardSrlPdf} from "./dashboard-srls-pdf.js?v=20261021-pdf";
import {setText} from "../core/dom.js";
import {applyTopPanelFromDocument} from "../core/page-header.js";
import {hasXmlParseError} from "../core/xml.js";
import {parseSrlDashboard, srlPosition} from "./dashboard-srls-model.js?v=20261020-tooltips";

const ENDPOINT = "/master/srlcalculation?cmd=overview";
let loadedDashboard = null;
let loadedTopPanel = {};

document.addEventListener("DOMContentLoaded", () => {
    initializeColumnResize();
    mountTopbar();
    initMenu().then(() => {
        document.getElementById("btnInterfaceManagement").hidden = !menuHasRoute("/web/view?page=psys-interfaces");
    });
    initHelpDialog();
    document.getElementById("btnDownloadPdf").addEventListener("click", () => {
        if (!loadedDashboard) return;
        try {
            downloadDashboardSrlPdf({dashboard: loadedDashboard, topPanel: loadedTopPanel});
        } catch (error) {
            console.error("Failed to generate SRL PDF", error);
            setText("srlStatus", "Could not generate the SRL PDF. Please try again.");
        }
    });
    loadDashboard();
});

async function loadDashboard() {
    const workspace = document.getElementById("srlWorkspace");
    loadedDashboard = null;
    document.getElementById("btnDownloadPdf").disabled = true;
    workspace.hidden = true;
    setText("srlStatus", "Loading SRL calculations...");
    setText("loadStatus", "Loading");
    try {
        const response = await fetch(ENDPOINT, {headers: {Accept: "application/xml,text/xml"}, cache: "no-store"});
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const xml = new DOMParser().parseFromString(await response.text(), "application/xml");
        if (hasXmlParseError(xml)) throw new Error("Invalid XML response.");
        const dashboard = parseSrlDashboard(xml);
        // Older cached model modules do not return lookup codes yet.
        dashboard.levelCodes ??= new Map([...xml.querySelectorAll("srlCalculation > meta > srlMeta > srl")]
            .map(level => [Number(level.getAttribute("srlId")), level.getAttribute("code") || ""]));
        const topPanel = applyTopPanelFromDocument(xml, {
            customerName: "customerName", projectName: "projectName", userName: "userName",
            workspaceEyebrow: "pageEyebrow", workspaceHeading: "pageHeading", workspaceHelpText: "pageHelpText"
        }, {userTagNames: ["Name", "UserName"]});
        renderDashboard(dashboard);
        loadedDashboard = dashboard;
        loadedTopPanel = topPanel;
        document.getElementById("btnDownloadPdf").disabled = false;
        workspace.hidden = dashboard.rows.length === 0;
        setText("srlStatus", dashboard.rows.length === 0 ? "No active systems in this project."
            : dashboard.measuredCount === dashboard.rows.length ? "" : "Some systems do not have a calculated SRL level yet.", "");
        setText("loadStatus", "Loaded");
    } catch (error) {
        console.error("Failed to load dashboard SRL", error);
        setText("srlStatus", `Could not load SRL calculations. ${error.message} Reload the page to try again.`);
        setText("loadStatus", "Error");
    }
}

function renderDashboard(dashboard) {
    renderSummary(dashboard.summary, dashboard.levels, dashboard.levelCodes);
    const scale = document.getElementById("srlScale");
    scale.replaceChildren();
    for (const level of dashboard.levels) {
        const label = element("span", "", `SRL ${level}`);
        label.style.left = `${srlPosition(level, dashboard.levels)}%`;
        scale.append(label);
    }
    const fragment = document.createDocumentFragment();
    for (const [index, group] of dashboard.groups.entries()) {
        const header = document.createElement("tr");
        header.className = "dashboard-srls-group";
        const heading = element("td", "dashboard-srls-group-heading");
        heading.colSpan = 4;
        const toggle = element("button", "dashboard-srls-group-toggle");
        toggle.type = "button";
        toggle.setAttribute("aria-expanded", "false");
        const icon = element("span", "", "\u25b8");
        icon.setAttribute("aria-hidden", "true");
        const title = element("strong", "dashboard-srls-group-title");
        title.textContent = group.system.code;
        const count = element("span", "dashboard-srls-group-count", String(group.children.length));
        count.setAttribute("aria-label", `${group.children.length} underlying systems`);
        const label = element("span", "dashboard-srls-group-label");
        label.append(icon, title);
        const name = element("span", "dashboard-srls-group-name");
        name.append(element("strong", "dashboard-srls-name", group.system.name));
        name.title = group.system.name;
        const track = renderSystemTrack(group.summary ? Math.round(group.summary.average) : null,
            dashboard.levels, group.summary ? dashboard.levelCodes.get(Math.round(group.summary.average)) || "" : "");
        toggle.append(label, name, count, track);
        bindSystemEditor(header, group.system.entityId);
        heading.append(toggle);
        header.append(heading);
        fragment.append(header);
        const children = [];
        for (const system of group.children) {
            const row = document.createElement("tr");
            row.id = `srl-group-${index}-system-${children.length}`;
            row.hidden = true;
            children.push(row);
            const code = element("th", "dashboard-srls-code", system.code);
            code.scope = "row";
            const depth = (String(system.code).match(/\./g) || []).length;
            code.style.paddingLeft = `${32 + depth * 15}px`;
            bindSystemEditor(row, system.entityId);
            row.append(code, element("td", "dashboard-srls-name", system.name), document.createElement("td"));
            row.querySelector(".dashboard-srls-name").title = system.name;
            const valueCell = document.createElement("td");
            const description = dashboard.levelCodes.get(system.level) || "";
            const track = renderSystemTrack(system.level, dashboard.levels, description);
            valueCell.append(track);
            row.append(valueCell);
            fragment.append(row);
        }
        toggle.setAttribute("aria-controls", children.map(row => row.id).join(" "));
        toggle.addEventListener("click", () => {
            const expanded = toggle.getAttribute("aria-expanded") !== "true";
            toggle.setAttribute("aria-expanded", String(expanded));
            icon.textContent = expanded ? "\u25be" : "\u25b8";
            for (const row of children) row.hidden = !expanded;
        });
    }
    document.getElementById("srlSystems").replaceChildren(fragment);
    setText("srlSystemCount", `${dashboard.rows.length} systems · ${dashboard.measuredCount} calculated`);
}

function renderSystemTrack(level, levels, description) {
    const track = element("div", "dashboard-srls-system-track");
    if (level === null) {
        track.append(element("span", "dashboard-srls-unavailable", "Not calculated"));
        return track;
    }
    const position = `${srlPosition(level, levels)}%`;
    const line = element("span", "dashboard-srls-progress");
    line.style.width = position;
    const marker = element("span", "dashboard-srls-system-marker");
    marker.style.left = position;
    track.append(line, marker);
    track.setAttribute("role", "img");
    track.setAttribute("aria-label", description);
    track.title = description;
    return track;
}

function bindSystemEditor(row, entityId) {
    if (!entityId) return;
    row.title = "Double-click to edit physical architecture";
    row.addEventListener("dblclick", () => openEditDialog({
        page: "systemsbreakdown-edit",
        mode: "edit",
        id: String(entityId),
        title: "Edit Physical Architecture",
        onSaved: () => window.location.reload()
    }));
}

function renderSummary(summary, levels, levelCodes, container = document.getElementById("srlProjectSummary")) {
    container.replaceChildren();
    if (!summary) {
        container.append(element("span", "", "No calculated SRL levels available."));
        return;
    }
    container.append(element("span", "dashboard-srls-summary-line"));
    for (const [key, label] of [["lowest", "Low"], ["average", "Current Level"], ["highest", "High"]]) {
        const level = Math.round(summary[key]);
        const item = element("div", `dashboard-srls-summary-item dashboard-srls-summary-item--${key}`);
        item.style.left = `${srlPosition(level, levels)}%`;
        item.setAttribute("aria-label", `${label}: SRL ${level}`);
        item.title = levelCodes.get(level) || "";
        item.append(element("span", "dashboard-srls-project-marker"),
            element("span", "", label));
        container.append(item);
    }
}

function initializeColumnResize() {
    const preferenceKey = "dashboard.srls.columnWidths";
    const table = document.querySelector(".dashboard-srls-table");
    const minimums = {code: 100, name: 120, count: 140};
    const widths = {code: 160, name: 280, count: 140};
    try {
        const stored = JSON.parse(userPreferences.getItem(preferenceKey) || "{}");
        for (const key of Object.keys(widths)) {
            const value = Number(String(stored?.[key] ?? "").replace(/px$/, ""));
            if (Number.isFinite(value) && value >= minimums[key] && value <= 5000) widths[key] = value;
        }
    } catch { /* Invalid preferences use default widths. */ }
    const apply = () => {
        for (const [key, width] of Object.entries(widths)) table.style.setProperty(`--srl-${key}-width`, `${width}px`);
        table.style.minWidth = `${widths.code + widths.name + widths.count + 470}px`;
    };
    const save = () => userPreferences.setItem(preferenceKey, JSON.stringify(widths));
    apply();
    for (const header of table.querySelectorAll("th[data-resize-column]")) {
        const key = header.dataset.resizeColumn;
        const handle = element("span", "dashboard-srls-column-resizer");
        handle.tabIndex = 0;
        handle.setAttribute("role", "separator");
        handle.setAttribute("aria-orientation", "vertical");
        handle.setAttribute("aria-label", `Resize ${header.textContent}`);
        handle.title = "Drag to resize column";
        header.append(handle);
        handle.addEventListener("keydown", event => {
            if (!["ArrowLeft", "ArrowRight"].includes(event.key)) return;
            event.preventDefault();
            widths[key] = Math.max(minimums[key], Math.min(5000, widths[key] + (event.key === "ArrowRight" ? 10 : -10)));
            apply();
            save();
        });
        handle.addEventListener("mousedown", event => {
            if (event.button !== 0) return;
            event.preventDefault();
            event.stopPropagation();
            const startX = event.clientX;
            const startWidth = widths[key];
            document.body.classList.add("dashboard-srls-column-resizing");
            const move = event => {
                widths[key] = Math.max(minimums[key], Math.min(5000, Math.round(startWidth + event.clientX - startX)));
                apply();
            };
            const stop = () => {
                save();
                document.body.classList.remove("dashboard-srls-column-resizing");
                window.removeEventListener("mousemove", move);
                window.removeEventListener("mouseup", stop);
                window.removeEventListener("blur", stop);
            };
            window.addEventListener("mousemove", move);
            window.addEventListener("mouseup", stop);
            window.addEventListener("blur", stop);
        });
    }
}

function element(tag, className, text = "") {
    const node = document.createElement(tag);
    node.className = className;
    node.textContent = text;
    return node;
}
