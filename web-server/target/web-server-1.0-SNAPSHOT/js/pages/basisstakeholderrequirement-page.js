import { createGenericPage } from "../generel.js";
import { initMenu } from "../components/menu.js";
import { mountTopbar } from "../components/topbar.js";
import { initTabs } from "../components/tabs.js";
import { createHistoryTable } from "../components/history-table.js";
import { createNotesTable } from "../components/notes-table.js";
import { createAttachmentsTable } from "../components/attachments-table.js";
import { createEntityRelationsTable } from "../components/entity-relations-table.js";
import { createExportDialog } from "../components/export-dialog.js";
import { createImportDialog } from "../components/import-dialog.js";
import { initHelpDialog } from "../components/help-dialog.js";
import {
    closeDialogElement,
    setInputValue,
    setText,
    showDialog
} from "../core/dom.js";
import {
    getDirectChild,
    getDirectText,
    serializeXml
} from "../core/xml.js";
import {
    buildEntityAttachmentsXml,
    buildEntityNotesXml,
    buildEntityRelationsXml,
    parseEntityAttachmentsFromDoc,
    parseEntityNotesFromDoc,
    parseEntityRelationsFromDoc
} from "../core/entity-xml.js";
import {
    fieldEditable,
    fieldRequired,
    fieldVisible
} from "../core/field-display.js";
import { nowIsoLocal } from "../core/date.js";
import { escapeHtml } from "../core/html.js";
import { isTruthy } from "../core/utils.js";

const historyTable = createHistoryTable({
    entrySelector: "entityHistory",
    sortKeys: ["changedDateTime", "changedByUserId", "version"],
    indicatorPrefix: "si-h-"
});

const notesTable = createNotesTable();
const attachmentsTable = createAttachmentsTable();
const relationsTable = createEntityRelationsTable();

function findDetailNode(root) {
    return root.querySelector("stakeholderRequirementDocument > stakeholderRequirement")
        || root.querySelector("stakeholderRequirementDocument stakeholderRequirement")
        || root.querySelector("stakeholderRequirement");
}

function ensureChild(doc, parent, tagName) {
    let child = getDirectChild(parent, tagName);

    if (!child) {
        child = doc.createElement(tagName);
        parent.appendChild(child);
    }

    return child;
}

function getNotesElements() {
    return {
        body: document.getElementById("notesBody"),
        empty: document.getElementById("notesEmpty")
    };
}

function getAttachmentsElements() {
    return {
        body: document.getElementById("attachmentsBody"),
        empty: document.getElementById("attachmentsEmpty")
    };
}

function getRelationsElements() {
    return {
        body: document.getElementById("relationsBody"),
        empty: document.getElementById("relationsEmpty")
    };
}

function renderNotes() {
    notesTable.render(getNotesElements());
}

function renderAttachments() {
    attachmentsTable.render(getAttachmentsElements());
}

function renderRelations() {
    relationsTable.render(getRelationsElements());
}

function renderHistoryFromDoc(doc, elements) {
    const root = doc?.documentElement || doc;
    const historyNode = root.getElementsByTagName("entityHistories")?.[0] || null;

    historyTable.render(historyNode, {
        body: elements.historyBody,
        empty: elements.historyEmpty
    });
}

function parseNotesFromDoc(doc) {
    notesTable.setNotes(parseEntityNotesFromDoc(doc));
    renderNotes();
}

function parseAttachmentsFromDoc(doc) {
    attachmentsTable.setAttachments(parseEntityAttachmentsFromDoc(doc));
    renderAttachments();
}

function parseRelationsFromDoc(doc) {
    relationsTable.setRelations(parseEntityRelationsFromDoc(doc));
    renderRelations();
}

function getFieldUiValue(field) {
    const control = (field.getAttribute("control") || "").toLowerCase();

    if (control === "select") {
        return (field.getElementsByTagName("Value")?.[0]?.textContent || "").trim();
    }

    const valueNode = getDirectChild(field, "Value");

    if (valueNode) {
        return valueNode.textContent || "";
    }

    return getDirectText(field).trim();
}

function renderBasisInfoFieldMarkup(field) {
    const name = field.tagName;
    const label = field.getAttribute("header") || field.getAttribute("label") || name;
    const control = (field.getAttribute("control") || "").toLowerCase();
    const editable = fieldEditable(field);
    const visible = fieldVisible(field);
    const required = fieldRequired(field);
    const value = getFieldUiValue(field);

    if (!visible) {
        return "";
    }

    const readonlyAttr = editable ? "" : "readonly";
    const disabledAttr = editable ? "" : "disabled";
    const requiredStar = required ? '<span class="field-required" aria-hidden="true">*</span>' : "";
    const requiredAttr = required ? "required" : "";
    const escapedName = escapeHtml(name);
    const escapedLabel = escapeHtml(label);
    const escapedValue = escapeHtml(value);

    if (control === "hidden") {
        return `<input type="hidden" data-field="${escapedName}" id="fld-${escapedName}" value="${escapedValue}">`;
    }

    if (control === "checkbox") {
        const checked = isTruthy(value) ? "checked" : "";

        return `
            <div class="page-field checkbox-field">
                <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
                <input id="fld-${escapedName}" data-field="${escapedName}" type="checkbox" ${checked} ${disabledAttr} ${requiredAttr} />
            </div>
        `;
    }

    if (control === "datetime") {
        const normalized = value ? value.replace(" ", "T") : "";

        return `
            <div class="page-field">
                <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
                <input id="fld-${escapedName}" data-field="${escapedName}" type="datetime-local" value="${escapeHtml(normalized)}" ${readonlyAttr} ${requiredAttr} />
            </div>
        `;
    }

    if (control === "date") {
        const normalized = value ? value.substring(0, 10) : "";

        return `
            <div class="page-field">
                <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
                <input id="fld-${escapedName}" data-field="${escapedName}" type="date" value="${escapeHtml(normalized)}" ${readonlyAttr} ${requiredAttr} />
            </div>
        `;
    }

    if (control === "textarea"
        || name === "RequirementDescription"
        || name === "StakeholderRequirementDescription"
        || name === "ReqDescription"
        || name === "Description") {
        return `
            <div class="page-field description-field">
                <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
                <textarea id="fld-${escapedName}" data-field="${escapedName}" ${readonlyAttr} ${requiredAttr}>${escapedValue}</textarea>
            </div>
        `;
    }

    if (control === "select") {
        const options = Array.from(field.getElementsByTagName("Option"));
        const selectedValue = (field.getElementsByTagName("Value")?.[0]?.textContent || "").trim();

        return `
            <div class="page-field">
                <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
                <select id="fld-${escapedName}" data-field="${escapedName}" ${disabledAttr} ${requiredAttr}>
                    ${options.map((option) => {
            const optionValue = option.getAttribute("value") || "";
            const optionLabel = (option.textContent || "").trim();
            const selected = optionValue === selectedValue ? "selected" : "";

            return `<option value="${escapeHtml(optionValue)}" ${selected}>${escapeHtml(optionLabel)}</option>`;
        }).join("")}
                </select>
            </div>
        `;
    }

    return `
        <div class="page-field">
            <label for="fld-${escapedName}">${escapedLabel}${requiredStar}</label>
            <input id="fld-${escapedName}" data-field="${escapedName}" type="text" value="${escapedValue}" ${readonlyAttr} ${requiredAttr} />
        </div>
    `;
}

function renderBasisInfoFromDoc(doc) {
    const root = doc?.documentElement || doc;
    const detailContainer = root.getElementsByTagName("stakeholderRequirementDocument")?.[0] || null;
    const detailNode = detailContainer
        ? detailContainer.getElementsByTagName("stakeholderRequirement")?.[0] || null
        : root.getElementsByTagName("stakeholderRequirement")?.[0] || null;

    const basisInfoFields = document.getElementById("basisInfoFields");

    if (!basisInfoFields) {
        return;
    }

    if (!detailNode) {
        basisInfoFields.innerHTML = '<div class="page-empty">No detail XML returned.</div>';
        return;
    }

    basisInfoFields.innerHTML = Array.from(detailNode.children || [])
        .map(renderBasisInfoFieldMarkup)
        .join("");
}

function wireNotesEvents() {
    const noteDialog = document.getElementById("noteDialog");
    const noteTextArea = document.getElementById("noteTextArea");
    const noteDlgTitle = document.getElementById("noteDlgTitle");
    const noteDlgStatus = document.getElementById("noteDlgStatus");
    const noteSaveBtn = document.getElementById("noteSaveBtn");
    const noteCancelBtn = document.getElementById("noteCancelBtn");
    const notesAddNewBtn = document.getElementById("notesAddNew");
    const notesBody = document.getElementById("notesBody");

    const state = {
        mode: "add",
        editingIndex: -1,
        initialText: ""
    };

    function openDialog(mode, index, text) {
        state.mode = mode;
        state.editingIndex = index;
        state.initialText = text ?? "";

        setText(noteDlgTitle, mode === "add" ? "Add Note" : "Edit Note", "");
        setText(noteDlgStatus, mode === "add" ? "Create a new note." : "Edit the selected note.", "");
        setInputValue(noteTextArea, state.initialText);

        showDialog(noteDialog);
        setTimeout(() => noteTextArea?.focus?.(), 0);
    }

    function closeDialog() {
        closeDialogElement(noteDialog);
    }

    function saveCurrentNote() {
        const text = noteTextArea?.value ?? "";
        const existing = state.mode === "edit" ? notesTable.getNote(state.editingIndex) : null;

        const note = {
            entityNotePK: existing?.entityNotePK ?? "",
            noteText: text,
            createdById: existing?.createdById ?? "",
            createdByText: existing?.createdByText ?? "",
            createdTime: existing?.createdTime || nowIsoLocal(),
            isNew: state.mode === "add"
        };

        if (state.mode === "add") {
            notesTable.addNote(note);
        } else {
            notesTable.updateNote(state.editingIndex, note);
        }

        renderNotes();
        closeDialog();
    }

    notesAddNewBtn?.addEventListener("click", () => openDialog("add", -1, ""));
    noteSaveBtn?.addEventListener("click", saveCurrentNote);
    noteCancelBtn?.addEventListener("click", closeDialog);

    noteDialog?.addEventListener("cancel", (event) => {
        event.preventDefault();
        closeDialog();
    });

    notesBody?.addEventListener("dblclick", (event) => {
        const row = event.target.closest("tr[data-note-index]");

        if (!row) {
            return;
        }

        const index = Number(row.getAttribute("data-note-index"));
        const note = notesTable.getNote(index);

        if (!note) {
            return;
        }

        openDialog("edit", index, note.noteText || "");
    });
}

function wireAttachmentsEvents() {
    const attachmentDialog = document.getElementById("attachmentDialog");
    const attachmentDlgTitle = document.getElementById("attachmentDlgTitle");
    const attachmentDlgStatus = document.getElementById("attachmentDlgStatus");
    const attachmentFileInput = document.getElementById("attachmentFileInput");
    const attachmentDescriptionInput = document.getElementById("attachmentDescriptionInput");
    const attachmentFileInfo = document.getElementById("attachmentFileInfo");
    const attachmentSaveBtn = document.getElementById("attachmentSaveBtn");
    const attachmentCancelBtn = document.getElementById("attachmentCancelBtn");
    const attachmentsAddNewBtn = document.getElementById("attachmentsAddNew");
    const attachmentsBody = document.getElementById("attachmentsBody");

    const state = {
        pendingFile: null
    };

    function setInfoText() {
        setText(
            attachmentFileInfo,
            state.pendingFile
                ? `${state.pendingFile.name} (${state.pendingFile.size} bytes)`
                : "No file selected.",
            ""
        );
    }

    function openDialog() {
        state.pendingFile = null;

        setText(attachmentDlgTitle, "Add Attachment", "");
        setText(attachmentDlgStatus, "Select a file and add a description.", "");
        setInputValue(attachmentDescriptionInput, "");
        setInputValue(attachmentFileInput, "");

        setInfoText();
        showDialog(attachmentDialog);
    }

    function closeDialog() {
        closeDialogElement(attachmentDialog);
    }

    async function saveCurrentAttachment() {
        const description = (attachmentDescriptionInput?.value ?? "").slice(0, 255);

        if (!state.pendingFile) {
            window.alert("Please select a file first.");
            return;
        }

        if (!description.trim()) {
            window.alert("Description is required.");
            return;
        }

        const attachment = await attachmentsTable.buildAttachmentFromFile(state.pendingFile, description);
        attachment.createdTime = nowIsoLocal();
        attachment.entityAttachmentPK = "";
        attachment.createdById = "";
        attachment.isNew = true;

        attachmentsTable.addAttachment(attachment);
        renderAttachments();
        closeDialog();
    }

    attachmentsAddNewBtn?.addEventListener("click", openDialog);
    attachmentSaveBtn?.addEventListener("click", async () => {
        await saveCurrentAttachment();
    });
    attachmentCancelBtn?.addEventListener("click", closeDialog);

    attachmentDialog?.addEventListener("cancel", (event) => {
        event.preventDefault();
        closeDialog();
    });

    attachmentFileInput?.addEventListener("change", () => {
        state.pendingFile = attachmentFileInput.files?.[0] || null;
        setInfoText();
    });

    attachmentsBody?.addEventListener("dblclick", (event) => {
        const row = event.target.closest("tr[data-attachment-index]");

        if (!row) {
            return;
        }

        const index = Number(row.getAttribute("data-attachment-index"));
        const attachment = attachmentsTable.getAttachment(index);

        if (!attachment || attachment.isDeleted) {
            return;
        }

        if (attachmentsTable.canOpenInBrowser(attachment.contentType, attachment.fileName)) {
            attachmentsTable.openAttachmentInBrowser(attachment);
        }
    });
}

function wireRelationsEvents() {
    const relationDialog = document.getElementById("relationDialog");
    const relationDlgTitle = document.getElementById("relationDlgTitle");
    const relationDlgStatus = document.getElementById("relationDlgStatus");
    const relationOkBtn = document.getElementById("relationOkBtn");
    const relationCancelBtn = document.getElementById("relationCancelBtn");
    const relationsAddNewBtn = document.getElementById("relationsAddNew");
    const relationsBody = document.getElementById("relationsBody");

    function openDialog() {
        setText(relationDlgTitle, "Add relation", "");
        setText(relationDlgStatus, "Dummy dialog for relation creation.", "");
        showDialog(relationDialog);
    }

    function closeDialog() {
        closeDialogElement(relationDialog);
    }

    relationsAddNewBtn?.addEventListener("click", openDialog);
    relationOkBtn?.addEventListener("click", closeDialog);
    relationCancelBtn?.addEventListener("click", closeDialog);

    relationDialog?.addEventListener("cancel", (event) => {
        event.preventDefault();
        closeDialog();
    });

    relationsBody?.addEventListener("dblclick", (event) => {
        const row = event.target.closest("tr[data-relation-index]");

        if (!row) {
            return;
        }

        const index = Number(row.getAttribute("data-relation-index"));
        const relation = relationsTable.getRelation(index);

        if (!relation || relation.isDeleted) {
            return;
        }

        const ok = relationsTable.openRelationInBrowser(relation);

        if (!ok) {
            window.alert("The relation link could not be opened.");
        }
    });
}

function buildBasisStakeholderRequirementSavePayload(elements, context) {
    const currentDoc = context?.currentDoc;

    if (!currentDoc || !currentDoc.documentElement) {
        throw new Error("No XML document loaded.");
    }

    const updatedDoc = currentDoc.cloneNode(true);
    const root = updatedDoc.documentElement;

    const detailContainer = getDirectChild(root, "stakeholderRequirementDocument");
    const detailNode = detailContainer
        ? getDirectChild(detailContainer, "stakeholderRequirement")
        : getDirectChild(root, "stakeholderRequirement");

    if (!detailNode) {
        throw new Error("No detail XML returned.");
    }

    const fields = Array.from(elements.basisInfoFields.querySelectorAll("[data-field]"));

    fields.forEach((uiField) => {
        const name = uiField.getAttribute("data-field");

        if (!name) {
            return;
        }

        const child = ensureChild(updatedDoc, detailNode, name);
        const control = (child.getAttribute("control") || "").toLowerCase();

        if (control === "select") {
            const value = uiField.selectedOptions?.[0]?.value?.trim() || "";
            let valueNode = child.getElementsByTagName("Value")?.[0] || null;

            if (!valueNode) {
                valueNode = updatedDoc.createElement("Value");
                child.insertBefore(valueNode, child.firstChild);
            }

            valueNode.textContent = value;

            Array.from(child.getElementsByTagName("Option")).forEach((option) => {
                if (option.getAttribute("selected") != null) {
                    option.removeAttribute("selected");
                }

                if ((option.getAttribute("value") || "").trim() === value) {
                    option.setAttribute("selected", "true");
                }
            });

            return;
        }

        if (uiField.type === "checkbox") {
            child.textContent = uiField.checked ? "true" : "false";
        } else {
            child.textContent = uiField.value ?? "";
        }
    });

    buildEntityNotesXml(updatedDoc, notesTable.getNotes());
    buildEntityAttachmentsXml(updatedDoc, attachmentsTable.getAttachments());
    buildEntityRelationsXml(updatedDoc, relationsTable.getRelations());

    return serializeXml(updatedDoc);
}

function start() {
    mountTopbar();

    const exportDialog = createExportDialog({
        dialogId: "exportDialog",
        openButtonId: "btnExport",
        exportUrl: "/basis/stakeholderrequirement",
        baseFileName: "stakeholderrequirements"
    });

    let page = null;

    const importDialog = createImportDialog({
        dialogId: "importDialog",
        openButtonId: "btnImport",
        importUrl: "/basis/stakeholderrequirement?cmd=import",
        onImportComplete: async () => {
            await page?.start?.();
        }
    });

    page = createGenericPage({
        listUrl: "/basis/stakeholderrequirement?cmd=list",
        detailUrl: "/basis/stakeholderrequirement?cmd=edit&id=",
        saveUrl: "/basis/stakeholderrequirement?cmd=save",
        rootListTag: "BasisStakeholderRequirementList",
        listEntityTag: "stakeholderRequirements",
        detailEntityTag: "stakeholderRequirement",
        detailContainerTag: "stakeholderRequirementDocument",
        findDetailNode,
        afterListLoad: (_doc, _elements, context) => {
            const rowCount = context?.rowCount || 0;
            exportDialog.setVisible(rowCount > 0);
            importDialog.setVisible(rowCount > 0);
        },
        afterDetailLoad: (doc, elements) => {
            renderBasisInfoFromDoc(doc);
            renderHistoryFromDoc(doc, elements);
            parseNotesFromDoc(doc);
            parseAttachmentsFromDoc(doc);
            parseRelationsFromDoc(doc);
        },
        buildSavePayload: buildBasisStakeholderRequirementSavePayload
    });

    initTabs([
        { btnId: "tabBtn1", panelId: "tabPanel1" },
        { btnId: "tabBtn2", panelId: "tabPanel2" },
        { btnId: "tabBtn3", panelId: "tabPanel3" },
        { btnId: "tabBtn4", panelId: "tabPanel4" },
        { btnId: "tabBtn5", panelId: "tabPanel5" },
        { btnId: "tabBtn6", panelId: "tabPanel6" }
    ]);

    initMenu(document);
    initHelpDialog();

    wireNotesEvents();
    wireAttachmentsEvents();
    wireRelationsEvents();
    exportDialog.bind();
    importDialog.bind();

    page.start();
}

start();
