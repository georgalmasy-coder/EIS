import { renderPieChart } from "../../components/pie-chart.js";
import { setErrorState, setLoadingState } from "../../core/placeholders.js";
import { getDirectChild, getDirectChildren } from "../../core/xml.js";

function normalizeColor(color) {
    const map = {
        green: "#22c55e",
        red: "#ef4444",
        yellow: "#eab308",
        purple: "#a855f7",
        puple: "#a855f7",
        blue: "#3b82f6",
        orange: "#f97316",
        crimson: "#FF5A5F",
        skyblue: "#4FC3F7",
        limegreen: "#66BB6A",
        amber: "#FFCA28",
        violet: "#AB47BC",
        tangerine: "#FF8A65",
        cyan: "#26C6DA",
        magenta: "#EC407A",
        gold: "#FFD54F"
    };

    const normalized = String(color || "").trim().toLowerCase();

    return map[normalized] || normalized || "#60a5fa";
}

function mapSrlNodesToChartItems(rootNode) {
    const srlListNode = getDirectChild(rootNode, "SrlList");
    const srlNodes = srlListNode ? getDirectChildren(srlListNode, "Srl") : [];

    return srlNodes.map((node) => ({
        label: node.getAttribute("label") || "—",
        hover: node.getAttribute("hover") || "",
        color: normalizeColor(node.getAttribute("color")),
        value: Number(node.getAttribute("value") || "0")
    }));
}

export function renderOverviewSrlChart(rootNode, elements) {
    const items = mapSrlNodesToChartItems(rootNode);

    renderPieChart({
        slicesElement: elements.irlSlices,
        legendElement: elements.irlLegend,
        items,
        cx: 110,
        cy: 110,
        radius: 70,
        emptyText: items.length ? "SRL values are empty." : "No SRL data found."
    });
}

export function setOverviewSrlLoading(elements) {
    if (elements.irlSlices) {
        elements.irlSlices.innerHTML = "";
    }

    setLoadingState(elements.irlLegend, "Loading SRL data…");
}

export function setOverviewSrlError(elements) {
    if (elements.irlSlices) {
        elements.irlSlices.innerHTML = "";
    }

    setErrorState(elements.irlLegend, "Failed to load SRL data.");
}