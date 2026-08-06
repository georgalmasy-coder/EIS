import { createAutoRefreshController, fetchJson } from "/js/admin-dashboard/dashboard-api.js";
import {
    drawLineChart,
    renderDonut,
    renderLegend,
    setBar,
    setText
} from "/js/admin-dashboard/dashboard-charts.js";
import { formatDateTimeForDisplay } from "/js/core/format.js";
import { escapeHtml } from "/js/core/html.js";
import { toNumber } from "/js/core/utils.js";

const DATA_URL = "/admin/api/dashboard/alerts";
const REFRESH_MS = 20000;
const ALERT_DASHBOARD_DAYS = 10;

export function createViewController(context) {
    const root = context.root;

    function fallbackData() {
        return {
            critical: 0,
            high: 0,
            medium: 0,
            failedEmails: 0,
            failedJobs: 0,
            alertsTrend: emptyTrend(),
            severity: [
                { label: "Critical", value: 0, color: "#ef4444" },
                { label: "High", value: 0, color: "#fb923c" },
                { label: "Medium", value: 0, color: "#f7c948" }
            ],
            alertHealth: [
                { label: "Open today", value: 0, color: "#ef4444" },
                { label: "Resolved today", value: 0, color: "#84d64b" }
            ],
            affectedServices: [],
            openIncidents: []
        };
    }

    function emptyTrend() {
        return Array.from({ length: ALERT_DASHBOARD_DAYS }, function () {
            return 0;
        });
    }

    function formatIncidentCount(count) {
        const safeCount = toNumber(count, 0);

        if (safeCount === 1) {
            return "1 error";
        }

        return `${safeCount} errors`;
    }

    function normalizeTrend(values) {
        const source = Array.isArray(values) ? values : [];
        const normalized = source.map(function (value) {
            return toNumber(value, 0);
        });

        if (normalized.length >= ALERT_DASHBOARD_DAYS) {
            return normalized.slice(normalized.length - ALERT_DASHBOARD_DAYS);
        }

        return [
            ...Array.from({ length: ALERT_DASHBOARD_DAYS - normalized.length }, function () {
                return 0;
            }),
            ...normalized
        ];
    }

    function renderAlertsTrendSubtitle(values) {
        const subtitle = root.querySelector('[data-field="alertsTrendSubtitle"]');

        if (!subtitle) {
            return;
        }

        const trend = normalizeTrend(values);
        const total = trend.reduce(function (sum, value) {
            return sum + value;
        }, 0);

        subtitle.textContent = `Incident trend for the last ${ALERT_DASHBOARD_DAYS} days: ${total} total`;
    }

    function renderAffectedServices(rows) {
        const list = root.querySelector('[data-list="affectedServices"]');

        if (!list) {
            return;
        }

        const safeRows = Array.isArray(rows) ? rows : [];

        if (safeRows.length === 0) {
            list.innerHTML = `
                <div class="dashboard-status-row">
                    <span class="dashboard-status-label">No affected services</span>
                    <strong class="dashboard-status-value dashboard-status-ok">0 errors</strong>
                </div>
            `;
            return;
        }

        list.innerHTML = safeRows.map(function (row) {
            return `
                <div class="dashboard-status-row">
                    <span class="dashboard-status-label">${escapeHtml(row.service || "Unknown")}</span>
                    <strong class="dashboard-status-value dashboard-status-error">${escapeHtml(formatIncidentCount(row.count))}</strong>
                </div>
            `;
        }).join("");
    }

    function renderOpenIncidents(rows) {
        const body = root.querySelector('[data-table="openIncidents"]');

        if (!body) {
            return;
        }

        const safeRows = Array.isArray(rows) ? rows : [];

        if (safeRows.length === 0) {
            body.innerHTML = `
                <tr>
                    <td colspan="8">No incidents found</td>
                </tr>
            `;
            return;
        }

        body.innerHTML = safeRows.map(function (row) {
            return `
                <tr>
                    <td>${escapeHtml(formatDateTimeForDisplay(row.logCreated, "da-DK", row.logCreated || "—"))}</td>
                    <td>${escapeHtml(row.customer || "")}</td>
                    <td>${escapeHtml(row.project || "")}</td>
                    <td>${escapeHtml(row.user || "")}</td>
                    <td>${escapeHtml(row.serviceType || "—")}</td>
                    <td>${escapeHtml(row.severityType || "—")}</td>
                    <td>${escapeHtml(row.module || "—")}</td>
                    <td>${escapeHtml(row.message || "—")}</td>
                </tr>
            `;
        }).join("");
    }

    function render(data) {
        const alertsTrend = normalizeTrend(data.alertsTrend);

        setText(root, '[data-field="critical"]', data.critical ?? "—");
        setText(root, '[data-field="high"]', data.high ?? "—");
        setText(root, '[data-field="medium"]', data.medium ?? "—");
        setText(root, '[data-field="failedEmails"]', data.failedEmails ?? "—");
        setText(root, '[data-field="failedJobs"]', data.failedJobs ?? "—");

        setBar(root, '[data-bar="critical"]', data.critical, 10);
        setBar(root, '[data-bar="high"]', data.high, 20);
        setBar(root, '[data-bar="medium"]', data.medium, 50);
        setBar(root, '[data-bar="failedEmails"]', data.failedEmails, 20);
        setBar(root, '[data-bar="failedJobs"]', data.failedJobs, 20);

        const severity = Array.isArray(data.severity)
            ? data.severity
            : [];

        const alertHealth = Array.isArray(data.alertHealth)
            ? data.alertHealth
            : [];

        renderDonut(
            root.querySelector('[data-chart="severity"]'),
            severity,
            String((data.critical ?? 0) + (data.high ?? 0) + (data.medium ?? 0))
        );

        renderLegend(
            root.querySelector('[data-legend="severity"]'),
            severity
        );

        renderDonut(
            root.querySelector('[data-chart="alertHealth"]'),
            alertHealth,
            "Today"
        );

        renderLegend(
            root.querySelector('[data-legend="alertHealth"]'),
            alertHealth
        );

        drawLineChart(
            root.querySelector('[data-chart="alertsTrend"]'),
            alertsTrend,
            {
                color: "#ef4444",
                startLabel: `${ALERT_DASHBOARD_DAYS} days ago`,
                endLabel: "Today"
            }
        );

        renderAlertsTrendSubtitle(alertsTrend);
        renderAffectedServices(data.affectedServices);
        renderOpenIncidents(data.openIncidents);
    }

    async function load() {
        let data;

        try {
            data = await fetchJson(DATA_URL);
        } catch (error) {
            console.warn("Using fallback alerts dashboard data.", error);
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