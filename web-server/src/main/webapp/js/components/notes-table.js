import { nowIsoLocal, parseDateTime } from "../core/date.js";
import { buildEntityNotesXml, parseEntityNotesFromDoc } from "../core/entity-xml.js";
import { escapeHtml } from "../core/html.js";

const DEFAULT_MAX_NOTE_LENGTH = 4000;

const DEFAULT_CONFIG = {
    bodyId: "notesBody",
    emptyId: "notesEmpty",
    dialogId: "noteDialog",
    titleId: "noteDlgTitle",
    statusId: "noteDlgStatus",
    textAreaId: "noteTextArea",
    addButtonId: "notesAddNew",
    saveButtonId: "noteSaveBtn",
    cancelButtonId: "noteCancelBtn",
    closeButtonId: "",
    maxNoteLength: DEFAULT_MAX_NOTE_LENGTH,
    readOnly: false,
    allowEditOnRowDoubleClick: true,
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

function normalizeNote(note = {}) {
    return {
        entityNotePK: note.entityNotePK ?? "",
        noteText: note.noteText ?? "",
        createdById: note.createdById ?? "",
        createdByText: note.createdByText ?? "",
        createdTime: note.createdTime ?? "",
        isNew: normalizeBoolean(note.isNew)
    };
}

function byId(id) {
    return id ? document.getElementById(id) : null;
}

function getConfiguredElements(config) {
    return {
        body: byId(config.bodyId),
        empty: byId(config.emptyId),
        dialog: byId(config.dialogId),
        title: byId(config.titleId),
        status: byId(config.statusId),
        textArea: byId(config.textAreaId),
        addButton: byId(config.addButtonId),
        saveButton: byId(config.saveButtonId),
        cancelButton: byId(config.cancelButtonId),
        closeButton: byId(config.closeButtonId)
    };
}

function setElementText(element, value) {
    if (element) {
        element.textContent = value;
    }
}

function setInputValue(element, value) {
    if (element) {
        element.value = value;
    }
}

function setDisabled(element, disabled) {
    if (element) {
        element.disabled = disabled === true;
    }
}

function setHidden(element, hidden) {
    if (element) {
        element.hidden = hidden === true;
    }
}

function setTextAreaReadOnly(textArea, readOnly) {
    if (!textArea) {
        return;
    }

    textArea.readOnly = readOnly === true;
    textArea.classList.toggle("is-readonly", readOnly === true);
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

function formatCreatedTime(value) {
    return parseDateTime(value);
}

function truncateNoteText(value) {
    const text = normalizeText(value);

    if (!text) {
        return "";
    }

    const newline = text.indexOf("\n");

    if (newline >= 0) {
        return text.slice(0, newline);
    }

    return text.length > 75 ? `${text.slice(0, 75)}…` : text;
}

function setEmptyVisible(emptyElement, visible) {
    if (!emptyElement) {
        return;
    }

    emptyElement.style.display = visible ? "block" : "none";
}

function hasCreatedBy(note) {
    return normalizeText(note?.createdById).trim() !== ""
        || normalizeText(note?.createdByText).trim() !== "";
}

function isPersistedNote(note) {
    return hasCreatedBy(note);
}

function canEditNote(note, globalReadOnly) {
    if (globalReadOnly) {
        return false;
    }

    return !isPersistedNote(note);
}

function createNoteRowMarkup(note, index, globalReadOnly) {
    const editable = canEditNote(note, globalReadOnly);
    const rowClass = editable ? "is-editable" : "is-readonly";
    const title = editable
        ? "Double-click to edit note"
        : "Double-click to read note";

    return `
        <tr class="${rowClass}" data-note-index="${index}" title="${escapeHtml(title)}">
            <td title="${escapeHtml(note.noteText)}">${escapeHtml(truncateNoteText(note.noteText))}</td>
            <td>${escapeHtml(note.createdByText || note.createdById || "")}</td>
            <td>${escapeHtml(formatCreatedTime(note.createdTime))}</td>
        </tr>
    `;
}

export function createNotesTable(config = {}) {
    const state = {
        notes: [],
        readOnly: config.readOnly === true,
        bound: false,
        mode: "add",
        editingIndex: -1,
        initialText: "",
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
            state.config.onChange(getNotes());
        }
    }

    function notifyAfterRender() {
        if (typeof state.config.onAfterRender === "function") {
            state.config.onAfterRender(getNotes());
        }
    }

    function setNotes(notes, options = {}) {
        state.notes = Array.isArray(notes)
            ? notes.map(normalizeNote)
            : [];

        if (options.render !== false) {
            render();
        }
    }

    function loadFromDocument(doc, options = {}) {
        setNotes(parseEntityNotesFromDoc(doc), options);
    }

    function writeToDocument(doc) {
        buildEntityNotesXml(doc, getNotes());
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

    function getNotes() {
        return state.notes;
    }

    function getNote(index) {
        return state.notes[index] || null;
    }

    function addNote(note, options = {}) {
        if (state.readOnly) {
            return;
        }

        state.notes.unshift({
            ...normalizeNote(note),
            isNew: true
        });

        if (options.render !== false) {
            render();
        }

        if (options.notify !== false) {
            notifyChange();
        }
    }

    function updateNote(index, note, options = {}) {
        if (state.readOnly) {
            return;
        }

        if (index < 0 || index >= state.notes.length) {
            return;
        }

        const existing = getNote(index);

        if (!canEditNote(existing, state.readOnly)) {
            return;
        }

        state.notes[index] = {
            ...existing,
            ...normalizeNote({
                ...existing,
                ...note
            })
        };

        if (options.render !== false) {
            render();
        }

        if (options.notify !== false) {
            notifyChange();
        }
    }

    function clear(options = {}) {
        state.notes = [];
        state.mode = "add";
        state.editingIndex = -1;
        state.initialText = "";

        if (options.render !== false) {
            render();
        }

        if (options.notify === true) {
            notifyChange();
        }
    }

    function render() {
        const elements = getElements();

        if (!elements.body || !elements.empty) {
            return;
        }

        elements.body.innerHTML = state.notes
            .map((note, index) => createNoteRowMarkup(note, index, state.readOnly))
            .join("");

        setEmptyVisible(elements.empty, state.notes.length === 0);
        setDisabled(elements.addButton, state.readOnly);

        notifyAfterRender();
    }

    function resetDialog() {
        const elements = getElements();

        state.mode = "add";
        state.editingIndex = -1;
        state.initialText = "";

        setInputValue(elements.textArea, "");
        setElementText(elements.status, "Create a new note.");
        setTextAreaReadOnly(elements.textArea, false);
        setHidden(elements.saveButton, false);
        setDisabled(elements.saveButton, false);

        if (elements.textArea) {
            elements.textArea.maxLength = state.config.maxNoteLength;
            elements.textArea.required = true;
        }
    }

    function openDialog(mode = "add", index = -1, text = "") {
        const elements = getElements();
        const note = index >= 0 ? getNote(index) : null;
        const readonlyDialog = mode === "view" || (note && !canEditNote(note, state.readOnly));

        if (state.readOnly && mode === "add") {
            return;
        }

        state.mode = readonlyDialog ? "view" : mode;
        state.editingIndex = index;
        state.initialText = text ?? "";

        if (state.mode === "add") {
            setElementText(elements.title, "Add Note");
            setElementText(elements.status, "Create a new note.");
        } else if (state.mode === "edit") {
            setElementText(elements.title, "Edit Note");
            setElementText(elements.status, "Edit the selected note.");
        } else {
            setElementText(elements.title, "View Note");
            setElementText(elements.status, "This note was already created and is read-only.");
        }

        setInputValue(elements.textArea, state.initialText);
        setTextAreaReadOnly(elements.textArea, readonlyDialog);
        setHidden(elements.saveButton, readonlyDialog);
        setDisabled(elements.saveButton, readonlyDialog);

        if (elements.textArea) {
            elements.textArea.maxLength = state.config.maxNoteLength;
            elements.textArea.required = !readonlyDialog;
        }

        showDialog(elements.dialog);

        setTimeout(() => {
            elements.textArea?.focus?.();
        }, 0);
    }

    function openAddDialog() {
        openDialog("add", -1, "");
    }

    function openEditDialog(index) {
        const note = getNote(index);

        if (!note) {
            return;
        }

        if (canEditNote(note, state.readOnly)) {
            openDialog("edit", index, note.noteText || "");
            return;
        }

        openViewDialog(index);
    }

    function openViewDialog(index) {
        const note = getNote(index);

        if (!note) {
            return;
        }

        openDialog("view", index, note.noteText || "");
    }

    function closeNoteDialog() {
        const elements = getElements();

        closeDialog(elements.dialog);
        resetDialog();
    }

    function validatePendingNote() {
        const elements = getElements();
        const text = normalizeText(elements.textArea?.value);

        if (state.mode === "view") {
            return "";
        }

        if (!text.trim()) {
            return "Note text is required.";
        }

        if (text.length > state.config.maxNoteLength) {
            return `Note text must be maximum ${state.config.maxNoteLength} characters.`;
        }

        return "";
    }

    function buildPendingNote() {
        const elements = getElements();
        const text = normalizeText(elements.textArea?.value);

        if (state.mode === "edit") {
            const existing = getNote(state.editingIndex);

            return {
                entityNotePK: existing?.entityNotePK ?? "",
                noteText: text,
                createdById: existing?.createdById ?? "",
                createdByText: existing?.createdByText ?? "",
                createdTime: existing?.createdTime || nowIsoLocal(),
                isNew: existing?.isNew ?? false
            };
        }

        return {
            entityNotePK: "",
            noteText: text,
            createdById: "",
            createdByText: "",
            createdTime: nowIsoLocal(),
            isNew: true
        };
    }

    function savePendingNote() {
        if (state.readOnly || state.mode === "view") {
            return;
        }

        const elements = getElements();
        const validationMessage = validatePendingNote();

        if (validationMessage) {
            setElementText(elements.status, validationMessage);
            window.alert(validationMessage);
            return;
        }

        const note = buildPendingNote();

        if (state.mode === "edit") {
            updateNote(state.editingIndex, note);
        } else {
            addNote(note);
        }

        closeNoteDialog();
    }

    function handleTableDoubleClick(event) {
        if (!state.config.allowEditOnRowDoubleClick) {
            return;
        }

        const row = event.target.closest("[data-note-index]");

        if (!row) {
            return;
        }

        const index = Number(row.getAttribute("data-note-index"));
        const note = getNote(index);

        if (!note) {
            return;
        }

        if (canEditNote(note, state.readOnly)) {
            openEditDialog(index);
            return;
        }

        openViewDialog(index);
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

        elements.addButton?.addEventListener("click", openAddDialog);
        elements.saveButton?.addEventListener("click", savePendingNote);
        elements.cancelButton?.addEventListener("click", closeNoteDialog);
        elements.closeButton?.addEventListener("click", closeNoteDialog);

        elements.dialog?.addEventListener("cancel", (event) => {
            event.preventDefault();
            closeNoteDialog();
        });

        elements.body?.addEventListener("dblclick", handleTableDoubleClick);

        state.bound = true;

        setReadOnly(state.readOnly);
    }

    function destroy() {
        state.bound = false;
    }

    return {
        bind,
        destroy,

        loadFromDocument,
        writeToDocument,

        setNotes,
        getNotes,
        getNote,
        clear,

        setReadOnly,

        addNote,
        updateNote,

        render,

        openAddDialog,
        openEditDialog,
        openViewDialog,
        closeNoteDialog,
        resetDialog
    };
}