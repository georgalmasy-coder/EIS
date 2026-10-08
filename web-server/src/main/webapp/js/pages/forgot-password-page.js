const form = document.getElementById("requestForm");
const status = document.getElementById("status");
const submitBtn = document.getElementById("submitBtn");
const email = document.getElementById("email");

// Keep links from previously issued reset emails working.
const legacyToken = new URL(window.location.href).searchParams.get("token");
if (legacyToken) {
    window.location.replace(`enter-new-password.html?token=${encodeURIComponent(legacyToken)}`);
}

form.addEventListener("submit", async event => {
    event.preventDefault();
    if (!form.reportValidity()) return;
    submitBtn.disabled = true;
    submitBtn.textContent = "Sending…";
    status.textContent = "";
    status.className = "status";
    try {
        const response = await fetch("api/security/password-reset-request", {
            method: "POST", credentials: "same-origin",
            headers: {"Content-Type": "application/json", Accept: "application/json"},
            body: JSON.stringify({email: email.value.trim()})
        });
        const result = await response.json();
        if (!response.ok || result.success !== true) {
            throw new Error(result.message || "Could not request a reset link. Please try again.");
        }
        form.hidden = true;
        status.className = "status ok";
        status.textContent = result.message;
    } catch (error) {
        status.className = "status error";
        status.textContent = error.message || "Could not request a reset link. Please try again.";
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = "Send reset link";
    }
});
