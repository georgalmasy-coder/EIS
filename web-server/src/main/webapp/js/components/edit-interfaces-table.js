import { getAttribute, getChildText } from "../core/xml.js";
import { openInterfaceEditDialog, resolveInterfaceBasePath } from "./interface-edit-dialog.js";

const COLUMN_DEFINITIONS = [
    { key: "interfaceClass", label: "Class", sublabel: "From -> To", type: "classification", width: "30%" },
    { key: "interfaceIrl", label: "IRL", sublabel: "From -> To", type: "irl", width: "18%" },
    { key: "nextIrlMeeting", label: "Next IRL Meeting", source: "nextIrlMeeting", width: "18%", formatter: formatDanishDate },
    { key: "toEntityCode", label: "ID", source: "toEntityCode", width: "14%" },
    { key: "toEntityName", label: "Name", source: "toEntityName", width: "20%" }
];

export function createEditInterfacesTable(options = {}) {
    const tableBodyId = options.tableBodyId || "interfacesBody";
    const emptyId = options.emptyId || "interfacesEmpty";
    const countId = options.countId || "interfacesCount";
    let currentData = null;
    let currentBasePath = "";
    let currentStructureLabel = "";

    document.addEventListener("dblclick", (event) => {
        const row = event.target?.closest?.(`#${tableBodyId} .edit-interfaces-row`);
        if (!row || !currentData) {
            return;
        }

        const index = Number(row.dataset.interfaceIndex);
        const record = Number.isInteger(index) ? currentData.interfaces[index] : null;
        if (record) {
            openRecord(record, currentData, currentBasePath, currentStructureLabel);
        }
    });

    function loadFromDocument(xmlDocument) {
        const rootElement = xmlDocument?.querySelector("editInterfaces");
        const tableBody = document.getElementById(tableBodyId);
        const emptyState = document.getElementById(emptyId);
        const countElement = document.getElementById(countId);

        if (!tableBody) {
            return;
        }

        const data = parseEditInterfaces(rootElement, xmlDocument);
        const basePath = options.basePath || resolveInterfaceBasePath(data.entityType);
        const structureLabel = options.structureLabel || data.entityTypeName || "Physical Architecture";
        currentData = data;
        currentBasePath = basePath;
        currentStructureLabel = structureLabel;
        const fragment = document.createDocumentFragment();

        data.interfaces.forEach((record, index) => {
            const row = document.createElement("tr");
            row.className = "edit-interfaces-row";
            row.dataset.interfaceIndex = String(index);
            row.title = "Double-click to edit interface";
            row.tabIndex = 0;
            row.addEventListener("dblclick", (event) => {
                event.preventDefault();
                event.stopPropagation();
                openRecord(record, data, basePath, structureLabel);
            });

            for (const column of COLUMN_DEFINITIONS) {
                row.appendChild(column.type ? buildDualCell(record, column, data.lookup) : buildSingleCell(record, column));
            }

            fragment.appendChild(row);
        });

        tableBody.replaceChildren(fragment);

        if (emptyState) {
            emptyState.hidden = data.interfaces.length > 0;
        }

        if (countElement) {
            countElement.textContent = `${data.interfaces.length}`;
        }
    }

    return {
        loadFromDocument
    };

    function openRecord(record, data, basePath, structureLabel) {
        openInterfaceEditDialog({
            basePath,
            structureLabel,
            lookup: data.lookup,
            fromStructure: {
                entityId: data.entityId || record.fromEntityId,
                id: data.fromEntityCode || data.entityId || record.fromEntityId,
                name: data.fromEntityName || ""
            },
            toStructure: {
                entityId: record.toEntityId,
                id: record.toEntityCode,
                name: record.toEntityName
            },
            cell: {
                irlId: record.fromIrlId,
                classificationIds: splitIds(record.fromClassificationIds),
                nextIrlMeeting: record.nextIrlMeeting
            },
            onSaved: options.onSaved || (() => window.location.reload())
        });
    }
}

function parseEditInterfaces(rootElement, xmlDocument) {
    if (!rootElement) {
        return {
            entityId: "",
            entityType: "",
            entityTypeName: "",
            fromEntityCode: "",
            fromEntityName: "",
            interfaces: [],
            lookup: {
                irlById: new Map(),
                classificationById: new Map()
            }
        };
    }

    const entityType = getAttribute(rootElement, "entityType", "");
    const currentEntity = parseCurrentEntity(xmlDocument, entityType);

    return {
        entityId: getAttribute(rootElement, "entityId", "") || currentEntity.entityId,
        entityType,
        entityTypeName: getAttribute(rootElement, "entityTypeName", ""),
        fromEntityCode: currentEntity.code,
        fromEntityName: currentEntity.name,
        interfaces: Array.from(rootElement.querySelectorAll(":scope > interfaces > interface")).map((element) => ({
            fromIrlId: getChildText(element, "fromIrlId", ""),
            fromClassificationIds: getChildText(element, "fromClassificationIds", ""),
            toIrlId: getChildText(element, "toIrlId", ""),
            toClassificationIds: getChildText(element, "toClassificationIds", ""),
            nextIrlMeeting: getChildText(element, "nextIrlMeeting", ""),
            toEntityId: getChildText(element, "toEntityId", ""),
            toEntityCode: getChildText(element, "toEntityCode", ""),
            toEntityName: getChildText(element, "toEntityName", "")
        })),
        lookup: {
            irlById: parseLookupMap(rootElement, "irlMeta > irl", "irlId"),
            classificationById: parseLookupMap(rootElement, "classificationMeta > classification", "classId")
        }
    };
}

function parseCurrentEntity(xmlDocument, entityType) {
    const detailNode = findCurrentEntityNode(xmlDocument, entityType);
    const fieldNames = getCurrentEntityFieldNames(entityType);

    return {
        entityId: getChildText(detailNode, "EntityId", ""),
        code: getFirstChildText(detailNode, fieldNames.code),
        name: getFirstChildText(detailNode, fieldNames.name)
    };
}

function findCurrentEntityNode(xmlDocument, entityType) {
    const selectorsByEntityType = {
        "2": ["SystemBreakdownInfo > systembreakdowns > systembreakdown", "systembreakdowns > systembreakdown", "systembreakdown"],
        "5": ["StakeholderRequirementInfo > stakeholderRequirements > stakeholderRequirement", "stakeholderRequirements > stakeholderRequirement", "stakeholderRequirement"],
        "6": ["SystemRequirementInfo > systemRequirements > systemRequirement", "systemRequirements > systemRequirement", "systemRequirement"],
        "8": ["LogicalStructureInfo > logicalStructures > logicalDocument", "logicalStructures > logicalDocument", "logicalDocument", "logicalStructure"],
        "9": ["FunctionalStructureInfo > functionStructures > functionalDocument", "functionStructures > functionalDocument", "functionalDocument", "functionStructure"]
    };

    for (const selector of selectorsByEntityType[String(entityType)] || []) {
        const node = xmlDocument?.querySelector?.(selector);
        if (node?.querySelector?.("EntityId")) {
            return node;
        }
    }

    return null;
}

function getCurrentEntityFieldNames(entityType) {
    const fieldNamesByEntityType = {
        "2": { code: ["SBSCode", "SystemCode", "SystemBreakdownCode"], name: ["SystemName", "SystemBreakdownName", "Name"] },
        "5": { code: ["StakeholderReqCode", "BasisReqCode"], name: ["RequirementName", "Name"] },
        "6": { code: ["SystemReqCode"], name: ["RequirementName", "Name"] },
        "8": { code: ["LogicalCode"], name: ["LogicalName", "Name"] },
        "9": { code: ["FunctionalCode", "FunctionCode"], name: ["FunctionalName", "FunctionName", "FunctionDescription", "Name"] }
    };

    return fieldNamesByEntityType[String(entityType)] || { code: [], name: [] };
}

function getFirstChildText(parentElement, tagNames) {
    for (const tagName of tagNames || []) {
        const value = getChildText(parentElement, tagName, "");
        if (value) {
            return value;
        }
    }

    return "";
}

function splitIds(rawValue) {
    return String(rawValue || "")
        .split(",")
        .map((value) => value.trim())
        .filter(Boolean);
}

function parseLookupMap(parentElement, selector, idAttribute) {
    const map = new Map();

    for (const element of parentElement.querySelectorAll(`:scope > ${selector}`)) {
        const id = getAttribute(element, idAttribute, "");

        if (!id) {
            continue;
        }

        map.set(id, {
            id,
            code: getAttribute(element, "code", id),
            description: getAttribute(element, "description", ""),
            color: getAttribute(element, "color", "")
        });
    }

    return map;
}

function buildSingleCell(record, column) {
    const cell = document.createElement("td");
    cell.className = `edit-interfaces-cell edit-interfaces-cell--${column.key}`;

    const rawValue = String(record?.[column.source] || "").trim();
    const formattedValue = column.formatter ? column.formatter(rawValue) : rawValue;
    const text = document.createElement("span");
    text.className = "edit-interfaces-cell-text";
    text.textContent = formattedValue || "--";
    cell.title = formattedValue || "--";
    cell.appendChild(text);

    return cell;
}

function buildDualCell(record, column, lookup) {
    const cell = document.createElement("td");
    cell.className = `edit-interfaces-cell edit-interfaces-cell--dual edit-interfaces-cell--${column.key}`;

    const topRaw = column.type === "classification" ? record.fromClassificationIds : record.fromIrlId;
    const bottomRaw = column.type === "classification" ? record.toClassificationIds : record.toIrlId;
    const topResolved = column.type === "classification"
        ? resolveLookupList(lookup.classificationById, topRaw)
        : resolveLookupValue(lookup.irlById, topRaw);
    const bottomResolved = column.type === "classification"
        ? resolveLookupList(lookup.classificationById, bottomRaw)
        : resolveLookupValue(lookup.irlById, bottomRaw);

    const wrapper = document.createElement("div");
    wrapper.className = "edit-interfaces-dual";
    wrapper.appendChild(buildDualLine(topResolved, "right"));
    wrapper.appendChild(buildDualLine(bottomResolved, "left"));
    cell.title = `${topResolved.title || topResolved.label || "--"}\n${bottomResolved.title || bottomResolved.label || "--"}`;
    cell.appendChild(wrapper);

    return cell;
}

function buildDualLine(resolved, direction) {
    const line = document.createElement("div");
    line.className = "edit-interfaces-dual-line";
    line.appendChild(buildArrow(direction));
    line.appendChild(resolved.color ? buildLookupPill(resolved) : buildDualText(resolved));
    return line;
}

function buildDualText(resolved) {
    const text = document.createElement("span");
    text.className = "edit-interfaces-dual-text";
    text.textContent = resolved.label || "--";
    return text;
}

function buildLookupPill(resolved) {
    const pill = document.createElement("span");
    pill.className = "edit-interfaces-pill";
    pill.textContent = resolved.label || "--";
    pill.title = resolved.title || resolved.label || "--";

    if (resolved.color) {
        pill.style.setProperty("--edit-interfaces-pill-color", resolved.color);
    }

    return pill;
}

function buildArrow(direction = "right") {
    const arrow = document.createElement("span");
    arrow.className = `edit-interfaces-dual-arrow edit-interfaces-dual-arrow--${direction}`;
    arrow.textContent = direction === "left" ? "\u2190" : "\u2192";
    return arrow;
}

function resolveLookupValue(map, id) {
    const normalizedId = String(id || "").trim();

    if (!normalizedId) {
        return { label: "--", title: "--", color: "" };
    }

    const lookup = map?.get(normalizedId);

    return {
        label: lookup?.code || normalizedId,
        title: lookup?.description || lookup?.code || normalizedId,
        color: lookup?.color || ""
    };
}

function resolveLookupList(map, rawValue) {
    const ids = String(rawValue || "")
        .split(",")
        .map((value) => value.trim())
        .filter(Boolean);

    if (!ids.length) {
        return { label: "--", title: "--", color: "" };
    }

    const items = ids.map((id) => resolveLookupValue(map, id));

    return {
        label: items.map((item) => item.label).join(", "),
        title: items.map((item) => item.title).join(", "),
        color: ""
    };
}

function formatDanishDate(value) {
    const match = String(value || "").trim().match(/^(\d{4})-(\d{2})-(\d{2})$/);
    return match ? `${match[3]}/${match[2]}-${match[1]}` : String(value || "").trim();
}
