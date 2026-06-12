import { initMenu } from "../components/menu.js";
import { setText } from "../core/dom.js";
import {
    getChildText,
    hasXmlParseError
} from "../core/xml.js";
import { cssEscape } from "../core/css.js";
import { naturalCompare } from "../components/sortable-table.js";

const RELATION_DIAGRAM_ENDPOINT = "/basis/relationdiagram?cmd=overview";

const state = {
    stakeholderRequirements: [],
    systemRequirements: [],
    requirementsByInternalId: new Map(),
    relations: [],
    selectedInternalId: null,
    hoverInternalId: null,
    resizeObserver: null
};

document.addEventListener("DOMContentLoaded", () => {
    initializePageShell();
    initializeEvents();
    loadRelationDiagram();
});

function initializePageShell() {
    setText("customerName", "—", "");
    setText("projectName", "—", "");
    setText("userName", "—", "");
    setText("loadStatus", "Loading", "");

    initMenu();
}

function initializeEvents() {
    const filterOnlyWithoutRelations = document.getElementById("filterOnlyWithoutRelations");
    const filterOnlyWithRelations = document.getElementById("filterOnlyWithRelations");
    const filterRequirementText = document.getElementById("filterRequirementText");
    const btnClearSelection = document.getElementById("btnClearSelection");
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

    btnClearSelection?.addEventListener("click", () => {
        state.selectedInternalId = null;
        state.hoverInternalId = null;

        if (filterOnlyWithoutRelations) {
            filterOnlyWithoutRelations.checked = false;
        }

        if (filterOnlyWithRelations) {
            filterOnlyWithRelations.checked = false;
        }

        if (filterRequirementText) {
            filterRequirementText.value = "";
        }

        applyFiltersAndRedraw();
    });

    dialogCloseButton?.addEventListener("click", () => {
        const dialog = document.getElementById("requirementDialog");

        if (dialog?.open) {
            dialog.close();
        }
    });

    window.addEventListener("resize", debounce(() => {
        drawRelations();
        updateHighlight();
    }, 80));
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
        "stakeholderRequirements"
    );

    const systemRequirements = parseRequirements(
        relationDiagramElement,
        "systemRequirements"
    );

    const relations = parseRelations(relationDiagramElement);

    return {
        stakeholderRequirements,
        systemRequirements,
        relations
    };
}

function parseRequirements(parentElement, groupSelector) {
    const requirementElements = parentElement.querySelectorAll(
        `:scope > ${groupSelector} > requirement`
    );

    return Array.from(requirementElements).map((requirementElement) => {
        const internalId = requirementElement.getAttribute("id") || "";

        return {
            internalId,
            visibleId: getChildText(requirementElement, "id", "—"),
            name: getChildText(requirementElement, "name", "—"),
            description: getChildText(requirementElement, "description", ""),
            type: groupSelector === "stakeholderRequirements" ? "stakeholder" : "system"
        };
    }).filter((requirement) => requirement.internalId);
}

function parseRelations(relationDiagramElement) {
    const relationElements = relationDiagramElement.querySelectorAll(":scope > relations > relation");

    return Array.from(relationElements).map((relationElement, index) => ({
        id: `relation-${index}`,
        from: getChildText(relationElement, "from", ""),
        to: getChildText(relationElement, "to", "")
    })).filter((relation) => relation.from && relation.to);
}

function applyTopPanel(xmlDocument) {
    const topPanelElement = xmlDocument.querySelector("TopPanel");

    if (!topPanelElement) {
        return;
    }

    setText("customerName", getChildText(topPanelElement, "CustomerName", "—"), "");
    setText("projectName", getChildText(topPanelElement, "ProjectName", "—"), "");
    setText("userName", getChildText(topPanelElement, "Name", "—"), "");
}

function setRelationDiagramState(diagram) {
    state.stakeholderRequirements = diagram.stakeholderRequirements;
    state.systemRequirements = diagram.systemRequirements;
    state.relations = diagram.relations;
    state.selectedInternalId = null;
    state.hoverInternalId = null;
    state.requirementsByInternalId = new Map();

    for (const requirement of state.stakeholderRequirements) {
        state.requirementsByInternalId.set(requirement.internalId, requirement);
    }

    for (const requirement of state.systemRequirements) {
        state.requirementsByInternalId.set(requirement.internalId, requirement);
    }
}

function renderRelationDiagram() {
    setText("stakeholderRequirementCount", String(state.stakeholderRequirements.length), "");
    setText("systemRequirementCount", String(state.systemRequirements.length), "");
    setText("relationCount", String(state.relations.length), "");

    const stakeholderList = document.getElementById("stakeholderRequirementsList");
    const systemList = document.getElementById("systemRequirementsList");

    if (!stakeholderList || !systemList) {
        throw new Error("Missing relation diagram list elements.");
    }

    stakeholderList.innerHTML = "";
    systemList.innerHTML = "";

    if (!state.stakeholderRequirements.length && !state.systemRequirements.length) {
        showEmptyState("No requirements returned from endpoint.");
        return;
    }

    hideEmptyState();

    renderRequirementCards(stakeholderList, state.stakeholderRequirements);
    renderRequirementCards(systemList, state.systemRequirements);

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

    stakeholderList?.addEventListener("scroll", handleDiagramScroll, { passive: true });
    systemList?.addEventListener("scroll", handleDiagramScroll, { passive: true });
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

    const cards = document.querySelectorAll(".relationdiagram-requirement-card");

    for (const card of cards) {
        const internalId = card.dataset.internalId;
        const requirement = state.requirementsByInternalId.get(internalId);
        const hasRelations = hasAnyRelation(internalId);

        let shouldShow = true;

        if (onlyWithoutRelations) {
            shouldShow = !hasRelations;
        }

        if (onlyWithRelations) {
            shouldShow = hasRelations;
        }

        if (shouldShow && searchText) {
            shouldShow = requirementMatchesSearch(requirement, searchText);
        }

        card.classList.toggle("is-hidden-by-filter", !shouldShow);
    }
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
    const svg = document.getElementById("relationsSvg");
    const canvasWrap = document.querySelector(".relationdiagram-canvas-wrap");

    if (!svg || !canvasWrap) {
        return;
    }

    svg.innerHTML = "";

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

        const points = getRelationPoints(fromCard, toCard, canvasRect, fromRequirement, toRequirement);

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

function getRelationPoints(fromCard, toCard, canvasRect, fromRequirement, toRequirement) {
    const fromRect = fromCard.getBoundingClientRect();
    const toRect = toCard.getBoundingClientRect();

    const fromIsStakeholder = fromRequirement.type === "stakeholder";
    const toIsStakeholder = toRequirement.type === "stakeholder";

    let leftRect;
    let rightRect;

    if (fromIsStakeholder && !toIsStakeholder) {
        leftRect = fromRect;
        rightRect = toRect;
    } else if (!fromIsStakeholder && toIsStakeholder) {
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
    const activeInternalId = state.hoverInternalId || state.selectedInternalId;
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

    setText("dialogTitle", requirement.type === "stakeholder" ? "Stakeholder Requirement" : "System Requirement", "");
    setText("dialogRequirementId", requirement.visibleId, "");
    setText("dialogRequirementName", requirement.name, "");
    setText("dialogRequirementDescription", requirement.description || "—", "");

    const relatedList = document.getElementById("dialogRelatedRequirements");

    if (relatedList) {
        relatedList.innerHTML = "";

        const relatedRequirements = getRelatedRequirements(requirement.internalId);

        if (!relatedRequirements.length) {
            const li = document.createElement("li");
            li.textContent = "No related requirements.";
            relatedList.appendChild(li);
        } else {
            for (const relatedRequirement of relatedRequirements) {
                const li = document.createElement("li");
                li.textContent = `${relatedRequirement.visibleId} - ${relatedRequirement.name}`;
                relatedList.appendChild(li);
            }
        }
    }

    if (!dialog.open) {
        dialog.showModal();
    }
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

function getRelatedRequirements(internalId) {
    const relatedIds = getRelatedRequirementIds(internalId);

    return Array.from(relatedIds)
        .map((relatedId) => state.requirementsByInternalId.get(relatedId))
        .filter(Boolean)
        .sort((a, b) => naturalCompare(a.visibleId, b.visibleId));
}

function hasAnyRelation(internalId) {
    return state.relations.some((relation) => relation.from === internalId || relation.to === internalId);
}

function findVisibleRequirementCard(internalId) {
    const card = document.querySelector(`.relationdiagram-requirement-card[data-internal-id="${cssEscape(internalId)}"]`);

    if (!card || card.classList.contains("is-hidden-by-filter")) {
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

function debounce(callback, delay) {
    let timeoutId = null;

    return (...args) => {
        window.clearTimeout(timeoutId);

        timeoutId = window.setTimeout(() => {
            callback(...args);
        }, delay);
    };
}