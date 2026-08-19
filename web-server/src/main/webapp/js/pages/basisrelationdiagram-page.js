import { initMenu } from "../components/menu.js";
import { initHelpDialog } from "../components/help-dialog.js";
import { openEditDialog } from "../components/edit-dialog.js";
import { createRelationCreationDialogController } from "../components/relation-creation-dialog.js";
import { setText } from "../core/dom.js";
import { applyTopPanelFromDocument as applyPageHeaderFromDocument } from "../core/page-header.js";
import {
    getChildText,
    hasXmlParseError
} from "../core/xml.js";
import { cssEscape } from "../core/css.js";
import { naturalCompare } from "../components/sortable-table.js";

const RELATION_DIAGRAM_ENDPOINT = "/basis/relationdiagram?cmd=overview";
const RELATION_CREATE_ENDPOINT = "/basis/entityrelations/createrelation";

const ENTITY_TYPE_LABELS = {
    stakeholder: "Stakeholder Requirement",
    system: "System Requirement",
    systemsBreakdown: "Physical Structure"
};

const VIEW_MODES = {
    stakeholderSystem: "stakeholder-system",
    stakeholderSystemBreakdown: "stakeholder-system-breakdown",
    systemBreakdown: "system-breakdown"
};

const state = {
    stakeholderRequirements: [],
    systemRequirements: [],
    systemsBreakdowns: [],
    requirementsByInternalId: new Map(),
    relations: [],
    viewMode: VIEW_MODES.stakeholderSystemBreakdown,
    focusedInternalId: null,
    selectedInternalId: null,
    hoverInternalId: null,
    resizeObserver: null
};

let relationCreationDialog = null;

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", startBasisRelationDiagramPage);
} else {
    startBasisRelationDiagramPage();
}

window.addEventListener("error", (event) => {
    console.error("Unhandled basis relation diagram error", event.error || event.message || event);
    setText("loadStatus", "Error", "");
    showEmptyState(`Could not initialize relation diagram. ${event.error?.message || event.message || "Unknown error"}`);
});

window.addEventListener("unhandledrejection", (event) => {
    console.error("Unhandled basis relation diagram rejection", event.reason);
    setText("loadStatus", "Error", "");
    showEmptyState(`Could not initialize relation diagram. ${event.reason?.message || event.reason || "Unknown error"}`);
});

function startBasisRelationDiagramPage() {
    initializePageShell();
    loadRelationDiagram();

    try {
        relationCreationDialog = createRelationCreationDialogController({
            endpointUrl: RELATION_CREATE_ENDPOINT,
            title: "Create relation",
            onCreated: () => loadRelationDiagram()
        });
    } catch (error) {
        console.error("Failed to initialize relation creation dialog", error);
    }

    initializeEvents();
}

function initializePageShell() {
    setText("customerName", "—", "");
    setText("projectName", "—", "");
    setText("userName", "—", "");
    setText("loadStatus", "Loading", "");

    initMenu();
    initHelpDialog();
    applyViewMode(state.viewMode, { redraw: false });
}

function initializeEvents() {
    const filterOnlyWithoutRelations = document.getElementById("filterOnlyWithoutRelations");
    const filterOnlyWithRelations = document.getElementById("filterOnlyWithRelations");
    const filterRequirementText = document.getElementById("filterRequirementText");
    const focusDropZone = document.getElementById("focusDropZone");
    const btnClearFocus = document.getElementById("btnClearFocus");
    const btnClearSelection = document.getElementById("btnClearSelection");
    const dialogFocusButton = document.getElementById("dialogFocusButton");
    const dialogEditButton = document.getElementById("dialogEditButton");
    const dialogCloseButton = document.getElementById("dialogCloseButton");
    const viewButtons = document.querySelectorAll("[data-view-mode]");

    filterOnlyWithoutRelations?.addEventListener("change", () => {
        if (filterOnlyWithoutRelations.checked && filterOnlyWithRelations) {
            filterOnlyWithRelations.checked = false;
        }

        applyFiltersAndRedraw();
    });

    filterOnlyWithRelations?.addEventListener("change", () => {
        if (filterOnlyWithRelations.checked && filterOnlyWithoutRelations) {
            filterOnlyWithoutRelations.checked = false;
        }

        applyFiltersAndRedraw();
    });

    filterRequirementText?.addEventListener("input", debounce(() => {
        applyFiltersAndRedraw();
    }, 120));

    filterRequirementText?.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            filterRequirementText.value = "";
            applyFiltersAndRedraw();
            filterRequirementText.blur();
        }
    });

    focusDropZone?.addEventListener("dragenter", handleFocusDragEnter);
    focusDropZone?.addEventListener("dragover", handleFocusDragOver);
    focusDropZone?.addEventListener("dragleave", handleFocusDragLeave);
    focusDropZone?.addEventListener("drop", handleFocusDrop);
    focusDropZone?.addEventListener("keydown", (event) => {
        if ((event.key === "Enter" || event.key === " ") && state.focusedInternalId) {
            event.preventDefault();
            clearFocusedRequirement();
        }
    });

    btnClearFocus?.addEventListener("click", () => {
        clearFocusedRequirement();
    });

    btnClearSelection?.addEventListener("click", () => {
        clearAllFilters();
    });

    dialogFocusButton?.addEventListener("click", () => {
        const requirement = getRequirementForDialogFocus();
        if (requirement) {
            setFocusedRequirement(requirement.internalId);
        }
    });

    dialogEditButton?.addEventListener("click", () => {
        const requirement = getDialogRequirement();
        if (requirement) {
            openRequirementEditDialog(requirement);
        }
    });

    dialogCloseButton?.addEventListener("click", () => {
        const dialog = document.getElementById("requirementDialog");

        if (dialog?.open) {
            dialog.close();
        }
    });

    viewButtons.forEach((button) => {
        button.addEventListener("click", () => {
            const viewMode = button.getAttribute("data-view-mode");
            if (viewMode) {
                applyViewMode(viewMode);
            }
        });
    });

    window.addEventListener("resize", debounce(() => {
        drawRelations();
        updateHighlight();
    }, 80));
}

function clearAllFilters() {
    state.focusedInternalId = null;
    state.selectedInternalId = null;
    state.hoverInternalId = null;

    const filterOnlyWithoutRelations = document.getElementById("filterOnlyWithoutRelations");
    const filterOnlyWithRelations = document.getElementById("filterOnlyWithRelations");
    const filterRequirementText = document.getElementById("filterRequirementText");

    if (filterOnlyWithoutRelations) {
        filterOnlyWithoutRelations.checked = false;
    }

    if (filterOnlyWithRelations) {
        filterOnlyWithRelations.checked = false;
    }

    if (filterRequirementText) {
        filterRequirementText.value = "";
    }

    updateFocusDropZone();
    applyFiltersAndRedraw();
}

async function loadRelationDiagram() {
    showEmptyState("Loading relation diagram…");
    setText("loadStatus", "Loading", "");

    try {
        const response = await fetch(RELATION_DIAGRAM_ENDPOINT, {
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
            throw new Error("The relation diagram endpoint returned invalid XML.");
        }

        const diagram = parseRelationDiagramDocument(xmlDocument);

        applyTopPanel(xmlDocument);
        setRelationDiagramState(diagram);
        renderRelationDiagram();

        setText("loadStatus", "Loaded", "");
    } catch (error) {
        console.error("Failed to load relation diagram", error);
        setText("loadStatus", "Error", "");
        showEmptyState(`Could not load relation diagram. ${error.message}`);
    }
}

function parseRelationDiagramDocument(xmlDocument) {
    const relationDiagramElement = xmlDocument.querySelector("relationDiagram");

    if (!relationDiagramElement) {
        throw new Error("Missing relationDiagram element.");
    }

    const stakeholderRequirements = parseRequirements(
        relationDiagramElement,
        "stakeholderRequirements",
        "requirement"
    );

    const systemRequirements = parseRequirements(
        relationDiagramElement,
        "systemRequirements",
        "requirement"
    );

    const systemsBreakdowns = parseRequirements(
        relationDiagramElement,
        "systemsBreakdowns",
        "systemsBreakdown"
    );

    const relations = parseRelations(relationDiagramElement);

    return {
        stakeholderRequirements,
        systemRequirements,
        systemsBreakdowns,
        relations
    };
}

function parseRequirements(parentElement, groupSelector, itemSelector) {
    const requirementElements = parentElement.querySelectorAll(
        `:scope > ${groupSelector} > ${itemSelector}`
    );

    return Array.from(requirementElements).map((requirementElement) => {
        const internalId = requirementElement.getAttribute("id") || "";
        const entityTypeId = parseInteger(requirementElement.getAttribute("entityType"));
        const entityId = parseInteger(requirementElement.getAttribute("entityId"))
            ?? parseEntityId(internalId, getChildText(requirementElement, "id", ""));
        const requirementType = groupSelector === "stakeholderRequirements"
            ? "stakeholder"
            : groupSelector === "systemRequirements"
                ? "system"
                : "systemsBreakdown";

        return {
            internalId,
            visibleId: getChildText(requirementElement, "id", "—"),
            name: getChildText(requirementElement, "name", "—"),
            description: getChildText(requirementElement, "description", ""),
            type: requirementType,
            entityId,
            entityTypeId,
            entityTypeLabel: ENTITY_TYPE_LABELS[requirementType] || "Entity"
        };
    }).filter((requirement) => requirement.internalId);
}

function parseRelations(relationDiagramElement) {
    const relationElements = relationDiagramElement.querySelectorAll(
        ":scope > StakeholderRequirementToSystemRequirementRelations > relation, :scope > SystemRequirementToSystemsBreakdownRelations > relation"
    );

    return Array.from(relationElements).map((relationElement, index) => ({
        id: `relation-${index}`,
        from: getChildText(relationElement, "from", ""),
        to: getChildText(relationElement, "to", ""),
        type: getChildText(relationElement, "type", "")
    })).filter((relation) => relation.from && relation.to);
}

function applyTopPanel(xmlDocument) {
    applyPageHeaderFromDocument(xmlDocument, {
        customerName: "customerName",
        projectName: "projectName",
        userName: "userName",
        workspaceEyebrow: "pageEyebrow",
        workspaceHeading: "pageHeading",
        workspaceHelpText: "pageHelpText"
    });
}

function setRelationDiagramState(diagram) {
    state.stakeholderRequirements = diagram.stakeholderRequirements;
    state.systemRequirements = diagram.systemRequirements;
    state.systemsBreakdowns = diagram.systemsBreakdowns;
    state.relations = diagram.relations;
    state.focusedInternalId = null;
    state.selectedInternalId = null;
    state.hoverInternalId = null;
    state.requirementsByInternalId = new Map();

    for (const requirement of state.stakeholderRequirements) {
        state.requirementsByInternalId.set(requirement.internalId, requirement);
    }

    for (const requirement of state.systemRequirements) {
        state.requirementsByInternalId.set(requirement.internalId, requirement);
    }

    for (const requirement of state.systemsBreakdowns) {
        state.requirementsByInternalId.set(requirement.internalId, requirement);
    }

    applyViewMode(state.viewMode, { redraw: false });
}

function renderRelationDiagram() {
    setText("stakeholderRequirementCount", String(state.stakeholderRequirements.length), "");
    setText("systemRequirementCount", String(state.systemRequirements.length), "");
    setText("systemsBreakdownCount", String(state.systemsBreakdowns.length), "");
    setText("relationCount", String(state.relations.length), "");

    const stakeholderList = document.getElementById("stakeholderRequirementsList");
    const systemList = document.getElementById("systemRequirementsList");
    const systemsBreakdownsList = document.getElementById("systemsBreakdownsList");

    if (!stakeholderList || !systemList || !systemsBreakdownsList) {
        throw new Error("Missing relation diagram list elements.");
    }

    stakeholderList.innerHTML = "";
    systemList.innerHTML = "";
    systemsBreakdownsList.innerHTML = "";

    if (!state.stakeholderRequirements.length && !state.systemRequirements.length && !state.systemsBreakdowns.length) {
        showEmptyState("No requirements returned from endpoint.");
        return;
    }

    hideEmptyState();

    renderRequirementCards(stakeholderList, state.stakeholderRequirements);
    renderRequirementCards(systemList, state.systemRequirements);
    renderRequirementCards(systemsBreakdownsList, state.systemsBreakdowns);
    updateFocusDropZone();

    applyViewMode(state.viewMode, { redraw: false });
    setupScrollListeners();
    setupResizeObserver();

    requestAnimationFrame(() => {
        applyFiltersAndRedraw();
    });
}

function renderRequirementCards(container, requirements) {
    for (const requirement of requirements) {
        const card = document.createElement("button");
        card.type = "button";
        card.className = "relationdiagram-requirement-card";
        card.dataset.internalId = requirement.internalId;
        card.dataset.entityTypeId = requirement.entityTypeId ? String(requirement.entityTypeId) : "";
        card.draggable = true;
        card.title = buildRequirementTooltip(requirement);

        const code = document.createElement("span");
        code.className = "relationdiagram-requirement-code";
        code.textContent = requirement.visibleId;

        const name = document.createElement("span");
        name.className = "relationdiagram-requirement-name";
        name.textContent = requirement.name;

        card.append(code, name);

        card.addEventListener("mouseenter", () => {
            state.hoverInternalId = requirement.internalId;
            updateHighlight();
        });

        card.addEventListener("mouseleave", () => {
            state.hoverInternalId = null;
            updateHighlight();
        });

        card.addEventListener("click", () => {
            state.selectedInternalId = requirement.internalId;
            updateHighlight();
            openRequirementDialog(requirement);
        });

        card.addEventListener("dragstart", (event) => {
            event.dataTransfer?.setData("text/plain", requirement.internalId);
            event.dataTransfer?.setData("application/x-relationdiagram-internal-id", requirement.internalId);
            if (event.dataTransfer) {
                event.dataTransfer.effectAllowed = "copy";
            }
            card.classList.add("is-dragging");
        });

        card.addEventListener("dragenter", handleRelationCardDragEnter);
        card.addEventListener("dragover", handleRelationCardDragOver);
        card.addEventListener("dragleave", handleRelationCardDragLeave);
        card.addEventListener("drop", handleRelationCardDrop);
        card.addEventListener("dragend", () => {
            card.classList.remove("is-dragging");
            card.classList.remove("is-drop-target");
        });

        container.appendChild(card);
    }
}

function buildRequirementTooltip(requirement) {
    return [
        `ID: ${requirement.visibleId}`,
        `Name: ${requirement.name}`,
        requirement.description ? `Description: ${requirement.description}` : ""
    ].filter(Boolean).join("\n");
}

function setupScrollListeners() {
    const stakeholderList = document.getElementById("stakeholderRequirementsList");
    const systemList = document.getElementById("systemRequirementsList");
    const systemsBreakdownsList = document.getElementById("systemsBreakdownsList");

    stakeholderList?.addEventListener("scroll", handleDiagramScroll, { passive: true });
    systemList?.addEventListener("scroll", handleDiagramScroll, { passive: true });
    systemsBreakdownsList?.addEventListener("scroll", handleDiagramScroll, { passive: true });
}

const handleDiagramScroll = debounce(() => {
    drawRelations();
    updateHighlight();
}, 20);

function setupResizeObserver() {
    const frame = document.getElementById("relationdiagramFrame");

    if (!frame || typeof ResizeObserver === "undefined") {
        return;
    }

    if (state.resizeObserver) {
        state.resizeObserver.disconnect();
    }

    state.resizeObserver = new ResizeObserver(debounce(() => {
        drawRelations();
        updateHighlight();
    }, 60));

    state.resizeObserver.observe(frame);
}

function applyFiltersAndRedraw() {
    applyFilters();

    requestAnimationFrame(() => {
        drawRelations();
        updateHighlight();
    });
}

function applyFilters() {
    const onlyWithoutRelations = document.getElementById("filterOnlyWithoutRelations")?.checked === true;
    const onlyWithRelations = document.getElementById("filterOnlyWithRelations")?.checked === true;
    const searchText = document.getElementById("filterRequirementText")?.value?.trim().toLowerCase() || "";
    const focusVisibleIds = state.focusedInternalId ? getFocusedVisibleIds(state.focusedInternalId) : null;

    const cards = document.querySelectorAll(".relationdiagram-requirement-card");

    for (const card of cards) {
        const internalId = card.dataset.internalId;
        const requirement = state.requirementsByInternalId.get(internalId);
        const hasRelations = hasAnyRelation(internalId);

        let shouldShow = true;

        if (focusVisibleIds) {
            shouldShow = focusVisibleIds.has(internalId);
        }

        if (shouldShow && !focusVisibleIds && onlyWithoutRelations) {
            shouldShow = !hasRelations;
        }

        if (shouldShow && !focusVisibleIds && onlyWithRelations) {
            shouldShow = hasRelations;
        }

        if (shouldShow && !focusVisibleIds && searchText) {
            shouldShow = requirementMatchesSearch(requirement, searchText);
        }

        card.classList.toggle("is-hidden-by-filter", !shouldShow);
    }
}

function getFocusedVisibleIds(internalId) {
    const visibleIds = new Set([internalId]);

    for (const relatedId of getRelatedRequirementIds(internalId)) {
        visibleIds.add(relatedId);
    }

    return visibleIds;
}

function setFocusedRequirement(internalId) {
    if (!internalId) {
        return;
    }

    if (state.focusedInternalId && state.focusedInternalId !== internalId) {
        updateFocusDropZone(`Clear the current focus before choosing another entity.`);
        return;
    }

    state.focusedInternalId = internalId;
    state.selectedInternalId = internalId;
    state.hoverInternalId = null;

    const filterOnlyWithoutRelations = document.getElementById("filterOnlyWithoutRelations");
    const filterOnlyWithRelations = document.getElementById("filterOnlyWithRelations");
    const filterRequirementText = document.getElementById("filterRequirementText");

    if (filterOnlyWithoutRelations) {
        filterOnlyWithoutRelations.checked = false;
    }

    if (filterOnlyWithRelations) {
        filterOnlyWithRelations.checked = false;
    }

    if (filterRequirementText) {
        filterRequirementText.value = "";
    }

    updateFocusDropZone();
    applyFiltersAndRedraw();
}

function clearFocusedRequirement() {
    if (!state.focusedInternalId) {
        updateFocusDropZone();
        return;
    }

    state.focusedInternalId = null;
    state.selectedInternalId = null;
    state.hoverInternalId = null;
    updateFocusDropZone();
    applyFiltersAndRedraw();
}

function updateFocusDropZone(message = "") {
    const dropZone = document.getElementById("focusDropZone");
    const dropZoneText = document.getElementById("focusDropZoneText");
    const clearButton = document.getElementById("btnClearFocus");
    const focusedRequirement = state.focusedInternalId ? state.requirementsByInternalId.get(state.focusedInternalId) : null;

    if (!dropZone || !dropZoneText || !clearButton) {
        return;
    }

    dropZone.classList.toggle("is-active", Boolean(focusedRequirement));
    dropZone.classList.remove("is-drop-target");

    if (focusedRequirement) {
        dropZoneText.textContent = `${focusedRequirement.visibleId} - ${focusedRequirement.name}`;
        clearButton.hidden = false;
        clearButton.disabled = false;
        dropZone.setAttribute("title", "Focus is active. Clear it before choosing another entity.");
    } else if (message) {
        dropZoneText.textContent = message;
        clearButton.hidden = true;
        dropZone.setAttribute("title", message);
    } else {
        dropZoneText.textContent = "Drop an entity here";
        clearButton.hidden = true;
        dropZone.setAttribute("title", "Drop an entity here to focus on it");
    }

    const dialogFocusButton = document.getElementById("dialogFocusButton");
    if (dialogFocusButton) {
        const dialogInternalId = dialogFocusButton.dataset.internalId || "";
        dialogFocusButton.disabled = Boolean(state.focusedInternalId) && state.focusedInternalId !== dialogInternalId;
        dialogFocusButton.textContent = state.focusedInternalId === dialogInternalId ? "Focused" : "Focus this";
    }
}

function getRequirementForDialogFocus() {
    const dialogFocusButton = document.getElementById("dialogFocusButton");
    if (!dialogFocusButton?.dataset.internalId) {
        return null;
    }

    return state.requirementsByInternalId.get(dialogFocusButton.dataset.internalId) || null;
}

function getDialogRequirement() {
    return getRequirementForDialogFocus();
}

function handleFocusDragEnter(event) {
    event.preventDefault();

    const dropZone = event.currentTarget;
    if (dropZone && !state.focusedInternalId) {
        dropZone.classList.add("is-drop-target");
    }
}

function handleFocusDragOver(event) {
    event.preventDefault();

    const dropZone = event.currentTarget;
    if (dropZone && !state.focusedInternalId) {
        dropZone.classList.add("is-drop-target");
    }
}

function handleFocusDragLeave(event) {
    const dropZone = event.currentTarget;
    if (!dropZone) {
        return;
    }

    if (event.relatedTarget && dropZone.contains(event.relatedTarget)) {
        return;
    }

    dropZone.classList.remove("is-drop-target");
}

function handleFocusDrop(event) {
    event.preventDefault();

    const dropZone = event.currentTarget;
    dropZone?.classList.remove("is-drop-target");

    if (state.focusedInternalId) {
        updateFocusDropZone("Clear the current focus before choosing another entity.");
        return;
    }

    const internalId = event.dataTransfer?.getData("application/x-relationdiagram-internal-id")
        || event.dataTransfer?.getData("text/plain")
        || "";

    if (!internalId || !state.requirementsByInternalId.has(internalId)) {
        return;
    }

    setFocusedRequirement(internalId);
}

function handleRelationCardDragEnter(event) {
    event.preventDefault();
    event.currentTarget?.classList.add("is-drop-target");
}

function handleRelationCardDragOver(event) {
    event.preventDefault();

    if (event.dataTransfer) {
        event.dataTransfer.dropEffect = "copy";
    }

    event.currentTarget?.classList.add("is-drop-target");
}

function handleRelationCardDragLeave(event) {
    const card = event.currentTarget;

    if (!card) {
        return;
    }

    if (event.relatedTarget && card.contains(event.relatedTarget)) {
        return;
    }

    card.classList.remove("is-drop-target");
}

function handleRelationCardDrop(event) {
    event.preventDefault();
    event.stopPropagation();

    const card = event.currentTarget;

    if (!card) {
        return;
    }

    card.classList.remove("is-drop-target");

    const sourceInternalId = event.dataTransfer?.getData("application/x-relationdiagram-internal-id")
        || event.dataTransfer?.getData("text/plain")
        || "";

    const targetInternalId = card.dataset.internalId || "";

    if (!sourceInternalId || !targetInternalId) {
        return;
    }

    const sourceRequirement = state.requirementsByInternalId.get(sourceInternalId);
    const targetRequirement = state.requirementsByInternalId.get(targetInternalId);

    if (!sourceRequirement || !targetRequirement || !relationCreationDialog) {
        return;
    }

    relationCreationDialog.open({
        fromEntityId: sourceRequirement.entityId,
        fromEntityType: sourceRequirement.entityTypeId,
        toEntityId: targetRequirement.entityId,
        toEntityType: targetRequirement.entityTypeId,
        fromEntityCode: sourceRequirement.visibleId,
        toEntityCode: targetRequirement.visibleId,
        fromEntityLabel: sourceRequirement.entityTypeLabel,
        toEntityLabel: targetRequirement.entityTypeLabel,
        fromEntityName: sourceRequirement.name,
        toEntityName: targetRequirement.name
    }).catch((error) => {
        console.error("Failed to open relation creation dialog", error);
        window.alert(error?.message || "Failed to open relation creation dialog.");
    });
}

function parseInteger(value) {
    if (value == null || value === "") {
        return null;
    }

    const parsed = Number.parseInt(String(value), 10);
    return Number.isFinite(parsed) ? parsed : null;
}

function parseEntityId(internalId, fallbackValue) {
    const fallbackNumeric = parseInteger(fallbackValue);

    if (fallbackNumeric != null) {
        return fallbackNumeric;
    }

    if (!internalId || !internalId.includes("-")) {
        return null;
    }

    return parseInteger(internalId.slice(internalId.lastIndexOf("-") + 1));
}

function requirementMatchesSearch(requirement, searchText) {
    if (!requirement) {
        return false;
    }

    const visibleId = String(requirement.visibleId || "").toLowerCase();
    const name = String(requirement.name || "").toLowerCase();
    const description = String(requirement.description || "").toLowerCase();

    return visibleId.includes(searchText)
        || name.includes(searchText)
        || description.includes(searchText);
}

function drawRelations() {
    const showStakeholderSystem = state.viewMode === VIEW_MODES.stakeholderSystem
        || state.viewMode === VIEW_MODES.stakeholderSystemBreakdown;
    const showSystemBreakdown = state.viewMode === VIEW_MODES.stakeholderSystemBreakdown
        || state.viewMode === VIEW_MODES.systemBreakdown;

    drawRelationsInCanvas(
        "relationsSvgStakeholderSystem",
        "stakeholder",
        "system",
        showStakeholderSystem
    );

    drawRelationsInCanvas(
        "relationsSvgSystemBreakdown",
        "system",
        "systemsBreakdown",
        showSystemBreakdown
    );
}

function drawRelationsInCanvas(svgId, leftType, rightType, isEnabled) {
    const svg = document.getElementById(svgId);
    const canvasWrap = svg?.parentElement;

    if (!svg || !canvasWrap) {
        return;
    }

    svg.innerHTML = "";

    if (!isEnabled) {
        return;
    }

    const canvasRect = canvasWrap.getBoundingClientRect();
    svg.setAttribute("viewBox", `0 0 ${canvasRect.width} ${canvasRect.height}`);
    svg.setAttribute("width", String(canvasRect.width));
    svg.setAttribute("height", String(canvasRect.height));

    for (const relation of state.relations) {
        const fromCard = findVisibleRequirementCard(relation.from);
        const toCard = findVisibleRequirementCard(relation.to);

        if (!fromCard || !toCard) {
            continue;
        }

        const fromRequirement = state.requirementsByInternalId.get(relation.from);
        const toRequirement = state.requirementsByInternalId.get(relation.to);

        if (!fromRequirement || !toRequirement) {
            continue;
        }

        const points = getRelationPoints(fromCard, toCard, canvasRect, fromRequirement, toRequirement, leftType, rightType);

        if (!points) {
            continue;
        }

        const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
        path.classList.add("relationdiagram-line");
        path.dataset.relationId = relation.id;
        path.dataset.from = relation.from;
        path.dataset.to = relation.to;
        path.setAttribute("d", buildCurvePath(points));

        svg.appendChild(path);
    }
}

function getRelationPoints(fromCard, toCard, canvasRect, fromRequirement, toRequirement, leftType, rightType) {
    const fromRect = fromCard.getBoundingClientRect();
    const toRect = toCard.getBoundingClientRect();

    const fromIsLeft = fromRequirement.type === leftType;
    const fromIsRight = fromRequirement.type === rightType;
    const toIsLeft = toRequirement.type === leftType;
    const toIsRight = toRequirement.type === rightType;

    let leftRect;
    let rightRect;

    if (fromIsLeft && toIsRight) {
        leftRect = fromRect;
        rightRect = toRect;
    } else if (fromIsRight && toIsLeft) {
        leftRect = toRect;
        rightRect = fromRect;
    } else {
        return null;
    }

    return {
        x1: 0,
        y1: leftRect.top + leftRect.height / 2 - canvasRect.top,
        x2: canvasRect.width,
        y2: rightRect.top + rightRect.height / 2 - canvasRect.top
    };
}

function buildCurvePath(points) {
    const distance = Math.max(40, Math.abs(points.x2 - points.x1));
    const controlOffset = distance * 0.45;

    const c1x = points.x1 + controlOffset;
    const c1y = points.y1;
    const c2x = points.x2 - controlOffset;
    const c2y = points.y2;

    return `M ${points.x1} ${points.y1} C ${c1x} ${c1y}, ${c2x} ${c2y}, ${points.x2} ${points.y2}`;
}

function updateHighlight() {
    const activeInternalId = state.focusedInternalId || state.hoverInternalId || state.selectedInternalId;
    const relatedIds = activeInternalId ? getRelatedRequirementIds(activeInternalId) : new Set();

    const cards = document.querySelectorAll(".relationdiagram-requirement-card");

    for (const card of cards) {
        const internalId = card.dataset.internalId;
        const isSelected = internalId === activeInternalId;
        const isRelated = relatedIds.has(internalId);
        const shouldMute = Boolean(activeInternalId) && !isSelected && !isRelated;

        card.classList.toggle("is-selected", isSelected);
        card.classList.toggle("is-related", isRelated);
        card.classList.toggle("is-muted", shouldMute);
    }

    const lines = document.querySelectorAll(".relationdiagram-line");

    for (const line of lines) {
        const from = line.dataset.from;
        const to = line.dataset.to;
        const isActive = Boolean(activeInternalId) && (from === activeInternalId || to === activeInternalId);

        line.classList.toggle("is-active", isActive);
        line.classList.toggle("is-muted", Boolean(activeInternalId) && !isActive);
    }
}

function openRequirementDialog(requirement) {
    const dialog = document.getElementById("requirementDialog");

    if (!dialog) {
        return;
    }

    setText(
        "dialogTitle",
        requirement.type === "stakeholder"
            ? "Stakeholder Requirement"
            : requirement.type === "systemsBreakdown"
                ? "Physical Structure"
                : "System Requirement",
        ""
    );
    setText("dialogRequirementId", requirement.visibleId, "");
    setText("dialogRequirementName", requirement.name, "");
    setText("dialogRequirementDescription", requirement.description || "—", "");

    const dialogFocusButton = document.getElementById("dialogFocusButton");
    const dialogEditButton = document.getElementById("dialogEditButton");
    if (dialogFocusButton) {
        dialogFocusButton.dataset.internalId = requirement.internalId;
    }

    if (dialogEditButton) {
        dialogEditButton.hidden = !getRequirementEditConfig(requirement);
    }

    const systemsBreakdownsField = document.getElementById("dialogRelatedSystemsBreakdownsField");
    if (systemsBreakdownsField) {
        systemsBreakdownsField.hidden = requirement.type !== "system";
    }

    renderRelatedRequirementList(
        "dialogRelatedRequirements",
        getRelatedRequirements(requirement.internalId, (relatedRequirement) => relatedRequirement.type !== "systemsBreakdown"),
        "No related requirements."
    );

    if (requirement.type === "system") {
        renderRelatedRequirementList(
            "dialogRelatedSystemsBreakdowns",
            getRelatedRequirements(requirement.internalId, (relatedRequirement) => relatedRequirement.type === "systemsBreakdown"),
            "No related physical structure."
        );
    } else {
        renderRelatedRequirementList("dialogRelatedSystemsBreakdowns", [], "No related physical structure.");
    }

    if (!dialog.open) {
        dialog.showModal();
    }

    updateFocusDropZone();
}

function openRequirementEditDialog(requirement) {
    const config = getRequirementEditConfig(requirement);

    if (!config) {
        return;
    }

    const id = requirement.entityId || parseEntityId(requirement.internalId);

    if (!id) {
        window.alert(`${config.label} has no entity id.`);
        return;
    }

    openEditDialog({
        page: config.page,
        mode: "edit",
        id,
        title: `Edit ${config.label}`,
        onSaved: () => window.location.reload()
    });
}

function getRequirementEditConfig(requirement) {
    if (!requirement) {
        return null;
    }

    if (requirement.type === "stakeholder") {
        return {
            label: "Stakeholder Requirement",
            page: "stakeholderrequirement-edit"
        };
    }

    if (requirement.type === "system") {
        return {
            label: "System Requirement",
            page: "systemrequirement-edit"
        };
    }

    if (requirement.type === "systemsBreakdown") {
        return {
            label: "Physical Structure",
            page: "systemsbreakdown-edit"
        };
    }

    return null;
}

function getRelatedRequirementIds(internalId) {
    const relatedIds = new Set();

    for (const relation of state.relations) {
        if (relation.from === internalId) {
            relatedIds.add(relation.to);
        }

        if (relation.to === internalId) {
            relatedIds.add(relation.from);
        }
    }

    return relatedIds;
}

function renderRelatedRequirementList(listId, relatedRequirements, emptyMessage) {
    const relatedList = document.getElementById(listId);

    if (!relatedList) {
        return;
    }

    relatedList.innerHTML = "";

    if (!relatedRequirements.length) {
        const li = document.createElement("li");
        li.textContent = emptyMessage;
        relatedList.appendChild(li);
        return;
    }

    for (const relatedRequirement of relatedRequirements) {
        const li = document.createElement("li");
        li.textContent = `${relatedRequirement.visibleId} - ${relatedRequirement.name}`;
        relatedList.appendChild(li);
    }
}

function getRelatedRequirements(internalId, predicate = () => true) {
    const relatedIds = getRelatedRequirementIds(internalId);

    return Array.from(relatedIds)
        .map((relatedId) => state.requirementsByInternalId.get(relatedId))
        .filter((requirement) => Boolean(requirement) && predicate(requirement))
        .sort((a, b) => naturalCompare(a.visibleId, b.visibleId));
}

function hasAnyRelation(internalId) {
    return state.relations.some((relation) => relation.from === internalId || relation.to === internalId);
}

function findVisibleRequirementCard(internalId) {
    const card = document.querySelector(`.relationdiagram-requirement-card[data-internal-id="${cssEscape(internalId)}"]`);

    if (!card || card.classList.contains("is-hidden-by-filter") || card.closest(".is-hidden-by-view")) {
        return null;
    }

    const rect = card.getBoundingClientRect();

    if (rect.width === 0 || rect.height === 0) {
        return null;
    }

    return card;
}

function showEmptyState(message) {
    const emptyState = document.getElementById("relationdiagramEmptyState");

    if (!emptyState) {
        return;
    }

    emptyState.textContent = message;
    emptyState.classList.add("is-visible");
}

function hideEmptyState() {
    const emptyState = document.getElementById("relationdiagramEmptyState");

    if (!emptyState) {
        return;
    }

    emptyState.classList.remove("is-visible");
}

function applyViewMode(viewMode, options = {}) {
    state.viewMode = viewMode;

    const frame = document.getElementById("relationdiagramFrame");
    if (!frame) {
        return;
    }

    const showStakeholder = viewMode === VIEW_MODES.stakeholderSystem
        || viewMode === VIEW_MODES.stakeholderSystemBreakdown;
    const showBreakdown = viewMode === VIEW_MODES.stakeholderSystemBreakdown
        || viewMode === VIEW_MODES.systemBreakdown;

    frame.dataset.viewMode = viewMode;

    setViewVisibility("stakeholderRequirementsList", "relationsSvgStakeholderSystem", showStakeholder);
    setViewVisibility("systemRequirementsList", null, true);
    setViewVisibility("systemsBreakdownsList", "relationsSvgSystemBreakdown", showBreakdown);

    setButtonPressed("relationdiagramViewStakeholderSystem", viewMode === VIEW_MODES.stakeholderSystem);
    setButtonPressed("relationdiagramViewStakeholderSystemBreakdown", viewMode === VIEW_MODES.stakeholderSystemBreakdown);
    setButtonPressed("relationdiagramViewSystemBreakdown", viewMode === VIEW_MODES.systemBreakdown);

    if (options.redraw !== false) {
        requestAnimationFrame(() => {
            drawRelations();
            updateHighlight();
        });
    }
}

function setViewVisibility(listId, canvasId, isVisible) {
    const list = document.getElementById(listId);
    if (list) {
        list.closest(".relationdiagram-column")?.classList.toggle("is-hidden-by-view", !isVisible);
    }

    if (canvasId) {
        const canvas = document.getElementById(canvasId);
        canvas?.parentElement?.classList.toggle("is-hidden-by-view", !isVisible);
    }
}

function setButtonPressed(buttonId, isPressed) {
    const button = document.getElementById(buttonId);
    if (button) {
        button.setAttribute("aria-pressed", String(isPressed));
    }
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

