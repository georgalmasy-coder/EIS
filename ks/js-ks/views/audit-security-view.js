import { createAutoRefreshController, fetchJson } from "/js/admin-dashboard/dashboard-api.js";
import {
    drawHourlyLoginActivityChart,
    drawLineChart,
    formatMetric,
    renderDonut,
    renderLegend,
    setBar,
    setText
} from "/js/admin-dashboard/dashboard-charts.js";
import { formatDateTimeForDisplay } from "/js/core/format.js";
import { escapeHtml } from "/js/core/html.js";
import { toNumber } from "/js/core/utils.js";

const DATA_URL = "/admin/api/dashboard/audit-security";
const REFRESH_MS = 45000;

export function createViewController(context) {
    const root = context.root;

    function fallbackData() {
        return {
            securityScore: 91,
            failedLogins: 74,
            lockedUsers: 3,
            mfaCoverage: 86,
            adminChanges: 4,
            newUsers: 12,
            activeUsers: 312,
            loginsToday: 128,
            failedLoginTrend: [12, 16, 14, 20, 24, 31, 28, 36, 42, 74],
            hourlyLoginActivity7Days: buildFallbackHourlyLoginActivity(),
            loginsByCountry: [
                {
                    countryCode: "DK",
                    countryName: "Denmark",
                    successful: 320,
                    failed: 18,
                    total: 338
                },
                {
                    countryCode: "SE",
                    countryName: "Sweden",
                    successful: 84,
                    failed: 6,
                    total: 90
                },
                {
                    countryCode: "DE",
                    countryName: "Germany",
                    successful: 44,
                    failed: 7,
                    total: 51
                },
                {
                    countryCode: "??",
                    countryName: "Unknown",
                    successful: 12,
                    failed: 11,
                    total: 23
                }
            ],
            loginHealth: [
                { label: "Successful", value: 448, color: "#84d64b" },
                { label: "Failed", value: 42, color: "#ef4444" }
            ],
            mfaCoverageDistribution: [
                { label: "MFA enabled", value: 86, color: "#84d64b" },
                { label: "Missing MFA", value: 14, color: "#ef4444" }
            ],
            securityEventTypes: [
                { label: "Failed login", value: 74, color: "#ef4444" },
                { label: "Permission change", value: 4, color: "#f7c948" },
                { label: "Locked account", value: 3, color: "#8b5cf6" },
                { label: "New user", value: 12, color: "#2f9cff" }
            ],
            securityStatus: [
                { label: "Password policy", status: "OK" },
                { label: "MFA coverage", status: "Warning" },
                { label: "Admin users", status: "OK" },
                { label: "Suspicious activity", status: "Warning" }
            ],
            recentLogins: [
                {
                    time: "2026-05-23T11:52:00",
                    email: "admin@bepa.dk",
                    ipAddress: "87.54.12.10",
                    countryName: "Denmark",
                    success: true,
                    status: "OK"
                },
                {
                    time: "2026-05-23T11:47:00",
                    email: "unknown@example.com",
                    ipAddress: "18.194.21.4",
                    countryName: "Germany",
                    success: false,
                    status: "Invalid password"
                },
                {
                    time: "2026-05-23T11:31:00",
                    email: "peter@example.com",
                    ipAddress: "192.168.1.20",
                    countryName: "Private network",
                    success: true,
                    status: "OK"
                }
            ],
            auditEvents: [
                {
                    time: "11:52",
                    user: "admin@bepa.dk",
                    action: "Changed user role",
                    object: "user: peter@example.com",
                    status: "OK"
                },
                {
                    time: "11:33",
                    user: "system",
                    action: "Locked user account",
                    object: "user: unknown@example.com",
                    status: "Warning"
                },
                {
                    time: "10:47",
                    user: "security@bepa.dk",
                    action: "Created admin user",
                    object: "user: new-admin@example.com",
                    status: "Review"
                }
            ]
        };
    }

    function buildFallbackHourlyLoginActivity() {
        const points = [];

        for (let index = 0; index < 168; index++) {
            const hour = new Date();
            hour.setHours(hour.getHours() - (167 - index), 0, 0, 0);

            const dailyPattern = Math.max(0, Math.sin((index % 24) / 24 * Math.PI) * 20);
            const successful = Math.round(4 + dailyPattern + Math.random() * 8);
            const failed = Math.round(Math.random() * 4);

            points.push({
                hour: hour.toISOString(),
                successful,
                failed,
                total: successful + failed
            });
        }

        return points;
    }

    function renderSecurityStatus(rows) {
        const list = root.querySelector('[data-list="securityStatus"]');

        if (!list) {
            return;
        }

        const safeRows = Array.isArray(rows) ? rows : [];

        list.innerHTML = safeRows.map(function (row) {
            const status = row.status || "Unknown";
            const statusClass = status === "OK"
                ? "dashboard-status-ok"
                : status === "Warning" || status === "Review"
                    ? "dashboard-status-warning"
                    : "dashboard-status-error";

            return `
                <div class="dashboard-status-row">
                    <span class="dashboard-status-label">${escapeHtml(row.label || "Unknown")}</span>
                    <strong class="dashboard-status-value ${statusClass}">${escapeHtml(status)}</strong>
                </div>
            `;
        }).join("");
    }

    function renderAuditEvents(rows) {
        const body = root.querySelector('[data-table="auditEvents"]');

        if (!body) {
            return;
        }

        const safeRows = Array.isArray(rows) ? rows : [];

        body.innerHTML = safeRows.map(function (row) {
            const status = row.status || "Unknown";
            const statusClass = getPillClass(status);

            return `
                <tr>
                    <td>${escapeHtml(formatDisplayTime(row.time))}</td>
                    <td>${escapeHtml(row.user || "—")}</td>
                    <td>${escapeHtml(row.action || "—")}</td>
                    <td>${escapeHtml(row.object || "—")}</td>
                    <td><span class="dashboard-pill ${statusClass}">${escapeHtml(status)}</span></td>
                </tr>
            `;
        }).join("");
    }

    function renderLoginsByCountry(rows) {
        const body = root.querySelector('[data-table="loginsByCountry"]');

        if (!body) {
            return;
        }

        const safeRows = Array.isArray(rows) ? rows : [];

        body.innerHTML = safeRows.map(function (row) {
            const successful = toNumber(row.successful, 0);
            const failed = toNumber(row.failed, 0);
            const total = toNumber(row.total, 0);
            const failurePercent = total > 0
                ? ((failed / total) * 100).toFixed(1)
                : "0.0";

            return `
                <tr>
                    <td>${escapeHtml(row.countryName || "Unknown")}</td>
                    <td>${escapeHtml(successful)}</td>
                    <td>${escapeHtml(failed)}</td>
                    <td>${escapeHtml(total)}</td>
                    <td>${escapeHtml(failurePercent)}%</td>
                </tr>
            `;
        }).join("");
    }

    function renderRecentLogins(rows) {
        const body = root.querySelector('[data-table="recentLogins"]');

        if (!body) {
            return;
        }

        const safeRows = Array.isArray(rows) ? rows : [];

        body.innerHTML = safeRows.map(function (row) {
            const success = Boolean(row.success);
            const status = row.status || (success ? "OK" : "Failed");
            const statusClass = success ? "ok" : "error";

            return `
                <tr>
                    <td>${escapeHtml(formatDisplayTime(row.time))}</td>
                    <td>${escapeHtml(row.email || "—")}</td>
                    <td>${escapeHtml(row.countryName || "Unknown")}</td>
                    <td>${escapeHtml(row.ipAddress || "—")}</td>
                    <td><span class="dashboard-pill ${statusClass}">${success ? "OK" : "Failed"}</span></td>
                    <td>${escapeHtml(status)}</td>
                </tr>
            `;
        }).join("");
    }

    function render(data) {
        setText(root, '[data-field="securityScore"]', formatMetric(data.securityScore, 0));
        setText(root, '[data-field="activeUsers"]', data.activeUsers ?? "—");
        setText(root, '[data-field="loginsToday"]', data.loginsToday ?? "—");
        setText(root, '[data-field="failedLogins"]', data.failedLogins ?? "—");
        setText(root, '[data-field="lockedUsers"]', data.lockedUsers ?? "—");
        setText(root, '[data-field="newUsers"]', data.newUsers ?? "—");
        setText(root, '[data-field="mfaCoverage"]', formatMetric(data.mfaCoverage, 0));
        setText(root, '[data-field="adminChanges"]', data.adminChanges ?? "—");

        setBar(root, '[data-bar="securityScore"]', data.securityScore, 100);
        setBar(root, '[data-bar="activeUsers"]', data.activeUsers, Math.max(Number(data.activeUsers || 0), 100));
        setBar(root, '[data-bar="loginsToday"]', data.loginsToday, Math.max(Number(data.loginsToday || 0), 100));
        setBar(root, '[data-bar="failedLogins"]', data.failedLogins, Math.max(Number(data.failedLogins || 0), 100));
        setBar(root, '[data-bar="lockedUsers"]', data.lockedUsers, Math.max(Number(data.lockedUsers || 0), 10));
        setBar(root, '[data-bar="newUsers"]', data.newUsers, Math.max(Number(data.newUsers || 0), 25));
        setBar(root, '[data-bar="mfaCoverage"]', data.mfaCoverage, 100);
        setBar(root, '[data-bar="adminChanges"]', data.adminChanges, Math.max(Number(data.adminChanges || 0), 10));

        const loginHealth = Array.isArray(data.loginHealth)
            ? data.loginHealth
            : [];

        const mfaCoverageDistribution = Array.isArray(data.mfaCoverageDistribution)
            ? data.mfaCoverageDistribution
            : [];

        const securityEventTypes = Array.isArray(data.securityEventTypes)
            ? data.securityEventTypes
            : [];

        drawHourlyLoginActivityChart(
            root.querySelector('[data-chart="hourlyLoginActivity7Days"]'),
            data.hourlyLoginActivity7Days
        );

        drawLineChart(
            root.querySelector('[data-chart="failedLoginTrend"]'),
            data.failedLoginTrend,
            {
                color: "#ef4444",
                startLabel: "10 days ago",
                endLabel: "Now"
            }
        );

        renderDonut(
            root.querySelector('[data-chart="loginHealth"]'),
            loginHealth,
            "Logins"
        );

        renderLegend(
            root.querySelector('[data-legend="loginHealth"]'),
            loginHealth
        );

        renderDonut(
            root.querySelector('[data-chart="mfaCoverage"]'),
            mfaCoverageDistribution,
            `${formatMetric(data.mfaCoverage, 0)}%`
        );

        renderLegend(
            root.querySelector('[data-legend="mfaCoverage"]'),
            mfaCoverageDistribution
        );

        renderDonut(
            root.querySelector('[data-chart="securityEventTypes"]'),
            securityEventTypes,
            "Events"
        );

        renderLegend(
            root.querySelector('[data-legend="securityEventTypes"]'),
            securityEventTypes
        );

        renderLoginsByCountry(data.loginsByCountry);
        renderSecurityStatus(data.securityStatus);
        renderRecentLogins(data.recentLogins);
        renderAuditEvents(data.auditEvents);
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

    function getPillClass(status) {
        if (status === "OK") {
            return "ok";
        }

        if (status === "Warning" || status === "Review") {
            return "warning";
        }

        return "error";
    }

    async function load() {
        let data;

        try {
            data = await fetchJson(DATA_URL);
        } catch (error) {
            console.warn("Using fallback audit and security dashboard data.", error);
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