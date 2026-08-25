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

const RELATION_DIAGRAM_ENDPOINT = "/pro/nflprelationdiagram?cmd=overview";
const RELATION_CREATE_ENDPOINT = "/basis/entityrelations/createrelation";

const STORAGE_KEYS = {
    selectedView: "pro.nflprelationdiagram.selectedView"
};

const ENTITY_TYPES = {
    stakeholder: {
        label: "Stakeholder Requirement",
        page: "stakeholderrequirement-edit"
    },
    system: {
        label: "System Requirement",
        page: "systemrequirement-edit"
    },
    functional: {
        label: "Functional Structure",
        page: "functionalstructure-edit"
    },
    logical: {
        label: "Logical Structure",
        page: "logicalstructure-edit"
    },
    physical: {
        label: "Physical Structure",
        page: "systemsbreakdown-edit"
    }
};

const RELATION_GROUPS = [
    { selector: "StakeholderRequirementToStakeholderRequirementRelations", fromType: "stakeholder", toType: "stakeholder" },
    { selector: "StakeholderRequirementToSystemRequirementRelations", fromType: "stakeholder", toType: "system" },
    { selector: "StakeholderRequirementToFunctionalStructureRelations", fromType: "stakeholder", toType: "functional" },
    { selector: "StakeholderRequirementToLogicalStructureRelations", fromType: "stakeholder", toType: "logical" },
    { selector: "StakeholderRequirementToSystemsBreakdownRelations", fromType: "stakeholder", toType: "physical" },

    { selector: "SystemRequirementToStakeholderRequirementRelations", fromType: "system", toType: "stakeholder" },
    { selector: "SystemRequirementToSystemRequirementRelations", fromType: "system", toType: "system" },
    { selector: "SystemRequirementToFunctionalStructureRelations", fromType: "system", toType: "functional" },
    { selector: "SystemRequirementToLogicalStructureRelations", fromType: "system", toType: "logical" },
    { selector: "SystemRequirementToSystemsBreakdownRelations", fromType: "system", toType: "physical" },

    { selector: "FunctionalStructureToStakeholderRequirementRelations", fromType: "functional", toType: "stakeholder" },
    { selector: "FunctionalStructureToSystemRequirementRelations", fromType: "functional", toType: "system" },
    { selector: "FunctionalStructureToFunctionalStructureRelations", fromType: "functional", toType: "functional" },
    { selector: "FunctionalStructureToLogicalStructureRelations", fromType: "functional", toType: "logical" },
    { selector: "FunctionalStructureToSystemsBreakdownRelations", fromType: "functional", toType: "physical" },

    { selector: "LogicalStructureToStakeholderRequirementRelations", fromType: "logical", toType: "stakeholder" },
    { selector: "LogicalStructureToSystemRequirementRelations", fromType: "logical", toType: "system" },
    { selector: "LogicalStructureToFunctionalStructureRelations", fromType: "logical", toType: "functional" },
    { selector: "LogicalStructureToLogicalStructureRelations", fromType: "logical", toType: "logical" },
    { selector: "LogicalStructureToSystemsBreakdownRelations", fromType: "logical", toType: "physical" },

    { selector: "SystemsBreakdownToStakeholderRequirementRelations", fromType: "physical", toType: "stakeholder" },
    { selector: "SystemsBreakdownToSystemRequirementRelations", fromType: "physical", toType: "system" },
    { selector: "SystemsBreakdownToFunctionalStructureRelations", fromType: "physical", toType: "functional" },
    { selector: "SystemsBreakdownToLogicalStructureRelations", fromType: "physical", toType: "logical" },
    { selector: "SystemsBreakdownToSystemsBreakdownRelations", fromType: "physical", toType: "physical" }
];

const RELATED_GROUPS = {
    stakeholder: [
        { type: "stakeholder", label: "Related stakeholder requirements" },
        { type: "system", label: "Related system requirements" },
        { type: "functional", label: "Related functional structures" },
        { type: "logical", label: "Related logical structures" },
        { type: "physical", label: "Related physical structures" }
    ],
    system: [
        { type: "stakeholder", label: "Related stakeholder requirements" },
        { type: "system", label: "Related system requirements" },
        { type: "functional", label: "Related functional structures" },
        { type: "logical", label: "Related logical structures" },
        { type: "physical", label: "Related physical structures" }
    ],
    functional: [
        { type: "stakeholder", label: "Related stakeholder requirements" },
        { type: "system", label: "Related system requirements" },
        { type: "functional", label: "Related functional structures" },
        { type: "logical", label: "Related logical structures" },
        { type: "physical", label: "Related physical structures" }
    ],
    logical: [
        { type: "stakeholder", label: "Related stakeholder requirements" },
        { type: "system", label: "Related system requirements" },
        { type: "functional", label: "Related functional structures" },
        { type: "logical", label: "Related logical structures" },
        { type: "physical", label: "Related physical structures" }
    ],
    physical: [
        { type: "stakeholder", label: "Related stakeholder requirements" },
        { type: "system", label: "Related system requirements" },
        { type: "functional", label: "Related functional structures" },
        { type: "logical", label: "Related logical structures" },
        { type: "physical", label: "Related physical structures" }
    ]
};

const TYPE_ORDER = {
    stakeholder: 0,
    system: 1,
    functional: 2,
    logical: 3,
    physical: 4
};

const state = {
    stakeholderRequirements: [],
    systemRequirements: [],
    functionalStructures: [],
    logicalStructures: [],
    physicalStructures: [],
    relations: [],
    requirementsByInternalId: new Map(),
    columnVisibility: {
        stakeholder: true,
        system: true,
        functional: true,
        logical: true,
        physical: true
    },
    focusedInternalId: null,
    selectedInternalId: null,
    hoverInternalId: null,
    dialogInternalId: "",
    resizeObserver: null,
    scrollListenersInstalled: false
};

let relationCreationDialog = null;

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", startNflpRelationDiagramPage);
} else {
    startNflpRelationDiagramPage();
}

function startNflpRelationDiagramPage() {
    try {
        initializePageShell();
        loadRelationDiagram();

        try {
            relationCreationDialog = createRelationCreationDialogController({
                endpointUrl: RELATION_CREATE_ENDPOINT,
                title: "Create relation",
                onCreated: () => loadRelationDiagram()
            });
        } catch (dialogError) {
            console.error("Failed to initialize RFLP relation creation dialog", dialogError);
        }

        initializeEvents();
    } catch (error) {
        console.error("Failed to initialize RFLP relation diagram", error);
        setText("loadStatus", "Error");
        showEmptyState(`Could not initialize relation diagram. ${error.message}`);
    }
}

window.addEventListener("error", (event) => {
    console.error("Unhandled RFLP relation diagram error", event.error || event.message || event);
    setText("loadStatus", "Error");
    showEmptyState(`Could not initialize relation diagram. ${event.error?.message || event.message || "Unknown error"}`);
});

window.addEventListener("unhandledrejection", (event) => {
    console.error("Unhandled RFLP relation diagram rejection", event.reason);
    setText("loadStatus", "Error");
    showEmptyState(`Could not initialize relation diagram. ${event.reason?.message || event.reason || "Unknown error"}`);
});

function initializePageShell() {
    setText("customerName", "-");
    setText("projectName", "-");
    setText("userName", "-");
    setText("loadStatus", "Loading");

    initMenu();
    initHelpDialog();

    applyViewMode();
}

function initializeEvents() {
    const viewStakeholder = document.getElementById("viewStakeholder");
    const viewSystem = document.getElementById("viewSystem");
    const viewFunctional = document.getElementById("viewFunctional");
    const viewLogical = document.getElementById("viewLogical");
    const viewPhysical = document.getElementById("viewPhysical");

    viewStakeholder?.addEventListener("change", () => {
        state.columnVisibility.stakeholder = viewStakeholder.checked;
        applyViewMode();
    });

    viewSystem?.addEventListener("change", () => {
        state.columnVisibility.system = viewSystem.checked;
        applyViewMode();
    });

    viewFunctional?.addEventListener("change", () => {
        state.columnVisibility.functional = viewFunctional.checked;
        applyViewMode();
    });

    viewLogical?.addEventListener("change", () => {
        state.columnVisibility.logical = viewLogical.checked;
        applyViewMode();
    });

    viewPhysical?.addEventListener("change", () => {
        state.columnVisibility.physical = viewPhysical.checked;
        applyViewMode();
    });

    const filterOnlyWithoutRelations = document.getElementById("filterOnlyWithoutRelations");
    const filterOnlyWithRelations = document.getElementById("filterOnlyWithRelations");
    const filterRequirementText = document.getElementById("filterRequirementText");
    const focusDropZone = document.getElementById("focusDropZone");
    const btnClearFocus = document.getElementById("btnClearFocus");
    const btnClearSelection = document.getElementById("btnClearSelection");
    const dialogFocusButton = document.getElementById("dialogFocusButton");
    const dialogEditButton = document.getElementById("dialogEditButton");
    const dialogCloseButton = document.getElementById("dialogCloseButton");

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
        const requirement = getDialogRequirement();
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

    document.getElementById("requirementDialog")?.addEventListener("close", () => {
        state.dialogInternalId = "";
        updateDialogActions();
    });

    window.addEventListener("resize", debounce(() => {
        drawRelations();
        updateHighlight();
    }, 80));
}

async function loadRelationDiagram() {
    showEmptyState("Loading relation diagram...");
    setText("loadStatus", "Loading");

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

        setText("loadStatus", "Loaded");
    } catch (error) {
        console.error("Failed to load RFLP relation diagram", error);
        setText("loadStatus", "Error");
        showEmptyState(`Could not load relation diagram. ${error.message}`);
    }
}

function parseRelationDiagramDocument(xmlDocument) {
    const root = xmlDocument.querySelector("NflpRelationDiagram");

    if (!root) {
        throw new Error("Missing NflpRelationDiagram element.");
    }

    const stakeholderRequirements = parseRequirementGroup(root, "stakeholderRequirements", "requirement", "stakeholder");
    const systemRequirements = parseRequirementGroup(root, "systemRequirements", "requirement", "system");
    const functionalStructures = parseRequirementGroup(root, "functionalStructures", "functional", "functional");
    const logicalStructures = parseRequirementGroup(root, "logicalStructures", "logical", "logical");
    const physicalStructures = parseRequirementGroup(root, "systemsBreakdowns", "systemsBreakdown", "physical");
    const relations = RELATION_GROUPS.flatMap((group) => parseRelationGroup(root, group));

    return {
        stakeholderRequirements,
        systemRequirements,
        functionalStructures,
        logicalStructures,
        physicalStructures,
        relations
    };
}

function parseRequirementGroup(parentElement, groupSelector, itemSelector, type) {
    const requirementElements = parentElement.querySelectorAll(`:scope > ${groupSelector} > ${itemSelector}`);

    return Array.from(requirementElements)
        .map((requirementElement) => {
            const internalId = requirementElement.getAttribute("id") || "";

            return {
                internalId,
                entityId: parseInteger(requirementElement.getAttribute("entityId"))
                    ?? parseEntityId(internalId, directTextOf(requirementElement, "code") || directTextOf(requirementElement, "id")),
                visibleId: directTextOf(requirementElement, "code") || directTextOf(requirementElement, "id") || "-",
                name: directTextOf(requirementElement, "name") || "-",
                description: directTextOf(requirementElement, "description") || "",
                type,
                entityTypeId: parseInteger(requirementElement.getAttribute("entityType")),
                entityTypeLabel: ENTITY_TYPES[type]?.label || "Entity"
            };
        })
        .filter((requirement) => requirement.internalId);
}

function parseRelationGroup(root, group) {
    const relationElements = root.querySelectorAll(`:scope > ${group.selector} > relation`);

    return Array.from(relationElements)
        .map((relationElement, index) => ({
            id: `${group.selector}-${index}`,
            from: directTextOf(relationElement, "from"),
            to: directTextOf(relationElement, "to"),
            type: directTextOf(relationElement, "type"),
            fromType: group.fromType,
            toType: group.toType
        }))
        .filter((relation) => relation.from && relation.to);
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
    state.functionalStructures = diagram.functionalStructures;
    state.logicalStructures = diagram.logicalStructures;
    state.physicalStructures = diagram.physicalStructures;
    state.relations = diagram.relations;
    state.focusedInternalId = null;
    state.selectedInternalId = null;
    state.hoverInternalId = null;
    state.requirementsByInternalId = new Map();

    [
        ...state.stakeholderRequirements,
        ...state.systemRequirements,
        ...state.functionalStructures,
        ...state.logicalStructures,
        ...state.physicalStructures
    ].forEach((requirement) => {
        state.requirementsByInternalId.set(requirement.internalId, requirement);
    });

    applyViewMode(state.viewMode, { redraw: false });
}

function renderRelationDiagram() {
    setText("stakeholderRequirementCount", String(state.stakeholderRequirements.length));
    setText("systemRequirementCount", String(state.systemRequirements.length));
    setText("functionalStructureCount", String(state.functionalStructures.length));
    setText("logicalStructureCount", String(state.logicalStructures.length));
    setText("physicalStructureCount", String(state.physicalStructures.length));
    setText("relationCount", String(state.relations.length));

    const lists = [
        ["stakeholderRequirementsList", state.stakeholderRequirements],
        ["systemRequirementsList", state.systemRequirements],
        ["functionalStructuresList", state.functionalStructures],
        ["logicalStructuresList", state.logicalStructures],
        ["physicalStructuresList", state.physicalStructures]
    ];

    lists.forEach(([listId, requirements]) => {
        const list = document.getElementById(listId);
        if (!list) {
            throw new Error(`Missing list element: ${listId}`);
        }

        list.innerHTML = "";
        renderRequirementCards(list, requirements);
    });

    if (
        !state.stakeholderRequirements.length &&
        !state.systemRequirements.length &&
        !state.functionalStructures.length &&
        !state.logicalStructures.length &&
        !state.physicalStructures.length
    ) {
        showEmptyState("No items returned from endpoint.");
        return;
    }

    hideEmptyState();
    updateFocusDropZone();

    if (!state.scrollListenersInstalled) {
        setupScrollListeners();
        state.scrollListenersInstalled = true;
    }

    setupResizeObserver();

    requestAnimationFrame(() => {
        applyFiltersAndRedraw();
    });
}

function renderRequirementCards(container, requirements) {
    requirements.forEach((requirement) => {
        const card = document.createElement("button");
        card.type = "button";
        card.className = "relationdiagram-requirement-card";
        card.dataset.internalId = requirement.internalId;
        card.dataset.entityType = requirement.type;
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

        card.addEventListener("dblclick", (event) => {
            event.preventDefault();
            event.stopPropagation();
            openRequirementEditDialog(requirement);
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
    });
}

function setupScrollListeners() {
    [
        "stakeholderRequirementsList",
        "systemRequirementsList",
        "functionalStructuresList",
        "logicalStructuresList",
        "physicalStructuresList"
    ].forEach((listId) => {
        document.getElementById(listId)?.addEventListener("scroll", handleDiagramScroll, { passive: true });
    });
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

    document.querySelectorAll(".relationdiagram-requirement-card").forEach((card) => {
        const internalId = card.dataset.internalId || "";
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
    });
}

function getFocusedVisibleIds(internalId) {
    return getConnectedRequirementIds(internalId);
}

function getConnectedRequirementIds(internalId) {
    const visited = new Set();
    const queue = [internalId];

    while (queue.length > 0) {
        const currentId = queue.shift();

        if (!currentId || visited.has(currentId)) {
            continue;
        }

        visited.add(currentId);

        getRelatedRequirementIds(currentId).forEach((relatedId) => {
            if (!visited.has(relatedId)) {
                queue.push(relatedId);
            }
        });
    }

    return visited;
}

function setFocusedRequirement(internalId) {
    if (!internalId) {
        return;
    }

    if (state.focusedInternalId && state.focusedInternalId !== internalId) {
        updateFocusDropZone("Clear the current focus before choosing another entity.");
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

    updateDialogActions();
}

function updateDialogActions() {
    const dialogFocusButton = document.getElementById("dialogFocusButton");
    const dialogEditButton = document.getElementById("dialogEditButton");
    const requirement = getDialogRequirement();

    if (!requirement) {
        if (dialogFocusButton) {
            dialogFocusButton.disabled = true;
            dialogFocusButton.textContent = "Focus this";
        }

        if (dialogEditButton) {
            dialogEditButton.hidden = true;
        }

        return;
    }

    if (dialogFocusButton) {
        dialogFocusButton.disabled = Boolean(state.focusedInternalId) && state.focusedInternalId !== requirement.internalId;
        dialogFocusButton.textContent = state.focusedInternalId === requirement.internalId ? "Focused" : "Focus this";
    }

    if (dialogEditButton) {
        dialogEditButton.hidden = !getEditDialogConfig(requirement);
    }
}

function getDialogRequirement() {
    if (!state.dialogInternalId) {
        return null;
    }

    return state.requirementsByInternalId.get(state.dialogInternalId) || null;
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

function parseEntityId(internalId, fallbackValue = "") {
    const fallbackNumeric = parseInteger(fallbackValue);

    if (fallbackNumeric != null) {
        return fallbackNumeric;
    }

    if (!internalId || !internalId.includes("-")) {
        return null;
    }

    return parseInteger(internalId.slice(internalId.lastIndexOf("-") + 1));
}

function drawRelations() {
    const svg = document.getElementById("relationsSvgGlobal");
    const frame = document.getElementById("relationdiagramFrame");

    if (!svg || !frame) return;

    svg.innerHTML = "";
    const frameRect = frame.getBoundingClientRect();

    svg.setAttribute("viewBox", `0 0 ${frameRect.width} ${frameRect.height}`);
    svg.setAttribute("width", String(frameRect.width));
    svg.setAttribute("height", String(frameRect.height));

    state.relations.forEach((relation) => {
        const fromCard = findVisibleRequirementCard(relation.from);
        const toCard = findVisibleRequirementCard(relation.to);

        if (!fromCard || !toCard) return;

        const fromRect = fromCard.getBoundingClientRect();
        const toRect = toCard.getBoundingClientRect();

        const fromType = fromCard.dataset.entityType;
        const toType = toCard.dataset.entityType;

        const fromVisible = state.columnVisibility[fromType];
        const toVisible = state.columnVisibility[toType];

        if (!fromVisible || !toVisible) return;

        const p1 = {
            x: fromRect.left + fromRect.width / 2 - frameRect.left,
            y: fromRect.top + fromRect.height / 2 - frameRect.top
        };
        const p2 = {
            x: toRect.left + toRect.width / 2 - frameRect.left,
            y: toRect.top + toRect.height / 2 - frameRect.top
        };

        const typeOrderFrom = TYPE_ORDER[fromType];
        const typeOrderTo = TYPE_ORDER[toType];

        let x1, x2;
        if (fromType === toType) {
            const isLeftOriented = typeOrderFrom <= 2;
            x1 = isLeftOriented ? fromRect.left - frameRect.left : fromRect.right - frameRect.left;
            x2 = isLeftOriented ? toRect.left - frameRect.left : toRect.right - frameRect.left;
        } else {
            const fromIsLeftOfTo = typeOrderFrom < typeOrderTo;
            x1 = fromIsLeftOfTo ? fromRect.right - frameRect.left : fromRect.left - frameRect.left;
            x2 = fromIsLeftOfTo ? toRect.left - frameRect.left : toRect.right - frameRect.left;
        }

        const points = {
            x1: x1,
            y1: p1.y,
            x2: x2,
            y2: p2.y,
            fromType: fromType,
            toType: toType
        };

        const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
        path.classList.add("relationdiagram-line");
        path.dataset.relationId = relation.id;
        path.dataset.from = relation.from;
        path.dataset.to = relation.to;
        path.setAttribute("d", buildCurvePath(points));

        svg.appendChild(path);
    });
}

function buildCurvePath(points) {
    const { x1, y1, x2, y2, fromType, toType } = points;

    if (fromType === toType) {
        const typeOrder = TYPE_ORDER[fromType];
        const sign = typeOrder <= 2 ? -1 : 1;
        const cp1x = x1 + (60 * sign);
        const cp2x = x2 + (60 * sign);
        return `M ${x1} ${y1} C ${cp1x} ${y1}, ${cp2x} ${y2}, ${x2} ${y2}`;
    }

    const dx = Math.abs(x2 - x1);
    const controlOffset = Math.min(dx * 0.4, 150);

    const cp1x = x1 + (x2 > x1 ? controlOffset : -controlOffset);
    const cp2x = x2 + (x2 > x1 ? -controlOffset : controlOffset);

    return `M ${x1} ${y1} C ${cp1x} ${y1}, ${cp2x} ${y2}, ${x2} ${y2}`;
}

function updateHighlight() {
    const activeInternalId = state.focusedInternalId || state.hoverInternalId || state.selectedInternalId;
    const relatedIds = activeInternalId ? getRelatedRequirementIds(activeInternalId) : new Set();
    const highlightedIds = activeInternalId ? new Set([activeInternalId, ...relatedIds]) : new Set();

    document.querySelectorAll(".relationdiagram-requirement-card").forEach((card) => {
        const internalId = card.dataset.internalId;
        const isSelected = internalId === activeInternalId;
        const isRelated = relatedIds.has(internalId);
        const shouldMute = Boolean(activeInternalId) && !highlightedIds.has(internalId);

        card.classList.toggle("is-selected", isSelected);
        card.classList.toggle("is-related", isRelated);
        card.classList.toggle("is-muted", shouldMute);
    });

    document.querySelectorAll(".relationdiagram-line").forEach((line) => {
        const from = line.dataset.from;
        const to = line.dataset.to;
        const isActive = Boolean(activeInternalId)
            && highlightedIds.has(from)
            && highlightedIds.has(to);

        line.classList.toggle("is-active", isActive);
        line.classList.toggle("is-muted", Boolean(activeInternalId) && !isActive);
    });
}

function openRequirementDialog(requirement) {
    const dialog = document.getElementById("requirementDialog");

    if (!dialog) {
        return;
    }

    state.dialogInternalId = requirement.internalId;

    setText("dialogTitle", ENTITY_TYPES[requirement.type]?.label || "Item");
    setText("dialogRequirementId", requirement.visibleId);
    setText("dialogRequirementName", requirement.name);
    setText("dialogRequirementDescription", requirement.description || "-");

    renderRelatedGroups(requirement);
    updateDialogActions();

    if (!dialog.open) {
        dialog.showModal();
    }

    updateFocusDropZone();
}

function renderRelatedGroups(requirement) {
    const container = document.getElementById("dialogRelatedGroups");

    if (!container) {
        return;
    }

    container.innerHTML = "";

    const groupDefinitions = RELATED_GROUPS[requirement.type] || [];

    if (!groupDefinitions.length) {
        const empty = document.createElement("div");
        empty.className = "relationdiagram-dialog-value";
        empty.textContent = "No related items.";
        container.appendChild(empty);
        return;
    }

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

function openRequirementEditDialog(requirement) {
    const config = getEditDialogConfig(requirement);

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

function getEditDialogConfig(requirement) {
    return requirement ? ENTITY_TYPES[requirement.type] || null : null;
}

function getRelatedRequirementIds(internalId) {
    const relatedIds = new Set();
    const queue = [internalId];

    while (queue.length) {
        const currentInternalId = queue.shift();

        state.relations.forEach((relation) => {
            let relatedInternalId = null;

            if (relation.from === currentInternalId) {
                relatedInternalId = relation.to;
            } else if (relation.to === currentInternalId) {
                relatedInternalId = relation.from;
            }

            if (!relatedInternalId || relatedInternalId === internalId || relatedIds.has(relatedInternalId)) {
                return;
            }

            relatedIds.add(relatedInternalId);
            queue.push(relatedInternalId);
        });
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

function applyViewMode() {
    updateGridLayout();

    requestAnimationFrame(() => {
        drawRelations();
        updateHighlight();
    });
}

function updateGridLayout() {
    const frame = document.getElementById("relationdiagramFrame");
    if (!frame) return;

    const visibility = state.columnVisibility;
    const types = ["stakeholder", "system", "functional", "logical", "physical"];
    const activeTypes = types.filter(type => visibility[type]);

    const isDesktop = window.innerWidth >= 720;

    if (!isDesktop) {
        frame.style.gridTemplateColumns = "";
        return;
    }

    let template = "";
    activeTypes.forEach((type, index) => {
        template += "1fr";
        if (index < activeTypes.length - 1) {
            template += " 70px ";
        }
    });

    frame.style.gridTemplateColumns = template;

    types.forEach((type) => {
        const column = document.querySelector(`.relationdiagram-column[data-entity-type="${type}"]`);
        if (column) {
            column.classList.toggle("is-hidden-by-view", !visibility[type]);
        }
    });

    const canvasWraps = frame.querySelectorAll(".relationdiagram-canvas-wrap");
    canvasWraps.forEach(wrap => wrap.classList.add("is-hidden-by-view"));

    let visibleColCount = 0;
    const totalVisible = activeTypes.length;

    types.forEach((type, index) => {
        if (visibility[type]) {
            visibleColCount++;
            if (visibleColCount < totalVisible) {
                if (canvasWraps[index]) {
                    canvasWraps[index].classList.remove("is-hidden-by-view");
                }
            }
        }
    });
}

function buildRequirementTooltip(requirement) {
    return [
        `ID: ${requirement.visibleId}`,
        `Name: ${requirement.name}`,
        requirement.description ? `Description: ${requirement.description}` : ""
    ].filter(Boolean).join("\n");
}

function parseRequirementIdFromInternalId(internalId) {
    return String(parseEntityId(internalId) || "");
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
