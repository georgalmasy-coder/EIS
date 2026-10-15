import assert from "node:assert/strict";
import {readFile} from "node:fs/promises";
import vm from "node:vm";
import {getComparisonFields, renderComparison, renderVersionHeadings} from "../../main/webapp/js/components/history-diff-dialog.js";

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
    assert.ok(page.includes('history-table.js?rev=history-compare-2026-10-15-3"'), `${name}: updated history component bypasses cached imports`);
    assert.ok(page.includes("    findDetailNode,"), `${name}: uses Basis Info detail node`);
    assert.ok(html.includes('scope="col">Select</th>'), `${name}: Select header`);
}
console.log("History comparison tests passed.");
