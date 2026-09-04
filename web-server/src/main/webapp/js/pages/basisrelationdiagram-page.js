import { initMenu } from "../components/menu.js";
import { initHelpDialog } from "../components/help-dialog.js";
import { openEditDialog } from "../components/edit-dialog.js";
import { createRelationCreationDialogController } from "../components/relation-creation-dialog.js";
import { setText } from "../core/dom.js";
import { applyTopPanelFromDocument as applyPageHeaderFromDocument } from "../core/page-header.js";
import {
    getChildText,
    directTextOf,
    hasXmlParseError
} from "../core/xml.js";
import { cssEscape } from "../core/css.js";
import { naturalCompare } from "../components/sortable-table.js";

const RELATION_DIAGRAM_ENDPOINT = "/basis/relationdiagram?cmd=overview";
const RELATION_CREATE_ENDPOINT = "/basis/entityrelations/createrelation";

const ENTITY_TYPE_LABELS = {
    stakeholder: "Stakeholder Requirement",
    system: "Systems Requirement",
    systemsBreakdown: "Physical Structure"
};

const RELATED_GROUPS = {
    stakeholder: [
        { type: "stakeholder", label: "Related stakeholder requirements" },
        { type: "system", label: "Related system requirements" },
        { type: "systemsBreakdown", label: "Related physical structures" }
    ],
    system: [
        { type: "stakeholder", label: "Related stakeholder requirements" },
        { type: "system", label: "Related system requirements" },
        { type: "systemsBreakdown", label: "Related physical structures" }
    ],
    systemsBreakdown: [
        { type: "stakeholder", label: "Related stakeholder requirements" },
        { type: "system", label: "Related system requirements" },
        { type: "systemsBreakdown", label: "Related physical structures" }
    ]
};

const TYPE_ORDER = {
    stakeholder: 0,
    system: 1,
    systemsBreakdown: 2
};

const state = {
    stakeholderRequirements: [],
    systemRequirements: [],
    systemsBreakdowns: [],
    requirementsByInternalId: new Map(),
    relations: [],
    columnVisibility: {
        stakeholder: true,
        system: true,
        systemsBreakdown: true
    },
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
    applyViewMode(null, { redraw: false });
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

    const viewStakeholder = document.getElementById("viewStakeholder");
    const viewSystem = document.getElementById("viewSystem");
    const viewSystemsBreakdown = document.getElementById("viewSystemsBreakdown");

    viewStakeholder?.addEventListener("change", () => {
        state.columnVisibility.stakeholder = viewStakeholder.checked;
        applyViewMode();
    });

    viewSystem?.addEventListener("change", () => {
        state.columnVisibility.system = viewSystem.checked;
        applyViewMode();
    });

    viewSystemsBreakdown?.addEventListener("change", () => {
        state.columnVisibility.systemsBreakdown = viewSystemsBreakdown.checked;
        applyViewMode();
    });

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


    window.addEventListener("resize", debounce(() => {
        const frame = document.getElementById("relationdiagramFrame");
        if (frame) updateGridLayout(frame);
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
            ?? parseEntityId(internalId, directTextOf(requirementElement, "code") || directTextOf(requirementElement, "id"));
        const requirementType = groupSelector === "stakeholderRequirements"
            ? "stakeholder"
            : groupSelector === "systemRequirements"
                ? "system"
                : "systemsBreakdown";

        return {
            internalId,
            visibleId: directTextOf(requirementElement, "code") || directTextOf(requirementElement, "id") || "—",
            name: directTextOf(requirementElement, "name") || "—",
            description: directTextOf(requirementElement, "description") || "",
            type: requirementType,
            entityId,
            entityTypeId,
            entityTypeLabel: ENTITY_TYPE_LABELS[requirementType] || "Entity"
        };
    }).filter((requirement) => requirement.internalId);
}

function parseRelations(relationDiagramElement) {
    const relationElements = relationDiagramElement.querySelectorAll(
        ":scope > StakeholderRequirementToSystemRequirementRelations > relation, " +
        ":scope > SystemRequirementToSystemsBreakdownRelations > relation, " +
        ":scope > StakeholderRequirementToSystemsBreakdownRelations > relation, " +
        ":scope > StakeholderRequirementToStakeholderRequirementRelations > relation, " +
        ":scope > SystemRequirementToSystemRequirementRelations > relation, " +
        ":scope > SystemsBreakdownToSystemsBreakdownRelations > relation"
    );

    return Array.from(relationElements).map((relationElement, index) => ({
        id: `relation-${index}`,
        from: directTextOf(relationElement, "from"),
        to: directTextOf(relationElement, "to"),
        type: directTextOf(relationElement, "type")
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

    applyViewMode(null, { redraw: false });
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

    applyViewMode(null, { redraw: false });
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
            event.dataTransfer?.setData("text/plain", requirement.visibleId);
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

    const dataId = event.dataTransfer?.getData("application/x-relationdiagram-internal-id")
        || event.dataTransfer?.getData("text/plain")
        || "";

    if (!dataId) {
        return;
    }

    const requirement = findRequirementById(dataId);
    if (!requirement) {
        return;
    }

    setFocusedRequirement(requirement.internalId);
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

    const sourceDataId = event.dataTransfer?.getData("application/x-relationdiagram-internal-id")
        || event.dataTransfer?.getData("text/plain")
        || "";

    const targetInternalId = card.dataset.internalId || "";

    if (!sourceDataId || !targetInternalId) {
        return;
    }

    const sourceRequirement = findRequirementById(sourceDataId);
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

function findRequirementById(id) {
    if (!id) {
        return null;
    }

    if (state.requirementsByInternalId.has(id)) {
        return state.requirementsByInternalId.get(id);
    }

    return Array.from(state.requirementsByInternalId.values())
        .find((r) => r.visibleId === id) || null;
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
    drawGlobalRelations("relationsSvgGlobal");

    const oldSvgs = ["relationsSvgStakeholderSystem", "relationsSvgSystemBreakdown"];
    for (const id of oldSvgs) {
        const svg = document.getElementById(id);
        if (svg) svg.innerHTML = "";
    }
}

function drawGlobalRelations(svgId) {
    const svg = document.getElementById(svgId);
    const frame = document.getElementById("relationdiagramFrame");

    if (!svg || !frame) {
        return;
    }

    svg.innerHTML = "";

    const frameRect = frame.getBoundingClientRect();
    svg.setAttribute("viewBox", `0 0 ${frameRect.width} ${frameRect.height}`);
    svg.setAttribute("width", String(frameRect.width));
    svg.setAttribute("height", String(frameRect.height));

    const { stakeholder, system, systemsBreakdown } = state.columnVisibility;

    for (const relation of state.relations) {
        const fromRequirement = state.requirementsByInternalId.get(relation.from);
        const toRequirement = state.requirementsByInternalId.get(relation.to);

        if (!fromRequirement || !toRequirement) {
            continue;
        }

        // Tjek synlighed af kolonner
        if (fromRequirement.type === "stakeholder" && !stakeholder) continue;
        if (toRequirement.type === "stakeholder" && !stakeholder) continue;
        if (fromRequirement.type === "system" && !system) continue;
        if (toRequirement.type === "system" && !system) continue;
        if (fromRequirement.type === "systemsBreakdown" && !systemsBreakdown) continue;
        if (toRequirement.type === "systemsBreakdown" && !systemsBreakdown) continue;

        const fromCard = findVisibleRequirementCard(relation.from);
        const toCard = findVisibleRequirementCard(relation.to);

        if (!fromCard || !toCard) {
            continue;
        }

        const points = getGlobalRelationPoints(fromCard, toCard, frameRect, fromRequirement, toRequirement);

        if (!points) {
            continue;
        }

        const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
        path.classList.add("relationdiagram-line");
        path.dataset.relationId = relation.id;
        path.dataset.from = relation.from;
        path.dataset.to = relation.to;
        
        if (points.isInternal) {
            path.setAttribute("d", buildInternalCurvePath(points, fromRequirement.type));
        } else {
            path.setAttribute("d", buildCurvePath(points));
        }

        svg.appendChild(path);
    }
}

function getGlobalRelationPoints(fromCard, toCard, frameRect, fromRequirement, toRequirement) {
    const fromRect = fromCard.getBoundingClientRect();
    const toRect = toCard.getBoundingClientRect();

    const fromOrder = TYPE_ORDER[fromRequirement.type];
    const toOrder = TYPE_ORDER[toRequirement.type];

    let x1, y1, x2, y2, isInternal = false;

    if (fromOrder < toOrder) {
        x1 = fromRect.right - frameRect.left;
        x2 = toRect.left - frameRect.left;
        y1 = fromRect.top + fromRect.height / 2 - frameRect.top;
        y2 = toRect.top + toRect.height / 2 - frameRect.top;
    } else if (fromOrder > toOrder) {
        x1 = fromRect.left - frameRect.left;
        x2 = toRect.right - frameRect.left;
        y1 = fromRect.top + fromRect.height / 2 - frameRect.top;
        y2 = toRect.top + toRect.height / 2 - frameRect.top;
    } else {
        isInternal = true;
        if (fromRequirement.type === "systemsBreakdown") {
            x1 = fromRect.right - frameRect.left;
            x2 = toRect.right - frameRect.left;
        } else {
            x1 = fromRect.left - frameRect.left;
            x2 = toRect.left - frameRect.left;
        }
        y1 = fromRect.top + fromRect.height / 2 - frameRect.top;
        y2 = toRect.top + toRect.height / 2 - frameRect.top;
    }

    return { x1, y1, x2, y2, isInternal };
}

function buildInternalCurvePath(points, type) {
    const height = Math.abs(points.y2 - points.y1);
    const x = points.x1;
    const sign = type === "systemsBreakdown" ? 1 : -1;
    
    if (height < 1) {
        // Selv-relation eller næsten samme position
        const size = 30;
        return `M ${x} ${points.y1} C ${x + sign * size} ${points.y1 - size}, ${x + sign * size} ${points.y1 + size}, ${x} ${points.y1}`;
    }
    const curveWidth = Math.min(60, height / 2 + 20);
    return `M ${x} ${points.y1} C ${x + sign * curveWidth} ${points.y1}, ${x + sign * curveWidth} ${points.y2}, ${x} ${points.y2}`;
}

function buildCurvePath(points) {
    const dx = points.x2 - points.x1;
    const distance = Math.max(40, Math.abs(dx));
    const controlOffset = distance * 0.45;
    
    // Determine direction to avoid bending "into" the boxes
    const sign = dx > 0 ? 1 : -1;

    const c1x = points.x1 + (controlOffset * sign);
    const c1y = points.y1;
    const c2x = points.x2 - (controlOffset * sign);
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

function renderRelatedGroups(requirement) {
    const container = document.getElementById("dialogRelatedGroups");

    if (!container) {
        return;
    }

    container.innerHTML = "";

    const groupDefinitions = RELATED_GROUPS[requirement.type] || [];

    groupDefinitions.forEach((group) => {
        const relatedRequirements = getRelatedRequirements(
            requirement.internalId,
            (relatedRequirement) => relatedRequirement.type === group.type
        );

        if (relatedRequirements.length > 0) {
            const section = document.createElement("div");
            section.className = "relationdiagram-dialog-related-group";

            const label = document.createElement("div");
            label.className = "relationdiagram-dialog-label";
            label.textContent = group.label;

            const list = document.createElement("ul");
            list.className = "relationdiagram-related-list";

            relatedRequirements.forEach((relatedRequirement) => {
                const li = document.createElement("li");
                li.textContent = `${relatedRequirement.visibleId} - ${relatedRequirement.name}`;
                list.appendChild(li);
            });

            section.append(label, list);
            container.appendChild(section);
        }
    });

    if (container.innerHTML === "") {
        const empty = document.createElement("div");
        empty.className = "relationdiagram-dialog-value";
        empty.textContent = "No related items.";
        container.appendChild(empty);
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
                : "Systems Requirement",
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

    renderRelatedGroups(requirement);

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
            label: "Systems Requirement",
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

function applyViewMode(unused, options = {}) {
    const frame = document.getElementById("relationdiagramFrame");
    if (!frame) {
        return;
    }

    const { stakeholder, system, systemsBreakdown } = state.columnVisibility;

    setViewVisibility("stakeholderRequirementsList", stakeholder);
    setViewVisibility("systemRequirementsList", system);
    setViewVisibility("systemsBreakdownsList", systemsBreakdown);

    // Mellemrum (canvas) synlighed
    const stakeholderSystemCanvas = document.getElementById("relationsSvgStakeholderSystem")?.parentElement;
    const systemBreakdownCanvas = document.getElementById("relationsSvgSystemBreakdown")?.parentElement;

    // Logik for canvas (mellemrum) synlighed:
    // Vi skal have ét canvas synligt for hvert mellemrum mellem de synlige kolonner.
    const showGap1 = stakeholder && (system || systemsBreakdown);
    const showGap2 = system && systemsBreakdown;

    if (stakeholderSystemCanvas) {
        stakeholderSystemCanvas.classList.toggle("is-hidden-by-view", !showGap1);
    }
    if (systemBreakdownCanvas) {
        systemBreakdownCanvas.classList.toggle("is-hidden-by-view", !showGap2);
    }

    updateGridLayout(frame);

    if (options.redraw !== false) {
        requestAnimationFrame(() => {
            drawRelations();
            updateHighlight();
        });
    }
}

function updateGridLayout(frame) {
    const { stakeholder, system, systemsBreakdown } = state.columnVisibility;
    
    // Hvis vi er på mobil, lad CSS styre layoutet (grid-template-rows)
    if (window.innerWidth <= 720) {
        frame.style.gridTemplateColumns = "";
        return;
    }

    const isCompact = window.innerWidth <= 1000;
    const colWidth = isCompact ? "minmax(200px, 1fr)" : "minmax(240px, 1fr)";
    const gapWidth = isCompact ? "minmax(96px, 140px)" : "minmax(120px, 160px)";

    const columns = [];
    if (stakeholder) columns.push(colWidth);
    if (stakeholder && (system || systemsBreakdown)) columns.push(gapWidth);
    if (system) columns.push(colWidth);
    if (system && systemsBreakdown) columns.push(gapWidth);
    if (systemsBreakdown) columns.push(colWidth);

    if (columns.length === 0) {
        frame.style.gridTemplateColumns = "1fr";
    } else {
        frame.style.gridTemplateColumns = columns.join(" ");
    }
}

function setViewVisibility(listId, isVisible) {
    const list = document.getElementById(listId);
    if (list) {
        list.closest(".relationdiagram-column")?.classList.toggle("is-hidden-by-view", !isVisible);
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

