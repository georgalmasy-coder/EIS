import { initMenu } from "../components/menu.js";
import { initHelpDialog } from "../components/help-dialog.js";
import { mountTopbar } from "../components/topbar.js";
import { openEditDialog } from "../components/edit-dialog.js";
import { setText } from "../core/dom.js";
import { applyTopPanel as applyPageHeader, parseTopPanel as parsePageTopPanel } from "../core/page-header.js";
import { hasXmlParseError } from "../core/xml.js";
import { downloadBaselineDetailPdf } from "./baseline-detail-pdf.js";

const API_URL = "/basis/baseline";
const LIST_PAGE_URL = "/web/view?page=baseline-main";

const STAKEHOLDER_REQUIREMENT_EDIT_PAGE_URL = "/web/view?page=stakeholderrequirement-edit";
const SYSTEM_REQUIREMENT_EDIT_PAGE_URL = "/web/view?page=systemrequirement-edit";
const SYSTEMS_BREAKDOWN_EDIT_PAGE_URL = "/web/view?page=systemsbreakdown-edit";

const state = {
    currentDoc: null,
    topPanel: {
        customerName: "—",
        projectName: "—",
        userName: "—"
    },
    baseline: null,
    stakeholderRequirements: [],
    systemRequirements: [],
    functionalStructures: null,
    logicalStructures: null,
    physicalStructures: null
};

const els = {};

document.addEventListener("DOMContentLoaded", initialize);

function initialize() {
    initializeShell();
    collectElements();
    bindEvents();

    const baselinePK = getBaselinePKFromUrl();

    if (!baselinePK) {
        setLoadStatus("Error");
        renderError("BaselinePK is missing.");
        return;
    }

    loadBaselineDetail(baselinePK);
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

    els.btnDownloadPdf = document.getElementById("btnDownloadPdf");
    els.btnBackToBaselines = document.getElementById("btnBackToBaselines");

    els.detailTagName = document.getElementById("detailTagName");
    els.detailDescription = document.getElementById("detailDescription");
    els.detailChangedBy = document.getElementById("detailChangedBy");
    els.detailChangedDateTime = document.getElementById("detailChangedDateTime");
    els.previousBaselineDateTime = document.getElementById("previousBaselineDateTime");

    els.stakeholderRequirementsBody = document.getElementById("stakeholderRequirementsBody");
    els.stakeholderRequirementsEmpty = document.getElementById("stakeholderRequirementsEmpty");
    els.stakeholderRequirementsCount = document.getElementById("stakeholderRequirementsCount");
    els.stakeholderRequirementsSection = document.getElementById("stakeholderRequirementsSection");

    els.systemRequirementsBody = document.getElementById("systemRequirementsBody");
    els.systemRequirementsEmpty = document.getElementById("systemRequirementsEmpty");
    els.systemRequirementsCount = document.getElementById("systemRequirementsCount");
    els.systemRequirementsSection = document.getElementById("systemRequirementsSection");

    els.functionalStructuresBody = document.getElementById("functionalStructuresBody");
    els.functionalStructuresEmpty = document.getElementById("functionalStructuresEmpty");
    els.functionalStructuresCount = document.getElementById("functionalStructuresCount");
    els.functionalStructuresSection = document.getElementById("functionalStructuresSection");

    els.logicalStructuresBody = document.getElementById("logicalStructuresBody");
    els.logicalStructuresEmpty = document.getElementById("logicalStructuresEmpty");
    els.logicalStructuresCount = document.getElementById("logicalStructuresCount");
    els.logicalStructuresSection = document.getElementById("logicalStructuresSection");

    els.physicalStructuresBody = document.getElementById("physicalStructuresBody");
    els.physicalStructuresEmpty = document.getElementById("physicalStructuresEmpty");
    els.physicalStructuresCount = document.getElementById("physicalStructuresCount");
    els.physicalStructuresSection = document.getElementById("physicalStructuresSection");
}

function bindEvents() {
    if (els.btnDownloadPdf) {
        els.btnDownloadPdf.addEventListener("click", function () {
            downloadBaselineDetailPdf({
                topPanel: state.topPanel,
                baseline: state.baseline,
                stakeholderRequirements: state.stakeholderRequirements,
                systemRequirements: state.systemRequirements,
                functionalStructures: state.functionalStructures,
                logicalStructures: state.logicalStructures,
                physicalStructures: state.physicalStructures,
                formatDateTime: formatDanishDateTime
            });
        });
    }

    if (els.btnBackToBaselines) {
        els.btnBackToBaselines.addEventListener("click", function () {
            window.location.href = getReturnUrl();
        });
    }

    document.querySelectorAll("[data-collapse-section]").forEach(function (section) {
        const toggle = section.querySelector("[data-collapse-toggle]");
        const panel = section.querySelector("[data-collapse-panel]");
        const icon = section.querySelector(".baseline-collapse-icon");

        if (!toggle || !panel) {
            return;
        }

        toggle.setAttribute("aria-expanded", "false");
        panel.hidden = true;

        if (icon) {
            icon.textContent = "▸";
        }

        toggle.addEventListener("click", function () {
            const isOpen = toggle.getAttribute("aria-expanded") === "true";
            const nextOpen = !isOpen;

            toggle.setAttribute(
                "aria-expanded",
                String(nextOpen)
            );

            panel.hidden = !nextOpen;

            if (icon) {
                icon.textContent = nextOpen ? "▾" : "▸";
            }
        });
    });
}

async function loadBaselineDetail(baselinePK) {
    setLoadStatus("Loading");

    try {
        const doc = await fetchXml(`${API_URL}?cmd=edit&id=${encodeURIComponent(String(baselinePK))}`);

        state.currentDoc = doc;
        state.topPanel = parsePageTopPanel(doc);
        state.baseline = parseBaseline(doc);

        state.stakeholderRequirements = parseChanges(
            doc,
            "stakeholderRequirements"
        );

        state.systemRequirements = parseChanges(
            doc,
            "systemRequirements"
        );

        state.functionalStructures = parseChanges(
            doc,
            "functionalStructures"
        );

        state.logicalStructures = parseChanges(
            doc,
            "logicalStructures"
        );

        state.physicalStructures = parseChanges(
            doc,
            "physicalStructures"
        );

        if (state.physicalStructures === null) {
            state.physicalStructures = parseChanges(
                doc,
                "systemsBreakdown"
            );
        }

        applyTopPanel();
        renderBaseline(state.baseline);

        renderChanges(
            state.stakeholderRequirements,
            els.stakeholderRequirementsSection,
            els.stakeholderRequirementsBody,
            els.stakeholderRequirementsEmpty,
            els.stakeholderRequirementsCount,
            STAKEHOLDER_REQUIREMENT_EDIT_PAGE_URL
        );

        renderChanges(
            state.systemRequirements,
            els.systemRequirementsSection,
            els.systemRequirementsBody,
            els.systemRequirementsEmpty,
            els.systemRequirementsCount,
            SYSTEM_REQUIREMENT_EDIT_PAGE_URL
        );

        renderChanges(
            state.functionalStructures,
            els.functionalStructuresSection,
            els.functionalStructuresBody,
            els.functionalStructuresEmpty,
            els.functionalStructuresCount,
            SYSTEMS_BREAKDOWN_EDIT_PAGE_URL
        );

        renderChanges(
            state.logicalStructures,
            els.logicalStructuresSection,
            els.logicalStructuresBody,
            els.logicalStructuresEmpty,
            els.logicalStructuresCount,
            SYSTEMS_BREAKDOWN_EDIT_PAGE_URL
        );

        renderChanges(
            state.physicalStructures,
            els.physicalStructuresSection,
            els.physicalStructuresBody,
            els.physicalStructuresEmpty,
            els.physicalStructuresCount,
            SYSTEMS_BREAKDOWN_EDIT_PAGE_URL
        );

        setLoadStatus("Loaded");
    } catch (error) {
        console.error(error);
        setLoadStatus("Error");
        renderError(error.message || "Could not load baseline detail.");
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

function parseBaseline(doc) {
    const baselineElement = doc.querySelector("baselineDetail > baseline");

    if (!baselineElement) {
        return null;
    }

    return {
        baselinePK: intValue(textOf(baselineElement, "baselinePK")),
        customerId: intValue(textOf(baselineElement, "customerId")),
        projectId: intValue(textOf(baselineElement, "projectId")),
        tagName: textOf(baselineElement, "tagName"),
        description: textOf(baselineElement, "description"),
        changedByUserId: intValue(textOf(baselineElement, "changedByUserId")),
        changedBy: textOf(baselineElement, "changedBy"),
        changedDateTime: textOf(baselineElement, "changedDateTime"),
        previousBaselineDateTime: textOf(baselineElement, "previousBaselineDateTime")
    };
}

function parseChanges(
    doc,
    sectionName
) {
    const sectionElement = doc.querySelector(`baselineDetail > ${sectionName}`);

    if (!sectionElement) {
        return null;
    }

    return Array.from(sectionElement.querySelectorAll("change"))
        .map(function (changeElement) {
            return {
                activity: textOf(changeElement, "activity"),
                id: textOf(changeElement, "id"),
                name: textOf(changeElement, "name"),
                lastModified: textOf(changeElement, "lastModified"),
                lastModifiedBy: textOf(changeElement, "lastModifiedBy"),
                entityType: intValue(textOf(changeElement, "entityType")),
                entityId: intValue(textOf(changeElement, "entityId"))
            };
        });
}

function renderBaseline(baseline) {
    if (!baseline) {
        renderError("Baseline was not found.");
        return;
    }

    setText(
        "detailTagName",
        baseline.tagName,
        "—"
    );

    setText(
        "detailDescription",
        baseline.description,
        "—"
    );

    setText(
        "detailChangedBy",
        baseline.changedBy || fallbackUserText(baseline.changedByUserId),
        "—"
    );

    setText(
        "detailChangedDateTime",
        formatDanishDateTime(baseline.changedDateTime),
        "\u2014"
    );

    setText(
        "previousBaselineDateTime",
        formatDanishDateTime(baseline.previousBaselineDateTime),
        "\u2014"
    );

    document.title = baseline.tagName
        ? `Baseline detail - ${baseline.tagName}`
        : "Baseline detail";
}

function renderChanges(
    rows,
    sectionElement,
    bodyElement,
    emptyElement,
    countElement,
    editPageUrl
) {
    if (sectionElement) {
        sectionElement.hidden = rows === null;
    }

    if (rows === null) {
        clear(bodyElement);

        if (countElement) {
            countElement.textContent = "0";
        }

        if (emptyElement) {
            emptyElement.classList.remove("is-visible");
            emptyElement.hidden = true;
        }

        return;
    }

    rows = rows || [];
    clear(bodyElement);

    if (countElement) {
        countElement.textContent = String(rows.length);
    }

    if (!rows.length) {
        if (emptyElement) {
            emptyElement.classList.add("is-visible");
            emptyElement.hidden = false;
        }

        return;
    }

    if (emptyElement) {
        emptyElement.classList.remove("is-visible");
        emptyElement.hidden = true;
    }

    rows.forEach(function (row) {
        const tr = document.createElement("tr");

        tr.dataset.entityId = row.entityId == null ? "" : String(row.entityId);
        tr.dataset.entityType = row.entityType == null ? "" : String(row.entityType);

        const activityCell = document.createElement("td");
        activityCell.appendChild(activityBadge(row.activity));

        tr.appendChild(activityCell);
        tr.appendChild(cell(row.id));
        tr.appendChild(cell(row.name));
        tr.appendChild(cell(row.lastModifiedBy));
        tr.appendChild(cell(formatDanishDateTime(row.lastModified)));

        tr.addEventListener("dblclick", function () {
            openEditPage(
                editPageUrl,
                row.entityId
            );
        });

        bodyElement.appendChild(tr);
    });
}

function openEditPage(
    editPageUrl,
    entityId
) {
    if (!editPageUrl || !entityId) {
        return;
    }

    const editPage = new URL(editPageUrl, window.location.href).searchParams.get("page") || "";

    if (!editPage) {
        return;
    }

    openEditDialog({
        page: editPage,
        mode: "edit",
        id: String(entityId),
        title: "Edit",
        onSaved: () => window.location.reload()
    });
}

function getCurrentRelativeUrl() {
    return `${window.location.pathname}${window.location.search}`;
}

function activityBadge(activity) {
    const span = document.createElement("span");
    const normalized = String(activity || "").trim();

    span.className = `baseline-activity ${activityClass(normalized)}`;
    span.textContent = normalized;

    return span;
}

function activityClass(activity) {
    switch (activity.toLowerCase()) {
        case "created":
            return "baseline-activity-created";
        case "modified":
            return "baseline-activity-modified";
        case "inactivated":
            return "baseline-activity-inactivated";
        default:
            return "";
    }
}

function renderError(message) {
    state.baseline = null;
    state.stakeholderRequirements = [];
    state.systemRequirements = [];
    state.functionalStructures = null;
    state.logicalStructures = null;
    state.physicalStructures = null;

    setText(
        "detailTagName",
        "—",
        "—"
    );

    setText(
        "detailDescription",
        message || "An error occurred.",
        "—"
    );

    setText(
        "detailChangedBy",
        "—",
        "—"
    );

    setText(
        "detailChangedDateTime",
        "\u2014",
        "\u2014"
    );

    setText(
        "previousBaselineDateTime",
        "\u2014",
        "\u2014"
    );

    renderChanges(
        [],
        els.stakeholderRequirementsSection,
        els.stakeholderRequirementsBody,
        els.stakeholderRequirementsEmpty,
        els.stakeholderRequirementsCount,
        STAKEHOLDER_REQUIREMENT_EDIT_PAGE_URL
    );

    renderChanges(
        [],
        els.systemRequirementsSection,
        els.systemRequirementsBody,
        els.systemRequirementsEmpty,
        els.systemRequirementsCount,
        SYSTEM_REQUIREMENT_EDIT_PAGE_URL
    );

    renderChanges(
        null,
        els.functionalStructuresSection,
        els.functionalStructuresBody,
        els.functionalStructuresEmpty,
        els.functionalStructuresCount,
        SYSTEMS_BREAKDOWN_EDIT_PAGE_URL
    );

    renderChanges(
        null,
        els.logicalStructuresSection,
        els.logicalStructuresBody,
        els.logicalStructuresEmpty,
        els.logicalStructuresCount,
        SYSTEMS_BREAKDOWN_EDIT_PAGE_URL
    );

    renderChanges(
        null,
        els.physicalStructuresSection,
        els.physicalStructuresBody,
        els.physicalStructuresEmpty,
        els.physicalStructuresCount,
        SYSTEMS_BREAKDOWN_EDIT_PAGE_URL
    );
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

function getBaselinePKFromUrl() {
    const parameters = new URLSearchParams(window.location.search);
    return intValue(parameters.get("baselinePK"));
}

function getReturnUrl() {
    const parameters = new URLSearchParams(window.location.search);
    const returnUrl = parameters.get("returnUrl");

    if (returnUrl) {
        return returnUrl;
    }

    return LIST_PAGE_URL;
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

    const element = parent.getElementsByTagName(tagName)[0]
        || parent.querySelector(tagName);

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

function cell(value) {
    const td = document.createElement("td");
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

function setLoadStatus(message) {
    setText("loadStatus", message, "");
}

