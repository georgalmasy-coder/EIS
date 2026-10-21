const codeOrder = new Intl.Collator(undefined, {numeric: true, sensitivity: "base"});

export function buildSrlDashboard(systems, components, levelIds) {
    const componentsById = new Map(components.map(component => [component.entityId, component]));
    const levels = [...new Set(levelIds.filter(value => Number.isFinite(value) && value > 0))].sort((a, b) => a - b);
    const rows = systems.map(system => {
        const component = componentsById.get(system.entityId);
        const level = component?.measuredSrlId;
        return {...system, srl: component?.srl ?? null,
            level: Number.isFinite(level) && levels.includes(level) ? level : null};
    }).sort((a, b) => codeOrder.compare(a.code, b.code) || codeOrder.compare(a.entityId, b.entityId));
    const summary = summarizeSrl(rows);
    return {levels, rows, measuredCount: rows.filter(row => row.level !== null).length,
        summary, groups: groupSystems(rows)};
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
    const components = [...root.querySelectorAll("calculation > componentSrl > component")].map(component => ({
        entityId: component.getAttribute("entityId"),
        measuredSrlId: readNumber(component.getAttribute("measuredSrlId")),
        srl: readNumber(component.getAttribute("srl"))
    }));
    const srlMeta = [...root.querySelectorAll("meta > srlMeta > srl")];
    const levels = srlMeta.map(level => readNumber(level.getAttribute("srlId")));
    return {...buildSrlDashboard(systems, components, levels),
        levelCodes: new Map(srlMeta.map(level => [readNumber(level.getAttribute("srlId")), level.getAttribute("code") || ""]))};
}

function readNumber(value) {
    return value === null || value.trim() === "" || !Number.isFinite(Number(value)) ? null : Number(value);
}

export function srlPosition(value, levels) {
    if (!levels.length || !Number.isFinite(value)) return null;
    if (levels.length === 1) return 50;
    return Math.max(0, Math.min(100, 100 * (value - levels[0]) / (levels.at(-1) - levels[0])));
}
