const codeOrder = new Intl.Collator(undefined, {numeric: true, sensitivity: "base"});

export function buildPrlMarkers(summary) {
    const markers = new Map();
    for (const [key, label] of [["lowest", "Low"], ["average", "Current Level"], ["highest", "High"]]) {
        const level = Math.round(summary[key]);
        if (!markers.has(level)) markers.set(level, {level, keys: [], labels: []});
        markers.get(level).keys.push(key);
        markers.get(level).labels.push(label);
    }
    return [...markers.values()].map(marker => ({...marker, label: marker.labels.join(" / ")}));
}

export function buildSrlDashboard(systems, components, levelIds) {
    const componentsById = new Map(components.map(component => [component.entityId, component]));
    const levels = [...new Set(levelIds.filter(value => Number.isFinite(value) && value > 0))].sort((a, b) => a - b);
    const rows = systems.map(system => {
        const component = componentsById.get(system.entityId);
        const level = component?.measuredSrlId;
        return {...system, srl: component?.srl ?? null,
            calculationTooltip: component?.calculationTooltip || "",
            level: Number.isFinite(level) && levels.includes(level) ? level : null};
    }).sort((a, b) => codeOrder.compare(a.code, b.code) || codeOrder.compare(a.entityId, b.entityId));
    const groups = groupSystems(rows);
    for (const group of groups) {
        // Calculate once for level 1 and show that same reference on every descendant.
        const measured = group.system.codeLevel === 1
            ? group.children.filter(row => row.level !== null) : [];
        const average = measured.length ? Math.round(group.summary.average) : null;
        for (const row of [group.system, ...group.children]) {
            row.descendantAverage = average;
            row.measuredDescendantCount = measured.length;
            row.averageSourceCode = average === null ? "" : group.system.code;
        }
    }
    const summary = summarizeSrl(rows);
    return {levels, rows, measuredCount: rows.filter(row => row.level !== null).length,
        summary, groups};
}

function summarizeSrl(rows) {
    let lowest = Infinity;
    let highest = -Infinity;
    let sum = 0;
    let measuredCount = 0;
    for (const row of rows) {
        if (row.level === null) continue;
        lowest = Math.min(lowest, row.level);
        highest = Math.max(highest, row.level);
        sum += row.level;
        measuredCount++;
    }
    return measuredCount ? {lowest, average: sum / measuredCount, highest} : null;
}

function groupSystems(rows) {
    const roots = rows.filter(row => row.codeLevel === 1);
    const byCode = new Map(roots.map(root => [root.code, {system: root, children: []}]));
    const groups = [...byCode.values()];
    for (const row of rows) {
        if (row.codeLevel === 1) continue;
        let code = row.code;
        let group;
        while (code.includes('.')) {
            code = code.slice(0, code.lastIndexOf('.'));
            group = byCode.get(code);
            if (group) break;
        }
        if (group) group.children.push(row);
        else groups.push({system: row, children: []});
    }
    for (const group of groups) {
        group.summary = summarizeSrl(group.children.length ? group.children : [group.system]);
    }
    return groups.sort((a, b) => codeOrder.compare(a.system.code, b.system.code));
}

export function parseSrlDashboard(xml) {
    const root = xml.querySelector("srlCalculation");
    if (!root) throw new Error("Missing SRL calculation data.");
    const systems = [...root.querySelectorAll("meta > systemsMeta > system")].map(system => ({
        entityId: system.getAttribute("entityId"), code: system.getAttribute("code") || "",
        name: system.getAttribute("name") || "", codeLevel: readNumber(system.getAttribute("codeLevel"))
    }));
    const diagnostics = readCalculationTooltips(root);
    const components = [...root.querySelectorAll("calculation > componentSrl > component")].map(component => ({
        entityId: component.getAttribute("entityId"),
        calculationTooltip: diagnostics.get(component.getAttribute("entityId")) || "",
        measuredSrlId: readNumber(component.getAttribute("measuredSrlId")),
        srl: readNumber(component.getAttribute("srl"))
    }));
    const srlMeta = [...root.querySelectorAll("meta > srlMeta > srl")];
    const levels = srlMeta.map(level => readNumber(level.getAttribute("srlId")));
    return {...buildSrlDashboard(systems, components, levels),
        levelCodes: new Map(srlMeta.map(level => [readNumber(level.getAttribute("srlId")), level.getAttribute("code") || ""]))};
}

// Temporary test diagnostics. Use server results for totals and level selection;
// rounded XML values must never be used to recalculate the dashboard result.
function readCalculationTooltips(root) {
    const read = (selector, attributes) => [...(root.querySelectorAll(selector) || [])].map(node =>
        Object.fromEntries(attributes.map(key => [key, node.getAttribute(key) || ""])));
    const systems = read("meta > systemsMeta > system", ["entityId", "code", "codeLevel", "trlId", "normalizedTrl"]);
    const systemsById = new Map(systems.map(system => [system.entityId, system]));
    const [maxima] = read("meta > lookupMaxima", ["maxTrlId", "maxIrlId"]);
    if (!maxima) return new Map();
    const interfaces = read("calculation > SystemsInterfaces > systemInterface",
        ["fromEntityId", "toEntityId", "irlValue"]);
    const bySource = new Map();
    for (const cell of interfaces) {
        if (!bySource.has(cell.fromEntityId)) bySource.set(cell.fromEntityId, []);
        bySource.get(cell.fromEntityId).push(cell);
    }
    const transitions = read("calculation > srlTransitionModel > level",
        ["srlId", "referenceValue", "lowerBound", "upperBound", "upperBoundInclusive"]);
    return new Map(read("calculation > componentSrl > component",
        ["entityId", "weightedTrlSum", "interfaceCount", "srl", "measuredSrlId", "measuredSrlRange"])
        .filter(component => systemsById.has(component.entityId))
        .map(component => [component.entityId, formatSrlCalculationTooltip({
            system: systemsById.get(component.entityId), component, maxima, systemsById,
            interfaces: bySource.get(component.entityId) || [], transitions
        })]));
}

export function formatSrlCalculationTooltip({system, component, maxima, systemsById, interfaces, transitions}) {
    const lines = [
        `SRL calculation (temporary test details): ${system.code}`,
        "Displayed decimals are rounded; the server calculates and selects the level using unrounded values.",
        `Normalisation: TRL / ${maxima.maxTrlId}; adjusted IRL / ${maxima.maxIrlId}.`,
        "Adjusted IRL = database IRL ID - 2. Only positive outgoing interfaces count, including the diagonal.",
        `Own TRL: ${system.trlId || "missing (uses 0)"}; normalised TRL: ${system.normalizedTrl || "0"}.`,
        "Interface contributions = normalised IRL × target system's normalised TRL:"
    ];
    const terms = [];
    for (const cell of interfaces) {
        const target = systemsById.get(cell.toEntityId);
        const trl = target?.trlId || "0";
        const term = `(${cell.irlValue}/${maxima.maxIrlId} × ${trl}/${maxima.maxTrlId})`;
        terms.push(term);
        const contribution = Number(maxima.maxIrlId) > 0 && Number(maxima.maxTrlId) > 0
            ? (Number(cell.irlValue) / Number(maxima.maxIrlId) * Number(trl) / Number(maxima.maxTrlId)).toFixed(9) : "0";
        lines.push(`${system.code} → ${target?.code || cell.toEntityId}${cell.toEntityId === system.entityId ? " (diagonal: maximum IRL)" : ""}: adjusted IRL ${cell.irlValue}; target TRL ${target?.trlId || "missing (uses 0)"}; ${term} ≈ ${contribution}`);
    }
    if (!interfaces.length) lines.push("No positive interfaces.");
    lines.push(`Weighted TRL sum (AA) = ${terms.join(" + ") || "0"} ≈ ${component.weightedTrlSum}.`,
        `Interface count = ${component.interfaceCount} (including diagonal and targets with missing TRL).`,
        Number(component.interfaceCount) > 0
            ? `Component SRL (AB) = unrounded AA / ${component.interfaceCount} ≈ ${component.srl}.`
            : `Component SRL (AB) = ${component.srl} (no interfaces; no division).`,
        "Transition references: reference(L) = (L / max TRL) × (L / max adjusted IRL).",
        "Lower bound = midpoint of previous and current reference (first: 0). Upper bound = midpoint of current and next reference (last: 1).",
        "Select the SRL with the greatest unrounded lower bound ≤ component SRL.");
    for (const level of transitions) {
        lines.push(`SRL ${level.srlId}: (${level.srlId}/${maxima.maxTrlId}) × (${level.srlId}/${maxima.maxIrlId}) ≈ ${level.referenceValue}; bounds ≈ [${level.lowerBound}, ${level.upperBound}${level.upperBoundInclusive === "true" ? "]" : ")"}${level.srlId === component.measuredSrlId ? " ← selected" : ""}`);
    }
    lines.push(`Result: ${component.measuredSrlId ? `SRL ${component.measuredSrlId}; displayed range ${component.measuredSrlRange}` : "Not calculated (no matching SRL level)"}.`);
    return lines.join("\n");
}

function readNumber(value) {
    return value === null || value.trim() === "" || !Number.isFinite(Number(value)) ? null : Number(value);
}

export function srlPosition(value, levels) {
    if (!levels.length || !Number.isFinite(value)) return null;
    if (levels.length === 1) return 50;
    return Math.max(0, Math.min(100, 100 * (value - levels[0]) / (levels.at(-1) - levels[0])));
}
