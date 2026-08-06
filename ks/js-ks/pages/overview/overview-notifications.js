import { fieldDisplayValue, fieldHeader } from "../../core/field-display.js";
import { formatLocalDateTime, formatFallback } from "../../core/format.js";
import {
    hideState,
    setEmptyState,
    setErrorState,
    setLoadingState
} from "../../core/placeholders.js";
import { getDirectChild, getDirectChildren, textOf } from "../../core/xml.js";
import {
    applySortIndicators,
    bindSortableHeaders,
    compareSortableValues,
    nextSortState
} from "../../components/sortable-table.js";

const NOTIFICATION_COLUMNS = [
    { key: "notificationText", tag: "NotificationText" },
    { key: "createdBy", tag: "CreatedById" },
    { key: "createdTime", tag: "CreatedTime" },
    { key: "acknowledgeTime", tag: "AcknowledgeTime" }
];

let notifications = [];
let notificationSortState = { key: "createdTime", dir: "asc" };

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

function compareNotificationValues(a, b, key) {
    return compareSortableValues(a[key], b[key], { locale: "da" });
}

function setNotificationSortIndicators() {
    applySortIndicators(
        NOTIFICATION_COLUMNS.map((column) => column.key),
        notificationSortState
    );
}

function renderNotificationsTable(elements) {
    elements.notificationsBody.innerHTML = "";

    if (!notifications.length) {
        setEmptyState(elements.notificationsEmpty, "No notifications found.");
        return;
    }

    hideState(elements.notificationsEmpty);

    notifications.forEach((row) => {
        const tr = document.createElement("tr");

        [
            formatFallback(row.notificationText),
            formatFallback(row.createdBy),
            formatFallback(row.createdTimeDisplay),
            formatFallback(row.acknowledgeTimeDisplay)
        ].forEach((value) => {
            const td = document.createElement("td");
            td.textContent = value;
            tr.appendChild(td);
        });

        elements.notificationsBody.appendChild(tr);
    });
}

function sortNotificationsBy(key, elements) {
    notificationSortState = nextSortState(notificationSortState, key);

    notifications.sort((a, b) => {
        const result = compareNotificationValues(a, b, key);
        return notificationSortState.dir === "asc" ? result : -result;
    });

    setNotificationSortIndicators();
    renderNotificationsTable(elements);
}

function renderNotificationsHeader(sampleNotificationNode, elements) {
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

        sortSpan.append(document.createTextNode(fieldHeader(node, column.tag) + " "));
        sortSpan.appendChild(indicator);
        th.appendChild(sortSpan);
        tr.appendChild(th);
    });

    elements.notificationsHead.innerHTML = "";
    elements.notificationsHead.appendChild(tr);

    bindSortableHeaders(elements.notificationsHead, (key) => sortNotificationsBy(key, elements));
}

export function renderOverviewNotifications(notificationsNode, elements) {
    const notificationNodes = notificationsNode
        ? getDirectChildren(notificationsNode, "Notification")
        : [];

    renderNotificationsHeader(notificationNodes[0] || null, elements);

    notifications = notificationNodes.map(normalizeNotification);
    notifications.sort((a, b) => compareNotificationValues(a, b, notificationSortState.key));

    setNotificationSortIndicators();
    renderNotificationsTable(elements);
}

export function setOverviewNotificationsLoading(elements) {
    if (elements.notificationsBody) {
        elements.notificationsBody.innerHTML = "";
    }

    setLoadingState(elements.notificationsEmpty, "Loading XML from web service…");
}

export function setOverviewNotificationsError(elements) {
    if (elements.notificationsBody) {
        elements.notificationsBody.innerHTML = "";
    }

    setErrorState(elements.notificationsEmpty, "Failed to load notifications.");
}