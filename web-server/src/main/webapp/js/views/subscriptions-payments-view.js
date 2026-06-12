import { createAutoRefreshController, fetchJson } from "/js/admin-dashboard/dashboard-api.js";
import {
    drawLineChart,
    formatMetric,
    renderDonut,
    renderLegend,
    setBar,
    setText
} from "/js/admin-dashboard/dashboard-charts.js";
import { formatInteger } from "/js/core/format.js";
import { escapeHtml } from "/js/core/html.js";

const DATA_URL = "/admin/api/dashboard/subscriptions-payments";
const REFRESH_MS = 90000;

export function createViewController(context) {
    const root = context.root;

    function fallbackData() {
        return {
            mrr: 184500,
            arr: 2214000,
            paymentErrors: 6,
            trialsExpiring: 9,
            churn: 1.8,
            mrrTrend: [130000, 138000, 145000, 151000, 160000, 166000, 172000, 178000, 181000, 184500],
            revenueByModule: [
                { label: "Basis", value: 114000, color: "#2f9cff" },
                { label: "Pro", value: 52000, color: "#8b5cf6" },
                { label: "Master", value: 18500, color: "#84d64b" }
            ],
            subscriptionStatus: [
                { label: "Active", value: 118, color: "#84d64b" },
                { label: "Trial", value: 8, color: "#2f9cff" },
                { label: "Suspended", value: 2, color: "#f7c948" },
                { label: "Cancelled", value: 1, color: "#ef4444" }
            ],
            paymentHealth: [
                { label: "Successful", value: 96, color: "#84d64b" },
                { label: "Failed", value: 4, color: "#ef4444" }
            ],
            failedPayments: [
                {
                    time: "11:21",
                    customer: "Global Engineering Ltd.",
                    amount: "4.900 kr.",
                    reason: "Card declined",
                    status: "Open"
                },
                {
                    time: "10:44",
                    customer: "ACME GmbH",
                    amount: "1.900 kr.",
                    reason: "Insufficient funds",
                    status: "Retrying"
                },
                {
                    time: "09:18",
                    customer: "Nordic Systems A/S",
                    amount: "990 kr.",
                    reason: "Expired card",
                    status: "Customer contacted"
                }
            ]
        };
    }

    function formatCurrency(value) {
        return formatInteger(value, "da-DK", "0");
    }

    function renderFailedPayments(rows) {
        const body = root.querySelector('[data-table="failedPayments"]');

        if (!body) {
            return;
        }

        const safeRows = Array.isArray(rows) ? rows : [];

        body.innerHTML = safeRows.map(function (row) {
            const status = row.status || "Unknown";
            const statusClass = status === "Customer contacted"
                ? "ok"
                : status === "Retrying"
                    ? "warning"
                    : "error";

            return `
                <tr>
                    <td>${escapeHtml(row.time || "—")}</td>
                    <td>${escapeHtml(row.customer || "—")}</td>
                    <td>${escapeHtml(row.amount || "—")}</td>
                    <td>${escapeHtml(row.reason || "—")}</td>
                    <td><span class="dashboard-pill ${statusClass}">${escapeHtml(status)}</span></td>
                </tr>
            `;
        }).join("");
    }

    function render(data) {
        setText(root, '[data-field="mrr"]', formatCurrency(data.mrr));
        setText(root, '[data-field="arr"]', formatCurrency(data.arr));
        setText(root, '[data-field="paymentErrors"]', data.paymentErrors ?? "—");
        setText(root, '[data-field="trialsExpiring"]', data.trialsExpiring ?? "—");
        setText(root, '[data-field="churn"]', formatMetric(data.churn, 1));

        setBar(root, '[data-bar="mrr"]', data.mrr, 250000);
        setBar(root, '[data-bar="arr"]', data.arr, 3000000);
        setBar(root, '[data-bar="paymentErrors"]', data.paymentErrors, 25);
        setBar(root, '[data-bar="trialsExpiring"]', data.trialsExpiring, 30);
        setBar(root, '[data-bar="churn"]', data.churn, 10);

        const revenueByModule = Array.isArray(data.revenueByModule)
            ? data.revenueByModule
            : [];

        const subscriptionStatus = Array.isArray(data.subscriptionStatus)
            ? data.subscriptionStatus
            : [];

        const paymentHealth = Array.isArray(data.paymentHealth)
            ? data.paymentHealth
            : [];

        renderDonut(
            root.querySelector('[data-chart="revenueByModule"]'),
            revenueByModule,
            "MRR"
        );

        renderLegend(
            root.querySelector('[data-legend="revenueByModule"]'),
            revenueByModule.map(function (item) {
                return {
                    label: item.label,
                    value: `${formatCurrency(item.value)} kr.`,
                    color: item.color
                };
            })
        );

        renderDonut(
            root.querySelector('[data-chart="subscriptionStatus"]'),
            subscriptionStatus,
            "Subs"
        );

        renderLegend(
            root.querySelector('[data-legend="subscriptionStatus"]'),
            subscriptionStatus
        );

        renderDonut(
            root.querySelector('[data-chart="paymentHealth"]'),
            paymentHealth,
            "Pay"
        );

        renderLegend(
            root.querySelector('[data-legend="paymentHealth"]'),
            paymentHealth
        );

        drawLineChart(
            root.querySelector('[data-chart="mrrTrend"]'),
            data.mrrTrend,
            { color: "#84d64b" }
        );

        renderFailedPayments(data.failedPayments);
    }

    async function load() {
        let data;

        try {
            data = await fetchJson(DATA_URL);
        } catch (error) {
            console.warn("Using fallback subscriptions and payments dashboard data.", error);
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