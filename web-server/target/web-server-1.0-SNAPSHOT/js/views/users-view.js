import { createAutoRefreshController, fetchJson } from "/js/admin-dashboard/dashboard-api.js";
import {
    drawLineChart,
    renderDonut,
    renderLegend,
    setBar,
    setText
} from "/js/admin-dashboard/dashboard-charts.js";
import { formatDateForDisplay } from "/js/core/format.js";
import { escapeHtml } from "/js/core/html.js";

const DATA_URL = "/admin/api/dashboard/users";
const REFRESH_MS = 60000;

export function createViewController(context) {
    const root = context.root;

    function fallbackData() {
        return {
            activeUsers: 3842,
            loginsToday: 312,
            failedLogins: 18,
            lockedAccounts: 3,
            newUsers: 86,
            loginActivity: [44, 58, 73, 91, 120, 144, 168, 201, 244, 312],
            usersByRole: [
                { label: "Admin", value: 42, color: "#ef4444" },
                { label: "Editor", value: 620, color: "#f7c948" },
                { label: "User", value: 3180, color: "#84d64b" }
            ],
            loginHealth: [
                { label: "Successful", value: 312, color: "#84d64b" },
                { label: "Failed", value: 18, color: "#ef4444" }
            ],
            usersByCustomer: [
                { customer: "Nordic Systems A/S", users: 420 },
                { customer: "ACME GmbH", users: 315 },
                { customer: "Global Engineering Ltd.", users: 280 },
                { customer: "Energy Platform ApS", users: 170 },
                { customer: "Industrial Systems AB", users: 145 }
            ],
            inactiveUsers: [
                {
                    user: "anna@example.com",
                    customer: "Nordic Systems A/S",
                    role: "User",
                    lastLogin: "2026-04-02",
                    status: "Warning"
                },
                {
                    user: "peter@example.com",
                    customer: "ACME GmbH",
                    role: "Editor",
                    lastLogin: "2026-03-18",
                    status: "Warning"
                },
                {
                    user: "old-admin@example.com",
                    customer: "Global Engineering Ltd.",
                    role: "Admin",
                    lastLogin: "2026-02-12",
                    status: "Critical"
                }
            ]
        };
    }

    function renderUsersByCustomer(rows) {
        const list = root.querySelector('[data-list="usersByCustomer"]');

        if (!list) {
            return;
        }

        const safeRows = Array.isArray(rows) ? rows : [];

        list.innerHTML = safeRows.map(function (row) {
            return `
                <div class="dashboard-status-row">
                    <span class="dashboard-status-label">${escapeHtml(row.customer || "Unknown")}</span>
                    <strong class="dashboard-status-value">${escapeHtml(row.users ?? 0)}</strong>
                </div>
            `;
        }).join("");
    }

    function renderInactiveUsers(rows) {
        const body = root.querySelector('[data-table="inactiveUsers"]');

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
                    <td>${escapeHtml(row.user || "—")}</td>
                    <td>${escapeHtml(row.customer || "—")}</td>
                    <td>${escapeHtml(row.role || "—")}</td>
                    <td>${escapeHtml(formatDateForDisplay(row.lastLogin, "da-DK", row.lastLogin || "—"))}</td>
                    <td><span class="dashboard-pill ${statusClass}">${escapeHtml(status)}</span></td>
                </tr>
            `;
        }).join("");
    }

    function render(data) {
        setText(root, '[data-field="activeUsers"]', data.activeUsers ?? "—");
        setText(root, '[data-field="loginsToday"]', data.loginsToday ?? "—");
        setText(root, '[data-field="failedLogins"]', data.failedLogins ?? "—");
        setText(root, '[data-field="lockedAccounts"]', data.lockedAccounts ?? "—");
        setText(root, '[data-field="newUsers"]', data.newUsers ?? "—");

        setBar(root, '[data-bar="activeUsers"]', data.activeUsers, 5000);
        setBar(root, '[data-bar="loginsToday"]', data.loginsToday, 600);
        setBar(root, '[data-bar="failedLogins"]', data.failedLogins, 100);
        setBar(root, '[data-bar="lockedAccounts"]', data.lockedAccounts, 25);
        setBar(root, '[data-bar="newUsers"]', data.newUsers, 150);

        const usersByRole = Array.isArray(data.usersByRole)
            ? data.usersByRole
            : [];

        const loginHealth = Array.isArray(data.loginHealth)
            ? data.loginHealth
            : [];

        renderDonut(
            root.querySelector('[data-chart="usersByRole"]'),
            usersByRole,
            String(data.activeUsers ?? "—")
        );

        renderLegend(
            root.querySelector('[data-legend="usersByRole"]'),
            usersByRole
        );

        renderDonut(
            root.querySelector('[data-chart="loginHealth"]'),
            loginHealth,
            String(data.loginsToday ?? "—")
        );

        renderLegend(
            root.querySelector('[data-legend="loginHealth"]'),
            loginHealth
        );

        drawLineChart(
            root.querySelector('[data-chart="loginActivity"]'),
            data.loginActivity,
            { color: "#2f9cff" }
        );

        renderUsersByCustomer(data.usersByCustomer);
        renderInactiveUsers(data.inactiveUsers);
    }

    async function load() {
        let data;

        try {
            data = await fetchJson(DATA_URL);
        } catch (error) {
            console.warn("Using fallback users dashboard data.", error);
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