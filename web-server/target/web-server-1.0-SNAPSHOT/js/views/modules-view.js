import { createAutoRefreshController, fetchJson } from "/js/admin-dashboard/dashboard-api.js";
import {
    drawLineChart,
    renderDonut,
    renderLegend,
    setBar,
    setText
} from "/js/admin-dashboard/dashboard-charts.js";
import { escapeHtml } from "/js/core/html.js";

const DATA_URL = "/admin/api/dashboard/modules";
const REFRESH_MS = 180000;

export function createViewController(context) {
    const root = context.root;

    function fallbackData() {
        return {
            basisCustomers: 104,
            proCustomers: 21,
            masterCustomers: 3,
            upgrades: 6,
            downgrades: 1,
            moduleAdoption: [82, 88, 91, 96, 102, 108, 114, 119, 124, 128],
            moduleDistribution: [
                { label: "Basis", value: 104, color: "#2f9cff" },
                { label: "Pro", value: 21, color: "#8b5cf6" },
                { label: "Master", value: 3, color: "#84d64b" }
            ],
            featureUsage: [
                { feature: "Requirements Management", usage: 118 },
                { feature: "System Breakdown", usage: 94 },
                { feature: "Attachments", usage: 72 },
                { feature: "Notes", usage: 63 },
                { feature: "Export", usage: 41 }
            ],
            moduleStatus: [
                { module: "Basis", status: "Available" },
                { module: "Pro", status: "Coming Soon" },
                { module: "Master", status: "Coming Soon" }
            ],
            recentModuleChanges: [
                {
                    time: "11:32",
                    customer: "Nordic Systems A/S",
                    change: "Upgrade requested",
                    from: "Basis",
                    to: "Pro",
                    status: "Pending"
                },
                {
                    time: "10:18",
                    customer: "ACME GmbH",
                    change: "Module enabled",
                    from: "None",
                    to: "Basis",
                    status: "OK"
                },
                {
                    time: "09:44",
                    customer: "Global Engineering Ltd.",
                    change: "Downgrade requested",
                    from: "Pro",
                    to: "Basis",
                    status: "Review"
                }
            ]
        };
    }

    function renderFeatureUsage(rows) {
        const list = root.querySelector('[data-list="featureUsage"]');

        if (!list) {
            return;
        }

        const safeRows = Array.isArray(rows) ? rows : [];

        list.innerHTML = safeRows.map(function (row) {
            return `
                <div class="dashboard-status-row">
                    <span class="dashboard-status-label">${escapeHtml(row.feature || "Unknown")}</span>
                    <strong class="dashboard-status-value">${escapeHtml(row.usage ?? 0)}</strong>
                </div>
            `;
        }).join("");
    }

    function renderModuleStatus(rows) {
        const list = root.querySelector('[data-list="moduleStatus"]');

        if (!list) {
            return;
        }

        const safeRows = Array.isArray(rows) ? rows : [];

        list.innerHTML = safeRows.map(function (row) {
            const status = row.status || "Unknown";
            const statusClass = status === "Available"
                ? "dashboard-status-ok"
                : status === "Coming Soon"
                    ? "dashboard-status-warning"
                    : "dashboard-status-error";

            return `
                <div class="dashboard-status-row">
                    <span class="dashboard-status-label">${escapeHtml(row.module || "Unknown")}</span>
                    <strong class="dashboard-status-value ${statusClass}">${escapeHtml(status)}</strong>
                </div>
            `;
        }).join("");
    }

    function renderRecentModuleChanges(rows) {
        const body = root.querySelector('[data-table="recentModuleChanges"]');

        if (!body) {
            return;
        }

        const safeRows = Array.isArray(rows) ? rows : [];

        body.innerHTML = safeRows.map(function (row) {
            const status = row.status || "Unknown";
            const statusClass = status === "OK"
                ? "ok"
                : status === "Pending" || status === "Review"
                    ? "warning"
                    : "error";

            return `
                <tr>
                    <td>${escapeHtml(row.time || "—")}</td>
                    <td>${escapeHtml(row.customer || "—")}</td>
                    <td>${escapeHtml(row.change || "—")}</td>
                    <td>${escapeHtml(row.from || "—")}</td>
                    <td>${escapeHtml(row.to || "—")}</td>
                    <td><span class="dashboard-pill ${statusClass}">${escapeHtml(status)}</span></td>
                </tr>
            `;
        }).join("");
    }

    function render(data) {
        setText(root, '[data-field="basisCustomers"]', data.basisCustomers ?? "—");
        setText(root, '[data-field="proCustomers"]', data.proCustomers ?? "—");
        setText(root, '[data-field="masterCustomers"]', data.masterCustomers ?? "—");
        setText(root, '[data-field="upgrades"]', data.upgrades ?? "—");
        setText(root, '[data-field="downgrades"]', data.downgrades ?? "—");

        setBar(root, '[data-bar="basisCustomers"]', data.basisCustomers, 150);
        setBar(root, '[data-bar="proCustomers"]', data.proCustomers, 100);
        setBar(root, '[data-bar="masterCustomers"]', data.masterCustomers, 50);
        setBar(root, '[data-bar="upgrades"]', data.upgrades, 25);
        setBar(root, '[data-bar="downgrades"]', data.downgrades, 25);

        const moduleDistribution = Array.isArray(data.moduleDistribution)
            ? data.moduleDistribution
            : [];

        renderDonut(
            root.querySelector('[data-chart="moduleDistribution"]'),
            moduleDistribution,
            String(
                (data.basisCustomers ?? 0)
                + (data.proCustomers ?? 0)
                + (data.masterCustomers ?? 0)
            )
        );

        renderLegend(
            root.querySelector('[data-legend="moduleDistribution"]'),
            moduleDistribution
        );

        drawLineChart(
            root.querySelector('[data-chart="moduleAdoption"]'),
            data.moduleAdoption,
            { color: "#8b5cf6" }
        );

        renderFeatureUsage(data.featureUsage);
        renderModuleStatus(data.moduleStatus);
        renderRecentModuleChanges(data.recentModuleChanges);
    }

    async function load() {
        let data;

        try {
            data = await fetchJson(DATA_URL);
        } catch (error) {
            console.warn("Using fallback modules dashboard data.", error);
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