const form = document.getElementById("loginForm");
const errorEl = document.getElementById("error");
const btn = document.getElementById("loginBtn");

form.addEventListener("submit", async (e) => {
    e.preventDefault();
    errorEl.textContent = "";

    const username = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value;

    if (!username || !password) {
        errorEl.textContent = "Please enter username and password.";
        return;
    }

    btn.disabled = true;
    try {
        const res = await fetch(`${window.location.origin}${window.location.pathname.replace(/\/[^/]*$/, "")}/api/login`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            credentials: "same-origin", // sikrer cookies (session) med i requests
            body: JSON.stringify({ username, password })
        });

        if (res.ok) {
            // Session-cookie er sat af serveren. Redirect til ny side.
            window.location.href = "select-project.html";
//GFA            window.location.href = "/api/projectselected";
            return;
        }

        if (res.status === 401) {
            try {
                const payload = await res.json();
                errorEl.textContent = payload?.message || "Invalid username or password";
            } catch {
                errorEl.textContent = "Invalid username or password";
            }
            return;
        }

        errorEl.textContent = "Unexpected error occurred. Please try again.";
    } catch {
        errorEl.textContent = "Network error. Check network connection and try again.";
    } finally {
        btn.disabled = false;
    }
});
