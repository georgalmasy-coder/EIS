import { closeDialogElement } from "../core/dom.js";
import { getEditDialogMessageSource } from "./edit-dialog-bridge.js";

let dialogSequence = 0;
const dialogRegistry = new Map();
let messageListenerInstalled = false;

function ensureMessageListener() {
    if (messageListenerInstalled) {
        return;
    }

    window.addEventListener("message", handleDialogMessage);
    messageListenerInstalled = true;
}

function handleDialogMessage(event) {
    if (event.origin !== window.location.origin) {
        return;
    }

    const data = event.data || {};

    if (data.source !== getEditDialogMessageSource() || !data.dialogId) {
        return;
    }

    const record = dialogRegistry.get(data.dialogId);

    if (!record) {
        return;
    }

    if (data.type === "status") {
        if (typeof record.onStatus === "function") {
            record.onStatus(data.payload || {});
        }

        return;
    }

    if (data.type === "open-history") {
        const payload = data.payload || {};
        const { dialogId, ...baseOptions } = record.baseOptions;
        const nextOptions = {
            ...baseOptions,
            mode: payload.mode || "edit-version",
            id: payload.id || "",
            version: payload.version || "",
            readOnly: true,
            title: payload.title || record.baseOptions.title || "",
            dialogClassName: payload.dialogClassName || record.baseOptions.dialogClassName || "",
            onSaved: record.baseOptions.onSaved,
            onClosed: record.baseOptions.onClosed,
            onStatus: record.baseOptions.onStatus,
            onOpenHistoricalVersion: record.baseOptions.onOpenHistoricalVersion,
            onRequestRefresh: record.baseOptions.onRequestRefresh
        };

        openEditDialog(nextOptions);
        return;
    }

    const reason = data.payload?.reason || data.type;

    closeDialogRecord(record, reason, data.payload || {});

    if (data.type === "saved" && typeof record.baseOptions.onSaved === "function") {
        record.baseOptions.onSaved(data.payload || {});
    }
}

function closeDialogRecord(record, reason, payload = {}) {
    if (!record) {
        return;
    }

    if (record.closing) {
        return;
    }

    record.closing = true;
    dialogRegistry.delete(record.dialogId);

    if (record.dialog?.open) {
        closeDialogElement(record.dialog);
    } else {
        record.dialog?.removeAttribute("open");
    }

    record.dialog?.remove();

    if (typeof record.baseOptions.onClosed === "function") {
        record.baseOptions.onClosed({
            reason,
            payload,
            dialogId: record.dialogId
        });
    }
}

function buildDialogUrl(options, dialogId) {
    if (!options.page) {
        throw new Error("Missing edit page name.");
    }

    const url = new URL(`/web/view?page=${encodeURIComponent(options.page)}`, window.location.href);

    if (options.mode) {
        url.searchParams.set("mode", options.mode);
    }

    if (options.id) {
        url.searchParams.set("id", options.id);
    }

    if (options.version) {
        url.searchParams.set("version", options.version);
    }

    url.searchParams.set("modal", "1");
    url.searchParams.set("dialogId", dialogId);

    if (options.readOnly) {
        url.searchParams.set("readOnly", "1");
    }

    if (options.title) {
        url.searchParams.set("dialogTitle", options.title);
    }

    return url.toString();
}

function createDialogElement(options, dialogId) {
    const dialog = document.createElement("dialog");
    dialog.className = `eis-edit-dialog ${options.dialogClassName || ""}`.trim();
    dialog.setAttribute("aria-label", options.title || "Edit dialog");

    dialog.innerHTML = `
        <form method="dialog" class="eis-edit-dialog-form">
            <iframe
                class="eis-edit-dialog-frame"
                title="${escapeHtml(options.title || "Edit dialog")}"
                loading="eager"
                referrerpolicy="same-origin"
            ></iframe>
        </form>
    `;

    dialog.dataset.dialogId = dialogId;

    return dialog;
}

function escapeHtml(value) {
    return String(value || "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

export function openEditDialog(options = {}) {
    ensureMessageListener();

    const dialogId = options.dialogId || `eis-edit-dialog-${++dialogSequence}`;
    const dialog = createDialogElement(options, dialogId);
    const iframe = dialog.querySelector("iframe");

    if (!iframe) {
        throw new Error("Could not create edit dialog iframe.");
    }

    const record = {
        dialogId,
        dialog,
        iframe,
        baseOptions: {
            ...options,
            dialogId
        },
        onStatus: options.onStatus || null
    };

    dialogRegistry.set(dialogId, record);

    dialog.addEventListener("cancel", (event) => {
        event.preventDefault();
        closeDialogRecord(record, "cancel");
    });

    dialog.addEventListener("close", () => {
        if (dialogRegistry.has(dialogId) && !record.closing) {
            closeDialogRecord(record, "close");
        }
    });

    const body = document.body || document.documentElement;
    body.appendChild(dialog);

    iframe.src = buildDialogUrl(options, dialogId);

    if (typeof dialog.showModal === "function") {
        dialog.showModal();
    } else {
        dialog.setAttribute("open", "open");
    }

    return {
        dialogId,
        dialog,
        iframe,
        close: () => closeDialogRecord(record, "programmatic")
    };
}
