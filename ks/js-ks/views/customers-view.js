import { createAutoRefreshController, fetchJson } from "/js/admin-dashboard/dashboard-api.js";
import {
    drawLineChart,
    renderDonut,
    renderLegend,
    setBar,
    setText
} from "/js/admin-dashboard/dashboard-charts.js";
import { escapeHtml } from "/js/core/html.js";

const DATA_URL = "/admin/api/dashboard/customers";
const REFRESH_MS = 60000;

export function createViewController(context) {
    const root = context.root;

    function fallbackData() {
        return {
            activeCustomers: 128,
            newCustomersThisMonth: 14,
            pendingCustomers: 5,
            suspendedCustomers: 2,
            customersWithIssues: 7,
            customerGrowth: [82, 88, 91, 97, 102, 108, 113, 119, 123, 128],
            customersByModule: [
                { label: "Basis", value: 104, color: "#2f9cff" },
                { label: "Pro", value: 21, color: "#8b5cf6" },
                { label: "Master", value: 3, color: "#84d64b" }
            ],
            customerHealth: [
                { label: "Healthy", value: 110, color: "#84d64b" },
                { label: "Warning", value: 13, color: "#f7c948" },
                { label: "Critical", value: 5, color: "#ef4444" }
            ],
            customersByCountry: [
                { country: "Denmark", customers: 82 },
                { country: "Sweden", customers: 14 },
                { country: "Germany", customers: 11 },
                { country: "United Kingdom", customers: 9 },
                { country: "United States", customers: 6 }
            ],
            customersWithProblems: [
                {
                    customer: "Nordic Systems A/S",
                    country: "Denmark",
                    module: "Basis",
                    status: "Warning",
                    issue: "Email confirmation missing"
                },
                {
                    customer: "Global Engineering Ltd.",
                    country: "United Kingdom",
                    module: "Pro",
                    status: "Critical",
                    issue: "Payment failed"
                },
                {
                    customer: "ACME GmbH",
                    country: "Germany",
                    module: "Basis",
                    status: "Warning",
                    issue: "Admin verification required"
                }
            ]
        };
    }

    function renderCustomersByCountry(rows) {
        const list = root.querySelector('[data-list="customersByCountry"]');

        if (!list) {
            return;
        }

        const safeRows = Array.isArray(rows) ? rows : [];

        list.innerHTML = safeRows.map(function (row) {
            return `
                <div class="dashboard-status-row">
                    <span class="dashboard-status-label">${escapeHtml(row.country || "Unknown")}</span>
                    <strong class="dashboard-status-value">${escapeHtml(row.customers ?? 0)}</strong>
                </div>
            `;
        }).join("");
    }

    function renderCustomersWithProblems(rows) {
        const body = root.querySelector('[data-table="customersWithProblems"]');

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
                    <td>${escapeHtml(row.customer || "—")}</td>
                    <td>${escapeHtml(row.country || "—")}</td>
                    <td>${escapeHtml(row.module || "—")}</td>
                    <td><span class="dashboard-pill ${statusClass}">${escapeHtml(status)}</span></td>
                    <td>${escapeHtml(row.issue || "—")}</td>
                </tr>
            `;
        }).join("");
    }

    function render(data) {
        setText(root, '[data-field="activeCustomers"]', data.activeCustomers ?? "—");
        setText(root, '[data-field="newCustomersThisMonth"]', data.newCustomersThisMonth ?? "—");
        setText(root, '[data-field="pendingCustomers"]', data.pendingCustomers ?? "—");
        setText(root, '[data-field="suspendedCustomers"]', data.suspendedCustomers ?? "—");
        setText(root, '[data-field="customersWithIssues"]', data.customersWithIssues ?? "—");

        setBar(root, '[data-bar="activeCustomers"]', data.activeCustomers, 250);
        setBar(root, '[data-bar="newCustomersThisMonth"]', data.newCustomersThisMonth, 50);
        setBar(root, '[data-bar="pendingCustomers"]', data.pendingCustomers, 25);
        setBar(root, '[data-bar="suspendedCustomers"]', data.suspendedCustomers, 20);
        setBar(root, '[data-bar="customersWithIssues"]', data.customersWithIssues, 25);

        const customersByModule = Array.isArray(data.customersByModule)
            ? data.customersByModule
            : [];

        const customerHealth = Array.isArray(data.customerHealth)
            ? data.customerHealth
            : [];

        renderDonut(
            root.querySelector('[data-chart="customersByModule"]'),
            customersByModule,
            String(data.activeCustomers ?? "—")
        );

        renderLegend(
            root.querySelector('[data-legend="customersByModule"]'),
            customersByModule
        );

        renderDonut(
            root.querySelector('[data-chart="customerHealth"]'),
            customerHealth,
            String(data.activeCustomers ?? "—")
        );

        renderLegend(
            root.querySelector('[data-legend="customerHealth"]'),
            customerHealth
        );

        drawLineChart(
            root.querySelector('[data-chart="customerGrowth"]'),
            data.customerGrowth,
            { color: "#84d64b" }
        );

        renderCustomersByCountry(data.customersByCountry);
        renderCustomersWithProblems(data.customersWithProblems);
    }

    async function load() {
        let data;

        try {
            data = await fetchJson(DATA_URL);
        } catch (error) {
            console.warn("Using fallback customers dashboard data.", error);
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