const form = document.getElementById("resetForm");
const statusEl = document.getElementById("status");
const submitBtn = document.getElementById("submitBtn");

const tokenInput = document.getElementById("token");

document.addEventListener("DOMContentLoaded", initialize);

function initialize() {
    const url = new URL(window.location.href);
    const token = url.searchParams.get("token") || "";

    if (tokenInput) {
        tokenInput.value = token;
    }

    if (token) {
        validateToken(token);
    }

    form.addEventListener("submit", handleSubmit);
}

async function validateToken(token) {
    try {
        const response = await fetch(`/api/security/password-reset?token=${encodeURIComponent(token)}`, {
            headers: { Accept: "application/xml" },
            credentials: "same-origin"
        });

        const textValue = await response.text();
        const doc = parseXml(textValue);
        const valid = doc ? text(doc, "valid").toLowerCase() === "true" : false;

        if (!valid) {
            setStatus("The reset link is invalid or expired.", "error");
            submitBtn.disabled = true;
            return;
        }

        setStatus("Reset link validated.", "ok");
    } catch (error) {
        setStatus(`Could not validate reset link: ${error.message}`, "error");
        submitBtn.disabled = true;
    }
}

async function handleSubmit(event) {
    event.preventDefault();
    setStatus("", "");

    const token = tokenInput ? tokenInput.value.trim() : "";
    const newPassword = document.getElementById("newPassword").value;
    const confirmPassword = document.getElementById("confirmPassword").value;

    if (!token || !newPassword || !confirmPassword) {
        setStatus("Please fill out all fields.", "error");
        return;
    }

    submitBtn.disabled = true;

    try {
        const xml = `<?xml version="1.0" encoding="UTF-8"?><passwordReset><token>${escapeXml(token)}</token><newPassword>${escapeXml(newPassword)}</newPassword><confirmPassword>${escapeXml(confirmPassword)}</confirmPassword></passwordReset>`;

        const response = await fetch("/api/security/password-reset", {
            method: "POST",
            headers: {
                Accept: "application/xml",
                "Content-Type": "application/xml; charset=UTF-8"
            },
            body: xml,
            credentials: "same-origin"
        });

        const textValue = await response.text();
        const doc = parseXml(textValue);
        const success = doc ? text(doc, "success").toLowerCase() === "true" : false;
        const message = doc ? text(doc, "message") : "Password reset failed.";

        if (!response.ok || !success) {
            setStatus(message || "Password reset failed.", "error");
            return;
        }

        setStatus(message || "Password updated.", "ok");
        form.reset();
        if (tokenInput) {
            tokenInput.value = token;
        }
    } catch (error) {
        setStatus(`Password reset failed: ${error.message}`, "error");
    } finally {
        submitBtn.disabled = false;
    }
}

function parseXml(textValue) {
    const trimmed = String(textValue || "").trim();

    if (!trimmed) {
        return null;
    }

    const parser = new DOMParser();
    const doc = parser.parseFromString(trimmed, "application/xml");

    if (doc.querySelector("parsererror")) {
        return null;
    }

    return doc;
}

function text(node, selector) {
    const element = node ? node.querySelector(selector) : null;
    return element ? element.textContent || "" : "";
}

function setStatus(message, className) {
    if (!statusEl) {
        return;
    }

    statusEl.textContent = message || "";
    statusEl.className = `status ${className || ""}`.trim();
}

function escapeXml(value) {
    return String(value == null ? "" : value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&apos;");
}
