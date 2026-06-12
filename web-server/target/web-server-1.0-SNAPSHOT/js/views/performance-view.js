import { createAutoRefreshController, fetchJson } from "/js/admin-dashboard/dashboard-api.js";
import {
    setBar,
    setText
} from "/js/admin-dashboard/dashboard-charts.js";
import { formatNumber as formatNumberValue } from "/js/core/format.js";
import { escapeHtml } from "/js/core/html.js";
import { toNumber } from "/js/core/utils.js";

const DATA_URL = "/admin/api/dashboard/performance";
const REFRESH_MS = 60000;

export function createViewController(context) {
    const root = context.root;
    let chartHoverItems = [];
    let chartTooltip = null;

    function fallbackData() {
        return {
            goodPerformanceCount: 0,
            acceptablePerformanceCount: 0,
            poorPerformanceCount: 0,
            modulePerformance: [],
            projectModulePerformance: [],
            recentPerformanceMeasurements: []
        };
    }

    function formatNumber(value, decimals = 0) {
        const number = Number(value);

        if (!Number.isFinite(number)) {
            return decimals > 0 ? "0.0" : "0";
        }

        return formatNumberValue(number.toFixed(decimals), "da-DK", decimals > 0 ? "0.0" : "0");
    }

    function formatDuration(value) {
        return `${formatNumber(value, 0)} ms`;
    }

    function getPerformanceClass(interval) {
        const normalized = String(interval || "").toLowerCase();

        if (normalized === "good") {
            return "ok";
        }

        if (normalized === "acceptable") {
            return "warning";
        }

        if (normalized === "poor") {
            return "error";
        }

        return "warning";
    }

    function getKpiMax(data) {
        return Math.max(
            1,
            toNumber(data.goodPerformanceCount, 0),
            toNumber(data.acceptablePerformanceCount, 0),
            toNumber(data.poorPerformanceCount, 0)
        );
    }

    function ensureChartTooltip() {
        if (chartTooltip) {
            return chartTooltip;
        }

        chartTooltip = document.createElement("div");
        chartTooltip.style.position = "fixed";
        chartTooltip.style.zIndex = "9999";
        chartTooltip.style.pointerEvents = "none";
        chartTooltip.style.display = "none";
        chartTooltip.style.maxWidth = "280px";
        chartTooltip.style.padding = "10px 12px";
        chartTooltip.style.border = "1px solid rgba(141, 153, 168, 0.35)";
        chartTooltip.style.borderRadius = "10px";
        chartTooltip.style.background = "rgba(15, 23, 42, 0.96)";
        chartTooltip.style.color = "#f8fafc";
        chartTooltip.style.boxShadow = "0 16px 40px rgba(0, 0, 0, 0.35)";
        chartTooltip.style.font = "12px Arial";
        chartTooltip.style.lineHeight = "1.45";

        document.body.appendChild(chartTooltip);

        return chartTooltip;
    }

    function showChartTooltip(event, item) {
        const tooltip = ensureChartTooltip();
        const row = item.row;
        const valueLabel = item.type === "duration"
            ? "Avg duration"
            : "Count";
        const value = item.type === "duration"
            ? formatDuration(row.avgDurationMs)
            : formatNumber(row.count);

        tooltip.innerHTML = `
            <div style="font-weight:700;margin-bottom:6px;">${escapeHtml(valueLabel)}</div>
            <div><strong>Project:</strong> ${escapeHtml(row.project || "—")}</div>
            <div><strong>Module:</strong> ${escapeHtml(row.module || "—")}</div>
            <div><strong>Avg duration:</strong> ${escapeHtml(formatDuration(row.avgDurationMs))}</div>
            <div><strong>Count:</strong> ${escapeHtml(formatNumber(row.count))}</div>
            <div style="margin-top:6px;color:#cbd5e1;"><strong>Value:</strong> ${escapeHtml(value)}</div>
        `;

        const offset = 14;
        const viewportWidth = window.innerWidth;
        const viewportHeight = window.innerHeight;

        tooltip.style.display = "block";

        const tooltipRect = tooltip.getBoundingClientRect();

        let left = event.clientX + offset;
        let top = event.clientY + offset;

        if (left + tooltipRect.width > viewportWidth - 8) {
            left = event.clientX - tooltipRect.width - offset;
        }

        if (top + tooltipRect.height > viewportHeight - 8) {
            top = event.clientY - tooltipRect.height - offset;
        }

        tooltip.style.left = `${Math.max(8, left)}px`;
        tooltip.style.top = `${Math.max(8, top)}px`;
    }

    function hideChartTooltip() {
        if (chartTooltip) {
            chartTooltip.style.display = "none";
        }
    }

    function findChartHoverItem(canvas, event) {
        const rect = canvas.getBoundingClientRect();
        const scaleX = canvas.width / rect.width;
        const scaleY = canvas.height / rect.height;
        const x = (event.clientX - rect.left) * scaleX;
        const y = (event.clientY - rect.top) * scaleY;

        return chartHoverItems.find(function (item) {
            return x >= item.x
                && x <= item.x + item.width
                && y >= item.y
                && y <= item.y + item.height;
        });
    }

    function attachChartHoverHandlers(canvas) {
        if (canvas.dataset.performanceHoverAttached === "true") {
            return;
        }

        canvas.dataset.performanceHoverAttached = "true";

        canvas.addEventListener("mousemove", function (event) {
            const item = findChartHoverItem(canvas, event);

            if (!item) {
                canvas.style.cursor = "default";
                hideChartTooltip();
                return;
            }

            canvas.style.cursor = "pointer";
            showChartTooltip(event, item);
        });

        canvas.addEventListener("mouseleave", function () {
            canvas.style.cursor = "default";
            hideChartTooltip();
        });
    }

    function drawModulePerformanceChart(rows) {
        const canvas = root.querySelector('[data-chart="modulePerformance"]');

        if (!canvas) {
            return;
        }

        attachChartHoverHandlers(canvas);

        const context2d = canvas.getContext("2d");
        const width = canvas.width;
        const height = canvas.height;
        const paddingLeft = 62;
        const paddingRight = 62;
        const paddingTop = 46;
        const paddingBottom = 82;
        const chartWidth = width - paddingLeft - paddingRight;
        const chartHeight = height - paddingTop - paddingBottom;
        const safeRows = Array.isArray(rows) ? rows : [];

        chartHoverItems = [];
        context2d.clearRect(0, 0, width, height);

        if (safeRows.length === 0) {
            context2d.fillStyle = "#8d99a8";
            context2d.font = "14px Arial";
            context2d.fillText("No performance data available.", paddingLeft, 48);
            return;
        }

        const maxAvgDuration = Math.max(
            1,
            ...safeRows.map(function (row) {
                return toNumber(row.avgDurationMs, 0);
            })
        );

        const maxCount = Math.max(
            1,
            ...safeRows.map(function (row) {
                return toNumber(row.count, 0);
            })
        );

        drawDualAxisGrid(
            context2d,
            width,
            height,
            paddingLeft,
            paddingRight,
            paddingTop,
            paddingBottom,
            maxAvgDuration,
            maxCount
        );

        const groupWidth = chartWidth / safeRows.length;
        const barWidth = Math.min(30, Math.max(8, groupWidth * 0.22));
        const durationColor = "#2f9cff";
        const countColor = "#8b5cf6";

        safeRows.forEach(function (row, index) {
            const groupX = paddingLeft + index * groupWidth;
            const centerX = groupX + groupWidth / 2;

            const avgDuration = toNumber(row.avgDurationMs, 0);
            const count = toNumber(row.count, 0);

            const durationHeight = (avgDuration / maxAvgDuration) * chartHeight;
            const countHeight = (count / maxCount) * chartHeight;

            const durationX = centerX - barWidth - 3;
            const countX = centerX + 3;

            const durationY = height - paddingBottom - durationHeight;
            const countY = height - paddingBottom - countHeight;

            context2d.fillStyle = durationColor;
            context2d.fillRect(durationX, durationY, barWidth, durationHeight);

            chartHoverItems.push({
                type: "duration",
                row,
                x: durationX,
                y: durationY,
                width: barWidth,
                height: Math.max(durationHeight, 2)
            });

            context2d.fillStyle = countColor;
            context2d.fillRect(countX, countY, barWidth, countHeight);

            chartHoverItems.push({
                type: "count",
                row,
                x: countX,
                y: countY,
                width: barWidth,
                height: Math.max(countHeight, 2)
            });

            context2d.save();
            context2d.translate(centerX, height - paddingBottom + 16);
            context2d.rotate(-Math.PI / 5);
            context2d.fillStyle = "#8d99a8";
            context2d.font = "11px Arial";
            context2d.textAlign = "right";
            context2d.fillText(buildChartLabel(row), 0, 0);
            context2d.restore();
        });

        drawLegend(context2d, paddingLeft, durationColor, countColor);
    }

    function buildChartLabel(row) {
        const project = row.project || "";
        const module = row.module || "Unknown";

        if (!project) {
            return module;
        }

        return `${project} / ${module}`;
    }

    function drawDualAxisGrid(context2d, width, height, paddingLeft, paddingRight, paddingTop, paddingBottom, maxAvgDuration, maxCount) {
        const chartWidth = width - paddingLeft - paddingRight;
        const chartHeight = height - paddingTop - paddingBottom;
        const steps = 4;

        context2d.save();

        context2d.strokeStyle = "rgba(141, 153, 168, 0.22)";
        context2d.lineWidth = 1;
        context2d.fillStyle = "#8d99a8";
        context2d.font = "11px Arial";

        for (let i = 0; i <= steps; i++) {
            const fraction = i / steps;
            const y = height - paddingBottom - fraction * chartHeight;
            const avgLabelValue = Math.round(fraction * maxAvgDuration);
            const countLabelValue = Math.round(fraction * maxCount);

            context2d.beginPath();
            context2d.moveTo(paddingLeft, y);
            context2d.lineTo(paddingLeft + chartWidth, y);
            context2d.stroke();

            context2d.textAlign = "left";
            context2d.fillText(String(avgLabelValue), 8, y + 4);

            context2d.textAlign = "right";
            context2d.fillText(String(countLabelValue), width - 8, y + 4);
        }

        context2d.strokeStyle = "rgba(141, 153, 168, 0.45)";

        context2d.beginPath();
        context2d.moveTo(paddingLeft, paddingTop);
        context2d.lineTo(paddingLeft, height - paddingBottom);
        context2d.lineTo(width - paddingRight, height - paddingBottom);
        context2d.stroke();

        context2d.beginPath();
        context2d.moveTo(width - paddingRight, paddingTop);
        context2d.lineTo(width - paddingRight, height - paddingBottom);
        context2d.stroke();

        context2d.fillStyle = "#8d99a8";
        context2d.font = "12px Arial";
        context2d.textAlign = "left";
        context2d.fillText("Avg duration (ms)", paddingLeft, height - 12);

        context2d.textAlign = "right";
        context2d.fillText("Count", width - paddingRight, height - 12);

        context2d.restore();
    }

    function drawLegend(context2d, paddingLeft, durationColor, countColor) {
        context2d.save();

        context2d.font = "12px Arial";

        context2d.fillStyle = durationColor;
        context2d.fillRect(paddingLeft, 18, 10, 10);
        context2d.fillStyle = "#8d99a8";
        context2d.fillText("Avg duration (left axis)", paddingLeft + 16, 27);

        context2d.fillStyle = countColor;
        context2d.fillRect(paddingLeft + 190, 18, 10, 10);
        context2d.fillStyle = "#8d99a8";
        context2d.fillText("Count (right axis)", paddingLeft + 206, 27);

        context2d.restore();
    }

    function renderProjectModulePerformance(rows) {
        const body = root.querySelector('[data-table="projectModulePerformance"]');

        if (!body) {
            return;
        }

        const safeRows = Array.isArray(rows) ? rows : [];

        if (safeRows.length === 0) {
            body.innerHTML = `
                <tr>
                    <td colspan="7">No performance data found</td>
                </tr>
            `;
            return;
        }

        body.innerHTML = safeRows.map(function (row) {
            return `
                <tr>
                    <td>${escapeHtml(row.project || "—")}</td>
                    <td>${escapeHtml(row.module || "—")}</td>
                    <td>${escapeHtml(formatDuration(row.avgDurationMs))}</td>
                    <td>${escapeHtml(formatNumber(row.count))}</td>
                    <td><span class="dashboard-pill ok">${escapeHtml(formatNumber(row.goodPerformanceCount))}</span></td>
                    <td><span class="dashboard-pill warning">${escapeHtml(formatNumber(row.acceptablePerformanceCount))}</span></td>
                    <td><span class="dashboard-pill error">${escapeHtml(formatNumber(row.poorPerformanceCount))}</span></td>
                </tr>
            `;
        }).join("");
    }

    function renderRecentPerformanceMeasurements(rows) {
        const body = root.querySelector('[data-table="recentPerformanceMeasurements"]');

        if (!body) {
            return;
        }

        const safeRows = Array.isArray(rows) ? rows : [];

        if (safeRows.length === 0) {
            body.innerHTML = `
                <tr>
                    <td colspan="6">No recent performance measurements found</td>
                </tr>
            `;
            return;
        }

        body.innerHTML = safeRows.map(function (row) {
            const interval = row.performanceInterval || "Unknown";
            const intervalClass = getPerformanceClass(interval);

            return `
                <tr>
                    <td>${escapeHtml(row.created || "—")}</td>
                    <td>${escapeHtml(row.customer || "")}</td>
                    <td>${escapeHtml(row.project || "")}</td>
                    <td>${escapeHtml(row.module || "—")}</td>
                    <td>${escapeHtml(formatDuration(row.durationMs))}</td>
                    <td><span class="dashboard-pill ${intervalClass}">${escapeHtml(interval)}</span></td>
                </tr>
            `;
        }).join("");
    }

    function render(data) {
        const kpiMax = getKpiMax(data);
        const chartRows = Array.isArray(data.projectModulePerformance)
            ? data.projectModulePerformance
            : [];

        setText(root, '[data-field="goodPerformanceCount"]', formatNumber(data.goodPerformanceCount));
        setText(root, '[data-field="acceptablePerformanceCount"]', formatNumber(data.acceptablePerformanceCount));
        setText(root, '[data-field="poorPerformanceCount"]', formatNumber(data.poorPerformanceCount));

        setBar(root, '[data-bar="goodPerformanceCount"]', data.goodPerformanceCount, kpiMax);
        setBar(root, '[data-bar="acceptablePerformanceCount"]', data.acceptablePerformanceCount, kpiMax);
        setBar(root, '[data-bar="poorPerformanceCount"]', data.poorPerformanceCount, kpiMax);

        drawModulePerformanceChart(chartRows);
        renderProjectModulePerformance(data.projectModulePerformance);
        renderRecentPerformanceMeasurements(data.recentPerformanceMeasurements);
    }

    async function load() {
        let data;

        try {
            data = await fetchJson(DATA_URL);
        } catch (error) {
            console.warn("Using fallback performance dashboard data.", error);
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