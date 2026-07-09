const MESSAGE_SOURCE = "eis-edit-dialog";

function getSearchParams() {
    try {
        return new URLSearchParams(window.location.search);
    } catch {
        return new URLSearchParams();
    }
}

export function isEditDialogMode() {
    return getSearchParams().get("modal") === "1";
}

export function getEditDialogContext() {
    const params = getSearchParams();

    return {
        modal: params.get("modal") === "1",
        dialogId: params.get("dialogId") || "",
        page: params.get("page") || "",
        mode: params.get("mode") || "",
        id: params.get("id") || "",
        version: params.get("version") || "",
        readOnly: params.get("readOnly") === "1"
    };
}

function postDialogMessage(type, payload = {}) {
    const context = getEditDialogContext();

    if (!context.modal || !context.dialogId || window === window.top) {
        return false;
    }

    window.parent.postMessage({
        source: MESSAGE_SOURCE,
        type,
        dialogId: context.dialogId,
        payload
    }, window.location.origin);

    return true;
}

export function requestEditDialogClose(reason = "close", payload = {}) {
    return postDialogMessage("close", {
        reason,
        ...payload
    });
}

export function requestEditDialogSaved(payload = {}) {
    return postDialogMessage("saved", payload);
}

export function requestEditDialogOpenHistory(payload = {}) {
    return postDialogMessage("open-history", payload);
}

export function requestEditDialogStatus(payload = {}) {
    return postDialogMessage("status", payload);
}

export function getEditDialogMessageSource() {
    return MESSAGE_SOURCE;
}
