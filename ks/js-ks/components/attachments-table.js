import { nowIsoLocal, parseDateTime } from "../core/date.js";
import { buildEntityAttachmentsXml, parseEntityAttachmentsFromDoc } from "../core/entity-xml.js";
import { formatFileSize } from "../core/format.js";
import { escapeHtml } from "../core/html.js";

const DEFAULT_MAX_DESCRIPTION_LENGTH = 255;

const DEFAULT_CONFIG = {
    bodyId: "attachmentsBody",
    emptyId: "attachmentsEmpty",
    dialogId: "attachmentDialog",
    titleId: "attachmentDlgTitle",
    statusId: "attachmentDlgStatus",
    fileInputId: "attachmentFileInput",
    descriptionInputId: "attachmentDescriptionInput",
    fileInfoId: "attachmentFileInfo",
    addButtonId: "attachmentsAddNew",
    saveButtonId: "attachmentSaveBtn",
    cancelButtonId: "attachmentCancelBtn",
    closeButtonId: "",
    maxDescriptionLength: DEFAULT_MAX_DESCRIPTION_LENGTH,
    confirmDelete: true,
    allowOpenOnRowDoubleClick: true,
    readOnly: false,
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

function normalizeAttachment(attachment = {}) {
    return {
        entityAttachmentPK: attachment.entityAttachmentPK ?? "",
        fileName: attachment.fileName ?? "",
        contentType: attachment.contentType ?? "",
        fileSize: attachment.fileSize ?? "",
        description: attachment.description ?? "",
        isDeleted: normalizeBoolean(attachment.isDeleted),
        fileData: attachment.fileData ?? "",
        createdById: attachment.createdById ?? "",
        createdByText: attachment.createdByText ?? "",
        createdTime: attachment.createdTime ?? "",
        isNew: normalizeBoolean(attachment.isNew)
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
        fileInput: byId(config.fileInputId),
        descriptionInput: byId(config.descriptionInputId),
        fileInfo: byId(config.fileInfoId),
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

function formatCreatedTime(value) {
    return parseDateTime(value);
}

function formatCreatedBy(attachment) {
    const text = normalizeText(attachment?.createdByText).trim();

    return text || "—";
}

function getFileExtension(fileName) {
    const name = normalizeText(fileName).toLowerCase();
    const dot = name.lastIndexOf(".");

    return dot >= 0 ? name.slice(dot + 1) : "";
}

function canOpenInBrowser(contentType, fileName) {
    const ct = normalizeText(contentType).toLowerCase();
    const ext = getFileExtension(fileName);

    if (ct.startsWith("image/")) {
        return true;
    }

    if (ct === "application/pdf") {
        return true;
    }

    if (ct.startsWith("text/")) {
        return true;
    }

    return [
        "pdf",
        "png",
        "jpg",
        "jpeg",
        "gif",
        "webp",
        "bmp",
        "svg",
        "txt",
        "html",
        "htm",
        "csv",
        "xml",
        "json",
        "md"
    ].includes(ext);
}

function base64ToBytes(base64) {
    const clean = normalizeText(base64).trim();

    if (!clean) {
        return new Uint8Array();
    }

    const binary = atob(clean);
    const bytes = new Uint8Array(binary.length);

    for (let index = 0; index < binary.length; index += 1) {
        bytes[index] = binary.charCodeAt(index);
    }

    return bytes;
}

function openAttachmentInBrowser(attachment) {
    const fileData = normalizeText(attachment?.fileData).trim();
    const contentType = normalizeText(attachment?.contentType).trim() || "application/octet-stream";

    if (!fileData) {
        return false;
    }

    try {
        const bytes = base64ToBytes(fileData);
        const blob = new Blob([bytes], { type: contentType });
        const url = URL.createObjectURL(blob);
        const win = window.open(url, "_blank");

        if (!win) {
            URL.revokeObjectURL(url);
            return false;
        }

        setTimeout(() => URL.revokeObjectURL(url), 10_000);

        return true;
    } catch (error) {
        console.error("Could not open attachment.", error);
        return false;
    }
}

function downloadAttachment(attachment) {
    const fileData = normalizeText(attachment?.fileData).trim();
    const fileName = normalizeText(attachment?.fileName).trim();

    if (!fileData || !fileName) {
        return false;
    }

    try {
        const bytes = base64ToBytes(fileData);
        const blob = new Blob([bytes], {
            type: normalizeText(attachment?.contentType).trim() || "application/octet-stream"
        });
        const url = URL.createObjectURL(blob);

        const anchor = document.createElement("a");
        anchor.href = url;
        anchor.download = fileName;
        anchor.style.display = "none";

        document.body.appendChild(anchor);
        anchor.click();
        anchor.remove();

        setTimeout(() => URL.revokeObjectURL(url), 10_000);

        return true;
    } catch (error) {
        console.error("Could not download attachment.", error);
        return false;
    }
}

function readFileAsBase64(file) {
    return new Promise((resolve, reject) => {
        if (!file) {
            reject(new Error("No file selected."));
            return;
        }

        const reader = new FileReader();

        reader.onload = () => {
            const result = reader.result;

            if (typeof result !== "string") {
                reject(new Error("Could not read file."));
                return;
            }

            const base64Part = result.includes(",")
                ? result.split(",")[1]
                : "";

            resolve(base64Part);
        };

        reader.onerror = () => reject(new Error("Could not read file."));
        reader.readAsDataURL(file);
    });
}

function createAttachmentRowMarkup(attachment, index, readOnly) {
    const openable = canOpenInBrowser(attachment.contentType, attachment.fileName);
    const rowClass = attachment.isDeleted
        ? "is-deleted"
        : openable
            ? "is-openable"
            : "is-muted";

    const deletedLabel = attachment.isDeleted
        ? `<span class="attachment-deleted-label">Deleted</span>`
        : "";

    const openButton = attachment.isDeleted || !openable
        ? ""
        : `<button type="button" class="attachment-open-btn" data-attachment-open="${index}" aria-label="Open attachment" title="Open attachment">↗</button>`;

    const downloadButton = attachment.isDeleted
        ? ""
        : `<button type="button" class="attachment-download-btn" data-attachment-download="${index}" aria-label="Download attachment" title="Download attachment">⬇</button>`;

    const deleteButton = attachment.isDeleted || readOnly
        ? ""
        : `<button type="button" class="attachment-delete-btn" data-attachment-delete="${index}" aria-label="Delete attachment" title="Delete attachment">🗑</button>`;

    return `
        <tr class="${rowClass}" data-attachment-index="${index}">
            <td title="${escapeHtml(attachment.fileName)}">${escapeHtml(attachment.fileName)}</td>
            <td title="${escapeHtml(attachment.description)}">${escapeHtml(attachment.description)}</td>
            <td>${escapeHtml(formatFileSize(attachment.fileSize, "da-DK", ""))}</td>
            <td title="${escapeHtml(formatCreatedBy(attachment))}">${escapeHtml(formatCreatedBy(attachment))}</td>
            <td>${escapeHtml(formatCreatedTime(attachment.createdTime))}</td>
            <td class="attachment-actions">
                <span class="attachment-action-group">
                    ${deletedLabel}
                    ${openButton}
                    ${downloadButton}
                    ${deleteButton}
                </span>
            </td>
        </tr>
    `;
}

export function createAttachmentsTable(config = {}) {
    const state = {
        attachments: [],
        pendingFile: null,
        bound: false,
        readOnly: config.readOnly === true,
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
            state.config.onChange(getAttachments());
        }
    }

    function notifyAfterRender() {
        if (typeof state.config.onAfterRender === "function") {
            state.config.onAfterRender(getAttachments());
        }
    }

    function setAttachments(attachments, options = {}) {
        state.attachments = Array.isArray(attachments)
            ? attachments.map(normalizeAttachment)
            : [];

        if (options.render !== false) {
            render();
        }
    }

    function loadFromDocument(doc, options = {}) {
        setAttachments(parseEntityAttachmentsFromDoc(doc), options);
    }

    function writeToDocument(doc) {
        buildEntityAttachmentsXml(doc, getAttachments());
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

    function getAttachments() {
        return state.attachments;
    }

    function getAttachment(index) {
        return state.attachments[index] || null;
    }

    function addAttachment(attachment, options = {}) {
        if (state.readOnly) {
            return;
        }

        state.attachments.unshift({
            ...normalizeAttachment(attachment),
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

        if (index < 0 || index >= state.attachments.length) {
            return;
        }

        state.attachments[index] = {
            ...state.attachments[index],
            isDeleted: true
        };

        if (options.render !== false) {
            render();
        }

        if (options.notify !== false) {
            notifyChange();
        }
    }

    function restoreDeleted(index, options = {}) {
        if (state.readOnly) {
            return;
        }

        if (index < 0 || index >= state.attachments.length) {
            return;
        }

        state.attachments[index] = {
            ...state.attachments[index],
            isDeleted: false
        };

        if (options.render !== false) {
            render();
        }

        if (options.notify !== false) {
            notifyChange();
        }
    }

    function removeNewAttachment(index, options = {}) {
        if (state.readOnly) {
            return;
        }

        if (index < 0 || index >= state.attachments.length) {
            return;
        }

        const attachment = state.attachments[index];

        if (attachment?.isNew === true && !attachment?.entityAttachmentPK) {
            state.attachments.splice(index, 1);
        } else {
            state.attachments[index] = {
                ...attachment,
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
        state.attachments = [];
        state.pendingFile = null;

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

        elements.body.innerHTML = state.attachments
            .map((attachment, index) => createAttachmentRowMarkup(attachment, index, state.readOnly))
            .join("");

        setEmptyVisible(elements.empty, state.attachments.length === 0);
        setDisabled(elements.addButton, state.readOnly);

        notifyAfterRender();
    }

    async function buildAttachmentFromFile(file, description) {
        const fileName = normalizeText(file?.name).trim();
        const fileData = await readFileAsBase64(file);

        return {
            entityAttachmentPK: "",
            fileName,
            contentType: normalizeText(file?.type).trim(),
            fileSize: file?.size ?? 0,
            description: normalizeText(description).trim().slice(0, state.config.maxDescriptionLength),
            isDeleted: false,
            fileData,
            createdById: "",
            createdByText: "",
            createdTime: nowIsoLocal(),
            isNew: true
        };
    }

    function resetDialog() {
        const elements = getElements();

        state.pendingFile = null;

        setInputValue(elements.fileInput, "");
        setInputValue(elements.descriptionInput, "");
        setElementText(elements.fileInfo, "No file selected.");
        setElementText(elements.status, "Select a file and add a description.");

        if (elements.descriptionInput) {
            elements.descriptionInput.maxLength = state.config.maxDescriptionLength;
            elements.descriptionInput.required = true;
        }
    }

    function openAddDialog() {
        if (state.readOnly) {
            return;
        }

        const elements = getElements();

        resetDialog();

        setElementText(elements.title, "Add Attachment");
        showDialog(elements.dialog);

        setTimeout(() => {
            elements.fileInput?.focus?.();
        }, 0);
    }

    function closeAddDialog() {
        const elements = getElements();

        closeDialog(elements.dialog);
        resetDialog();
    }

    function setSelectedFile(file) {
        const elements = getElements();

        state.pendingFile = file || null;

        setElementText(
            elements.fileInfo,
            state.pendingFile
                ? `${state.pendingFile.name} (${formatFileSize(state.pendingFile.size, "da-DK", "0 B")})`
                : "No file selected."
        );
    }

    function validatePendingAttachment() {
        const elements = getElements();

        if (!state.pendingFile) {
            return "Please select a file first.";
        }

        const description = normalizeText(elements.descriptionInput?.value).trim();

        if (!description) {
            return "Description is required.";
        }

        if (description.length > state.config.maxDescriptionLength) {
            return `Description must be maximum ${state.config.maxDescriptionLength} characters.`;
        }

        return "";
    }

    async function savePendingAttachment() {
        if (state.readOnly) {
            return;
        }

        const elements = getElements();
        const validationMessage = validatePendingAttachment();

        if (validationMessage) {
            setElementText(elements.status, validationMessage);
            window.alert(validationMessage);
            return;
        }

        try {
            setElementText(elements.status, "Reading file…");

            const description = normalizeText(elements.descriptionInput?.value).trim();
            const attachment = await buildAttachmentFromFile(state.pendingFile, description);

            addAttachment(attachment);

            setElementText(elements.status, "Attachment added.");
            closeAddDialog();
        } catch (error) {
            console.error("Could not add attachment.", error);
            setElementText(elements.status, "Could not add attachment.");
            window.alert(error?.message || "Could not add attachment.");
        }
    }

    function handleOpen(index) {
        const attachment = getAttachment(index);

        if (!attachment || attachment.isDeleted) {
            return;
        }

        if (!canOpenInBrowser(attachment.contentType, attachment.fileName)) {
            handleDownload(index);
            return;
        }

        const opened = openAttachmentInBrowser(attachment);

        if (!opened) {
            window.alert("Could not open attachment.");
        }
    }

    function handleDownload(index) {
        const attachment = getAttachment(index);

        if (!attachment || attachment.isDeleted) {
            return;
        }

        const downloaded = downloadAttachment(attachment);

        if (!downloaded) {
            window.alert("Could not download attachment.");
        }
    }

    function handleDelete(index) {
        if (state.readOnly) {
            return;
        }

        const attachment = getAttachment(index);

        if (!attachment || attachment.isDeleted) {
            return;
        }

        if (state.config.confirmDelete && !window.confirm("Delete this attachment?")) {
            return;
        }

        removeNewAttachment(index);
    }

    function handleTableClick(event) {
        const openButton = event.target.closest("[data-attachment-open]");
        const downloadButton = event.target.closest("[data-attachment-download]");
        const deleteButton = event.target.closest("[data-attachment-delete]");

        if (openButton) {
            event.preventDefault();
            event.stopPropagation();
            handleOpen(Number(openButton.getAttribute("data-attachment-open")));
            return;
        }

        if (downloadButton) {
            event.preventDefault();
            event.stopPropagation();
            handleDownload(Number(downloadButton.getAttribute("data-attachment-download")));
            return;
        }

        if (deleteButton) {
            event.preventDefault();
            event.stopPropagation();
            handleDelete(Number(deleteButton.getAttribute("data-attachment-delete")));
        }
    }

    function handleTableDoubleClick(event) {
        if (!state.config.allowOpenOnRowDoubleClick) {
            return;
        }

        const row = event.target.closest("[data-attachment-index]");

        if (!row) {
            return;
        }

        handleOpen(Number(row.getAttribute("data-attachment-index")));
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

        elements.fileInput?.addEventListener("change", () => {
            setSelectedFile(elements.fileInput?.files?.[0] || null);
        });

        elements.saveButton?.addEventListener("click", async () => {
            await savePendingAttachment();
        });

        elements.cancelButton?.addEventListener("click", closeAddDialog);
        elements.closeButton?.addEventListener("click", closeAddDialog);

        elements.dialog?.addEventListener("cancel", (event) => {
            event.preventDefault();
            closeAddDialog();
        });

        elements.body?.addEventListener("click", handleTableClick);
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

        setAttachments,
        getAttachments,
        getAttachment,
        clear,

        setReadOnly,

        addAttachment,
        markDeleted,
        restoreDeleted,
        removeNewAttachment,

        render,

        openAddDialog,
        closeAddDialog,
        resetDialog,

        canOpenInBrowser,
        openAttachmentInBrowser,
        downloadAttachment,
        buildAttachmentFromFile
    };
}
