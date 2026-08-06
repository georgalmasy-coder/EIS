import { escapeHtml } from "../core/html.js";
import { clampNumber, toNumber } from "../core/utils.js";

export function numberOrZero(value) {
    return toNumber(value, 0);
}

export function clamp(value, min, max) {
    return clampNumber(value, min, max);
}

export function formatMetric(value, decimals = 0) {
    return numberOrZero(value).toFixed(decimals).replace(/\.0$/, "");
}

export function setText(root, selector, value) {
    const element = root.querySelector(selector);

    if (element) {
        element.textContent = value;
    }
}

export function setBar(root, selector, value, max = 100) {
    const element = root.querySelector(selector);

    if (!element) {
        return;
    }

    const percent = clamp((numberOrZero(value) / max) * 100, 0, 100);
    element.style.width = `${percent}%`;

    if (percent >= 90) {
        element.style.background = "linear-gradient(90deg, var(--admin-red), #fb7185)";
    } else if (percent >= 75) {
        element.style.background = "linear-gradient(90deg, var(--admin-yellow), #fde68a)";
    } else {
        element.style.background = "linear-gradient(90deg, var(--admin-blue), #91d5ff)";
    }
}

export function drawLineChart(canvas, series, options = {}) {
    if (!canvas) {
        return;
    }

    const context = canvas.getContext("2d");
    const width = canvas.width;
    const height = canvas.height;

    const paddingLeft = options.paddingLeft || 58;
    const paddingRight = options.paddingRight || 28;
    const paddingTop = options.paddingTop || 34;
    const paddingBottom = options.paddingBottom || 46;

    const chartWidth = width - paddingLeft - paddingRight;
    const chartHeight = height - paddingTop - paddingBottom;

    const color = options.color || "#2f9cff";
    const fillColor = options.fillColor || null;
    const values = Array.isArray(series) ? series.map(numberOrZero) : [];

    context.clearRect(0, 0, width, height);

    if (values.length === 0) {
        drawNoData(context, paddingLeft);
        return;
    }

    const maxValue = typeof options.max === "number"
        ? options.max
        : Math.max(1, ...values);

    const minValue = typeof options.min === "number"
        ? options.min
        : 0;

    drawDetailedGrid(context, {
        width,
        height,
        paddingLeft,
        paddingRight,
        paddingTop,
        paddingBottom,
        chartWidth,
        chartHeight,
        minValue,
        maxValue,
        yUnit: options.yUnit || "",
        yLabel: options.yLabel || "",
        xLabels: Array.isArray(options.xLabels) ? options.xLabels : null,
        startLabel: options.leftFooter || options.startLabel || "Oldest",
        endLabel: options.rightFooter || options.endLabel || "Now"
    });

    const points = values.map(function (value, index) {
        const normalizedValue = clamp(value, minValue, maxValue);
        const x = paddingLeft + (index / Math.max(values.length - 1, 1)) * chartWidth;
        const y = paddingTop + chartHeight - ((normalizedValue - minValue) / Math.max(maxValue - minValue, 1)) * chartHeight;

        return {
            x,
            y,
            value: normalizedValue
        };
    });

    drawAreaAndLine(context, points, {
        baselineY: paddingTop + chartHeight,
        color,
        fillColor,
        lineWidth: options.lineWidth || 2
    });
}

export function drawCpuLoadChart(canvas, cpuLoadSeries, options = {}) {
    if (!canvas) {
        return;
    }

    const context = canvas.getContext("2d");
    const width = canvas.width;
    const height = canvas.height;

    const paddingLeft = options.paddingLeft || 58;
    const paddingRight = options.paddingRight || 28;
    const paddingTop = options.paddingTop || 34;
    const paddingBottom = options.paddingBottom || 48;

    const chartWidth = width - paddingLeft - paddingRight;
    const chartHeight = height - paddingTop - paddingBottom;

    const minValue = 0;
    const maxValue = 100;
    const historyHours = options.historyHours || 8;
    const now = new Date();
    const windowEnd = now.getTime();
    const windowStart = windowEnd - historyHours * 60 * 60 * 1000;

    const safePoints = Array.isArray(cpuLoadSeries) ? cpuLoadSeries : [];

    context.clearRect(0, 0, width, height);

    drawDetailedGrid(context, {
        width,
        height,
        paddingLeft,
        paddingRight,
        paddingTop,
        paddingBottom,
        chartWidth,
        chartHeight,
        minValue,
        maxValue,
        yUnit: "%",
        yLabel: options.yLabel || "% Utilization",
        xLabels: buildTimeLabelsForWindow(windowStart, windowEnd, 8),
        startLabel: options.leftFooter || `${historyHours} hours ago`,
        endLabel: options.rightFooter || "Now"
    });

    const points = safePoints
        .map(function (point) {
            const value = extractCpuLoadValue(point);
            const time = extractCpuLoadTime(point);

            if (!Number.isFinite(time)) {
                return null;
            }

            if (time < windowStart || time > windowEnd) {
                return null;
            }

            const normalizedValue = clamp(value, minValue, maxValue);
            const x = paddingLeft + ((time - windowStart) / Math.max(windowEnd - windowStart, 1)) * chartWidth;
            const y = paddingTop + chartHeight - ((normalizedValue - minValue) / Math.max(maxValue - minValue, 1)) * chartHeight;

            return {
                x,
                y,
                value: normalizedValue
            };
        })
        .filter(function (point) {
            return point !== null;
        });

    if (points.length === 0) {
        drawNoData(context, paddingLeft);
        return;
    }

    drawAreaAndLine(context, points, {
        baselineY: paddingTop + chartHeight,
        color: options.color || "#2f9cff",
        fillColor: options.fillColor || "rgba(47, 156, 255, 0.22)",
        lineWidth: options.lineWidth || 2
    });

    drawPointMarkers(context, points, options.color || "#2f9cff");
}

export function drawMultiLineChart(canvas, seriesDefinitions, options = {}) {
    if (!canvas) {
        return;
    }

    const context = canvas.getContext("2d");
    const width = canvas.width;
    const height = canvas.height;
    const padding = options.padding || 40;
    const chartWidth = width - padding * 2;
    const chartHeight = height - padding * 2;

    context.clearRect(0, 0, width, height);

    if (!Array.isArray(seriesDefinitions) || seriesDefinitions.length === 0) {
        drawNoData(context, padding);
        return;
    }

    const allValues = seriesDefinitions.flatMap(function (definition) {
        return Array.isArray(definition.values) ? definition.values.map(numberOrZero) : [];
    });

    if (allValues.length === 0) {
        drawNoData(context, padding);
        return;
    }

    const maxValue = Math.max(1, ...allValues);

    drawGrid(context, width, height, padding, maxValue);

    seriesDefinitions.forEach(function (definition) {
        const values = Array.isArray(definition.values) ? definition.values.map(numberOrZero) : [];

        if (!values.length) {
            return;
        }

        context.beginPath();

        values.forEach(function (value, index) {
            const x = padding + (index / Math.max(values.length - 1, 1)) * chartWidth;
            const y = height - padding - (value / maxValue) * chartHeight;

            if (index === 0) {
                context.moveTo(x, y);
            } else {
                context.lineTo(x, y);
            }
        });

        context.strokeStyle = definition.color || "#2f9cff";
        context.lineWidth = definition.lineWidth || 2;
        context.stroke();
    });

    drawXAxisLabels(context, width, height, padding, options.startLabel || "Oldest", options.endLabel || "Now");

    if (options.legend !== false) {
        drawCanvasLegend(context, seriesDefinitions, width, padding);
    }
}

export function drawHourlyLoginActivityChart(canvas, points, options = {}) {
    if (!canvas) {
        return;
    }

    const safePoints = Array.isArray(points) ? points : [];

    const successfulValues = safePoints.map(function (point) {
        return numberOrZero(point.successful);
    });

    const failedValues = safePoints.map(function (point) {
        return numberOrZero(point.failed);
    });

    const totalValues = safePoints.map(function (point) {
        return numberOrZero(point.total);
    });

    drawMultiLineChart(
        canvas,
        [
            {
                label: "Total",
                values: totalValues,
                color: options.totalColor || "#2f9cff",
                lineWidth: 2
            },
            {
                label: "Successful",
                values: successfulValues,
                color: options.successfulColor || "#84d64b",
                lineWidth: 2
            },
            {
                label: "Failed",
                values: failedValues,
                color: options.failedColor || "#ef4444",
                lineWidth: 2
            }
        ],
        {
            padding: options.padding || 44,
            startLabel: options.startLabel || "7 days ago",
            endLabel: options.endLabel || "Now",
            legend: true
        }
    );
}

function extractCpuLoadValue(point) {
    if (typeof point === "number") {
        return point;
    }

    if (!point) {
        return 0;
    }

    if (typeof point.value === "number") {
        return point.value;
    }

    if (typeof point.cpu === "number") {
        return point.cpu;
    }

    if (typeof point.load === "number") {
        return point.load;
    }

    return 0;
}

function extractCpuLoadTime(point) {
    if (!point || typeof point === "number") {
        return NaN;
    }

    if (typeof point.epochMillis === "number") {
        return point.epochMillis;
    }

    if (typeof point.timestampMillis === "number") {
        return point.timestampMillis;
    }

    const rawTime = point.time || point.timestamp || point.createdAt;

    if (!rawTime) {
        return NaN;
    }

    const normalizedTime = String(rawTime)
        .trim()
        .replace(" ", "T");

    /*
     * Backend currently returns LocalDateTime without timezone, e.g.:
     * 2026-06-12T18:20:10
     *
     * Do NOT append "Z" here. Appending "Z" treats the value as UTC and shifts
     * it by the local timezone offset in the browser. That makes fresh samples
     * look like they are in the future and they get filtered out.
     */
    const date = new Date(normalizedTime);
    const time = date.getTime();

    return Number.isFinite(time) ? time : NaN;
}

function drawAreaAndLine(context, points, options) {
    if (!Array.isArray(points) || points.length === 0) {
        return;
    }

    const color = options.color || "#2f9cff";
    const fillColor = options.fillColor || null;
    const baselineY = options.baselineY;
    const lineWidth = options.lineWidth || 2;

    if (fillColor && points.length > 1) {
        context.save();
        context.beginPath();

        points.forEach(function (point, index) {
            if (index === 0) {
                context.moveTo(point.x, point.y);
            } else {
                context.lineTo(point.x, point.y);
            }
        });

        context.lineTo(points[points.length - 1].x, baselineY);
        context.lineTo(points[0].x, baselineY);
        context.closePath();

        context.fillStyle = fillColor;
        context.fill();
        context.restore();
    }

    context.save();
    context.beginPath();

    points.forEach(function (point, index) {
        if (index === 0) {
            context.moveTo(point.x, point.y);
        } else {
            context.lineTo(point.x, point.y);
        }
    });

    context.strokeStyle = color;
    context.lineWidth = lineWidth;
    context.stroke();
    context.restore();
}

function drawPointMarkers(context, points, color) {
    if (!Array.isArray(points) || points.length === 0) {
        return;
    }

    context.save();
    context.fillStyle = color || "#2f9cff";

    points.forEach(function (point) {
        context.beginPath();
        context.arc(point.x, point.y, 2, 0, Math.PI * 2);
        context.fill();
    });

    context.restore();
}

function drawDetailedGrid(context, options) {
    const {
        width,
        height,
        paddingLeft,
        paddingRight,
        paddingTop,
        paddingBottom,
        chartWidth,
        chartHeight,
        minValue,
        maxValue,
        yUnit,
        yLabel,
        xLabels,
        startLabel,
        endLabel
    } = options;

    const horizontalSteps = 4;
    const verticalSteps = 8;

    context.save();

    context.strokeStyle = "rgba(141, 153, 168, 0.22)";
    context.lineWidth = 1;
    context.fillStyle = "#8d99a8";
    context.font = "11px Arial";

    if (yLabel) {
        context.fillText(yLabel, paddingLeft, 16);
    }

    for (let i = 0; i <= horizontalSteps; i++) {
        const fraction = i / horizontalSteps;
        const y = paddingTop + chartHeight - fraction * chartHeight;
        const labelValue = Math.round(minValue + fraction * (maxValue - minValue));
        const labelText = `${labelValue}${yUnit}`;

        context.beginPath();
        context.moveTo(paddingLeft, y);
        context.lineTo(width - paddingRight, y);
        context.stroke();

        context.fillStyle = "#8d99a8";
        context.fillText(labelText, 8, y + 4);
    }

    for (let i = 0; i <= verticalSteps; i++) {
        const fraction = i / verticalSteps;
        const x = paddingLeft + fraction * chartWidth;

        context.beginPath();
        context.moveTo(x, paddingTop);
        context.lineTo(x, paddingTop + chartHeight);
        context.stroke();

        const labelText = xLabels && xLabels[i]
            ? xLabels[i]
            : null;

        if (labelText) {
            const labelWidth = context.measureText(labelText).width;
            context.fillText(labelText, x - labelWidth / 2, height - 24);
        }
    }

    context.strokeStyle = "rgba(141, 153, 168, 0.45)";
    context.beginPath();
    context.moveTo(paddingLeft, paddingTop);
    context.lineTo(paddingLeft, paddingTop + chartHeight);
    context.lineTo(width - paddingRight, paddingTop + chartHeight);
    context.lineTo(width - paddingRight, paddingTop);
    context.closePath();
    context.stroke();

    context.fillStyle = "#8d99a8";
    context.font = "12px Arial";
    context.fillText(startLabel, paddingLeft, height - 8);
    context.fillText(endLabel, width - paddingRight - context.measureText(endLabel).width, height - 8);

    context.restore();
}

function drawGrid(context, width, height, padding, maxValue) {
    const chartWidth = width - padding * 2;
    const chartHeight = height - padding * 2;
    const steps = 4;

    context.save();

    context.strokeStyle = "rgba(141, 153, 168, 0.22)";
    context.lineWidth = 1;
    context.fillStyle = "#8d99a8";
    context.font = "11px Arial";

    for (let i = 0; i <= steps; i++) {
        const fraction = i / steps;
        const y = height - padding - fraction * chartHeight;
        const labelValue = Math.round(fraction * maxValue);

        context.beginPath();
        context.moveTo(padding, y);
        context.lineTo(padding + chartWidth, y);
        context.stroke();

        context.fillText(String(labelValue), 6, y + 4);
    }

    const verticalSteps = 8;

    for (let i = 0; i <= verticalSteps; i++) {
        const x = padding + (i / verticalSteps) * chartWidth;

        context.beginPath();
        context.moveTo(x, padding);
        context.lineTo(x, height - padding);
        context.stroke();
    }

    context.strokeStyle = "rgba(141, 153, 168, 0.45)";
    context.beginPath();
    context.moveTo(padding, padding);
    context.lineTo(padding, height - padding);
    context.lineTo(width - padding, height - padding);
    context.lineTo(width - padding, padding);
    context.closePath();
    context.stroke();

    context.restore();
}

function drawXAxisLabels(context, width, height, padding, startLabel, endLabel) {
    context.save();

    context.fillStyle = "#8d99a8";
    context.font = "12px Arial";
    context.fillText(startLabel, padding, height - 8);
    context.fillText(endLabel, width - padding - context.measureText(endLabel).width, height - 8);

    context.restore();
}

function drawCanvasLegend(context, seriesDefinitions, width, padding) {
    context.save();

    let x = padding;
    const y = 18;

    context.font = "12px Arial";
    context.fillStyle = "#8d99a8";

    seriesDefinitions.forEach(function (definition) {
        const label = definition.label || "Series";
        const color = definition.color || "#2f9cff";

        context.fillStyle = color;
        context.fillRect(x, y - 9, 10, 10);

        context.fillStyle = "#8d99a8";
        context.fillText(label, x + 14, y);

        x += context.measureText(label).width + 42;

        if (x > width - padding - 80) {
            x = padding;
        }
    });

    context.restore();
}

function drawNoData(context, padding) {
    context.fillStyle = "#8d99a8";
    context.font = "14px Arial";
    context.fillText("No chart data available.", padding, 42);
}

function buildTimeLabelsForWindow(windowStart, windowEnd, steps) {
    const labels = [];

    for (let index = 0; index <= steps; index++) {
        const fraction = index / steps;
        const time = new Date(windowStart + fraction * (windowEnd - windowStart));

        labels.push(`${pad2(time.getHours())}:${pad2(time.getMinutes())}`);
    }

    return labels;
}

function pad2(value) {
    return String(value).padStart(2, "0");
}

export function renderDonut(svg, items, centerText) {
    if (!svg) {
        return;
    }

    const radius = 62;
    const circumference = 2 * Math.PI * radius;
    const safeItems = Array.isArray(items) ? items : [];
    const total = Math.max(1, safeItems.reduce(function (sum, item) {
        return sum + numberOrZero(item.value);
    }, 0));

    svg.innerHTML = `
        <circle cx="90" cy="90" r="${radius}" class="dashboard-donut-track"></circle>
        <circle cx="90" cy="90" r="38" class="dashboard-donut-center"></circle>
        <text x="90" y="96" class="dashboard-donut-center-text">${escapeHtml(centerText)}</text>
    `;

    let offset = 0;

    safeItems.forEach(function (item) {
        const value = numberOrZero(item.value);
        const length = (value / total) * circumference;

        const circle = document.createElementNS("http://www.w3.org/2000/svg", "circle");
        circle.setAttribute("cx", "90");
        circle.setAttribute("cy", "90");
        circle.setAttribute("r", String(radius));
        circle.setAttribute("class", "dashboard-donut-segment");
        circle.setAttribute("stroke", item.color || "#2f9cff");
        circle.setAttribute("stroke-dasharray", `${length} ${circumference - length}`);
        circle.setAttribute("stroke-dashoffset", String(-offset));

        svg.appendChild(circle);
        offset += length;
    });
}

export function renderLegend(container, items) {
    if (!container) {
        return;
    }

    const safeItems = Array.isArray(items) ? items : [];

    container.innerHTML = safeItems.map(function (item) {
        return `
            <div class="dashboard-legend-item">
                <span class="dashboard-legend-swatch" style="background:${escapeHtml(item.color || "#2f9cff")}"></span>
                <span>${escapeHtml(item.label || "Unknown")}: ${escapeHtml(String(item.value ?? "—"))}</span>
            </div>
        `;
    }).join("");
}

export { escapeHtml };