import { createAutoRefreshController, fetchJson } from "/js/admin-dashboard/dashboard-api.js";
import {
    drawCpuLoadChart,
    drawMultiLineChart,
    formatMetric,
    setBar,
    setText
} from "/js/admin-dashboard/dashboard-charts.js";

const DATA_URL = "/admin/api/dashboard/system-status";
const REFRESH_MS = 10000;

export function createViewController(context) {
    const root = context.root;

    function fallbackData() {
        return {
            cpu: 0,
            memory: 0,
            disk: 0,
            response: 0,
            availability: 100,
            apiStatus: "OK",
            databaseStatus: "OK",
            queueStatus: "OK",
            incidentCount: 0,
            memorySeries: [],
            cpuLoadSeries: []
        };
    }

    function drawSystemCpuLoadChart(data) {
        const canvas = root.querySelector('[data-chart="cpuLoad8Hours"]');

        if (!canvas) {
            return;
        }

        drawCpuLoadChart(canvas, Array.isArray(data.cpuLoadSeries) ? data.cpuLoadSeries : [], {
            color: "#2f9cff",
            fillColor: "rgba(47, 156, 255, 0.22)",
            yLabel: "% Utilization",
            leftFooter: "8 hours ago",
            rightFooter: "Now"
        });
    }

    function drawMemoryChart(data) {
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

        drawMemoryChart(data);
        drawSystemCpuLoadChart(data);
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