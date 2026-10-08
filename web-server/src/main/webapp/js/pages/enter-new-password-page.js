const form = document.getElementById("resetForm");
const status = document.getElementById("status");
const submitBtn = document.getElementById("submitBtn");
const newPassword = document.getElementById("newPassword");
const confirmPassword = document.getElementById("confirmPassword");
const loginLink = document.getElementById("loginLink");
const requestLink = document.getElementById("requestLink");
const url = new URL(window.location.href);
const token = url.searchParams.get("token") || "";
// Do not retain the bearer token in the address bar or subsequent referrers.
if (token) {
    url.searchParams.delete("token");
    window.history.replaceState(null, "", url.pathname + url.search + url.hash);
}
let ready = false;
let saved = false;

function setStatus(message, kind = "") {
    status.textContent = message;
    status.className = `status ${kind}`.trim();
}

function parseXml(value) {
    const doc = new DOMParser().parseFromString(value, "application/xml");
    if (doc.querySelector("parsererror")) throw new Error("Could not read the server response. Please try again.");
    return doc;
}

function xmlText(doc, selector) {
    return doc.querySelector(selector)?.textContent || "";
}

function escapeXml(value) {
    return value.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;").replaceAll("'", "&apos;");
}

async function initialize() {
    if (!token) {
        setStatus("Open the reset link from your email, or request a new link.", "error");
        requestLink.hidden = false;
        return;
    }
    try {
        const response = await fetch(`api/security/password-reset?token=${encodeURIComponent(token)}`, {
            headers: {Accept: "application/xml"}, credentials: "same-origin", cache: "no-store"
        });
        const doc = parseXml(await response.text());
        if (!response.ok || xmlText(doc, "valid") !== "true") {
            throw new Error("This reset link is invalid, expired or already used. Please request a new link.");
        }
        ready = true;
        form.hidden = false;
        submitBtn.disabled = false;
        setStatus("");
        newPassword.focus();
    } catch (error) {
        setStatus(error.message || "Could not validate the reset link. Please try again.", "error");
        requestLink.hidden = false;
    }
}

form.addEventListener("submit", async event => {
    event.preventDefault();
    if (!ready || saved || submitBtn.disabled) return;
    if (!form.reportValidity()) return;
    if (newPassword.value.length < 8 || newPassword.value.length > 128 || !newPassword.value.trim()) {
        setStatus("Password must contain 8–128 characters and cannot consist only of spaces.", "error");
        return;
    }
    if (newPassword.value !== confirmPassword.value) {
        setStatus("Passwords do not match.", "error");
        confirmPassword.focus();
        return;
    }
    submitBtn.disabled = true;
    submitBtn.textContent = "Saving…";
    setStatus("");
    try {
        const body = `<?xml version="1.0" encoding="UTF-8"?><passwordReset><token>${escapeXml(token)}</token><newPassword>${escapeXml(newPassword.value)}</newPassword><confirmPassword>${escapeXml(confirmPassword.value)}</confirmPassword></passwordReset>`;
        const response = await fetch("api/security/password-reset", {
            method: "POST", credentials: "same-origin",
            headers: {Accept: "application/xml", "Content-Type": "application/xml; charset=UTF-8"}, body
        });
        const doc = parseXml(await response.text());
        if (!response.ok || xmlText(doc, "success") !== "true") {
            throw new Error(xmlText(doc, "message") || "Password reset failed. Please request a new link.");
        }
        saved = true;
        ready = false;
        form.reset();
        form.hidden = true;
        setStatus("Your password has been updated. Log in with your new password to continue.", "ok");
        loginLink.hidden = false;
        loginLink.focus();
    } catch (error) {
        setStatus(error.message || "Could not save your password. Please try again.", "error");
        requestLink.hidden = false;
    } finally {
        submitBtn.disabled = saved;
        submitBtn.textContent = "Save new password";
    }
});

initialize();
