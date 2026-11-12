export function mountHierarchyControls(inputId, onToggle, {container = false} = {}) {
    const input = document.getElementById(inputId);
    const label = container ? input?.firstElementChild : input?.closest("label");
    if (!label || document.getElementById(`${inputId}HierarchyControls`)) return;
    const controls = document.createElement("div");
    controls.id = `${inputId}HierarchyControls`;
    controls.className = "hierarchy-bulk-controls";
    controls.setAttribute("role", "group");
    controls.setAttribute("aria-label", "Expand or collapse all");
    for (const [expanded, title, path] of [
        [true, "Expand all", "m5 9 7-6 7 6M5 15l7 6 7-6"],
        [false, "Collapse all", "m5 3 7 6 7-6M5 21l7-6 7 6"]
    ]) {
        const button = document.createElement("button");
        button.type = "button";
        button.title = title;
        button.setAttribute("aria-label", title);
        button.innerHTML = `<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="${path}"/></svg>`;
        button.addEventListener("click", () => onToggle(expanded));
        controls.append(button);
    }
    label.before(controls);
    const resize = () => controls.style.setProperty("--hierarchy-control-height", `${input.getBoundingClientRect().height}px`);
    resize();
    // Also updates when the previously hidden requirements tab becomes visible.
    new ResizeObserver(resize).observe(input);
}

export function setHierarchyControlsVisible(inputId, visible) {
    const controls = document.getElementById(`${inputId}HierarchyControls`);
    if (controls) controls.hidden = !visible;
}
