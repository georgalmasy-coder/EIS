import { initMenu } from "../components/menu.js";
import { initHelpDialog } from "../components/help-dialog.js";
import { applyTopPanelFromDocument } from "../core/page-header.js";
import { toDateTimeLocalValue } from "../core/date.js";

const API_URL = "/api/admin/users";

const STORAGE_PREFIX = "userAdministration";
const STORAGE_FILTER = `${STORAGE_PREFIX}.table.filter`;
const STORAGE_SORT_KEY = `${STORAGE_PREFIX}.table.sortKey`;
const STORAGE_SORT_DIRECTION = `${STORAGE_PREFIX}.table.sortDirection`;
const STORAGE_COLUMN_WIDTHS = `${STORAGE_PREFIX}.table.columnWidths`;
const STORAGE_GROUP_BY = `${STORAGE_PREFIX}.table.groupBy`;
const STORAGE_GROUP_COLLAPSED = `${STORAGE_PREFIX}.table.groupCollapsed`;

const DEFAULT_COLUMN_WIDTHS = {
    customerNames: "220px",
    name: "180px",
    userRole: "180px",
    email: "240px",
    phone: "150px",
    departmentDescription: "180px",
    mfa: "140px",
    lockedUntil: "180px",
    lastLoginAt: "180px",
    active: "90px"
};

const DEFAULT_PHONE_RULE = {
    country: "Denmark",
    code: "+45",
    min: 8,
    max: 8,
    example: "12 34 56 78"
};

const PHONE_RULES = [
    { country: "Denmark", code: "+45", min: 8, max: 8, example: "12 34 56 78" },
    { country: "Sweden", code: "+46", min: 7, max: 10, example: "70 123 45 67" },
    { country: "Norway", code: "+47", min: 8, max: 8, example: "123 45 678" },
    { country: "Germany", code: "+49", min: 7, max: 13, example: "151 23456789" },
    { country: "United Kingdom", code: "+44", min: 10, max: 10, example: "7123 456789" },
    { country: "United States", code: "+1", min: 10, max: 10, example: "(123) 456-7890" },
    { country: "Canada", code: "+1", min: 10, max: 10, example: "(123) 456-7890" },
    { country: "France", code: "+33", min: 9, max: 9, example: "6 12 34 56 78" },
    { country: "Netherlands", code: "+31", min: 9, max: 9, example: "6 12345678" },
    { country: "Belgium", code: "+32", min: 8, max: 9, example: "470 12 34 56" },
    { country: "Spain", code: "+34", min: 9, max: 9, example: "612 34 56 78" },
    { country: "Italy", code: "+39", min: 8, max: 11, example: "312 345 6789" },
    { country: "Finland", code: "+358", min: 7, max: 10, example: "40 1234567" },
    { country: "Poland", code: "+48", min: 9, max: 9, example: "123 456 789" },
    { country: "Portugal", code: "+351", min: 9, max: 9, example: "912 345 678" },
    { country: "Switzerland", code: "+41", min: 9, max: 9, example: "79 123 45 67" },
    { country: "Austria", code: "+43", min: 7, max: 13, example: "664 1234567" },
    { country: "Ireland", code: "+353", min: 7, max: 9, example: "85 123 4567" },
    { country: "Iceland", code: "+354", min: 7, max: 7, example: "123 4567" },
    { country: "Faroe Islands", code: "+298", min: 6, max: 6, example: "123456" },
    { country: "Greenland", code: "+299", min: 6, max: 6, example: "123456" }
];

const state = {
    users: [],
    filteredUsers: [],
    selectedUserId: null,
    sortKey: localStorage.getItem(STORAGE_SORT_KEY) || "name",
    sortDirection: localStorage.getItem(STORAGE_SORT_DIRECTION) || "asc",
    columnWidths: loadColumnWidths(),
    groupBy: loadGroupBy(),
    collapsedGroupPaths: loadCollapsedGroupPaths(),
    currentDoc: null,
    userDetail: null,
    lookups: {},
    departments: [],
    linkedCustomerIds: [],
    saving: false
};

const els = {};

document.addEventListener("DOMContentLoaded", initialize);

function initialize() {
    initMenu();
    initHelpDialog();
    collectElements();
    fillPhoneCountryCodes(DEFAULT_PHONE_RULE);
    applyColumnWidths();
    bindEvents();
    initializeTabs();
    renderGroupByZone();

    if (els.userFilter) {
        els.userFilter.value = localStorage.getItem(STORAGE_FILTER) || "";
        syncFilterClearButton(els.userFilter);
    }

    loadUsers();
}

function collectElements() {
    els.loadStatus = document.getElementById("loadStatus");
    els.customerName = document.getElementById("customerName");
    els.projectName = document.getElementById("projectName");
    els.userName = document.getElementById("userName");
    els.userFilter = document.getElementById("userFilter");
    els.btnClearFilter = document.getElementById("btnClearFilter");
    els.btnClearGrouping = document.getElementById("btnClearGrouping");
    els.groupByZone = document.getElementById("groupByZone");
    els.userColGroup = document.getElementById("userColGroup");
    els.userHeaderRow = document.getElementById("userHeaderRow");
    els.usersBody = document.getElementById("usersBody");
    els.usersEmpty = document.getElementById("usersEmpty");

    els.userEditDialog = document.getElementById("userEditDialog");
    els.userEditForm = document.getElementById("userEditForm");
    els.userDialogTitle = document.getElementById("userDialogTitle");
    els.userDialogStatus = document.getElementById("userDialogStatus");
    els.btnSaveUser = document.getElementById("btnSaveUser");
    els.btnCancelUser = document.getElementById("btnCancelUser");
    els.btnResetMfa = document.getElementById("btnResetMfa");
    els.btnDisableMfa = document.getElementById("btnDisableMfa");
    els.btnMarkMfaResetRequired = document.getElementById("btnMarkMfaResetRequired");
    els.btnClearMfaResetRequired = document.getElementById("btnClearMfaResetRequired");
    els.btnSendPasswordResetLink = document.getElementById("btnSendPasswordResetLink");
    els.fieldPhoneCountryCode = document.getElementById("fieldPhoneCountryCode");
    els.fieldPhoneHelp = document.getElementById("fieldPhoneHelp");

    document.querySelectorAll("[id]").forEach(function (element) {
        els[element.id] = element;
    });
}

function bindEvents() {
    if (els.userFilter) {
        els.userFilter.addEventListener("input", function () {
            localStorage.setItem(STORAGE_FILTER, els.userFilter.value || "");
            syncFilterClearButton(els.userFilter);
            applyFilterSortAndRender();
        });

        els.userFilter.addEventListener("keydown", function (event) {
            if (event.key === "Escape") {
                els.userFilter.value = "";
                localStorage.setItem(STORAGE_FILTER, "");
                syncFilterClearButton(els.userFilter);
                applyFilterSortAndRender();
                els.userFilter.blur();
            }
        });
    }

    if (els.btnClearFilter) {
        els.btnClearFilter.addEventListener("click", function () {
            if (els.userFilter) {
                els.userFilter.value = "";
                syncFilterClearButton(els.userFilter);
                els.userFilter.focus();
            }

            localStorage.setItem(STORAGE_FILTER, "");
            applyFilterSortAndRender();
        });
    }

    if (els.userHeaderRow) {
        els.userHeaderRow.querySelectorAll("th[data-key]").forEach(function (th) {
            th.addEventListener("click", function (event) {
                if (event.target && event.target.classList.contains("col-resizer")) {
                    return;
                }

                const key = th.getAttribute("data-key");
                changeSort(key);
            });

            th.addEventListener("dragstart", function (event) {
                const key = th.getAttribute("data-key");
                event.dataTransfer.setData("text/plain", key);
                event.dataTransfer.effectAllowed = "copy";
            });
        });
    }

    if (els.groupByZone) {
        els.groupByZone.addEventListener("dragover", function (event) {
            event.preventDefault();
        });

        els.groupByZone.addEventListener("drop", function (event) {
            event.preventDefault();
            addGroupByKey(event.dataTransfer.getData("text/plain"));
        });
    }

    if (els.btnClearGrouping) {
        els.btnClearGrouping.addEventListener("click", function () {
            state.groupBy = [];
            state.collapsedGroupPaths = [];
            persistGroupBy();
            persistCollapsedGroupPaths();
            renderGroupByZone();
            applyFilterSortAndRender();
        });
    }

    bindColumnResize();

    if (els.btnCancelUser) {
        els.btnCancelUser.addEventListener("click", closeDialog);
    }

    if (els.btnSaveUser) {
        els.btnSaveUser.addEventListener("click", saveUserAdministration);
    }

    if (els.btnResetMfa) {
        els.btnResetMfa.addEventListener("click", function () {
            runAction("resetMfa");
        });
    }

    if (els.btnDisableMfa) {
        els.btnDisableMfa.addEventListener("click", function () {
            runAction("disableMfa");
        });
    }

    if (els.btnMarkMfaResetRequired) {
        els.btnMarkMfaResetRequired.addEventListener("click", function () {
            runAction("markMfaResetRequired");
        });
    }

    if (els.btnClearMfaResetRequired) {
        els.btnClearMfaResetRequired.addEventListener("click", function () {
            runAction("clearMfaResetRequired");
        });
    }

    if (els.btnSendPasswordResetLink) {
        els.btnSendPasswordResetLink.addEventListener("click", function () {
            runAction("sendPasswordResetLink");
        });
    }

    if (els.fieldPhoneCountryCode) {
        els.fieldPhoneCountryCode.addEventListener("change", function () {
            formatCurrentPhoneValue();
        });
    }

    if (els.fieldPhone) {
        els.fieldPhone.addEventListener("input", function () {
            formatCurrentPhoneValue();
        });
    }
}

function initializeTabs() {
    document.querySelectorAll(".page-tab-btn[role='tab']").forEach(function (button) {
        button.addEventListener("click", function () {
            const panelId = button.getAttribute("aria-controls");
            if (!panelId) {
                return;
            }

            document.querySelectorAll(".page-tab-btn[role='tab']").forEach(function (otherButton) {
                otherButton.classList.toggle("is-active", otherButton === button);
                otherButton.setAttribute("aria-selected", otherButton === button ? "true" : "false");
            });

            document.querySelectorAll(".tab-panel[role='tabpanel']").forEach(function (panel) {
                panel.classList.toggle("is-active", panel.id === panelId);
            });
        });
    });
}

async function loadUsers() {
    setLoadStatus("Loading...");

    try {
        const doc = await fetchXml(API_URL);
        applyTopPanelFromDocument(doc, els, { userTagNames: ["Name", "UserName"] });
        state.lookups = parseLookups(doc);
        state.users = parseUsers(doc);
        applyFilterSortAndRender();
        setLoadStatus(`Loaded ${state.users.length} users`);
    } catch (error) {
        console.error(error);
        state.users = [];
        applyFilterSortAndRender();
        setLoadStatus("Error loading users");
    }
}

async function openUserDetail(userId) {
    if (!userId) {
        return;
    }

    state.selectedUserId = userId;
    setDialogStatus("Loading user...", "is-loading");
    clearDialog();

    if (els.userEditDialog && typeof els.userEditDialog.showModal === "function") {
        els.userEditDialog.showModal();
    }

    try {
        const doc = await fetchXml(`${API_URL}?userId=${encodeURIComponent(userId)}`);
        state.currentDoc = doc;
        applyTopPanelFromDocument(doc, els, { userTagNames: ["Name", "UserName"] });
        state.lookups = parseLookups(doc);
        fillLookupSelect("fieldUserMfaPolicy", "userMfaPolicy");

        const detail = parseUserDetail(doc);
        state.userDetail = detail.user;
        state.departments = detail.departments;
        state.linkedCustomerIds = detail.linkedCustomerIds;
        fillDialog(detail);
        setDialogStatus("Loaded.", "is-ok");
    } catch (error) {
        console.error(error);
        setDialogStatus("Error loading user.", "is-error");
    }
}

async function saveUserAdministration() {
    if (state.saving) {
        return;
    }

    const userId = value("fieldUserId");
    if (!userId) {
        setDialogStatus("User ID is missing.", "is-error");
        return;
    }

    state.saving = true;
    setSaveButtonDisabled(true);
    setDialogStatus("Saving user administration data...", "is-loading");

    try {
        const xml = buildSaveXml();
        const doc = await postXml(API_URL, xml);
        const result = parseActionResult(doc, "userAdministrationSaveResult");

        if (!result.success) {
            setDialogStatus(result.message || "User data could not be saved.", "is-error");
            return;
        }

        setDialogStatus(result.message || "User data saved.", "is-ok");
        await loadUsers();
        closeDialog();
        state.selectedUserId = null;
        state.userDetail = null;
    } catch (error) {
        console.error(error);
        setDialogStatus(`Save failed: ${error.message}`, "is-error");
    } finally {
        state.saving = false;
        setSaveButtonDisabled(false);
    }
}

async function runAction(action) {
    const userId = value("fieldUserId");
    if (!userId) {
        setDialogStatus("User ID is missing.", "is-error");
        return;
    }

    try {
        setDialogStatus(`Running ${action}...`, "is-loading");
        const doc = await postXml(`${API_URL}?action=${encodeURIComponent(action)}&userId=${encodeURIComponent(userId)}`, "");
        const result = parseActionResult(doc, "userAdministrationActionResult");

        if (!result.success) {
            setDialogStatus(result.message || "Action failed.", "is-error");
            return;
        }

        setDialogStatus(result.message || "Action completed.", "is-ok");
        await loadUsers();
        await openUserDetail(parseInt(userId, 10));
    } catch (error) {
        console.error(error);
        setDialogStatus(`Action failed: ${error.message}`, "is-error");
    }
}

function parseUsers(doc) {
    if (!doc) {
        return [];
    }

    return Array.from(doc.querySelectorAll("users > user")).map(function (node) {
        return parseUserNode(node);
    });
}

function parseUserDetail(doc) {
    const detail = doc ? doc.querySelector("userDetail") : null;

    if (!detail) {
        return {
            user: {},
            linkedCustomerIds: [],
            customers: [],
            departments: []
        };
    }

    return {
        user: parseUserNode(detail.querySelector(":scope > user")),
        linkedCustomerIds: Array.from(detail.querySelectorAll(":scope > linkedCustomers > customer > customerId"))
            .map(function (node) {
                const parsed = parseInt(node.textContent || "", 10);
                return Number.isFinite(parsed) ? parsed : null;
            })
            .filter(Boolean),
        customers: Array.from(detail.querySelectorAll(":scope > customers > customer")).map(parseCustomerNode),
        departments: Array.from(detail.querySelectorAll(":scope > departments > department")).map(parseDepartmentNode)
    };
}

function parseUserNode(node) {
    if (!node) {
        return {};
    }

    return {
        userId: intText(node, "userId"),
        initials: text(node, "initials"),
        name: text(node, "name"),
        email: text(node, "email"),
        phone: text(node, "phone"),
        departmentId: intText(node, "departmentId"),
        departmentName: text(node, "departmentName"),
        departmentDescription: text(node, "departmentDescription"),
        customerNames: text(node, "customerNames"),
        active: boolText(node, "active"),
        userRole: intText(node, "userRole"),
        userRoleLabel: text(node, "userRoleLabel"),
        lockedUntil: text(node, "lockedUntil"),
        lastLoginAt: text(node, "lastLoginAt"),
        mfaEnabled: boolText(node, "mfaEnabled"),
        mfaVerified: boolText(node, "mfaVerified"),
        mfaSecret: boolText(node, "mfaSecret"),
        userMfaPolicy: text(node, "userMfaPolicy"),
        mfaResetRequired: boolText(node, "mfaResetRequired"),
        mfaResetAt: text(node, "mfaResetAt"),
        mfaResetByUserId: intText(node, "mfaResetByUserId"),
        passwordSet: boolText(node, "passwordSet")
    };
}

function parseCustomerNode(node) {
    return {
        customerId: intText(node, "customerId"),
        customerName: text(node, "customerName"),
        country: text(node, "country"),
        customerStatus: text(node, "customerStatus"),
        contactEmail: text(node, "contactEmail")
    };
}

function parseDepartmentNode(node) {
    return {
        departmentId: intText(node, "departmentId"),
        customerId: intText(node, "customerId"),
        customerName: text(node, "customerName"),
        departmentName: text(node, "departmentName"),
        departmentDescription: text(node, "departmentDescription"),
        active: boolText(node, "active"),
        displayName: text(node, "displayName")
    };
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
            const country = optionNode.getAttribute("country") || optionNode.getAttribute("label") || "";
            return {
                code: optionNode.getAttribute("code") || "",
                country,
                label: optionNode.getAttribute("label") || country || optionNode.getAttribute("code") || "",
                min: parseInt(optionNode.getAttribute("min") || "", 10),
                max: parseInt(optionNode.getAttribute("max") || "", 10),
                example: optionNode.getAttribute("example") || ""
            };
        }).filter(function (option) {
            return option.code;
        });
    });

    return lookups;
}

function fillDialog(detail) {
    const user = detail.user || {};
    const linkedCountry = getLinkedCustomerCountry(detail.customers || [], detail.linkedCustomerIds || []);
    const phoneRule = getPhoneRuleForValue(user.phone, linkedCountry);

    setValue("fieldUserId", user.userId);
    setValue("fieldInitials", user.initials);
    setValue("fieldName", user.name);
    setValue("fieldEmail", user.email);
    fillLookupSelect("fieldUserRole", "userRole", user.userRole == null ? "1" : String(user.userRole));
    fillPhoneCountryCodes(phoneRule);
    setValue("fieldPhone", formatPhoneNumberForDisplay(user.phone, phoneRule));
    setValue("fieldLockedUntil", toDateTimeLocalValue(user.lockedUntil));
    setValue("fieldCustomerNames", user.customerNames);
    setValue("fieldMfaResetByUserId", user.mfaResetByUserId);
    const mfaVerifiedField = document.getElementById("fieldMfaVerified");
    if (mfaVerifiedField) {
        mfaVerifiedField.checked = Boolean(user.mfaVerified);
    }

    const mfaSecretField = document.getElementById("fieldMfaSecret");
    if (mfaSecretField) {
        mfaSecretField.checked = Boolean(user.mfaSecret);
    }

    const mfaResetRequiredField = document.getElementById("fieldMfaResetRequired");
    if (mfaResetRequiredField) {
        mfaResetRequiredField.checked = Boolean(user.mfaResetRequired);
    }

    const passwordSetField = document.getElementById("fieldPasswordSet");
    if (passwordSetField) {
        passwordSetField.checked = Boolean(user.passwordSet);
    }

    setValue("fieldActive", null);
    const activeField = document.getElementById("fieldActive");
    if (activeField) {
        activeField.checked = Boolean(user.active);
    }

    fillLookupSelect("fieldUserMfaPolicy", "userMfaPolicy", user.userMfaPolicy || "DEFAULT");
    fillDepartmentSelect(detail.departments || [], user.departmentId);
    updatePhoneConstraints();
    updatePhoneHelp();

    if (els.userDialogTitle) {
        els.userDialogTitle.textContent = `User Administration - ${user.name || user.email || user.userId || ""}`;
    }
}

function fillDepartmentSelect(departments, selectedDepartmentId) {
    const select = document.getElementById("fieldDepartmentId");
    if (!select) {
        return;
    }

    const previousValue = String(selectedDepartmentId || select.value || "");
    select.innerHTML = `<option value="">—</option>` + (departments || []).map(function (department) {
        const value = department.departmentId == null ? "" : String(department.departmentId);
        const selected = value === previousValue ? " selected" : "";
        const label = department.displayName || [department.customerName, department.departmentName, department.departmentDescription].filter(Boolean).join(" - ");
        return `<option value="${escapeAttribute(value)}"${selected}>${escapeHtml(label || value)}</option>`;
    }).join("");
}

function fillLookupSelect(selectId, lookupName, selectedValue) {
    const select = document.getElementById(selectId);
    if (!select) {
        return;
    }

    const options = state.lookups[lookupName] || [];
    const currentValue = selectedValue == null ? String(select.value || "") : String(selectedValue);

    select.innerHTML = options.map(function (lookupOption) {
        const selected = lookupOption.code === currentValue ? " selected" : "";
        return `<option value="${escapeAttribute(lookupOption.code)}"${selected}>${escapeHtml(lookupOption.label || lookupOption.code)}</option>`;
    }).join("");
}

function getLinkedCustomerCountry(customers, linkedIds) {
    const ids = new Set((linkedIds || []).map(function (value) {
        return Number(value);
    }));

    const linkedCustomer = (customers || []).find(function (customer) {
        return ids.has(Number(customer.customerId));
    });

    return linkedCustomer ? linkedCustomer.country || "" : "";
}

function fillPhoneCountryCodes(selectedRule) {
    if (!els.fieldPhoneCountryCode) {
        return;
    }

    const currentRule = selectedRule || getPhoneRuleByCode(els.fieldPhoneCountryCode.value) || DEFAULT_PHONE_RULE;
    const phoneRules = getPhoneCountryRules();
    els.fieldPhoneCountryCode.innerHTML = phoneRules
        .slice()
        .sort(function (left, right) {
            return String(left.country || "").localeCompare(String(right.country || ""));
        })
        .map(function (rule) {
            const selected = rule.code === currentRule.code && rule.country === currentRule.country ? " selected" : "";
            return `<option value="${escapeAttribute(rule.code)}" data-country="${escapeAttribute(rule.country)}" data-min="${escapeAttribute(rule.min)}" data-max="${escapeAttribute(rule.max)}" data-example="${escapeAttribute(rule.example)}"${selected}>${escapeHtml(`${rule.country} (${rule.code})`)}</option>`;
        }).join("");

    selectPhoneCountryRule(currentRule);
}

function getPhoneRuleByCountry(country) {
    const normalizedCountry = normalizeCountryCode(country);
    const phoneRules = getPhoneCountryRules();
    return phoneRules.find(function (rule) {
        return normalizeCountryCode(rule.country) === normalizedCountry;
    }) || null;
}

function getPhoneRuleByCode(code) {
    const normalizedCode = String(code || "").trim();
    const phoneRules = getPhoneCountryRules();
    return phoneRules.find(function (rule) {
        return rule.code === normalizedCode;
    }) || null;
}

function getPhoneRuleForValue(phone, fallbackCountry) {
    const normalizedPhone = normalizePhoneNumber(phone);
    const phoneRules = getPhoneCountryRules();
    const fallbackRule = getPhoneRuleByCountry(fallbackCountry) || getPhoneRuleByCountry(DEFAULT_PHONE_RULE.country) || DEFAULT_PHONE_RULE;

    if (!normalizedPhone) {
        return fallbackRule;
    }

    const prefixMatches = phoneRules.filter(function (rule) {
        return normalizedPhone.startsWith(rule.code);
    });

    if (prefixMatches.length === 1) {
        return prefixMatches[0];
    }

    if (prefixMatches.length > 1) {
        const fallbackMatch = prefixMatches.find(function (rule) {
            return normalizeCountryCode(rule.country) === normalizeCountryCode(fallbackCountry);
        });

        return fallbackMatch || prefixMatches[0];
    }

    return fallbackRule;
}

function selectedPhoneRule() {
    if (!els.fieldPhoneCountryCode) {
        return DEFAULT_PHONE_RULE;
    }

    const selectedOption = els.fieldPhoneCountryCode.options[els.fieldPhoneCountryCode.selectedIndex];

    if (!selectedOption) {
        return getPhoneRuleByCountry(DEFAULT_PHONE_RULE.country) || DEFAULT_PHONE_RULE;
    }

    const code = String(selectedOption.value || "").trim();
    const country = String(selectedOption.getAttribute("data-country") || "").trim();
    const phoneRules = getPhoneCountryRules();

    return phoneRules.find(function (rule) {
        return rule.code === code && rule.country === country;
    }) || getPhoneRuleByCode(code) || getPhoneRuleByCountry(country) || DEFAULT_PHONE_RULE;
}

function getPhoneCountryRules() {
    const countryCodeLookup = state.lookups && state.lookups.countryCode;

    if (Array.isArray(countryCodeLookup) && countryCodeLookup.length) {
        return countryCodeLookup.map(function (rule) {
            return {
                country: String(rule.country || rule.label || "").trim(),
                code: String(rule.code || "").trim(),
                min: Number.isFinite(rule.min) ? rule.min : DEFAULT_PHONE_RULE.min,
                max: Number.isFinite(rule.max) ? rule.max : DEFAULT_PHONE_RULE.max,
                example: String(rule.example || "").trim()
            };
        }).filter(function (rule) {
            return rule.code;
        });
    }

    return PHONE_RULES.slice();
}

function selectPhoneCountryRule(rule) {
    if (!els.fieldPhoneCountryCode) {
        return;
    }

    const selectedRule = rule || DEFAULT_PHONE_RULE;
    const options = Array.from(els.fieldPhoneCountryCode.options || []);
    const matchIndex = options.findIndex(function (option) {
        return option.value === selectedRule.code && String(option.getAttribute("data-country") || "") === selectedRule.country;
    });

    if (matchIndex >= 0) {
        els.fieldPhoneCountryCode.selectedIndex = matchIndex;
        return;
    }

    const fallbackIndex = options.findIndex(function (option) {
        return option.value === DEFAULT_PHONE_RULE.code && String(option.getAttribute("data-country") || "") === DEFAULT_PHONE_RULE.country;
    });

    els.fieldPhoneCountryCode.selectedIndex = fallbackIndex >= 0 ? fallbackIndex : 0;
}

function onlyDigits(value) {
    return String(value == null ? "" : value).replace(/\D/g, "");
}

function extractLocalPhoneDigits(value, rule) {
    const normalized = normalizePhoneNumber(value);
    const selectedRule = rule || DEFAULT_PHONE_RULE;

    if (!normalized) {
        return "";
    }

    if (selectedRule.code && normalized.startsWith(selectedRule.code)) {
        return onlyDigits(normalized.slice(selectedRule.code.length));
    }

    return onlyDigits(normalized);
}

function formatPhoneDigits(digits, rule) {
    const selectedRule = rule || DEFAULT_PHONE_RULE;
    const safeDigits = onlyDigits(digits).slice(0, selectedRule.max || 15);

    if (!safeDigits) {
        return "";
    }

    if (selectedRule.code === "+45" && safeDigits.length <= 8) {
        return safeDigits.replace(/(\d{2})(?=\d)/g, "$1 ").trim();
    }

    if (selectedRule.code === "+47" && safeDigits.length <= 8) {
        return safeDigits.replace(/^(\d{3})(\d{0,2})(\d{0,3}).*/, function (_match, first, second, third) {
            return [first, second, third].filter(Boolean).join(" ");
        });
    }

    if (selectedRule.code === "+1" && safeDigits.length <= 10) {
        if (safeDigits.length <= 3) {
            return safeDigits;
        }

        if (safeDigits.length <= 6) {
            return `(${safeDigits.slice(0, 3)}) ${safeDigits.slice(3)}`;
        }

        return `(${safeDigits.slice(0, 3)}) ${safeDigits.slice(3, 6)}-${safeDigits.slice(6)}`;
    }

    if (selectedRule.code === "+44" && safeDigits.length <= 10) {
        return safeDigits.replace(/^(\d{4})(\d{0,6}).*/, function (_match, first, second) {
            return [first, second].filter(Boolean).join(" ");
        });
    }

    if (selectedRule.code === "+33" && safeDigits.length <= 9) {
        return safeDigits.replace(/^(\d)(\d{0,2})(\d{0,2})(\d{0,2})(\d{0,2}).*/, function (_match, first, second, third, fourth, fifth) {
            return [first, second, third, fourth, fifth].filter(Boolean).join(" ");
        });
    }

    if (selectedRule.code === "+48" && safeDigits.length <= 9) {
        return safeDigits.replace(/^(\d{3})(\d{0,3})(\d{0,3}).*/, function (_match, first, second, third) {
            return [first, second, third].filter(Boolean).join(" ");
        });
    }

    if (safeDigits.length <= 6) {
        return safeDigits;
    }

    if (safeDigits.length <= 10) {
        return safeDigits.replace(/^(\d{3})(\d{0,3})(\d{0,4}).*/, function (_match, first, second, third) {
            return [first, second, third].filter(Boolean).join(" ");
        });
    }

    return safeDigits.replace(/^(\d{3})(\d{0,4})(\d{0,4})(\d{0,4}).*/, function (_match, first, second, third, fourth) {
        return [first, second, third, fourth].filter(Boolean).join(" ");
    });
}

function updatePhoneConstraints() {
    const input = document.getElementById("fieldPhone");
    if (!input) {
        return;
    }

    const rule = selectedPhoneRule();
    input.setAttribute("inputmode", "tel");
    input.setAttribute("autocomplete", "tel");
    input.setAttribute("type", "tel");
    input.setAttribute("placeholder", rule.example || "");
    input.setAttribute("pattern", phonePatternForRule(rule));
    input.setAttribute("title", phoneTitleForRule(rule));
    input.maxLength = Math.max((rule.max || 15) + 6, 15);
}

function updatePhoneHelp() {
    if (!els.fieldPhoneHelp) {
        return;
    }

    const rule = selectedPhoneRule();

    if (rule.min === rule.max) {
        els.fieldPhoneHelp.textContent = `${rule.country}: exactly ${rule.min} digits. Example: ${rule.example}`;
    } else {
        els.fieldPhoneHelp.textContent = `${rule.country}: ${rule.min}-${rule.max} digits. Example: ${rule.example}`;
    }
}

function formatPhoneNumberForDisplay(value, rule) {
    const selectedRule = rule || DEFAULT_PHONE_RULE;
    const localDigits = extractLocalPhoneDigits(value, selectedRule);
    return formatPhoneDigits(localDigits, selectedRule);
}

function formatCurrentPhoneValue() {
    const input = document.getElementById("fieldPhone");
    if (!input) {
        return;
    }

    const rule = selectedPhoneRule();
    input.value = formatPhoneDigits(extractLocalPhoneDigits(input.value, rule), rule);
    updatePhoneConstraints();
    updatePhoneHelp();
}

function validatePhoneNumber() {
    const input = document.getElementById("fieldPhone");
    if (!input) {
        return null;
    }

    const rule = selectedPhoneRule();
    const digits = onlyDigits(input.value);

    if (!digits) {
        return isPhoneRequired() ? "Phone number is required." : null;
    }

    if (rule.min === rule.max && digits.length !== rule.min) {
        return `Phone number for ${rule.country} must contain exactly ${rule.min} digits.`;
    }

    if (digits.length < rule.min || digits.length > rule.max) {
        return `Phone number for ${rule.country} must contain between ${rule.min} and ${rule.max} digits.`;
    }

    return null;
}

function isPhoneRequired() {
    const phoneNode = state.currentDoc
        ? state.currentDoc.querySelector("userDetail > user > UserPhone, userDetail > user > Phone, user > UserPhone, user > Phone")
        : null;

    return String(phoneNode?.getAttribute("required") || "").toLowerCase() === "true";
}

function getFullPhoneNumber() {
    const rule = selectedPhoneRule();
    const input = document.getElementById("fieldPhone");
    if (!input) {
        return "";
    }

    const digits = onlyDigits(input.value);

    if (!digits) {
        return "";
    }

    return `${rule.code} ${formatPhoneDigits(digits, rule)}`.trim();
}

function phonePatternForRule(rule) {
    return "^\\d[\\d\\s().-]{3,}$";
}

function phoneTitleForRule(rule) {
    if (!rule || !rule.country) {
        return "Phone number";
    }

    return `Phone number for ${rule.country}`;
}

function normalizeCountryCode(country) {
    const normalized = String(country || "")
        .trim()
        .toUpperCase()
        .replace(/[\s-]+/g, "_");

    const map = {
        DENMARK: "DK",
        DANMARK: "DK",
        SWEDEN: "SE",
        SVERIGE: "SE",
        NORWAY: "NO",
        GERMANY: "DE",
        DEUTSCHLAND: "DE",
        UNITED_KINGDOM: "GB",
        UK: "GB",
        GREAT_BRITAIN: "GB",
        UNITED_STATES: "US",
        USA: "US",
        CANADA: "CA",
        FRANCE: "FR",
        NETHERLANDS: "NL",
        BELGIUM: "BE",
        SPAIN: "ES",
        ITALY: "IT",
        FINLAND: "FI",
        POLAND: "PL",
        PORTUGAL: "PT",
        SWITZERLAND: "CH",
        AUSTRIA: "AT",
        IRELAND: "IE",
        ICELAND: "IS",
        FAROE_ISLANDS: "FO",
        GREENLAND: "GL"
    };

    return map[normalized] || normalized;
}

function normalizePhoneNumber(value) {
    return String(value == null ? "" : value)
        .trim()
        .replace(/[()\s.-]/g, "");
}

function buildSaveXml() {
    const phone = getFullPhoneNumber();
    const phoneValidationError = validatePhoneNumber();

    if (phoneValidationError) {
        throw new Error(phoneValidationError);
    }

    const xml = [];
    xml.push('<?xml version="1.0" encoding="UTF-8"?>');
    xml.push("<userAdministrationSave>");
    xml.push("<user>");
    appendXmlElement(xml, "userId", value("fieldUserId"));
    appendXmlElement(xml, "initials", value("fieldInitials"));
    appendXmlElement(xml, "name", value("fieldName"));
    appendXmlElement(xml, "email", value("fieldEmail"));
    appendXmlElement(xml, "userRole", value("fieldUserRole"));
    appendXmlElement(xml, "phone", phone);
    appendXmlElement(xml, "departmentId", value("fieldDepartmentId"));
    appendXmlElement(xml, "active", document.getElementById("fieldActive") && document.getElementById("fieldActive").checked ? "true" : "false");
    appendXmlElement(xml, "lockedUntil", value("fieldLockedUntil"));
    appendXmlElement(xml, "userMfaPolicy", value("fieldUserMfaPolicy"));
    xml.push("</user>");

    xml.push("<linkedCustomers>");
    (state.linkedCustomerIds || []).forEach(function (customerId) {
        xml.push("<customer>");
        appendXmlElement(xml, "customerId", customerId);
        xml.push("</customer>");
    });
    xml.push("</linkedCustomers>");

    xml.push("</userAdministrationSave>");
    return xml.join("");
}

async function fetchXml(url, options = {}) {
    const response = await fetch(url, {
        ...options,
        headers: {
            "Accept": "application/xml",
            ...(options.headers || {})
        }
    });

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
        const result = parseActionResult(doc, "error");
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

function parseActionResult(doc, rootName) {
    const root = doc ? doc.querySelector(rootName) : null;
    if (!root) {
        const errorNode = doc ? doc.querySelector("error") : null;
        return {
            success: false,
            userId: null,
            message: errorNode ? errorNode.textContent || "Invalid response." : "Invalid response."
        };
    }

    if (rootName === "error") {
        return {
            success: false,
            userId: null,
            message: (root.textContent || "").trim() || "Request failed."
        };
    }

    return {
        success: text(root, "success").toLowerCase() === "true",
        userId: intText(root, "userId"),
        message: text(root, "message") || text(root, "error")
    };
}

function applyFilterSortAndRender() {
    const filter = normalize(els.userFilter ? els.userFilter.value : "");

    state.filteredUsers = state.users.filter(function (user) {
        if (!filter) {
            return true;
        }

        return [
            user.customerNames,
            user.initials,
            user.name,
            user.userRoleLabel,
            user.email,
            user.phone,
            user.departmentName,
            user.departmentDescription,
            user.userMfaPolicy,
            user.lockedUntil,
            user.lastLoginAt
        ].some(function (itemValue) {
            return normalize(itemValue).includes(filter);
        });
    });

    state.filteredUsers.sort(compareUsers);
    renderUsers();
    updateSortIndicators();
}

function compareUsers(left, right) {
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

    if (key === "active") {
        return row.active ? 1 : 0;
    }

    if (key === "mfa") {
        return row.mfaEnabled ? 1 : 0;
    }

    if (key === "userRole") {
        return Number.isFinite(row.userRole) ? row.userRole : 0;
    }

    if (key === "lockedUntil") {
        const time = dateTimeMillis(row.lockedUntil);
        return Number.isFinite(time) ? time : 0;
    }

    if (key === "lastLoginAt") {
        const time = dateTimeMillis(row.lastLoginAt);
        return Number.isFinite(time) ? time : 0;
    }

    if (key === "departmentDescription" || key === "departmentName") {
        return String(row.departmentDescription || row.departmentName || "").toLowerCase();
    }

    return String(row[key] == null ? "" : row[key]).toLowerCase();
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

function renderUsers() {
    if (!els.usersBody) {
        return;
    }

    if (!state.filteredUsers.length) {
        els.usersBody.innerHTML = "";
        if (els.usersEmpty) {
            els.usersEmpty.hidden = false;
            els.usersEmpty.textContent = state.users.length ? "No users match the filter." : "No users.";
        }
        return;
    }

    if (els.usersEmpty) {
        els.usersEmpty.hidden = true;
    }

    const rowsHtml = state.groupBy.length
        ? renderGroupedRows(state.filteredUsers, state.groupBy, [])
        : state.filteredUsers.map(renderUserRow).join("");

    els.usersBody.innerHTML = rowsHtml;

    els.usersBody.querySelectorAll(".group-toggle").forEach(function (button) {
        button.addEventListener("click", function (event) {
            event.preventDefault();
            event.stopPropagation();

            const groupPath = button.getAttribute("data-group-path");
            if (!groupPath) {
                return;
            }

            toggleGroupCollapse(groupPath);
        });
    });

    els.usersBody.querySelectorAll("tr[data-user-id]").forEach(function (row) {
        row.addEventListener("click", function () {
            els.usersBody.querySelectorAll("tr.is-selected").forEach(function (selectedRow) {
                selectedRow.classList.remove("is-selected");
            });
            row.classList.add("is-selected");
        });

        row.addEventListener("dblclick", function () {
            const userId = parseInt(row.getAttribute("data-user-id"), 10);
            openUserDetail(userId);
        });
    });
}

function renderGroupedRows(rows, groupKeys, pathParts) {
    if (!groupKeys.length) {
        return rows.map(renderUserRow).join("");
    }

    const [groupKey, ...rest] = groupKeys;
    const groups = new Map();

    rows.forEach(function (row) {
        const groupValue = groupValueText(row, groupKey);
        const key = groupValue || "—";
        if (!groups.has(key)) {
            groups.set(key, []);
        }
        groups.get(key).push(row);
    });

    let html = "";
    Array.from(groups.entries()).forEach(function ([groupValue, groupRows]) {
        const nextPath = [...pathParts, `${groupKey}:${groupValue}`];
        const groupPath = nextPath.join("|");
        const isCollapsed = isGroupCollapsed(groupPath);
        html += `
            <tr class="user-group-row${isCollapsed ? " is-collapsed" : ""}" data-group-path="${escapeAttribute(groupPath)}">
                <td colspan="10">
                    <button type="button" class="group-toggle" data-group-path="${escapeAttribute(groupPath)}" aria-expanded="${isCollapsed ? "false" : "true"}">${isCollapsed ? "▸" : "▾"}</button>
                    ${escapeHtml(labelForGroupKey(groupKey))}: ${escapeHtml(groupValue)}
                    <span class="group-count">(${groupRows.length})</span>
                </td>
            </tr>
        `;
        if (!isCollapsed) {
            html += renderGroupedRows(groupRows, rest, nextPath);
        }
    });

    return html;
}

function renderUserRow(user) {
    const locked = formatDateTime(user.lockedUntil);
    const activeIcon = renderActiveIcon(user.active);
    return `
        <tr data-user-id="${escapeAttribute(user.userId)}">
            <td title="${escapeAttribute(user.customerNames)}">${escapeHtml(user.customerNames || "—")}</td>
            <td title="${escapeAttribute(user.name)}">${escapeHtml(user.name || "—")}</td>
            <td>${renderUserRole(user.userRole, user.userRoleLabel)}</td>
            <td title="${escapeAttribute(user.email)}">${escapeHtml(user.email || "—")}</td>
            <td title="${escapeAttribute(user.phone)}">${escapeHtml(user.phone || "—")}</td>
            <td title="${escapeAttribute(user.departmentDescription)}">${escapeHtml(user.departmentDescription || user.departmentName || "—")}</td>
            <td>${renderPill(user.mfaEnabled ? "On" : "Off", user.mfaEnabled ? "pill-ok" : "pill-off")}</td>
            <td title="${escapeAttribute(locked)}">${escapeHtml(locked || "—")}</td>
            <td title="${escapeAttribute(formatDateTime(user.lastLoginAt))}">${escapeHtml(formatDateTime(user.lastLoginAt) || "—")}</td>
            <td class="user-active-cell">${activeIcon}</td>
        </tr>
    `;
}

function renderPill(textValue, className) {
    return `<span class="user-pill ${escapeAttribute(className)}">${escapeHtml(textValue)}</span>`;
}

function renderUserRole(roleId, roleLabel) {
    const label = roleLabel || userRoleLabelFromId(roleId) || "Unknown";
    const roleClass = `role-${normalizeRoleClass(label)}`;
    return `<span class="user-role-pill ${escapeAttribute(roleClass)}" title="${escapeAttribute(label)}">${escapeHtml(label)}</span>`;
}

function renderActiveIcon(value) {
    return value
        ? `<span class="user-administration-active-state" title="Active" aria-label="Active"><span class="user-administration-active-dot is-active" aria-hidden="true"></span></span>`
        : `<span class="user-administration-active-state" title="Inactive" aria-label="Inactive"><span class="user-administration-active-dot is-inactive" aria-hidden="true"></span></span>`;
}

function renderGroupByZone() {
    if (!els.groupByZone) {
        return;
    }

    if (!state.groupBy.length) {
        els.groupByZone.innerHTML = `<span class="empty">Drop a column here.</span>`;
        return;
    }

    els.groupByZone.innerHTML = state.groupBy.map(function (key) {
        return `
            <span class="user-group-chip" draggable="true" data-group-key="${escapeAttribute(key)}">
                <span>${escapeHtml(labelForGroupKey(key))}</span>
                <button type="button" aria-label="Remove group">${escapeHtml("×")}</button>
            </span>
        `;
    }).join("");

    els.groupByZone.querySelectorAll(".user-group-chip").forEach(function (chip) {
        chip.addEventListener("dragstart", function (event) {
            chip.classList.add("is-dragging");
            event.dataTransfer.setData("text/plain", chip.getAttribute("data-group-key"));
        });

        chip.addEventListener("dragend", function () {
            chip.classList.remove("is-dragging");
        });

        chip.addEventListener("dragover", function (event) {
            event.preventDefault();
        });

        chip.addEventListener("drop", function (event) {
            event.preventDefault();
            const incomingKey = event.dataTransfer.getData("text/plain");
            const targetKey = chip.getAttribute("data-group-key");
            reorderGroupBy(incomingKey, targetKey);
        });

        chip.querySelector("button").addEventListener("click", function () {
            removeGroupByKey(chip.getAttribute("data-group-key"));
        });
    });
}

function addGroupByKey(key) {
    if (!key) {
        return;
    }

    if (state.groupBy.includes(key)) {
        return;
    }

    state.groupBy.push(key);
    state.collapsedGroupPaths = [];
    persistGroupBy();
    persistCollapsedGroupPaths();
    renderGroupByZone();
    applyFilterSortAndRender();
}

function removeGroupByKey(key) {
    state.groupBy = state.groupBy.filter(function (item) {
        return item !== key;
    });
    state.collapsedGroupPaths = [];
    persistGroupBy();
    persistCollapsedGroupPaths();
    renderGroupByZone();
    applyFilterSortAndRender();
}

function reorderGroupBy(sourceKey, targetKey) {
    if (!sourceKey || !targetKey || sourceKey === targetKey) {
        return;
    }

    const next = state.groupBy.filter(function (item) {
        return item !== sourceKey;
    });
    const targetIndex = next.indexOf(targetKey);

    if (targetIndex < 0) {
        next.push(sourceKey);
    } else {
        next.splice(targetIndex, 0, sourceKey);
    }

    state.groupBy = next;
    state.collapsedGroupPaths = [];
    persistGroupBy();
    persistCollapsedGroupPaths();
    renderGroupByZone();
    applyFilterSortAndRender();
}

function labelForGroupKey(key) {
    const labels = {
        customerNames: "Customer",
        name: "Name",
        userRole: "Role",
        email: "Email",
        phone: "Phone",
        departmentDescription: "Department",
        departmentName: "Department",
        mfa: "MFA",
        lockedUntil: "Locked",
        lastLoginAt: "Last login",
        active: "Active"
    };

    return labels[key] || key;
}

function groupValueText(row, key) {
    if (!row) {
        return "";
    }

    if (key === "active") {
        return row.active ? "Yes" : "No";
    }

    if (key === "mfa") {
        return row.mfaEnabled ? "On" : "Off";
    }

    if (key === "userRole") {
        return row.userRoleLabel || userRoleLabelFromId(row.userRole) || "Unknown";
    }

    if (key === "lockedUntil") {
        return formatDateTime(row.lockedUntil) || "None";
    }

    return String(row[key] || "").trim();
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

            document.body.classList.add("user-administration-column-resizing");

            function onMouseMove(moveEvent) {
                const nextWidth = Math.max(70, startWidth + moveEvent.clientX - startX);
                state.columnWidths[key] = `${nextWidth}px`;
                applyColumnWidths();
            }

            function onMouseUp() {
                localStorage.setItem(STORAGE_COLUMN_WIDTHS, JSON.stringify(state.columnWidths));
                document.body.classList.remove("user-administration-column-resizing");
                document.removeEventListener("mousemove", onMouseMove);
                document.removeEventListener("mouseup", onMouseUp);
            }

            document.addEventListener("mousemove", onMouseMove);
            document.addEventListener("mouseup", onMouseUp);
        });
    });
}

function applyColumnWidths() {
    if (!els.userColGroup) {
        return;
    }

    Object.keys(DEFAULT_COLUMN_WIDTHS).forEach(function (key) {
        const col = els.userColGroup.querySelector(`col[data-col-key="${key}"]`);
        if (col) {
            col.style.width = state.columnWidths[key] || DEFAULT_COLUMN_WIDTHS[key];
        }
    });
}

function loadColumnWidths() {
    try {
        const json = localStorage.getItem(STORAGE_COLUMN_WIDTHS);
        const parsed = json ? JSON.parse(json) : {};
        return {
            ...DEFAULT_COLUMN_WIDTHS,
            ...(parsed || {})
        };
    } catch {
        return { ...DEFAULT_COLUMN_WIDTHS };
    }
}

function loadGroupBy() {
    try {
        const json = localStorage.getItem(STORAGE_GROUP_BY);
        return Array.isArray(JSON.parse(json)) ? JSON.parse(json) : [];
    } catch {
        return [];
    }
}

function persistGroupBy() {
    localStorage.setItem(STORAGE_GROUP_BY, JSON.stringify(state.groupBy));
}

function loadCollapsedGroupPaths() {
    try {
        const json = localStorage.getItem(STORAGE_GROUP_COLLAPSED);
        return Array.isArray(JSON.parse(json)) ? JSON.parse(json) : [];
    } catch {
        return [];
    }
}

function persistCollapsedGroupPaths() {
    localStorage.setItem(STORAGE_GROUP_COLLAPSED, JSON.stringify(state.collapsedGroupPaths));
}

function isGroupCollapsed(groupPath) {
    return state.collapsedGroupPaths.includes(groupPath);
}

function toggleGroupCollapse(groupPath) {
    if (!groupPath) {
        return;
    }

    if (isGroupCollapsed(groupPath)) {
        state.collapsedGroupPaths = state.collapsedGroupPaths.filter(function (path) {
            return path !== groupPath;
        });
    } else {
        state.collapsedGroupPaths = [...state.collapsedGroupPaths, groupPath];
    }

    persistCollapsedGroupPaths();
    applyFilterSortAndRender();
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

function closeDialog() {
    if (els.userEditDialog) {
        els.userEditDialog.close();
    }
}

function clearDialog() {
    if (els.userEditForm) {
        els.userEditForm.querySelectorAll("input:not([type='checkbox']), textarea, select").forEach(function (field) {
            field.value = "";
        });
        const activeField = document.getElementById("fieldActive");
        if (activeField) {
            activeField.checked = false;
        }
    }

    if (els.fieldPhoneHelp) {
        els.fieldPhoneHelp.textContent = "Select a country code and enter the local phone number.";
    }

}

function setLoadStatus(textValue) {
    if (els.loadStatus) {
        els.loadStatus.textContent = textValue || "";
    }
}

function syncFilterClearButton(filterInput) {
    const clearButton = document.getElementById("btnClearFilter");

    if (!clearButton || !filterInput) {
        return;
    }

    clearButton.hidden = String(filterInput.value || "") === "";
}

function setDialogStatus(textValue, className) {
    if (!els.userDialogStatus) {
        return;
    }

    els.userDialogStatus.classList.remove("is-error", "is-ok", "is-loading");
    els.userDialogStatus.textContent = textValue || "";
    els.userDialogStatus.setAttribute("role", className === "is-error" ? "alert" : "status");
    els.userDialogStatus.setAttribute("aria-live", className === "is-error" ? "assertive" : "polite");

    if (className) {
        els.userDialogStatus.classList.add(className);
    }
}

function setSaveButtonDisabled(disabled) {
    if (els.btnSaveUser) {
        els.btnSaveUser.disabled = Boolean(disabled);
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

function text(node, selector) {
    const element = node ? node.querySelector(selector) : null;
    return element ? element.textContent || "" : "";
}

function intText(node, selector) {
    const parsed = parseInt(text(node, selector), 10);
    return Number.isFinite(parsed) ? parsed : null;
}

function boolText(node, selector) {
    return text(node, selector).toLowerCase() === "true";
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

    if (/[zZ]$/.test(normalized) || /[+-]\d{2}:\d{2}$/.test(normalized)) {
        const zonedDate = new Date(normalized);
        return zonedDate.getTime();
    }

    const match = normalized.match(/^(\d{4})-(\d{2})-(\d{2})(?:[ T](\d{2}):(\d{2})(?::(\d{2})(?:\.\d+)?)?)?$/);

    if (!match) {
        const fallback = new Date(normalized);
        return fallback.getTime();
    }

    const year = Number(match[1]);
    const month = Number(match[2]) - 1;
    const day = Number(match[3]);
    const hour = Number(match[4] || 0);
    const minute = Number(match[5] || 0);
    const second = Number(match[6] || 0);

    return new Date(year, month, day, hour, minute, second).getTime();
}

function pad2(itemValue) {
    return String(itemValue).padStart(2, "0");
}

function formatDateTime(itemValue) {
    const millis = dateTimeMillis(itemValue);
    if (!Number.isFinite(millis)) {
        return "";
    }

    const date = new Date(millis);
    return `${pad2(date.getDate())}/${pad2(date.getMonth() + 1)}/${date.getFullYear()} ${pad2(date.getHours())}:${pad2(date.getMinutes())}:${pad2(date.getSeconds())}`;
}

function formatName(initials, name) {
    if (initials && name) {
        return `${initials} ${name}`;
    }

    return name || initials || "";
}

function yesNo(value) {
    return value ? "Yes" : "No";
}

function userRoleLabelFromId(roleId) {
    switch (Number(roleId)) {
        case 1:
            return "Bepa system administrator";
        case 2:
            return "Customer Administrator";
        case 3:
            return "Project Member";
        case 4:
            return "Project Viewer";
        default:
            return "Invalid User Role";
    }
}

function normalizeRoleClass(label) {
    return String(label || "")
        .trim()
        .toLowerCase()
        .replaceAll("&", "and")
        .replace(/[^a-z0-9]+/g, "-")
        .replace(/^-+|-+$/g, "");
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
