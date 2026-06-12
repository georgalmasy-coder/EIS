const API_URL = "/api/admin/customers";

const STORAGE_PREFIX = "customerAdministration";
const STORAGE_FILTER = `${STORAGE_PREFIX}.table.filter`;
const STORAGE_SORT_KEY = `${STORAGE_PREFIX}.table.sortKey`;
const STORAGE_SORT_DIRECTION = `${STORAGE_PREFIX}.table.sortDirection`;
const STORAGE_COLUMN_WIDTHS = `${STORAGE_PREFIX}.table.columnWidths`;

const DEFAULT_COLUMN_WIDTHS = {
    customerName: "240px",
    cvrNumber: "120px",
    contactName: "180px",
    contactEmail: "240px",
    contactPhone: "150px",
    customerStatus: "160px",
    createdDateTime: "180px"
};

const state = {
    customers: [],
    filteredCustomers: [],
    selectedCustomerId: null,
    sortKey: localStorage.getItem(STORAGE_SORT_KEY) || "customerName",
    sortDirection: localStorage.getItem(STORAGE_SORT_DIRECTION) || "asc",
    columnWidths: loadColumnWidths(),
    customerDetail: null,
    lookups: {},
    saving: false
};

const els = {};

document.addEventListener("DOMContentLoaded", initialize);

function initialize() {
    collectElements();
    applyColumnWidths();
    bindEvents();
    initializeTabs();

    if (els.customerFilter) {
        els.customerFilter.value = localStorage.getItem(STORAGE_FILTER) || "";
    }

    loadCustomers();
}

function collectElements() {
    els.loadStatus = document.getElementById("loadStatus");
    els.customerFilter = document.getElementById("customerFilter");
    els.btnRefreshCustomers = document.getElementById("btnRefreshCustomers");

    els.customerColGroup = document.getElementById("customerColGroup");
    els.customerHeaderRow = document.getElementById("customerHeaderRow");
    els.customersBody = document.getElementById("customersBody");
    els.customersEmpty = document.getElementById("customersEmpty");

    els.customerEditDialog = document.getElementById("customerEditDialog");
    els.customerEditForm = document.getElementById("customerEditForm");
    els.customerDialogTitle = document.getElementById("customerDialogTitle");
    els.customerDialogStatus = document.getElementById("customerDialogStatus");
    els.btnSaveCustomer = document.getElementById("btnSaveCustomer");
    els.btnCancelCustomer = document.getElementById("btnCancelCustomer");

    els.modulesBody = document.getElementById("modulesBody");
    els.modulesEmpty = document.getElementById("modulesEmpty");
    els.workflowEventsBody = document.getElementById("workflowEventsBody");
    els.workflowEventsEmpty = document.getElementById("workflowEventsEmpty");

    document.querySelectorAll("[id]").forEach(function (element) {
        els[element.id] = element;
    });
}

function bindEvents() {
    if (els.btnRefreshCustomers) {
        els.btnRefreshCustomers.addEventListener("click", loadCustomers);
    }

    if (els.customerFilter) {
        els.customerFilter.addEventListener("input", function () {
            localStorage.setItem(STORAGE_FILTER, els.customerFilter.value || "");
            applyFilterSortAndRender();
        });
    }

    if (els.customerHeaderRow) {
        els.customerHeaderRow.querySelectorAll("th[data-key]").forEach(function (th) {
            th.addEventListener("click", function (event) {
                if (event.target && event.target.classList.contains("col-resizer")) {
                    return;
                }

                const key = th.getAttribute("data-key");
                changeSort(key);
            });
        });
    }

    bindColumnResize();

    if (els.btnCancelCustomer) {
        els.btnCancelCustomer.addEventListener("click", closeDialog);
    }

    if (els.btnSaveCustomer) {
        els.btnSaveCustomer.addEventListener("click", saveCustomerAdministration);
    }
}

async function loadCustomers() {
    setLoadStatus("Loading...");

    try {
        const doc = await fetchXml(API_URL);
        state.lookups = parseLookups(doc);
        fillAllLookupSelects();
        state.customers = parseCustomerList(doc);
        applyFilterSortAndRender();
        setLoadStatus(`Loaded ${state.customers.length} customers`);
    } catch (error) {
        console.error(error);
        state.customers = [];
        applyFilterSortAndRender();
        setLoadStatus("Error loading customers");
    }
}

async function openCustomerDetail(customerId) {
    if (!customerId) {
        return;
    }

    state.selectedCustomerId = customerId;
    setDialogStatus("Loading customer...", "is-loading");
    clearDialog();

    if (els.customerEditDialog && typeof els.customerEditDialog.showModal === "function") {
        els.customerEditDialog.showModal();
    }

    try {
        const doc = await fetchXml(`${API_URL}?customerId=${encodeURIComponent(customerId)}`);
        state.lookups = parseLookups(doc);
        fillAllLookupSelects();
        state.customerDetail = parseCustomerDetail(doc);
        fillDialog(state.customerDetail);
        setDialogStatus("Loaded.", "is-ok");
    } catch (error) {
        console.error(error);
        setDialogStatus("Error loading customer.", "is-error");
    }
}

async function saveCustomerAdministration() {
    if (state.saving) {
        return;
    }

    const customerId = value("fieldCustomerId");

    if (!customerId) {
        setDialogStatus("Customer ID is missing.", "is-error");
        return;
    }

    state.saving = true;
    setSaveButtonDisabled(true);
    setDialogStatus("Saving customer administration data...", "is-loading");

    try {
        const xml = buildSaveXml();
        const doc = await postXml(API_URL, xml);
        const result = parseSaveResult(doc);

        if (!result.success) {
            setDialogStatus(result.message || "Customer administration data could not be saved.", "is-error");
            return;
        }

        setDialogStatus(result.message || "Customer administration data saved.", "is-ok");

        await loadCustomers();

        if (state.selectedCustomerId) {
            const detailDoc = await fetchXml(`${API_URL}?customerId=${encodeURIComponent(state.selectedCustomerId)}`);
            state.lookups = parseLookups(detailDoc);
            fillAllLookupSelects();
            state.customerDetail = parseCustomerDetail(detailDoc);
            fillDialog(state.customerDetail);
        }
    } catch (error) {
        console.error(error);
        setDialogStatus(`Save failed: ${error.message}`, "is-error");
    } finally {
        state.saving = false;
        setSaveButtonDisabled(false);
    }
}

async function fetchXml(url, options = {}) {
    const mergedOptions = {
        ...options,
        headers: {
            "Accept": "application/xml",
            ...(options.headers || {})
        }
    };

    const response = await fetch(url, mergedOptions);
    const textValue = await response.text();

    if (!response.ok) {
        throw new Error(`Request failed: ${response.status}`);
    }

    return parseXmlText(textValue, url);
}

async function postXml(url, xml) {
    const response = await fetch(url, {
        method: "POST",
        headers: {
            "Accept": "application/xml",
            "Content-Type": "application/xml; charset=UTF-8"
        },
        body: xml
    });

    const textValue = await response.text();
    const doc = parseXmlText(textValue, url);

    if (!response.ok) {
        const result = parseSaveResult(doc);
        throw new Error(result.message || `Request failed: ${response.status}`);
    }

    return doc;
}

function parseXmlText(textValue, url) {
    const trimmed = String(textValue || "").trim();

    if (!trimmed) {
        return null;
    }

    const parser = new DOMParser();
    const doc = parser.parseFromString(trimmed, "application/xml");
    const parserError = doc.querySelector("parsererror");

    if (parserError) {
        throw new Error(`Invalid XML returned from ${url}`);
    }

    return doc;
}

function parseLookups(doc) {
    const lookups = {};

    if (!doc) {
        return lookups;
    }

    doc.querySelectorAll("lookups > lookup").forEach(function (lookupNode) {
        const name = lookupNode.getAttribute("name") || "";

        if (!name) {
            return;
        }

        lookups[name] = Array.from(lookupNode.querySelectorAll(":scope > option")).map(function (optionNode) {
            return {
                code: optionNode.getAttribute("code") || "",
                label: optionNode.getAttribute("label") || optionNode.getAttribute("code") || ""
            };
        }).filter(function (option) {
            return option.code;
        });
    });

    return lookups;
}

function fillAllLookupSelects() {
    fillSelectFromLookup("fieldCustomerStatus", "customerStatus");
    fillSelectFromLookup("fieldSubscriptionStatus", "subscriptionStatus");
    fillSelectFromLookup("fieldPaymentStatus", "paymentStatus");
    fillSelectFromLookup("fieldPaymentMethodStatus", "paymentMethodStatus");
}

function fillSelectFromLookup(
    selectId,
    lookupName
) {
    const select = document.getElementById(selectId);

    if (!select) {
        return;
    }

    const currentValue = select.value || "";
    const options = state.lookups[lookupName] || [];

    select.innerHTML = "";

    options.forEach(function (lookupOption) {
        const option = document.createElement("option");
        option.value = lookupOption.code;
        option.textContent = lookupOption.label || lookupOption.code;

        if (lookupOption.code === currentValue) {
            option.selected = true;
        }

        select.appendChild(option);
    });
}

function parseCustomerList(doc) {
    if (!doc) {
        return [];
    }

    return Array.from(doc.querySelectorAll("customers > customer")).map(function (node) {
        const customerStatus = text(node, "customerStatus");

        return {
            customerId: intText(node, "customerId"),
            customerPK: intText(node, "customerPK"),
            version: intText(node, "version"),
            customerName: text(node, "customerName"),
            cvrNumber: text(node, "cvrNumber"),
            contactName: text(node, "contactName"),
            contactEmail: text(node, "contactEmail"),
            contactPhone: text(node, "contactPhone"),
            customerStatus,
            customerStatusLabel: lookupLabel("customerStatus", customerStatus),
            createdDateTime: text(node, "createdDateTime"),
            changedDateTime: text(node, "changedDateTime")
        };
    });
}

function parseCustomerDetail(doc) {
    const detail = doc ? doc.querySelector("customerDetail") : null;

    if (!detail) {
        return {};
    }

    return {
        customer: parseElement(detail.querySelector(":scope > customer")),
        subscription: parseElement(detail.querySelector(":scope > subscription")),
        latestPayment: parseElement(detail.querySelector(":scope > latestPayment")),
        paymentMethod: parseElement(detail.querySelector(":scope > paymentMethod")),
        modules: Array.from(detail.querySelectorAll(":scope > modules > module")).map(parseElement),
        workflow: parseElement(detail.querySelector(":scope > workflow")),
        workflowEvents: Array.from(detail.querySelectorAll(":scope > workflowEvents > event")).map(parseElement)
    };
}

function parseElement(element) {
    const result = {};

    if (!element) {
        return result;
    }

    Array.from(element.children).forEach(function (child) {
        result[child.tagName] = child.textContent || "";
    });

    return result;
}

function parseSaveResult(doc) {
    const root = doc ? doc.querySelector("customerAdministrationSaveResult") : null;

    if (!root) {
        return {
            success: false,
            customerId: null,
            message: "Invalid save response."
        };
    }

    return {
        success: text(root, "success").toLowerCase() === "true",
        customerId: intText(root, "customerId"),
        message: text(root, "message")
    };
}

function applyFilterSortAndRender() {
    const filter = normalize(els.customerFilter ? els.customerFilter.value : "");

    state.filteredCustomers = state.customers.filter(function (customer) {
        if (!filter) {
            return true;
        }

        return [
            customer.customerName,
            customer.cvrNumber,
            customer.contactName,
            customer.contactEmail,
            customer.contactPhone,
            customer.customerStatus,
            customer.customerStatusLabel,
            customer.createdDateTime,
            formatDanishDateTime(customer.createdDateTime)
        ].some(function (itemValue) {
            return normalize(itemValue).includes(filter);
        });
    });

    state.filteredCustomers.sort(compareCustomers);

    renderCustomers();
    updateSortIndicators();
}

function compareCustomers(left, right) {
    const key = state.sortKey;
    const direction = state.sortDirection === "desc" ? -1 : 1;

    const leftValue = valueForSort(left, key);
    const rightValue = valueForSort(right, key);

    if (leftValue < rightValue) {
        return -1 * direction;
    }

    if (leftValue > rightValue) {
        return 1 * direction;
    }

    return 0;
}

function valueForSort(row, key) {
    if (!row) {
        return "";
    }

    if (key === "createdDateTime" || key === "changedDateTime") {
        const time = dateTimeMillis(row[key]);
        return Number.isFinite(time) ? time : 0;
    }

    if (key === "customerStatus") {
        return String(row.customerStatusLabel || row.customerStatus || "").toLowerCase();
    }

    const itemValue = row[key];

    if (itemValue == null) {
        return "";
    }

    return String(itemValue).toLowerCase();
}

function changeSort(key) {
    if (!key) {
        return;
    }

    if (state.sortKey === key) {
        state.sortDirection = state.sortDirection === "asc" ? "desc" : "asc";
    } else {
        state.sortKey = key;
        state.sortDirection = "asc";
    }

    localStorage.setItem(STORAGE_SORT_KEY, state.sortKey);
    localStorage.setItem(STORAGE_SORT_DIRECTION, state.sortDirection);

    applyFilterSortAndRender();
}

function renderCustomers() {
    if (!els.customersBody) {
        return;
    }

    if (!state.filteredCustomers.length) {
        els.customersBody.innerHTML = "";

        if (els.customersEmpty) {
            els.customersEmpty.hidden = false;
            els.customersEmpty.textContent = state.customers.length ? "No customers match the filter." : "No customers.";
        }

        return;
    }

    if (els.customersEmpty) {
        els.customersEmpty.hidden = true;
    }

    els.customersBody.innerHTML = state.filteredCustomers.map(function (customer) {
        const createdLocal = formatDanishDateTime(customer.createdDateTime);

        return `
            <tr data-customer-id="${escapeAttribute(customer.customerId)}">
                <td title="${escapeAttribute(customer.customerName)}">${escapeHtml(customer.customerName)}</td>
                <td title="${escapeAttribute(customer.cvrNumber)}">${escapeHtml(customer.cvrNumber)}</td>
                <td title="${escapeAttribute(customer.contactName)}">${escapeHtml(customer.contactName)}</td>
                <td title="${escapeAttribute(customer.contactEmail)}">${escapeHtml(customer.contactEmail)}</td>
                <td title="${escapeAttribute(customer.contactPhone)}">${escapeHtml(customer.contactPhone)}</td>
                <td>${renderStatus(customer.customerStatus, customer.customerStatusLabel)}</td>
                <td title="${escapeAttribute(createdLocal)}">${escapeHtml(createdLocal)}</td>
            </tr>
        `;
    }).join("");

    els.customersBody.querySelectorAll("tr[data-customer-id]").forEach(function (row) {
        row.addEventListener("dblclick", function () {
            const customerId = parseInt(row.getAttribute("data-customer-id"), 10);
            openCustomerDetail(customerId);
        });
    });
}

function renderStatus(status, label) {
    const safeStatus = status || "";
    const safeLabel = label || humanReadableCode(safeStatus);
    const statusClass = `status-${safeStatus.toLowerCase().replaceAll("_", "-")}`;

    return `<span class="customer-status-pill ${escapeAttribute(statusClass)}" title="${escapeAttribute(safeStatus)}">${escapeHtml(safeLabel || "—")}</span>`;
}

function updateSortIndicators() {
    document.querySelectorAll(".sort-indicator").forEach(function (indicator) {
        indicator.textContent = "";
    });

    const indicator = document.getElementById(`si-${state.sortKey}`);

    if (indicator) {
        indicator.textContent = state.sortDirection === "asc" ? "▲" : "▼";
    }
}

function bindColumnResize() {
    document.querySelectorAll(".col-resizer[data-resize-key]").forEach(function (resizer) {
        resizer.addEventListener("mousedown", function (event) {
            event.preventDefault();
            event.stopPropagation();

            const key = resizer.getAttribute("data-resize-key");
            const th = resizer.closest("th");
            const startX = event.clientX;
            const startWidth = th ? th.offsetWidth : 120;

            function onMouseMove(moveEvent) {
                const nextWidth = Math.max(70, startWidth + moveEvent.clientX - startX);
                state.columnWidths[key] = `${nextWidth}px`;
                applyColumnWidths();
            }

            function onMouseUp() {
                localStorage.setItem(STORAGE_COLUMN_WIDTHS, JSON.stringify(state.columnWidths));
                document.removeEventListener("mousemove", onMouseMove);
                document.removeEventListener("mouseup", onMouseUp);
            }

            document.addEventListener("mousemove", onMouseMove);
            document.addEventListener("mouseup", onMouseUp);
        });
    });
}

function applyColumnWidths() {
    if (!els.customerColGroup) {
        return;
    }

    Object.keys(DEFAULT_COLUMN_WIDTHS).forEach(function (key) {
        const col = els.customerColGroup.querySelector(`col[data-col-key="${key}"]`);

        if (col) {
            col.style.width = state.columnWidths[key] || DEFAULT_COLUMN_WIDTHS[key];
        }
    });
}

function loadColumnWidths() {
    try {
        const parsed = JSON.parse(localStorage.getItem(STORAGE_COLUMN_WIDTHS) || "{}");
        return {
            ...DEFAULT_COLUMN_WIDTHS,
            ...(parsed || {})
        };
    } catch (_error) {
        return { ...DEFAULT_COLUMN_WIDTHS };
    }
}

function initializeTabs() {
    const buttons = Array.from(document.querySelectorAll(".page-tab-btn[role='tab']"));

    buttons.forEach(function (button) {
        button.addEventListener("click", function () {
            const panelId = button.getAttribute("aria-controls");

            buttons.forEach(function (btn) {
                btn.classList.remove("is-active");
                btn.setAttribute("aria-selected", "false");
            });

            document.querySelectorAll(".tab-panel").forEach(function (panel) {
                panel.classList.remove("is-active");
            });

            button.classList.add("is-active");
            button.setAttribute("aria-selected", "true");

            const panel = document.getElementById(panelId);

            if (panel) {
                panel.classList.add("is-active");
            }
        });
    });
}

function fillDialog(detail) {
    const customer = detail.customer || {};
    const subscription = detail.subscription || {};
    const payment = detail.latestPayment || {};
    const paymentMethod = detail.paymentMethod || {};
    const workflow = detail.workflow || {};

    setValue("fieldCustomerId", customer.customerId);
    setValue("fieldCustomerPK", customer.customerPK);
    setValue("fieldVersion", customer.version);
    setValue("fieldCustomerName", customer.customerName);
    setValue("fieldCvrNumber", customer.cvrNumber);
    setValue("fieldPhone", customer.phone);
    setValue("fieldAddress", customer.address);
    setValue("fieldZipCode", customer.zipCode);
    setValue("fieldCity", customer.city);
    setValue("fieldCountry", customer.country);
    setValue("fieldContactName", customer.contactName);
    setValue("fieldContactEmail", customer.contactEmail);
    setValue("fieldCustomerStatus", customer.customerStatus);
    setValue("fieldCreatedDateTime", formatDanishDateTime(customer.createdDateTime));
    setValue("fieldChangedDateTime", formatDanishDateTime(customer.changedDateTime));
    setValue("fieldChangedByUserId", customer.changedByUserId);

    setValue("fieldSubscriptionId", subscription.subscriptionId);
    setValue("fieldSubscriptionStatus", subscription.subscriptionStatus);
    setValue("fieldSubscriptionPlanId", subscription.subscriptionPlanId);
    setValue("fieldSubscriptionPlanName", subscription.subscriptionPlanName);
    setValue("fieldTrialStartAt", formatDanishDateTime(subscription.trialStartAt));
    setValue("fieldTrialEndAt", formatDanishDateTime(subscription.trialEndAt));
    setValue("fieldTrialReminderSentAt", formatDanishDateTime(subscription.trialReminderSentAt));
    setValue("fieldPeriodStartAt", formatDanishDateTime(subscription.periodStartAt));
    setValue("fieldPeriodEndAt", formatDanishDateTime(subscription.periodEndAt));
    setValue("fieldRenewalReminderSentAt", formatDanishDateTime(subscription.renewalReminderSentAt));
    setValue("fieldContinuationConfirmedAt", formatDanishDateTime(subscription.continuationConfirmedAt));
    setValue("fieldRenewalConfirmedAt", formatDanishDateTime(subscription.renewalConfirmedAt));
    setValue("fieldSubscriptionGracePeriodEndsAt", formatDanishDateTime(subscription.gracePeriodEndsAt));
    setValue("fieldSubscriptionCreatedAt", formatDanishDateTime(subscription.createdAt));
    setValue("fieldSubscriptionUpdatedAt", formatDanishDateTime(subscription.updatedAt));

    setValue("fieldPaymentId", payment.paymentId);
    setValue("fieldPaymentStatus", payment.paymentStatus);
    setValue("fieldPaymentProvider", payment.paymentProvider);
    setValue("fieldPaymentProviderReference", payment.paymentProviderReference);
    setValue("fieldPaymentAmount", payment.amount);
    setValue("fieldPaymentCurrency", payment.currency);
    setValue("fieldPaymentDueAt", formatDanishDateTime(payment.paymentDueAt));
    setValue("fieldPaymentGracePeriodEndsAt", formatDanishDateTime(payment.gracePeriodEndsAt));
    setValue("fieldRequestedAt", formatDanishDateTime(payment.requestedAt));
    setValue("fieldAuthorizedAt", formatDanishDateTime(payment.authorizedAt));
    setValue("fieldCapturedAt", formatDanishDateTime(payment.capturedAt));
    setValue("fieldSucceededAt", formatDanishDateTime(payment.succeededAt));
    setValue("fieldFailedAt", formatDanishDateTime(payment.failedAt));
    setValue("fieldCancelledAt", formatDanishDateTime(payment.cancelledAt));
    setValue("fieldFailureReason", payment.failureReason);
    setValue("fieldPaymentCreatedAt", formatDanishDateTime(payment.createdAt));
    setValue("fieldPaymentUpdatedAt", formatDanishDateTime(payment.updatedAt));

    setValue("fieldCustomerPaymentMethodId", paymentMethod.customerPaymentMethodId);
    setValue("fieldPaymentMethodProvider", paymentMethod.paymentProvider);
    setValue("fieldProviderPaymentMethodReference", paymentMethod.providerPaymentMethodReference);
    setValue("fieldCardholderName", paymentMethod.cardholderName);
    setValue("fieldCardBrand", paymentMethod.cardBrand);
    setValue("fieldMaskedCardNumber", paymentMethod.maskedCardNumber);
    setValue("fieldExpiryMonth", paymentMethod.expiryMonth);
    setValue("fieldExpiryYear", paymentMethod.expiryYear);
    setValue("fieldBillingZipCode", paymentMethod.billingZipCode);
    setValue("fieldPaymentMethodStatus", paymentMethod.paymentMethodStatus);
    setValue("fieldPaymentMethodCreatedAt", formatDanishDateTime(paymentMethod.createdAt));
    setValue("fieldPaymentMethodUpdatedAt", formatDanishDateTime(paymentMethod.updatedAt));

    setValue("fieldWorkflowType", lookupLabel("workflowType", workflow.workflowType));
    setValue("fieldWorkflowStatus", lookupLabel("workflowStatus", workflow.workflowStatus));
    setValue("fieldCurrentState", lookupLabel("workflowState", workflow.currentState));
    setValue("fieldNextActionAt", formatDanishDateTime(workflow.nextActionAt));
    setValue("fieldRetryCount", workflow.retryCount);
    setValue("fieldLastEventType", lookupLabel("workflowEventType", workflow.lastEventType));
    setValue("fieldLastEventAt", formatDanishDateTime(workflow.lastEventAt));
    setValue("fieldLastError", workflow.lastError);
    setValue("fieldLockedAt", formatDanishDateTime(workflow.lockedAt));
    setValue("fieldLockedBy", workflow.lockedBy);
    setValue("fieldWorkflowCreatedAt", formatDanishDateTime(workflow.createdAt));
    setValue("fieldWorkflowUpdatedAt", formatDanishDateTime(workflow.updatedAt));

    if (els.customerDialogTitle) {
        els.customerDialogTitle.textContent = `Customer Administration - ${customer.customerName || customer.customerId || ""}`;
    }

    renderModules(detail.modules || []);
    renderWorkflowEvents(detail.workflowEvents || []);
}

function clearDialog() {
    if (els.customerEditForm) {
        els.customerEditForm.querySelectorAll("input, textarea, select").forEach(function (field) {
            field.value = "";
        });
    }

    renderModules([]);
    renderWorkflowEvents([]);
}

function renderModules(modules) {
    if (!els.modulesBody) {
        return;
    }

    if (!modules.length) {
        els.modulesBody.innerHTML = "";

        if (els.modulesEmpty) {
            els.modulesEmpty.hidden = false;
        }

        return;
    }

    if (els.modulesEmpty) {
        els.modulesEmpty.hidden = true;
    }

    els.modulesBody.innerHTML = modules.map(function (module, index) {
        return `
            <tr data-module-index="${index}">
                <td class="customer-technical-field">
                    <input data-module-field="customerModuleId" value="${escapeAttribute(module.customerModuleId)}" />
                    <input data-module-field="subscriptionPlanId" value="${escapeAttribute(module.subscriptionPlanId)}" />
                </td>
                <td><input data-module-field="moduleCode" value="${escapeAttribute(module.moduleCode)}" /></td>
                <td><input data-module-field="moduleName" value="${escapeAttribute(module.moduleName)}" /></td>
                <td>${renderLookupSelect("customerModuleStatus", "customerModuleStatus", module.customerModuleStatus)}</td>
                <td>${escapeHtml(formatDanishDateTime(module.createdAt))}</td>
                <td>${escapeHtml(formatDanishDateTime(module.updatedAt))}</td>
            </tr>
        `;
    }).join("");
}

function renderLookupSelect(
    lookupName,
    dataFieldName,
    selectedValue
) {
    const options = state.lookups[lookupName] || [];

    const optionHtml = options.map(function (lookupOption) {
        const selected = lookupOption.code === selectedValue ? " selected" : "";

        return `<option value="${escapeAttribute(lookupOption.code)}"${selected}>${escapeHtml(lookupOption.label || lookupOption.code)}</option>`;
    }).join("");

    return `<select data-module-field="${escapeAttribute(dataFieldName)}">${optionHtml}</select>`;
}

function renderWorkflowEvents(events) {
    if (!els.workflowEventsBody) {
        return;
    }

    if (!events.length) {
        els.workflowEventsBody.innerHTML = "";

        if (els.workflowEventsEmpty) {
            els.workflowEventsEmpty.hidden = false;
        }

        return;
    }

    if (els.workflowEventsEmpty) {
        els.workflowEventsEmpty.hidden = true;
    }

    els.workflowEventsBody.innerHTML = events.map(function (event) {
        return `
            <tr>
                <td>${escapeHtml(lookupLabel("workflowEventType", event.eventType))}</td>
                <td>${escapeHtml(event.eventCategory)}</td>
                <td>${escapeHtml(lookupLabel("workflowState", event.fromState))}</td>
                <td>${escapeHtml(lookupLabel("workflowState", event.toState))}</td>
                <td title="${escapeAttribute(event.description)}">${escapeHtml(event.description)}</td>
                <td>${escapeHtml(formatDanishDateTime(event.createdAt))}</td>
                <td>${escapeHtml(event.createdByUserId)}</td>
            </tr>
        `;
    }).join("");
}

function buildSaveXml() {
    const xml = [];

    xml.push('<?xml version="1.0" encoding="UTF-8"?>');
    xml.push("<customerAdministrationSave>");

    appendCustomerXml(xml);
    appendSubscriptionXml(xml);
    appendLatestPaymentXml(xml);
    appendPaymentMethodXml(xml);
    appendModulesXml(xml);

    xml.push("</customerAdministrationSave>");

    return xml.join("");
}

function appendCustomerXml(xml) {
    xml.push("<customer>");

    appendXmlElement(xml, "customerId", value("fieldCustomerId"));
    appendXmlElement(xml, "customerPK", value("fieldCustomerPK"));
    appendXmlElement(xml, "version", value("fieldVersion"));
    appendXmlElement(xml, "customerName", value("fieldCustomerName"));
    appendXmlElement(xml, "cvrNumber", value("fieldCvrNumber"));
    appendXmlElement(xml, "phone", value("fieldPhone"));
    appendXmlElement(xml, "address", value("fieldAddress"));
    appendXmlElement(xml, "zipCode", value("fieldZipCode"));
    appendXmlElement(xml, "city", value("fieldCity"));
    appendXmlElement(xml, "country", value("fieldCountry"));
    appendXmlElement(xml, "contactName", value("fieldContactName"));
    appendXmlElement(xml, "contactEmail", value("fieldContactEmail"));
    appendXmlElement(xml, "customerStatus", value("fieldCustomerStatus"));
    appendXmlElement(xml, "changedByUserId", value("fieldChangedByUserId"));

    xml.push("</customer>");
}

function appendSubscriptionXml(xml) {
    xml.push("<subscription>");

    appendXmlElement(xml, "subscriptionId", value("fieldSubscriptionId"));
    appendXmlElement(xml, "customerId", value("fieldCustomerId"));
    appendXmlElement(xml, "subscriptionStatus", value("fieldSubscriptionStatus"));
    appendXmlElement(xml, "subscriptionPlanId", value("fieldSubscriptionPlanId"));
    appendXmlElement(xml, "subscriptionPlanName", value("fieldSubscriptionPlanName"));
    appendXmlElement(xml, "trialStartAt", toIsoDateTime(value("fieldTrialStartAt")));
    appendXmlElement(xml, "trialEndAt", toIsoDateTime(value("fieldTrialEndAt")));
    appendXmlElement(xml, "trialReminderSentAt", toIsoDateTime(value("fieldTrialReminderSentAt")));
    appendXmlElement(xml, "periodStartAt", toIsoDateTime(value("fieldPeriodStartAt")));
    appendXmlElement(xml, "periodEndAt", toIsoDateTime(value("fieldPeriodEndAt")));
    appendXmlElement(xml, "renewalReminderSentAt", toIsoDateTime(value("fieldRenewalReminderSentAt")));
    appendXmlElement(xml, "continuationConfirmedAt", toIsoDateTime(value("fieldContinuationConfirmedAt")));
    appendXmlElement(xml, "renewalConfirmedAt", toIsoDateTime(value("fieldRenewalConfirmedAt")));
    appendXmlElement(xml, "gracePeriodEndsAt", toIsoDateTime(value("fieldSubscriptionGracePeriodEndsAt")));

    xml.push("</subscription>");
}

function appendLatestPaymentXml(xml) {
    xml.push("<latestPayment>");

    appendXmlElement(xml, "paymentId", value("fieldPaymentId"));
    appendXmlElement(xml, "customerId", value("fieldCustomerId"));
    appendXmlElement(xml, "subscriptionId", value("fieldSubscriptionId"));
    appendXmlElement(xml, "paymentStatus", value("fieldPaymentStatus"));
    appendXmlElement(xml, "paymentProvider", value("fieldPaymentProvider"));
    appendXmlElement(xml, "paymentProviderReference", value("fieldPaymentProviderReference"));
    appendXmlElement(xml, "amount", value("fieldPaymentAmount"));
    appendXmlElement(xml, "currency", value("fieldPaymentCurrency"));
    appendXmlElement(xml, "paymentDueAt", toIsoDateTime(value("fieldPaymentDueAt")));
    appendXmlElement(xml, "gracePeriodEndsAt", toIsoDateTime(value("fieldPaymentGracePeriodEndsAt")));
    appendXmlElement(xml, "requestedAt", toIsoDateTime(value("fieldRequestedAt")));
    appendXmlElement(xml, "authorizedAt", toIsoDateTime(value("fieldAuthorizedAt")));
    appendXmlElement(xml, "capturedAt", toIsoDateTime(value("fieldCapturedAt")));
    appendXmlElement(xml, "succeededAt", toIsoDateTime(value("fieldSucceededAt")));
    appendXmlElement(xml, "failedAt", toIsoDateTime(value("fieldFailedAt")));
    appendXmlElement(xml, "cancelledAt", toIsoDateTime(value("fieldCancelledAt")));
    appendXmlElement(xml, "failureReason", value("fieldFailureReason"));

    xml.push("</latestPayment>");
}

function appendPaymentMethodXml(xml) {
    xml.push("<paymentMethod>");

    appendXmlElement(xml, "customerPaymentMethodId", value("fieldCustomerPaymentMethodId"));
    appendXmlElement(xml, "customerId", value("fieldCustomerId"));
    appendXmlElement(xml, "paymentProvider", value("fieldPaymentMethodProvider"));
    appendXmlElement(xml, "providerPaymentMethodReference", value("fieldProviderPaymentMethodReference"));
    appendXmlElement(xml, "cardholderName", value("fieldCardholderName"));
    appendXmlElement(xml, "cardBrand", value("fieldCardBrand"));
    appendXmlElement(xml, "maskedCardNumber", value("fieldMaskedCardNumber"));
    appendXmlElement(xml, "expiryMonth", value("fieldExpiryMonth"));
    appendXmlElement(xml, "expiryYear", value("fieldExpiryYear"));
    appendXmlElement(xml, "billingZipCode", value("fieldBillingZipCode"));
    appendXmlElement(xml, "paymentMethodStatus", value("fieldPaymentMethodStatus"));

    xml.push("</paymentMethod>");
}

function appendModulesXml(xml) {
    xml.push("<modules>");

    collectModules().forEach(function (module) {
        xml.push("<module>");

        appendXmlElement(xml, "customerModuleId", module.customerModuleId);
        appendXmlElement(xml, "customerId", value("fieldCustomerId"));
        appendXmlElement(xml, "subscriptionPlanId", module.subscriptionPlanId);
        appendXmlElement(xml, "moduleCode", module.moduleCode);
        appendXmlElement(xml, "moduleName", module.moduleName);
        appendXmlElement(xml, "customerModuleStatus", module.customerModuleStatus);

        xml.push("</module>");
    });

    xml.push("</modules>");
}

function collectModules() {
    if (!els.modulesBody) {
        return [];
    }

    return Array.from(els.modulesBody.querySelectorAll("tr[data-module-index]")).map(function (row) {
        return {
            customerModuleId: moduleValue(row, "customerModuleId"),
            subscriptionPlanId: moduleValue(row, "subscriptionPlanId"),
            moduleCode: moduleValue(row, "moduleCode"),
            moduleName: moduleValue(row, "moduleName"),
            customerModuleStatus: moduleValue(row, "customerModuleStatus")
        };
    });
}

function moduleValue(row, fieldName) {
    const input = row.querySelector(`[data-module-field="${fieldName}"]`);
    return input ? input.value || "" : "";
}

function appendXmlElement(xml, elementName, itemValue) {
    xml.push("<");
    xml.push(elementName);
    xml.push(">");

    if (itemValue != null && itemValue !== "") {
        xml.push(escapeXml(itemValue));
    }

    xml.push("</");
    xml.push(elementName);
    xml.push(">");
}

function closeDialog() {
    if (els.customerEditDialog) {
        els.customerEditDialog.close();
    }
}

function value(id) {
    const element = document.getElementById(id);
    return element ? element.value || "" : "";
}

function setValue(id, itemValue) {
    const element = document.getElementById(id);

    if (!element) {
        return;
    }

    element.value = itemValue == null ? "" : String(itemValue);
}

function setLoadStatus(textValue) {
    if (els.loadStatus) {
        els.loadStatus.textContent = textValue || "";
    }
}

function setDialogStatus(textValue, className) {
    if (!els.customerDialogStatus) {
        return;
    }

    els.customerDialogStatus.classList.remove("is-error", "is-ok", "is-loading");
    els.customerDialogStatus.textContent = textValue || "";

    if (className) {
        els.customerDialogStatus.classList.add(className);
    }
}

function setSaveButtonDisabled(disabled) {
    if (els.btnSaveCustomer) {
        els.btnSaveCustomer.disabled = Boolean(disabled);
    }
}

function text(node, selector) {
    const element = node ? node.querySelector(selector) : null;
    return element ? element.textContent || "" : "";
}

function intText(node, selector) {
    const textValue = text(node, selector);
    const parsed = parseInt(textValue, 10);

    return Number.isFinite(parsed) ? parsed : null;
}

function lookupLabel(
    lookupName,
    code
) {
    if (!code) {
        return "";
    }

    const options = state.lookups[lookupName] || [];
    const option = options.find(function (lookupOption) {
        return lookupOption.code === code;
    });

    return option ? option.label : humanReadableCode(code);
}

function humanReadableCode(code) {
    if (!code) {
        return "";
    }

    return String(code)
        .trim()
        .toLowerCase()
        .split("_")
        .filter(Boolean)
        .map(function (part) {
            return part.charAt(0).toUpperCase() + part.slice(1);
        })
        .join(" ");
}

function normalize(itemValue) {
    return String(itemValue || "").trim().toLowerCase();
}

function dateTimeMillis(itemValue) {
    if (!itemValue) {
        return NaN;
    }

    const normalized = String(itemValue).trim();

    if (!normalized) {
        return NaN;
    }

    const isoValue = normalized.includes("T")
        ? normalized
        : normalized.replace(" ", "T");

    const date = new Date(isoValue.endsWith("Z") ? isoValue : `${isoValue}Z`);

    return date.getTime();
}

function formatDanishDateTime(itemValue) {
    const millis = dateTimeMillis(itemValue);

    if (!Number.isFinite(millis)) {
        return "";
    }

    return new Intl.DateTimeFormat("da-DK", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit"
    }).format(new Date(millis));
}

function toIsoDateTime(itemValue) {
    const rawValue = String(itemValue || "").trim();

    if (!rawValue) {
        return "";
    }

    const danishMatch = rawValue.match(/^(\d{2})[./-](\d{2})[./-](\d{4}),?\s+(\d{2})[.:](\d{2})$/);

    if (danishMatch) {
        const [, day, month, year, hour, minute] = danishMatch;
        return `${year}-${month}-${day}T${hour}:${minute}`;
    }

    return rawValue;
}

function escapeXml(itemValue) {
    return String(itemValue == null ? "" : itemValue)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&apos;");
}

function escapeHtml(itemValue) {
    return String(itemValue == null ? "" : itemValue)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function escapeAttribute(itemValue) {
    return escapeHtml(itemValue);
}