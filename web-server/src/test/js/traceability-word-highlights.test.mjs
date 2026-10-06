import assert from "node:assert/strict";
import {readFile} from "node:fs/promises";

// Minimal DOM lets this renderer test run without browser dependencies.
class Node {
    constructor(tag = null, text = "") { this.tag = tag; this.text = text; this.children = []; }
    append(node) { this.children.push(node); }
    replaceChildren(fragment) { this.children = fragment.children; }
    set textContent(text) { this.text = text; this.children = []; }
    get textContent() { return this.text + this.children.map(node => node.textContent).join(""); }
}
globalThis.document = {
    createDocumentFragment: () => new Node(),
    createTextNode: text => new Node(null, text),
    createElement: tag => new Node(tag)
};
const source = await readFile(new URL("../../main/webapp/js/components/traceability-word-highlights.js", import.meta.url), "utf8");
const { highlightMatchingWords } = await import(`data:text/javascript;base64,${Buffer.from(source).toString("base64")}`);
const element = new Node();
const boldWords = () => element.children.filter(node => node.tag === "strong").map(node => node.textContent);

highlightMatchingWords(element, "Solar, systemkrav\nCooling cooling", ["solar", "cooling", "system"]);
assert.equal(element.textContent, "Solar, systemkrav\nCooling cooling");
assert.deepEqual(boldWords(), ["Solar", "Cooling", "cooling"]);
highlightMatchingWords(element, "KØLING cafe\u0301", ["køling", "café"]);
assert.deepEqual(boldWords(), ["KØLING", "cafe\u0301"]);
highlightMatchingWords(element, "<script>alert('system')</script>", ["system"]);
assert.equal(element.textContent, "<script>alert('system')</script>");
assert.deepEqual(boldWords(), ["system"]);
assert.ok(element.children.every(node => node.tag === null || node.tag === "strong"));
highlightMatchingWords(element, "Next requirement");
assert.deepEqual(boldWords(), []);
assert.equal(element.textContent, "Next requirement");
console.log("Traceability word highlight checks passed");
