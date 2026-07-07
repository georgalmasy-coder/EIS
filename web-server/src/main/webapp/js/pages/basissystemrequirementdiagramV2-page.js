import { initMenu } from "../components/menu.js";
import { initHelpDialog } from "../components/help-dialog.js";
import { applyTopbarMetadata } from "../components/topbar.js";
import { setText } from "../core/dom.js";
import {
    getChildText,
    hasXmlParseError
} from "../core/xml.js";
import {
    calculateLevelFromRequirementCode,
    getAncestorCodes,
    getNearestExistingParentCode,
    getParentRequirementCode,
    isRootRequirement,
    isValidRequirementCode,
    normalizeRequirementCode
} from "../core/requirement-code.js";
import { naturalCompare } from "../components/sortable-table.js";

const SYSTEM_REQUIREMENT_ENDPOINT = "/basis/systemrequirement?cmd=list";

const NODE_WIDTH = 190;
const NODE_HEIGHT = 92;
const HORIZONTAL_GAP = 96;
const VERTICAL_GAP = 72;
const ROOT_WIDTH = 230;
const ROOT_HEIGHT = 96;
const MAX_REQUIREMENT_LEVEL = 4;

const state = {
    requirements: [],
    filteredRequirements: [],
    contextMenuTargetType: null,
    contextRequirement: null,
    lastRenderedTree: null,
    lastRenderedLayout: null,
    topPanel: {
        customerName: "—",
        projectName: "—",
        userName: "—"
    }
};

document.addEventListener("DOMContentLoaded", () => {
    initializePageShell();
    initializeEvents();
    loadSystemRequirementDiagram();
});

function initializePageShell() {
    setText("customerName", "—", "");
    setText("projectName", "—", "");
    setText("userName", "—", "");
    setText("loadStatus", "Loading", "");

    initMenu();
    initHelpDialog();
}

function initializeEvents() {
    const filterRequirementText = document.getElementById("filterRequirementText");
    const btnClearFilter = document.getElementById("btnClearFilter");
    const btnDownloadDiagramPdf = document.getElementById("btnDownloadDiagramPdf");
    const dialogCloseButton = document.getElementById("dialogCloseButton");

    filterRequirementText?.addEventListener("input", debounce(() => {
        applyFilterAndRender();
    }, 120));

    filterRequirementText?.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            filterRequirementText.value = "";
            applyFilterAndRender();
            filterRequirementText.blur();
        }
    });

    btnClearFilter?.addEventListener("click", () => {
        if (filterRequirementText) {
            filterRequirementText.value = "";
        }

        applyFilterAndRender();
    });

    btnDownloadDiagramPdf?.addEventListener("click", () => {
        downloadDiagramPdf();
    });

    dialogCloseButton?.addEventListener("click", () => {
        const dialog = document.getElementById("requirementDialog");

        if (dialog?.open) {
            dialog.close();
        }
    });

    window.addEventListener("resize", debounce(() => {
        renderDiagram();
    }, 100));

    initializeContextMenuEvents();
}

async function loadSystemRequirementDiagram() {
    showEmptyState("Loading system requirement hierarchy diagram…");
    setText("loadStatus", "Loading", "");

    try {
        const response = await fetch(SYSTEM_REQUIREMENT_ENDPOINT, {
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
            throw new Error("The system requirement endpoint returned invalid XML.");
        }

        state.currentDoc = xmlDocument;
        state.topPanel = parseTopPanel(xmlDocument);
        state.requirements = parseSystemRequirements(xmlDocument)
            .filter((requirement) => calculateLevelFromRequirementCode(requirement.id) <= MAX_REQUIREMENT_LEVEL);
        state.filteredRequirements = [...state.requirements];

        applyTopPanel();
        setText("systemRequirementCount", String(state.requirements.length), "");

        renderDiagram();

        setText("loadStatus", "Loaded", "");
    } catch (error) {
        console.error("Failed to load system requirement hierarchy diagram", error);
        setText("loadStatus", "Error", "");
        showEmptyState(`Could not load system requirement hierarchy diagram. ${error.message}`);
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
        customerName: getChildText(topPanelElement, "CustomerName", "—"),
        projectName: getChildText(topPanelElement, "ProjectName", "—"),
        userName: getChildText(topPanelElement, "Name", "—")
    };
}

function applyTopPanel() {
    applyTopbarMetadata(document, state.currentDoc || state.topPanel);
}

function parseSystemRequirements(xmlDocument) {
    const requirementNodes = Array.from(
        xmlDocument.querySelectorAll("systemRequirements > systemRequirement")
    );

    return requirementNodes.map((node, index) => {
        const id = normalizeRequirementCode(getFirstFieldDisplayText(node, [
            "SystemReqCode",
            "SystemRequirementCode",
            "RequirementCode"
        ], "—"));

        const name = getFirstFieldDisplayText(node, [
            "RequirementName",
            "ReqName",
            "Name"
        ], "—");

        const description = getFirstFieldDisplayText(node, [
            "RequirementDescription",
            "ReqDescription",
            "Description"
        ], "");

        const verificationStatus = getFirstFieldDisplayText(node, [
            "RequirementVerificationStatusId",
            "RequirementVerificationStatus",
            "ReqVerificationStatusId",
            "ReqVerificationStatus"
        ], "—");

        const businessPriority = getFirstFieldDisplayText(node, [
            "RequirementBusinessPriorityId",
            "RequirementBusinessPriority",
            "ReqBusinessPriorityId",
            "ReqBusinessPriority"
        ], "—");

        const requirementStatus = getFirstFieldDisplayText(node, [
            "RequirementStatusId",
            "RequirementStatus",
            "ReqStatusId",
            "ReqStatus"
        ], "—");

        return {
            internalId: getFirstFieldDisplayText(node, [
                "EntityId",
                "SystemRequirementId",
                "RequirementId"
            ], "") || node.getAttribute("id") || node.getAttribute("entityId") || `requirement-${index}`,
            id,
            name,
            description,
            verificationStatus,
            businessPriority,
            requirementStatus,
            parentId: getParentRequirementCode(id)
        };
    }).filter((requirement) => isValidRequirementCode(requirement.id))
        .sort((left, right) => naturalCompare(left.id, right.id));
}

function getFirstFieldDisplayText(parent, fieldNames, fallback) {
    for (const fieldName of fieldNames) {
        const value = getFieldDisplayText(parent, fieldName, "");

        if (value !== "") {
            return value;
        }
    }

    return fallback;
}

function getFieldDisplayText(parent, fieldName, fallback) {
    const field = parent.querySelector(`:scope > ${fieldName}`);

    if (!field) {
        return fallback;
    }

    const selectedOption = field.querySelector(":scope > Option[selected='true']");

    if (selectedOption?.textContent?.trim()) {
        return selectedOption.textContent.trim();
    }

    const valueNode = field.querySelector(":scope > Value");
    const value = valueNode?.textContent?.trim() || "";

    if (value) {
        const matchingOption = Array.from(field.querySelectorAll(":scope > Option")).find((option) => {
            return String(option.getAttribute("value") || "").trim() === value;
        });

        if (matchingOption?.textContent?.trim()) {
            return matchingOption.textContent.trim();
        }

        return value;
    }

    const option = field.querySelector(":scope > Option");

    if (option?.textContent?.trim()) {
        return option.textContent.trim();
    }

    const text = field.textContent?.trim();

    return text || fallback;
}

function applyFilterAndRender() {
    const searchText = document.getElementById("filterRequirementText")?.value?.trim().toLowerCase() || "";

    if (!searchText) {
        state.filteredRequirements = [...state.requirements];
        renderDiagram();
        return;
    }

    const directlyMatched = new Set();

    for (const requirement of state.requirements) {
        if (requirementMatchesSearch(requirement, searchText)) {
            directlyMatched.add(requirement.id);
        }
    }

    const visibleIds = new Set(directlyMatched);

    for (const requirement of state.requirements) {
        if (directlyMatched.has(requirement.id)) {
            addAncestors(requirement, visibleIds);
            addDescendants(requirement, visibleIds);
        }
    }

    state.filteredRequirements = state.requirements.filter((requirement) => visibleIds.has(requirement.id));
    renderDiagram();
}

function requirementMatchesSearch(requirement, searchText) {
    return String(requirement.id || "").toLowerCase().includes(searchText)
        || String(requirement.name || "").toLowerCase().includes(searchText)
        || String(requirement.description || "").toLowerCase().includes(searchText)
        || String(requirement.verificationStatus || "").toLowerCase().includes(searchText)
        || String(requirement.businessPriority || "").toLowerCase().includes(searchText)
        || String(requirement.requirementStatus || "").toLowerCase().includes(searchText);
}

function addAncestors(requirement, visibleIds) {
    let current = requirement;
    const availableIds = new Set(state.requirements.map((item) => item.id));

    while (current?.parentId) {
        const nearestParentCode = getNearestExistingParentCode(current, availableIds);

        if (!nearestParentCode) {
            break;
        }

        const parent = state.requirements.find((candidate) => candidate.id === nearestParentCode);

        if (!parent) {
            break;
        }

        visibleIds.add(parent.id);
        current = parent;
    }
}

function addDescendants(requirement, visibleIds) {
    const children = state.requirements.filter((candidate) => {
        return getParentRequirementCode(candidate.id) === requirement.id
            && calculateLevelFromRequirementCode(candidate.id) <= MAX_REQUIREMENT_LEVEL;
    });

    for (const child of children) {
        visibleIds.add(child.id);
        addDescendants(child, visibleIds);
    }
}

function renderDiagram() {
    const canvas = document.getElementById("systemRequirementDiagramCanvas");
    const nodesLayer = document.getElementById("systemRequirementDiagramNodes");
    const svg = document.getElementById("systemRequirementDiagramSvg");

    if (!canvas || !nodesLayer || !svg) {
        return;
    }

    nodesLayer.innerHTML = "";
    svg.innerHTML = "";

    state.lastRenderedTree = null;
    state.lastRenderedLayout = null;

    if (!state.requirements.length) {
        showEmptyState("No system requirements returned from endpoint.");
        return;
    }

    if (!state.filteredRequirements.length) {
        showEmptyState("No system requirements match the current filter.");
        return;
    }

    hideEmptyState();

    const tree = buildTree(state.filteredRequirements);
    const layout = calculateTreeLayout(tree);

    state.lastRenderedTree = tree;
    state.lastRenderedLayout = layout;

    canvas.style.width = `${layout.width}px`;
    canvas.style.height = `${layout.height}px`;

    svg.setAttribute("width", String(layout.width));
    svg.setAttribute("height", String(layout.height));
    svg.setAttribute("viewBox", `0 0 ${layout.width} ${layout.height}`);

    renderConnections(svg, tree);
    renderNodes(nodesLayer, tree);
}

function buildTree(requirements) {
    const availableRequirementIds = new Set(requirements.map((requirement) => requirement.id));
    const nodesById = new Map();

    for (const requirement of requirements) {
        nodesById.set(requirement.id, {
            type: "requirement",
            requirement,
            children: [],
            x: 0,
            y: 0,
            width: NODE_WIDTH,
            height: NODE_HEIGHT,
            subtreeHeight: NODE_HEIGHT
        });
    }

    const root = {
        type: "project",
        requirement: null,
        children: [],
        x: 0,
        y: 0,
        width: ROOT_WIDTH,
        height: ROOT_HEIGHT,
        subtreeHeight: ROOT_HEIGHT
    };

    for (const requirement of requirements) {
        const node = nodesById.get(requirement.id);

        if (!node) {
            continue;
        }

        if (isRootRequirement(requirement)) {
            root.children.push(node);
            continue;
        }

        const parentCode = getNearestExistingParentCode(requirement, availableRequirementIds);
        const parentNode = parentCode ? nodesById.get(parentCode) : null;

        if (parentNode) {
            parentNode.children.push(node);
        } else {
            root.children.push(node);
        }
    }

    sortTree(root);

    return root;
}

function sortTree(node) {
    node.children.sort((left, right) => naturalCompare(left.requirement?.id || "", right.requirement?.id || ""));

    for (const child of node.children) {
        sortTree(child);
    }
}

function calculateTreeLayout(root) {
    calculateSubtreeHeight(root);

    const maxLevel = getMaxRequirementLevel(root);
    const width = Math.max(
        80 + ROOT_WIDTH + HORIZONTAL_GAP + (NODE_WIDTH + HORIZONTAL_GAP) * Math.max(1, maxLevel),
        980
    );

    const height = Math.max(root.subtreeHeight + 120, 560);

    assignPositions(root, 48, height / 2);

    return { width, height };
}

function calculateSubtreeHeight(node) {
    if (!node.children.length) {
        node.subtreeHeight = node.height;
        return node.subtreeHeight;
    }

    let childrenHeight = 0;

    for (const child of node.children) {
        childrenHeight += calculateSubtreeHeight(child);
    }

    childrenHeight += VERTICAL_GAP * Math.max(0, node.children.length - 1);
    node.subtreeHeight = Math.max(node.height, childrenHeight);

    return node.subtreeHeight;
}

function assignPositions(node, leftX, centerY) {
    node.x = leftX;
    node.y = centerY - node.height / 2;

    if (!node.children.length) {
        return;
    }

    const totalChildrenHeight = node.children.reduce((sum, child) => sum + child.subtreeHeight, 0)
        + VERTICAL_GAP * Math.max(0, node.children.length - 1);

    let currentY = centerY - totalChildrenHeight / 2;
    const childLeftX = leftX + node.width + HORIZONTAL_GAP;

    for (const child of node.children) {
        const childCenterY = currentY + child.subtreeHeight / 2;
        assignPositions(child, childLeftX, childCenterY);
        currentY += child.subtreeHeight + VERTICAL_GAP;
    }
}

function getMaxRequirementLevel(root) {
    let maxLevel = 0;

    walkTree(root, (node) => {
        if (node.type === "requirement") {
            maxLevel = Math.max(maxLevel, calculateLevelFromRequirementCode(node.requirement.id));
        }
    });

    return Math.min(maxLevel, MAX_REQUIREMENT_LEVEL);
}

function renderConnections(svg, root) {
    walkTree(root, (node) => {
        for (const child of node.children) {
            const path = document.createElementNS("http://www.w3.org/2000/svg", "path");

            const startX = node.x + node.width;
            const startY = node.y + node.height / 2;
            const endX = child.x;
            const endY = child.y + child.height / 2;
            const midX = startX + (endX - startX) / 2;

            path.setAttribute("d", `M ${startX} ${startY} H ${midX} V ${endY} H ${endX}`);
            path.classList.add("systemrequirementdiagram-connection");

            svg.appendChild(path);
        }
    });
}

function renderNodes(nodesLayer, root) {
    walkTree(root, (node) => {
        const element = document.createElement("button");
        element.type = "button";
        element.className = node.type === "project"
            ? "systemrequirementdiagram-node systemrequirementdiagram-project-node"
            : `systemrequirementdiagram-node systemrequirementdiagram-requirement-node ${getStatusClass(node.requirement.requirementStatus)}`;

        element.style.left = `${node.x}px`;
        element.style.top = `${node.y}px`;
        element.style.width = `${node.width}px`;
        element.style.height = `${node.height}px`;

        if (node.type === "project") {
            renderProjectNode(element);

            element.addEventListener("contextmenu", (event) => {
                event.preventDefault();
                event.stopPropagation();
                showDiagramContextMenu(event.clientX, event.clientY, "project", null);
            });
        } else {
            renderRequirementNode(element, node.requirement);

            element.addEventListener("click", () => {
                hideDiagramContextMenu();
                openRequirementDialog(node.requirement);
            });

            element.addEventListener("contextmenu", (event) => {
                event.preventDefault();
                event.stopPropagation();
                showDiagramContextMenu(event.clientX, event.clientY, "requirement", node.requirement);
            });
        }

        nodesLayer.appendChild(element);
    });
}

function renderProjectNode(element) {
    const code = document.createElement("span");
    code.className = "systemrequirementdiagram-node-code";
    code.textContent = "";

    const name = document.createElement("span");
    name.className = "systemrequirementdiagram-node-name";
    name.textContent = state.topPanel.projectName || "Project";

    const footer = document.createElement("span");
    footer.className = "systemrequirementdiagram-node-footer";
    footer.textContent = state.topPanel.customerName || "Customer";

    element.append(code, name, footer);
}

function renderRequirementNode(element, requirement) {
    element.title = buildRequirementTooltip(requirement);

    const code = document.createElement("span");
    code.className = "systemrequirementdiagram-node-code";
    code.textContent = requirement.id;

    const name = document.createElement("span");
    name.className = "systemrequirementdiagram-node-name";
    name.textContent = requirement.name;

    const footer = document.createElement("span");
    footer.className = "systemrequirementdiagram-node-footer";
    footer.textContent = requirement.requirementStatus || "—";

    element.append(code, name, footer);
}

function buildRequirementTooltip(requirement) {
    return [
        `ID: ${requirement.id}`,
        `Name: ${requirement.name}`,
        requirement.description ? `Description: ${requirement.description}` : "",
        `Verification Status: ${requirement.verificationStatus || "—"}`,
        `Business Priority: ${requirement.businessPriority || "—"}`,
        `Requirement Status: ${requirement.requirementStatus || "—"}`
    ].filter(Boolean).join("\n");
}

function openRequirementDialog(requirement) {
    const dialog = document.getElementById("requirementDialog");

    if (!dialog) {
        return;
    }

    setText("dialogTitle", `${requirement.id} - ${requirement.name}`, "");
    setText("dialogRequirementId", requirement.id || "—", "");
    setText("dialogRequirementName", requirement.name || "—", "");
    setText("dialogRequirementDescription", requirement.description || "—", "");
    setText("dialogVerificationStatus", requirement.verificationStatus || "—", "");
    setText("dialogBusinessPriority", requirement.businessPriority || "—", "");
    setText("dialogRequirementStatus", requirement.requirementStatus || "—", "");

    if (!dialog.open) {
        dialog.showModal();
    }
}

function initializeContextMenuEvents() {
    const contextMenu = document.getElementById("diagramContextMenu");
    const temporaryHelloCancelButton = document.getElementById("temporaryHelloCancelButton");

    contextMenu?.addEventListener("click", (event) => {
        const button = event.target.closest("[data-context-action]");

        if (!button) {
            return;
        }

        const action = button.getAttribute("data-context-action");
        const requirement = state.contextRequirement;

        hideDiagramContextMenu();

        if (action === "project-hello") {
            openTemporaryProjectHelloDialog();
            return;
        }

        if (action === "requirement-hello" && requirement) {
            openTemporaryRequirementHelloDialog(requirement);
        }
    });

    temporaryHelloCancelButton?.addEventListener("click", () => {
        closeTemporaryHelloDialog();
    });

    document.addEventListener("click", () => {
        hideDiagramContextMenu();
    });

    document.addEventListener("contextmenu", (event) => {
        const diagramNode = event.target.closest(".systemrequirementdiagram-node");

        if (!diagramNode) {
            hideDiagramContextMenu();
        }
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            hideDiagramContextMenu();
        }
    });

    window.addEventListener("blur", () => {
        hideDiagramContextMenu();
    });

    window.addEventListener("resize", () => {
        hideDiagramContextMenu();
    });

    document.addEventListener("scroll", () => {
        hideDiagramContextMenu();
    }, true);
}

function showDiagramContextMenu(clientX, clientY, targetType, requirement) {
    const contextMenu = document.getElementById("diagramContextMenu");
    const projectButton = document.getElementById("contextMenuProjectHello");
    const requirementButton = document.getElementById("contextMenuRequirementHello");

    if (!contextMenu) {
        return;
    }

    state.contextMenuTargetType = targetType;
    state.contextRequirement = requirement;

    if (projectButton) {
        projectButton.hidden = targetType !== "project";
    }

    if (requirementButton) {
        requirementButton.hidden = targetType !== "requirement";
    }

    contextMenu.style.left = "0px";
    contextMenu.style.top = "0px";
    contextMenu.classList.add("is-visible");
    contextMenu.setAttribute("aria-hidden", "false");

    const menuRect = contextMenu.getBoundingClientRect();
    const margin = 8;

    let left = clientX;
    let top = clientY;

    if (left + menuRect.width + margin > window.innerWidth) {
        left = window.innerWidth - menuRect.width - margin;
    }

    if (top + menuRect.height + margin > window.innerHeight) {
        top = window.innerHeight - menuRect.height - margin;
    }

    contextMenu.style.left = `${Math.max(margin, left)}px`;
    contextMenu.style.top = `${Math.max(margin, top)}px`;

    const firstVisibleButton = Array.from(contextMenu.querySelectorAll("button"))
        .find((button) => !button.hidden);

    firstVisibleButton?.focus?.();
}

function hideDiagramContextMenu() {
    const contextMenu = document.getElementById("diagramContextMenu");

    if (!contextMenu) {
        return;
    }

    contextMenu.classList.remove("is-visible");
    contextMenu.setAttribute("aria-hidden", "true");

    state.contextMenuTargetType = null;
    state.contextRequirement = null;
}

function openTemporaryProjectHelloDialog() {
    setText("temporaryHelloDialogTitle", "Hello Projekt", "");
    setText("temporaryHelloDialogMessage", "Hello Projekt", "");
    setText("temporaryHelloRequirementValue", "—", "");

    const requirementInfo = document.getElementById("temporaryHelloRequirementInfo");

    if (requirementInfo) {
        requirementInfo.hidden = true;
    }

    openTemporaryHelloDialog();
}

function openTemporaryRequirementHelloDialog(requirement) {
    setText("temporaryHelloDialogTitle", "Hello child", "");
    setText("temporaryHelloDialogMessage", "Hello child", "");
    setText(
        "temporaryHelloRequirementValue",
        `${requirement.id || "—"} - ${requirement.name || "—"}`,
        ""
    );

    const requirementInfo = document.getElementById("temporaryHelloRequirementInfo");

    if (requirementInfo) {
        requirementInfo.hidden = false;
    }

    openTemporaryHelloDialog();
}

function openTemporaryHelloDialog() {
    const dialog = document.getElementById("temporaryHelloDialog");

    if (!dialog) {
        return;
    }

    if (!dialog.open) {
        dialog.showModal();
    }
}

function closeTemporaryHelloDialog() {
    const dialog = document.getElementById("temporaryHelloDialog");

    if (!dialog) {
        return;
    }

    if (dialog.open) {
        dialog.close();
    }
}

/* PDF export */

function downloadDiagramPdf() {
    if (!state.lastRenderedTree || !state.lastRenderedLayout) {
        renderDiagram();
    }

    if (!state.lastRenderedTree || !state.lastRenderedLayout) {
        return;
    }

    const pdfBytes = createDiagramPdf(state.lastRenderedTree, state.lastRenderedLayout);
    const blob = new Blob([pdfBytes], { type: "application/pdf" });

    downloadBlob(blob, buildDiagramPdfFileName());
}

function buildDiagramPdfFileName() {
    const projectName = String(state.topPanel.projectName || "project")
        .trim()
        .replace(/[^\p{L}\p{N}._-]+/gu, "-")
        .replace(/-+/g, "-")
        .replace(/^-|-$/g, "") || "project";

    return `system-requirement-diagram-${projectName}.pdf`;
}

function createDiagramPdf(tree, layout) {
    const pageWidth = 595.28;
    const pageHeight = 841.89;
    const margin = 28;
    const titleHeight = 36;
    const footerHeight = 24;

    const availableWidth = pageWidth - margin * 2;
    const availableHeight = pageHeight - margin * 2 - titleHeight - footerHeight;

    const scale = availableWidth / Math.max(layout.width, 1);
    const diagramPageHeight = availableHeight / scale;

    const pageRanges = calculatePdfPageRanges(tree, layout.height, diagramPageHeight);
    const generatedAt = formatGeneratedAt(new Date());

    const pages = pageRanges.map((range, index) => {
        return createDiagramPdfPageContent({
            tree,
            layout,
            pageWidth,
            pageHeight,
            margin,
            titleHeight,
            footerHeight,
            availableWidth,
            scale,
            range,
            pageNumber: index + 1,
            pageCount: pageRanges.length,
            generatedAt
        });
    });

    return buildPdfDocument(pageWidth, pageHeight, pages);
}

function calculatePdfPageRanges(tree, diagramHeight, preferredPageHeight) {
    const nodeSpans = collectPdfNodeSpans(tree);
    const ranges = [];

    let startY = 0;

    while (startY < diagramHeight) {
        let endY = Math.min(startY + preferredPageHeight, diagramHeight);

        if (endY < diagramHeight) {
            endY = adjustPdfPageBreakToAvoidCuttingNodes(startY, endY, nodeSpans, preferredPageHeight);
        }

        if (endY <= startY) {
            endY = Math.min(startY + preferredPageHeight, diagramHeight);
        }

        ranges.push({ startY, endY });
        startY = endY;
    }

    return ranges;
}

function collectPdfNodeSpans(tree) {
    const spans = [];

    walkTree(tree, (node) => {
        spans.push({
            top: node.y,
            bottom: node.y + node.height
        });
    });

    return spans.sort((left, right) => left.top - right.top);
}

function adjustPdfPageBreakToAvoidCuttingNodes(startY, proposedEndY, nodeSpans, preferredPageHeight) {
    const cuttingNode = nodeSpans.find((span) => {
        return span.top < proposedEndY && span.bottom > proposedEndY;
    });

    if (!cuttingNode) {
        return proposedEndY;
    }

    const breakBeforeNode = cuttingNode.top - 8;
    const minimumUsefulPageHeight = preferredPageHeight * 0.45;

    if (breakBeforeNode > startY + minimumUsefulPageHeight) {
        return breakBeforeNode;
    }

    return cuttingNode.bottom + 8;
}

function createDiagramPdfPageContent(options) {
    const {
        tree,
        layout,
        pageWidth,
        pageHeight,
        margin,
        titleHeight,
        footerHeight,
        availableWidth,
        scale,
        range,
        pageNumber,
        pageCount,
        generatedAt
    } = options;

    const commands = [];
    const diagramDrawWidth = layout.width * scale;
    const offsetX = margin + (availableWidth - diagramDrawWidth) / 2;
    const offsetY = margin + titleHeight;

    commands.push("q");
    commands.push("1 1 1 rg");
    commands.push(`0 0 ${formatPdfNumber(pageWidth)} ${formatPdfNumber(pageHeight)} re`);
    commands.push("f");
    commands.push("Q");

    drawPdfHeader(commands, pageWidth, pageHeight, margin);

    const transformPoint = (x, y) => ({
        x: offsetX + x * scale,
        y: pageHeight - (offsetY + (y - range.startY) * scale)
    });

    const isVisibleInRange = (node) => {
        return node.y + node.height >= range.startY && node.y <= range.endY;
    };

    drawPdfConnections(commands, tree, transformPoint, scale, range);
    drawPdfNodes(commands, tree, transformPoint, scale, isVisibleInRange);

    commands.push("q");
    commands.push("1 1 1 rg");
    commands.push(`0 0 ${formatPdfNumber(pageWidth)} ${formatPdfNumber(margin + footerHeight - 4)} re`);
    commands.push("f");
    commands.push("Q");

    drawPdfFooter(commands, pageWidth, margin, pageNumber, pageCount, generatedAt);

    return commands.join("\n");
}

function drawPdfHeader(commands, pageWidth, pageHeight, margin) {
    drawPdfText(
        commands,
        `System Requirement Hierarchy Diagram - ${state.topPanel.projectName || "Project"}`,
        margin,
        pageHeight - margin - 10,
        12,
        "Helvetica-Bold",
        [31, 41, 55]
    );

    drawPdfText(
        commands,
        `${state.topPanel.customerName || "Customer"} · Requirements: ${state.filteredRequirements.length}`,
        margin,
        pageHeight - margin - 26,
        8,
        "Helvetica",
        [100, 116, 139]
    );

    commands.push("q");
    commands.push(`${formatRgb([226, 232, 240])} RG`);
    commands.push("0.6 w");
    commands.push(`${formatPdfNumber(margin)} ${formatPdfNumber(pageHeight - margin - 34)} m`);
    commands.push(`${formatPdfNumber(pageWidth - margin)} ${formatPdfNumber(pageHeight - margin - 34)} l`);
    commands.push("S");
    commands.push("Q");
}

function drawPdfFooter(commands, pageWidth, margin, pageNumber, pageCount, generatedAt) {
    const pageText = `Page ${pageNumber} of ${pageCount}`;
    const userText = `User Name: ${state.topPanel.userName || "—"}`;
    const timeText = `Generated: ${generatedAt}`;

    const footerFontSize = 8;
    const footerColor = [100, 116, 139];

    const estimatedUserTextWidth = userText.length * footerFontSize * 0.52;
    const estimatedTimeTextWidth = timeText.length * footerFontSize * 0.52;
    const userTextX = (pageWidth - estimatedUserTextWidth) / 2;

    commands.push("q");
    commands.push(`${formatRgb([226, 232, 240])} RG`);
    commands.push("0.6 w");
    commands.push(`${formatPdfNumber(margin)} ${formatPdfNumber(margin + 8)} m`);
    commands.push(`${formatPdfNumber(pageWidth - margin)} ${formatPdfNumber(margin + 8)} l`);
    commands.push("S");
    commands.push("Q");

    drawPdfText(commands, pageText, margin, margin - 6, footerFontSize, "Helvetica", footerColor);
    drawPdfText(commands, userText, userTextX, margin - 6, footerFontSize, "Helvetica", footerColor);
    drawPdfText(commands, timeText, pageWidth - margin - estimatedTimeTextWidth, margin - 6, footerFontSize, "Helvetica", footerColor);
}

function drawPdfConnections(commands, root, transformPoint, scale, range) {
    walkTree(root, (node) => {
        for (const child of node.children) {
            const connectionTop = Math.min(node.y + node.height / 2, child.y + child.height / 2);
            const connectionBottom = Math.max(node.y + node.height / 2, child.y + child.height / 2);

            if (connectionBottom < range.startY || connectionTop > range.endY) {
                continue;
            }

            const startX = node.x + node.width;
            const startY = node.y + node.height / 2;
            const endX = child.x;
            const endY = child.y + child.height / 2;
            const midX = startX + (endX - startX) / 2;

            const p1 = transformPoint(startX, startY);
            const p2 = transformPoint(midX, startY);
            const p3 = transformPoint(midX, endY);
            const p4 = transformPoint(endX, endY);

            commands.push("q");
            commands.push(`${formatRgb(hexToRgb("#aeb8c8"))} RG`);
            commands.push(`${formatPdfNumber(Math.max(0.7, 1.4 * scale))} w`);
            commands.push(`${formatPdfNumber(p1.x)} ${formatPdfNumber(p1.y)} m`);
            commands.push(`${formatPdfNumber(p2.x)} ${formatPdfNumber(p2.y)} l`);
            commands.push(`${formatPdfNumber(p3.x)} ${formatPdfNumber(p3.y)} l`);
            commands.push(`${formatPdfNumber(p4.x)} ${formatPdfNumber(p4.y)} l`);
            commands.push("S");
            commands.push("Q");
        }
    });
}

function drawPdfNodes(commands, root, transformPoint, scale, isVisibleInRange) {
    walkTree(root, (node) => {
        if (!isVisibleInRange(node)) {
            return;
        }

        const topLeft = transformPoint(node.x, node.y);
        const width = node.width * scale;
        const height = node.height * scale;
        const x = topLeft.x;
        const y = topLeft.y - height;

        const isProject = node.type === "project";
        const background = isProject ? "#4b5563" : "#3f3f46";
        const border = isProject ? "#7c8798" : "#565f73";
        const footerColor = isProject ? "#6d28d9" : getPdfStatusColor(node.requirement.requirementStatus);

        drawPdfRect(commands, x, y, width, height, background, border);

        const footerHeight = Math.max(10, 18 * scale);
        drawPdfFilledRect(commands, x, y, width, footerHeight, footerColor);

        if (isProject) {
            drawPdfTextCentered(commands, state.topPanel.projectName || "Project", x, y + footerHeight, width, height - footerHeight, Math.max(5, 8.5 * scale), "Helvetica-Bold", [255, 255, 255]);
            drawPdfTextCentered(commands, state.topPanel.customerName || "Customer", x, y, width, footerHeight, Math.max(4.8, 7.4 * scale), "Helvetica-Bold", [255, 255, 255]);
            return;
        }

        const codeHeight = Math.max(10, 20 * scale);

        drawPdfText(commands, node.requirement.id || "—", x + 5 * scale, y + height - codeHeight + 6 * scale, Math.max(4.8, 7.4 * scale), "Helvetica-Bold", [229, 231, 235]);
        drawPdfTextCentered(commands, node.requirement.name || "—", x + 4 * scale, y + footerHeight, width - 8 * scale, height - footerHeight - codeHeight, Math.max(5.2, 8.2 * scale), "Helvetica-Bold", [255, 255, 255]);
        drawPdfTextCentered(commands, node.requirement.requirementStatus || "—", x, y, width, footerHeight, Math.max(4.8, 7.4 * scale), "Helvetica-Bold", [255, 255, 255]);
    });
}

function drawPdfRect(commands, x, y, width, height, fillHex, strokeHex) {
    commands.push("q");
    commands.push(`${formatRgb(hexToRgb(fillHex))} rg`);
    commands.push(`${formatRgb(hexToRgb(strokeHex))} RG`);
    commands.push("0.8 w");
    commands.push(`${formatPdfNumber(x)} ${formatPdfNumber(y)} ${formatPdfNumber(width)} ${formatPdfNumber(height)} re`);
    commands.push("B");
    commands.push("Q");
}

function drawPdfFilledRect(commands, x, y, width, height, fillHex) {
    commands.push("q");
    commands.push(`${formatRgb(hexToRgb(fillHex))} rg`);
    commands.push(`${formatPdfNumber(x)} ${formatPdfNumber(y)} ${formatPdfNumber(width)} ${formatPdfNumber(height)} re`);
    commands.push("f");
    commands.push("Q");
}

function drawPdfTextCentered(commands, text, x, y, width, height, fontSize, fontName, color) {
    const lines = wrapPdfText(text, Math.max(1, Math.floor(width / (fontSize * 0.52))), 2);
    const lineHeight = fontSize * 1.15;
    const totalTextHeight = lines.length * lineHeight;
    const startY = y + height / 2 + totalTextHeight / 2 - fontSize;

    lines.forEach((line, index) => {
        const estimatedWidth = line.length * fontSize * 0.52;
        const textX = x + Math.max(0, (width - estimatedWidth) / 2);
        const textY = startY - index * lineHeight;

        drawPdfText(commands, line, textX, textY, fontSize, fontName, color);
    });
}

function drawPdfText(commands, text, x, y, fontSize, fontName, color) {
    commands.push("q");
    commands.push(`${formatRgb(color)} rg`);
    commands.push("BT");
    commands.push(`/${fontName} ${formatPdfNumber(fontSize)} Tf`);
    commands.push(`${formatPdfNumber(x)} ${formatPdfNumber(y)} Td`);
    commands.push(`(${escapePdfText(text)}) Tj`);
    commands.push("ET");
    commands.push("Q");
}

function wrapPdfText(text, maxChars, maxLines) {
    const words = String(text || "—").trim().split(/\s+/);
    const lines = [];
    let current = "";

    for (const word of words) {
        const candidate = current ? `${current} ${word}` : word;

        if (candidate.length <= maxChars) {
            current = candidate;
            continue;
        }

        if (current) {
            lines.push(current);
        }

        current = word;

        if (lines.length >= maxLines) {
            break;
        }
    }

    if (current && lines.length < maxLines) {
        lines.push(current);
    }

    if (!lines.length) {
        lines.push("—");
    }

    return lines.slice(0, maxLines);
}

function getPdfStatusColor(status) {
    const normalized = normalizeStatus(status);

    if (normalized === "new") return "#2563eb";
    if (normalized === "changed") return "#f97316";
    if (normalized === "validated") return "#0891b2";
    if (normalized === "approved") return "#16a34a";
    if (normalized === "deprecated") return "#7f1d1d";
    if (normalized === "potential duplicate") return "#9333ea";
    if (normalized === "incomplete") return "#dc2626";
    if (normalized === "sample") return "#64748b";
    if (normalized === "out of scope") return "#a16207";

    return "#475569";
}

function buildPdfDocument(pageWidth, pageHeight, pageContents) {
    const encoder = new TextEncoder();
    const objects = [];
    const pageObjectNumbers = [];

    const catalogObjectNumber = 1;
    const pagesObjectNumber = 2;
    const helveticaObjectNumber = 3;
    const helveticaBoldObjectNumber = 4;

    objects[catalogObjectNumber] = "<< /Type /Catalog /Pages 2 0 R >>";
    objects[helveticaObjectNumber] = "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>";
    objects[helveticaBoldObjectNumber] = "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>";

    let nextObjectNumber = 5;

    pageContents.forEach((content) => {
        const pageObjectNumber = nextObjectNumber++;
        const contentObjectNumber = nextObjectNumber++;

        pageObjectNumbers.push(pageObjectNumber);

        objects[pageObjectNumber] = `<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${formatPdfNumber(pageWidth)} ${formatPdfNumber(pageHeight)}] /Resources << /Font << /Helvetica 3 0 R /Helvetica-Bold 4 0 R >> >> /Contents ${contentObjectNumber} 0 R >>`;

        const contentBytes = encoder.encode(content);
        objects[contentObjectNumber] = `<< /Length ${contentBytes.length} >>\nstream\n${content}\nendstream`;
    });

    objects[pagesObjectNumber] = `<< /Type /Pages /Kids [${pageObjectNumbers.map((number) => `${number} 0 R`).join(" ")}] /Count ${pageObjectNumbers.length} >>`;

    let pdf = "%PDF-1.4\n";
    const offsets = [0];

    for (let objectNumber = 1; objectNumber < objects.length; objectNumber++) {
        offsets[objectNumber] = encoder.encode(pdf).length;
        pdf += `${objectNumber} 0 obj\n${objects[objectNumber]}\nendobj\n`;
    }

    const xrefOffset = encoder.encode(pdf).length;

    pdf += `xref\n0 ${objects.length}\n`;
    pdf += "0000000000 65535 f \n";

    for (let objectNumber = 1; objectNumber < objects.length; objectNumber++) {
        pdf += `${String(offsets[objectNumber]).padStart(10, "0")} 00000 n \n`;
    }

    pdf += `trailer\n<< /Size ${objects.length} /Root 1 0 R >>\n`;
    pdf += `startxref\n${xrefOffset}\n%%EOF`;

    return encoder.encode(pdf);
}

function downloadBlob(blob, fileName) {
    const url = URL.createObjectURL(blob);

    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = fileName;
    anchor.style.display = "none";

    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();

    setTimeout(() => URL.revokeObjectURL(url), 10_000);
}

function hexToRgb(hex) {
    const normalized = String(hex || "").replace("#", "").trim();
    const value = normalized.length === 3
        ? normalized.split("").map((char) => char + char).join("")
        : normalized.padEnd(6, "0").slice(0, 6);

    return [
        parseInt(value.slice(0, 2), 16) || 0,
        parseInt(value.slice(2, 4), 16) || 0,
        parseInt(value.slice(4, 6), 16) || 0
    ];
}

function formatRgb(rgb) {
    return rgb.map((value) => formatPdfNumber(value / 255)).join(" ");
}

function formatPdfNumber(value) {
    return Number(value || 0)
        .toFixed(3)
        .replace(/\.?0+$/, "");
}

function escapePdfText(value) {
    return String(value || "")
        .replace(/\\/g, "\\\\")
        .replace(/\(/g, "\\(")
        .replace(/\)/g, "\\)")
        .replace(/[\r\n\t]+/g, " ");
}

function formatGeneratedAt(date) {
    const pad = (value) => String(value).padStart(2, "0");

    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

/* Shared utility functions */

function walkTree(node, callback) {
    callback(node);

    for (const child of node.children) {
        walkTree(child, callback);
    }
}

function getStatusClass(status) {
    const normalized = normalizeStatus(status);

    if (normalized === "new") return "status-new";
    if (normalized === "changed") return "status-changed";
    if (normalized === "validated") return "status-validated";
    if (normalized === "approved") return "status-approved";
    if (normalized === "deprecated") return "status-deprecated";
    if (normalized === "potential duplicate") return "status-potential-duplicate";
    if (normalized === "incomplete") return "status-incomplete";
    if (normalized === "sample") return "status-sample";
    if (normalized === "out of scope") return "status-out-of-scope";

    return "status-unknown";
}

function normalizeStatus(status) {
    return String(status || "")
        .trim()
        .replace(/\s+/g, " ")
        .toLowerCase();
}

function showEmptyState(message) {
    const emptyState = document.getElementById("systemRequirementDiagramEmptyState");

    if (!emptyState) {
        return;
    }

    emptyState.textContent = message;
    emptyState.classList.add("is-visible");
}

function hideEmptyState() {
    const emptyState = document.getElementById("systemRequirementDiagramEmptyState");

    if (!emptyState) {
        return;
    }

    emptyState.classList.remove("is-visible");
}

function debounce(callback, delay) {
    let timeoutId = null;

    return (...args) => {
        window.clearTimeout(timeoutId);

        timeoutId = window.setTimeout(() => {
            callback(...args);
        }, delay);
    };
}
