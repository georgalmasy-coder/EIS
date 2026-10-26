import {createAutoRefreshController, fetchJson} from "/js/admin-dashboard/dashboard-api.js";
import {
    drawLineChart,
    formatMetric,
    renderDonut,
    renderLegend,
    setBar,
    setText
} from "/js/admin-dashboard/dashboard-charts.js";
import {formatDateTimeForDisplay} from "/js/core/format.js";
import {escapeHtml} from "/js/core/html.js";

const DATA_URL = "/admin/api/dashboard/overview";
const REFRESH_MS = 20000;

export function createViewController(context) {
    const root = context.root;

    function renderLatestEvents(rows) {
        const body = root.querySelector('[data-table="latestEvents"]');

        if (!body) {
            return;
        }

        const safeRows = Array.isArray(rows) ? rows : [];

        body.innerHTML = safeRows.map(function (row) {
            return `
                <tr>
                    <td>${escapeHtml(formatDisplayTime(row.time))}</td>
                    <td>${escapeHtml(row.type || "—")}</td>
                    <td>${escapeHtml(row.description || "—")}</td>
                    <td>${escapeHtml(row.project || "—")}</td>
                    <td>${escapeHtml(formatDisplayTime(row.processedAt))}</td>
                </tr>
            `;
        }).join("");
    }

    function renderServiceStatus(rows) {
        const body = root.querySelector('[data-table="serviceStatus"]');
        if (!body) {
            return;
        }
        const safeRows = Array.isArray(rows) ? rows : [];
        body.innerHTML = safeRows.map(function (row) {
            const status = row.status || "Unknown";
            const statusClass = status === "OK"
                ? "dashboard-status-ok"
                : status === "Warning"
                    ? "dashboard-status-warning"
                    : status === "Unknown" ? "" : "dashboard-status-error";
            return `
                <tr>
                    <td>${escapeHtml(row.label || "Unknown")}</td>
                    <td><strong class="${statusClass}">${escapeHtml(status)}</strong></td>
                    <td>${escapeHtml(formatDisplayTime(row.lastObservedAt))}</td>
                    <td>${escapeHtml(row.description || "—")}</td>
                </tr>
            `;
        }).join("");
    }
    function render(data) {
        setText(root, '[data-field="onlineUsers"]', data.onlineUsers ?? "—");
        setText(root, '[data-field="otherOnlineUsers"]', `Other users (excluding you): ${data.otherOnlineUsers ?? "—"}`);
        setText(root, '[data-field="onlineWindow"]', `Server activity within the last ${data.sessionTimeoutMinutes ?? "—"} minutes (including background requests)`);
        const components = Array.isArray(data.healthComponents) ? data.healthComponents : [];
        setText(root, '[data-field="activeCustomers"]', data.activeCustomers ?? "—");
        setText(root, '[data-field="activeUsers"]', data.activeUsers ?? "—");
        setText(root, '[data-field="criticalAlerts"]', data.criticalAlerts ?? "—");
        setText(root, '[data-field="paymentErrors"]', data.paymentErrors ?? "—");
        setText(root, '[data-field="pendingCustomerConfirmations"]', data.pendingCustomerConfirmations ?? "—");
        setText(root, '[data-field="retryingEmails"]', data.retryingEmails ?? data.failedEmails ?? "—");
        setText(root, '[data-field="undeliveredEmails"]', data.undeliveredEmails ?? "—");
        setText(root, '[data-field="failedIntegrations"]', data.failedIntegrations ?? "—");
        setText(root, '[data-field="lockedUsers"]', data.lockedUsers ?? "—");

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
            data.systemHealth == null ? "—" : `${formatMetric(data.systemHealth, 0)}%`
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
        renderServiceStatus(components);
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
        render(await fetchJson(DATA_URL));
    }

    return createAutoRefreshController({
        refreshMs: REFRESH_MS,
        load,
        setLoadStatus: context.setLoadStatus,
        setLastRefreshNow: context.setLastRefreshNow,
        refreshButton: context.refreshButton
    });
}
