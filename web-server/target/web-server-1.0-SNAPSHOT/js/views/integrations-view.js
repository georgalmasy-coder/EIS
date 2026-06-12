import { createAutoRefreshController, fetchJson } from "/js/admin-dashboard/dashboard-api.js";
import {
    drawLineChart,
    renderDonut,
    renderLegend,
    setBar,
    setText
} from "/js/admin-dashboard/dashboard-charts.js";
import { escapeHtml } from "/js/core/html.js";

const DATA_URL = "/admin/api/dashboard/integrations";
const REFRESH_MS = 30000;

export function createViewController(context) {
    const root = context.root;

    function fallbackData() {
        return {
            virkStatus: "OK",
            emailStatus: "OK",
            paymentStatus: "Warning",
            ssoStatus: "OK",
            externalApiStatus: "OK",
            virkHealth: 98,
            emailHealth: 99,
            paymentHealthScore: 86,
            ssoHealth: 99,
            externalApiHealth: 96,
            latency: [110, 125, 118, 140, 135, 155, 149, 162, 151, 170],
            successFailure: [
                { label: "Success", value: 96, color: "#84d64b" },
                { label: "Failure", value: 4, color: "#ef4444" }
            ],
            integrationStatus: [
                { integration: "Virk.dk / CVR", status: "OK" },
                { integration: "Email Provider", status: "OK" },
                { integration: "Payment Provider", status: "Warning" },
                { integration: "SSO Provider", status: "OK" },
                { integration: "External API Gateway", status: "OK" }
            ],
            callVolume: [
                { integration: "Virk.dk / CVR", calls: 44 },
                { integration: "Email Provider", calls: 318 },
                { integration: "Payment Provider", calls: 86 },
                { integration: "SSO Provider", calls: 210 },
                { integration: "External API Gateway", calls: 128 }
            ],
            integrationEvents: [
                {
                    time: "11:48",
                    integration: "Virk.dk / CVR",
                    operation: "Company lookup",
                    latency: "142 ms",
                    status: "OK"
                },
                {
                    time: "11:42",
                    integration: "Payment Provider",
                    operation: "Payment validation",
                    latency: "618 ms",
                    status: "Warning"
                },
                {
                    time: "11:35",
                    integration: "Email Provider",
                    operation: "Send confirmation email",
                    latency: "233 ms",
                    status: "OK"
                }
            ]
        };
    }

    function renderIntegrationStatus(rows) {
        const list = root.querySelector('[data-list="integrationStatus"]');

        if (!list) {
            return;
        }

        const safeRows = Array.isArray(rows) ? rows : [];

        list.innerHTML = safeRows.map(function (row) {
            const status = row.status || "Unknown";
            const statusClass = status === "OK"
                ? "dashboard-status-ok"
                : status === "Warning"
                    ? "dashboard-status-warning"
                    : "dashboard-status-error";

            return `
                <div class="dashboard-status-row">
                    <span class="dashboard-status-label">${escapeHtml(row.integration || "Unknown")}</span>
                    <strong class="dashboard-status-value ${statusClass}">${escapeHtml(status)}</strong>
                </div>
            `;
        }).join("");
    }

    function renderCallVolume(rows) {
        const list = root.querySelector('[data-list="callVolume"]');

        if (!list) {
            return;
        }

        const safeRows = Array.isArray(rows) ? rows : [];

        list.innerHTML = safeRows.map(function (row) {
            return `
                <div class="dashboard-status-row">
                    <span class="dashboard-status-label">${escapeHtml(row.integration || "Unknown")}</span>
                    <strong class="dashboard-status-value">${escapeHtml(row.calls ?? 0)}</strong>
                </div>
            `;
        }).join("");
    }

    function renderIntegrationEvents(rows) {
        const body = root.querySelector('[data-table="integrationEvents"]');

        if (!body) {
            return;
        }

        const safeRows = Array.isArray(rows) ? rows : [];

        body.innerHTML = safeRows.map(function (row) {
            const status = row.status || "Unknown";
            const statusClass = status === "OK"
                ? "ok"
                : status === "Warning"
                    ? "warning"
                    : "error";

            return `
                <tr>
                    <td>${escapeHtml(row.time || "—")}</td>
                    <td>${escapeHtml(row.integration || "—")}</td>
                    <td>${escapeHtml(row.operation || "—")}</td>
                    <td>${escapeHtml(row.latency || "—")}</td>
                    <td><span class="dashboard-pill ${statusClass}">${escapeHtml(status)}</span></td>
                </tr>
            `;
        }).join("");
    }

    function render(data) {
        setText(root, '[data-field="virkStatus"]', data.virkStatus || "Unknown");
        setText(root, '[data-field="emailStatus"]', data.emailStatus || "Unknown");
        setText(root, '[data-field="paymentStatus"]', data.paymentStatus || "Unknown");
        setText(root, '[data-field="ssoStatus"]', data.ssoStatus || "Unknown");
        setText(root, '[data-field="externalApiStatus"]', data.externalApiStatus || "Unknown");

        setBar(root, '[data-bar="virkHealth"]', data.virkHealth, 100);
        setBar(root, '[data-bar="emailHealth"]', data.emailHealth, 100);
        setBar(root, '[data-bar="paymentHealthScore"]', data.paymentHealthScore, 100);
        setBar(root, '[data-bar="ssoHealth"]', data.ssoHealth, 100);
        setBar(root, '[data-bar="externalApiHealth"]', data.externalApiHealth, 100);

        const successFailure = Array.isArray(data.successFailure)
            ? data.successFailure
            : [];

        renderDonut(
            root.querySelector('[data-chart="successFailure"]'),
            successFailure,
            `${data.externalApiHealth ?? "—"}%`
        );

        renderLegend(
            root.querySelector('[data-legend="successFailure"]'),
            successFailure
        );

        drawLineChart(
            root.querySelector('[data-chart="latency"]'),
            data.latency,
            { color: "#22d3ee" }
        );

        renderIntegrationStatus(data.integrationStatus);
        renderCallVolume(data.callVolume);
        renderIntegrationEvents(data.integrationEvents);
    }

    async function load() {
        let data;

        try {
            data = await fetchJson(DATA_URL);
        } catch (error) {
            console.warn("Using fallback integrations dashboard data.", error);
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