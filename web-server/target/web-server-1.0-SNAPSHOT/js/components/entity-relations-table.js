import { nowIsoLocal, parseDateTime } from "../core/date.js";
import {
    appendTextElement,
    getDirectChild,
    textOf
} from "../core/xml.js";
import { escapeHtml } from "../core/html.js";

const DEFAULT_CONFIG = {
    bodyId: "relationsBody",
    emptyId: "relationsEmpty",
    addButtonId: "relationsAddNew",

    dialogId: "relationDialog",
    titleId: "relationDlgTitle",
    statusId: "relationDlgStatus",
    okButtonId: "relationOkBtn",
    cancelButtonId: "relationCancelBtn",
    closeButtonId: "",

    relationContainerName: "EntityRelations",
    relationElementName: "EntityRelation",

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
        link: relation.link ?? "",
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
        cancelButton: byId(config.cancelButtonId),
        closeButton: byId(config.closeButtonId)
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
            link: textOf(node, "Link"),
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

    appendTextElement(doc, entityRelation, "EntityId", relation.entityId ?? "");
    appendTextElement(doc, entityRelation, "EntityType", relation.entityType ?? "");
    appendTextElement(doc, entityRelation, "RelatedEntityId", relation.relatedEntityId ?? "");
    appendTextElement(doc, entityRelation, "RelatedEntityType", relation.relatedEntityType ?? "");
    appendTextElement(doc, entityRelation, "CreatedTime", relation.createdTime ?? "");
    appendTextElement(doc, entityRelation, "RelatedEntityTypeName", relation.relatedEntityTypeName ?? "");
    appendTextElement(doc, entityRelation, "RelatedEntityCode", relation.relatedEntityCode ?? "");
    appendTextElement(doc, entityRelation, "RelatedEntityName", relation.relatedEntityName ?? "");
    appendTextElement(doc, entityRelation, "Link", relation.link ?? "");
    appendTextElement(doc, entityRelation, "IsDeleted", relation.isDeleted ? "true" : "false");

    appendCreatedByXml(doc, entityRelation, relation);

    container.appendChild(entityRelation);
}

function formatCreatedTime(value) {
    return parseDateTime(value);
}

function createRelationRowMarkup(relation, index, readOnly) {
    const rowClass = relation.isDeleted
        ? "is-deleted"
        : "is-openable";

    const deleteButton = relation.isDeleted || readOnly
        ? ""
        : `<button type="button" class="relation-delete-btn" data-relation-delete="${index}" aria-label="Delete relation" title="Delete relation">🗑</button>`;

    return `
        <tr class="${rowClass}" data-relation-index="${index}" title="Double-click to open relation">
            <td title="${escapeHtml(relation.relatedEntityTypeName)}">${escapeHtml(relation.relatedEntityTypeName)}</td>
            <td title="${escapeHtml(relation.relatedEntityCode)}">${escapeHtml(relation.relatedEntityCode)}</td>
            <td title="${escapeHtml(relation.relatedEntityName)}">${escapeHtml(relation.relatedEntityName)}</td>
            <td>${escapeHtml(formatCreatedTime(relation.createdTime))}</td>
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

export function createEntityRelationsTable(config = {}) {
    const state = {
        relations: [],
        readOnly: config.readOnly === true,
        bound: false,
        config: {
            ...DEFAULT_CONFIG,
            ...config
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

    function getRelations() {
        return state.relations;
    }

    function getRelation(index) {
        return state.relations[index] || null;
    }

    function loadFromDocument(doc, options = {}) {
        const container = findRelationContainer(doc, state.config.relationContainerName);
        const relations = parseRelationsFromContainer(container, state.config.relationElementName);

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

        setDisabled(elements.addButton, state.readOnly);

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

        renderElements.body.innerHTML = state.relations
            .map((relation, index) => createRelationRowMarkup(relation, index, state.readOnly))
            .join("");

        setEmptyVisible(renderElements.empty, state.relations.length === 0);
        setDisabled(getElements().addButton, state.readOnly);

        notifyAfterRender();
    }

    async function requestNewRelation() {
        if (state.readOnly) {
            return;
        }

        const elements = getElements();

        if (typeof state.config.onAddRelationRequest === "function") {
            const relation = await state.config.onAddRelationRequest({
                table: api,
                relations: getRelations()
            });

            if (relation) {
                addRelation(relation);
            }

            return;
        }

        setElementText(elements.title, "Add relation");
        setElementText(elements.status, "Relation picker is not implemented yet.");
        showDialog(elements.dialog);
    }

    function closeRelationDialog() {
        const elements = getElements();

        closeDialog(elements.dialog);
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

    function bind(customConfig = {}) {
        state.config = {
            ...state.config,
            ...customConfig
        };

        state.readOnly = state.config.readOnly === true || state.readOnly === true;

        if (state.bound) {
            setReadOnly(state.readOnly);
            return;
        }

        const elements = getElements();

        elements.addButton?.addEventListener("click", async () => {
            await requestNewRelation();
        });

        elements.okButton?.addEventListener("click", closeRelationDialog);
        elements.cancelButton?.addEventListener("click", closeRelationDialog);
        elements.closeButton?.addEventListener("click", closeRelationDialog);

        elements.dialog?.addEventListener("cancel", (event) => {
            event.preventDefault();
            closeRelationDialog();
        });

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