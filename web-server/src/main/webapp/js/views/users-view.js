import {createAutoRefreshController, fetchJson} from "/js/admin-dashboard/dashboard-api.js";
import {drawLineChart, renderDonut, renderLegend, setBar, setText} from "/js/admin-dashboard/dashboard-charts.js";
import {formatDateForDisplay} from "/js/core/format.js";
import {escapeHtml} from "/js/core/html.js";

const DATA_URL = "/admin/api/dashboard/users";
const REFRESH_MS = 60000;

export function createViewController(context) {
    const root = context.root;

    function renderUsersByCustomer(rows) {
        const list = root.querySelector('[data-list="usersByCustomer"]');

        if (!list) {
            return;
        }

        const safeRows = Array.isArray(rows) ? rows : [];

        if (safeRows.length === 0) {
            list.textContent = "No users linked to customer accounts.";
            return;
        }

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

        if (safeRows.length === 0) {
            body.innerHTML = '<tr><td colspan="5">No users requiring review.</td></tr>';
            return;
        }

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
                    <td>${escapeHtml(row.lastLogin ? formatDateForDisplay(row.lastLogin, "da-DK", row.lastLogin) : "Never")}</td>
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
            String(usersByRole.reduce((total, role) => total + Number(role.value || 0), 0))
        );

        renderLegend(
            root.querySelector('[data-legend="usersByRole"]'),
            usersByRole
        );

        renderDonut(
            root.querySelector('[data-chart="loginHealth"]'),
            loginHealth,
            String(loginHealth.reduce((total, item) => total + Number(item.value || 0), 0))
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
        const data = await fetchJson(DATA_URL);
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
