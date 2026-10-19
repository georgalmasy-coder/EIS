import assert from "node:assert/strict";
import {readFile} from "node:fs/promises";
import vm from "node:vm";

const source = await readFile(new URL("../../main/webapp/js/pages/customer-project-settings-page.js", import.meta.url), "utf8");
const nodes = new Map();
class Element {
    constructor(tag = "div") { this.tag = tag; this.children = []; this.events = {}; this.dataset = {}; this.value = ""; this.hidden = false; this.disabled = false; }
    set id(value) { this._id = value; nodes.set(value, this); }
    get id() { return this._id; }
    addEventListener(event, callback) { this.events[event] = callback; }
    append(...elements) { this.children.push(...elements); }
    add(option) { this.append(option); }
    replaceChildren() { this.children = []; }
    setAttribute(name, value) { this[name] = value; }
    removeAttribute(name) { delete this[name]; }
    querySelector(selector) {
        if (selector.startsWith(":invalid")) return null;
        return rows(this).find(row => !row.hidden) ?? null;
    }
}
function rows(element) { return element.children.flatMap(child => child.className === "setting-row" ? [child] : rows(child)); }
function node(id) { if (!nodes.has(id)) { const element = new Element(); element.id = id; } return nodes.get(id); }
const panels = ["security", "traceabilityMatching"].map(group => {
    const panel = new Element(); panel.dataset.panel = group; panel.append(node(`${group}Fields`)); return panel;
});
const categories = panels.map(panel => { const button = new Element("button"); button.dataset.category = panel.dataset.panel; return button; });
const pageEvents = {};
const windowEvents = {};
const requests = [];
const replies = [];
const definitions = [
    {key: "mfaPolicy", scope: "customer", group: "security", label: "MFA", type: "policy", defaultValue: "OPTIONAL", help: "Customer security"},
    {key: "fuzzyEnabled", scope: "project", group: "traceabilityMatching", label: "Fuzzy", type: "boolean", defaultValue: "true", help: "Similar words"},
    {key: "stopwords", scope: "project", group: "traceabilityMatching", label: "Stopwords", type: "text", defaultValue: "og,the", help: "Excluded words"}
];
const initial = {customerId: 1, projectId: 2, customerName: "Customer", projectName: "Project 2", definitions,
    values: {mfaPolicy: "REQUIRED", fuzzyEnabled: "false", stopwords: "custom"}};
replies.push({ok: true, payload: initial});
vm.runInNewContext(source.replace(/^import .*;$/gm, ""), {
    document: {
        getElementById: node,
        createElement: tag => new Element(tag),
        addEventListener: (event, callback) => { pageEvents[event] = callback; },
        querySelectorAll(selector) {
            if (selector === "[data-panel]") return panels;
            if (selector === "[data-category]") return categories;
            return panels.flatMap(rows);
        },
        querySelector() { return panels.flatMap(rows).find(row => !row.hidden) ?? null; }
    },
    window: {addEventListener: (event, callback) => { windowEvents[event] = callback; }},
    Option: class { constructor(text, value) { this.text = text; this.value = value; } },
    initMenu: async () => {}, applyTopbarMetadata() {}, getTopbarMetadata: () => ({}),
    URLSearchParams,
    fetch: async (url, options) => {
        requests.push({url, options});
        const reply = replies.shift(); assert.ok(reply, "Unexpected request");
        return {ok: reply.ok, json: async () => structuredClone(reply.payload)};
    }
});
await pageEvents.DOMContentLoaded();
assert.equal(node("saveSettings").disabled, true, "Initial values are clean");
assert.equal(node("setting-fuzzyEnabled").checked, false, "Saved false must not become true");

node("restoreDefaults").events.click();
assert.equal(node("setting-mfaPolicy").value, "OPTIONAL");
assert.equal(node("setting-stopwords").value, "custom", "Customer restore must preserve project draft");
node("setting-mfaPolicy").value = "REQUIRED";
node("settingsForm").events.input();
assert.equal(node("setting-mfaPolicy").value, "REQUIRED");
assert.equal(node("saveSettings").disabled, true);

categories[1].events.click();
node("restoreDefaults").events.click();
assert.equal(node("setting-fuzzyEnabled").checked, true);
assert.equal(node("setting-stopwords").value, "og,the");
assert.equal(node("setting-mfaPolicy").value, "REQUIRED", "Project restore must preserve customer draft");
node("setting-fuzzyEnabled").checked = false;
node("setting-stopwords").value = "";
node("settingsForm").events.input();
replies.push({ok: false, payload: {error: "The selected project changed"}});
await node("settingsForm").events.submit({preventDefault() {}});
const submitted = requests.at(-1).options.body;
assert.equal(submitted.get("customerId"), "1");
assert.equal(submitted.get("projectId"), "2");
assert.equal(submitted.get("fuzzyEnabled"), "false", "Unchecked boolean must be explicitly posted");
assert.equal(submitted.get("stopwords"), "", "Empty stopwords must be explicitly posted");
assert.equal(node("settingsStatus").textContent, "The selected project changed");
assert.equal(node("saveSettings").disabled, false, "Failed save retains editable draft");
assert.equal(node("setting-stopwords").value, "");

replies.push({ok: true, payload: {...initial, values: {...initial.values, stopwords: ""}}});
await node("settingsForm").events.submit({preventDefault() {}});
assert.equal(node("saveSettings").disabled, true, "Successful save establishes a clean snapshot");
assert.equal(node("settingsStatus").textContent, "Settings saved.");

replies.push({ok: true, payload: {...initial, projectId: null, projectName: ""}});
await node("retrySettings").events.click();
assert.equal(node("projectRequired").hidden, false);
assert.equal(node("restoreDefaults").disabled, true, "No project means no project defaults action");
categories[0].events.click();
node("restoreDefaults").events.click();
replies.push({ok: true, payload: {...initial, projectId: null, values: {...initial.values, mfaPolicy: "OPTIONAL"}}});
await node("settingsForm").events.submit({preventDefault() {}});
assert.equal(requests.at(-1).options.body.has("stopwords"), false, "Customer-only save must not submit project defaults");
assert.equal(requests.at(-1).options.body.get("projectId"), "");
console.log("customer-project-settings tests passed");
