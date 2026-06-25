import { nowIsoLocal, parseDateTime } from "../core/date.js";
import {
    appendTextElement,
    getDirectChild,
    textOf
} from "../core/xml.js";
import { escapeHtml } from "../core/html.js";

const ENTITY_RELATIONS_TABLE_VERSION = "relation-dialog-xml-request-2026-06-24";

const DEFAULT_CONFIG = {
    bodyId: "relationsBody",
    emptyId: "relationsEmpty",
    addButtonId: "relationsAddNew",

    dialogId: "relationDialog",
    titleId: "relationDlgTitle",
    statusId: "relationDlgStatus",
    okButtonId: "relationOkBtn",
    noRelevantButtonId: "relationNoRelevantBtn",
    cancelButtonId: "relationCancelBtn",
    closeButtonId: "",

    relationContainerName: "EntityRelations",
    relationElementName: "EntityRelation",
    relationRequestUrl: "",

    readOnly: false,
    confirmDelete: true,
    allowOpenOnRowDoubleClick: true,

    onAddRelationRequest: null,
    onChange: null,
    onAfterRender: null
};

function normalizeText(value) {
    return value == null ? "" : String(value);
}

function normalizeBoolean(value) {
    if (typeof value === "boolean") {
        return value;
    }

    const normalized = normalizeText(value).trim().toLowerCase();

    return normalized === "true"
        || normalized === "1"
        || normalized === "yes";
}

function normalizeRelation(relation = {}) {
        return {
            entityRelationPK: relation.entityRelationPK ?? "",
            entityId: relation.entityId ?? "",
            entityType: relation.entityType ?? "",
            relatedEntityId: relation.relatedEntityId ?? "",
            relatedEntityType: relation.relatedEntityType ?? "",
            createdById: relation.createdById ?? "",
            createdByText: relation.createdByText ?? "",
            createdTime: relation.createdTime ?? "",
            relatedEntityTypeName: relation.relatedEntityTypeName ?? "",
            relatedEntityCode: relation.relatedEntityCode ?? "",
            relatedEntityName: relation.relatedEntityName ?? "",
            relationTypeName: relation.relationTypeName ?? "",
        isDeleted: normalizeBoolean(relation.isDeleted),
        isNew: normalizeBoolean(relation.isNew)
    };
}

function byId(id) {
    return id ? document.getElementById(id) : null;
}

function getConfiguredElements(config) {
    return {
        body: byId(config.bodyId),
        empty: byId(config.emptyId),
        addButton: byId(config.addButtonId),

        dialog: byId(config.dialogId),
        title: byId(config.titleId),
        status: byId(config.statusId),
        okButton: byId(config.okButtonId),
        noRelevantButton: byId(config.noRelevantButtonId),
        cancelButton: byId(config.cancelButtonId),
        closeButton: byId(config.closeButtonId),
        dialogContent: byId("relationDialogContent")
    };
}

function setElementText(element, value) {
    if (element) {
        element.textContent = value;
    }
}

function setDisabled(element, disabled) {
    if (element) {
        element.disabled = disabled === true;
    }
}

function showDialog(dialog) {
    if (!dialog) {
        return;
    }

    if (typeof dialog.showModal === "function" && !dialog.open) {
        dialog.showModal();
        return;
    }

    dialog.setAttribute("open", "open");
}

function closeDialog(dialog) {
    if (!dialog) {
        return;
    }

    if (typeof dialog.close === "function" && dialog.open) {
        dialog.close();
        return;
    }

    dialog.removeAttribute("open");
}

function setEmptyVisible(emptyElement, visible) {
    if (!emptyElement) {
        return;
    }

    emptyElement.style.display = visible ? "block" : "none";
}

function parseCreatedBy(node) {
    const createdByNode = node?.getElementsByTagName("CreatedById")?.[0] || null;
    const createdById = createdByNode?.getElementsByTagName("Value")?.[0]?.textContent?.trim() || "";
    const createdByText = createdByNode?.getElementsByTagName("Option")?.[0]?.textContent?.trim() || "";

    return {
        createdById,
        createdByText
    };
}

function parseRelationsFromContainer(containerNode, relationElementName) {
    return Array.from(containerNode?.getElementsByTagName(relationElementName) || []).map((node) => {
        const createdBy = parseCreatedBy(node);

        return normalizeRelation({
            entityRelationPK: textOf(node, "EntityRelationPK").trim(),
            entityId: textOf(node, "EntityId").trim(),
            entityType: textOf(node, "EntityType").trim(),
            relatedEntityId: textOf(node, "RelatedEntityId").trim(),
            relatedEntityType: textOf(node, "RelatedEntityType").trim(),
            createdById: createdBy.createdById,
            createdByText: createdBy.createdByText,
            createdTime: textOf(node, "CreatedTime").trim(),
            relatedEntityTypeName: textOf(node, "RelatedEntityTypeName"),
            relatedEntityCode: textOf(node, "RelatedEntityCode"),
            relatedEntityName: textOf(node, "RelatedEntityName"),
            relationTypeName: textOf(node, "RelationTypeName"),
            isDeleted: textOf(node, "IsDeleted").trim() === "true",
            isNew: false
        });
    });
}

function findRelationContainer(doc, containerName) {
    const root = doc?.documentElement || doc;

    if (!root) {
        return null;
    }

    return getDirectChild(root, containerName)
        || root.getElementsByTagName(containerName)?.[0]
        || null;
}

function ensureRelationContainer(doc, containerName) {
    const root = doc.documentElement;
    let container = getDirectChild(root, containerName);

    if (!container) {
        container = doc.createElement(containerName);
        root.appendChild(container);
    }

    return container;
}

function clearChildren(node) {
    while (node?.firstChild) {
        node.removeChild(node.firstChild);
    }
}

function appendCreatedByXml(doc, parent, relation) {
    const createdById = doc.createElement("CreatedById");

    appendTextElement(doc, createdById, "Value", relation.createdById ?? "");

    if (relation.createdByText) {
        const option = doc.createElement("Option");
        option.setAttribute("value", relation.createdById ?? "");

        if (relation.createdById) {
            option.setAttribute("selected", "true");
        }

        option.textContent = relation.createdByText ?? "";
        createdById.appendChild(option);
    }

    parent.appendChild(createdById);
}

function appendRelationXml(doc, container, relationElementName, relation) {
    const entityRelation = doc.createElement(relationElementName);

    appendTextElement(doc, entityRelation, "EntityRelationPK", relation.entityRelationPK ?? "");
    appendTextElement(doc, entityRelation, "EntityId", relation.entityId ?? "");
    appendTextElement(doc, entityRelation, "EntityType", relation.entityType ?? "");
    appendTextElement(doc, entityRelation, "RelatedEntityId", relation.relatedEntityId ?? "");
    appendTextElement(doc, entityRelation, "RelatedEntityType", relation.relatedEntityType ?? "");
    appendTextElement(doc, entityRelation, "CreatedById", relation.createdById ?? "");
    appendTextElement(doc, entityRelation, "CreatedTime", relation.createdTime ?? "");
    appendTextElement(doc, entityRelation, "RelationTypeName", relation.relationTypeName ?? "");
    appendTextElement(doc, entityRelation, "IsDeleted", relation.isDeleted ? "true" : "false");

    container.appendChild(entityRelation);
}

function formatCreatedTime(value) {
    return parseDateTime(value);
}

function formatCreatedBy(relation) {
    return relation.createdByText || relation.createdById || "";
}

function createRelationRowMarkup(relation, index, readOnly) {
    const rowClass = relation.isDeleted
        ? "is-deleted"
        : "is-openable";

    const deleteButton = relation.isDeleted || readOnly
        ? ""
        : `<button type="button" class="relation-delete-btn" data-relation-delete="${index}" aria-label="Delete relation" title="Delete relation">
                <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                    <path d="M9 3.5h6a1 1 0 0 1 1 1V6h4a1 1 0 1 1 0 2h-1.1l-.75 10.2A2.5 2.5 0 0 1 15.66 20H8.34a2.5 2.5 0 0 1-2.49-1.8L5.1 8H4a1 1 0 0 1 0-2h4V4.5a1 1 0 0 1 1-1Zm1 1.5v1h4v-1h-4Zm-2.9 3 .64 9.1c.04.55.5 1 1.05 1h7.12c.55 0 1.01-.45 1.05-1L16.6 8H7.1Zm2.4 2a.9.9 0 0 1 .9.9v4.2a.9.9 0 1 1-1.8 0v-4.2a.9.9 0 0 1 .9-.9Zm4 0a.9.9 0 0 1 .9.9v4.2a.9.9 0 1 1-1.8 0v-4.2a.9.9 0 0 1 .9-.9Z"></path>
                </svg>
            </button>`;

    const relatedEntityTypeName = relation.relatedEntityTypeName ?? "";
    const relatedEntityCode = relation.relatedEntityCode ?? "";
    const relatedEntityName = relation.relatedEntityName ?? "";
    const relationTypeName = relation.relationTypeName ?? "";
    const createdBy = formatCreatedBy(relation);
    const createdTime = formatCreatedTime(relation.createdTime);

    return `
        <tr class="${rowClass}" data-relation-index="${index}" title="Double-click to open relation">
            <td title="${escapeHtml(relatedEntityTypeName)}">${escapeHtml(relatedEntityTypeName)}</td>
            <td title="${escapeHtml(relatedEntityCode)}">${escapeHtml(relatedEntityCode)}</td>
            <td title="${escapeHtml(relatedEntityName)}">${escapeHtml(relatedEntityName)}</td>
            <td title="${escapeHtml(relationTypeName)}">${escapeHtml(relationTypeName)}</td>
            <td title="${escapeHtml(createdBy)}">${escapeHtml(createdBy)}</td>
            <td title="${escapeHtml(createdTime)}">${escapeHtml(createdTime)}</td>
            <td class="attachment-actions">
                <span class="attachment-action-group">
                    ${deleteButton}
                </span>
            </td>
        </tr>
    `;
}

function openRelationInBrowser(relation) {
    const link = normalizeText(relation?.link).trim();

    if (!link) {
        return false;
    }

    window.open(link, "_blank", "noopener,noreferrer");

    return true;
}

function createXmlDocument(rootName) {
    const parser = new DOMParser();
    const doc = parser.parseFromString(`<${rootName}></${rootName}>`, "application/xml");

    return doc;
}

function ensureDialogStructure(elements) {
    const dialog = elements.dialog;
    const form = dialog?.querySelector("form");
    const status = elements.status;
    const content = elements.dialogContent;
    const footer = dialog?.querySelector(".dlg-footer");

    if (!dialog || !form || !status || !footer) {
        return;
    }

    if (!content) {
        const dialogContent = document.createElement("div");
        dialogContent.id = "relationDialogContent";
        dialogContent.className = "relation-dialog-content";
        dialogContent.setAttribute("aria-label", "Relation lists");
        form.insertBefore(dialogContent, footer);
    }

    if (!dialog.querySelector("#relationNoRelevantBtn")) {
        const noRelevantButton = document.createElement("button");
        noRelevantButton.id = "relationNoRelevantBtn";
        noRelevantButton.type = "button";
        noRelevantButton.textContent = "Add Not Relevant Relation";
        noRelevantButton.className = "primary";
        footer.insertBefore(noRelevantButton, elements.cancelButton || footer.lastElementChild);
    }
}

function buildRelationRequestXml(state) {
    const requestDoc = createXmlDocument("RelationListRequest");
    const root = requestDoc.documentElement;

    appendTextElement(requestDoc, root, "EntityType", state.entityContext.entityTypeId ?? "");
    appendTextElement(requestDoc, root, "EntityId", state.entityContext.entityId ?? "");

    const relationsNode = requestDoc.createElement("EntityRelations");
    getActiveRelations(state.relations).forEach((relation) => {
        const entityRelation = requestDoc.createElement("EntityRelation");
        appendTextElement(requestDoc, entityRelation, "EntityRelationPK", relation.entityRelationPK ?? "");
        appendTextElement(requestDoc, entityRelation, "EntityId", relation.entityId ?? "");
        appendTextElement(requestDoc, entityRelation, "EntityType", relation.entityType ?? "");
        appendTextElement(requestDoc, entityRelation, "RelatedEntityId", relation.relatedEntityId ?? "");
        appendTextElement(requestDoc, entityRelation, "RelatedEntityType", relation.relatedEntityType ?? "");
        relationsNode.appendChild(entityRelation);
    });

    root.appendChild(relationsNode);

    return new XMLSerializer().serializeToString(requestDoc);
}

function parseRelationListResponse(xmlText) {
    const doc = new DOMParser().parseFromString(xmlText, "application/xml");

    if (doc.getElementsByTagName("parsererror").length > 0) {
        throw new Error("The relation list endpoint returned invalid XML.");
    }

    const listNodes = Array.from(doc.getElementsByTagName("RelationList"));

    return listNodes.map((listNode) => {
        const optionNodes = Array.from(listNode.getElementsByTagName("RelationOption"));

        return {
            label: textOf(listNode, "Label").trim(),
            showConfirmedRelation: textOf(listNode, "ShowConfirmedRelation").trim() === "true",
            showNoRelevantRelation: textOf(listNode, "ShowNoRelevantRelation").trim() === "true",
            options: optionNodes.map((optionNode) => ({
                entityId: textOf(optionNode, "EntityId").trim(),
                entityType: textOf(optionNode, "EntityType").trim(),
                entityRelationPK: textOf(optionNode, "EntityRelationPK").trim(),
                entityTypeName: textOf(optionNode, "EntityTypeName").trim(),
                entityCode: textOf(optionNode, "EntityCode").trim(),
                entityName: textOf(optionNode, "EntityName").trim()
            })).filter((option) => option.entityId && option.entityType)
        };
    });
}

function getActiveRelations(relations) {
    return (Array.isArray(relations) ? relations : [])
        .map(normalizeRelation)
        .filter((relation) => relation.isDeleted !== true);
}

function hasEntityContext(entityContext) {
    return !!normalizeText(entityContext?.entityId).trim()
        && !!normalizeText(entityContext?.entityTypeId).trim();
}

function collectSelectedDialogOption(elements) {
    const content = elements.dialogContent;
    const selects = Array.from(content?.querySelectorAll("select[data-relation-select]") || []);

    for (const select of selects) {
        const value = select.value?.trim();

        if (!value) {
            continue;
        }

        const listIndex = Number(select.getAttribute("data-relation-list-index"));
        const optionIndex = Number(select.getAttribute("data-selected-option-index") || select.selectedIndex - 1);

        return {
            listIndex,
            optionIndex,
            select
        };
    }

    return null;
}

function getSelectedDialogOption(elements) {
    const selection = collectSelectedDialogOption(elements);

    if (!selection) {
        return null;
    }

    const listData = elements.dialogContent?.__relationLists?.[selection.listIndex] || null;
    const option = listData?.options?.[selection.optionIndex] || null;

    if (!option) {
        return null;
    }

    return {
        listIndex: selection.listIndex,
        listLabel: listData?.label || "",
        relationTypeName: "",
        option
    };
}

function clearOtherSelections(content, activeSelect) {
    const selects = Array.from(content?.querySelectorAll("select[data-relation-select]") || []);

    selects.forEach((select) => {
        const shouldDisable = activeSelect && select !== activeSelect;

        select.disabled = shouldDisable;

        if (shouldDisable) {
            select.value = "";
        }
    });
}

function updateDialogButtons(elements) {
    const selection = collectSelectedDialogOption(elements);
    const hasSelection = !!selection;
    const dialogLists = elements.dialogContent?.__relationLists || [];

    const allowedButtons = dialogLists.reduce((accumulator, list) => {
        return {
            showConfirmedRelation: accumulator.showConfirmedRelation || list.showConfirmedRelation === true,
            showNoRelevantRelation: accumulator.showNoRelevantRelation || list.showNoRelevantRelation === true
        };
    }, {
        showConfirmedRelation: false,
        showNoRelevantRelation: false
    });

    setDisabled(elements.okButton, !hasSelection);
    setDisabled(elements.noRelevantButton, !hasSelection);
    if (elements.okButton) {
        elements.okButton.hidden = !allowedButtons.showConfirmedRelation;
    }
    if (elements.noRelevantButton) {
        elements.noRelevantButton.hidden = !allowedButtons.showNoRelevantRelation;
    }
}

function renderRelationDialog(elements, dialogData) {
    const content = elements.dialogContent;

    if (!content) {
        throw new Error("Missing relation dialog content container.");
    }

    content.__relationLists = dialogData.lists;

    if (!dialogData.lists.length) {
        content.innerHTML = '<div class="relation-dialog-empty">No relation lists returned.</div>';
        updateDialogButtons(elements);
        return;
    }

    content.innerHTML = dialogData.lists.map((list, listIndex) => {
        const optionsMarkup = list.options.map((option, optionIndex) => {
            const label = option.entityCode && option.entityName
                ? `${option.entityCode} - ${option.entityName}`
                : (option.entityName || option.entityCode || option.entityId);

            return `<option value="${escapeHtml(option.entityId)}" data-option-index="${optionIndex}">${escapeHtml(label)}</option>`;
        }).join("");

        return `
            <section class="relation-dialog-list" data-relation-list-index="${listIndex}">
                <div class="relation-dialog-list-label">${escapeHtml(list.label || `List ${listIndex + 1}`)}</div>
                <select class="relation-dialog-select" data-relation-select data-relation-list-index="${listIndex}">
                    <option value="">Select entity</option>
                    ${optionsMarkup}
                </select>
            </section>
        `;
    }).join("");

    content.querySelectorAll("select[data-relation-select]").forEach((select) => {
        select.addEventListener("change", (event) => {
            const target = event.currentTarget;
            const hasValue = !!target.value?.trim();

            if (hasValue) {
                clearOtherSelections(content, target);
            } else {
                clearOtherSelections(content, null);
            }

            updateDialogButtons(elements);
        });
    });

    updateDialogButtons(elements);
}

function setDialogStatus(elements, message) {
    setElementText(elements.status, message);
}

function getDialogSelectionPayload(elements) {
    const selection = collectSelectedDialogOption(elements);

    if (!selection) {
        return null;
    }

    const dialogLists = elements.dialogContent?.__relationLists || [];
    const listData = dialogLists[selection.listIndex];
    const option = listData?.options?.[selection.optionIndex] || null;

    if (!option) {
        return null;
    }

    return {
        listIndex: selection.listIndex,
        listLabel: listData?.label || "",
        option
    };
}

function buildLocalRelation(state, dialogSelection, relationTypeName) {
    const entityContext = state.entityContext || {};
    const option = dialogSelection.option;

    return normalizeRelation({
        entityRelationPK: "",
        entityId: entityContext.entityId ?? "",
        entityType: entityContext.entityTypeId ?? "",
        relatedEntityId: option.entityId ?? "",
        relatedEntityType: option.entityType ?? "",
        createdById: "",
        createdByText: "",
        createdTime: nowIsoLocal(),
        relatedEntityTypeName: option.entityTypeName || dialogSelection.listLabel || "",
        relatedEntityCode: option.entityCode || "",
        relatedEntityName: option.entityName || "",
        relationTypeName,
        isDeleted: false,
        isNew: true
    });
}

export function createEntityRelationsTable(config = {}) {
    const state = {
        relations: [],
        readOnly: config.readOnly === true,
        bound: false,
        entityContext: {
            entityId: "",
            entityTypeId: ""
        },
        config: {
            ...DEFAULT_CONFIG,
            ...config
        },
        dialogState: {
            open: false
        }
    };

    state.readOnly = state.config.readOnly === true;

    function getElements() {
        return getConfiguredElements(state.config);
    }

    function notifyChange() {
        if (typeof state.config.onChange === "function") {
            state.config.onChange(getRelations());
        }
    }

    function notifyAfterRender() {
        if (typeof state.config.onAfterRender === "function") {
            state.config.onAfterRender(getRelations());
        }
    }

    function setRelations(relations, options = {}) {
        state.relations = Array.isArray(relations)
            ? relations.map(normalizeRelation)
            : [];

        if (options.render !== false) {
            render();
        }
    }

    function setEntityContext(entityContext = {}, options = {}) {
        state.entityContext = {
            entityId: entityContext.entityId ?? state.entityContext.entityId ?? "",
            entityTypeId: entityContext.entityTypeId ?? state.entityContext.entityTypeId ?? ""
        };

        if (options.render !== false) {
            render();
        }
    }

    function getEntityContext() {
        return { ...state.entityContext };
    }

    function getRelations() {
        return state.relations;
    }

    function getRelation(index) {
        return state.relations[index] || null;
    }

    function loadFromDocument(doc, options = {}) {
        const container = findRelationContainer(doc, state.config.relationContainerName);
        const relations = parseRelationsFromContainer(container, state.config.relationElementName);

        if (!state.entityContext.entityId && relations.length) {
            setEntityContext({
                entityId: relations[0].entityId || "",
                entityTypeId: relations[0].entityType || ""
            }, { render: false });
        }

        setRelations(relations, options);
    }

    function writeToDocument(doc) {
        const container = ensureRelationContainer(doc, state.config.relationContainerName);

        clearChildren(container);

        state.relations.forEach((relation) => {
            appendRelationXml(
                doc,
                container,
                state.config.relationElementName,
                normalizeRelation(relation)
            );
        });

        return doc;
    }

    function setReadOnly(readOnly, options = {}) {
        state.readOnly = readOnly === true;

        const elements = getElements();

        setDisabled(elements.addButton, state.readOnly || !hasEntityContext(state.entityContext));

        if (options.render !== false) {
            render();
        }
    }

    function addRelation(relation, options = {}) {
        if (state.readOnly) {
            return;
        }

        state.relations.unshift({
            ...normalizeRelation(relation),
            createdTime: relation?.createdTime ?? nowIsoLocal(),
            isDeleted: false,
            isNew: true
        });

        if (options.render !== false) {
            render();
        }

        if (options.notify !== false) {
            notifyChange();
        }
    }

    function markDeleted(index, options = {}) {
        if (state.readOnly) {
            return;
        }

        if (index < 0 || index >= state.relations.length) {
            return;
        }

        state.relations[index] = {
            ...state.relations[index],
            isDeleted: true
        };

        if (options.render !== false) {
            render();
        }

        if (options.notify !== false) {
            notifyChange();
        }
    }

    function removeNewRelation(index, options = {}) {
        if (state.readOnly) {
            return;
        }

        if (index < 0 || index >= state.relations.length) {
            return;
        }

        const relation = getRelation(index);

        if (relation?.isNew === true) {
            state.relations.splice(index, 1);
        } else {
            state.relations[index] = {
                ...relation,
                isDeleted: true
            };
        }

        if (options.render !== false) {
            render();
        }

        if (options.notify !== false) {
            notifyChange();
        }
    }

    function clear(options = {}) {
        state.relations = [];

        if (options.render !== false) {
            render();
        }

        if (options.notify === true) {
            notifyChange();
        }
    }

    function render(elements = null) {
        const renderElements = elements || getElements();

        if (!renderElements?.body || !renderElements?.empty) {
            return;
        }

        renderElements.body.setAttribute("data-entity-relations-table-version", ENTITY_RELATIONS_TABLE_VERSION);

        renderElements.body.innerHTML = state.relations
            .map((relation, index) => createRelationRowMarkup(relation, index, state.readOnly))
            .join("");

        setEmptyVisible(renderElements.empty, state.relations.length === 0);
        setDisabled(getElements().addButton, state.readOnly || !hasEntityContext(state.entityContext));

        notifyAfterRender();
    }

    async function requestNewRelation() {
        if (state.readOnly || !hasEntityContext(state.entityContext)) {
            return;
        }

        const elements = getElements();

        if (typeof state.config.onAddRelationRequest === "function") {
            const relation = await state.config.onAddRelationRequest({
                table: api,
                relations: getRelations(),
                entityContext: getEntityContext()
            });

            if (relation) {
                addRelation(relation);
            }

            return;
        }

        if (!state.config.relationRequestUrl) {
            setDialogStatus(elements, "Relation request URL is not configured.");
            showDialog(elements.dialog);
            return;
        }

        setDialogStatus(elements, "Loading relation lists...");
        renderRelationDialog(elements, { lists: [] });
        showDialog(elements.dialog);

        try {
            const response = await fetch(state.config.relationRequestUrl, {
                method: "POST",
                headers: {
                    "Content-Type": "application/xml; charset=UTF-8",
                    "Accept": "application/xml,text/xml,*/*"
                },
                body: buildRelationRequestXml(state),
                cache: "no-store"
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status} ${response.statusText}`);
            }

            const xmlText = await response.text();
            const dialogData = parseRelationListResponse(xmlText);

            renderRelationDialog(elements, { lists: dialogData });
            setDialogStatus(elements, dialogData.length ? "Select a relation." : "No relation lists returned.");
        } catch (error) {
            console.error("Failed to load relation lists", error);
            renderRelationDialog(elements, { lists: [] });
            setDialogStatus(elements, `Could not load relation lists. ${error.message}`);
        }
    }

    function closeRelationDialog() {
        const elements = getElements();

        closeDialog(elements.dialog);
    }

    function resetRelationDialog() {
        const elements = getElements();
        const content = elements.dialogContent;

        if (content) {
            content.innerHTML = "";
            delete content.__relationLists;
        }

        setDialogStatus(elements, "");
        setDisabled(elements.okButton, true);
        setDisabled(elements.noRelevantButton, true);
        if (elements.okButton) {
            elements.okButton.hidden = true;
        }
        if (elements.noRelevantButton) {
            elements.noRelevantButton.hidden = true;
        }
    }

    function handleAddConfirmedRelation() {
        const elements = getElements();
        const selection = getDialogSelectionPayload(elements);

        if (!selection) {
            return;
        }

        addRelation(buildLocalRelation(state, selection, "Confirmed"));
        closeRelationDialog();
    }

    function handleAddNoRelevantRelation() {
        const elements = getElements();
        const selection = getDialogSelectionPayload(elements);

        if (!selection) {
            return;
        }

        addRelation(buildLocalRelation(state, selection, "Not Relevant"));
        closeRelationDialog();
    }

    function handleDelete(index) {
        if (state.readOnly) {
            return;
        }

        const relation = getRelation(index);

        if (!relation || relation.isDeleted) {
            return;
        }

        if (state.config.confirmDelete && !window.confirm("Delete this relation?")) {
            return;
        }

        removeNewRelation(index);
    }

    function handleOpen(index) {
        const relation = getRelation(index);

        if (!relation || relation.isDeleted) {
            return;
        }

        const ok = openRelationInBrowser(relation);

        if (!ok) {
            window.alert("The relation link could not be opened.");
        }
    }

    function handleTableClick(event) {
        const deleteButton = event.target.closest("[data-relation-delete]");

        if (!deleteButton) {
            return;
        }

        event.preventDefault();
        event.stopPropagation();

        handleDelete(Number(deleteButton.getAttribute("data-relation-delete")));
    }

    function handleTableDoubleClick(event) {
        if (!state.config.allowOpenOnRowDoubleClick) {
            return;
        }

        const row = event.target.closest("[data-relation-index]");

        if (!row) {
            return;
        }

        handleOpen(Number(row.getAttribute("data-relation-index")));
    }

    function handleDialogCancel(event) {
        event.preventDefault();
        resetRelationDialog();
        closeRelationDialog();
    }

    function bind(customConfig = {}) {
        state.config = {
            ...state.config,
            ...customConfig
        };

        state.readOnly = state.config.readOnly === true || state.readOnly === true;

        ensureDialogStructure(getElements());

        if (state.bound) {
            setReadOnly(state.readOnly);
            return;
        }

        const elements = getElements();

        setElementText(elements.okButton, "Add Confirmed Relation");
        setElementText(elements.noRelevantButton, "Add Not Relevant Relation");
        setElementText(elements.cancelButton, "Cancel");

        elements.addButton?.addEventListener("click", async () => {
            await requestNewRelation();
        });

        elements.okButton?.addEventListener("click", handleAddConfirmedRelation);
        elements.noRelevantButton?.addEventListener("click", handleAddNoRelevantRelation);
        elements.cancelButton?.addEventListener("click", handleDialogCancel);
        elements.closeButton?.addEventListener("click", handleDialogCancel);

        elements.dialog?.addEventListener("cancel", handleDialogCancel);

        elements.body?.addEventListener("click", handleTableClick);
        elements.body?.addEventListener("dblclick", handleTableDoubleClick);

        state.bound = true;

        setReadOnly(state.readOnly);
    }

    function destroy() {
        state.bound = false;
    }

    const api = {
        bind,
        destroy,

        loadFromDocument,
        writeToDocument,

        setRelations,
        getRelations,
        getRelation,
        clear,

        setReadOnly,
        setEntityContext,
        getEntityContext,

        addRelation,
        markDeleted,
        removeNewRelation,

        render,

        requestNewRelation,
        openRelationInBrowser
    };

    return api;
}

export function createEntityRelationsTables(configs = []) {
    const tables = configs.map((config) => createEntityRelationsTable(config));

    function bind() {
        tables.forEach((table) => table.bind());
    }

    function loadFromDocument(doc) {
        tables.forEach((table) => table.loadFromDocument(doc));
    }

    function writeToDocument(doc) {
        tables.forEach((table) => table.writeToDocument(doc));

        return doc;
    }

    function setReadOnly(readOnly) {
        tables.forEach((table) => table.setReadOnly(readOnly));
    }

    function render() {
        tables.forEach((table) => table.render());
    }

    function getTables() {
        return tables;
    }

    return {
        bind,
        loadFromDocument,
        writeToDocument,
        setReadOnly,
        render,
        getTables
    };
}
