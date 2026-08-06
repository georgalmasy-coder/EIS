import { createAutoRefreshController, fetchJson } from "/js/admin-dashboard/dashboard-api.js";
import {
    drawLineChart,
    formatMetric,
    renderDonut,
    renderLegend,
    setBar,
    setText
} from "/js/admin-dashboard/dashboard-charts.js";
import { escapeHtml } from "/js/core/html.js";

const DATA_URL = "/admin/api/dashboard/customer-creation";
const REFRESH_MS = 30000;

export function createViewController(context) {
    const root = context.root;

    function fallbackData() {
        return {
            started: 120,
            customerInfo: 92,
            payment: 74,
            confirmed: 68,
            activated: 58,
            pendingConfirmations: 5,
            cvrLookupsToday: 44,
            cvrSuccessRate: 88,
            paymentValidationRate: 91,
            failedCreations: 4,
            creationTrend: [4, 8, 6, 10, 12, 9, 14, 18, 16, 22],
            cvrLookup: [
                { label: "Success", value: 88, color: "#84d64b" },
                { label: "Failed", value: 12, color: "#ef4444" }
            ],
            failedCreationAttempts: [
                {
                    time: "11:18",
                    customer: "Global Engineering Ltd.",
                    step: "Payment",
                    error: "Card validation failed",
                    status: "Open"
                },
                {
                    time: "10:42",
                    customer: "Nordic Systems A/S",
                    step: "Email Confirmation",
                    error: "Confirmation email could not be sent",
                    status: "Warning"
                },
                {
                    time: "09:55",
                    customer: "Unknown",
                    step: "CVR Lookup",
                    error: "CVR lookup returned no company",
                    status: "Handled"
                }
            ]
        };
    }

    function renderFailedCreationAttempts(rows) {
        const body = root.querySelector('[data-table="failedCreationAttempts"]');

        if (!body) {
            return;
        }

        const safeRows = Array.isArray(rows) ? rows : [];

        body.innerHTML = safeRows.map(function (row) {
            const status = row.status || "Unknown";
            const statusClass = status === "Handled"
                ? "ok"
                : status === "Warning"
                    ? "warning"
                    : "error";

            return `
                <tr>
                    <td>${escapeHtml(row.time || "—")}</td>
                    <td>${escapeHtml(row.customer || "—")}</td>
                    <td>${escapeHtml(row.step || "—")}</td>
                    <td>${escapeHtml(row.error || "—")}</td>
                    <td><span class="dashboard-pill ${statusClass}">${escapeHtml(status)}</span></td>
                </tr>
            `;
        }).join("");
    }

    function render(data) {
        setText(root, '[data-field="started"]', data.started ?? "—");
        setText(root, '[data-field="customerInfo"]', data.customerInfo ?? "—");
        setText(root, '[data-field="payment"]', data.payment ?? "—");
        setText(root, '[data-field="confirmed"]', data.confirmed ?? "—");
        setText(root, '[data-field="activated"]', data.activated ?? "—");

        setText(root, '[data-field="pendingConfirmations"]', data.pendingConfirmations ?? "—");
        setText(root, '[data-field="cvrLookupsToday"]', data.cvrLookupsToday ?? "—");
        setText(root, '[data-field="cvrSuccessRate"]', formatMetric(data.cvrSuccessRate, 0));
        setText(root, '[data-field="paymentValidationRate"]', formatMetric(data.paymentValidationRate, 0));
        setText(root, '[data-field="failedCreations"]', data.failedCreations ?? "—");

        setBar(root, '[data-bar="pendingConfirmations"]', data.pendingConfirmations, 25);
        setBar(root, '[data-bar="cvrLookupsToday"]', data.cvrLookupsToday, 100);
        setBar(root, '[data-bar="cvrSuccessRate"]', data.cvrSuccessRate, 100);
        setBar(root, '[data-bar="paymentValidationRate"]', data.paymentValidationRate, 100);
        setBar(root, '[data-bar="failedCreations"]', data.failedCreations, 20);

        const cvrLookup = Array.isArray(data.cvrLookup)
            ? data.cvrLookup
            : [];

        renderDonut(
            root.querySelector('[data-chart="cvrLookup"]'),
            cvrLookup,
            `${formatMetric(data.cvrSuccessRate, 0)}%`
        );

        renderLegend(
            root.querySelector('[data-legend="cvrLookup"]'),
            cvrLookup
        );

        drawLineChart(
            root.querySelector('[data-chart="creationTrend"]'),
            data.creationTrend,
            { color: "#84d64b" }
        );

        renderFailedCreationAttempts(data.failedCreationAttempts);
    }

    async function load() {
        let data;

        try {
            data = await fetchJson(DATA_URL);
        } catch (error) {
            console.warn("Using fallback customer creation dashboard data.", error);
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