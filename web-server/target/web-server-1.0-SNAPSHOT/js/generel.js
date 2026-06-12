import { byId } from "./core/dom.js";
import { fetchXml, postXml } from "./core/http.js";
import { applyTopPanel, setLoadStatus } from "./core/page-header.js";
import { hideState, setEmptyState, setErrorState, setLoadingState } from "./core/placeholders.js";
import { bootstrapPage } from "./core/bootstrap-page.js";
import {
    escapeXml,
    getDirectChild,
    textOf,
    toBool
} from "./core/xml.js";
import {
    fieldControl,
    fieldEditable,
    fieldHeader,
    fieldLabel,
    fieldMaxLength,
    fieldMinLength,
    fieldRequired,
    fieldSize,
    fieldValue,
    fieldVisible,
    isCheckboxField,
    isHiddenField,
    isSelectField,
    isTextareaField
} from "./core/field-display.js";
import {
    parseDateTime,
    toDateInputValue,
    toDateTimeInputValue
} from "./core/date.js";
import {
    bindSortableHeaders,
    compareSortableValues,
    nextSortState,
    applySortIndicators
} from "./components/sortable-table.js";
import { validateFieldsFromDetailNode } from "./core/validation.js";

function fieldWidth(field) {
    return field.getAttribute("tableWidth") || "auto";
}

function fieldOrder(field, index) {
    const raw = field.getAttribute("displayOrder");
    const parsed = Number(raw);

    return Number.isFinite(parsed) ? parsed : index;
}

function selectDisplayValue(field) {
    const selectedOption = Array.from(field?.getElementsByTagName("Option") || []).find(
        (opt) => toBool(opt.getAttribute("selected"))
    );

    if (selectedOption) {
        return (selectedOption.textContent || "").trim();
    }

    const valueNode = field?.getElementsByTagName("Value")?.[0];

    if (valueNode) {
        return (valueNode.textContent || "").trim();
    }

    return fieldValue(field);
}

function createFieldMarkup(field) {
    const name = field.tagName;
    const label = fieldLabel(field);
    const editable = fieldEditable(field);
    const visible = fieldVisible(field);
    const required = fieldRequired(field);
    const value = fieldValue(field);
    const control = fieldControl(field);
    const minLen = fieldMinLength(field);
    const maxLen = fieldMaxLength(field);
    const size = fieldSize(field);

    const requiredStar = required ? `<span class="field-required" aria-hidden="true">*</span>` : "";
    const requiredAttr = required ? "required" : "";
    const labelMarkup = `${escapeXml(label)}${requiredStar}`;

    if (isHiddenField(field)) {
        return `<input type="hidden" data-field="${name}" id="fld-${name}" value="${escapeXml(value)}">`;
    }

    if (!visible) {
        return "";
    }

    const readonlyAttr = editable ? "" : "readonly";
    const disabledAttr = editable ? "" : "disabled";
    const readOnlyClass = editable ? "" : " is-readonly";

    const minlengthAttr = minLen !== null ? `minlength="${minLen}"` : "";
    const maxlengthAttr = maxLen !== null ? `maxlength="${maxLen}"` : "";
    const sizeAttr = size !== null ? `size="${size}"` : "";

    if (isCheckboxField(field)) {
        const checked = toBool(value) ? "checked" : "";

        return `
            <div class="page-field checkbox-field${readOnlyClass}">
                <input id="fld-${name}" data-field="${name}" type="checkbox" ${checked} ${disabledAttr} ${requiredAttr} />
                <label for="fld-${name}">${labelMarkup}</label>
            </div>
        `;
    }

    if (isTextareaField(field)) {
        return `
            <div class="page-field${readOnlyClass}">
                <label for="fld-${name}">${labelMarkup}</label>
                <textarea id="fld-${name}" data-field="${name}" ${readonlyAttr} ${requiredAttr} ${minlengthAttr} ${maxlengthAttr}>${escapeXml(value)}</textarea>
            </div>
        `;
    }

    if (isSelectField(field)) {
        const options = Array.from(field.getElementsByTagName("Option"));
        const selectedValue = (field.getElementsByTagName("Value")?.[0]?.textContent || "").trim();

        return `
            <div class="page-field${readOnlyClass}">
                <label for="fld-${name}">${labelMarkup}</label>
                <select id="fld-${name}" data-field="${name}" ${disabledAttr} ${requiredAttr}>
                    ${options.map((option) => {
            const optionValue = option.getAttribute("value") || "";
            const optionLabel = (option.textContent || "").trim();
            const selected = optionValue === selectedValue ? "selected" : "";

            return `<option value="${escapeXml(optionValue)}" ${selected}>${escapeXml(optionLabel)}</option>`;
        }).join("")}
                </select>
            </div>
        `;
    }

    if (control === "date") {
        return `
            <div class="page-field${readOnlyClass}">
                <label for="fld-${name}">${labelMarkup}</label>
                <input id="fld-${name}" data-field="${name}" type="date" value="${escapeXml(toDateInputValue(value))}" ${readonlyAttr} ${requiredAttr} />
            </div>
        `;
    }

    if (control === "datetime") {
        return `
            <div class="page-field${readOnlyClass}">
                <label for="fld-${name}">${labelMarkup}</label>
                <input id="fld-${name}" data-field="${name}" type="datetime-local" value="${escapeXml(toDateTimeInputValue(value))}" ${readonlyAttr} ${requiredAttr} />
            </div>
        `;
    }

    return `
        <div class="page-field${readOnlyClass}">
            <label for="fld-${name}">${labelMarkup}</label>
            <input id="fld-${name}" data-field="${name}" type="text" value="${escapeXml(value)}" ${readonlyAttr} ${requiredAttr} ${minlengthAttr} ${maxlengthAttr} ${sizeAttr} />
        </div>
    `;
}

function createActiveIcon(value) {
    return value
        ? `<span class="active-icon is-active" title="Active" aria-label="Active">✅</span>`
        : `<span class="active-icon is-inactive" title="Inactive" aria-label="Inactive">⛔</span>`;
}

function createRowValue(column, row) {
    const value = row?.[column.name];

    if (column.name === "Active") {
        return createActiveIcon(Boolean(value));
    }

    if (column.control === "datetime" || column.control === "date") {
        return parseDateTime(value);
    }

    return value ?? "";
}

function buildColumnsFromNode(detailNode) {
    const children = Array.from(detailNode?.children || []);

    const columns = children
        .map((field, index) => ({
            name: field.tagName,
            label: fieldHeader(field),
            control: fieldControl(field) || "text",
            editable: fieldEditable(field),
            visible: fieldVisible(field),
            tableWidth: fieldWidth(field),
            displayOrder: fieldOrder(field, index)
        }))
        .filter((column) => column.visible || column.name === "Active")
        .sort((a, b) => a.displayOrder - b.displayOrder);

    const activeIndex = columns.findIndex((c) => c.name === "Active");

    if (activeIndex >= 0 && activeIndex !== columns.length - 1) {
        const [active] = columns.splice(activeIndex, 1);
        columns.push(active);
    }

    return columns;
}

function buildColGroupMarkup(columns) {
    return columns.map((column) => `<col style="width:${column.tableWidth};">`).join("");
}

function buildHeaderMarkup(columns) {
    return columns.map((column) => {
        if (column.name === "Active") {
            return `
                <th data-key="Active" class="page-active-header">
                    <span class="sort">Active <span class="sort-indicator" id="si-Active"></span></span>
                    <label class="page-active-only" title="When selected, only active rows are shown">
                        <input id="activeOnly" type="checkbox" checked />
                    </label>
                </th>
            `;
        }

        return `
            <th data-key="${column.name}">
                <span class="sort">${column.label} <span class="sort-indicator" id="si-${column.name}"></span></span>
            </th>
        `;
    }).join("");
}

function extractRowFromEntityNode(node) {
    const row = {
        entityId: textOf(node, "EntityId"),
        active: toBool(textOf(node, "Active"))
    };

    Array.from(node?.children || []).forEach((field) => {
        const name = field.tagName;
        const control = fieldControl(field);

        if (control === "hidden") {
            row[name] = fieldValue(field);
            return;
        }

        if (control === "checkbox") {
            row[name] = toBool(field.textContent);
            return;
        }

        if (control === "select") {
            row[name] = selectDisplayValue(field);
            row[`${name}Value`] = (field.getElementsByTagName("Value")?.[0]?.textContent || "").trim();
            return;
        }

        row[name] = fieldValue(field);
    });

    return row;
}

function validateDialog(elements, detailNode) {
    if (!detailNode) {
        return ["No detail XML returned."];
    }

    return validateFieldsFromDetailNode(detailNode, elements.basisInfoFields);
}

export function createGenericPage({
                                      listUrl,
                                      detailUrl,
                                      saveUrl,
                                      rootListTag,
                                      listEntityTag,
                                      detailEntityTag,
                                      detailContainerTag = "systemBreakdownDocument",
                                      findDetailNode,
                                      afterDetailLoad,
                                      buildSavePayload: buildSavePayloadOverride,
                                      afterListLoad
                                  }) {
    let allRows = [];
    let currentDoc = null;
    let currentDetailNode = null;
    let currentColumns = [];
    let sortState = { key: null, dir: "asc" };
    let reloadPage = async function () {};
    let dirty = false;

    function getElements() {
        return {
            customerName: byId("customerName"),
            projectName: byId("projectName"),
            userName: byId("userName"),
            loadStatus: byId("loadStatus"),
            emptyState: byId("emptyState"),
            tbody: byId("tbody"),
            mainColGroup: byId("mainColGroup"),
            mainHeaderRow: byId("mainHeaderRow"),
            activeOnly: byId("activeOnly"),
            editDialog: byId("editDialog"),
            btnAddNew: byId("btnAddNew"),
            btnDlgClose: byId("btnDlgClose"),
            btnCancel: byId("btnCancel"),
            btnSave: byId("btnSave"),
            dlgStatus: byId("dlgStatus"),
            basisInfoFields: byId("basisInfoFields"),
            topPanel: byId("topPanel"),
            historyBody: byId("historyBody"),
            historyEmpty: byId("historyEmpty"),
            noteBody: byId("noteBody"),
            noteEmpty: byId("noteEmpty")
        };
    }

    function compareValues(a, b, key) {
        return compareSortableValues(a?.[key], b?.[key], { locale: "en" });
    }

    function setMainIndicators() {
        applySortIndicators(
            currentColumns.map((c) => c.name),
            sortState,
            "si-"
        );
    }

    function renderList(elements) {
        const visibleRows = allRows.filter((row) => !elements.activeOnly?.checked || row.active);

        elements.tbody.innerHTML = "";

        if (!visibleRows.length) {
            setEmptyState(
                elements.emptyState,
                allRows.length ? "No rows match the current filter." : "No rows returned from the web service."
            );
            return;
        }

        hideState(elements.emptyState);

        visibleRows.forEach((row) => {
            const tr = document.createElement("tr");
            tr.innerHTML = currentColumns.map((column) => `<td>${createRowValue(column, row)}</td>`).join("");
            tr.addEventListener("dblclick", () => openEditor(row.entityId, elements));
            elements.tbody.appendChild(tr);
        });
    }

    function sortListBy(key, elements) {
        sortState = nextSortState(sortState, key);

        allRows.sort((a, b) => {
            const result = compareValues(a, b, key);
            return sortState.dir === "asc" ? result : -result;
        });

        setMainIndicators();
        renderList(elements);
    }

    function renderBasisInfo(detailNode, elements) {
        if (!detailNode) {
            elements.basisInfoFields.innerHTML = '<div class="page-empty">No detail XML returned.</div>';
            return;
        }

        elements.basisInfoFields.innerHTML = Array.from(detailNode.children || [])
            .map(createFieldMarkup)
            .join("");
    }

    function applyListXml(doc, elements) {
        const root = doc.getElementsByTagName(rootListTag)[0] || doc;
        const topPanel = getDirectChild(root, "TopPanel");
        const listNode = getDirectChild(root, listEntityTag);

        applyTopPanel(topPanel, elements, { userTagNames: ["Name", "UserName"] });

        allRows = Array.from(listNode?.getElementsByTagName(detailEntityTag) || []).map(extractRowFromEntityNode);

        const firstRowNode = listNode?.getElementsByTagName(detailEntityTag)?.[0] || null;

        if (firstRowNode) {
            currentColumns = buildColumnsFromNode(firstRowNode);
            elements.mainColGroup.innerHTML = buildColGroupMarkup(currentColumns);
            elements.mainHeaderRow.innerHTML = buildHeaderMarkup(currentColumns);
            elements.activeOnly = byId("activeOnly");
        }

        sortState = { key: null, dir: "asc" };
        setMainIndicators();
        renderList(elements);
        setLoadStatus(elements.loadStatus, "Loaded");

        if (typeof afterListLoad === "function") {
            afterListLoad(doc, elements, { rowCount: allRows.length });
        }
    }

    function applyDetailXml(doc, elements, entityIdForStatus = "") {
        const root = doc.documentElement || doc;
        const topPanel = getDirectChild(root, "TopPanel");

        const detailContainer = getDirectChild(root, detailContainerTag);
        const detailNode = detailContainer
            ? getDirectChild(detailContainer, detailEntityTag)
            : getDirectChild(root, detailEntityTag);

        currentDoc = doc;
        currentDetailNode = detailNode;

        applyTopPanel(topPanel, elements, { userTagNames: ["Name", "UserName"] });
        renderBasisInfo(detailNode, elements);

        if (typeof afterDetailLoad === "function") {
            afterDetailLoad(doc, elements, {
                entityId: entityIdForStatus,
                detailNode,
                root
            });
        }

        elements.dlgStatus.textContent = entityIdForStatus ? `Editing system #${entityIdForStatus}` : "Creating new system";
    }

    function buildSavePayload(elements) {
        const validationErrors = validateDialog(elements, currentDetailNode);

        if (validationErrors.length) {
            throw new Error(validationErrors.join("\n"));
        }

        if (typeof buildSavePayloadOverride === "function") {
            const payload = buildSavePayloadOverride(elements, {
                currentDoc,
                currentDetailNode,
                detailEntityTag
            });

            if (!payload || typeof payload !== "string") {
                throw new Error("buildSavePayload must return an XML string.");
            }

            return payload;
        }

        const fieldNodes = currentDetailNode ? Array.from(currentDetailNode.children || []) : [];

        const valuesXml = fieldNodes.map((field) => {
            const name = field.tagName;
            const control = fieldControl(field);
            const uiField = elements.basisInfoFields.querySelector(`[data-field="${name}"]`);

            if (control === "hidden") {
                return `<${name}>${escapeXml(fieldValue(field))}</${name}>`;
            }

            if (!uiField) {
                return `<${name}>${escapeXml(fieldValue(field))}</${name}>`;
            }

            if (control === "select") {
                const selectedOption = uiField.selectedOptions?.[0] || null;
                const selectedValue = (selectedOption?.value || "").trim();
                return `<${name}><Value>${escapeXml(selectedValue)}</Value></${name}>`;
            }

            let value;

            if (uiField.type === "checkbox") {
                value = uiField.checked ? "true" : "false";
            } else if (uiField.type === "date") {
                value = uiField.value || "";
            } else if (uiField.type === "datetime-local") {
                value = uiField.value || "";
            } else {
                value = uiField.value ?? "";
            }

            return `<${name}>${escapeXml(value)}</${name}>`;
        }).join("");

        return `<${detailEntityTag}>${valuesXml}</${detailEntityTag}>`;
    }

    async function openEditor(id, elements) {
        if (elements.editDialog.open && dirty && !window.confirm("Du har ændringer, der ikke er gemt. Vil du fortsætte og kassere dem?")) {
            return;
        }

        dirty = false;
        currentDoc = null;
        elements.dlgStatus.textContent = "Loading…";
        elements.editDialog.showModal();

        try {
            const doc = await fetchXml(detailUrl + encodeURIComponent(id || ""));
            applyDetailXml(doc, elements, id || "");
        } catch (error) {
            console.error("Failed to load detail XML:", error);
            elements.dlgStatus.textContent = "Failed to load details.";
            setErrorState(elements.emptyState, `Failed to load XML from ${detailUrl}.`);
        }
    }

    async function saveEditor(elements) {
        elements.dlgStatus.textContent = "Saving…";

        try {
            const payload = buildSavePayload(elements);
            await postXml(saveUrl, payload);

            elements.dlgStatus.textContent = "Saved.";
            dirty = false;
            elements.editDialog.close();
            await reloadPage();
        } catch (error) {
            console.error("Failed to save:", error);
            elements.dlgStatus.textContent = "Save failed.";

            if (error?.message) {
                window.alert(error.message);
            }
        }
    }

    async function loadList(elements) {
        try {
            const doc = await fetchXml(listUrl);
            applyListXml(doc, elements);
        } catch (error) {
            setLoadStatus(elements.loadStatus, "Error");
            setErrorState(elements.emptyState, `Failed to load XML from ${listUrl}.`);
            console.error("Failed to load list:", error);
            throw error;
        }
    }

    function bindEvents(elements) {
        elements.activeOnly?.addEventListener("change", () => renderList(elements));
        elements.btnAddNew?.addEventListener("click", () => openEditor("", elements));

        elements.btnDlgClose?.addEventListener("click", () => {
            if (dirty && !window.confirm("Du har ændringer, der ikke er gemt. Vil du fortsætte og kassere dem?")) {
                return;
            }

            elements.editDialog.close();
        });

        elements.btnCancel?.addEventListener("click", () => {
            if (dirty && !window.confirm("Du har ændringer, der ikke er gemt. Vil du fortsætte og kassere dem?")) {
                return;
            }

            elements.editDialog.close();
        });

        elements.btnSave?.addEventListener("click", () => saveEditor(elements));

        bindSortableHeaders(elements.mainHeaderRow, (key) => sortListBy(key, elements));

        elements.editDialog.addEventListener("input", () => {
            dirty = true;
        });
    }

    function beforeLoad(elements) {
        setLoadStatus(elements.loadStatus, "Loading…");
        setLoadingState(elements.emptyState, "Loading XML from web service…");
    }

    function afterLoad(elements) {
        setLoadStatus(elements.loadStatus, "Loaded");
    }

    function onError(error, elements) {
        setLoadStatus(elements.loadStatus, "Error");
        setErrorState(elements.emptyState, `Failed to load XML from ${listUrl}.`);
        console.error(error);
    }

    async function start() {
        const elements = getElements();

        const page = await bootstrapPage({
            getElements: () => elements,
            initialize: async () => {},
            initializeMenu: false,
            beforeLoad,
            load: loadList,
            afterLoad,
            onError
        });

        reloadPage = page.reload;
        bindEvents(elements);
    }

    return { start, applyListXml, applyDetailXml, loadList, openEditor };
}