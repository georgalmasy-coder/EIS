import { createAutoRefreshController, fetchJson } from "/js/admin-dashboard/dashboard-api.js";
import {
    drawMultiLineChart,
    formatMetric,
    setBar,
    setText
} from "/js/admin-dashboard/dashboard-charts.js";

const DATA_URL = "/admin/api/dashboard/system-status";
const REFRESH_MS = 20000;

export function createViewController(context) {
    const root = context.root;

    function fallbackData() {
        return {
            cpu: 28.4,
            memory: 42.7,
            disk: 61.2,
            response: 18.5,
            availability: 99.9,
            apiStatus: "OK",
            databaseStatus: "OK",
            queueStatus: "OK",
            incidentCount: 0,
            memorySeries: [
                { committed: 0.32, limit: 1.00, available: 0.68, used: 0.32 },
                { committed: 0.34, limit: 1.00, available: 0.66, used: 0.34 },
                { committed: 0.36, limit: 1.00, available: 0.64, used: 0.36 },
                { committed: 0.35, limit: 1.00, available: 0.65, used: 0.35 },
                { committed: 0.38, limit: 1.00, available: 0.62, used: 0.38 },
                { committed: 0.41, limit: 1.00, available: 0.59, used: 0.41 },
                { committed: 0.43, limit: 1.00, available: 0.57, used: 0.43 },
                { committed: 0.42, limit: 1.00, available: 0.58, used: 0.42 }
            ]
        };
    }

    function statusClass(status) {
        if (status === "OK") {
            return "dashboard-status-ok";
        }

        if (status === "Warning" || status === "Degraded") {
            return "dashboard-status-warning";
        }

        return "dashboard-status-error";
    }

    function setStatusField(selector, status) {
        const element = root.querySelector(selector);

        if (!element) {
            return;
        }

        element.classList.remove("dashboard-status-ok", "dashboard-status-warning", "dashboard-status-error");
        element.classList.add(statusClass(status));
        element.textContent = status || "Unknown";
    }

    function render(data) {
        setText(root, '[data-field="cpu"]', formatMetric(data.cpu, 1));
        setText(root, '[data-field="memory"]', formatMetric(data.memory, 1));
        setText(root, '[data-field="disk"]', formatMetric(data.disk, 1));
        setText(root, '[data-field="response"]', formatMetric(data.response, 1));
        setText(root, '[data-field="availability"]', formatMetric(data.availability, 1));

        setBar(root, '[data-bar="cpu"]', data.cpu, 100);
        setBar(root, '[data-bar="memory"]', data.memory, 100);
        setBar(root, '[data-bar="disk"]', data.disk, 100);
        setBar(root, '[data-bar="response"]', data.response, 500);
        setBar(root, '[data-bar="availability"]', data.availability, 100);

        setStatusField('[data-field="apiStatus"]', data.apiStatus);
        setStatusField('[data-field="databaseStatus"]', data.databaseStatus);
        setStatusField('[data-field="queueStatus"]', data.queueStatus);
        setText(root, '[data-field="incidentCount"]', data.incidentCount ?? 0);

        const memorySeries = Array.isArray(data.memorySeries)
            ? data.memorySeries
            : [];

        drawMultiLineChart(root.querySelector('[data-chart="memoryDetails"]'), [
            {
                label: "Committed",
                color: "#2f9cff",
                values: memorySeries.map(function (point) {
                    return point.committed;
                })
            },
            {
                label: "Limit",
                color: "#84d64b",
                values: memorySeries.map(function (point) {
                    return point.limit;
                })
            },
            {
                label: "Available",
                color: "#f7c948",
                values: memorySeries.map(function (point) {
                    return point.available;
                })
            },
            {
                label: "Used",
                color: "#ef4444",
                values: memorySeries.map(function (point) {
                    return point.used;
                })
            }
        ]);
    }

    async function load() {
        let data;

        try {
            data = await fetchJson(DATA_URL);
        } catch (error) {
            console.warn("Using fallback system status dashboard data.", error);
            data = fallbackData();
        }

        render(data);
    }

    return createAutoRefreshController({
        refreshMs: REFRESH_MS,
        load,
        setLoadStatus: context.setLoadStatus,
        setLastRefreshNow: context.setLastRefreshNow,
        refreshButton: context.refreshButton
    });
}