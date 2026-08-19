import { initMenu } from "../components/menu.js";
import { mountTopbar } from "../components/topbar.js";
import { applyTopPanelFromDocument } from "../core/page-header.js";
import { closeDialogElement, clearChildren, setText, showDialog } from "../core/dom.js";
import { escapeXml, getAttribute, getDirectChild, getDirectChildren, getChildText, hasXmlParseError, parseXml } from "../core/xml.js";
import { fetchXml, postXml } from "../core/http.js";

const DATA_URL = "/api/admin/subscription-editor";
const EUROPEAN_CURRENCIES = [
    { code: "EUR", label: "EUR - Euro" },
    { code: "ALL", label: "ALL - Albanian Lek" },
    { code: "AMD", label: "AMD - Armenian Dram" },
    { code: "AZN", label: "AZN - Azerbaijani Manat" },
    { code: "BAM", label: "BAM - Bosnia-Herzegovina Convertible Mark" },
    { code: "BGN", label: "BGN - Bulgarian Lev" },
    { code: "BYN", label: "BYN - Belarusian Ruble" },
    { code: "CHF", label: "CHF - Swiss Franc" },
    { code: "CZK", label: "CZK - Czech Koruna" },
    { code: "DKK", label: "DKK - Danish Krone" },
    { code: "GEL", label: "GEL - Georgian Lari" },
    { code: "GBP", label: "GBP - Pound Sterling" },
    { code: "GIP", label: "GIP - Gibraltar Pound" },
    { code: "HRK", label: "HRK - Croatian Kuna" },
    { code: "HUF", label: "HUF - Hungarian Forint" },
    { code: "ISK", label: "ISK - Icelandic Krona" },
    { code: "MDL", label: "MDL - Moldovan Leu" },
    { code: "MKD", label: "MKD - Macedonian Denar" },
    { code: "NOK", label: "NOK - Norwegian Krone" },
    { code: "PLN", label: "PLN - Polish Zloty" },
    { code: "RON", label: "RON - Romanian Leu" },
    { code: "RSD", label: "RSD - Serbian Dinar" },
    { code: "RUB", label: "RUB - Russian Ruble" },
    { code: "SEK", label: "SEK - Swedish Krona" },
    { code: "TRY", label: "TRY - Turkish Lira" },
    { code: "UAH", label: "UAH - Ukrainian Hryvnia" }
];

const state = {
    currentDoc: null,
    topPanel: {
        customerName: "—",
        projectName: "—",
        userName: "—"
    },
    lookups: {},
    plans: [],
    filteredPlans: [],
    selectedPlanId: null
};

const els = {};

document.addEventListener("DOMContentLoaded", initialize);

function initialize() {
    initMenu();
    mountTopbar(document);
    collectElements();
    bindEvents();
    loadPlans();
}

function collectElements() {
    document.querySelectorAll("[id]").forEach(function (element) {
        els[element.id] = element;
    });

    els.workspaceEyebrow = els.pageEyebrow;
    els.workspaceHeading = els.pageHeading;
    els.workspaceHelpText = els.pageHelpText;
}

function bindEvents() {
    els.planFilter?.addEventListener("input", function () {
        syncFilterClearButton();
        renderPlans();
    });

    els.planFilter?.addEventListener("keydown", function (event) {
        if (event.key === "Escape") {
            els.planFilter.value = "";
            syncFilterClearButton();
            renderPlans();
            els.planFilter.blur();
        }
    });

    els.btnClearFilter?.addEventListener("click", function () {
        if (els.planFilter) {
            els.planFilter.value = "";
            syncFilterClearButton();
            els.planFilter.focus();
        }
        renderPlans();
    });

    els.btnCreateSubscription?.addEventListener("click", function () {
        openCreateDialog();
    });

    els.btnCancelSubscription?.addEventListener("click", function () {
        closeDialogElement(els.subscriptionEditorDialog);
    });

    els.btnSaveSubscription?.addEventListener("click", function () {
        saveCurrentSubscription();
    });

    els.billingPeriodsBody?.addEventListener("click", function (event) {
        const toggleTarget = event.target.closest("[data-billing-period-active-toggle]");
        if (!toggleTarget) {
            return;
        }

        const row = toggleTarget.closest("tr[data-billing-period-row]");
        if (!row) {
            return;
        }

        const activeField = row.querySelector('[data-field="active"]');
        if (!activeField) {
            return;
        }

        const nextValue = !activeField.checked;
        activeField.checked = nextValue;
        toggleTarget.classList.toggle("is-active", nextValue);
        toggleTarget.classList.toggle("is-inactive", !nextValue);
        toggleTarget.title = nextValue ? "Active" : "Inactive";
        toggleTarget.setAttribute("aria-label", nextValue ? "Active" : "Inactive");
        event.preventDefault();
    });

    els.tabBtnDetails?.addEventListener("click", function () {
        setActiveTab("details");
    });

    els.tabBtnCustomers?.addEventListener("click", function () {
        setActiveTab("customers");
    });
}

async function loadPlans() {
    setLoadStatus("Loading...");

    try {
        const doc = await fetchXml(`${DATA_URL}?cmd=list`);
        state.currentDoc = doc;
        state.topPanel = applyTopPanelFromDocument(doc, els, { userTagNames: ["Name", "UserName"] });
        state.lookups = parseLookups(doc);
        state.plans = parsePlans(doc);

        populateModuleSelects();
        syncFilterClearButton();
        renderPlans();
        setLoadStatus(`Loaded ${state.plans.length} subscriptions`);
    } catch (error) {
        console.error(error);
        setLoadStatus("Error");
        state.plans = [];
        renderPlans();
    }
}

function parseLookups(doc) {
    const lookups = {};

    getDirectChildren(getDirectChild(doc.documentElement, "lookups"), "lookup").forEach(function (lookupNode) {
        const name = getAttribute(lookupNode, "name", "");
        if (!name) {
            return;
        }

        lookups[name] = getDirectChildren(lookupNode, "option").map(function (optionNode) {
            return {
                code: getAttribute(optionNode, "code", ""),
                label: getAttribute(optionNode, "label", ""),
                moduleCode: getAttribute(optionNode, "moduleCode", ""),
                moduleName: getAttribute(optionNode, "moduleName", ""),
                description: getAttribute(optionNode, "description", ""),
                active: normalizeBool(getAttribute(optionNode, "active", "true"))
            };
        }).filter(function (option) {
            return option.code;
        });
    });

    return lookups;
}

function parsePlans(doc) {
    const plansElement = getDirectChild(doc.documentElement, "subscriptionPlans");
    const planNodes = getDirectChildren(plansElement, "subscriptionPlan");

    return planNodes.map(parsePlan);
}

function parsePlan(node) {
    return {
        subscriptionPlanId: getChildText(node, "SubscriptionPlanId", ""),
        moduleCode: getChildText(node, "ModuleCode", ""),
        moduleName: getChildText(node, "ModuleName", ""),
        description: getChildText(node, "Description", ""),
        validFrom: getChildText(node, "ValidFrom", ""),
        validTo: getChildText(node, "ValidTo", ""),
        trialDays: getChildText(node, "TrialDays", ""),
        active: normalizeBool(getChildText(node, "Active", "false")),
        displayName: getChildText(node, "DisplayName", ""),
        isCurrent: normalizeBool(getChildText(node, "IsCurrent", "false"))
    };
}

function renderPlans() {
    const body = els.plansBody;
    const empty = els.plansEmpty;

    if (!body) {
        return;
    }

    const filter = normalizeText(els.planFilter ? els.planFilter.value : "");
    state.filteredPlans = state.plans.filter(function (plan) {
        if (!filter) {
            return true;
        }

        return [
            plan.moduleName,
            plan.description,
            plan.validFrom,
            plan.validTo
        ].some(function (value) {
            return normalizeText(value).includes(filter);
        });
    });

    setPlanTableCount(state.filteredPlans.length, state.plans.length);

    if (!state.filteredPlans.length) {
        body.innerHTML = "";
        if (empty) {
            empty.hidden = false;
            empty.textContent = state.plans.length ? "No subscriptions match the filter." : "No subscriptions.";
        }
        return;
    }

    if (empty) {
        empty.hidden = true;
    }

    body.innerHTML = state.filteredPlans.map(function (plan) {
        const currentClass = plan.isCurrent ? " is-current" : "";

        return `
            <tr data-plan-id="${escapeXml(plan.subscriptionPlanId)}" class="${currentClass.trim()}">
                <td>${escapeHtml(formatSubscriptionLabel(plan.moduleName || plan.moduleCode))}</td>
                <td class="subscription-editor-description-cell" title="${escapeHtml(plan.description)}">${escapeHtml(plan.description)}</td>
                <td>${escapeHtml(formatDate(plan.validFrom))}</td>
                <td>${escapeHtml(formatDate(plan.validTo))}</td>
                <td class="department-main-active-cell">${renderActiveIcon(plan.active)}</td>
            </tr>
        `;
    }).join("");

    body.querySelectorAll("tr[data-plan-id]").forEach(function (row) {
        row.addEventListener("click", function () {
            body.querySelectorAll("tr.is-selected").forEach(function (selectedRow) {
                selectedRow.classList.remove("is-selected");
            });
            row.classList.add("is-selected");
        });

        row.addEventListener("dblclick", function () {
            const planId = row.getAttribute("data-plan-id");
            if (planId) {
                openEditDialog(planId);
            }
        });
    });
}

function populateModuleSelects(selectedModuleCode = "") {
    const allOptions = state.lookups.subscriptions || [];
    const activeOptions = allOptions.filter(function (optionItem) {
        return optionItem.active !== false;
    });
    const createSelect = els.createModuleCode;
    const moduleSelect = els.fieldModuleCode;

    if (createSelect) {
        const currentValue = createSelect.value || selectedModuleCode || activeOptions[0]?.code || "";
        fillLookupSelect(createSelect, activeOptions, currentValue, false);
    }

    if (moduleSelect) {
        const currentValue = moduleSelect.value || selectedModuleCode || activeOptions[0]?.code || "";
        fillLookupSelect(moduleSelect, allOptions, currentValue, true);
    }
}

function fillLookupSelect(select, options, currentValue, keepSelectedEvenIfInactive) {
    if (!select) {
        return;
    }

    const selectedValue = String(currentValue || "").trim();
    const selectedOption = options.find(function (optionItem) {
        return optionItem.code === selectedValue;
    });

    select.innerHTML = "";

    const visibleOptions = options.filter(function (optionItem) {
        if (keepSelectedEvenIfInactive) {
            return optionItem.active !== false || optionItem.code === selectedValue;
        }

        return optionItem.active !== false;
    });

    visibleOptions.forEach(function (optionItem) {
        const option = document.createElement("option");
        option.value = optionItem.code;
        option.textContent = formatSubscriptionLabel(optionItem.label || optionItem.code);

        if (optionItem.code === selectedValue) {
            option.selected = true;
        }

        select.appendChild(option);
    });

    if (keepSelectedEvenIfInactive && selectedValue && !Array.from(select.options).some(function (option) {
        return option.value === selectedValue;
    })) {
        const fallbackOption = document.createElement("option");
        fallbackOption.value = selectedValue;
        fallbackOption.textContent = formatSubscriptionLabel(selectedOption?.label || selectedValue);
        fallbackOption.selected = true;
        select.insertBefore(fallbackOption, select.firstChild);
    }

    if (!select.value && visibleOptions.length) {
        select.value = visibleOptions[0].code;
    }
}

async function openCreateDialog() {
    const moduleCode = els.createModuleCode ? els.createModuleCode.value : "";
    await openDialog(`${DATA_URL}?cmd=create&moduleCode=${encodeURIComponent(moduleCode)}`);
}

async function openEditDialog(planId) {
    await openDialog(`${DATA_URL}?cmd=edit&id=${encodeURIComponent(planId)}`);
}

async function openDialog(url) {
    setDialogStatus("Loading...");

    try {
        const doc = await fetchXml(url);
        state.currentDoc = doc;
        const detail = getDirectChild(doc.documentElement, "subscriptionPlanDetail");

        if (!detail) {
            throw new Error("Subscription detail was not returned.");
        }

        const planNode = getDirectChild(detail, "subscriptionPlan");
        const plan = parsePlan(planNode);
        const billingPeriods = parseBillingPeriods(detail);
        const customers = parseCustomers(detail);

        state.selectedPlanId = plan.subscriptionPlanId;
        fillDialog(plan, billingPeriods, customers);
        setActiveTab("details");
        setDialogStatus("Loaded.");
        showDialog(els.subscriptionEditorDialog);
        requestAnimationFrame(syncBillingPeriodsHeight);
    } catch (error) {
        console.error(error);
        setDialogStatus("Error loading subscription.");
    }
}

function parseBillingPeriods(detail) {
    const billingPeriodsElement = getDirectChild(detail, "billingPeriods");
    const billingPeriodNodes = getDirectChildren(billingPeriodsElement, "billingPeriod");

    return billingPeriodNodes.map(function (node) {
        return {
            subscriptionPlanBillingPeriodId: getChildText(node, "SubscriptionPlanBillingPeriodId", ""),
            subscriptionPlanId: getChildText(node, "SubscriptionPlanId", ""),
            billingPeriodCode: getChildText(node, "BillingPeriodCode", ""),
            billingPeriodName: getChildText(node, "BillingPeriodName", ""),
            description: getChildText(node, "Description", ""),
            priceAmount: getChildText(node, "PriceAmount", ""),
            currency: getChildText(node, "Currency", ""),
            active: normalizeBool(getChildText(node, "Active", "false"))
        };
    });
}

function parseCustomers(detail) {
    const customersElement = getDirectChild(detail, "customers");
    const customerNodes = getDirectChildren(customersElement, "customer");

    return customerNodes.map(function (node) {
        return {
            customerId: getChildText(node, "CustomerId", ""),
            customerName: getChildText(node, "CustomerName", ""),
            subscriptionId: getChildText(node, "SubscriptionId", ""),
            subscriptionStatus: getChildText(node, "SubscriptionStatus", ""),
            subscriptionPlanId: getChildText(node, "SubscriptionPlanId", ""),
            subscriptionPlanName: getChildText(node, "SubscriptionPlanName", ""),
            renewalAt: getChildText(node, "RenewalAt", ""),
            periodEndAt: getChildText(node, "PeriodEndAt", ""),
            trialEndAt: getChildText(node, "TrialEndAt", ""),
            gracePeriodEndsAt: getChildText(node, "GracePeriodEndsAt", "")
        };
    });
}

function fillDialog(plan, billingPeriods, customers) {
    setValue("fieldSubscriptionPlanId", plan.subscriptionPlanId);
    setValue("fieldModuleCode", plan.moduleCode);
    setValue("fieldDescription", plan.description);
    setValue("fieldValidFrom", plan.validFrom);
    setValue("fieldValidTo", plan.validTo);
    setCheckbox("fieldActive", plan.active);

    renderBillingPeriods(billingPeriods);
    renderCustomers(customers);
}

function renderBillingPeriods(billingPeriods) {
    const body = els.billingPeriodsBody;
    if (!body) {
        return;
    }

    body.innerHTML = (billingPeriods || []).map(function (billingPeriod) {
        const isActive = Boolean(billingPeriod.active);
        return `
            <tr data-billing-period-row>
                <td>
                    <input type="hidden" data-field="subscriptionPlanBillingPeriodId" value="${escapeXml(billingPeriod.subscriptionPlanBillingPeriodId)}" />
                    <input type="hidden" data-field="billingPeriodCode" value="${escapeXml(billingPeriod.billingPeriodCode)}" />
                    <input data-field="billingPeriodName" type="text" value="${escapeXml(billingPeriod.billingPeriodName)}" readonly />
                </td>
                <td><input data-field="description" type="text" value="${escapeXml(billingPeriod.description)}" readonly /></td>
                <td><input data-field="priceAmount" type="number" min="0" step="0.01" value="${escapeXml(billingPeriod.priceAmount)}" /></td>
                <td>${renderCurrencySelect(billingPeriod.currency || "EUR")}</td>
                <td class="department-main-active-cell">
                    <input type="checkbox" data-field="active" ${isActive ? "checked" : ""} />
                    <span class="department-main-active-state${isActive ? " is-active" : " is-inactive"}" data-billing-period-active-toggle title="${isActive ? "Active" : "Inactive"}" aria-label="${isActive ? "Active" : "Inactive"}">
                        <span class="department-main-active-dot ${isActive ? "is-active" : "is-inactive"}" aria-hidden="true"></span>
                    </span>
                </td>
            </tr>
        `;
    }).join("");

    syncBillingPeriodsHeight();
}

function renderCustomers(customers) {
    const body = els.subscriptionCustomersBody;
    if (!body) {
        return;
    }

    if (!customers || !customers.length) {
        body.innerHTML = "";
        return;
    }

    body.innerHTML = customers.map(function (customer) {
        return `
            <tr>
                <td>${escapeHtml(customer.customerName || customer.customerId || "—")}</td>
                <td>${escapeHtml([customer.subscriptionPlanName, customer.subscriptionStatus].filter(Boolean).join(" / "))}</td>
                <td>${escapeHtml(formatDateTime(customer.renewalAt))}</td>
                <td>${escapeHtml(formatDateTime(customer.periodEndAt))}</td>
                <td>${escapeHtml(formatDateTime(customer.trialEndAt))}</td>
                <td>${escapeHtml(formatDateTime(customer.gracePeriodEndsAt))}</td>
            </tr>
        `;
    }).join("");
}

async function saveCurrentSubscription() {
    if (!els.subscriptionEditorForm || !els.subscriptionEditorForm.reportValidity()) {
        return;
    }

    const xml = buildSaveXml();

    try {
        setDialogStatus("Saving...");
        await postXml(`${DATA_URL}?cmd=save`, xml);
        setDialogStatus("Saved.");
        closeDialogElement(els.subscriptionEditorDialog);
        await loadPlans();
    } catch (error) {
        console.error(error);
        setDialogStatus(error.message || "Save failed.");
    }
}

function buildSaveXml() {
    const plan = {
        subscriptionPlanId: value("fieldSubscriptionPlanId"),
        moduleCode: value("fieldModuleCode"),
        description: value("fieldDescription"),
        validFrom: value("fieldValidFrom"),
        validTo: value("fieldValidTo"),
        active: checkboxValue("fieldActive")
    };

    const periods = Array.from(els.billingPeriodsBody?.querySelectorAll("tr[data-billing-period-row]") || []).map(function (row) {
        return {
            subscriptionPlanBillingPeriodId: rowValue(row, "subscriptionPlanBillingPeriodId"),
            billingPeriodCode: rowValue(row, "billingPeriodCode"),
            billingPeriodName: rowValue(row, "billingPeriodName"),
            description: rowValue(row, "description"),
            priceAmount: rowValue(row, "priceAmount"),
            currency: rowValue(row, "currency"),
            active: rowBoolValue(row, "active")
        };
    });

    const parts = [];
    parts.push('<?xml version="1.0" encoding="UTF-8"?>');
    parts.push("<subscriptionEditorSave>");
    parts.push("<subscriptionPlan>");
    appendXmlElement(parts, "SubscriptionPlanId", plan.subscriptionPlanId);
    appendXmlElement(parts, "ModuleCode", plan.moduleCode);
    appendXmlElement(parts, "Description", plan.description);
    appendXmlElement(parts, "ValidFrom", plan.validFrom);
    appendXmlElement(parts, "ValidTo", plan.validTo);
    appendXmlElement(parts, "Active", plan.active);
    parts.push("</subscriptionPlan>");
    parts.push("<billingPeriods>");

    periods.forEach(function (period) {
        parts.push("<billingPeriod>");
        appendXmlElement(parts, "SubscriptionPlanBillingPeriodId", period.subscriptionPlanBillingPeriodId);
        appendXmlElement(parts, "BillingPeriodCode", period.billingPeriodCode);
        appendXmlElement(parts, "BillingPeriodName", period.billingPeriodName);
        appendXmlElement(parts, "Description", period.description);
        appendXmlElement(parts, "PriceAmount", period.priceAmount);
        appendXmlElement(parts, "Currency", period.currency);
        appendXmlElement(parts, "Active", period.active);
        parts.push("</billingPeriod>");
    });

    parts.push("</billingPeriods>");
    parts.push("</subscriptionEditorSave>");

    return parts.join("");
}

function appendXmlElement(parts, tagName, value) {
    parts.push(`<${tagName}>`);
    if (value !== null && value !== undefined && String(value).trim() !== "") {
        parts.push(escapeXml(value));
    }
    parts.push(`</${tagName}>`);
}

function setActiveTab(tabName) {
    const detailsActive = tabName === "details";
    const customersActive = tabName === "customers";

    els.tabBtnDetails?.classList.toggle("is-active", detailsActive);
    els.tabBtnCustomers?.classList.toggle("is-active", customersActive);
    els.tabBtnDetails?.setAttribute("aria-selected", String(detailsActive));
    els.tabBtnCustomers?.setAttribute("aria-selected", String(customersActive));

    els.tabPanelDetails?.classList.toggle("is-active", detailsActive);
    els.tabPanelCustomers?.classList.toggle("is-active", customersActive);
}

function setLoadStatus(textValue) {
    setText(els.loadStatus, textValue, "");
}

function setPlanTableCount(filteredCount, totalCount) {
    setText(els.planTableCount, `${filteredCount} of ${totalCount}`, "");
}

function setDialogStatus(textValue) {
    setText(els.subscriptionDialogStatus, textValue, "");
}

function setValue(id, value) {
    const element = els[id];
    if (element) {
        element.value = value ?? "";
    }
}

function setCheckbox(id, checked) {
    const element = els[id];
    if (element) {
        element.checked = Boolean(checked);
    }
}

function value(id) {
    const element = els[id];
    return element ? String(element.value || "").trim() : "";
}

function checkboxValue(id) {
    const element = els[id];
    return element ? element.checked : false;
}

function rowValue(row, fieldName) {
    const field = row.querySelector(`[data-field="${fieldName}"]`);
    return field ? String(field.value || "").trim() : "";
}

function rowBoolValue(row, fieldName) {
    const field = row.querySelector(`[data-field="${fieldName}"]`);
    if (!field) {
        return false;
    }

    if (field.type === "checkbox") {
        return Boolean(field.checked);
    }

    return normalizeBool(field.value);
}

function syncFilterClearButton() {
    if (!els.btnClearFilter) {
        return;
    }

    const hasValue = Boolean(els.planFilter && els.planFilter.value.trim());
    els.btnClearFilter.hidden = !hasValue;
}

function normalizeText(value) {
    return String(value || "").trim().toLowerCase();
}

function normalizeBool(value) {
    return ["true", "1", "yes"].includes(String(value || "").trim().toLowerCase());
}

function formatDate(value) {
    if (!value) {
        return "—";
    }

    const date = new Date(`${value}T00:00:00`);
    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return new Intl.DateTimeFormat("da-DK", { dateStyle: "medium" }).format(date);
}

function formatDateTime(value) {
    if (!value) {
        return "—";
    }

    const normalized = String(value).trim().replace(" ", "T");
    const date = new Date(normalized.endsWith("Z") ? normalized : `${normalized}Z`);

    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return new Intl.DateTimeFormat("da-DK", { dateStyle: "short", timeStyle: "short" }).format(date);
}

function formatMoney(amount, currency) {
    const valueNumber = Number.parseFloat(String(amount || "0").replace(",", "."));
    const safeCurrency = currency || "EUR";

    if (!Number.isFinite(valueNumber)) {
        return `${amount || "0"} ${safeCurrency}`;
    }

    return `${valueNumber.toFixed(2)} ${safeCurrency}`;
}

function formatSubscriptionLabel(value) {
    const textValue = String(value || "").trim();

    if (!textValue) {
        return "";
    }

    if (/module$/i.test(textValue)) {
        return textValue;
    }

    return `${textValue} Module`;
}

function renderActiveIcon(value) {
    if (value) {
        return '<span class="department-main-active-state" title="Active" aria-label="Active"><span class="department-main-active-dot is-active" aria-hidden="true"></span></span>';
    }

    return '<span class="department-main-active-state" title="Inactive" aria-label="Inactive"><span class="department-main-active-dot is-inactive" aria-hidden="true"></span></span>';
}

function renderCurrencySelect(selectedCurrency) {
    const value = String(selectedCurrency || "EUR").trim().toUpperCase();
    const options = EUROPEAN_CURRENCIES.slice();

    if (!options.some(function (currency) {
        return currency.code === value;
    })) {
        options.unshift({ code: value, label: `${value} - Custom` });
    }

    return `
        <select data-field="currency">
            ${options.map(function (currency) {
                return `<option value="${escapeXml(currency.code)}"${currency.code === value ? " selected" : ""}>${escapeHtml(currency.label)}</option>`;
            }).join("")}
        </select>
    `;
}

function syncBillingPeriodsHeight() {
    const scroll = els.billingPeriodsScroll;
    const body = els.billingPeriodsBody;

    if (!scroll || !body) {
        return;
    }

    const table = body.closest("table");
    const headerRow = table?.querySelector("thead tr");
    const rows = Array.from(body.querySelectorAll("tr[data-billing-period-row]"));

    if (!headerRow || !rows.length) {
        scroll.style.height = "";
        return;
    }

    const visibleRows = rows.slice(0, 4);
    const headerHeight = headerRow.getBoundingClientRect().height;
    const rowsHeight = visibleRows.reduce(function (sum, row) {
        return sum + row.getBoundingClientRect().height;
    }, 0);

    scroll.style.height = `${Math.ceil(headerHeight + rowsHeight + 2)}px`;
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
