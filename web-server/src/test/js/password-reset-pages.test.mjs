import assert from "node:assert/strict";
import {readFile} from "node:fs/promises";
import vm from "node:vm";

const requestSource = await readFile(new URL("../../main/webapp/js/pages/forgot-password-page.js", import.meta.url), "utf8");
const resetSource = await readFile(new URL("../../main/webapp/js/pages/enter-new-password-page.js", import.meta.url), "utf8");
const flush = () => new Promise(resolve => setImmediate(resolve));

function page(source, href, replies = []) {
    const nodes = new Map();
    const calls = [];
    const redirects = [];
    const getNode = id => {
        if (!nodes.has(id)) nodes.set(id, {
            value: "", hidden: id !== "requestForm", disabled: id === "submitBtn" && source === resetSource,
            textContent: "", className: "", events: {},
            addEventListener(name, callback) { this.events[name] = callback; },
            reportValidity() { return true; }, focus() {},
            reset() { getNode("newPassword").value = ""; getNode("confirmPassword").value = ""; }
        });
        return nodes.get(id);
    };
    const replacements = [];
    class DOMParser {
        parseFromString(xml) {
            return {querySelector(tag) {
                const match = xml.match(new RegExp(`<${tag}>(.*?)</${tag}>`, "s"));
                return match ? {textContent: match[1]} : null;
            }};
        }
    }
    vm.runInNewContext(source, {
        document: {getElementById: getNode}, URL, DOMParser,
        window: {location: {href, replace: value => redirects.push(value)},
            history: {replaceState: (_state, _title, path) => replacements.push(path)}},
        fetch: async (url, options) => {
            calls.push({url, options});
            const reply = replies.shift();
            if (reply instanceof Error) throw reply;
            assert.ok(reply, "Unexpected HTTP request");
            return {ok: reply.ok ?? true, text: async () => reply.xml,
                json: async () => reply.json};
        }
    });
    return {getNode, calls, redirects, replacements,
        submit: id => getNode(id).events.submit({preventDefault() {}})};
}

let request = page(requestSource, "https://example.test/eis/forgot-password.html", [
    {json: {success: true, message: "Check your inbox."}}
]);
request.getNode("email").value = " person@example.test ";
await request.submit("requestForm");
assert.equal(request.calls[0].url, "api/security/password-reset-request");
assert.deepEqual(JSON.parse(request.calls[0].options.body), {email: "person@example.test"});
assert.equal(request.getNode("requestForm").hidden, true);
assert.equal(request.getNode("status").textContent, "Check your inbox.");

request = page(requestSource, "https://example.test/forgot-password.html", [new Error("Network unavailable")]);
await request.submit("requestForm");
assert.equal(request.getNode("submitBtn").disabled, false);
assert.equal(request.getNode("requestForm").hidden, false);
assert.equal(request.getNode("status").className, "status error");

request = page(requestSource, "https://example.test/forgot-password.html?token=old%2Blink");
assert.equal(request.redirects[0], "enter-new-password.html?token=old%2Blink");

let reset = page(resetSource, "https://example.test/enter-new-password.html");
await flush();
assert.equal(reset.calls.length, 0);
assert.equal(reset.getNode("resetForm").hidden, true);
assert.equal(reset.getNode("requestLink").hidden, false);

reset = page(resetSource, "https://example.test/enter-new-password.html?token=expired", [
    {xml: "<passwordReset><valid>false</valid></passwordReset>"}
]);
await flush();
assert.equal(reset.getNode("resetForm").hidden, true);
assert.equal(reset.getNode("submitBtn").disabled, true);
assert.equal(reset.replacements[0], "/enter-new-password.html");

reset = page(resetSource, "https://example.test/eis/enter-new-password.html?token=one-time-token", [
    {xml: "<passwordReset><valid>true</valid></passwordReset>"},
    {xml: "<passwordResetResult><success>true</success></passwordResetResult>"}
]);
await flush();
assert.equal(reset.getNode("resetForm").hidden, false);
reset.getNode("newPassword").value = "too-short";
reset.getNode("confirmPassword").value = "different";
await reset.submit("resetForm");
assert.equal(reset.calls.length, 1, "Mismatching passwords must not reach the server");
reset.getNode("newPassword").value = "short";
reset.getNode("confirmPassword").value = "short";
await reset.submit("resetForm");
assert.equal(reset.calls.length, 1, "Short passwords must not reach the server");
reset.getNode("newPassword").value = "  Æøå<&-password  ";
reset.getNode("confirmPassword").value = reset.getNode("newPassword").value;
await reset.submit("resetForm");
assert.ok(reset.calls[1].options.body.includes("<newPassword>  Æøå&lt;&amp;-password  </newPassword>"));
assert.equal(reset.getNode("resetForm").hidden, true);
assert.equal(reset.getNode("loginLink").hidden, false);
assert.equal(reset.getNode("newPassword").value, "");
assert.equal(reset.getNode("submitBtn").disabled, true);
await reset.submit("resetForm");
assert.equal(reset.calls.length, 2, "Saved tokens cannot be submitted again from the page");
assert.ok(reset.calls.every(call => !call.url.includes("api/login")), "Reset does not log the user in");

reset = page(resetSource, "https://example.test/enter-new-password.html?token=used", [
    {xml: "<passwordReset><valid>true</valid></passwordReset>"},
    {ok: false, xml: "<passwordResetResult><success>false</success><message>Link already used.</message></passwordResetResult>"}
]);
await flush();
reset.getNode("newPassword").value = "New-password-123";
reset.getNode("confirmPassword").value = reset.getNode("newPassword").value;
await reset.submit("resetForm");
assert.equal(reset.getNode("status").textContent, "Link already used.");
assert.equal(reset.getNode("requestLink").hidden, false);
assert.equal(reset.getNode("submitBtn").disabled, false);
console.log("Password reset page regression tests passed.");
