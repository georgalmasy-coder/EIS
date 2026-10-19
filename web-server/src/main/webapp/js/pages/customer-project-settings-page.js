import {getTopbarMetadata, initMenu} from "../components/menu.js";
import {applyTopbarMetadata} from "../components/topbar.js";

const byId = id => document.getElementById(id);
let settings = null;
let category = "security";
let busy = false;

function applicable() {
    return settings.definitions.filter(d => d.scope === "customer" || settings.projectId != null);
}

function readValue(definition) {
    const input = byId(`setting-${definition.key}`);
    return definition.type === "boolean" ? String(input.checked) : input.value;
}

function isDirty() {
    return settings && applicable().some(d => readValue(d) !== settings.values[d.key]);
}

function updateActions() {
    const dirty = !!isDirty();
    byId("settingsFields").disabled = busy || !settings;
    byId("saveSettings").disabled = busy || !dirty;
    byId("restoreDefaults").disabled = busy || !settings || (category === "traceabilityMatching" && settings.projectId == null);
    byId("unsavedChanges").hidden = !dirty;
}

function setValue(definition, value) {
    const input = byId(`setting-${definition.key}`);
    if (definition.type === "boolean") input.checked = value === "true";
    else input.value = value;
}

function status(message, error = false) {
    byId("settingsStatus").textContent = message;
    byId("settingsStatus").hidden = !message;
    byId("settingsStatus").dataset.error = String(error);
}

function render(payload) {
    settings = payload;
    byId("securityFields").replaceChildren();
    byId("traceabilityMatchingFields").replaceChildren();
    byId("settingsContext").textContent = `Customer: ${payload.customerName || payload.customerId} · Project: ${payload.projectId == null ? "No project selected" : (payload.projectName || payload.projectId)}`;
    byId("projectRequired").hidden = payload.projectId != null;
    for (const definition of applicable()) {
        const row = document.createElement("div");
        row.className = "setting-row";
        row.dataset.type = definition.type;
        row.dataset.search = `${definition.label} ${definition.help}`.toLowerCase();
        const label = document.createElement("label");
        label.htmlFor = `setting-${definition.key}`;
        label.textContent = definition.label;
        label.title = definition.help;
        const input = document.createElement(definition.type === "text" ? "textarea" : definition.type === "policy" ? "select" : "input");
        input.id = label.htmlFor;
        input.title = definition.help;
        input.setAttribute("aria-describedby", `help-${definition.key}`);
        if (definition.type === "policy") {
            for (const value of ["OPTIONAL", "REQUIRED", "DISABLED"]) input.add(new Option(value, value));
        } else if (definition.type === "integer") {
            input.type = "number"; input.min = definition.min; input.max = definition.max; input.step = "1"; input.required = true;
        } else if (definition.type === "boolean") input.type = "checkbox";
        else input.maxLength = 16000;
        const help = document.createElement("p");
        help.id = `help-${definition.key}`;
        help.className = "setting-help";
        help.textContent = definition.help;
        row.append(label, input, help);
        byId(`${definition.group}Fields`).append(row);
        setValue(definition, payload.values[definition.key]);
    }
    filter();
    updateActions();
}

function filter() {
    const query = byId("settingsSearch").value.trim().toLowerCase();
    for (const row of document.querySelectorAll(".setting-row")) row.hidden = !row.dataset.search.includes(query);
    // A search spans both scopes; selecting a category clears the search.
    for (const panel of document.querySelectorAll("[data-panel]")) panel.hidden = query ? !panel.querySelector(".setting-row:not([hidden])") : panel.dataset.panel !== category;
    byId("noSettingsMatch").hidden = !query || !!document.querySelector(".setting-row:not([hidden])");
}

async function responseJson(response) {
    if (response.redirected) throw new Error("Your session changed. Reload the page before editing settings.");
    const payload = await response.json();
    if (!response.ok) throw new Error(payload.error || "Could not load settings");
    return payload;
}

async function load() {
    busy = true; updateActions(); byId("retrySettings").hidden = true;
    try {
        render(await responseJson(await fetch("/api/customer-settings", {cache: "no-store"})));
        status("");
    } catch (error) { status(error.message, true); byId("retrySettings").hidden = false; }
    finally { busy = false; updateActions(); }
}

async function save(event) {
    event.preventDefault();
    if (busy || !settings) return;
    // Reveal invalid numeric fields even when they belong to another category.
    const invalid = byId("settingsForm").querySelector(":invalid:not(fieldset)");
    if (invalid) {
        category = invalid.closest("[data-panel]").dataset.panel;
        byId("settingsSearch").value = ""; filter(); invalid.reportValidity(); return;
    }
    const body = new URLSearchParams({customerId: settings.customerId, projectId: settings.projectId ?? ""});
    for (const definition of applicable()) body.set(definition.key, readValue(definition));
    busy = true; updateActions(); status("Saving settings...");
    try {
        render(await responseJson(await fetch("/api/customer-settings", {method: "POST", body})));
        status("Settings saved.");
    } catch (error) { status(error.message, true); }
    finally { busy = false; updateActions(); }
}

async function init() {
    byId("settingsForm").noValidate = true;
    byId("settingsForm").addEventListener("submit", save);
    byId("settingsForm").addEventListener("input", updateActions);
    byId("settingsForm").addEventListener("change", updateActions);
    byId("settingsSearch").addEventListener("input", filter);
    byId("retrySettings").addEventListener("click", load);
    byId("restoreDefaults").addEventListener("click", () => {
        for (const d of applicable().filter(d => d.group === category)) setValue(d, d.defaultValue);
        updateActions(); status("Defaults restored for this category. Save to keep the changes.");
    });
    for (const button of document.querySelectorAll("[data-category]")) button.addEventListener("click", () => {
        category = button.dataset.category;
        for (const other of document.querySelectorAll("[data-category]")) other.removeAttribute("aria-current");
        button.setAttribute("aria-current", "page");
        byId("settingsSearch").value = ""; filter(); updateActions();
    });
    window.addEventListener("beforeunload", event => { if (isDirty()) { event.preventDefault(); event.returnValue = ""; } });
    try { await initMenu(); applyTopbarMetadata(document, getTopbarMetadata()); }
    catch (error) { status(error.message, true); }
    await load();
}

document.addEventListener("DOMContentLoaded", init);
