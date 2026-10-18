import {parseEntityAttachmentsFromDoc, parseEntityNotesFromDoc, parseEntityLinksFromDoc, parseEntityRelationsFromDoc} from "../core/entity-xml.js";
import {parseDateTime} from "../core/date.js";
import {formatFileSize} from "../core/format.js";
import {escapeHtml} from "../core/html.js";
import {getInterfaceComparisonTables} from "./edit-interfaces-table.js?rev=history-sections-2026-10-16";

const createdBy = row => row.createdByText || row.createdById || "—";
const createdTime = row => parseDateTime(row.createdTime);
const provenance = row => `${row.createdById}:${row.createdTime}`;
const cell = value => ({value: String(value ?? ""), html: escapeHtml(value ?? "")});

function table(title, columns, records, key, values) {
    const occurrences = new Map();
    return {title, columns: columns.map(label => ({label})), rows: records.map(record => {
        // Notes and links get new database PKs when copied to the next entity version.
        const identity = key(record);
        const occurrence = occurrences.get(identity) || 0;
        occurrences.set(identity, occurrence + 1);
        return {key: `${identity}:${occurrence}`, cells: values(record).map(cell)};
    })};
}

export function getComparisonSections(doc, enabledSections) {
    const sections = [];
    if (enabledSections.includes("Attachments")) sections.push(table("Attachments",
        ["File Name", "Description", "File Size", "Uploaded By", "Uploaded"],
        parseEntityAttachmentsFromDoc(doc).filter(row => !row.isDeleted), row => row.entityAttachmentPK,
        row => [row.fileName, row.description, formatFileSize(row.fileSize), createdBy(row), createdTime(row)]));
    if (enabledSections.includes("Notes")) sections.push(table("Notes", ["Note", "Created By", "Created"],
        parseEntityNotesFromDoc(doc), provenance, row => [row.noteText, createdBy(row), createdTime(row)]));
    if (enabledSections.includes("Links")) sections.push(table("Links", ["Description", "URL", "Created By", "Created"],
        parseEntityLinksFromDoc(doc), provenance, row => [row.description, row.linkUrl, createdBy(row), createdTime(row)]));
    if (enabledSections.includes("Relations")) sections.push(table("Relations",
        ["Type", "ID", "Name", "Relation", "Created By", "Created"],
        parseEntityRelationsFromDoc(doc).filter(row => !row.isDeleted), row => `${row.relatedEntityType}:${row.relatedEntityId}`,
        row => [row.relatedEntityTypeName, row.relatedEntityCode, row.relatedEntityName, row.relationTypeName, createdBy(row), createdTime(row)]));
    if (enabledSections.includes("Interfaces")) sections.push(...getInterfaceComparisonTables(doc));
    return sections;
}

function cellChanged(peers, index) {
    return peers.some(peer => !peer || peer.cells[index]?.value !== peers[0]?.cells[index]?.value);
}

export function hasTableChanges(tables) {
    const maps = tables.map(table => new Map(table.rows.map(row => [row.key, row])));
    const keys = new Set(tables.flatMap(table => table.rows.map(row => row.key)));
    return [...keys].some(key => {
        const peers = maps.map(rows => rows.get(key));
        return peers.some(peer => !peer) || peers[0].cells.some((cell, index) => cellChanged(peers, index));
    });
}

export function renderTableComparison(tables, sectionIndex, selectedVersion = null) {
    const columns = tables[0].columns;
    const rowMaps = tables.map(table => new Map(table.rows.map(row => [row.key, row])));
    const keys = [...new Set(tables.flatMap(table => table.rows.map(row => row.key)))];
    const groupHeaders = columns.some(column => column.group)
        ? `<tr>${columns.reduce((groups, column) => {
            const last = groups.at(-1);
            if (last?.label === column.group) last.count++;
            else groups.push({label: column.group, count: 1});
            return groups;
        }, []).map(group => `<th colspan="${group.count}">${escapeHtml(group.label)}</th>`).join("")}</tr>` : "";
    return `<section class="history-diff-section"><h3>${escapeHtml(tables[0].title)}</h3>
        <div class="history-diff-tables">${tables.map((table, versionIndex) => selectedVersion !== null && versionIndex !== selectedVersion ? "" : `
        <div class="history-diff-table-panel"><table class="history-diff-table" aria-label="${escapeHtml(table.title)} — version column ${versionIndex + 1}">
        <thead>${groupHeaders}<tr data-diff-row="${sectionIndex}-header">${columns.map(column => `<th scope="col">${escapeHtml(column.label)}</th>`).join("")}</tr></thead>
        <tbody>${keys.map((key, rowIndex) => {
            const peers = rowMaps.map(rows => rows.get(key));
            const row = peers[versionIndex];
            const missing = peers.some(peer => !peer);
            if (!row) return `<tr data-diff-row="${sectionIndex}-${rowIndex}" class="is-changed"><td colspan="${columns.length}">Not present</td></tr>`;
            return `<tr data-diff-row="${sectionIndex}-${rowIndex}">${row.cells.map((cell, index) => {
                const changed = missing || cellChanged(peers, index);
                return `<td${changed ? ' class="is-changed"' : ""}>${cell.html}</td>`;
            }).join("")}</tr>`;
        }).join("") || `<tr><td colspan="${columns.length}">No rows.</td></tr>`}</tbody></table></div>`).join("")}</div></section>`;
}

export function alignComparisonRows(content) {
    const groups = new Map();
    content.querySelectorAll("[data-diff-row]").forEach(row => {
        row.style.height = "";
        const key = row.dataset.diffRow;
        if (!groups.has(key)) groups.set(key, []);
        groups.get(key).push(row);
    });
    for (const rows of groups.values()) {
        const height = Math.max(...rows.map(row => row.getBoundingClientRect().height));
        rows.forEach(row => row.style.height = `${height}px`);
    }
}
