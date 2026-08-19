import { initMenu } from "../components/menu.js";
import { initHelpDialog } from "../components/help-dialog.js";
import { mountTopbar } from "../components/topbar.js";
import { setText } from "../core/dom.js";
import { applyTopPanel as applyPageHeader, parseTopPanel as parsePageTopPanel } from "../core/page-header.js";
import { hasXmlParseError } from "../core/xml.js";

const API_URL = "/basis/baseline";
const DETAIL_PAGE_URL = "/web/view?page=baseline-detail";

const state = {
    baselines: [],
    selectedBaselinePK: null,
    sortKey: "changedDateTime",
    sortDirection: "desc",
    saving: false,
    currentDoc: null,
    topPanel: {
        customerName: "—",
        projectName: "—",
        userName: "—"
    }
};

const els = {};

document.addEventListener("DOMContentLoaded", initialize);

function initialize() {
    initializeShell();
    collectElements();
    bindEvents();
    loadBaselines();
}

function initializeShell() {
    setText("customerName", "—", "");
    setText("projectName", "—", "");
    setText("userName", "—", "");
    setText("loadStatus", "Loading", "");

    initMenu(document);
    mountTopbar(document);
    initHelpDialog();
}

function collectElements() {
    els.loadStatus = document.getElementById("loadStatus");
    els.baselineCount = document.getElementById("baselineCount");
    els.baselineBody = document.getElementById("baselineBody");
    els.baselineEmpty = document.getElementById("baselineEmpty");
    els.baselineHeaderRow = document.getElementById("baselineHeaderRow");

    els.btnAddBaseline = document.getElementById("btnAddBaseline");

    els.baselineDialog = document.getElementById("baselineDialog");
    els.baselineForm = document.getElementById("baselineForm");
    els.baselineDialogStatus = document.getElementById("baselineDialogStatus");
    els.fieldTagName = document.getElementById("fieldTagName");
    els.fieldDescription = document.getElementById("fieldDescription");
    els.btnSaveBaseline = document.getElementById("btnSaveBaseline");
    els.btnCancelBaseline = document.getElementById("btnCancelBaseline");
}

function bindEvents() {
    if (els.btnAddBaseline) {
        els.btnAddBaseline.addEventListener("click", openAddBaselineDialog);
    }

    if (els.btnCancelBaseline) {
        els.btnCancelBaseline.addEventListener("click", closeAddBaselineDialog);
    }

    if (els.btnSaveBaseline) {
        els.btnSaveBaseline.addEventListener("click", saveBaseline);
    }

    if (els.baselineHeaderRow) {
        els.baselineHeaderRow.querySelectorAll("th[data-key]").forEach(function (headerCell) {
            headerCell.addEventListener("click", function () {
                changeSort(headerCell.getAttribute("data-key"));
            });
        });
    }

    if (els.baselineDialog) {
        els.baselineDialog.addEventListener("cancel", function () {
            clearDialogStatus();
        });
    }
}

async function loadBaselines() {
    setLoadStatus("Loading");
    showEmpty("Loading baselines…");

    try {
        const doc = await fetchXml(`${API_URL}?cmd=list`);

        state.currentDoc = doc;
        state.topPanel = parsePageTopPanel(doc);
        state.baselines = parseBaselines(doc);

        applyTopPanel();
        applySortAndRender();

        setLoadStatus("Loaded");
    } catch (error) {
        console.error(error);
        state.baselines = [];
        renderBaselines([]);
        setLoadStatus("Error");
        showEmpty("Could not load baselines.");
    }
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
        customerName: firstTextOf(topPanelElement, [
            "CustomerName",
            "customerName"
        ]) || "—",
        projectName: firstTextOf(topPanelElement, [
            "ProjectName",
            "projectName"
        ]) || "—",
        userName: firstTextOf(topPanelElement, [
            "Name",
            "UserName",
            "userName",
            "name"
        ]) || "—"
    };
}

function applyTopPanel() {
    applyPageHeader(state.topPanel, {
        customerName: "customerName",
        projectName: "projectName",
        userName: "userName",
        workspaceEyebrow: "pageEyebrow",
        workspaceHeading: "pageHeading",
        workspaceHelpText: "pageHelpText"
    });
}

function parseBaselines(doc) {
    return Array.from(doc.querySelectorAll("baselineList > baselines > baseline"))
        .map(function (baselineElement) {
            return {
                baselinePK: intValue(textOf(baselineElement, "baselinePK")),
                customerId: intValue(textOf(baselineElement, "customerId")),
                projectId: intValue(textOf(baselineElement, "projectId")),
                tagName: textOf(baselineElement, "tagName"),
                description: textOf(baselineElement, "description"),
                changedByUserId: intValue(textOf(baselineElement, "changedByUserId")),
                changedBy: textOf(baselineElement, "changedBy"),
                changedDateTime: textOf(baselineElement, "changedDateTime")
            };
        });
}

function applySortAndRender() {
    const sorted = [...state.baselines].sort(function (a, b) {
        const aValue = sortableValue(a[state.sortKey]);
        const bValue = sortableValue(b[state.sortKey]);

        if (aValue < bValue) {
            return state.sortDirection === "asc" ? -1 : 1;
        }

        if (aValue > bValue) {
            return state.sortDirection === "asc" ? 1 : -1;
        }

        return 0;
    });

    renderBaselines(sorted);
    updateSortIndicators();
}

function renderBaselines(baselines) {
    clear(els.baselineBody);

    if (els.baselineCount) {
        els.baselineCount.textContent = `${baselines.length} of ${state.baselines.length}`;
    }

    if (!baselines.length) {
        showEmpty("No baselines have been created.");
        return;
    }

    hideEmpty();

    baselines.forEach(function (baseline) {
        const row = document.createElement("tr");
        row.dataset.baselinePk = String(baseline.baselinePK || "");

        if (baseline.baselinePK === state.selectedBaselinePK) {
            row.classList.add("is-selected");
        }

        row.appendChild(cell(baseline.tagName));
        row.appendChild(cell(baseline.description, "baseline-description-cell"));
        row.appendChild(cell(baseline.changedBy || fallbackUserText(baseline.changedByUserId)));
        row.appendChild(cell(formatDanishDateTime(baseline.changedDateTime)));

        row.addEventListener("click", function () {
            state.selectedBaselinePK = baseline.baselinePK;
            renderBaselines(baselines);
        });

        row.addEventListener("dblclick", function () {
            openBaselineDetail(baseline.baselinePK);
        });

        els.baselineBody.appendChild(row);
    });
}

function changeSort(key) {
    if (!key) {
        return;
    }

    if (state.sortKey === key) {
        state.sortDirection = state.sortDirection === "asc" ? "desc" : "asc";
    } else {
        state.sortKey = key;
        state.sortDirection = key === "changedDateTime" ? "desc" : "asc";
    }

    applySortAndRender();
}

function updateSortIndicators() {
    document.querySelectorAll(".sort-indicator").forEach(function (indicator) {
        indicator.textContent = "";
    });

    const activeIndicator = document.getElementById(`si-${state.sortKey}`);

    if (activeIndicator) {
        activeIndicator.textContent = state.sortDirection === "asc" ? "▲" : "▼";
    }
}

function openBaselineDetail(baselinePK) {
    if (!baselinePK) {
        return;
    }

    const url = new URL(DETAIL_PAGE_URL, window.location.href);
    url.searchParams.set("baselinePK", String(baselinePK));
    url.searchParams.set("returnUrl", "/web/view?page=baseline-main");

    window.location.href = url.toString();
}

function openAddBaselineDialog() {
    if (!els.baselineDialog) {
        return;
    }

    if (els.baselineForm) {
        els.baselineForm.reset();
    }

    clearDialogStatus();

    if (els.baselineDialog.showModal) {
        els.baselineDialog.showModal();
    } else {
        els.baselineDialog.setAttribute("open", "open");
    }

    if (els.fieldTagName) {
        els.fieldTagName.focus();
    }
}

function closeAddBaselineDialog() {
    if (!els.baselineDialog) {
        return;
    }

    clearDialogStatus();

    if (els.baselineDialog.close) {
        els.baselineDialog.close();
    } else {
        els.baselineDialog.removeAttribute("open");
    }
}

async function saveBaseline() {
    if (state.saving) {
        return;
    }

    const tagName = (els.fieldTagName?.value || "").trim();
    const description = (els.fieldDescription?.value || "").trim();

    const validationMessage = validateBaseline(
        tagName,
        description
    );

    if (validationMessage) {
        setDialogStatus(validationMessage, true);
        return;
    }

    state.saving = true;
    setDialogStatus("Saving...");
    setLoadStatus("Saving");

    try {
        await postXml(`${API_URL}?cmd=save`, buildSaveXml(
            tagName,
            description
        ));

        closeAddBaselineDialog();
        await loadBaselines();
        setLoadStatus("Loaded");
    } catch (error) {
        console.error(error);
        setDialogStatus(error.message || "Baseline could not be saved.", true);
        setLoadStatus("Error");
    } finally {
        state.saving = false;
    }
}

function validateBaseline(
    tagName,
    description
) {
    if (!tagName) {
        return "Tag-name is required.";
    }

    if (tagName.length > 150) {
        return "Tag-name must be maximum 150 characters.";
    }

    if (!description) {
        return "Description is required.";
    }

    return "";
}

function buildSaveXml(
    tagName,
    description
) {
    return `<?xml version="1.0" encoding="UTF-8"?>
<baselineDocument>
    <baseline>
        <tagName>${escapeXml(tagName)}</tagName>
        <description>${escapeXml(description)}</description>
    </baseline>
</baselineDocument>`;
}

async function postXml(
    url,
    xmlBody
) {
    const response = await fetch(
        url,
        {
            method: "POST",
            headers: {
                "Accept": "application/xml,text/xml,*/*",
                "Content-Type": "application/xml; charset=UTF-8"
            },
            cache: "no-store",
            body: xmlBody
        }
    );

    if (!response.ok) {
        const textValue = await response.text();
        throw new Error(textValue || `Request failed: ${response.status}`);
    }
}

async function fetchXml(url, options = {}) {
    const mergedOptions = {
        ...options,
        headers: {
            "Accept": "application/xml,text/xml,*/*",
            ...(options.headers || {})
        },
        cache: "no-store"
    };

    const response = await fetch(
        url,
        mergedOptions
    );

    const textValue = await response.text();

    if (!response.ok) {
        throw new Error(textValue || `Request failed: ${response.status}`);
    }

    return parseXmlText(
        textValue,
        url
    );
}

function parseXmlText(
    textValue,
    url
) {
    const parser = new DOMParser();
    const doc = parser.parseFromString(
        textValue,
        "application/xml"
    );

    if (hasXmlParseError(doc)) {
        throw new Error(`Invalid XML returned from ${url}`);
    }

    return doc;
}

function firstTextOf(
    parent,
    tagNames
) {
    for (const tagName of tagNames) {
        const value = textOf(
            parent,
            tagName
        );

        if (value) {
            return value;
        }
    }

    return "";
}

function textOf(
    parent,
    tagName
) {
    if (!parent) {
        return "";
    }

    const element = parent.querySelector(tagName);

    if (!element || element.textContent == null) {
        return "";
    }

    return element.textContent.trim();
}

function intValue(value) {
    if (value == null || String(value).trim() === "") {
        return null;
    }

    const parsed = Number.parseInt(
        String(value).trim(),
        10
    );

    return Number.isNaN(parsed) ? null : parsed;
}

function sortableValue(value) {
    if (value == null) {
        return "";
    }

    return String(value).toLowerCase();
}

function formatDanishDateTime(value) {
    if (!value) {
        return "";
    }

    const normalized = String(value).replace(" ", "T");
    const date = new Date(normalized);

    if (Number.isNaN(date.getTime())) {
        return value;
    }

    const day = String(date.getDate()).padStart(2, "0");
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const year = String(date.getFullYear());

    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");
    const seconds = String(date.getSeconds()).padStart(2, "0");

    return `${day}/${month}/${year} ${hours}:${minutes}:${seconds}`;
}

function fallbackUserText(userId) {
    return userId == null ? "" : String(userId);
}

function cell(
    value,
    className = ""
) {
    const td = document.createElement("td");

    if (className) {
        td.className = className;
    }

    td.textContent = value == null ? "" : String(value);

    return td;
}

function clear(element) {
    if (!element) {
        return;
    }

    while (element.firstChild) {
        element.removeChild(element.firstChild);
    }
}

function showEmpty(message) {
    if (!els.baselineEmpty) {
        return;
    }

    els.baselineEmpty.textContent = message;
    els.baselineEmpty.classList.add("is-visible");
    els.baselineEmpty.hidden = false;
}

function hideEmpty() {
    if (!els.baselineEmpty) {
        return;
    }

    els.baselineEmpty.classList.remove("is-visible");
    els.baselineEmpty.hidden = true;
}

function setLoadStatus(message) {
    setText("loadStatus", message, "");
}

function setDialogStatus(
    message,
    isError = false
) {
    if (!els.baselineDialogStatus) {
        return;
    }

    els.baselineDialogStatus.textContent = message || "";
    els.baselineDialogStatus.classList.toggle(
        "is-error",
        Boolean(isError)
    );
}

function clearDialogStatus() {
    setDialogStatus("Idle");
}

function escapeXml(value) {
    return String(value == null ? "" : value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&apos;");
}
