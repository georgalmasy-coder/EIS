import { initMenu } from "../components/menu.js";
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
const HORIZONTAL_GAP = 42;
const VERTICAL_GAP = 72;
const ROOT_WIDTH = 230;
const ROOT_HEIGHT = 96;

const state = {
    requirements: [],
    filteredRequirements: [],
    contextMenuTargetType: null,
    contextRequirement: null,
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
}

function initializeEvents() {
    const filterRequirementText = document.getElementById("filterRequirementText");
    const btnClearFilter = document.getElementById("btnClearFilter");
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

        state.topPanel = parseTopPanel(xmlDocument);
        state.requirements = parseSystemRequirements(xmlDocument);
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
    setText("customerName", state.topPanel.customerName, "");
    setText("projectName", state.topPanel.projectName, "");
    setText("userName", state.topPanel.userName, "");
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
        return getParentRequirementCode(candidate.id) === requirement.id;
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
            subtreeWidth: NODE_WIDTH
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
        subtreeWidth: ROOT_WIDTH
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
    calculateSubtreeWidth(root);

    const width = Math.max(root.subtreeWidth + 80, 900);
    assignPositions(root, width / 2, 32);

    const maxBottom = getMaxBottom(root);
    const height = Math.max(maxBottom + 80, 520);

    return { width, height };
}

function calculateSubtreeWidth(node) {
    if (!node.children.length) {
        node.subtreeWidth = node.width;
        return node.subtreeWidth;
    }

    let childrenWidth = 0;

    for (const child of node.children) {
        childrenWidth += calculateSubtreeWidth(child);
    }

    childrenWidth += HORIZONTAL_GAP * Math.max(0, node.children.length - 1);
    node.subtreeWidth = Math.max(node.width, childrenWidth);

    return node.subtreeWidth;
}

function assignPositions(node, centerX, topY) {
    node.x = centerX - node.width / 2;
    node.y = topY;

    if (!node.children.length) {
        return;
    }

    const totalChildrenWidth = node.children.reduce((sum, child) => sum + child.subtreeWidth, 0)
        + HORIZONTAL_GAP * Math.max(0, node.children.length - 1);

    let currentX = centerX - totalChildrenWidth / 2;
    const childTopY = topY + node.height + VERTICAL_GAP;

    for (const child of node.children) {
        const childCenterX = currentX + child.subtreeWidth / 2;
        assignPositions(child, childCenterX, childTopY);
        currentX += child.subtreeWidth + HORIZONTAL_GAP;
    }
}

function getMaxBottom(node) {
    let maxBottom = node.y + node.height;

    for (const child of node.children) {
        maxBottom = Math.max(maxBottom, getMaxBottom(child));
    }

    return maxBottom;
}

function renderConnections(svg, root) {
    walkTree(root, (node) => {
        for (const child of node.children) {
            const path = document.createElementNS("http://www.w3.org/2000/svg", "path");

            const startX = node.x + node.width / 2;
            const startY = node.y + node.height;
            const endX = child.x + child.width / 2;
            const endY = child.y;
            const midY = startY + (endY - startY) / 2;

            path.setAttribute("d", `M ${startX} ${startY} V ${midY} H ${endX} V ${endY}`);
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

function walkTree(node, callback) {
    callback(node);

    for (const child of node.children) {
        walkTree(child, callback);
    }
}

function getStatusClass(status) {
    const normalized = normalizeStatus(status);

    if (normalized === "new") {
        return "status-new";
    }

    if (normalized === "changed") {
        return "status-changed";
    }

    if (normalized === "validated") {
        return "status-validated";
    }

    if (normalized === "approved") {
        return "status-approved";
    }

    if (normalized === "deprecated") {
        return "status-deprecated";
    }

    if (normalized === "potential duplicate") {
        return "status-potential-duplicate";
    }

    if (normalized === "incomplete") {
        return "status-incomplete";
    }

    if (normalized === "sample") {
        return "status-sample";
    }

    if (normalized === "out of scope") {
        return "status-out-of-scope";
    }

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