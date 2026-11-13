// Classic script installed by PageViewer before any page modules, once per window/iframe.
(() => {
    if (window.eisClientIncidents) return;
    const script = document.currentScript;
    const build = new URL(script.src, location.href).searchParams.get("v") || "";
    const seen = new Map();
    let remaining = 20;
    let reporting = false;
    let gesture = "";
    const clip = (value, limit) => String(value ?? "").slice(0, limit);

    // Keep navigation context, but do not collect arbitrary query parameters or form values.
    function pageUrl(value) {
        const url = new URL(value, location.href);
        const safe = new URL(url.pathname, url.origin);
        for (const key of ["page", "mode", "id", "version", "modal", "dialogId"]) {
            if (url.searchParams.has(key)) safe.searchParams.set(key, url.searchParams.get(key));
        }
        return clip(safe.href, 1000);
    }

    function report(error, details = {}) {
        if (reporting || !remaining) return;
        reporting = true;
        try {
            const message = clip(details.message || error?.message || error || "Unknown client error", 1000);
            const stack = clip(error?.stack || "", 16000);
            const file = details.file ? pageUrl(details.file) : "";
            const key = [message, stack, file, details.line, details.column].join("|");
            const now = Date.now();
            if (seen.has(key) && now - seen.get(key) < 60_000) return;
            seen.set(key, now);
            remaining--;
            let parentUrl = "";
            try { if (window.parent !== window) parentUrl = pageUrl(window.parent.location.href); } catch { /* Cross-origin host. */ }
            const params = new URLSearchParams(location.search);
            const payload = {
                kind: clip(details.kind || "reported", 100), message, stack, file,
                line: clip(details.line, 10), column: clip(details.column, 10),
                page: params.get("page") || "", url: pageUrl(location.href), parentUrl,
                dialogId: clip(params.get("dialogId"), 100), build,
                userAgent: clip(navigator.userAgent, 1000), time: new Date().toISOString(), gesture
            };
            // Both synchronous transport failures and rejected requests must remain silent.
            void fetch("/api/incidents/client", {
                method: "POST", credentials: "same-origin", keepalive: true,
                headers: {"Content-Type": "application/json"}, body: JSON.stringify(payload)
            }).catch(() => {});
        } catch { /* Reporting must never interfere with the page or report itself. */ }
        finally { reporting = false; }
    }

    window.eisClientIncidents = {report};
    window.addEventListener("error", event => {
        // Resource loading errors do not carry a JavaScript exception.
        if (!event.message && !event.error) return;
        report(event.error, {kind: "error", message: event.message, file: event.filename,
            line: event.lineno, column: event.colno});
    });
    window.addEventListener("unhandledrejection", event => report(event.reason, {kind: "unhandledrejection"}));
    document.addEventListener("pointerdown", event => {
        let target = event.target;
        while (target?.nodeType === 1) {
            if (target.classList.contains("eis-dialog-resize")) { gesture = "resize"; return; }
            if (target.style.cursor === "move") { gesture = "drag"; return; }
            target = target.parentElement;
        }
    }, true);
    for (const event of ["pointerup", "pointercancel", "lostpointercapture"]) {
        document.addEventListener(event, () => { gesture = ""; }, true);
    }
})();
