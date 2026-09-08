const ACTION_SELECT = "select-entity-for-move";
const ACTION_MOVE = "move-selected-entity";
const ACTION_MOVE_ROOT = "move-selected-entity-to-root";

export function createEntityMoveSelection({ menuId, entityType, scopeRoot }) {
    let selectedEntity = null;
    let contextEntity = null;
    let pendingTarget = null;
    let observer = null;

    ensureStylesheet();

    function initialize() {
        const menu = document.getElementById(menuId);
        if (!menu) return;

        if (!menu.querySelector(`[data-context-action="${ACTION_SELECT}"]`)) {
            menu.insertAdjacentHTML("beforeend", `
                <button type="button" data-context-action="${ACTION_SELECT}"></button>
                <button type="button" data-context-action="${ACTION_MOVE}" hidden></button>
                <button type="button" data-context-action="${ACTION_MOVE_ROOT}" hidden></button>
            `);
        }

        ensureDialog();
        observer = new MutationObserver(() => {
            if (!selectionIsApplied()) applySelectionMarker();
        });
        observer.observe(document.body, { childList: true, subtree: true });
    }

    function prepareContextMenu(entity) {
        contextEntity = normalizeEntity(entity);
        const menu = document.getElementById(menuId);
        const selectButton = menu?.querySelector(`[data-context-action="${ACTION_SELECT}"]`);
        const moveButton = menu?.querySelector(`[data-context-action="${ACTION_MOVE}"]`);
        const moveRootButton = menu?.querySelector(`[data-context-action="${ACTION_MOVE_ROOT}"]`);
        const hasTarget = Boolean(contextEntity?.entityId);

        if (selectButton) {
            selectButton.textContent = hasTarget ? `Select ${formatEntityLabel(contextEntity)}` : "Select";
            selectButton.toggleAttribute("hidden", !hasTarget);
        }

        const canMove = Boolean(selectedEntity?.entityId && hasTarget && selectedEntity.entityId !== contextEntity.entityId);
        if (moveButton) {
            moveButton.textContent = canMove
                ? `Move ${formatEntityLabel(selectedEntity)} To ${formatEntityLabel(contextEntity)}`
                : "Move";
            moveButton.toggleAttribute("hidden", !canMove);
        }

        if (moveRootButton) {
            moveRootButton.textContent = selectedEntity
                ? `Move ${formatEntityLabel(selectedEntity)} To Root`
                : "Move To Root";
            moveRootButton.toggleAttribute("hidden", !selectedEntity?.entityId);
        }
    }

    function handleContextAction(action, entity) {
        const target = normalizeEntity(entity) || contextEntity;
        if (action === ACTION_SELECT && target?.entityId) {
            selectedEntity = target;
            applySelectionMarker();
            return true;
        }
        if (action === ACTION_MOVE && selectedEntity?.entityId && target?.entityId && selectedEntity.entityId !== target.entityId) {
            openConfirmation(target);
            return true;
        }
        if (action === ACTION_MOVE_ROOT && selectedEntity?.entityId) {
            openConfirmation(null);
            return true;
        }
        return false;
    }

    function applySelectionMarker() {
        document.querySelectorAll(`.entity-move-selected[data-entity-move-owner="${entityType}"]`).forEach((element) => {
            element.classList.remove("entity-move-selected");
            element.removeAttribute("data-entity-move-owner");
        });
        document.querySelectorAll(`.entity-move-checkmark[data-entity-move-owner="${entityType}"]`).forEach((element) => element.remove());
        if (!selectedEntity?.entityId) return;

        const escapedId = CSS.escape(selectedEntity.entityId);
        getScopeRoot().querySelectorAll(`[data-entity-id="${escapedId}"]`).forEach((element) => {
            element.classList.add("entity-move-selected");
            element.dataset.entityMoveOwner = entityType;
            const isRow = element.tagName === "TR";
            let host = element;
            if (isRow) {
                host = Array.from(element.cells || []).find((cell) => cell.title === selectedEntity.code)
                    || element.querySelector("td");
            }
            if (!host) return;
            host.classList.add("entity-move-checkmark-host");
            host.insertAdjacentHTML("beforeend", `<span class="entity-move-checkmark" data-entity-move-owner="${entityType}" aria-label="Selected">&#10003;</span>`);
        });
    }

    function selectionIsApplied() {
        if (!selectedEntity?.entityId) {
            return document.querySelectorAll(`[data-entity-move-owner="${entityType}"]`).length === 0;
        }
        const matches = getScopeRoot().querySelectorAll(`[data-entity-id="${CSS.escape(selectedEntity.entityId)}"]`);
        return matches.length > 0 && Array.from(matches).every((element) =>
            element.classList.contains("entity-move-selected") && element.querySelector(".entity-move-checkmark")
        );
    }

    function openConfirmation(target) {
        const dialog = ensureDialog();
        pendingTarget = target;
        dialog.querySelector("[data-move-from]").textContent = formatEntityLabel(selectedEntity);
        dialog.querySelector("[data-move-to]").textContent = target ? formatEntityLabel(target) : "Root";
        dialog.showModal();
    }

    function ensureDialog() {
        const dialogId = `entityMoveConfirmationDialog-${entityType}`;
        const titleId = `entityMoveDialogTitle-${entityType}`;
        let dialog = document.getElementById(dialogId);
        if (dialog) return dialog;

        document.body.insertAdjacentHTML("beforeend", `
            <dialog id="${dialogId}" class="entity-move-dialog" aria-labelledby="${titleId}">
                <form method="dialog">
                    <h3 id="${titleId}">Confirm move</h3>
                    <p>The selected entity will become a child of the destination entity.</p>
                    <dl>
                        <dt>From:</dt><dd data-move-from></dd>
                        <dt>To:</dt><dd data-move-to></dd>
                    </dl>
                    <div class="entity-move-dialog-actions">
                        <button type="button" data-move-confirm>Confirm</button>
                        <button type="button" data-move-cancel>Cancel</button>
                    </div>
                </form>
            </dialog>
        `);
        dialog = document.getElementById(dialogId);
        dialog.querySelector("[data-move-cancel]").addEventListener("click", () => dialog.close());
        dialog.addEventListener("cancel", () => dialog.close());
        dialog.querySelector("[data-move-confirm]").addEventListener("click", () => confirmMove(dialog));
        return dialog;
    }

    async function confirmMove(dialog) {
        const confirmButton = dialog.querySelector("[data-move-confirm]");
        confirmButton.disabled = true;
        try {
            const response = await fetch(`/master/${entityType}/move`, {
                method: "POST",
                headers: { "Content-Type": "application/json", "Accept": "text/plain" },
                body: JSON.stringify({
                    fromEntityId: selectedEntity.entityId,
                    fromCode: selectedEntity.code,
                    toEntityId: pendingTarget?.entityId ?? null,
                    toCode: pendingTarget?.code ?? null
                })
            });
            const responseText = (await response.text()).trim();
            if (!response.ok || responseText !== "OK") {
                throw new Error(responseText || `Move failed (${response.status}).`);
            }
            window.location.reload();
        } catch (error) {
            window.alert(error.message || "The entity could not be moved.");
            confirmButton.disabled = false;
        }
    }

    initialize();
    return { prepareContextMenu, handleContextAction, applySelectionMarker };

    function getScopeRoot() {
        return scopeRoot ? document.querySelector(scopeRoot) || document : document;
    }
}

function normalizeEntity(entity) {
    if (!entity) return null;
    const entityId = String(entity.entityId ?? "").trim();
    const code = String(entity.id ?? entity.code ?? "").trim();
    const name = String(entity.name ?? "").trim();
    return { entityId, code: code || entityId, name };
}

function formatEntityLabel(entity) {
    return [entity?.code, entity?.name].filter(Boolean).join(" ");
}

function ensureStylesheet() {
    if (document.querySelector('link[data-entity-move-styles]')) return;
    const link = document.createElement("link");
    link.rel = "stylesheet";
    link.href = "../css/entity-move-selection.css";
    link.dataset.entityMoveStyles = "true";
    document.head.appendChild(link);
}
