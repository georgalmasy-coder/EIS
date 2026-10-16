const MODALS = 'dialog, [role="dialog"][aria-modal="true"]';
const HEADERS = '[data-dialog-drag-handle], .page-dialog-head, .help-dialog-head, .export-dialog-head, .import-dialog-head, .import-preview-header, .history-diff-toolbar, .dashboard-modal-header, .template-dialog-head, .dialog-head, .section-header';
const installations = new WeakMap();

export function dialogGeometryExcluded(dialog) {
    const location = dialog.ownerDocument.defaultView.location;
    const page = new URLSearchParams(location.search).get("page") || "";
    const context = `${location.pathname} ${page}`.toLowerCase();
    return /login|forgot-password|enter-new-password|reset-password|session-expired|createcustomer|customer-registration|customer-creation/.test(context)
        || Boolean(dialog.closest('[data-dialog-geometry="off"], .eis-login-dialog'));
}

function findHeader(root) {
    return root.querySelector(HEADERS) || root.querySelector('h1, h2, h3, .import-progress-dialog-text');
}

// Used by native dialogs, dashboard modals and the iframe editor shell.
export function enableDialogGeometry(dialog, header = findHeader(dialog)) {
    if (!header) return () => {};

    const viewport = dialog.ownerDocument.defaultView;
    let initial = null;
    const originalStyle = dialog.getAttribute("style");
    const originalHeaderStyle = header.getAttribute("style");
    if (viewport.getComputedStyle(dialog).position === "static") dialog.style.position = "relative";
    const handle = dialog.ownerDocument.createElement("span");
    handle.className = "eis-dialog-resize";
    handle.title = "Drag to resize";
    handle.setAttribute("aria-hidden", "true");
    dialog.appendChild(handle);
    header.style.cursor = "move";
    header.style.touchAction = "none";
    header.style.userSelect = "none";

    let gesture = null;
    let positioned = false;
    const bounds = () => ({
        width: viewport.document.documentElement.clientWidth,
        height: viewport.document.documentElement.clientHeight
    });
    const clamp = (value, min, max) => Math.max(min, Math.min(value, max));

    function place(left, top, width, height) {
        const available = bounds();
        // If the browser itself becomes smaller, keep the controls reachable.
        width = clamp(width, Math.min(initial.width, available.width), available.width);
        height = clamp(height, Math.min(initial.height, available.height), available.height);
        Object.assign(dialog.style, {
            position: "fixed", boxSizing: "border-box", margin: "0",
            right: "auto", bottom: "auto",
            left: `${clamp(left, 0, available.width - width)}px`,
            top: `${clamp(top, 0, available.height - height)}px`,
            width: `${width}px`, height: `${height}px`,
            minWidth: `${Math.min(initial.width, available.width)}px`,
            minHeight: `${Math.min(initial.height, available.height)}px`,
            maxWidth: `${available.width}px`, maxHeight: `${available.height}px`
        });
        positioned = true;
    }

    function start(event, resize) {
        if (event.button !== 0 || !event.isPrimary || gesture) return;
        if (!resize && event.target.closest("button, a, input, select, textarea, [role=button]")) return;
        const rect = dialog.getBoundingClientRect();
        // Capture the fully rendered size, including content loaded after opening.
        initial ||= rect;
        gesture = {target: event.currentTarget, id: event.pointerId, x: event.screenX, y: event.screenY, rect, resize};
        gesture.target.setPointerCapture(event.pointerId);
        event.preventDefault();
    }

    function move(event) {
        if (!gesture || gesture.id !== event.pointerId) return;
        const {rect, x, y, resize} = gesture;
        // Screen coordinates stay stable while the iframe moves beneath the pointer.
        const dx = event.screenX - x;
        const dy = event.screenY - y;
        if (resize) {
            dialog.setAttribute("data-dialog-resized", "");
            const available = bounds();
            place(rect.left, rect.top,
                clamp(rect.width + dx, Math.min(initial.width, available.width), available.width - rect.left),
                clamp(rect.height + dy, Math.min(initial.height, available.height), available.height - rect.top));
        } else {
            place(rect.left + dx, rect.top + dy, rect.width, rect.height);
        }
    }

    function stop(event) {
        if (!gesture || gesture.id !== event.pointerId) return;
        const {target, id} = gesture;
        gesture = null;
        if (target.hasPointerCapture(id)) target.releasePointerCapture(id);
    }

    const drag = event => start(event, false);
    const resize = event => start(event, true);
    header.addEventListener("pointerdown", drag);
    handle.addEventListener("pointerdown", resize);
    for (const target of [header, handle]) {
        target.addEventListener("pointermove", move);
        target.addEventListener("pointerup", stop);
        target.addEventListener("pointercancel", stop);
        target.addEventListener("lostpointercapture", stop);
    }
    const fit = () => {
        if (!positioned) return;
        if (gesture) stop({pointerId: gesture.id});
        const rect = dialog.getBoundingClientRect();
        place(rect.left, rect.top, rect.width, rect.height);
    };
    viewport.addEventListener("resize", fit);

    return () => {
        if (gesture) stop({pointerId: gesture.id});
        viewport.removeEventListener("resize", fit);
        header.removeEventListener("pointerdown", drag);
        handle.removeEventListener("pointerdown", resize);
        for (const target of [header, handle]) {
            target.removeEventListener("pointermove", move);
            target.removeEventListener("pointerup", stop);
            target.removeEventListener("pointercancel", stop);
            target.removeEventListener("lostpointercapture", stop);
        }
        handle.remove();
        dialog.removeAttribute("data-dialog-resized");
        if (originalStyle === null) dialog.removeAttribute("style");
        else dialog.setAttribute("style", originalStyle);
        if (originalHeaderStyle === null) header.removeAttribute("style");
        else header.setAttribute("style", originalHeaderStyle);
    };
}

export function enableEditDialogGeometry(dialog, iframe) {
    const title = iframe.contentDocument?.getElementById("pageTitle");
    // Prefer the editor's own title over headers in its nested help/note dialogs.
    const header = title?.closest('.section-header, .stakeholder-edit-header-row') || title?.parentElement;
    return enableDialogGeometry(dialog, header || iframe.contentDocument?.querySelector(HEADERS));
}

// Observe open/close and dynamically inserted dialogs without changing browser APIs.
export function installDialogGeometry(root = document) {
    if (installations.has(root)) return;
    const records = new Map();
    function visible(dialog) {
        return dialog.isConnected && !dialog.closest('[hidden]')
            && (dialog.localName !== "dialog" || dialog.open)
            && dialog.getClientRects().length > 0;
    }
    function scan() {
        for (const [dialog, cleanup] of records) {
            if (!visible(dialog)) {
                cleanup();
                records.delete(dialog);
            }
        }
        for (const dialog of root.querySelectorAll(MODALS)) {
            // The iframe shell attaches its own header once its page has loaded.
            if (records.has(dialog) || dialog.dataset.dialogGeometry === "manual" || dialog.classList.contains("eis-edit-dialog")
                || dialogGeometryExcluded(dialog) || !visible(dialog)) continue;
            const header = findHeader(dialog);
            if (header) records.set(dialog, enableDialogGeometry(dialog, header));
        }
    }
    const observer = new MutationObserver(mutations => {
        if (mutations.some(mutation => mutation.type === "attributes"
            || [...mutation.addedNodes, ...mutation.removedNodes].some(node => node.nodeType === 1))) scan();
    });
    observer.observe(root.documentElement, {subtree: true, childList: true, attributes: true, attributeFilter: ["open", "hidden"]});
    installations.set(root, observer);
    scan();
}
