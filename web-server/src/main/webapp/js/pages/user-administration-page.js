import { initMenu } from "../components/menu.js";
import { initHelpDialog } from "../components/help-dialog.js";
import { applyTopPanelFromDocument } from "../core/page-header.js";
import { toDateTimeLocalValue } from "../core/date.js";
import {
    applyPhoneConstraints as applyIntlPhoneConstraints,
    formatCurrentPhoneValue as syncIntlPhoneFieldValue,
    getFullPhoneNumber as getIntlPhoneNumber,
    initPhoneField,
    phonePatternForRule as phonePatternForIntlRule,
    phoneTitleForRule as phoneTitleForIntlRule,
    updatePhoneHelp as updateIntlPhoneHelp,
    validatePhoneNumber as validateIntlPhoneNumber
} from "../components/phone-intl-field.js";

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
    customerId: null,
    lookups: {},
    departments: [],
    projectAccessRows: [],
    saving: false
};

const els = {};

document.addEventListener("DOMContentLoaded", initialize);

function initialize() {
    initMenu();
    initHelpDialog();
    collectElements();
    ensureWorkspaceChrome();
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
    els.workspaceEyebrow = document.getElementById("workspaceEyebrow");
    els.workspaceHeading = document.getElementById("workspaceHeading");
    els.workspaceHelpText = document.getElementById("workspaceHelpText");
    els.userFilter = document.getElementById("userFilter");
    els.btnClearFilter = document.getElementById("btnClearFilter");
    els.groupByZone = document.getElementById("groupByZone");
    els.userColGroup = document.getElementById("userColGroup");
    els.userHeaderRow = document.getElementById("userHeaderRow");
    els.usersBody = document.getElementById("usersBody");
    els.usersEmpty = document.getElementById("usersEmpty");
    els.userTableCount = document.getElementById("userTableCount");

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
    els.userProjectsBody = document.getElementById("userProjectsBody");

    document.querySelectorAll("[id]").forEach(function (element) {
        els[element.id] = element;
    });
}

function ensureWorkspaceChrome() {
    const layout = document.querySelector(".user-administration-layout");

    if (layout) {
        layout.classList.remove("panel");
    }

    const sectionHeader = document.querySelector(".section-header.user-administration-section-header");

    if (sectionHeader && !document.getElementById("workspaceEyebrow")) {
        sectionHeader.className = "user-administration-header-row";
        sectionHeader.innerHTML = `
            <div class="workspace-header user-administration-workspace-header" aria-label="Workspace information">
                <div id="workspaceEyebrow" class="workspace-eyebrow user-administration-workspace-eyebrow"></div>
                <div id="workspaceHeading" class="workspace-heading user-administration-workspace-heading"></div>
                <div id="workspaceHelpText" class="workspace-helptext user-administration-workspace-helptext"></div>
            </div>
        `;
        els.workspaceEyebrow = document.getElementById("workspaceEyebrow");
        els.workspaceHeading = document.getElementById("workspaceHeading");
        els.workspaceHelpText = document.getElementById("workspaceHelpText");
    }

    const frame = document.querySelector(".user-administration-frame");

    if (frame && !document.getElementById("userTableCount")) {
        frame.insertAdjacentHTML(
            "beforeend",
            `
                <div class="data-table-footer user-administration-table-footer" aria-live="polite">
                    <span class="data-table-footer-count" id="userTableCount">0 of 0</span>
                </div>
            `
        );
        els.userTableCount = document.getElementById("userTableCount");
        frame.classList.add("has-table-footer");
    }
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
        syncWorkspaceHeaderVisibility();
        state.customerId = parseCustomerId(doc);
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

async function openUserDetail(userId, customerId = state.customerId) {
    if (!userId) {
        return;
    }

    state.selectedUserId = userId;
    state.customerId = customerId != null ? customerId : state.customerId;
    setDialogStatus("Loading user...", "is-loading");
    clearDialog();

    if (els.userEditDialog && typeof els.userEditDialog.showModal === "function") {
        els.userEditDialog.showModal();
    }

    try {
        const query = [`userId=${encodeURIComponent(userId)}`];

        if (state.customerId != null && state.customerId !== "") {
            query.push(`customerId=${encodeURIComponent(state.customerId)}`);
        }

        const doc = await fetchXml(`${API_URL}?${query.join("&")}`);
        state.currentDoc = doc;
        applyTopPanelFromDocument(doc, els, { userTagNames: ["Name", "UserName"] });
        syncWorkspaceHeaderVisibility();
        state.customerId = parseCustomerId(doc) ?? state.customerId;
        state.lookups = parseLookups(doc);
        fillLookupSelect("fieldUserMfaPolicy", "userMfaPolicy");

        const detail = parseUserDetail(doc);
        state.userDetail = detail.user;
        state.departments = detail.departments;
        state.projectAccessRows = detail.projectAccessRows;
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

    const emailField = document.getElementById("fieldEmail");
    if (emailField instanceof HTMLInputElement) {
        if (!emailField.checkValidity()) {
            emailField.reportValidity();
            setDialogStatus("Email is missing or invalid.", "is-error");
            return;
        }
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
        await openUserDetail(parseInt(userId, 10), state.customerId);
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
            customers: [],
            departments: [],
            projectAccessRows: []
        };
    }

    return {
        user: parseUserNode(detail.querySelector(":scope > user")),
        customers: Array.from(detail.querySelectorAll(":scope > customers > customer")).map(parseCustomerNode),
        departments: Array.from(detail.querySelectorAll(":scope > departments > department")).map(parseDepartmentNode),
        projectAccessRows: Array.from(detail.querySelectorAll(":scope > userProjects > project")).map(parseProjectAccessNode)
    };
}

function parseProjectAccessNode(node) {
    return {
        projectId: intText(node, "ProjectId"),
        projectName: text(node, "ProjectName"),
        selected: boolText(node, "Selected")
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
        themeId: intText(node, "themeId"),
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

function parseCustomerId(doc) {
    const root = doc ? doc.querySelector("userAdministration") : null;
    const valueNode = root
        ? Array.from(root.children || []).find(function (child) {
            return child.tagName === "customerId";
        })
        : null;
    const parsed = parseInt(valueNode ? valueNode.textContent || "" : "", 10);
    return Number.isFinite(parsed) ? parsed : null;
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
    const linkedCountry = getCustomerCountry(detail.customers || [], state.customerId);

    setValue("fieldUserId", user.userId);
    setValue("fieldInitials", user.initials);
    setValue("fieldName", user.name);
    setValue("fieldEmail", user.email);
    fillLookupSelect("fieldUserRole", "userRole", user.userRole == null ? "1" : String(user.userRole));
    fillLookupSelect("fieldTheme", "theme", user.themeId == null ? "" : String(user.themeId));
    setValue("fieldPhone", user.phone);
    initPhoneField(els.fieldPhone, {
        initialCountry: String(linkedCountry || "dk").toLowerCase() || "dk",
        onCountryChange: function () {
            updatePhoneHelp();
        },
        onInput: function () {
            updatePhoneHelp();
        }
    });
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
    renderProjectAccessTab(detail.projectAccessRows || []);
    updatePhoneHelp();

    if (els.userDialogTitle) {
        els.userDialogTitle.textContent = `User Administration - ${user.name || user.email || user.userId || ""}`;
    }
}

function renderProjectAccessTab(projectAccessRows) {
    if (!els.userProjectsBody) {
        return;
    }

    const rows = Array.isArray(projectAccessRows) ? projectAccessRows : [];

    if (!rows.length) {
        els.userProjectsBody.innerHTML = '<div class="page-empty user-projects-empty">No active projects found for this customer.</div>';
        return;
    }

    const tableRows = rows.map(function (row) {
        const projectId = row && row.projectId != null ? String(row.projectId) : "";
        const projectName = row && row.projectName ? row.projectName : `Project ${projectId || "-"}`;
        const checkboxId = `projectAccess-${projectId}`;
        const checked = row && row.selected ? "checked" : "";

        return `
            <tr>
                <td class="user-project-name-cell">
                    <label for="${escapeAttribute(checkboxId)}">${escapeHtml(projectName)}</label>
                </td>
                <td class="user-project-check-cell">
                    <input
                        id="${escapeAttribute(checkboxId)}"
                        class="user-project-access-checkbox"
                        type="checkbox"
                        data-project-id="${escapeAttribute(projectId)}"
                        ${checked}
                    />
                </td>
            </tr>
        `;
    }).join("");

    els.userProjectsBody.innerHTML = `
        <div class="user-projects-table-wrap">
            <table class="user-projects-table">
                <colgroup>
                    <col />
                    <col class="user-projects-check-col" />
                </colgroup>
                <thead>
                    <tr>
                        <th scope="col">Project name</th>
                        <th scope="col" class="user-projects-check-col">Access</th>
                    </tr>
                </thead>
                <tbody>
                    ${tableRows}
                </tbody>
            </table>
        </div>
    `;
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
    const currentValue = selectedValue == null ? "" : String(selectedValue);

    select.innerHTML = options.map(function (lookupOption) {
        const selected = lookupOption.code === currentValue ? " selected" : "";
        return `<option value="${escapeAttribute(lookupOption.code)}"${selected}>${escapeHtml(lookupOption.label || lookupOption.code)}</option>`;
    }).join("");
}

function getCustomerCountry(customers, customerId) {
    const linkedCustomer = (customers || []).find(function (customer) {
        return Number(customer.customerId) === Number(customerId);
    });

    return linkedCustomer ? linkedCustomer.country || "" : "";
}

function fillPhoneCountryCodes(selectedRule) {
    return;
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
    return null;
}

function selectedPhoneRule() {
    return null;
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
    applyIntlPhoneConstraints(els.fieldPhone);
}

function updatePhoneHelp() {
    updateIntlPhoneHelp(els.fieldPhoneHelp, els.fieldPhone);
}

function formatPhoneNumberForDisplay(value, rule) {
    return String(value == null ? "" : value);
}

function formatCurrentPhoneValue() {
    syncIntlPhoneFieldValue(els.fieldPhone);
    updatePhoneHelp();
}

function validatePhoneNumber() {
    return validateIntlPhoneNumber(els.fieldPhone);
}

function isPhoneRequired() {
    const phoneNode = state.currentDoc
        ? state.currentDoc.querySelector("userDetail > user > UserPhone, userDetail > user > Phone, user > UserPhone, user > Phone")
        : null;

    return String(phoneNode?.getAttribute("required") || "").toLowerCase() === "true";
}

function getFullPhoneNumber() {
    return getIntlPhoneNumber(els.fieldPhone);
}

function phonePatternForRule(rule) {
    return phonePatternForIntlRule(rule);
}

function phoneTitleForRule(rule) {
    return phoneTitleForIntlRule(rule);
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
    appendXmlElement(xml, "customerId", state.customerId);
    xml.push("<user>");
    appendXmlElement(xml, "userId", value("fieldUserId"));
    appendXmlElement(xml, "initials", value("fieldInitials"));
    appendXmlElement(xml, "name", value("fieldName"));
    appendXmlElement(xml, "email", value("fieldEmail"));
    appendXmlElement(xml, "userRole", value("fieldUserRole"));
    appendXmlElement(xml, "themeId", value("fieldTheme"));
    appendXmlElement(xml, "phone", phone);
    appendXmlElement(xml, "departmentId", value("fieldDepartmentId"));
    appendXmlElement(xml, "active", document.getElementById("fieldActive") && document.getElementById("fieldActive").checked ? "true" : "false");
    appendXmlElement(xml, "lockedUntil", value("fieldLockedUntil"));
    appendXmlElement(xml, "userMfaPolicy", value("fieldUserMfaPolicy"));
    xml.push("</user>");

    xml.push("<userProjects>");
    serializeProjectAccessRows(xml);
    xml.push("</userProjects>");

    xml.push("</userAdministrationSave>");
    return xml.join("");
}

function serializeProjectAccessRows(xml) {
    const projectFieldsRoot = document.getElementById("userProjectsBody") || document;
    const projectCheckboxes = Array.from(projectFieldsRoot.querySelectorAll("input[type='checkbox'][data-project-id]"));

    projectCheckboxes.forEach(function (checkbox) {
        const projectId = String(checkbox.getAttribute("data-project-id") || "").trim();

        if (!projectId) {
            return;
        }

        const row = (state.projectAccessRows || []).find(function (item) {
            return String(item.projectId) === projectId;
        });

        xml.push("<project>");
        appendXmlElement(xml, "ProjectId", projectId);
        appendXmlElement(xml, "ProjectName", row && row.projectName ? row.projectName : "");
        appendXmlElement(xml, "Selected", checkbox.checked ? "true" : "false");
        xml.push("</project>");
    });
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
        updateUserTableCount();
        return;
    }

    if (els.usersEmpty) {
        els.usersEmpty.hidden = true;
    }

    const rowsHtml = state.groupBy.length
        ? renderGroupedRows(state.filteredUsers, state.groupBy, [])
        : state.filteredUsers.map(renderUserRow).join("");

    els.usersBody.innerHTML = rowsHtml;
    updateUserTableCount();

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
            openUserDetail(userId, state.customerId);
        });
    });
}

function updateUserTableCount() {
    if (!els.userTableCount) {
        return;
    }

    els.userTableCount.textContent = `${state.filteredUsers.length} of ${state.users.length}`;
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

    const clearButtonMarkup = `
        <button
                id="btnClearGrouping"
                class="user-grouping-clear"
                type="button"
                aria-label="Clear grouping"
                title="Clear grouping"
                ${state.groupBy.length ? "" : "hidden"}
        >
            <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                <path d="M18.3 5.71 12 12l6.3 6.29-1.41 1.42L10.59 13.4 4.29 19.71 2.88 18.3 9.17 12 2.88 5.71 4.29 4.29l6.3 6.3 6.29-6.3z"></path>
            </svg>
        </button>
    `;

    if (!state.groupBy.length) {
        els.groupByZone.innerHTML = `<span class="empty">Drop a column here.</span>${clearButtonMarkup}`;
    } else {
        els.groupByZone.innerHTML = `${state.groupBy.map(function (key) {
            return `
                <span class="user-group-chip" draggable="true" data-group-key="${escapeAttribute(key)}">
                    <span>${escapeHtml(labelForGroupKey(key))}</span>
                    <button type="button" aria-label="Remove group">x</button>
                </span>
            `;
        }).join("")}${clearButtonMarkup}`;
    }

    const clearButton = document.getElementById("btnClearGrouping");

    clearButton?.addEventListener("click", function () {
        state.groupBy = [];
        state.collapsedGroupPaths = [];
        persistGroupBy();
        persistCollapsedGroupPaths();
        renderGroupByZone();
        applyFilterSortAndRender();
    });

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

    if (els.userProjectsBody) {
        els.userProjectsBody.innerHTML = "";
    }

    state.projectAccessRows = [];

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

function syncWorkspaceHeaderVisibility() {
    const workspaceRow = document.querySelector(".user-administration-header-row");

    if (!workspaceRow) {
        return;
    }

    const hasWorkspaceText = [els.workspaceEyebrow, els.workspaceHeading, els.workspaceHelpText].some(function (element) {
        return String(element?.textContent || "").trim() !== "";
    });

    workspaceRow.hidden = !hasWorkspaceText;
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

