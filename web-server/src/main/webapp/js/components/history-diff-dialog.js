import {fieldDisplayValue, fieldHeader, fieldVisible, isHiddenField} from "../core/field-display.js";
import {escapeHtml} from "../core/html.js";
import {hasXmlParseError} from "../core/xml.js";
import {enableDialogGeometry} from "./dialog-geometry.js";

function getDiffHostDocument() {
    let view = window;
    // A native dialog cannot escape its iframe, even with position: fixed.
    while (view.frameElement?.matches(".eis-edit-dialog-frame")) {
        view = view.parent;
    }
    return view.document;
}

function ensureDiffStyles(host) {
    const href = new URL("../../css/history-table.css", import.meta.url).href;
    const existing = [...host.querySelectorAll('link[rel="stylesheet"]')]
        .find(link => new URL(link.href).pathname === new URL(href).pathname);
    if (existing?.sheet) return Promise.resolve();
    const link = existing || host.createElement("link");
    return new Promise((resolve, reject) => {
        link.addEventListener("load", resolve, {once: true});
        link.addEventListener("error", () => reject(new Error("Could not load comparison styles.")), {once: true});
        if (!existing) {
            link.rel = "stylesheet";
            link.href = href;
            host.head.appendChild(link);
        }
    });
}

export function getComparisonFields(detail) {
    if (!detail) throw new Error("No entity details returned.");
    return new Map(Array.from(detail.children || [])
        .filter(field => fieldVisible(field) && !isHiddenField(field))
        .map(field => [field.tagName, {
            label: fieldHeader(field, field.tagName),
            value: fieldDisplayValue(field)
        }]));
}

export function renderComparison(...entities) {
    const keys = [...new Set(entities.flatMap(entity => [...entity.keys()]))];
    return keys.map(key => {
        const fields = entities.map(entity => entity.get(key));
        const label = fields.find(Boolean)?.label || key;
        const changed = fields.some(field => !field || field.value !== fields[0]?.value);
        return `<div class="history-diff-row${changed ? " is-changed" : ""}">${fields.map(field => `
            <div class="history-diff-field">
                <div class="history-diff-label">${escapeHtml(field?.label || label)}${changed ? ' <span class="history-diff-marker">(Changed)</span>' : ""}</div>
                <div class="history-diff-value">${escapeHtml(field ? field.value : "Field not present")}</div>
            </div>`).join("")}</div>`;
    }).join("") || "<p>No fields to compare.</p>";
}

export function renderVersionHeadings(versions, historyRows = []) {
    return versions.map(version => {
        const row = historyRows.find(row => row.version === version);
        return `<div class="history-diff-heading"><span>Version ${escapeHtml(version)}</span>
            <span>Changed: ${escapeHtml(row?.changedDateTime || "—")}</span>
            <span>Changed By: ${escapeHtml(row?.changedByUserId || "—")}</span></div>`;
    }).join("");
}

export async function openHistoryDiff({versions, historyRows, id, buildVersionUrl, findDetailNode}) {
    const host = getDiffHostDocument();
    if (host !== document) await ensureDiffStyles(host);
    // The editor may have closed or navigated while the stylesheet was loading.
    if (!window.frameElement?.isConnected && host !== document) return;
    const dialog = host.createElement("dialog");
    dialog.className = "history-diff-dialog";
    dialog.setAttribute("data-dialog-geometry", "manual");
    dialog.style.setProperty("--history-diff-columns", String(versions.length));
    dialog.setAttribute("aria-label", "Diff");
    dialog.innerHTML = `
        <header class="history-diff-toolbar"><h2>Diff</h2><button type="button" autofocus>Close</button></header>
        <p class="history-diff-legend">Changed values are highlighted.</p>
        <div class="history-diff-headings">${renderVersionHeadings(versions, historyRows)}</div>
        <div class="history-diff-content" aria-busy="true"><p role="status">Loading versions...</p></div>`;
    const controller = new AbortController();
    const editor = window.frameElement?.closest("dialog");
    let cleanupGeometry;
    let disposed = false;
    const dispose = () => {
        if (disposed) return;
        disposed = true;
        controller.abort();
        cleanupGeometry?.();
        window.removeEventListener("pagehide", dismiss);
        editor?.removeEventListener("close", dismiss);
        dialog.remove();
    };
    const dismiss = () => {
        if (dialog.open) dialog.close();
        // Remove synchronously: the source iframe's event loop may be going away.
        dispose();
    };
    dialog.querySelector("button").addEventListener("click", dismiss);
    dialog.addEventListener("close", dispose, {once: true});
    if (host !== document) {
        // Preserve the editor's original opening width and height limit.
        const measure = dialog.cloneNode(true);
        Object.assign(measure.style, {display: "block", visibility: "hidden", position: "fixed"});
        document.body.appendChild(measure);
        dialog.style.width = `${measure.getBoundingClientRect().width}px`;
        dialog.style.maxHeight = window.getComputedStyle(measure).maxHeight;
        measure.remove();
        window.addEventListener("pagehide", dismiss);
        editor?.addEventListener("close", dismiss);
    }
    host.body.appendChild(dialog);
    dialog.showModal();
    cleanupGeometry = enableDialogGeometry(dialog);
    const content = dialog.querySelector(".history-diff-content");
    Promise.all(versions.map(async version => {
        const url = buildVersionUrl({id, version});
        if (!url) throw new Error("Could not determine version URL.");
        const response = await fetch(url, {
            headers: {Accept: "application/xml,text/xml,*/*"},
            cache: "no-store",
            signal: controller.signal
        });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const doc = new DOMParser().parseFromString(await response.text(), "application/xml");
        if (hasXmlParseError(doc)) throw new Error("Invalid XML returned.");
        return getComparisonFields(findDetailNode(doc));
    })).then(entities => {
        content.innerHTML = renderComparison(...entities);
    }).catch(error => {
        if (controller.signal.aborted) return;
        content.innerHTML = `<p role="alert">Could not load versions. ${escapeHtml(error.message)}</p>`;
    }).finally(() => content.setAttribute("aria-busy", "false"));
    return dialog;
}
