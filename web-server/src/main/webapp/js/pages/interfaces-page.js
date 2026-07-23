import { initMenu } from "../components/menu.js";
import { initHelpDialog } from "../components/help-dialog.js";
import { mountTopbar, applyTopbarMetadata } from "../components/topbar.js";
import { openEditDialog } from "../components/edit-dialog.js";
import {
    closeDialogElement,
    setInputValue,
    setText,
    showDialog
} from "../core/dom.js";
import {
    getAttribute,
    getChildText,
    hasXmlParseError,
    serializeXml
} from "../core/xml.js";

const INTERFACE_ENDPOINT = "/pro/interfacematrix?cmd=overview";
const INTERFACE_SAVE_ENDPOINT = "/pro/interfacematrix?cmd=save";
const FALLBACK_DATE = "";

const state = {
    matrix: null,
    fromStructure: null,
    toStructure: null,
    currentCell: null,
    showOverdueOnly: false,
    selectedIrlIds: []
};

document.addEventListener("DOMContentLoaded", () => {
    initializePageShell();
    initializeEvents();
    loadInterfaceMatrix();
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
    initializeDialogEvents();
    initializeFilterEvents();
    initializeIrlFilterEvents();
}

async function loadInterfaceMatrix() {
    showEmptyState("Loading interface matrix...");
    setText("loadStatus", "Loading", "");

    try {
        const response = await fetch(INTERFACE_ENDPOINT, {
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
            throw new Error("The interface endpoint returned invalid XML.");
        }

        state.matrix = parseInterfaceMatrixDocument(xmlDocument);
        applyTopPanel(xmlDocument);
        renderInterfaceMatrix(state.matrix);

        setText("loadStatus", "Loaded", "");
    } catch (error) {
        console.error("Failed to load interface matrix", error);
        setText("loadStatus", "Error", "");
        showEmptyState(`Could not load interface matrix. ${error.message}`);
    }
}

function parseInterfaceMatrixDocument(xmlDocument) {
    const matrixElement =
        xmlDocument.querySelector("InterfaceMatrixDocument > interfaceMatrix")
        || xmlDocument.querySelector("interfaceMatrix");

    if (!matrixElement) {
        throw new Error("Missing interfaceMatrix element.");
    }

    const metaElement = matrixElement.querySelector(":scope > meta");

    const lookup = {
        trlById: parseLookupMap(matrixElement, "trlMeta > trl", "trlId"),
        irlById: parseLookupMap(matrixElement, "irlMeta > irl", "irlId"),
        classificationById: parseLookupMap(matrixElement, "classificationMeta > classification", "classId"),
        userById: parseLookupMap(matrixElement, "userMeta > user", "userId"),
        departmentById: parseLookupMap(matrixElement, "departmentMeta > department", "departmentId")
    };

    return {
        title: "Interface Management",
        columnGroupLabel: "To Physical Structure",
        rowGroupLabel: "From Physical Structure",
        generatedAt: getChildText(metaElement, "generatedAt", ""),
        structures: parsePhysicalStructures(matrixElement, lookup),
        cellsByKey: parseCells(matrixElement, lookup),
        lookup
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

function parsePhysicalStructures(matrixElement, lookup) {
    const structureElements = Array.from(matrixElement.querySelectorAll(":scope > physicalStructures > physicalStructure"));

    return structureElements.map((structureElement, index) => {
        const entityId = getChildText(structureElement, "entityId", String(index));
        const id = getChildText(structureElement, "id", "");
        const name = getChildText(structureElement, "name", "");

        return {
            entityId,
            id,
            name,
            systemOwnerId: getChildText(structureElement, "systemOwner", ""),
            systemOwnerCode: resolveLookupCode(lookup.userById, getChildText(structureElement, "systemOwner", "")),
            trlId: getChildText(structureElement, "trlId", ""),
            trlCode: resolveLookupCode(lookup.trlById, getChildText(structureElement, "trlId", "")),
            trlColor: resolveLookupColor(lookup.trlById, getChildText(structureElement, "trlId", "")),
            departmentId: getChildText(structureElement, "departmentId", ""),
            departmentCode: resolveLookupCode(lookup.departmentById, getChildText(structureElement, "departmentId", "")),
            deadlineNextTrl: getChildText(structureElement, "deadlineNextTrl", FALLBACK_DATE),
            label: buildStructureLabel(id, name)
        };
    });
}

function parseCells(matrixElement, lookup) {
    const cellsByKey = new Map();
    const cellElements = Array.from(matrixElement.querySelectorAll(":scope > cells > cell"));

    for (const cellElement of cellElements) {
        const fromEntityId = getAttribute(cellElement, "fromEntityId", "");
        const toEntityId = getAttribute(cellElement, "toEntityId", "");

        if (!fromEntityId || !toEntityId) {
            continue;
        }

        const classificationIds = getAttribute(cellElement, "classificationIds", "")
            .split(",")
            .map((value) => value.trim())
            .filter(Boolean);

        cellsByKey.set(buildCellKey(fromEntityId, toEntityId), {
            fromEntityId,
            toEntityId,
            irlId: getAttribute(cellElement, "irlId", ""),
            irlCode: resolveLookupCode(lookup.irlById, getAttribute(cellElement, "irlId", "")),
            irlColor: resolveLookupColor(lookup.irlById, getAttribute(cellElement, "irlId", "")),
            classificationIds,
            classificationCodes: classificationIds.map((id) => resolveLookupCode(lookup.classificationById, id)),
            nextIrlMeeting: getAttribute(cellElement, "nextIrlMeeting", FALLBACK_DATE),
            changedBy: getAttribute(cellElement, "changedBy", ""),
            changed: getAttribute(cellElement, "changed", FALLBACK_DATE)
        });
    }

    return cellsByKey;
}

function applyTopPanel(xmlDocument) {
    if (!xmlDocument.querySelector("TopPanel")) {
        return;
    }

    applyTopbarMetadata(document, xmlDocument);
}

function renderInterfaceMatrix(matrix) {
    const view = buildVisibleMatrix(matrix, state.showOverdueOnly, state.selectedIrlIds);

    setText("interfacesTitle", matrix.title || "Interface Management", "");
    setText(
        "interfacesStructureCount",
        (state.showOverdueOnly || state.selectedIrlIds.length > 0)
            ? `${view.rowStructures.length} rows, ${view.columnStructures.length} cols`
            : String(matrix.structures.length),
        ""
    );
    setText("interfacesColumnGroupLabel", matrix.columnGroupLabel || "To Physical Structure", "");

    const filterToggle = document.getElementById("interfacesOverdueOnlyToggle");
    if (filterToggle) {
        filterToggle.checked = state.showOverdueOnly;
    }

    renderIrlCheckboxList();

    const tableHead = document.getElementById("interfacesTableHead");
    const tableBody = document.getElementById("interfacesTableBody");

    if (!tableHead || !tableBody) {
        throw new Error("Missing table elements.");
    }

    tableHead.innerHTML = "";
    tableBody.innerHTML = "";

    if (!matrix.structures.length) {
        showEmptyState("No physical structures returned from endpoint.");
        return;
    }

    if (state.showOverdueOnly && (!view.rowStructures.length || !view.columnStructures.length)) {
        showEmptyState("No overdue interfaces found.");
        return;
    }

    hideEmptyState();
    renderHeader(tableHead, view);
    renderBody(tableBody, view);
}

function renderHeader(tableHead, matrix) {
    const headerRow = document.createElement("tr");

    const axisCorner = document.createElement("th");
    axisCorner.className = "interfaces-second-corner-axis";
    axisCorner.scope = "col";
    axisCorner.title = matrix.rowGroupLabel;
    headerRow.appendChild(axisCorner);

    const rowHeaderCorner = document.createElement("th");
    rowHeaderCorner.className = "interfaces-second-corner-row";
    rowHeaderCorner.scope = "col";
    rowHeaderCorner.title = matrix.rowGroupLabel;
    headerRow.appendChild(rowHeaderCorner);

    for (const column of matrix.columnStructures || matrix.structures) {
        const th = document.createElement("th");
        th.className = "interfaces-column-header";
        th.scope = "col";
        th.title = `${buildStructureLabel(column.id, column.name)}\nDouble-click to edit Physical Structure`;
        th.dataset.entityId = column.entityId || "";
        applyBackgroundColor(th, column.trlColor);
        th.appendChild(buildStructureHeaderContent(column));

        th.addEventListener("dblclick", () => {
            openPhysicalStructureEditPage(column);
        });

        headerRow.appendChild(th);
    }

    tableHead.replaceChildren(headerRow);
}

function renderBody(tableBody, matrix) {
    const fragment = document.createDocumentFragment();

    (matrix.rowStructures || matrix.structures).forEach((fromStructure, rowIndex) => {
        const tr = document.createElement("tr");

        if (rowIndex === 0) {
            const axisCell = document.createElement("th");
            axisCell.className = "interfaces-axis-cell";
            axisCell.rowSpan = (matrix.rowStructures || matrix.structures).length;
            axisCell.scope = "rowgroup";
            axisCell.title = matrix.rowGroupLabel;

            const axisLabel = document.createElement("div");
            axisLabel.className = "interfaces-axis-label";

            const axisText = document.createElement("span");
            axisText.textContent = matrix.rowGroupLabel || "From Physical Structure";

            axisLabel.appendChild(axisText);
            axisCell.appendChild(axisLabel);
            tr.appendChild(axisCell);
        }

        const rowHeader = document.createElement("th");
        rowHeader.className = "interfaces-row-header";
        rowHeader.scope = "row";
        rowHeader.title = `${buildStructureLabel(fromStructure.id, fromStructure.name)}\nDouble-click to edit Physical Structure`;
        rowHeader.dataset.entityId = fromStructure.entityId || "";
        applyBackgroundColor(rowHeader, fromStructure.trlColor);
        rowHeader.appendChild(buildStructureHeaderContent(fromStructure));

        rowHeader.addEventListener("dblclick", () => {
            openPhysicalStructureEditPage(fromStructure);
        });

        tr.appendChild(rowHeader);

        for (const toStructure of (matrix.columnStructures || matrix.structures)) {
            const td = document.createElement("td");
            const cell = matrix.cellsByKey.get(buildCellKey(fromStructure.entityId, toStructure.entityId)) || null;
            const isSelfReference = fromStructure.entityId === toStructure.entityId;

            td.className = isSelfReference ? "interfaces-cell interfaces-cell-self" : "interfaces-cell";
            td.dataset.fromEntityId = fromStructure.entityId || "";
            td.dataset.toEntityId = toStructure.entityId || "";
            td.title = buildCellTooltip(fromStructure, toStructure, cell, matrix.lookup, isSelfReference);

            if (!isSelfReference) {
                applyBackgroundColor(td, cell?.irlColor || "");
                td.addEventListener("dblclick", (event) => {
                    event.preventDefault();
                    event.stopPropagation();
                    openInterfaceDialog(fromStructure, toStructure, cell);
                });
            } else {
                applyBackgroundColor(td, "");
            }

            if (cell) {
                renderCellValue(td, cell);
            }

            tr.appendChild(td);
        }

        fragment.appendChild(tr);
    });

    tableBody.replaceChildren(fragment);
}

function buildVisibleMatrix(matrix, showOverdueOnly, selectedIrlIds) {
    const selectedIrlSet = new Set(selectedIrlIds || []);

    if (!showOverdueOnly && selectedIrlSet.size === 0) {
        return matrix;
    }

    const visibleRowEntityIds = new Set();
    const visibleColumnEntityIds = new Set();
    const visibleCells = new Map();

    for (const cell of matrix.cellsByKey.values()) {
        if (showOverdueOnly && !isCellOverdue(cell)) {
            continue;
        }

        if (selectedIrlSet.size > 0 && !selectedIrlSet.has(cell.irlId)) {
            continue;
        }

        if (cell.fromEntityId) {
            visibleRowEntityIds.add(cell.fromEntityId);
        }

        if (cell.toEntityId) {
            visibleColumnEntityIds.add(cell.toEntityId);
        }

        visibleCells.set(buildCellKey(cell.fromEntityId, cell.toEntityId), cell);
    }

    const rowStructures = matrix.structures.filter((structure) => visibleRowEntityIds.has(structure.entityId));
    const columnStructures = matrix.structures.filter((structure) => visibleColumnEntityIds.has(structure.entityId));

    return {
        ...matrix,
        rowStructures,
        columnStructures,
        cellsByKey: visibleCells
    };
}

function buildStructureHeaderContent(structure) {
    const container = document.createElement("span");
    container.className = "interfaces-header-content";

    const idSpan = document.createElement("span");
    idSpan.className = "interfaces-header-id";
    idSpan.textContent = structure.id || "-";

    const nameSpan = document.createElement("span");
    nameSpan.className = "interfaces-header-name";
    nameSpan.textContent = structure.name || "-";

    container.append(idSpan, nameSpan);
    return container;
}

function renderCellValue(cellElement, cell) {
    cellElement.innerHTML = "";

    const lines = [];
    const nextIrlMeetingDate = parseLocalDate(cell.nextIrlMeeting);
    const isOverdue = Boolean(nextIrlMeetingDate) && nextIrlMeetingDate < startOfToday();

    if (cell.irlCode) {
        lines.push(["IRL", cell.irlCode]);
    }

    if (cell.classificationCodes?.length) {
        lines.push(["Classification", cell.classificationCodes.join(", ")]);
    }

    if (cell.nextIrlMeeting) {
        lines.push(["Next Irl Meeting", formatDanishDate(cell.nextIrlMeeting)]);
    }

    if (!lines.length) {
        if (isOverdue) {
            cellElement.appendChild(createOverdueBadge());
        }
        return;
    }

    const container = document.createElement("div");
    container.className = "interfaces-cell-content";

    for (const [label, value] of lines) {
        const line = document.createElement("div");
        line.className = "interfaces-cell-line";

        const labelSpan = document.createElement("span");
        labelSpan.className = "interfaces-cell-line-label";
        labelSpan.textContent = `${label}:`;

        const valueSpan = document.createElement("span");
        valueSpan.className = label === "IRL"
            ? "interfaces-cell-line-value interfaces-cell-line-value--irl"
            : "interfaces-cell-line-value";
        valueSpan.textContent = value;
        if (label === "IRL") {
            valueSpan.title = value;
        }

        line.append(labelSpan, valueSpan);
        container.appendChild(line);
    }

    cellElement.appendChild(container);

    if (isOverdue) {
        cellElement.appendChild(createOverdueBadge());
    }
}

function isCellOverdue(cell) {
    if (!cell?.nextIrlMeeting) {
        return false;
    }

    const nextIrlMeetingDate = parseLocalDate(cell.nextIrlMeeting);
    return Boolean(nextIrlMeetingDate) && nextIrlMeetingDate < startOfToday();
}

function buildCellTooltip(fromStructure, toStructure, cell, lookup, isSelfReference) {
    if (isSelfReference) {
        return "Same physical structure";
    }

    const lines = [
        `From: ${buildStructureLabel(fromStructure.id, fromStructure.name)}`,
        `To: ${buildStructureLabel(toStructure.id, toStructure.name)}`
    ];

    if (!cell) {
        return lines.join("\n");
    }

    lines.push(`Changed By: ${resolveLookupCode(lookup?.userById, cell.changedBy) || "-"}`);
    lines.push(`Changed: ${cell.changed || "-"}`);

    return lines.join("\n");
}

function openPhysicalStructureEditPage(structure) {
    const id = structure?.entityId || "";

    if (!id) {
        window.alert("Physical Structure has no entity id.");
        return;
    }

    openEditDialog({
        page: "systemsbreakdown-edit",
        mode: "edit",
        id,
        title: "Edit Physical Structure",
        onSaved: () => window.location.reload()
    });
}

function initializeDialogEvents() {
    const dialog = document.getElementById("interfaceDialog");
    const cancelButton = document.getElementById("interfaceDialogCancelButton");
    const saveButton = document.getElementById("interfaceDialogSaveButton");

    cancelButton?.addEventListener("click", () => {
        closeInterfaceDialog(dialog);
    });

    saveButton?.addEventListener("click", async () => {
        await saveCurrentInterface();
    });

    dialog?.addEventListener("cancel", (event) => {
        event.preventDefault();
        closeInterfaceDialog(dialog);
    });
}

function initializeFilterEvents() {
    const toggle = document.getElementById("interfacesOverdueOnlyToggle");

    toggle?.addEventListener("change", () => {
        state.showOverdueOnly = Boolean(toggle.checked);
        if (state.matrix) {
            renderInterfaceMatrix(state.matrix);
        }
    });
}

function addIrlFilter(irlId) {
    const normalizedIrlId = String(irlId || "").trim();
    if (!normalizedIrlId) {
        return;
    }

    if (state.selectedIrlIds.includes(normalizedIrlId)) {
        return;
    }

    state.selectedIrlIds = [...state.selectedIrlIds, normalizedIrlId];
    if (state.matrix) {
        renderInterfaceMatrix(state.matrix);
    }
}

function removeIrlFilter(irlId) {
    const normalizedIrlId = String(irlId || "").trim();
    if (!normalizedIrlId) {
        return;
    }

    state.selectedIrlIds = state.selectedIrlIds.filter((value) => value !== normalizedIrlId);
    if (state.matrix) {
        renderInterfaceMatrix(state.matrix);
    }
}

function clearIrlFilters() {
    if (!state.selectedIrlIds.length) {
        return;
    }

    state.selectedIrlIds = [];
    if (state.matrix) {
        renderInterfaceMatrix(state.matrix);
    }
}

function initializeIrlFilterEvents() {
    const list = document.getElementById("interfacesIrlFilterList");

    list?.addEventListener("change", (event) => {
        const target = event.target;

        if (!(target instanceof HTMLInputElement) || target.type !== "checkbox") {
            return;
        }

        const irlId = target.getAttribute("data-irl-id") || "";
        if (!irlId) {
            return;
        }

        if (target.checked) {
            addIrlFilter(irlId);
        } else {
            removeIrlFilter(irlId);
        }
    });
}

function renderIrlCheckboxList() {
    const list = document.getElementById("interfacesIrlFilterList");
    if (!list || !state.matrix) {
        return;
    }

    list.innerHTML = "";

    for (const irl of state.matrix.lookup.irlById.values()) {
        const label = document.createElement("label");
        label.className = "interfaces-irl-filter-item";
        label.title = buildIrlTooltip(irl);

        const checkbox = document.createElement("input");
        checkbox.type = "checkbox";
        checkbox.checked = state.selectedIrlIds.includes(irl.id);
        checkbox.setAttribute("data-irl-id", irl.id);
        checkbox.setAttribute("aria-label", buildIrlTooltip(irl));

        const code = document.createElement("span");
        code.className = "interfaces-irl-filter-code";
        code.textContent = compactIrlLabel(irl.code || irl.id);

        label.append(code, checkbox);
        list.appendChild(label);
    }
}

function openInterfaceDialog(fromStructure, toStructure, cell) {
    const dialog = document.getElementById("interfaceDialog");

    if (!dialog) {
        return;
    }

    state.fromStructure = fromStructure || null;
    state.toStructure = toStructure || null;
    state.currentCell = cell || null;

    setText("interfaceDialogTitle", "Interface", "");
    setText("interfaceDialogStatus", cell ? "Editing existing interface" : "Creating new interface", "");

    renderStructureCard("interfaceFromFields", fromStructure);
    renderStructureCard("interfaceToFields", toStructure);
    renderIrlSelect(cell?.irlId || "");
    renderNextIrlMeeting(cell?.nextIrlMeeting || "");
    renderClassificationList(cell?.classificationIds || []);

    showDialog(dialog);
}

function renderStructureCard(containerId, structure) {
    const container = document.getElementById(containerId);

    if (!container) {
        return;
    }

    container.innerHTML = "";

    const fields = [
        ["ID", structure?.id || "-"],
        ["Name", structure?.name || "-"],
        ["System Owner", structure?.systemOwnerCode || "-"],
        ["TRL", structure?.trlCode || "-"],
        ["Department", structure?.departmentCode || "-"],
        ["Deadline Next Trl", structure?.deadlineNextTrl || "-"]
    ];

    for (const [label, value] of fields) {
        const field = document.createElement("div");
        field.className = "interfaces-dialog-card-field";

        const fieldLabel = document.createElement("span");
        fieldLabel.className = "interfaces-dialog-card-field-label";
        fieldLabel.textContent = label;

        const fieldValue = document.createElement("strong");
        fieldValue.className = "interfaces-dialog-card-field-value";
        fieldValue.textContent = value;

        field.append(fieldLabel, fieldValue);
        container.appendChild(field);
    }
}

function renderIrlSelect(selectedIrlId) {
    const select = document.getElementById("interfaceDialogIrlSelect");

    if (!select || !state.matrix) {
        return;
    }

    select.innerHTML = "";

    const emptyOption = document.createElement("option");
    emptyOption.value = "";
    emptyOption.textContent = "Select IRL";
    select.appendChild(emptyOption);

    for (const irl of state.matrix.lookup.irlById.values()) {
        const option = document.createElement("option");
        option.value = irl.id;
        option.textContent = irl.code || irl.id;
        select.appendChild(option);
    }

    select.value = selectedIrlId || "";
}

function renderNextIrlMeeting(value) {
    setInputValue("interfaceDialogNextIrlMeeting", value || "", "");
}

function renderClassificationList(selectedClassificationIds) {
    const container = document.getElementById("interfaceDialogClassificationList");

    if (!container || !state.matrix) {
        return;
    }

    container.innerHTML = "";

    const selectedSet = new Set(selectedClassificationIds || []);

    for (const classification of state.matrix.lookup.classificationById.values()) {
        const row = document.createElement("label");
        row.className = "interfaces-dialog-classification-item";

        const checkbox = document.createElement("input");
        checkbox.type = "checkbox";
        checkbox.value = classification.id;
        checkbox.checked = selectedSet.has(classification.id);

        const text = document.createElement("span");
        text.className = "interfaces-dialog-classification-text";

        const codeSpan = document.createElement("strong");
        codeSpan.textContent = classification.code || classification.id;

        const descriptionSpan = document.createElement("span");
        descriptionSpan.textContent = classification.description ? ` - ${classification.description}` : "";

        text.append(codeSpan, descriptionSpan);
        text.title = classification.description || classification.code || classification.id;

        row.append(checkbox, text);
        container.appendChild(row);
    }
}

function getInterfaceDialogDraft() {
    const irlSelect = document.getElementById("interfaceDialogIrlSelect");
    const nextIrlMeeting = document.getElementById("interfaceDialogNextIrlMeeting");
    const classificationList = document.getElementById("interfaceDialogClassificationList");

    const classificationIds = Array.from(classificationList?.querySelectorAll("input[type='checkbox']:checked") || [])
        .map((checkbox) => checkbox.value)
        .filter(Boolean);

    return {
        fromEntityId: state.fromStructure?.entityId || "",
        toEntityId: state.toStructure?.entityId || "",
        irlId: irlSelect?.value || "",
        nextIrlMeeting: nextIrlMeeting?.value || "",
        classificationIds
    };
}

async function saveCurrentInterface() {
    const draft = getInterfaceDialogDraft();

    if (!draft.fromEntityId || !draft.toEntityId) {
        window.alert("Missing from/to entity id.");
        return;
    }

    if (!draft.irlId) {
        window.alert("IRL is required.");
        return;
    }

    try {
        setText("interfaceDialogStatus", "Saving...");
        setText("loadStatus", "Saving", "");

        const payload = buildInterfaceSavePayload(draft);
        const response = await fetch(INTERFACE_SAVE_ENDPOINT, {
            method: "POST",
            headers: {
                "Content-Type": "application/xml; charset=UTF-8",
                "Accept": "application/xml,text/xml,*/*"
            },
            body: payload,
            cache: "no-store"
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status} ${response.statusText}`);
        }

        setText("interfaceDialogStatus", "Saved.");
        setText("loadStatus", "Saved", "");
        closeInterfaceDialog(document.getElementById("interfaceDialog"));
        window.location.reload();
    } catch (error) {
        console.error("Failed to save interface cell", error);
        setText("interfaceDialogStatus", `Save failed. ${error.message}`);
        setText("loadStatus", "Error", "");
        window.alert(`Save failed. ${error.message}`);
    }
}

function buildInterfaceSavePayload(draft) {
    const doc = document.implementation.createDocument("", "InterfaceMatrixSaveDocument", null);
    const root = doc.documentElement;
    const matrix = doc.createElement("interfaceMatrix");
    const cell = doc.createElement("cell");

    appendXmlTextElement(doc, cell, "fromEntityId", draft.fromEntityId);
    appendXmlTextElement(doc, cell, "toEntityId", draft.toEntityId);
    appendXmlTextElement(doc, cell, "irlId", draft.irlId);
    appendXmlTextElement(doc, cell, "nextIrlMeeting", draft.nextIrlMeeting || "");
    appendXmlTextElement(doc, cell, "classificationIds", draft.classificationIds.join(","));

    matrix.appendChild(cell);
    root.appendChild(matrix);

    return serializeXml(doc);
}

function appendXmlTextElement(doc, parent, tagName, value) {
    const element = doc.createElement(tagName);
    element.textContent = value ?? "";
    parent.appendChild(element);
}

function closeInterfaceDialog(dialog) {
    state.fromStructure = null;
    state.toStructure = null;
    state.currentCell = null;
    closeDialogElement(dialog);
}

function resolveLookupCode(map, id) {
    if (!id) {
        return "";
    }

    return map?.get(id)?.code || id;
}

function buildIrlTooltip(irl) {
    if (!irl) {
        return "";
    }

    return `${irl.code || irl.id || "-"}${irl.description ? ` - ${irl.description}` : ""}`;
}

function compactIrlLabel(value) {
    const text = String(value || "").trim();
    if (!text) {
        return "?";
    }

    return text.charAt(0);
}

function resolveLookupColor(map, id) {
    if (!id) {
        return "";
    }

    return map?.get(id)?.color || "";
}

function applyBackgroundColor(element, color) {
    if (!element) {
        return;
    }

    if (color) {
        element.style.backgroundColor = color;
        return;
    }

    element.style.backgroundColor = "";
}

function buildStructureLabel(id, name) {
    return `${id || "-"} - ${name || "-"}`;
}

function buildCellKey(fromEntityId, toEntityId) {
    return `${fromEntityId || ""}|${toEntityId || ""}`;
}

function formatDanishDate(value) {
    const date = parseLocalDate(value);

    if (!date) {
        return value || "";
    }

    const day = String(date.getDate()).padStart(2, "0");
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const year = String(date.getFullYear());

    return `${day}/${month}-${year}`;
}

function parseLocalDate(value) {
    if (!value) {
        return null;
    }

    const match = String(value).trim().match(/^(\d{4})-(\d{2})-(\d{2})$/);

    if (!match) {
        return null;
    }

    const year = Number(match[1]);
    const month = Number(match[2]) - 1;
    const day = Number(match[3]);
    const date = new Date(year, month, day);

    return Number.isNaN(date.getTime()) ? null : date;
}

function startOfToday() {
    const today = new Date();
    return new Date(today.getFullYear(), today.getMonth(), today.getDate());
}

function createOverdueBadge() {
    const badge = document.createElement("span");
    badge.className = "interfaces-cell-overdue-badge";
    badge.title = "Next IRL meeting is overdue";
    badge.setAttribute("aria-label", "Next IRL meeting is overdue");
    badge.innerHTML = `
        <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <path d="M6.2 8.2a.95.95 0 0 1-1.35 0 4.4 4.4 0 0 1 0-6.22.95.95 0 1 1 1.35 1.34 2.5 2.5 0 0 0 0 3.54.95.95 0 0 1 0 1.34z"></path>
            <path d="M17.8 8.2a.95.95 0 0 0 1.35 0 4.4 4.4 0 0 0 0-6.22.95.95 0 1 0-1.35 1.34 2.5 2.5 0 0 1 0 3.54.95.95 0 0 0 0 1.34z"></path>
            <path d="M12 2.25c-3.7 0-6.7 2.96-6.7 6.6v2.55c0 .95-.39 1.86-1.08 2.52l-.46.44A.95.95 0 0 0 4.4 16h15.2a.95.95 0 0 0 .64-1.64l-.46-.44a3.55 3.55 0 0 1-1.08-2.52V8.85c0-3.64-3-6.6-6.7-6.6z"></path>
            <path d="M9.8 17.2h4.4v.65a2.2 2.2 0 1 1-4.4 0v-.65z"></path>
        </svg>
    `;
    return badge;
}

function showEmptyState(message) {
    const emptyState = document.getElementById("interfacesEmptyState");

    if (!emptyState) {
        return;
    }

    emptyState.textContent = message;
    emptyState.classList.add("is-visible");
}

function hideEmptyState() {
    const emptyState = document.getElementById("interfacesEmptyState");

    if (!emptyState) {
        return;
    }

    emptyState.classList.remove("is-visible");
}
