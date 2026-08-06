function polarToCartesian(cx, cy, radius, angleInDegrees) {
    const angleInRadians = ((angleInDegrees - 90) * Math.PI) / 180.0;

    return {
        x: cx + radius * Math.cos(angleInRadians),
        y: cy + radius * Math.sin(angleInRadians)
    };
}

function describeArc(cx, cy, radius, startAngle, endAngle) {
    const start = polarToCartesian(cx, cy, radius, endAngle);
    const end = polarToCartesian(cx, cy, radius, startAngle);
    const largeArcFlag = endAngle - startAngle <= 180 ? "0" : "1";

    return [
        "M", cx, cy,
        "L", start.x, start.y,
        "A", radius, radius, 0, largeArcFlag, 0, end.x, end.y,
        "Z"
    ].join(" ");
}

function renderEmptyState(legendElement, emptyText) {
    const emptyElement = document.createElement("div");

    emptyElement.className = "empty";
    emptyElement.textContent = emptyText;

    legendElement.innerHTML = "";
    legendElement.appendChild(emptyElement);
}

export function renderPieChart({
                                   slicesElement,
                                   legendElement,
                                   items,
                                   cx = 110,
                                   cy = 110,
                                   radius = 70,
                                   emptyText = "No chart data found."
                               }) {
    if (!slicesElement || !legendElement) {
        return;
    }

    slicesElement.innerHTML = "";
    legendElement.innerHTML = "";

    if (!items || !items.length) {
        renderEmptyState(legendElement, emptyText);
        return;
    }

    const validItems = items.filter((item) => Number.isFinite(item.value) && item.value > 0);
    const total = validItems.reduce((sum, item) => sum + item.value, 0);

    if (!total) {
        renderEmptyState(legendElement, emptyText);
        return;
    }

    let currentAngle = 0;

    validItems.forEach((item) => {
        const percentage = (item.value / total) * 100;
        const sliceAngle = (percentage / 100) * 360;
        const endAngle = currentAngle + sliceAngle;

        const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
        path.setAttribute("d", describeArc(cx, cy, radius, currentAngle, endAngle));
        path.setAttribute("fill", item.color);
        path.setAttribute("stroke", "rgba(15,23,42,.95)");
        path.setAttribute("stroke-width", "2");
        path.setAttribute("aria-label", `${item.label} ${percentage.toFixed(1)}%`);

        if (item.hover) {
            const title = document.createElementNS("http://www.w3.org/2000/svg", "title");
            title.textContent = item.hover;
            path.appendChild(title);
        }

        slicesElement.appendChild(path);

        const legendItem = document.createElement("div");
        legendItem.className = "chart-legend-item";

        const legendColor = document.createElement("span");
        legendColor.className = "chart-legend-color";
        legendColor.style.backgroundColor = item.color;

        const legendText = document.createElement("div");
        legendText.className = "chart-legend-text";
        legendText.textContent = item.label;

        const small = document.createElement("small");
        small.textContent = `${item.value} (${percentage.toFixed(1)}%)`;

        legendText.appendChild(small);
        legendItem.append(legendColor, legendText);
        legendElement.appendChild(legendItem);

        currentAngle = endAngle;
    });
}