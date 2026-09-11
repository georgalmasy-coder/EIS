import { getAttribute, getChildText } from "../core/xml.js";

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

    function loadFromDocument(xmlDocument) {
        const rootElement = xmlDocument?.querySelector("editInterfaces");
        const tableBody = document.getElementById(tableBodyId);
        const emptyState = document.getElementById(emptyId);
        const countElement = document.getElementById(countId);

        if (!tableBody) {
            return;
        }

        const data = parseEditInterfaces(rootElement);
        const fragment = document.createDocumentFragment();

        for (const record of data.interfaces) {
            const row = document.createElement("tr");
            row.className = "edit-interfaces-row";

            for (const column of COLUMN_DEFINITIONS) {
                row.appendChild(column.type ? buildDualCell(record, column, data.lookup) : buildSingleCell(record, column));
            }

            fragment.appendChild(row);
        }

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
}

function parseEditInterfaces(rootElement) {
    if (!rootElement) {
        return {
            interfaces: [],
            lookup: {
                irlById: new Map(),
                classificationById: new Map()
            }
        };
    }

    return {
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
