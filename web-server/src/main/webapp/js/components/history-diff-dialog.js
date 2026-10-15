import {fieldDisplayValue, fieldHeader, fieldVisible, isHiddenField} from "../core/field-display.js";
import {escapeHtml} from "../core/html.js";
import {hasXmlParseError} from "../core/xml.js";

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

export function openHistoryDiff({versions, historyRows, id, buildVersionUrl, findDetailNode}) {
    const dialog = document.createElement("dialog");
    dialog.className = "history-diff-dialog";
    dialog.style.setProperty("--history-diff-columns", String(versions.length));
    dialog.setAttribute("aria-label", "Diff");
    dialog.innerHTML = `
        <header class="history-diff-toolbar"><h2>Diff</h2><button type="button" autofocus>Close</button></header>
        <p class="history-diff-legend">Changed values are highlighted.</p>
        <div class="history-diff-headings">${renderVersionHeadings(versions, historyRows)}</div>
        <div class="history-diff-content" aria-busy="true"><p role="status">Loading versions...</p></div>`;
    const controller = new AbortController();
    dialog.querySelector("button").addEventListener("click", () => dialog.close());
    dialog.addEventListener("close", () => {
        controller.abort();
        dialog.remove();
    }, {once: true});
    document.body.appendChild(dialog);
    dialog.showModal();
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
}
