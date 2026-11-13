export const hierarchyRoot = code => String(code || "").split(".")[0];

export function createDashboardHierarchy() {
    const collapsed = new Set();
    return {
        expanded(code) { return !collapsed.has(hierarchyRoot(code)); },
        toggle(code) {
            const root = hierarchyRoot(code);
            if (collapsed.has(root)) collapsed.delete(root);
            else collapsed.add(root);
        },
        setAll(codes, expanded) {
            collapsed.clear();
            if (!expanded) for (const code of codes) if (code) collapsed.add(hierarchyRoot(code));
        },
        visible(code) { return !code || code === hierarchyRoot(code) || !collapsed.has(hierarchyRoot(code)); }
    };
}

export function decorateDashboardCode(cell, code, name, expandable, expanded, onToggle) {
    cell.style.paddingLeft = "";
    const content = document.createElement("div");
    content.className = "hierarchy-code-cell";
    content.style.setProperty("--hierarchy-depth", String((String(code).match(/\./g) || []).length));
    const control = document.createElement(expandable ? "button" : "span");
    control.className = expandable ? "hierarchy-toggle" : "hierarchy-toggle-spacer";
    if (expandable) {
        control.type = "button";
        control.dataset.hierarchyCode = code;
        control.setAttribute("aria-expanded", String(expanded));
        control.setAttribute("aria-label", `${expanded ? "Collapse" : "Expand"} ${name || code}`);
        control.innerHTML = '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="m9 6 6 6-6 6"/></svg>';
        control.addEventListener("dblclick", event => event.stopPropagation());
        control.addEventListener("click", event => { event.stopPropagation(); onToggle(); });
    } else control.setAttribute("aria-hidden", "true");
    content.append(control, ...Array.from(cell.childNodes));
    cell.append(content);
}

export function focusDashboardToggle(body, code) {
    Array.from(body.querySelectorAll("[data-hierarchy-code]"))
        .find(button => button.dataset.hierarchyCode === code)?.focus({preventScroll: true});
}

// Interface data may omit the parent system itself; still provide a root group.
export function groupTeamworkRecords(records, hierarchy, systems = []) {
    const groups = new Map();
    for (const record of records) {
        const code = hierarchyRoot(record.fromSbsCode);
        if (!groups.has(code)) groups.set(code, []);
        groups.get(code).push(record);
    }
    // Every endpoint must also be visible as a source, including descendants.
    // Index existing orientations to avoid duplicates and growing-list searches.
    const endpoint = (record, side) => String(record[`${side}EntityId`] || record[`${side}SbsCode`] || "");
    const pairKey = (from, to) => JSON.stringify([from, to]);
    const orientations = new Set(records.map(record => pairKey(endpoint(record, "from"), endpoint(record, "to"))));
    for (const record of records) {
        const code = hierarchyRoot(record.toSbsCode);
        const key = pairKey(endpoint(record, "to"), endpoint(record, "from"));
        if (!code || orientations.has(key)) continue;
        orientations.add(key);
        if (!groups.has(code)) groups.set(code, []);
        const members = groups.get(code);
        const reversed = {...record};
        for (const key of Object.keys(record)) {
            if (key.startsWith("from") || key.startsWith("to")) {
                const suffix = key.slice(key.startsWith("from") ? 4 : 2);
                reversed[`from${suffix}`] = record[`to${suffix}`] ?? "";
                reversed[`to${suffix}`] = record[`from${suffix}`] ?? "";
            }
        }
        members.push(reversed);
    }
    const result = [];
    for (const [code, members] of groups) {
        if (!code) { result.push(...members.map(record => ({record}))); continue; }
        const root = members.find(record => record.fromSbsCode === code)
            || systems.find(record => record.fromSbsCode === code);
        result.push({group: true, code, name: root?.fromSystemName || "", count: members.length, record: root});
        if (hierarchy.expanded(code)) result.push(...members.filter(record => record !== root).map(record => ({record})));
    }
    return result;
}
