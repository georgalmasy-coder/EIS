// Loaded by PageViewer before the page modules. Navigation timing includes HTML,
// scripts, server work, data requests and rendering. Only the initial view counts.
(() => {
    const page = new URL(location.href).searchParams.get("page");
    const clickKey = "eis.pagePerformance.navigation";
    let start = 0;
    try {
        const click = JSON.parse(sessionStorage.getItem(clickKey));
        sessionStorage.removeItem(clickKey);
        const navigation = performance.getEntriesByType("navigation")[0];
        if (click?.url === location.href && navigation?.type === "navigate"
            && click.time <= performance.timeOrigin && performance.timeOrigin - click.time < 60000) {
            start = click.time - performance.timeOrigin;
        }
    } catch { /* Storage can be disabled; navigation timing still works. */ }

    document.addEventListener("click", event => {
        const link = event.target.closest?.("a[href]");
        if (!link || event.defaultPrevented || event.button !== 0 || event.ctrlKey || event.metaKey
            || event.shiftKey || event.altKey || link.download || (link.target && link.target !== "_self")) return;
        const url = new URL(link.href, location.href);
        if (url.origin !== location.origin || url.pathname !== "/web/view") return;
        try {
            sessionStorage.setItem(clickKey, JSON.stringify({url: url.href, time: performance.timeOrigin + performance.now()}));
        } catch { /* Optional refinement of the navigation start time. */ }
    });

    if (!page || page.toLowerCase() === "admindashboard") return;
    let done = false;
    let scheduled = false;
    let explicitlyReady = false;
    const ready = () => explicitlyReady || /^(loaded\b|idle$)/i.test(document.getElementById("loadStatus")?.textContent.trim() || "");
    // Virtual matrices mark unloaded cells. Wait for the first visible data, not
    // just snapshot metadata. Subsequent scrolling cannot create another sample.
    const waitingForCells = () => document.querySelector(".traceability-cell.is-loading, .interfaces-cell-loading");

    function check() {
        if (done || scheduled || !ready() || waitingForCells()) return;
        scheduled = true;
        requestAnimationFrame(() => requestAnimationFrame(() => {
            scheduled = false;
            if (done || !ready() || waitingForCells()) return;
            done = true;
            observer.disconnect();
            const durationMs = Math.round(performance.now() - start);
            fetch("/api/performance/frontend", {
                method: "POST", credentials: "same-origin", keepalive: true,
                body: new URLSearchParams({page, durationMs: String(durationMs)})
            }).catch(() => {}); // Telemetry must not interrupt the user.
        }));
    }

    const observer = new MutationObserver(check);
    observer.observe(document.documentElement, {subtree: true, childList: true, characterData: true, attributes: true, attributeFilter: ["class"]});
    document.addEventListener("eis:initial-view-ready", () => { explicitlyReady = true; check(); }, {once: true});
    check();
})();
