import { closeDialogElement, setInputValue, setText, showDialog } from "../core/dom.js";
import { serializeXml } from "../core/xml.js";

const ENTITY_TYPE_BASE_PATHS = new Map([
    ["2", "/master/psys/interfacematrix"],
    ["5", "/master/stk/interfacematrix"],
    ["6", "/master/sys/interfacematrix"],
    ["8", "/master/lsys/interfacematrix"],
    ["9", "/master/fsys/interfacematrix"]
]);

let activeDialog = null;

export function initInterfaceEditDialog(options = {}) {
    if (!activeDialog) {
        activeDialog = createInterfaceEditDialog(options);
    } else {
        activeDialog.configure(options);
    }

    return activeDialog;
}

export function openInterfaceEditDialog(options = {}) {
    const dialog = initInterfaceEditDialog(options);
    dialog.open(options);
}

function createInterfaceEditDialog(initialOptions = {}) {
    ensureDialogStyles();
    ensureDialogMarkup();

    const state = {
        basePath: initialOptions.basePath || document.body?.dataset.interfaceBasePath || "/master/psys/interfacematrix",
        structureLabel: initialOptions.structureLabel || document.body?.dataset.interfaceStructureLabel || "Physical Architecture",
        lookup: initialOptions.lookup || { irlById: new Map(), classificationById: new Map() },
        fromStructure: null,
        toStructure: null,
        currentCell: null,
        onSaved: initialOptions.onSaved || (() => window.location.reload())
    };

    wireEvents(state);

    return {
        configure(options = {}) {
            state.basePath = options.basePath || state.basePath;
            state.structureLabel = options.structureLabel || state.structureLabel;
            state.lookup = options.lookup || state.lookup;
            state.onSaved = options.onSaved || state.onSaved;
        },
        open(options = {}) {
            this.configure(options);
            openDialog(state, options.fromStructure, options.toStructure, options.cell);
        }
    };
}

export function resolveInterfaceBasePath(entityTypeId) {
    return ENTITY_TYPE_BASE_PATHS.get(String(entityTypeId || "").trim()) || "/master/psys/interfacematrix";
}

function ensureDialogStyles() {
    if (document.querySelector("link[data-interface-edit-dialog-styles]")) {
        return;
    }

    const link = document.createElement("link");
    link.rel = "stylesheet";
    link.href = "../css/interfaces.css";
    link.dataset.interfaceEditDialogStyles = "true";
    document.head?.appendChild(link);
}

function ensureDialogMarkup() {
    if (document.getElementById("interfaceDialog")) {
        return;
    }

    const template = document.createElement("template");
    template.innerHTML = `
        <dialog id="interfaceDialog" class="interfaces-dialog" aria-labelledby="interfaceDialogTitle">
            <form method="dialog" class="interfaces-dialog-form">
                <div class="interfaces-dialog-head">
                    <h3 id="interfaceDialogTitle">Edit Interface</h3>
                </div>
                <div class="interfaces-dialog-content">
                    <section class="interfaces-dialog-card" aria-labelledby="interfaceFromTitle">
                        <h4 id="interfaceFromTitle">From</h4>
                        <div class="interfaces-dialog-card-fields" id="interfaceFromFields"></div>
                    </section>
                    <section class="interfaces-dialog-card" aria-labelledby="interfaceToTitle">
                        <h4 id="interfaceToTitle">To</h4>
                        <div class="interfaces-dialog-card-fields" id="interfaceToFields"></div>
                    </section>
                    <section class="interfaces-dialog-card interfaces-dialog-card--full" aria-labelledby="interfaceDetailsTitle">
                        <h4 id="interfaceDetailsTitle">Interface Details</h4>
                        <div class="interfaces-dialog-form-grid">
                            <label class="interfaces-dialog-field">
                                <span>IRL</span>
                                <select id="interfaceDialogIrlSelect"></select>
                            </label>
                            <label class="interfaces-dialog-field">
                                <span>Next Irl Meeting</span>
                                <input id="interfaceDialogNextIrlMeeting" type="date" />
                            </label>
                        </div>
                        <div class="interfaces-dialog-classification-group">
                            <div class="interfaces-dialog-field-label">Classification</div>
                            <div id="interfaceDialogClassificationList" class="interfaces-dialog-classification-list"></div>
                        </div>
                    </section>
                </div>
                <div class="interfaces-dialog-footer">
                    <div class="interfaces-dialog-footer-note" id="interfaceDialogStatus"></div>
                    <div class="interfaces-dialog-footer-actions">
                        <button id="interfaceDialogSaveButton" class="primary" type="button">Save</button>
                        <button id="interfaceDialogCancelButton" type="button">Cancel</button>
                    </div>
                </div>
            </form>
        </dialog>
    `;
    document.body?.appendChild(template.content);
}

function wireEvents(state) {
    const dialog = document.getElementById("interfaceDialog");

    document.getElementById("interfaceDialogCancelButton")?.addEventListener("click", () => closeInterfaceDialog(state));
    document.getElementById("interfaceDialogSaveButton")?.addEventListener("click", async () => saveCurrentInterface(state));
    dialog?.addEventListener("cancel", (event) => {
        event.preventDefault();
        closeInterfaceDialog(state);
    });
}

function openDialog(state, fromStructure, toStructure, cell) {
    state.fromStructure = fromStructure || null;
    state.toStructure = toStructure || null;
    state.currentCell = cell || null;

    setText("interfaceDialogTitle", cell ? "Edit Interface" : "Create Interface", "");
    setText("interfaceFromTitle", `From ${state.structureLabel}`, "");
    setText("interfaceToTitle", `To ${state.structureLabel}`, "");
    setText("interfaceDialogStatus", "", "");

    renderStructureCard("interfaceFromFields", fromStructure);
    renderStructureCard("interfaceToFields", toStructure);
    renderIrlSelect(state, cell?.irlId || "");
    renderNextIrlMeeting(cell?.nextIrlMeeting || "");
    renderClassificationList(state, cell?.classificationIds || []);

    showDialog("interfaceDialog");
}

function renderStructureCard(containerId, structure) {
    const container = document.getElementById(containerId);
    if (!container) return;
    container.innerHTML = "";

    for (const [label, value] of [["ID", structure?.id || "-"], ["Name", structure?.name || "-"]]) {
        const field = document.createElement("div");
        field.className = "interfaces-dialog-card-field";
        const fieldLabel = document.createElement("span");
        fieldLabel.className = "interfaces-dialog-card-field-label";
        fieldLabel.textContent = label;
        const fieldValue = document.createElement("strong");
        fieldValue.className = "interfaces-dialog-card-field-value";
        fieldValue.textContent = value;
        field.append(fieldLabel, fieldValue);
        container.appendChild(field);
    }
}

function renderIrlSelect(state, selectedIrlId) {
    const select = document.getElementById("interfaceDialogIrlSelect");
    if (!select) return;
    select.innerHTML = "";

    const emptyOption = document.createElement("option");
    emptyOption.value = "";
    emptyOption.textContent = "Select IRL";
    select.appendChild(emptyOption);

    for (const irl of state.lookup.irlById.values()) {
        const option = document.createElement("option");
        option.value = irl.id;
        option.textContent = irl.code || irl.id;
        select.appendChild(option);
    }

    select.value = selectedIrlId || "";
}

function renderNextIrlMeeting(value) {
    setInputValue("interfaceDialogNextIrlMeeting", value || "", "");
}

function renderClassificationList(state, selectedClassificationIds) {
    const container = document.getElementById("interfaceDialogClassificationList");
    if (!container) return;
    container.innerHTML = "";

    const selectedSet = new Set(selectedClassificationIds || []);
    const classifications = Array.from(state.lookup.classificationById.values())
        .sort((left, right) => String(left.code || "").localeCompare(String(right.code || ""), undefined, {
            sensitivity: "base",
            numeric: true
        }));
    const groups = classifications.filter((classification) => /^[A-Z]_?$/.test(normalizeClassificationCode(classification.code)));

    for (const groupClassification of groups) {
        const groupKey = normalizeClassificationCode(groupClassification.code).charAt(0);
        const group = document.createElement("details");
        group.className = "interfaces-dialog-classification-group-item";
        const toggle = document.createElement("summary");
        toggle.className = "interfaces-dialog-classification-toggle";
        const title = document.createElement("strong");
        title.textContent = groupClassification.code || groupClassification.id;
        const description = document.createElement("span");
        description.className = "interfaces-dialog-classification-group-description";
        description.textContent = groupClassification.description || "";
        const selectionSummary = document.createElement("span");
        selectionSummary.className = "interfaces-dialog-classification-group-selection";
        toggle.append(title, description, selectionSummary);

        const rows = classifications
            .filter((classification) => normalizeClassificationCode(classification.code).startsWith(groupKey))
            .map((classification) => buildClassificationCheckbox(classification, selectedSet));
        const updateSelectionSummary = () => {
            const selectedCodes = rows
                .map((row) => row.querySelector("input[type='checkbox']:checked"))
                .filter(Boolean)
                .map((checkbox) => state.lookup.classificationById.get(checkbox.value)?.code || checkbox.value);
            selectionSummary.textContent = selectedCodes.length ? `(${selectedCodes.join(", ")})` : "";
        };
        rows.forEach((row) => row.querySelector("input[type='checkbox']")?.addEventListener("change", updateSelectionSummary));
        updateSelectionSummary();

        group.append(toggle, ...rows);
        container.appendChild(group);
    }
}

function buildClassificationCheckbox(classification, selectedSet) {
    const row = document.createElement("label");
    row.className = "interfaces-dialog-classification-item interfaces-dialog-classification-group-row";
    const checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    checkbox.value = classification.id;
    checkbox.checked = selectedSet.has(classification.id);
    const text = document.createElement("span");
    text.className = "interfaces-dialog-classification-text";
    const codeSpan = document.createElement("strong");
    codeSpan.textContent = classification.code || classification.id;
    const descriptionSpan = document.createElement("span");
    descriptionSpan.textContent = classification.description ? ` - ${classification.description}` : "";
    text.append(codeSpan, descriptionSpan);
    row.append(checkbox, text);
    return row;
}

async function saveCurrentInterface(state) {
    const draft = getInterfaceDialogDraft(state);

    if (!draft.fromEntityId || !draft.toEntityId) {
        window.alert("Missing from/to entity id.");
        return;
    }

    if (!draft.irlId) {
        window.alert("IRL is required.");
        return;
    }

    try {
        setText("interfaceDialogStatus", "Saving...");
        const response = await fetch(`${state.basePath}?cmd=save`, {
            method: "POST",
            headers: {
                "Content-Type": "application/xml; charset=UTF-8",
                "Accept": "application/xml,text/xml,*/*"
            },
            body: buildInterfaceSavePayload(draft),
            cache: "no-store"
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status} ${response.statusText}`);
        }

        setText("interfaceDialogStatus", "Saved.");
        closeInterfaceDialog(state);
        state.onSaved?.();
    } catch (error) {
        console.error("Failed to save interface cell", error);
        setText("interfaceDialogStatus", `Save failed. ${error.message}`);
        window.alert(`Save failed. ${error.message}`);
    }
}

function getInterfaceDialogDraft(state) {
    const classificationIds = Array.from(document.querySelectorAll("#interfaceDialogClassificationList input[type='checkbox']:checked"))
        .map((checkbox) => checkbox.value)
        .filter(Boolean);

    return {
        fromEntityId: state.fromStructure?.entityId || "",
        toEntityId: state.toStructure?.entityId || "",
        irlId: document.getElementById("interfaceDialogIrlSelect")?.value || "",
        nextIrlMeeting: document.getElementById("interfaceDialogNextIrlMeeting")?.value || "",
        classificationIds
    };
}

function buildInterfaceSavePayload(draft) {
    const doc = document.implementation.createDocument("", "InterfaceMatrixSaveDocument", null);
    const matrix = doc.createElement("interfaceMatrix");
    const cell = doc.createElement("cell");
    appendXmlTextElement(doc, cell, "fromEntityId", draft.fromEntityId);
    appendXmlTextElement(doc, cell, "toEntityId", draft.toEntityId);
    appendXmlTextElement(doc, cell, "irlId", draft.irlId);
    appendXmlTextElement(doc, cell, "nextIrlMeeting", draft.nextIrlMeeting || "");
    appendXmlTextElement(doc, cell, "classificationIds", draft.classificationIds.join(","));
    matrix.appendChild(cell);
    doc.documentElement.appendChild(matrix);
    return serializeXml(doc);
}

function appendXmlTextElement(doc, parent, tagName, value) {
    const element = doc.createElement(tagName);
    element.textContent = value ?? "";
    parent.appendChild(element);
}

function closeInterfaceDialog(state) {
    state.fromStructure = null;
    state.toStructure = null;
    state.currentCell = null;
    closeDialogElement("interfaceDialog");
}

function normalizeClassificationCode(code) {
    return String(code || "").trim().toUpperCase();
}
