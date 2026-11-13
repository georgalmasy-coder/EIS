import assert from "node:assert/strict";
import {readFile} from "node:fs/promises";
import vm from "node:vm";

const source = (await readFile(new URL("../../main/webapp/js/components/edit-interfaces-table.js", import.meta.url), "utf8"))
    .replace(/^import[^\n]+;\r?\n/gm, "")
    .replaceAll("export function", "function");

// The entry module must receive PageViewer's build version too. A fixed ?rev on
// the entry module otherwise keeps importing the old component URL from cache.
const pageViewer = await readFile(new URL("../../main/java/com/bepa/eis/server/api/web/application/views/PageViewer.java", import.meta.url), "utf8");
assert.ok(pageViewer.includes('html.replace(".js\\\"></script>", ".js?v=" + version'));
for (const name of ["systemsbreakdown", "systemrequirement", "stakeholderrequirement", "logicalstructure", "functionalstructure"]) {
    const html = await readFile(new URL(`../../main/resources/html-pages/${name}-edit.html`, import.meta.url), "utf8");
    const page = await readFile(new URL(`../../main/webapp/js/pages/${name}-edit-page.js`, import.meta.url), "utf8");
    const versionedHtml = html.replaceAll('.js"></script>', '.js?v=test-build"></script>');
    assert.ok(versionedHtml.includes(`${name}-edit-page.js?v=test-build`), `${name}: entry module must receive build version`);
    assert.ok(page.includes('edit-interfaces-table.js?rev=interface-refresh-2026-11-13'), `${name}: current component import`);
    for (const id of ["interfacesBody", "interfacesEmpty", "interfacesCount", "rightInterfacesBody", "rightInterfacesEmpty", "rightInterfacesCount", "addInterfaceToButton", "addInterfaceFromButton"]) {
        assert.ok(html.includes(`id="${id}"`), `${name}: missing table element ${id}`);
    }
}

class Node {
    constructor(tagName) {
        this.tagName = tagName;
        this.children = [];
        this.dataset = {};
        this.style = {setProperty() {}};
        this.events = {};
        this.textContent = "";
    }
    appendChild(node) { this.children.push(node); node.parent = this; return node; }
    replaceChildren(...nodes) {
        this.children = [];
        for (const node of nodes) {
            if (node.tagName === "fragment") node.children.forEach(child => this.appendChild(child));
            else this.appendChild(node);
        }
    }
    insertBefore(node) { this.appendChild(node); }
    querySelector(tag) { return this.children.find(node => node.tagName === tag) || null; }
    closest(tag) { return this.tagName === tag ? this : this.parent?.closest(tag); }
    addEventListener(name, callback) { this.events[name] = callback; }
}

const nodes = new Map();
for (const prefix of ["interfaces", "rightInterfaces"]) {
    const table = new Node("table");
    table.appendChild(new Node("thead"));
    const body = table.appendChild(new Node("tbody"));
    nodes.set(`${prefix}Body`, body);
    nodes.set(`${prefix}Empty`, new Node("div"));
    nodes.set(`${prefix}Count`, new Node("span"));
}
nodes.set("addInterfaceToButton", new Node("button"));
nodes.set("addInterfaceFromButton", new Node("button"));
let opened;
let refreshRequested;
const sandbox = {
    document: {
        getElementById: id => nodes.get(id),
        createElement: tag => new Node(tag),
        createDocumentFragment: () => new Node("fragment"),
        createTextNode: text => Object.assign(new Node("text"), {textContent: text})
    },
    getAttribute: (node, name, fallback) => node?.attributes?.[name] ?? fallback,
    getChildText: (node, name, fallback) => node?.fields?.[name] ?? fallback,
    resolveInterfaceBasePath: type => `/interfaces/${type}`,
    requestEditDialogRefresh: payload => { refreshRequested = payload; },
    openInterfaceEditDialog: options => { opened = options; }
};
vm.createContext(sandbox);
vm.runInContext(source, sandbox);
const component = sandbox.createEditInterfacesTable({onSaved() {}});
const left = {fields: {
    fromEntityId: "10", toEntityId: "20", toEntityCode: "B", toEntityName: "Receiver",
    fromIrlId: "7", fromClassificationIds: "AA", nextIrlMeeting: "2026-10-13", fromTrlId: "4", toTrlId: "8"
}};
const right = {fields: {
    fromEntityId: "30", fromEntityCode: "C", fromEntityName: "Sender C", toEntityId: "10",
    fromIrlId: "2", fromClassificationIds: "BB", nextIrlMeeting: "2026-11-14", fromTrlId: "6", toTrlId: "4"
}};

function load(type, leftRows = [left], rightRows = [right], readOnly = false) {
    const root = {
        attributes: {entityId: "10", entityType: type, readOnly: String(readOnly)},
        querySelectorAll(selector) {
            if (selector === ":scope > entities > entity") return ["10", "20", "30", "40"].map(id => ({fields: {entityId: id, id, name: "Entity " + id}}));
            if (selector === ":scope > leftInterfaces > interface") return leftRows;
            if (selector === ":scope > rightInterfaces > interface") return rightRows;
            return [];
        }
    };
    const detail = {
        fields: {EntityId: "10", SBSCode: "A", SystemName: "Current", StakeholderReqCode: "A",
            SystemReqCode: "A", RequirementName: "Current", LogicalCode: "A", LogicalName: "Current",
            FunctionalCode: "A", FunctionalName: "Current"},
        querySelector: () => true
    };
    const doc = {querySelector: selector => selector === "editInterfaces" ? root : detail};
    component.loadFromDocument(doc);
    return doc;
}

function doubleClick(row) {
    row.events.dblclick({preventDefault() {}, stopPropagation() {}});
}

for (const type of ["2", "5", "6", "8", "9"]) {
    const doc = load(type);
    const comparison = sandbox.getInterfaceComparisonTables(doc);
    assert.equal(comparison.length, 2);
    assert.equal(comparison[0].columns.length, type === "2" ? 7 : 5);
    assert.equal(comparison[1].columns[0].label, "ID");
    assert.equal(comparison[1].rows[0].key, "30:10");
    assert.equal(comparison[0].rows[0].key, "10:20");
    const leftBody = nodes.get("interfacesBody");
    const rightBody = nodes.get("rightInterfacesBody");
    assert.equal(leftBody.children.length, 1);
    assert.equal(rightBody.children.length, 1);
    assert.equal(leftBody.children[0].children.length, type === "2" ? 7 : 5);
    assert.equal(rightBody.children[0].children.length, type === "2" ? 7 : 5);
    assert.equal(rightBody.children[0].children[0].children[0].textContent, "C");
    assert.equal(rightBody.children[0].children[1].children[0].textContent, "Sender C");
    assert.equal(nodes.get("interfacesCount").textContent, "1");
    assert.equal(nodes.get("rightInterfacesCount").textContent, "1");
    doubleClick(leftBody.children[0]);
    assert.equal(opened.fromStructure.entityId, "10");
    assert.equal(opened.toStructure.entityId, "20");
    assert.equal(opened.cell.irlId, "7");
    doubleClick(rightBody.children[0]);
    assert.equal(opened.fromStructure.entityId, "30");
    assert.equal(opened.fromStructure.id, "C");
    assert.equal(opened.toStructure.entityId, "10");
    assert.equal(opened.toStructure.id, "A");
    assert.equal(opened.cell.irlId, "2");
    assert.equal(opened.cell.classificationIds.join(","), "BB");
    assert.equal(opened.cell.nextIrlMeeting, "2026-11-14");
    opened.onSaved({fromEntityId: "30", toEntityId: "10"});
    assert.equal(refreshRequested.kind, "interface");
    assert.equal(refreshRequested.basePath, '/interfaces/' + type);
    assert.equal(refreshRequested.fromEntityId, "30");
    assert.equal(refreshRequested.toEntityId, "10");
    const incomingHeaders = nodes.get("rightInterfacesBody").closest("table").querySelector("thead").children[0];
    assert.equal(incomingHeaders.children[0].textContent, "From");
    nodes.get("addInterfaceToButton").onclick();
    assert.equal(opened.selectableEndpoint, "to");
    assert.equal(opened.fromStructure.entityId, "10");
    assert.equal(opened.toStructure, null);
    assert.equal(opened.structures.map(item => item.entityId).join(","), "30,40");
    opened.onSaved({fromEntityId: "10", toEntityId: "40"});
    assert.equal(refreshRequested.fromEntityId, "10");
    assert.equal(refreshRequested.toEntityId, "40");
    nodes.get("addInterfaceFromButton").onclick();
    assert.equal(opened.selectableEndpoint, "from");
    assert.equal(opened.toStructure.entityId, "10");
    assert.equal(opened.fromStructure, null);
    assert.equal(opened.structures.map(item => item.entityId).join(","), "20,40");
}
load("2", [], [right]);
assert.equal(nodes.get("interfacesEmpty").hidden, false);
assert.equal(nodes.get("rightInterfacesEmpty").hidden, true);
load("2", [], []);
assert.equal(nodes.get("rightInterfacesCount").textContent, "0");
assert.equal(nodes.get("rightInterfacesBody").children.length, 0);
assert.equal(nodes.get("rightInterfacesEmpty").hidden, false);
component.loadFromDocument(null);
assert.equal(nodes.get("interfacesBody").children.length, 0);
load("2", [left], [right, {...right, fields: {...right.fields, toEntityId: "40"}},
    {...right, fields: {...right.fields, toEntityId: "50"}}]);
assert.equal(nodes.get("interfacesCount").textContent, "1");
assert.equal(nodes.get("rightInterfacesCount").textContent, "3");
load("2", [left], [right], true);
assert.equal(nodes.get("addInterfaceToButton").hidden, true);
assert.equal(nodes.get("addInterfaceFromButton").hidden, true);
opened = null;
doubleClick(nodes.get("rightInterfacesBody").children[0]);
assert.equal(opened, null);
load("2", [left, {...left, fields: {...left.fields, toEntityId: "30"}}, {...left, fields: {...left.fields, toEntityId: "40"}}], []);
assert.equal(nodes.get("addInterfaceToButton").disabled, true);
assert.equal(nodes.get("addInterfaceFromButton").disabled, false);
console.log("Interface direction, editing and creation tests passed for all five entity types.");
