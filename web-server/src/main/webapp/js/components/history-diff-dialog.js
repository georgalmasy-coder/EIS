import {fieldDisplayValue, fieldHeader, fieldVisible, isHiddenField} from "../core/field-display.js";
import {escapeHtml} from "../core/html.js";
import {hasXmlParseError} from "../core/xml.js";
import {enableDialogGeometry} from "./dialog-geometry.js";
import {getComparisonSections, renderTableComparison, alignComparisonRows, hasTableChanges} from "./history-diff-sections.js?rev=diff-tab-changes-2026-10-16";

function getDiffHostDocument() {
    let view = window;
    // A native dialog cannot escape its iframe, even with position: fixed.
    while (view.frameElement?.matches(".eis-edit-dialog-frame")) {
        view = view.parent;
    }
    return view.document;
}

function ensureDiffStyles(host) {
    const href = new URL("../../css/history-table.css?rev=diff-tab-changes-2026-10-16", import.meta.url).href;
    const existing = [...host.querySelectorAll('link[rel="stylesheet"]')]
        .find(link => link.href === href);
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
    return renderBasisComparison(entities);
}

function fieldsDiffer(fields) {
    return fields.some(field => !field || field.value !== fields[0]?.value);
}

function renderBasisComparison(entities, selectedVersion = null) {
    const keys = [...new Set(entities.flatMap(entity => [...entity.keys()]))];
    return keys.map((key, index) => {
        const fields = entities.map(entity => entity.get(key));
        const label = fields.find(Boolean)?.label || key;
        const changed = fieldsDiffer(fields);
        return `<div class="history-diff-row${changed ? " is-changed" : ""}" data-diff-row="basis-${index}">${fields.map((field, versionIndex) => selectedVersion !== null && selectedVersion !== versionIndex ? "" : `
            <div class="history-diff-field">
                <div class="history-diff-label">${escapeHtml(field?.label || label)}${changed ? ' <span class="history-diff-marker">(Changed)</span>' : ""}</div>
                <div class="history-diff-value">${escapeHtml(field ? field.value : "Field not present")}</div>
            </div>`).join("")}</div>`;
    }).join("") || "<p>No fields to compare.</p>";
}

let comparisonSequence = 0;

export function renderComparisonTabs(entities) {
    const prefix = `history-diff-${++comparisonSequence}`;
    const groupName = section => section.title.startsWith("Interfaces") ? "Interfaces" : section.title;
    const groups = ["Basis Info", ...new Set(entities[0].sections.map(groupName))];
    const fieldKeys = new Set(entities.flatMap(entity => [...entity.fields.keys()]));
    const changedGroups = groups.map((name, index) => index === 0
        ? [...fieldKeys].some(key => fieldsDiffer(entities.map(entity => entity.fields.get(key))))
        : entities[0].sections.some((section, sectionIndex) => groupName(section) === name
            && hasTableChanges(entities.map(entity => entity.sections[sectionIndex]))));
    return `<div class="history-diff-version-columns">${entities.map((entity, versionIndex) => `
        <div class="history-diff-version-column">
            <div class="history-diff-tabs" role="tablist" aria-label="Version column ${versionIndex + 1}">${groups.map((name, tabIndex) => `
                <button type="button" role="tab" data-diff-tab="${tabIndex}" id="${prefix}-v${versionIndex}-tab${tabIndex}" aria-controls="${prefix}-v${versionIndex}-panel${tabIndex}" aria-selected="${tabIndex === 0}" tabindex="${tabIndex === 0 ? "0" : "-1"}"${changedGroups[tabIndex] ? ` title="Contains changes" aria-label="${escapeHtml(name)} — contains changes"` : ""}>${escapeHtml(name)}${changedGroups[tabIndex] ? ' <span class="history-diff-tab-marker" aria-hidden="true">*</span>' : ""}</button>`).join("")}</div>
            ${groups.map((name, tabIndex) => `<div role="tabpanel" tabindex="0" data-diff-panel="${tabIndex}" id="${prefix}-v${versionIndex}-panel${tabIndex}" aria-labelledby="${prefix}-v${versionIndex}-tab${tabIndex}" ${tabIndex === 0 ? "" : "hidden"}>
                ${tabIndex === 0 ? renderBasisComparison(entities.map(entity => entity.fields), versionIndex)
                    : entity.sections.map((section, index) => groupName(section) === name ? renderTableComparison(entities.map(entity => entity.sections[index]), index, versionIndex) : "").join("")}
            </div>`).join("")}
        </div>`).join("")}</div>`;
}

export function bindComparisonTabs(content) {
    function selectTab(index) {
        content.querySelectorAll("[data-diff-tab]").forEach(tab => {
            const selected = tab.dataset.diffTab === index;
            tab.setAttribute("aria-selected", String(selected));
            tab.tabIndex = selected ? 0 : -1;
        });
        content.querySelectorAll("[data-diff-panel]").forEach(panel => panel.hidden = panel.dataset.diffPanel !== index);
        alignComparisonRows(content);
    }
    content.addEventListener("click", event => {
        const tab = event.target.closest("[data-diff-tab]");
        if (tab) selectTab(tab.dataset.diffTab);
    });
    content.addEventListener("keydown", event => {
        const tab = event.target.closest("[data-diff-tab]");
        if (!tab || !["ArrowLeft", "ArrowRight", "Home", "End"].includes(event.key)) return;
        event.preventDefault();
        const tabs = [...tab.parentElement.querySelectorAll("[data-diff-tab]")];
        const current = tabs.indexOf(tab);
        const index = event.key === "Home" ? 0 : event.key === "End" ? tabs.length - 1
            : (current + (event.key === "ArrowRight" ? 1 : -1) + tabs.length) % tabs.length;
        selectTab(tabs[index].dataset.diffTab);
        tabs[index].focus();
    });
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
    await ensureDiffStyles(host);
    // The editor may have closed or navigated while the stylesheet was loading.
    if (!window.frameElement?.isConnected && host !== document) return;
    const dialog = host.createElement("dialog");
    dialog.className = "history-diff-dialog";
    dialog.setAttribute("data-dialog-geometry", "manual");
    dialog.style.setProperty("--history-diff-columns", String(versions.length));
    dialog.setAttribute("aria-label", "Diff");
    dialog.innerHTML = `
        <header class="history-diff-toolbar"><h2>Diff</h2><button type="button" autofocus>Close</button></header>
        <p class="history-diff-legend">Changed values are highlighted. Tabs marked * contain changes. Historical relations and interfaces show their last state before the next entity version.</p>
        <div class="history-diff-headings">${renderVersionHeadings(versions, historyRows)}</div>
        <div class="history-diff-content" aria-busy="true"><p role="status">Loading versions...</p></div>`;
    const controller = new AbortController();
    const editor = window.frameElement?.closest("dialog");
    let cleanupGeometry;
    let rowObserver;
    let disposed = false;
    const dispose = () => {
        if (disposed) return;
        disposed = true;
        controller.abort();
        cleanupGeometry?.();
        rowObserver?.disconnect();
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
    const enabledSections = [...document.querySelectorAll('[role="tab"]')].map(tab => tab.textContent.trim());
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
        return {fields: getComparisonFields(findDetailNode(doc)), sections: getComparisonSections(doc, enabledSections)};
    })).then(entities => {
        if (controller.signal.aborted) return;
        content.innerHTML = renderComparisonTabs(entities);
        bindComparisonTabs(content);
        alignComparisonRows(content);
        // Keep the fully loaded opening height when switching tabs. Preserve any
        // height already set by dragging/resizing while the versions were loading.
        if (!dialog.style.height) dialog.style.height = `${dialog.getBoundingClientRect().height}px`;
        let lastWidth = content.getBoundingClientRect().width;
        rowObserver = new host.defaultView.ResizeObserver(() => {
            const width = content.getBoundingClientRect().width;
            if (width === lastWidth) return;
            lastWidth = width;
            alignComparisonRows(content);
        });
        rowObserver.observe(content);
    }).catch(error => {
        if (controller.signal.aborted) return;
        content.innerHTML = `<p role="alert">Could not load versions. ${escapeHtml(error.message)}</p>`;
    }).finally(() => content.setAttribute("aria-busy", "false"));
    return dialog;
}
