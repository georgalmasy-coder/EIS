import { createAutoRefreshController, fetchJson } from "/js/admin-dashboard/dashboard-api.js";
import {
    drawLineChart,
    formatMetric,
    renderDonut,
    renderLegend,
    setBar,
    setText
} from "/js/admin-dashboard/dashboard-charts.js";
import { formatDateTimeForDisplay } from "/js/core/format.js";
import { escapeHtml } from "/js/core/html.js";

const DATA_URL = "/admin/api/dashboard/overview";
const REFRESH_MS = 20000;

export function createViewController(context) {
    const root = context.root;

    function fallbackData() {
        return {
            systemHealth: 98,
            activeCustomers: 128,
            activeUsers: 3842,
            criticalAlerts: 1,
            paymentErrors: 3,
            pendingCustomerConfirmations: 5,
            retryingEmails: 0,
            undeliveredEmails: 3,
            failedIntegrations: 0,
            lockedUsers: 3,
            activityTrend: [40, 44, 48, 52, 49, 55, 62, 68, 65, 72, 78, 83],
            healthDistribution: [
                { label: "Healthy", value: 92, color: "#84d64b" },
                { label: "Warning", value: 6, color: "#f7c948" },
                { label: "Critical", value: 2, color: "#ef4444" }
            ],
            latestEvents: [
                {
                    time: "2026-05-24T11:42:00",
                    type: "Customer",
                    description: "New customer created",
                    project: "1",
                    status: "Ok"
                },
                {
                    time: "2026-05-24T11:21:00",
                    type: "Payment",
                    description: "Payment retry required",
                    project: "1",
                    status: "Awating"
                },
                {
                    time: "2026-05-24T10:58:00",
                    type: "System",
                    description: "Dashboard API refreshed",
                    project: "1",
                    status: "Ok"
                }
            ],
            serviceStatus: [
                { label: "API", status: "OK" },
                { label: "Database", status: "OK" },
                { label: "Queue", status: "OK" },
                { label: "Email", status: "Critical" }
            ]
        };
    }

    function renderLatestEvents(rows) {
        const body = root.querySelector('[data-table="latestEvents"]');

        if (!body) {
            return;
        }

        const safeRows = Array.isArray(rows) ? rows : [];

        body.innerHTML = safeRows.map(function (row) {
            const status = row.status || "Unknown";
            const statusClass = status === "Ok" || status === "OK"
                ? "ok"
                : status === "Warning" || status === "Awating"
                    ? "warning"
                    : "error";

            return `
                <tr>
                    <td>${escapeHtml(formatDisplayTime(row.time))}</td>
                    <td>${escapeHtml(row.type || "—")}</td>
                    <td>${escapeHtml(row.description || "—")}</td>
                    <td>${escapeHtml(row.project || "—")}</td>
                    <td><span class="dashboard-pill ${statusClass}">${escapeHtml(status)}</span></td>
                </tr>
            `;
        }).join("");
    }

    function renderServiceStatus(rows) {
        const list = root.querySelector('[data-list="serviceStatus"]');

        if (!list) {
            return;
        }

        const safeRows = Array.isArray(rows) ? rows : [];

        list.innerHTML = safeRows.map(function (row) {
            const statusClass = row.status === "OK"
                ? "dashboard-status-ok"
                : row.status === "Warning"
                    ? "dashboard-status-warning"
                    : "dashboard-status-error";

            return `
                <div class="dashboard-status-row">
                    <span class="dashboard-status-label">${escapeHtml(row.label || "Unknown")}</span>
                    <strong class="dashboard-status-value ${statusClass}">${escapeHtml(row.status || "Unknown")}</strong>
                </div>
            `;
        }).join("");
    }

    function render(data) {
        setText(root, '[data-field="systemHealth"]', formatMetric(data.systemHealth, 0));
        setText(root, '[data-field="activeCustomers"]', data.activeCustomers ?? "—");
        setText(root, '[data-field="activeUsers"]', data.activeUsers ?? "—");
        setText(root, '[data-field="criticalAlerts"]', data.criticalAlerts ?? "—");
        setText(root, '[data-field="paymentErrors"]', data.paymentErrors ?? "—");
        setText(root, '[data-field="pendingCustomerConfirmations"]', data.pendingCustomerConfirmations ?? "—");
        setText(root, '[data-field="retryingEmails"]', data.retryingEmails ?? data.failedEmails ?? "—");
        setText(root, '[data-field="undeliveredEmails"]', data.undeliveredEmails ?? "—");
        setText(root, '[data-field="failedIntegrations"]', data.failedIntegrations ?? "—");
        setText(root, '[data-field="lockedUsers"]', data.lockedUsers ?? "—");

        setBar(root, '[data-bar="systemHealth"]', data.systemHealth, 100);
        setBar(root, '[data-bar="activeCustomers"]', data.activeCustomers, 250);
        setBar(root, '[data-bar="activeUsers"]', data.activeUsers, 5000);
        setBar(root, '[data-bar="criticalAlerts"]', data.criticalAlerts, 10);
        setBar(root, '[data-bar="paymentErrors"]', data.paymentErrors, 20);

        const healthDistribution = Array.isArray(data.healthDistribution)
            ? data.healthDistribution
            : [];

        renderDonut(
            root.querySelector('[data-chart="healthDonut"]'),
            healthDistribution,
            `${formatMetric(data.systemHealth, 0)}%`
        );

        renderLegend(
            root.querySelector('[data-legend="healthLegend"]'),
            healthDistribution
        );

        drawLineChart(
            root.querySelector('[data-chart="activityTrend"]'),
            data.activityTrend,
            { color: "#2f9cff" }
        );

        renderLatestEvents(data.latestEvents);
        renderServiceStatus(data.serviceStatus);
    }

    function formatDisplayTime(value) {
        if (!value) {
            return "—";
        }

        if (typeof value === "string" && /^\d{2}:\d{2}$/.test(value)) {
            return value;
        }

        return formatDateTimeForDisplay(value, "en-GB", String(value));
    }

    async function load() {
        let data;

        try {
            data = await fetchJson(DATA_URL);
        } catch (error) {
            console.warn("Using fallback overview dashboard data.", error);
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