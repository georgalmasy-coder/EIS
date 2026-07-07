import { nowIsoLocal, parseDateTime } from "../core/date.js";
import {
    buildEntityLinksXml,
    parseEntityLinksFromDoc
} from "../core/entity-xml.js";
import { escapeHtml } from "../core/html.js";

const STORAGE_KEY = "eis.links.tableColumnWidths";
const DEFAULT_MAX_DESCRIPTION_LENGTH = 4000;
const DEFAULT_MAX_URL_LENGTH = 2048;

const DEFAULT_COLUMN_WIDTHS = [
    "29%",
    "29%",
    "18%",
    "14%",
    "5%",
    "5%"
];

const DEFAULT_CONFIG = {
    mountId: "linksMount",
    bodyId: "linksBody",
    emptyId: "linksEmpty",
    dialogId: "linkDialog",
    titleId: "linkDlgTitle",
    statusId: "linkDlgStatus",
    descriptionInputId: "linkDescriptionInput",
    urlInputId: "linkUrlInput",
    addButtonId: "linksAddNew",
    saveButtonId: "linkSaveBtn",
    cancelButtonId: "linkCancelBtn",
    closeButtonId: "linkDialogCloseButton",
    maxDescriptionLength: DEFAULT_MAX_DESCRIPTION_LENGTH,
    maxUrlLength: DEFAULT_MAX_URL_LENGTH,
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

function normalizeLink(link = {}) {
    return {
        entityLinkPK: link.entityLinkPK ?? "",
        description: link.description ?? "",
        linkUrl: link.linkUrl ?? "",
        createdById: link.createdById ?? "",
        createdByText: link.createdByText ?? "",
        createdTime: link.createdTime ?? "",
        isNew: normalizeBoolean(link.isNew)
    };
}

function byId(id) {
    return id ? document.getElementById(id) : null;
}

function getConfiguredElements(config) {
    return {
        mount: byId(config.mountId),
        body: byId(config.bodyId),
        empty: byId(config.emptyId),
        dialog: byId(config.dialogId),
        title: byId(config.titleId),
        status: byId(config.statusId),
        descriptionInput: byId(config.descriptionInputId),
        urlInput: byId(config.urlInputId),
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

function formatCreatedBy(link) {
    const text = normalizeText(link?.createdByText).trim();

    return text || normalizeText(link?.createdById).trim() || "â€”";
}

function isValidUrl(value) {
    const text = normalizeText(value).trim();

    if (!text) {
        return false;
    }

    try {
        const parsed = new URL(text);
        return parsed.protocol === "http:" || parsed.protocol === "https:";
    } catch {
        return false;
    }
}

function openLinkInBrowser(link) {
    const url = normalizeText(link?.linkUrl).trim();

    if (!isValidUrl(url)) {
        return false;
    }

    try {
        const anchor = document.createElement("a");
        anchor.href = url;
        anchor.target = "_blank";
        anchor.rel = "noopener noreferrer";
        anchor.style.display = "none";

        document.body.appendChild(anchor);
        anchor.click();
        anchor.remove();

        return true;
    } catch (error) {
        console.error("Could not open link.", error);
        return false;
    }
}

function createPencilIcon() {
    return `
        <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <path d="M12 20h9"></path>
            <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z"></path>
        </svg>
    `;
}

function createTrashIcon() {
    return `
        <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <path d="M3 6h18"></path>
            <path d="M8 6V4h8v2"></path>
            <path d="M6 6l1 14h10l1-14"></path>
            <path d="M10 11v6"></path>
            <path d="M14 11v6"></path>
        </svg>
    `;
}

async function probeUrlReachability(url) {
    const target = normalizeText(url).trim();

    if (!target) {
        return {
            verified: false,
            reachable: false,
            message: "Url is required."
        };
    }

    const controller = typeof AbortController !== "undefined" ? new AbortController() : null;
    const timeoutId = controller
        ? window.setTimeout(() => controller.abort(), 8000)
        : null;

    try {
        const headResponse = await fetch(target, {
            method: "HEAD",
            cache: "no-store",
            redirect: "follow",
            signal: controller?.signal
        });

        if (headResponse.ok) {
            return {
                verified: true,
                reachable: true,
                message: "Url is reachable."
            };
        }

        if (headResponse.status === 405 || headResponse.status === 501) {
            const getResponse = await fetch(target, {
                method: "GET",
                cache: "no-store",
                redirect: "follow",
                signal: controller?.signal
            });

            if (getResponse.ok) {
                return {
                    verified: true,
                    reachable: true,
                    message: "Url is reachable."
                };
            }

            return {
                verified: true,
                reachable: false,
                message: `Url responded with HTTP ${getResponse.status}.`
            };
        }

        return {
            verified: true,
            reachable: false,
            message: `Url responded with HTTP ${headResponse.status}.`
        };
    } catch (error) {
        try {
            const getResponse = await fetch(target, {
                method: "GET",
                cache: "no-store",
                redirect: "follow",
                signal: controller?.signal
            });

            if (getResponse.ok) {
                return {
                    verified: true,
                    reachable: true,
                    message: "Url is reachable."
                };
            }

            return {
                verified: true,
                reachable: false,
                message: `Url responded with HTTP ${getResponse.status}.`
            };
        } catch (fallbackError) {
            const reason = fallbackError?.name === "AbortError" || error?.name === "AbortError"
                ? "timed out"
                : "could not be verified from this browser";

            return {
                verified: false,
                reachable: false,
                message: `Url ${reason}.`
            };
        }
    } finally {
        if (timeoutId != null) {
            window.clearTimeout(timeoutId);
        }
    }
}

function getCurrentUserText() {
    return normalizeText(document.getElementById("userName")?.textContent).trim();
}

function createLinkRowMarkup(link, index, readOnly) {
    const rowClass = link.isNew ? "is-new" : "is-persisted";
    const editDisabled = readOnly ? "disabled" : "";
    const deleteDisabled = readOnly ? "disabled" : "";

    return `
        <tr class="${rowClass}" data-link-index="${index}">
            <td title="${escapeHtml(link.description)}"><span class="link-cell-text">${escapeHtml(link.description || "â€”")}</span></td>
            <td title="${escapeHtml(link.linkUrl)}"><span class="link-cell-text">${escapeHtml(link.linkUrl || "â€”")}</span></td>
            <td title="${escapeHtml(formatCreatedBy(link))}">${escapeHtml(formatCreatedBy(link))}</td>
            <td>${escapeHtml(formatCreatedTime(link.createdTime))}</td>
            <td class="link-actions">
                <span class="link-action-group">
                    <button type="button" class="link-action-btn link-edit-btn" data-link-edit="${index}" aria-label="Edit link" title="Edit link" ${editDisabled}>
                        ${createPencilIcon()}
                    </button>
                </span>
            </td>
            <td class="link-actions">
                <span class="link-action-group">
                    <button type="button" class="link-action-btn link-delete-btn" data-link-delete="${index}" aria-label="Delete link" title="Delete link" ${deleteDisabled}>
                        ${createTrashIcon()}
                    </button>
                </span>
            </td>
        </tr>
    `;
}

export function createLinksTable(config = {}) {
    const state = {
        links: [],
        editingIndex: null,
        readOnly: config.readOnly === true,
        bound: false,
        prepared: false,
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
            state.config.onChange(getLinks());
        }
    }

    function notifyAfterRender() {
        if (typeof state.config.onAfterRender === "function") {
            state.config.onAfterRender(getLinks());
        }
    }

    function setLinks(links, options = {}) {
        state.links = Array.isArray(links)
            ? links.map(normalizeLink)
            : [];

        if (options.render !== false) {
            render();
        }
    }

    function loadFromDocument(doc, options = {}) {
        setLinks(parseEntityLinksFromDoc(doc), options);
    }

    function writeToDocument(doc) {
        buildEntityLinksXml(doc, getLinks());
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

    function getLinks() {
        return state.links;
    }

    function getLink(index) {
        return state.links[index] || null;
    }

    function addLink(link, options = {}) {
        if (state.readOnly) {
            return;
        }

        state.links.unshift({
            ...normalizeLink(link),
            isNew: true
        });

        if (options.render !== false) {
            render();
        }

        if (options.notify !== false) {
            notifyChange();
        }
    }

    function updateLink(index, link, options = {}) {
        if (state.readOnly) {
            return;
        }

        if (index < 0 || index >= state.links.length) {
            return;
        }

        const existing = state.links[index];

        state.links[index] = {
            ...existing,
            ...normalizeLink(link),
            isNew: existing?.isNew === true || normalizeBoolean(link?.isNew)
        };

        if (options.render !== false) {
            render();
        }

        if (options.notify !== false) {
            notifyChange();
        }
    }

    function removeLink(index, options = {}) {
        if (state.readOnly) {
            return;
        }

        if (index < 0 || index >= state.links.length) {
            return;
        }

        state.links.splice(index, 1);

        if (options.render !== false) {
            render();
        }

        if (options.notify !== false) {
            notifyChange();
        }
    }

    function clear(options = {}) {
        state.links = [];
        state.editingIndex = null;

        if (options.render !== false) {
            render();
        }

        if (options.notify === true) {
            notifyChange();
        }
    }

    function ensureMountMarkup() {
        const elements = getElements();
        const mount = elements.mount;

        if (!mount) {
            return false;
        }

        if (!mount.querySelector(".links-toolbar")) {
            mount.innerHTML = `
                <div class="links-toolbar">
                    <button id="${state.config.addButtonId}" class="primary links-add-btn" type="button" aria-label="Add link" title="Add link">
                        Add Link
                    </button>
                </div>
                <div class="table-frame links-table-frame">
                    <div class="table-scroll">
                        <table class="data-table links-table" aria-label="Links">
                            <colgroup>
                                ${DEFAULT_COLUMN_WIDTHS.map((width) => `<col style="width: ${width};">`).join("")}
                            </colgroup>
                            <thead>
                                <tr>
                                    <th data-key="description">Description <span class="links-column-resizer" aria-hidden="true"></span></th>
                                    <th data-key="linkUrl">Url <span class="links-column-resizer" aria-hidden="true"></span></th>
                                    <th data-key="createdBy">Created By <span class="links-column-resizer" aria-hidden="true"></span></th>
                                    <th data-key="createdTime">Created <span class="links-column-resizer" aria-hidden="true"></span></th>
                                    <th data-key="editLink"><span class="links-column-resizer" aria-hidden="true"></span></th>
                                    <th data-key="deleteLink"><span class="links-column-resizer" aria-hidden="true"></span></th>
                                </tr>
                            </thead>
                            <tbody id="${state.config.bodyId}"></tbody>
                        </table>
                        <div class="empty links-table-empty" id="${state.config.emptyId}">No link rows.</div>
                    </div>
                </div>
            `;
        }

        return true;
    }

    function ensureDialogMarkup() {
        if (byId(state.config.dialogId)) {
            return;
        }

        const dialog = document.createElement("dialog");
        dialog.id = state.config.dialogId;
        dialog.setAttribute("aria-label", "Link dialog");
        dialog.innerHTML = `
            <form id="linkForm" method="dialog" class="dlg link-dlg">
                <div class="page-dialog-head">
                    <h3 id="${state.config.titleId}">Link</h3>
                </div>

                <div class="page-dialog-status" id="${state.config.statusId}">Idle</div>

                <div class="link-editor">
                    <div class="link-editor-row">
                        <label class="link-editor-label" for="${state.config.descriptionInputId}">
                            Description <span aria-hidden="true">*</span>
                        </label>
                        <textarea id="${state.config.descriptionInputId}" class="link-textarea" rows="10" cols="60" required></textarea>
                    </div>

                    <div class="link-editor-row">
                        <label class="link-editor-label" for="${state.config.urlInputId}">
                            Url <span aria-hidden="true">*</span>
                        </label>
                        <input id="${state.config.urlInputId}" class="link-input" type="text" required />
                    </div>
                </div>

                <div class="dlg-footer">
                    <button id="${state.config.saveButtonId}" class="primary" type="button">Save</button>
                    <button id="${state.config.cancelButtonId}" type="button">Cancel</button>
                </div>
            </form>
        `;

        document.body.appendChild(dialog);
    }

    function ensureTableColGroup(table) {
        let colGroup = table.querySelector(":scope > colgroup");

        if (!colGroup) {
            colGroup = document.createElement("colgroup");
            table.insertBefore(colGroup, table.firstChild);
        }

        while (colGroup.children.length < 6) {
            colGroup.appendChild(document.createElement("col"));
        }

        while (colGroup.children.length > 6) {
            colGroup.removeChild(colGroup.lastElementChild);
        }
    }

    function widthToPixels(width, fallback) {
        const raw = String(width || "").trim();

        if (!raw) {
            return fallback;
        }

        if (raw.endsWith("px")) {
            const parsed = Number(raw.replace("px", ""));
            return Number.isFinite(parsed) ? parsed : fallback;
        }

        if (raw.endsWith("%")) {
            const parsed = Number(raw.replace("%", ""));
            return Number.isFinite(parsed) ? Math.max(50, parsed * 10) : fallback;
        }

        if (/^\d+$/.test(raw)) {
            const parsed = Number(raw);
            return Number.isFinite(parsed) ? parsed : fallback;
        }

        return fallback;
    }

    function getStoredWidths() {
        try {
            const raw = localStorage.getItem(STORAGE_KEY);
            return raw ? JSON.parse(raw) || {} : {};
        } catch {
            return {};
        }
    }

    function persistWidths(widths) {
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(widths));
        } catch {
            // ignore storage failures
        }
    }

    function applyStoredColumnWidths(table) {
        const storedWidths = getStoredWidths();
        const colGroup = table.querySelector(":scope > colgroup");

        if (!colGroup) {
            return;
        }

        Array.from(colGroup.children).forEach((col, index) => {
            const storedWidth = storedWidths[String(index)];

            if (storedWidth) {
                col.style.width = storedWidth;
            }
        });

        updateTableMinWidth(table);
    }

    function updateTableMinWidth(table) {
        const colGroup = table.querySelector(":scope > colgroup");
        const widths = Array.from(colGroup?.children || []).map((col) => widthToPixels(col.style.width, 0));
        const totalWidth = widths.reduce((sum, width) => sum + width, 0);

        table.style.minWidth = `${Math.max(totalWidth || 0, 920)}px`;
    }

    function updateColumnWidth(table, columnIndex, widthPx) {
        const width = `${Math.max(50, Math.round(widthPx))}px`;
        const colGroup = table.querySelector(":scope > colgroup");
        const col = colGroup?.children?.[columnIndex];

        if (col) {
            col.style.width = width;
        }

        updateTableMinWidth(table);
    }

    function startColumnResize(event, table, columnIndex, headerCell) {
        const startX = event.clientX;
        const startWidth = headerCell.getBoundingClientRect().width;

        document.body.classList.add("links-column-resizing");

        function onMouseMove(moveEvent) {
            const delta = moveEvent.clientX - startX;
            const nextWidth = Math.max(50, startWidth + delta);

            updateColumnWidth(table, columnIndex, nextWidth);
        }

        function onMouseUp(upEvent) {
            const delta = upEvent.clientX - startX;
            const nextWidth = Math.max(50, startWidth + delta);
            const colGroup = table.querySelector(":scope > colgroup");
            const currentWidths = getStoredWidths();

            updateColumnWidth(table, columnIndex, nextWidth);
            currentWidths[String(columnIndex)] = `${Math.max(50, Math.round(nextWidth))}px`;
            persistWidths(currentWidths);

            document.body.classList.remove("links-column-resizing");
            window.removeEventListener("mousemove", onMouseMove);
            window.removeEventListener("mouseup", onMouseUp);

            if (colGroup) {
                updateTableMinWidth(table);
            }
        }

        window.addEventListener("mousemove", onMouseMove);
        window.addEventListener("mouseup", onMouseUp);
    }

    function prepareTable() {
        if (state.prepared) {
            return;
        }

        const elements = getElements();
        const table = elements.mount?.querySelector(".links-table");

        if (!table) {
            return;
        }

        ensureTableColGroup(table);
        applyStoredColumnWidths(table);

        Array.from(table.querySelectorAll("thead th")).forEach((headerCell, columnIndex) => {
            headerCell.classList.add("links-resizable-th");

            if (headerCell.querySelector(":scope > .links-column-resizer")) {
                return;
            }

            const handle = headerCell.querySelector(".links-column-resizer");

            handle?.addEventListener("click", (event) => {
                event.preventDefault();
                event.stopPropagation();
            });

            handle?.addEventListener("mousedown", (event) => {
                event.preventDefault();
                event.stopPropagation();

                startColumnResize(event, table, columnIndex, headerCell);
            });
        });

        state.prepared = true;
    }

    function render() {
        const elements = getElements();

        if (!elements.body || !elements.empty) {
            return;
        }

        elements.body.innerHTML = state.links
            .map((link, index) => createLinkRowMarkup(link, index, state.readOnly))
            .join("");

        setEmptyVisible(elements.empty, state.links.length === 0);
        setDisabled(elements.addButton, state.readOnly);

        notifyAfterRender();
    }

    function resetDialog() {
        const elements = getElements();

        state.editingIndex = null;

        setInputValue(elements.descriptionInput, "");
        setInputValue(elements.urlInput, "");
        setElementText(elements.status, "Create a new link.");
        setHidden(elements.saveButton, false);
        setDisabled(elements.saveButton, false);

        if (elements.descriptionInput) {
            elements.descriptionInput.maxLength = state.config.maxDescriptionLength;
            elements.descriptionInput.required = true;
        }

        if (elements.urlInput) {
            elements.urlInput.maxLength = state.config.maxUrlLength;
            elements.urlInput.required = true;
        }
    }

    function openLinkDialog(index = null) {
        if (state.readOnly) {
            return;
        }

        const elements = getElements();
        const link = Number.isInteger(index) ? getLink(index) : null;

        resetDialog();
        state.editingIndex = Number.isInteger(index) ? index : null;

        if (link) {
            setElementText(elements.title, "Edit Link");
            setElementText(elements.status, "Edit the description or url.");
            setInputValue(elements.descriptionInput, link.description || "");
            setInputValue(elements.urlInput, link.linkUrl || "");
        } else {
            setElementText(elements.title, "Add Link");
            setElementText(elements.status, "Create a new link.");
        }

        showDialog(elements.dialog);

        setTimeout(() => {
            elements.descriptionInput?.focus?.();
        }, 0);
    }

    function openAddDialog() {
        openLinkDialog(null);
    }

    function openEditDialog(index) {
        openLinkDialog(index);
    }

    function closeLinkDialog() {
        const elements = getElements();

        closeDialog(elements.dialog);
        resetDialog();
    }

    function validatePendingLink() {
        const elements = getElements();
        const description = normalizeText(elements.descriptionInput?.value).trim();
        const url = normalizeText(elements.urlInput?.value).trim();

        if (!description) {
            return "Description is required.";
        }

        if (description.length > state.config.maxDescriptionLength) {
            return `Description must be maximum ${state.config.maxDescriptionLength} characters.`;
        }

        if (!isValidUrl(url)) {
            return "Url must be a valid http or https URL.";
        }

        if (url.length > state.config.maxUrlLength) {
            return `Url must be maximum ${state.config.maxUrlLength} characters.`;
        }

        return "";
    }

    function buildPendingLink() {
        const elements = getElements();
        const createdByText = getCurrentUserText();
        const existingLink = state.editingIndex != null ? getLink(state.editingIndex) : null;

        return {
            entityLinkPK: existingLink?.entityLinkPK ?? "",
            description: normalizeText(elements.descriptionInput?.value).trim(),
            linkUrl: normalizeText(elements.urlInput?.value).trim(),
            createdById: existingLink?.createdById ?? "",
            createdByText: existingLink?.createdByText || createdByText,
            createdTime: existingLink?.createdTime || nowIsoLocal(),
            isNew: existingLink ? existingLink.isNew : true
        };
    }

    function saveLinkIntoTable(link) {
        if (state.editingIndex != null) {
            updateLink(state.editingIndex, link);
        } else {
            addLink(link);
        }
    }

    async function savePendingLink() {
        if (state.readOnly) {
            return;
        }

        const elements = getElements();
        const validationMessage = validatePendingLink();

        if (validationMessage) {
            setElementText(elements.status, validationMessage);
            window.alert(validationMessage);
            return;
        }

        const pendingLink = buildPendingLink();
        setElementText(elements.status, "Checking link...");

        const reachability = await probeUrlReachability(pendingLink.linkUrl);

        if (!reachability.reachable) {
            const message = reachability.verified
                ? `The url could not be reached (${reachability.message}). Save anyway?`
                : `The url could not be verified (${reachability.message}). Save anyway?`;

            setElementText(elements.status, message);

            if (!window.confirm(message)) {
                return;
            }
        } else {
            setElementText(elements.status, reachability.message);
        }

        saveLinkIntoTable(pendingLink);
        closeLinkDialog();
    }

    function handleEdit(index) {
        const link = getLink(index);

        if (!link) {
            return;
        }

        openEditDialog(index);
    }

    function handleDelete(index) {
        if (state.readOnly) {
            return;
        }

        const link = getLink(index);

        if (!link) {
            return;
        }

        if (state.config.confirmDelete && !window.confirm("Delete this link?")) {
            return;
        }

        removeLink(index);
    }

    function handleTableClick(event) {
        const editButton = event.target.closest("[data-link-edit]");
        const deleteButton = event.target.closest("[data-link-delete]");

        if (editButton) {
            event.preventDefault();
            event.stopPropagation();
            handleEdit(Number(editButton.getAttribute("data-link-edit")));
            return;
        }

        if (deleteButton) {
            event.preventDefault();
            event.stopPropagation();
            handleDelete(Number(deleteButton.getAttribute("data-link-delete")));
        }
    }

    function handleTableDoubleClick(event) {
        if (!state.config.allowOpenOnRowDoubleClick) {
            return;
        }

        const row = event.target.closest("[data-link-index]");

        if (!row) {
            return;
        }

        const link = getLink(Number(row.getAttribute("data-link-index")));

        if (!link) {
            return;
        }

        openLinkInBrowser(link);
    }

    function bind(customConfig = {}) {
        state.config = {
            ...state.config,
            ...customConfig
        };

        state.readOnly = state.config.readOnly === true || state.readOnly === true;

        ensureMountMarkup();
        ensureDialogMarkup();
        prepareTable();

        if (state.bound) {
            setReadOnly(state.readOnly);
            return;
        }

        const elements = getElements();

        elements.addButton?.addEventListener("click", openAddDialog);
        elements.saveButton?.addEventListener("click", async () => {
            await savePendingLink();
        });
        elements.cancelButton?.addEventListener("click", closeLinkDialog);
        elements.closeButton?.addEventListener("click", closeLinkDialog);

        elements.dialog?.addEventListener("cancel", (event) => {
            event.preventDefault();
            closeLinkDialog();
        });

        elements.body?.addEventListener("click", handleTableClick);
        elements.body?.addEventListener("dblclick", handleTableDoubleClick);

        state.bound = true;

        setReadOnly(state.readOnly);
    }

    function destroy() {
        state.bound = false;
        state.prepared = false;
    }

    return {
        bind,
        destroy,

        loadFromDocument,
        writeToDocument,

        setLinks,
        getLinks,
        getLink,
        clear,

        setReadOnly,

        addLink,
        updateLink,
        removeLink,

        render,

        openAddDialog,
        openEditDialog,
        closeLinkDialog,
        resetDialog,

        openLinkInBrowser
    };
}
