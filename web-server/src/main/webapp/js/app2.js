(function () {
    "use strict";

    const DATA_URL = "/project/overview";

    let notifications = [];
    let notificationSortState = { key: "createdTime", dir: "asc" };

    const NOTIFICATION_COLUMNS = [
        { key: "notificationText", tag: "NotificationText" },
        { key: "createdBy", tag: "CreatedById" },
        { key: "createdTime", tag: "CreatedTime" },
        { key: "acknowledgeTime", tag: "AcknowledgeTime" }
    ];

    const elCustomerName = document.getElementById("customerName");
    const elProjectName = document.getElementById("projectName");
    const elUserName = document.getElementById("userName");
    const elLoadStatus = document.getElementById("loadStatus");

    const elProjectFields = document.getElementById("projectFields");
    const elNotificationsHead = document.getElementById("notificationsHead");
    const elNotificationsBody = document.getElementById("notificationsBody");
    const elNotificationsEmpty = document.getElementById("notificationsEmpty");
    const elIrlSlices = document.getElementById("irlSlices");
    const elIrlLegend = document.getElementById("irlLegend");

    function textOf(parent, tagName) {
        if (!parent) return "";
        const el = parent.getElementsByTagName(tagName)[0];
        return el ? (el.textContent || "").trim() : "";
    }

    function parseXml(xmlText) {
        const parser = new DOMParser();
        const doc = parser.parseFromString(xmlText, "application/xml");

        if (doc.getElementsByTagName("parsererror").length) {
            throw new Error("Invalid XML returned by service.");
        }

        return doc;
    }

    function getDirectChild(parent, tagName) {
        if (!parent) return null;
        return Array.from(parent.children).find((child) => child.tagName === tagName) || null;
    }

    function getDirectChildren(parent, tagName) {
        if (!parent) return [];
        return Array.from(parent.children).filter((child) => child.tagName === tagName);
    }

    function isVisibleField(node) {
        const visible = (node.getAttribute("visible") || "").toLowerCase();
        const control = (node.getAttribute("control") || "").toLowerCase();
        return visible !== "false" && control !== "hidden";
    }

    function fieldLabel(node) {
        return node.getAttribute("label") || node.tagName;
    }

    function fieldHeader(node) {
        if (!node) return "";
        return node.getAttribute("header") || node.getAttribute("label") || node.tagName;
    }

    function fieldDisplayValue(node) {
        if (!node) return "";

        const control = (node.getAttribute("control") || "").toLowerCase();

        if (control === "select") {
            const currentValue = textOf(node, "Value");
            const options = Array.from(node.getElementsByTagName("Option"));
            const selectedOption =
                options.find((option) => (option.getAttribute("selected") || "").toLowerCase() === "true") ||
                options.find((option) => (option.getAttribute("value") || "") === currentValue);

            return selectedOption ? (selectedOption.textContent || "").trim() : currentValue;
        }

        if (control === "checkbox") {
            return ((node.textContent || "").trim().toLowerCase() === "true") ? "Yes" : "No";
        }

        return (node.textContent || "").trim();
    }

    function getDisplayLength(node, fallback) {
        const raw = node.getAttribute("displayLength");
        const parsed = Number(raw);
        return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
    }

    function applyControlWidth(control, node) {
        const controlTag = control.tagName;
        const controlType = (node.getAttribute("control") || "").toLowerCase();

        if (controlType === "checkbox" || controlType === "radio") {
            control.style.width = "auto";
            control.style.maxWidth = "none";
            return;
        }

        if (controlTag === "TEXTAREA") {
            const cols = Number(node.getAttribute("cols"));
            const width = Number.isFinite(cols) && cols > 0 ? cols : getDisplayLength(node, 35);
            control.style.width = width + "ch";
            control.style.maxWidth = "100%";
            return;
        }

        const width = getDisplayLength(node, controlTag === "SELECT" ? 15 : 20);
        control.style.width = width + "ch";
        control.style.maxWidth = "100%";
    }

    function applyCommonFieldAttributes(control, node) {
        const editable = (node.getAttribute("editable") || "").toLowerCase() === "true";
        const required = (node.getAttribute("required") || "").toLowerCase() === "true";
        const maxLength = node.getAttribute("maxLength");
        const minLength = node.getAttribute("minLength");

        control.name = node.tagName;
        control.id = "projectField_" + node.tagName;

        if (!editable) {
            control.disabled = true;
        }

        if (required) {
            control.required = true;
        }

        if (maxLength && "maxLength" in control) {
            control.maxLength = Number(maxLength);
        }

        if (minLength && "minLength" in control) {
            control.minLength = Number(minLength);
        }

        applyControlWidth(control, node);

        if (control.tagName === "INPUT" || control.tagName === "SELECT" || control.tagName === "TEXTAREA") {
            control.classList.add("dlg-input");
        }
    }

    function toDateInputValue(rawValue) {
        if (!rawValue) return "";
        const trimmed = rawValue.trim();

        if (/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) {
            return trimmed;
        }

        const match = trimmed.match(/^(\d{2})\/(\d{2})-(\d{4})$/);
        if (!match) return "";

        const day = match[1];
        const month = match[2];
        const year = match[3];

        return `${year}-${month}-${day}`;
    }

    function toDateTimeLocalValue(rawValue) {
        if (!rawValue) return "";
        const trimmed = rawValue.trim();

        if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/.test(trimmed)) {
            return trimmed;
        }

        if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(trimmed)) {
            return `${trimmed}:00`;
        }

        const match = trimmed.match(/^(\d{2})\/(\d{2})-(\d{4})\s+(\d{2}):(\d{2})(?::(\d{2}))?$/);
        if (!match) return "";

        const day = match[1];
        const month = match[2];
        const year = match[3];
        const hour = match[4];
        const minute = match[5];
        const second = match[6] || "00";

        return `${year}-${month}-${day}T${hour}:${minute}:${second}`;
    }

    function toTimeInputValue(rawValue) {
        if (!rawValue) return "";
        const trimmed = rawValue.trim();

        if (/^\d{2}:\d{2}$/.test(trimmed)) {
            return trimmed;
        }

        const match = trimmed.match(/^(\d{2}):(\d{2})(?::\d{2})?$/);
        if (!match) return "";

        return `${match[1]}:${match[2]}`;
    }

    function formatLocalDateTime(rawValue) {
        if (!rawValue) return "—";

        const trimmed = rawValue.trim();
        const normalized = trimmed.length === 19 ? trimmed.replace(" ", "T") : trimmed;
        const date = new Date(normalized);

        if (Number.isNaN(date.getTime())) {
            return rawValue;
        }

        return new Intl.DateTimeFormat("da-DK", {
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit"
        }).format(date);
    }

    function createProjectFieldControl(node) {
        const controlType = (node.getAttribute("control") || "text").toLowerCase();
        const rawValue = (node.textContent || "").trim();

        let control;

        switch (controlType) {
            case "textarea": {
                control = document.createElement("textarea");
                control.value = rawValue;
                control.rows = Number(node.getAttribute("rows") || "4");

                if (node.getAttribute("cols")) {
                    control.cols = Number(node.getAttribute("cols"));
                }

                applyCommonFieldAttributes(control, node);
                return control;
            }

            case "select": {
                control = document.createElement("select");
                applyCommonFieldAttributes(control, node);

                const currentValue = textOf(node, "Value");
                const options = Array.from(node.getElementsByTagName("Option"));

                options.forEach((optionNode) => {
                    const option = document.createElement("option");
                    option.value = optionNode.getAttribute("value") || "";
                    option.textContent = (optionNode.textContent || "").trim();

                    if (
                        (optionNode.getAttribute("selected") || "").toLowerCase() === "true" ||
                        option.value === currentValue
                    ) {
                        option.selected = true;
                    }

                    control.appendChild(option);
                });

                return control;
            }

            case "checkbox": {
                control = document.createElement("input");
                control.type = "checkbox";
                control.checked = rawValue.toLowerCase() === "true";

                applyCommonFieldAttributes(control, node);
                control.classList.remove("dlg-input");
                control.classList.add("project-checkbox");

                return control;
            }

            case "number": {
                control = document.createElement("input");
                control.type = "number";
                control.value = rawValue;

                applyCommonFieldAttributes(control, node);
                return control;
            }

            case "decimal": {
                control = document.createElement("input");
                control.type = "number";
                control.step = "0.01";
                control.value = rawValue;

                applyCommonFieldAttributes(control, node);
                return control;
            }

            case "date": {
                control = document.createElement("input");
                control.type = "date";
                control.value = toDateInputValue(rawValue);

                applyCommonFieldAttributes(control, node);

                if (!control.value && rawValue) {
                    control.type = "text";
                    control.value = rawValue;
                }

                return control;
            }

            case "datetime": {
                control = document.createElement("input");
                control.type = "datetime-local";
                control.step = "1";
                control.value = toDateTimeLocalValue(rawValue);

                applyCommonFieldAttributes(control, node);

                if (!control.value && rawValue) {
                    control.type = "text";
                    control.value = rawValue;
                }

                return control;
            }

            case "time": {
                control = document.createElement("input");
                control.type = "time";
                control.value = toTimeInputValue(rawValue);

                applyCommonFieldAttributes(control, node);

                if (!control.value && rawValue) {
                    control.type = "text";
                    control.value = rawValue;
                }

                return control;
            }

            case "radio": {
                control = document.createElement("input");
                control.type = "radio";
                control.checked = rawValue.toLowerCase() === "true";

                applyCommonFieldAttributes(control, node);
                control.classList.remove("dlg-input");
                control.classList.add("project-checkbox");

                return control;
            }

            case "text":
            default: {
                control = document.createElement("input");
                control.type = "text";
                control.value = rawValue;

                applyCommonFieldAttributes(control, node);
                return control;
            }
        }
    }

    function renderProject(projectNode) {
        elProjectFields.innerHTML = "";

        if (!projectNode) {
            elProjectFields.innerHTML = '<div class="empty">No project data found.</div>';
            return;
        }

        const visibleFields = Array.from(projectNode.children).filter(isVisibleField);

        if (!visibleFields.length) {
            elProjectFields.innerHTML = '<div class="empty">No visible project fields found.</div>';
            return;
        }

        visibleFields.forEach((field) => {
            const label = document.createElement("label");
            label.className = "detail-label";
            label.setAttribute("for", "projectField_" + field.tagName);
            label.textContent = fieldLabel(field);

            const valueWrap = document.createElement("div");
            valueWrap.className = "detail-value";

            const control = createProjectFieldControl(field);
            valueWrap.appendChild(control);

            elProjectFields.append(label, valueWrap);
        });
    }

    function normalizeNotification(notificationNode) {
        return {
            notificationText: textOf(notificationNode, "NotificationText"),
            createdBy: fieldDisplayValue(getDirectChild(notificationNode, "CreatedById")),
            createdTime: textOf(notificationNode, "CreatedTime"),
            acknowledgeTime: textOf(notificationNode, "AcknowledgeTime"),
            createdTimeDisplay: formatLocalDateTime(textOf(notificationNode, "CreatedTime")),
            acknowledgeTimeDisplay: formatLocalDateTime(textOf(notificationNode, "AcknowledgeTime"))
        };
    }

    function renderNotificationsHeader(sampleNotificationNode) {
        if (!elNotificationsHead) return;

        const tr = document.createElement("tr");

        NOTIFICATION_COLUMNS.forEach((column) => {
            const node = sampleNotificationNode ? getDirectChild(sampleNotificationNode, column.tag) : null;

            const th = document.createElement("th");
            th.dataset.key = column.key;

            const sortSpan = document.createElement("span");
            sortSpan.className = "sort";

            const indicator = document.createElement("span");
            indicator.className = "sort-indicator";
            indicator.id = "si-" + column.key;

            sortSpan.append(document.createTextNode(fieldHeader(node) + " "));
            sortSpan.appendChild(indicator);
            th.appendChild(sortSpan);
            tr.appendChild(th);
        });

        elNotificationsHead.innerHTML = "";
        elNotificationsHead.appendChild(tr);

        elNotificationsHead.querySelectorAll("th[data-key]").forEach((th) => {
            th.addEventListener("click", function () {
                sortNotificationsBy(th.dataset.key);
            });
        });
    }

    function compareValues(a, b, key) {
        const av = (a[key] ?? "").toString().trim();
        const bv = (b[key] ?? "").toString().trim();

        return av.localeCompare(bv, "da", {
            numeric: true,
            sensitivity: "base"
        });
    }

    function setNotificationSortIndicators() {
        NOTIFICATION_COLUMNS.forEach((column) => {
            const indicator = document.getElementById("si-" + column.key);
            if (!indicator) return;

            indicator.textContent =
                notificationSortState.key === column.key
                    ? (notificationSortState.dir === "asc" ? "▲" : "▼")
                    : "";
        });
    }

    function sortNotificationsBy(key) {
        if (notificationSortState.key === key) {
            notificationSortState.dir = notificationSortState.dir === "asc" ? "desc" : "asc";
        } else {
            notificationSortState.key = key;
            notificationSortState.dir = "asc";
        }

        notifications.sort((a, b) => {
            const result = compareValues(a, b, key);
            return notificationSortState.dir === "asc" ? result : -result;
        });

        setNotificationSortIndicators();
        renderNotifications();
    }

    function renderNotifications() {
        elNotificationsBody.innerHTML = "";

        if (!notifications.length) {
            elNotificationsEmpty.style.display = "block";
            elNotificationsEmpty.textContent = "No notifications found.";
            return;
        }

        elNotificationsEmpty.style.display = "none";

        notifications.forEach((row) => {
            const tr = document.createElement("tr");

            [
                row.notificationText || "—",
                row.createdBy || "—",
                row.createdTimeDisplay || "—",
                row.acknowledgeTimeDisplay || "—"
            ].forEach((value) => {
                const td = document.createElement("td");
                td.textContent = value;
                tr.appendChild(td);
            });

            elNotificationsBody.appendChild(tr);
        });
    }

    function normalizeColor(color) {
        const map = {
            green: "#22c55e",
            red: "#ef4444",
            yellow: "#eab308",
            purple: "#a855f7",
            puple: "#a855f7",
            blue: "#3b82f6",
            orange: "#f97316",
            crimson: "#FF5A5F",
            skyblue: "#4FC3F7",
            limegreen: "#66BB6A",
            amber: "#FFCA28",
            violet: "#AB47BC",
            tangerine: "#FF8A65",
            cyan: "#26C6DA",
            magenta: "#EC407A",
            gold: "#FFD54F"
        };

        const normalized = (color || "").trim().toLowerCase();
        return map[normalized] || normalized || "#60a5fa";
    }

    function polarToCartesian(cx, cy, radius, angleInDegrees) {
        const angleInRadians = ((angleInDegrees - 90) * Math.PI) / 180.0;

        return {
            x: cx + radius * Math.cos(angleInRadians),
            y: cy + radius * Math.sin(angleInRadians)
        };
    }

    function describeArc(cx, cy, radius, startAngle, endAngle) {
        const start = polarToCartesian(cx, cy, radius, endAngle);
        const end = polarToCartesian(cx, cy, radius, startAngle);
        const largeArcFlag = endAngle - startAngle <= 180 ? "0" : "1";

        return [
            "M", cx, cy,
            "L", start.x, start.y,
            "A", radius, radius, 0, largeArcFlag, 0, end.x, end.y,
            "Z"
        ].join(" ");
    }

    function renderIrlChart(rootNode) {
        elIrlSlices.innerHTML = "";
        elIrlLegend.innerHTML = "";

        const srlListNode = getDirectChild(rootNode, "SrlList");
        const srlNodes = srlListNode ? getDirectChildren(srlListNode, "Srl") : [];

        if (!srlNodes.length) {
            elIrlLegend.innerHTML = '<div class="empty">No SRL data found.</div>';
            return;
        }

        const items = srlNodes
            .map((node) => ({
                label: node.getAttribute("label") || "—",
                hover: node.getAttribute("hover") || "",
                color: normalizeColor(node.getAttribute("color")),
                value: Number(node.getAttribute("value") || "0")
            }))
            .filter((item) => Number.isFinite(item.value) && item.value > 0);

        const total = items.reduce((sum, item) => sum + item.value, 0);

        if (!total) {
            elIrlLegend.innerHTML = '<div class="empty">SRL values are empty.</div>';
            return;
        }

        let currentAngle = 0;

        items.forEach((item) => {
            const percentage = (item.value / total) * 100;
            const sliceAngle = (percentage / 100) * 360;
            const endAngle = currentAngle + sliceAngle;

            const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
            path.setAttribute("d", describeArc(110, 110, 70, currentAngle, endAngle));
            path.setAttribute("fill", item.color);
            path.setAttribute("stroke", "rgba(15,23,42,.95)");
            path.setAttribute("stroke-width", "2");
            path.setAttribute("aria-label", item.label + " " + percentage.toFixed(1) + "%");

            if (item.hover) {
                const title = document.createElementNS("http://www.w3.org/2000/svg", "title");
                title.textContent = item.hover;
                path.appendChild(title);
            }

            elIrlSlices.appendChild(path);

            const legendItem = document.createElement("div");
            legendItem.className = "chart-legend-item";

            const legendColor = document.createElement("span");
            legendColor.className = "chart-legend-color";
            legendColor.style.backgroundColor = item.color;

            const legendText = document.createElement("div");
            legendText.className = "chart-legend-text";
            legendText.innerHTML = item.label + "<small>" + item.value + " (" + percentage.toFixed(1) + "%)</small>";

            legendItem.append(legendColor, legendText);
            elIrlLegend.appendChild(legendItem);

            currentAngle = endAngle;
        });
    }

    function applyProjectOverviewXml(doc) {
        const root = doc.getElementsByTagName("ProjectOverview")[0] || doc;
        const topPanel = getDirectChild(root, "TopPanel");
        const project = getDirectChild(root, "Project");
        const notificationsNode = getDirectChild(root, "Notifications");

        elCustomerName.textContent = topPanel ? (textOf(topPanel, "CustomerName") || "—") : "—";
        elProjectName.textContent = topPanel ? (textOf(topPanel, "ProjectName") || "—") : "—";
        elUserName.textContent = topPanel ? (textOf(topPanel, "Name") || "—") : "—";

        renderProject(project);

        const notificationNodes = notificationsNode
            ? getDirectChildren(notificationsNode, "Notification")
            : [];

        renderNotificationsHeader(notificationNodes[0] || null);

        notifications = notificationNodes.map(normalizeNotification);

        notifications.sort((a, b) => compareValues(a, b, notificationSortState.key));
        setNotificationSortIndicators();
        renderNotifications();
        renderIrlChart(root);
    }

    async function loadProjectOverviewXml() {
        elLoadStatus.textContent = "Loading…";

        if (elNotificationsEmpty) {
            elNotificationsEmpty.style.display = "block";
            elNotificationsEmpty.textContent = "Loading XML from web service…";
        }

        if (elProjectFields) {
            elProjectFields.innerHTML = '<div class="empty">Loading project details…</div>';
        }

        if (elIrlLegend) {
            elIrlLegend.innerHTML = '<div class="empty">Loading SRL data…</div>';
        }

        try {
            const response = await fetch(DATA_URL, {
                headers: {
                    "Accept": "application/xml, text/xml"
                }
            });

            if (!response.ok) {
                throw new Error("HTTP " + response.status + " from " + DATA_URL);
            }

            const xmlText = await response.text();
            const doc = parseXml(xmlText);

            applyProjectOverviewXml(doc);
            elLoadStatus.textContent = "Loaded";
        } catch (error) {
            elLoadStatus.textContent = "Error";

            if (elProjectFields) {
                elProjectFields.innerHTML = '<div class="empty">Failed to load project details.</div>';
            }

            if (elNotificationsBody) {
                elNotificationsBody.innerHTML = "";
            }

            if (elNotificationsEmpty) {
                elNotificationsEmpty.style.display = "block";
                elNotificationsEmpty.textContent = "Failed to load notifications.";
            }

            if (elIrlSlices) {
                elIrlSlices.innerHTML = "";
            }

            if (elIrlLegend) {
                elIrlLegend.innerHTML = '<div class="empty">Failed to load SRL data.</div>';
            }

            console.error(error);
        }
    }

    renderNotifications();
    loadProjectOverviewXml();
})();
