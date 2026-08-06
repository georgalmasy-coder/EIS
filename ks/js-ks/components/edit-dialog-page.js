import { getEditDialogContext, isEditDialogMode, requestEditDialogClose, requestEditDialogOpenHistory, requestEditDialogSaved } from "./edit-dialog-bridge.js";

const COMMON_SHELL_SELECTORS = [
    ".topbar",
    ".menu-toggle",
    ".side-menu",
    ".left-hover-zone"
];

export function getEditDialogPageContext() {
    return getEditDialogContext();
}

export function isEditDialogPageMode() {
    return isEditDialogMode();
}

export function applyEditDialogShellMode(root = document) {
    const context = getEditDialogContext();

    if (!context.modal) {
        return context;
    }

    document.body?.classList.add("eis-edit-dialog-mode");

    COMMON_SHELL_SELECTORS.forEach((selector) => {
        root.querySelectorAll?.(selector)?.forEach((element) => {
            element.hidden = true;
            element.setAttribute("aria-hidden", "true");
            element.style.display = "none";
        });
    });

    return context;
}

export function closeEditDialog(reason = "cancel", payload = {}) {
    return requestEditDialogClose(reason, payload);
}

export function notifyEditDialogSaved(payload = {}) {
    return requestEditDialogSaved(payload);
}

export function requestHistoricalEditDialog(payload = {}) {
    return requestEditDialogOpenHistory(payload);
}
