import {
    buildPdfDocument,
    downloadBlob,
    clipPdfRect,
    endPdfClip,
    drawPdfBackground,
    drawPdfFilledRect,
    drawPdfMultilineText,
    drawPdfRect,
    drawPdfText,
    drawPdfTextCentered,
    formatGeneratedAt,
    formatPdfNumber,
    formatRgb,
    hexToRgb,
    fitPdfTextLines
} from "../core/pdf.js";
import { clampNumber } from "../core/utils.js";

export function downloadSystemsBreakdownDiagramPdf({
                                                       tree,
                                                       layout,
                                                       orientation,
                                                       topPanel,
                                                       systemCount
                                                   }) {
    if (!tree || !layout || !Array.isArray(layout.nodes)) {
        return;
    }

    const safeOrientation = orientation === "vertical" ? "vertical" : "horizontal";
    const safeTopPanel = topPanel || {};
    const safeSystemCount = Number.isFinite(Number(systemCount))
        ? Number(systemCount)
        : Math.max(0, (layout.nodes || []).filter((node) => node.type === "system").length);

    const normalizedLayout = normalizePdfLayout(tree, layout, safeOrientation);

    const pdfBytes = createDiagramPdf({
        tree,
        layout: normalizedLayout,
        orientation: safeOrientation,
        topPanel: safeTopPanel,
        systemCount: safeSystemCount
    });

    const blob = new Blob([pdfBytes], { type: "application/pdf" });
    downloadBlob(blob, buildDiagramPdfFileName(safeOrientation, safeTopPanel));
}

function buildDiagramPdfFileName(orientation, topPanel) {
    const projectName = String(topPanel.projectName || "project")
        .trim()
        .replace(/[^\p{L}\p{N}._-]+/gu, "-")
        .replace(/-+/g, "-")
        .replace(/^-|-$/g, "") || "project";

    return `physical-structure-${orientation}-diagram-${projectName}.pdf`;
}

/* ------------------------------------------------------------------ */
/* Layout normalization                                                */
/* ------------------------------------------------------------------ */

function normalizePdfLayout(tree, layout, orientation) {
    const nodes = (layout.nodes || []).map((node) => ({
        ...node,
        x: Number(node.x || 0),
        y: Number(node.y || 0),
        width: Number(node.width || layout.nodeWidth || 260),
        height: Number(node.height || layout.nodeHeight || 140)
    }));

    const nodeByKey = buildPositionedNodeLookup(nodes);
    const edgesFromTree = buildEdgesFromTree(tree, nodeByKey);
    const edges = edgesFromTree.length
        ? edgesFromTree
        : normalizeExistingEdges(layout.edges || [], nodeByKey);

    const margin = 28;
    const nodeWidth = Number(layout.nodeWidth || 260);
    const nodeHeight = Number(layout.nodeHeight || 140);

    const maxX = Math.max(...nodes.map((node) => node.x + node.width), 800);
    const maxY = Math.max(...nodes.map((node) => node.y + node.height), 480);

    return {
        ...layout,
        nodes,
        edges,
        width: Math.max(Number(layout.width || 0), maxX + margin),
        height: Math.max(Number(layout.height || 0), maxY + margin),
        nodeWidth,
        nodeHeight,
        orientation
    };
}

function buildPositionedNodeLookup(nodes) {
    const lookup = new Map();

    nodes.forEach((node) => {
        getNodeLookupKeys(node).forEach((key) => {
            if (key && !lookup.has(key)) {
                lookup.set(key, node);
            }
        });
    });

    return lookup;
}

function buildEdgesFromTree(root, nodeByKey) {
    const edges = [];

    walkTree(root, (node) => {
        const from = findPositionedNode(node, nodeByKey);

        if (!from) {
            return;
        }

        (node.children || []).forEach((child) => {
            const to = findPositionedNode(child, nodeByKey);

            if (to) {
                edges.push({ from, to });
            }
        });
    });

    return edges;
}

function normalizeExistingEdges(edges, nodeByKey) {
    return (edges || [])
        .map((edge) => {
            const from = findPositionedNode(edge.from, nodeByKey);
            const to = findPositionedNode(edge.to, nodeByKey);

            return from && to ? { from, to } : null;
        })
        .filter(Boolean);
}

function findPositionedNode(node, nodeByKey) {
    if (!node) {
        return null;
    }

    for (const key of getNodeLookupKeys(node)) {
        const positioned = nodeByKey.get(key);

        if (positioned) {
            return positioned;
        }
    }

    return null;
}

function getNodeLookupKeys(node) {
    if (!node) {
        return [];
    }

    const system = node.system || {};

    return [
        node.id,
        node.code,
        system.entityId,
        system.id,
        node.type === "project" ? "project-root" : "",
        node.type === "project" ? "Project" : ""
    ].map((value) => String(value || "").trim())
        .filter(Boolean);
}

/* ------------------------------------------------------------------ */
/* PDF creation                                                        */
/* ------------------------------------------------------------------ */

function createDiagramPdf({
                              tree,
                              layout,
                              orientation,
                              topPanel,
                              systemCount
                          }) {
    /*
     * Paper format:
     * horizontal diagram = A4 portrait / stående
     * vertical diagram   = A4 landscape / liggende
     */
    const isVerticalDiagram = orientation === "vertical";

    const pageWidth = isVerticalDiagram ? 841.89 : 595.28;
    const pageHeight = isVerticalDiagram ? 595.28 : 841.89;

    const margin = 28;
    const titleHeight = 38;
    const footerHeight = 24;

    const availableWidth = pageWidth - margin * 2;
    const availableHeight = pageHeight - margin * 2 - titleHeight - footerHeight;

    const scale = calculateScaleForFiveLevels(layout, orientation, availableWidth, availableHeight);
    const diagramPageWidth = availableWidth / scale;
    const diagramPageHeight = availableHeight / scale;

    const tiles = calculateTiles({
        layout,
        orientation,
        diagramPageWidth,
        diagramPageHeight
    });

    const generatedAt = formatGeneratedAt(new Date());

    const pages = tiles.map((tile, index) => {
        return createPageContent({
            layout,
            orientation,
            topPanel,
            systemCount,
            pageWidth,
            pageHeight,
            margin,
            titleHeight,
            footerHeight,
            availableWidth,
            availableHeight,
            scale,
            tile,
            pageNumber: index + 1,
            pageCount: tiles.length,
            generatedAt
        });
    });

    return buildPdfDocument(pageWidth, pageHeight, pages);
}

/*
 * Always reserve space for five hierarchy levels:
 * Project + level 1 + level 2 + level 3 + level 4.
 *
 * horizontal diagram: 5 levels must fit across the width.
 * vertical diagram:   5 levels must fit down the height.
 */
function calculateScaleForFiveLevels(layout, orientation, availableWidth, availableHeight) {
    const fiveLevelSpan = calculateRequiredFiveLevelSpan(layout, orientation);

    const fitScale = orientation === "vertical"
        ? availableHeight / Math.max(fiveLevelSpan, 1)
        : availableWidth / Math.max(fiveLevelSpan, 1);

    return clampNumber(fitScale, 0.16, 1);
}

function calculateRequiredFiveLevelSpan(layout, orientation) {
    const nodes = layout.nodes || [];
    const nodeWidth = Number(layout.nodeWidth || 260);
    const nodeHeight = Number(layout.nodeHeight || 140);

    if (orientation === "vertical") {
        const levelPositions = [...new Set(nodes.map((node) => Math.round(node.y)))]
            .sort((left, right) => left - right);

        const levelGap = calculateLevelGap(levelPositions, nodeHeight, 92);

        return nodeHeight * 5 + levelGap * 4;
    }

    const levelPositions = [...new Set(nodes.map((node) => Math.round(node.x)))]
        .sort((left, right) => left - right);

    const levelGap = calculateLevelGap(levelPositions, nodeWidth, 110);

    return nodeWidth * 5 + levelGap * 4;
}

function calculateLevelGap(levelPositions, nodeSize, fallbackGap) {
    if (!Array.isArray(levelPositions) || levelPositions.length < 2) {
        return fallbackGap;
    }

    const gaps = [];

    for (let index = 1; index < levelPositions.length; index++) {
        const distance = levelPositions[index] - levelPositions[index - 1];

        if (distance > nodeSize) {
            gaps.push(distance - nodeSize);
        }
    }

    if (!gaps.length) {
        return fallbackGap;
    }

    return Math.max(0, Math.min(...gaps));
}

function calculateTiles({
                            layout,
                            orientation,
                            diagramPageWidth,
                            diagramPageHeight
                        }) {
    if (orientation === "horizontal") {
        const yRanges = calculateAxisRanges({
            nodes: layout.nodes,
            axis: "y",
            sizeField: "height",
            totalSize: layout.height,
            preferredPageSize: diagramPageHeight,
            orientation
        });

        return yRanges.map((range) => ({
            startX: 0,
            endX: layout.width,
            startY: range.start,
            endY: range.end
        }));
    }

    const xRanges = calculateAxisRanges({
        nodes: layout.nodes,
        axis: "x",
        sizeField: "width",
        totalSize: layout.width,
        preferredPageSize: diagramPageWidth,
        orientation
    });

    return xRanges.map((range) => ({
        startX: range.start,
        endX: range.end,
        startY: 0,
        endY: layout.height
    }));
}

function calculateAxisRanges({
                                 nodes,
                                 axis,
                                 sizeField,
                                 totalSize,
                                 preferredPageSize,
                                 orientation
                             }) {
    const spans = (nodes || [])
        .map((node) => ({
            start: Number(node[axis] || 0),
            end: Number(node[axis] || 0) + Number(node[sizeField] || 0)
        }))
        .sort((left, right) => left.start - right.start);

    const ranges = [];
    let start = 0;

    while (start < totalSize) {
        let end = Math.min(start + preferredPageSize, totalSize);

        if (end < totalSize) {
            let nextEnd = end;

            do {
                end = nextEnd;
                nextEnd = adjustPageBreakToAvoidCuttingNodes(start, end, spans, preferredPageSize, orientation);
            } while (nextEnd !== end);
        }

        if (end <= start) {
            end = Math.min(start + preferredPageSize, totalSize);
        }

        ranges.push({ start, end });
        start = end;
    }

    if (!ranges.length) {
        ranges.push({ start: 0, end: totalSize || preferredPageSize });
    }

    return ranges;
}

function adjustPageBreakToAvoidCuttingNodes(start, proposedEnd, spans, preferredPageSize, orientation) {
    const cuttingNode = spans.find((span) => span.start < proposedEnd && span.end > proposedEnd);

    if (!cuttingNode) {
        return proposedEnd;
    }

    const pageBreakInset = orientation === "horizontal" ? 18 : 8;
    const breakBeforeNode = cuttingNode.start - pageBreakInset;

    if (breakBeforeNode > start + preferredPageSize * 0.45) {
        return breakBeforeNode;
    }

    return cuttingNode.end + pageBreakInset;
}

function createPageContent(options) {
    const {
        layout,
        orientation,
        topPanel,
        systemCount,
        pageWidth,
        pageHeight,
        margin,
        titleHeight,
        footerHeight,
        availableWidth,
        availableHeight,
        scale,
        tile,
        pageNumber,
        pageCount,
        generatedAt
    } = options;

    const commands = [];
    const tileWidth = tile.endX - tile.startX;
    const tileHeight = tile.endY - tile.startY;
    const drawWidth = tileWidth * scale;
    const drawHeight = tileHeight * scale;

    const offsetX = margin + Math.max(0, (availableWidth - drawWidth) / 2);
    const offsetY = margin + titleHeight + Math.max(0, (availableHeight - drawHeight) / 2);

    drawPdfBackground(commands, pageWidth, pageHeight);
    drawPdfHeader(commands, {
        pageWidth,
        pageHeight,
        margin,
        topPanel,
        systemCount,
        orientation
    });

    const clipBleedRight = orientation === "horizontal" ? 12 : 0;
    clipPdfRect(
        commands,
        margin,
        margin + footerHeight,
        pageWidth - margin * 2 + clipBleedRight,
        pageHeight - margin * 2 - titleHeight - footerHeight
    );

    const transformPoint = (x, y) => ({
        x: offsetX + (x - tile.startX) * scale,
        y: pageHeight - (offsetY + (y - tile.startY) * scale)
    });

    const isVisible = (node) => {
        return node.x + node.width >= tile.startX
            && node.x <= tile.endX
            && node.y + node.height >= tile.startY
            && node.y <= tile.endY;
    };

    drawPdfConnections(commands, layout.edges, transformPoint, scale, tile, orientation);
    drawPdfNodes(commands, layout.nodes, transformPoint, scale, isVisible, topPanel);

    endPdfClip(commands);

    commands.push("q");
    commands.push("1 1 1 rg");
    commands.push(`0 0 ${formatPdfNumber(pageWidth)} ${formatPdfNumber(margin + footerHeight - 4)} re`);
    commands.push("f");
    commands.push("Q");

    drawPdfFooter(commands, {
        pageWidth,
        margin,
        pageNumber,
        pageCount,
        generatedAt,
        userName: topPanel.userName || "—"
    });

    return commands.join("\n");
}

/* ------------------------------------------------------------------ */
/* Draw diagram                                                        */
/* ------------------------------------------------------------------ */

function drawPdfConnections(commands, edges, transformPoint, scale, tile, orientation) {
    (edges || []).forEach((edge) => {
        const from = edge.from;
        const to = edge.to;

        if (!from || !to || !edgeIntersectsTile(from, to, tile)) {
            return;
        }

        const points = orientation === "vertical"
            ? getVerticalConnectionPoints(from, to, transformPoint)
            : getHorizontalConnectionPoints(from, to, transformPoint);

        drawPdfStraightConnection(commands, points, scale);
    });
}

function edgeIntersectsTile(from, to, tile) {
    const left = Math.min(from.x, to.x);
    const right = Math.max(from.x + from.width, to.x + to.width);
    const top = Math.min(from.y, to.y);
    const bottom = Math.max(from.y + from.height, to.y + to.height);

    return !(right < tile.startX || left > tile.endX || bottom < tile.startY || top > tile.endY);
}

function getHorizontalConnectionPoints(from, to, transformPoint) {
    const startX = from.x + from.width;
    const startY = from.y + from.height / 2;
    const endX = to.x;
    const endY = to.y + to.height / 2;
    const midX = startX + ((endX - startX) / 2);

    return [
        transformPoint(startX, startY),
        transformPoint(midX, startY),
        transformPoint(midX, endY),
        transformPoint(endX, endY)
    ];
}

function getVerticalConnectionPoints(from, to, transformPoint) {
    const startX = from.x + from.width / 2;
    const startY = from.y + from.height;
    const endX = to.x + to.width / 2;
    const endY = to.y;
    const midY = startY + ((endY - startY) / 2);

    return [
        transformPoint(startX, startY),
        transformPoint(startX, midY),
        transformPoint(endX, midY),
        transformPoint(endX, endY)
    ];
}

function drawPdfStraightConnection(commands, points, scale) {
    if (!points || points.length < 2) {
        return;
    }

    commands.push("q");
    commands.push("0 0 0 RG");
    commands.push("1 w");
    commands.push(`${formatPdfNumber(points[0].x)} ${formatPdfNumber(points[0].y)} m`);

    points.slice(1).forEach((point) => {
        commands.push(`${formatPdfNumber(point.x)} ${formatPdfNumber(point.y)} l`);
    });

    commands.push("S");
    commands.push("Q");
}

function drawPdfNodes(commands, nodes, transformPoint, scale, isVisible, topPanel) {
    (nodes || []).forEach((node) => {
        if (!isVisible(node)) {
            return;
        }

        const topLeft = transformPoint(node.x, node.y);
        const width = node.width * scale;
        const height = node.height * scale;
        const x = topLeft.x;
        const y = topLeft.y - height;

        if (node.type === "project") {
            drawProjectNode(commands, node, topPanel, x, y, width, height, scale);
        } else {
            drawSystemNode(commands, node, x, y, width, height, scale);
        }
    });
}

function drawProjectNode(commands, node, topPanel, x, y, width, height, scale) {
    const footerHeight = Math.max(8, 18 * scale);

    drawPdfRect(commands, x, y, width, height, "#ffffff", "#000000");
    drawPdfFilledRect(commands, x, y, width, footerHeight, "#ffffff");

    drawPdfTextCentered(
        commands,
        topPanel.projectName || node.name || "Project",
        x + 5 * scale,
        y + footerHeight,
        width - 10 * scale,
        height - footerHeight,
        Math.max(3.8, 8.2 * scale),
        "Helvetica-Bold",
        [0, 0, 0],
        3
    );

    drawPdfTextCentered(
        commands,
        topPanel.customerName || node.description || "Customer",
        x + 4 * scale,
        y,
        width - 8 * scale,
        footerHeight,
        Math.max(3.5, 6.8 * scale),
        "Helvetica-Bold",
        [0, 0, 0],
        1
    );
}

function drawSystemNode(commands, node, x, y, width, height, scale) {
    const system = node.system || {};
    const code = system.id || node.code || "—";
    const name = system.name || node.name || "—";
    const trl = system.trl || "—";
    const trlTone = getTrlTone(trl);

    const statusBarHeight = Math.max(9, 19 * scale);
    const headerHeight = Math.max(15, 28 * scale);
    const trlFontSize = Math.max(3.8, 7.4 * scale) + 2;
    const trlTextWidth = width - 8 * scale;
    const trlTextHeight = Math.max(0, statusBarHeight - 4 * scale);
    const trlText = fitPdfTextLines(
        trl,
        trlTextWidth,
        trlFontSize,
        trlTextHeight,
        1
    )[0];

    drawPdfRect(commands, x, y, width, height, "#ffffff", "#000000");
    drawPdfFilledRect(commands, x, y, width, statusBarHeight, getPdfTrlColor(trlTone));

    drawPdfTextCentered(
        commands,
        trlText,
        x + 4 * scale,
        y,
        trlTextWidth,
        statusBarHeight,
        trlFontSize,
        "Helvetica-Bold",
        [0, 0, 0],
        1
    );

    drawPdfText(
        commands,
        code,
        x + 9 * scale,
        y + height - 20 * scale,
        Math.max(3.8, 7.4 * scale) + 2,
        "Helvetica-Bold",
        [0, 0, 0]
    );

    const titleFontSize = Math.max(3.8, 7.2 * scale);
    const titleLines = fitPdfTextLines(
        name,
        width - 18 * scale,
        titleFontSize + 2,
        Math.max((titleFontSize + 2) * 1.18 * 3, 14 * scale),
        3
    );

    drawPdfMultilineText(
        commands,
        titleLines,
        x + 9 * scale,
        y + height - headerHeight - 2 * scale,
        titleFontSize + 2,
        "Helvetica-Bold",
        [0, 0, 0],
        (titleFontSize + 2) * 1.18
    );
}

/* ------------------------------------------------------------------ */
/* PDF primitives                                                      */
/* ------------------------------------------------------------------ */

function drawPdfHeader(commands, options) {
    const {
        pageWidth,
        pageHeight,
        margin,
        topPanel,
        systemCount,
        orientation
    } = options;

    const orientationLabel = orientation === "vertical" ? "Vertical" : "Horizontal";

    drawPdfText(
        commands,
        `Physical Structure ${orientationLabel} Diagram - ${topPanel.projectName || "Project"}`,
        margin,
        pageHeight - margin - 10,
        12,
        "Helvetica-Bold",
        [31, 41, 55]
    );

    drawPdfText(
        commands,
        `${topPanel.customerName || "Customer"} · Systems: ${systemCount}`,
        margin,
        pageHeight - margin - 26,
        8,
        "Helvetica",
        [100, 116, 139]
    );

    commands.push("q");
    commands.push(`${formatRgb([226, 232, 240])} RG`);
    commands.push("0.6 w");
    commands.push(`${formatPdfNumber(margin)} ${formatPdfNumber(pageHeight - margin - 34)} m`);
    commands.push(`${formatPdfNumber(pageWidth - margin)} ${formatPdfNumber(pageHeight - margin - 34)} l`);
    commands.push("S");
    commands.push("Q");
}

function drawPdfFooter(commands, options) {
    const {
        pageWidth,
        margin,
        pageNumber,
        pageCount,
        generatedAt,
        userName
    } = options;

    const footerFontSize = 8;
    const footerColor = [100, 116, 139];

    const pageText = `Page ${pageNumber} of ${pageCount}`;
    const userText = ` ${userName || "—"}`;
    const timeText = `Generated: ${generatedAt}`;

    const userTextWidth = userText.length * footerFontSize * 0.52;
    const timeTextWidth = timeText.length * footerFontSize * 0.52;
    const userTextX = (pageWidth - userTextWidth) / 2;

    commands.push("q");
    commands.push(`${formatRgb([226, 232, 240])} RG`);
    commands.push("0.6 w");
    commands.push(`${formatPdfNumber(margin)} ${formatPdfNumber(margin + 8)} m`);
    commands.push(`${formatPdfNumber(pageWidth - margin)} ${formatPdfNumber(margin + 8)} l`);
    commands.push("S");
    commands.push("Q");

    drawPdfText(commands, pageText, margin, margin - 6, footerFontSize, "Helvetica", footerColor);
    drawPdfText(commands, userText, userTextX, margin - 6, footerFontSize, "Helvetica", footerColor);
    drawPdfText(commands, timeText, pageWidth - margin - timeTextWidth, margin - 6, footerFontSize, "Helvetica", footerColor);
}

/* ------------------------------------------------------------------ */
/* Utilities                                                           */
/* ------------------------------------------------------------------ */

function walkTree(node, callback) {
    if (!node) {
        return;
    }

    callback(node);

    (node.children || []).forEach((child) => {
        walkTree(child, callback);
    });
}

function getTrlTone(value) {
    const text = String(value || "").trim();
    const match = text.match(/^(\d)/) || text.match(/(\d)/);
    const tone = Number(match?.[1] || NaN);

    if (Number.isFinite(tone) && tone >= 1 && tone <= 9) {
        return tone;
    }

    return 0;
}

function getPdfTrlColor(tone) {
    if (tone === 1) return "#2563eb";
    if (tone === 2) return "#0ea5e9";
    if (tone === 3) return "#14b8a6";
    if (tone === 4) return "#16a34a";
    if (tone === 5) return "#a855f7";
    if (tone === 6) return "#f59e0b";
    if (tone === 7) return "#6d28d9";
    if (tone === 8) return "#ef4444";
    if (tone === 9) return "#64748b";

    return "#475569";
}

function truncatePdfText(value, maxChars) {
    const text = String(value || "—").trim();

    if (text.length <= maxChars) {
        return text;
    }

    if (maxChars <= 1) {
        return "…";
    }

    return `${text.slice(0, Math.max(1, maxChars - 1)).trimEnd()}…`;
}
