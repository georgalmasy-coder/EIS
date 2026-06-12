import { fetchJson as fetchJsonFromCore } from "../core/http.js";

export async function fetchJson(url) {
    return await fetchJsonFromCore(url, {
        credentials: "same-origin"
    });
}

export function createAutoRefreshController(options) {
    const refreshMs = options.refreshMs || 30000;
    const load = options.load;
    const setLoadStatus = options.setLoadStatus || function () {};
    const setLastRefreshNow = options.setLastRefreshNow || function () {};
    const refreshButton = options.refreshButton || null;

    let timer = null;
    let stopped = false;
    let loading = false;

    async function refresh() {
        if (loading || stopped) {
            return;
        }

        loading = true;

        if (refreshButton) {
            refreshButton.disabled = true;
        }

        setLoadStatus("Loading…");

        try {
            await load();
            setLastRefreshNow();
            setLoadStatus("Loaded");
        } catch (error) {
            console.error(error);
            setLoadStatus("Error");
        } finally {
            loading = false;

            if (refreshButton) {
                refreshButton.disabled = false;
            }
        }
    }

    function start() {
        stopped = false;
        refresh();

        timer = window.setInterval(function () {
            if (!document.hidden) {
                refresh();
            }
        }, refreshMs);
    }

    function stop() {
        stopped = true;

        if (timer) {
            window.clearInterval(timer);
            timer = null;
        }
    }

    return {
        start,
        stop,
        refresh
    };
}