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
    const padding = options.padding || 36;
    const chartWidth = width - padding * 2;
    const chartHeight = height - padding * 2;
    const color = options.color || "#2f9cff";
    const values = Array.isArray(series) ? series.map(numberOrZero) : [];

    context.clearRect(0, 0, width, height);

    if (values.length === 0) {
        drawNoData(context, padding);
        return;
    }

    const maxValue = Math.max(1, ...values);

    drawGrid(context, width, height, padding, maxValue);

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

    context.strokeStyle = color;
    context.lineWidth = options.lineWidth || 2;
    context.stroke();

    drawXAxisLabels(context, width, height, padding, options.startLabel || "Oldest", options.endLabel || "Now");
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

    context.strokeStyle = "rgba(141, 153, 168, 0.45)";
    context.beginPath();
    context.moveTo(padding, padding);
    context.lineTo(padding, height - padding);
    context.lineTo(width - padding, height - padding);
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