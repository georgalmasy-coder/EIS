// Compare payloads built by the same serializer so XML formatting and UI
// defaults cannot make a freshly loaded entity appear changed.
export function createEditDirtyState({buildPayload, isReadOnly, isNew, root = document}) {
    let initialPayload = null;
    let saving = false;

    function isDirty() {
        return initialPayload !== null && (isNew() || buildPayload() !== initialPayload);
    }

    function update() {
        const button = root.getElementById("btnSave");
        if (button) {
            button.disabled = isReadOnly() || saving || !isDirty();
        }
    }

    function reset() {
        initialPayload = buildPayload();
        update();
    }

    function bind() {
        const fields = root.getElementById("basisInfoFields");
        fields?.addEventListener("input", update);
        fields?.addEventListener("change", update);
        update();
    }

    function setSaving(value) {
        saving = value;
        update();
    }

    function canSave() {
        return !isReadOnly() && !saving && isDirty();
    }

    return {bind, reset, update, isDirty, canSave, setSaving};
}
