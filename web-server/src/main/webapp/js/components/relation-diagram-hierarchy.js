// Entity codes use the same dot-separated hierarchy as the requirement lists.
export function decorateRelationDiagramHierarchy(container, requirements, onChanged) {
    const cards = new Map(Array.from(container.querySelectorAll('.relationdiagram-requirement-card'), card => [card.dataset.internalId, card]));
    const nodes = requirements.map(requirement => ({requirement, code: String(requirement.visibleId || '').trim(), children: [], collapsed: false}));
    const byCode = new Map(nodes.map(node => [node.code, node]));
    const roots = [];
    for (const node of nodes) {
        let parentCode = node.code;
        while (parentCode.includes('.')) {
            parentCode = parentCode.slice(0, parentCode.lastIndexOf('.'));
            if (byCode.has(parentCode)) { node.parent = byCode.get(parentCode); break; }
        }
        (node.parent ? node.parent.children : roots).push(node);
    }
    function refresh() {
        const pending = roots.map(node => ({node, hidden: false}));
        while (pending.length) {
            const {node, hidden} = pending.pop();
            node.card.dataset.hierarchyHidden = String(hidden);
            if (node.toggle) {
                node.toggle.setAttribute('aria-expanded', String(!node.collapsed));
                node.toggle.title = (node.collapsed ? 'Expand ' : 'Collapse ') + node.code;
            }
            for (const child of node.children) pending.push({node: child, hidden: hidden || node.collapsed});
        }
    }
    const pending = roots.slice().reverse().map(node => ({node, depth: 0}));
    while (pending.length) {
        const {node, depth} = pending.pop();
        const card = cards.get(node.requirement.internalId);
        node.card = card;
        const row = document.createElement('div');
        row.className = 'relationdiagram-hierarchy-row';
        const level = node.code.split('.').filter(Boolean).length;
        row.style.setProperty('--hierarchy-depth', level > 1 ? 1 : 0);
        row.classList.toggle('is-child', level > 1);
        card.classList.toggle('is-level-one', level === 1);
        row.append(card);
        if (level === 1 && node.children.length) {
            const toggle = document.createElement('button');
            toggle.type = 'button';
            toggle.className = 'relationdiagram-hierarchy-toggle';
            toggle.setAttribute('aria-label', 'Toggle children of ' + node.code);
            toggle.innerHTML = '<svg viewBox="0 0 16 16" aria-hidden="true"><path d="m5 3 5 5-5 5"/></svg>';
            toggle.addEventListener('click', () => {
                node.collapsed = !node.collapsed;
                refresh();
                onChanged();
            });
            node.toggle = toggle;
            row.append(toggle);
        }
        container.append(row);
        for (const child of node.children.slice().reverse()) pending.push({node: child, depth: depth + 1});
    }
    refresh();
}
