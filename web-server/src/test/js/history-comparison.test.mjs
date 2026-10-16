import assert from "node:assert/strict";
import {readFile} from "node:fs/promises";
import vm from "node:vm";
import {getComparisonFields, renderComparison, renderVersionHeadings, renderComparisonTabs, bindComparisonTabs} from "../../main/webapp/js/components/history-diff-dialog.js";
import {getComparisonSections, renderTableComparison, alignComparisonRows} from "../../main/webapp/js/components/history-diff-sections.js";

globalThis.Node = {TEXT_NODE: 3};
function field(name, value, attributes = {}, children = []) {
    return {
        tagName: name, textContent: value, children,
        childNodes: [{nodeType: 3, textContent: value}],
        getAttribute: key => attributes[key] ?? null,
        getElementsByTagName: tag => children.filter(child => child.tagName === tag)
    };
}
const left = getComparisonFields({children: [
    field("Name", "Old <name>", {header: "Name"}),
    field("Active", "true", {control: "checkbox"}),
    field("Secret", "hidden", {visible: "false"}),
    field("Id", "1", {control: "hidden"}),
    field("Status", "", {control: "select"}, [field("Value", "2"), field("Option", "Approved", {value: "2", selected: "true"})])
]});
assert.deepEqual([...left.keys()], ["Name", "Active", "Status"]);
assert.equal(left.get("Status").value, "Approved");
assert.equal(left.get("Active").value, "Yes");
const right = new Map(left);
right.set("Name", {label: "Name", value: "New name"});
right.set("Added", {label: "Added field", value: ""});
const markup = renderComparison(left, right);
assert.equal((markup.match(/is-changed/g) || []).length, 2);
assert.ok(markup.includes("Old &lt;name&gt;"));
assert.ok(markup.includes("Field not present"));
assert.throws(() => getComparisonFields(null), /No entity details/);
const third = new Map(right);
third.set("Active", {label: "Active", value: "No"});
const tripleMarkup = renderComparison(left, right, third);
assert.equal((tripleMarkup.match(/history-diff-field/g) || []).length, 12, "four fields across three versions");
assert.equal((tripleMarkup.match(/is-changed/g) || []).length, 3, "changes in the third version are highlighted too");
const headings = renderVersionHeadings(["2"], [{version: "2", changedDateTime: "2026-10-15 12:00", changedByUserId: "User <name>"}]);
assert.match(headings, /Version 2/);
assert.match(headings, /Changed: 2026-10-15 12:00/);
assert.match(headings, /Changed By: User &lt;name&gt;/);
assert.equal((headings.match(/<span>/g) || []).length, 3);
assert.ok(!headings.includes("<br>"), "heading values are adjacent, not stacked");

// Each version gets fresh row PKs; provenance and related entity identities survive saves.
function xmlNode(tag, text = "", attrs = {}, children = []) {
    const node = field(tag, text, attrs, children);
    node.getElementsByTagName = name => children.flatMap(child => [
        ...(child.tagName === name ? [child] : []), ...child.getElementsByTagName(name)
    ]);
    return node;
}
function notesDoc(pk, text) {
    const root = xmlNode("Info", "", {}, [xmlNode("EntityNotes", "", {}, [xmlNode("EntityNote", "", {}, [
        xmlNode("EntityNotePK", pk), xmlNode("NoteText", text), xmlNode("CreatedTime", "2026-10-16T09:00:00"),
        xmlNode("CreatedById", "", {}, [xmlNode("Value", "12"), xmlNode("Option", "User", {selected: "true"})])
    ])])]);
    return {documentElement: root};
}
const oldNotes = getComparisonSections(notesDoc("101", "Old <note>\nFull text"), ["Notes"]);
const newNotes = getComparisonSections(notesDoc("202", "Updated note"), ["Notes"]);
assert.equal(oldNotes[0].rows[0].key, newNotes[0].rows[0].key, "copied notes align despite different PKs");
const noteComparison = renderTableComparison([oldNotes[0], newNotes[0]], 0);
assert.equal((noteComparison.match(/class="is-changed"/g) || []).length, 2, "only the changed cell is highlighted in both versions");
assert.ok(noteComparison.includes("Old &lt;note&gt;\nFull text"));
assert.ok(!noteComparison.includes("Not present"));
const emptyNotes = {...oldNotes[0], rows: []};
const addedDeleted = renderTableComparison([oldNotes[0], emptyNotes, newNotes[0]], 0);
assert.equal((addedDeleted.match(/Not present/g) || []).length, 1);
assert.equal((addedDeleted.match(/<table /g) || []).length, 3);
const reordered = {...oldNotes[0], rows: [
    {key: "second", cells: oldNotes[0].rows[0].cells}, oldNotes[0].rows[0]
]};
assert.ok(!renderTableComparison([reordered, {...reordered, rows: [...reordered.rows].reverse()}], 1).includes('class="is-changed"'), "row order alone does not count as a change");
assert.deepEqual(getComparisonSections(notesDoc("1", "text"), []), [], "only existing editor tabs are included");
const rowHeights = [20, 65, 30].map(height => ({dataset: {diffRow: "same"}, style: {}, getBoundingClientRect: () => ({height})}));
alignComparisonRows({querySelectorAll: () => rowHeights});
assert.deepEqual(rowHeights.map(row => row.style.height), ["65px", "65px", "65px"], "corresponding rows align across three versions");
const allDoc = notesDoc("1", "Note");
allDoc.querySelector = () => null;
function collection(container, name, entries) {
    allDoc.documentElement.children.push(xmlNode(container, "", {}, [xmlNode(name, "", {}, Object.entries(entries).map(([key, value]) => xmlNode(key, value)))]));
}
collection("EntityAttachments", "EntityAttachment", {EntityAttachmentPK: "7", FileName: "File.pdf", Description: "Attachment", FileSize: "2048"});
collection("EntityLinks", "EntityLink", {EntityLinkPK: "8", Description: "Link", LinkUrl: "https://example.com", CreatedTime: "2026-10-16T09:00:00"});
collection("EntityRelations", "EntityRelation", {EntityRelationPK: "9", RelatedEntityType: "6", RelatedEntityId: "10", RelatedEntityTypeName: "Requirement", RelatedEntityCode: "REQ-10", RelatedEntityName: "Related requirement", RelationTypeName: "Confirmed"});
const allSections = getComparisonSections(allDoc, ["Attachments", "Notes", "Links", "Relations", "Interfaces"]);
assert.equal(allSections.length, 6, "includes both interface tables");
assert.deepEqual(allSections.slice(0, 4).map(section => section.title), ["Attachments", "Notes", "Links", "Relations"]);
assert.deepEqual(allSections.slice(0, 4).map(section => section.columns.length), [5, 3, 4, 6]);
assert.ok(allSections[0].rows[0].cells[2].value.includes("KB"), "attachment file size uses the tab's formatter");
assert.equal(allSections[2].rows[0].cells[1].value, "https://example.com");
assert.equal(allSections[3].rows[0].cells[3].value, "Confirmed");
assert.ok(renderTableComparison([allSections[4], allSections[4]], 4).includes("No rows."));
const tabsMarkup = renderComparisonTabs([{fields: left, sections: allSections}, {fields: right, sections: allSections}, {fields: third, sections: allSections}]);
assert.equal((tabsMarkup.match(/role="tablist"/g) || []).length, 3, "each version has its own tabs");
assert.equal((tabsMarkup.match(/role="tab" /g) || []).length, 18, "six tabs per version, grouping both interface lists in one tab");
assert.equal((tabsMarkup.match(/role="tabpanel"/g) || []).length, 18);
assert.equal((tabsMarkup.match(/class="history-diff-tab-marker"/g) || []).length, 3, "only Basis Info is marked when lists are identical");
assert.ok(tabsMarkup.includes('aria-label="Basis Info — contains changes"'));
const unchangedTabs = renderComparisonTabs([{fields: left, sections: allSections}, {fields: left, sections: allSections}]);
assert.ok(!unchangedTabs.includes('class="history-diff-tab-marker"'), "unchanged tabs have no marker");
const changedNotesTabs = renderComparisonTabs([
    {fields: left, sections: oldNotes}, {fields: left, sections: oldNotes}, {fields: left, sections: newNotes}
]);
assert.equal((changedNotesTabs.match(/aria-label="Notes — contains changes"/g) || []).length, 3, "changes only in the third version mark Notes in all columns");
assert.equal((changedNotesTabs.match(/class="history-diff-tab-marker"/g) || []).length, 3);
const deletedNotesTabs = renderComparisonTabs([{fields: left, sections: oldNotes}, {fields: left, sections: [emptyNotes]}]);
assert.equal((deletedNotesTabs.match(/aria-label="Notes — contains changes"/g) || []).length, 2, "missing rows mark the affected tab");
const reorderedTabs = renderComparisonTabs([{fields: left, sections: [reordered]}, {fields: left, sections: [{...reordered, rows: [...reordered.rows].reverse()}]}]);
assert.ok(!reorderedTabs.includes('class="history-diff-tab-marker"'), "reordering alone does not mark a tab");
assert.ok(!deletedNotesTabs.includes("Ikke til stede"));
assert.ok(deletedNotesTabs.includes("Not present"));
const changedInterfaces = allSections.map((section, index) => index === 5 ? {...section, rows: [{key: "new-interface", cells: section.columns.map(() => ({value: "New", html: "New"}))}]} : section);
const interfaceTabs = renderComparisonTabs([{fields: left, sections: allSections}, {fields: left, sections: changedInterfaces}]);
assert.equal((interfaceTabs.match(/aria-label="Interfaces — contains changes"/g) || []).length, 2, "a change in either interface list marks their shared tab");
assert.equal((tabsMarkup.match(/data-diff-panel="4"[^>]*hidden/g) || []).length, 3, "Relations starts hidden in all version columns");
const tabsEvents = {};
const tabButtons = Array.from({length: 3}, () => Array.from({length: 6}, (_, index) => ({
    dataset: {diffTab: String(index)}, attributes: {},
    setAttribute(name, value) { this.attributes[name] = value; }
}))).flat();
const tabPanels = tabButtons.map(tab => ({dataset: {diffPanel: tab.dataset.diffTab}}));
bindComparisonTabs({
    addEventListener(type, fn) { tabsEvents[type] = fn; },
    querySelectorAll(selector) { return selector === "[data-diff-tab]" ? tabButtons : selector === "[data-diff-panel]" ? tabPanels : []; }
});
tabsEvents.click({target: {closest: () => tabButtons[4]}});
assert.equal(tabButtons.filter(tab => tab.attributes["aria-selected"] === "true").length, 3);
assert.ok(tabButtons.filter(tab => tab.attributes["aria-selected"] === "true").every(tab => tab.dataset.diffTab === "4"));
assert.equal(tabPanels.filter(panel => !panel.hidden).length, 3, "Relations is shown side by side; other lists stay hidden");

class Element {
    constructor() { this.events = {}; this.children = []; this.style = {}; }
    addEventListener(type, fn) { this.events[type] = fn; }
    removeEventListener(type) { delete this.events[type]; }
    appendChild(child) { this.children.push(child); return child; }
    setAttribute() {}
    getBoundingClientRect() { return {width: 250, height: 40}; }
    focus() {}
    remove() { this.removed = true; }
    contains(target) { return this.children.includes(target); }
}
const body = new Element();
const table = new Element();
const head = new Element();
body.closest = () => table;
table.querySelector = () => head;
const document = new Element();
document.body = new Element();
document.getElementById = id => id === "historyBody" ? body : new Element();
document.createElement = () => new Element();
let opened;
const source = (await readFile(new URL("../../main/webapp/js/components/history-table.js", import.meta.url), "utf8"))
    .replace(/^import[^\n]+;\r?\n/gm, "").replace("export function", "function");
const sandbox = {
    document, window: Object.assign(new Element(), {innerWidth: 1000, innerHeight: 800}),
    escapeHtml: value => String(value),
    applySortIndicators() {}, compareSortableValues: (a, b) => String(a).localeCompare(String(b)),
    openHistoryDiff: args => { opened = args; }
};
vm.createContext(sandbox);
vm.runInContext(source + "\nglobalThis.history = createHistoryTable({id: 'entity', buildVersionUrl() {}, findDetailNode() {}});", sandbox);
const rowMarkup = vm.runInContext('createHistoryRowMarkup({version: "2", latest: false, changedDateTime: "2026-10-15", changedByUserId: "User"}, new Set(["2"]))', sandbox);
assert.equal((rowMarkup.match(/<td\b/g) || []).length, 4, "history rows must include the Select cell");
assert.match(rowMarkup, /<td class="history-select-cell"><input type="checkbox"[^>]*checked/);
const history = sandbox.history;
history.bind();
function select(version, checked = true) {
    body.events.change({target: {matches: () => true, checked, closest: () => ({dataset: {version}})}});
}
function menu() {
    let prevented = false;
    table.events.contextmenu({clientX: 10, clientY: 10, preventDefault() { prevented = true; }});
    return prevented;
}
assert.equal(menu(), true);
assert.equal(document.body.children.at(-1).children[0].disabled, true);
document.body.children.at(-1).children[0].events.click();
assert.equal(opened, undefined, "disabled action cannot open comparison");
select("10");
assert.equal(menu(), true);
assert.equal(document.body.children.at(-1).children[0].disabled, true);
select("2");
assert.equal(menu(), true);
let button = document.body.children.at(-1).children[0];
assert.equal(button.disabled, false);
assert.equal(button.textContent, "Compare Version 2 and version 10");
history.render();
assert.equal(menu(), true, "rendering preserves selection");
button = document.body.children.at(-1).children[0];
button.events.click();
assert.deepEqual(Array.from(opened.versions), ["2", "10"]);
select("3");
assert.equal(menu(), true);
assert.equal(document.body.children.at(-1).children[0].disabled, false, "three versions enable comparison");
button = document.body.children.at(-1).children[0];
assert.equal(button.textContent, "Compare Version 2, version 3 and version 10");
button.events.click();
assert.deepEqual(Array.from(opened.versions), ["2", "3", "10"]);
select("4");
assert.equal(menu(), true);
assert.equal(document.body.children.at(-1).children[0].disabled, true, "four versions disable comparison");
select("4", false);
select("3", false);
assert.equal(menu(), true);
history.setContext({id: "another-entity"}, {render: false});
assert.equal(menu(), true);
assert.equal(document.body.children.at(-1).children[0].disabled, true, "selection is reset for another entity");
history.destroy();
assert.equal(table.events.contextmenu, undefined);
for (const name of ["stakeholder", "stakeholderrequirement", "systemrequirement", "functionalstructure", "logicalstructure", "systemsbreakdown"]) {
    const page = await readFile(new URL(`../../main/webapp/js/pages/${name}-edit-page.js`, import.meta.url), "utf8");
    const html = await readFile(new URL(`../../main/resources/html-pages/${name}-edit.html`, import.meta.url), "utf8");
    assert.ok(page.includes("buildVersionUrl: ({id, version})"), `${name}: version API configured`);
    assert.ok(page.includes('history-table.js?rev=diff-fixed-height-2026-10-16"'), `${name}: updated history component bypasses cached imports`);
    assert.ok(page.includes("    findDetailNode,"), `${name}: uses Basis Info detail node`);
    assert.ok(html.includes('scope="col">Select</th>'), `${name}: Select header`);
}
console.log("History comparison tests passed.");
