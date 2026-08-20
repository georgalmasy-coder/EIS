import { initMenu } from "../components/menu.js";
import { mountTopbar } from "../components/topbar.js";
import { clear, closeDialogElement, setText, showDialog } from "../core/dom.js";
import { applyTopPanel as applyPageHeader } from "../core/page-header.js";
import { fetchXml, postXml } from "../core/http.js";
import { escapeHtml } from "../core/html.js";
import { escapeXml, getAttribute, getChildText, getDirectChild, getDirectChildren, hasXmlParseError } from "../core/xml.js";

const API_URL = "/Menu";
const STORAGE_EXPANDED_KEY = "menuEditor.expandedParentIds";

const state = {
    currentDoc: null,
    menuItems: [],
    userRoles: [],
    menuItemTypes: [],
    subscriptions: [],
    menuIcons: [],
    selectedMenuId: null,
    expandedParentIds: loadExpandedParentIds(),
    currentItem: null,
    dialogMode: "edit",
    dialogParentMenuId: null,
    dirty: false,
    saving: false,
    contextTarget: null,
    loading: false
};

const els = {};

document.addEventListener("DOMContentLoaded", start);

function start() {
    initializeShell();
    collectElements();
    bindEvents();
    loadMenuData();
}

function initializeShell() {
    setText("customerName", "-", "");
    setText("projectName", "-", "");
    setText("userName", "-", "");
    setText("loadStatus", "Loading", "");

    initMenu();
    mountTopbar(document);
}

function collectElements() {
    document.querySelectorAll("[id]").forEach((element) => {
        els[element.id] = element;
    });
}

function bindEvents() {
    els.btnRefreshMenu?.addEventListener("click", () => loadMenuData(true));
    els.btnAddParentMenuItem?.addEventListener("click", () => openMenuDialog("create", null, null));
    els.btnSaveMenu?.addEventListener("click", saveMenuItem);
    els.btnCancelMenu?.addEventListener("click", () => closeMenuDialog("cancel"));

    els.menuBody?.addEventListener("click", onTreeClick);
    els.menuBody?.addEventListener("dblclick", onTreeDoubleClick);
    els.menuBody?.addEventListener("contextmenu", onTreeContextMenu);
    els.menuBody?.addEventListener("keydown", onTreeKeyDown);

    els.treeScroll?.addEventListener("contextmenu", onTreeFrameContextMenu);

    els.menuDialog?.addEventListener("cancel", (event) => {
        event.preventDefault();
        closeMenuDialog("cancel");
    });

    els.menuDialog?.addEventListener("close", () => {
        state.dirty = false;
        state.currentItem = null;
        state.dialogMode = "edit";
        state.dialogParentMenuId = null;
    });

    ["fieldMenuItemText", "fieldMenuItemUrl", "fieldDescription", "fieldMenuItemType", "fieldSubscriptionCode", "fieldIconId", "fieldCustomerIdRequired", "fieldProjectIdRequired", "fieldActive"].forEach((id) => {
        const element = byId(id);
        element?.addEventListener("input", markDirty);
        element?.addEventListener("change", markDirty);
    });

    byId("fieldIconId")?.addEventListener("change", updateMenuIconPreview);

    els.menuDialog?.addEventListener("input", markDirty);
    els.menuDialog?.addEventListener("change", markDirty);

    document.addEventListener("click", (event) => {
        if (!els.menuEditorContextMenu?.hidden && !els.menuEditorContextMenu.contains(event.target)) {
            closeContextMenu();
        }
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            closeContextMenu();
        }
    });

    window.addEventListener("resize", closeContextMenu);
    document.addEventListener("scroll", closeContextMenu, true);
}

async function loadMenuData(showSpinner = false) {
    if (state.loading) {
        return;
    }

    state.loading = true;

    if (showSpinner) {
        setText("loadStatus", "Loading", "");
    } else {
        showEmptyState("Loading menu items...");
    }

    try {
        const url = new URL(API_URL, window.location.origin);
        url.searchParams.set("cmd", "list");

        const xmlDocument = await fetchXml(url.toString(), {
            cache: "no-store",
            credentials: "same-origin"
        });

        if (hasXmlParseError(xmlDocument)) {
            throw new Error("Menu endpoint returned invalid XML.");
        }

        state.currentDoc = xmlDocument;
        state.userRoles = parseUserRoles(xmlDocument);
        state.menuItemTypes = parseMenuItemTypes(xmlDocument);
        state.subscriptions = parseSubscriptions(xmlDocument);
        state.menuIcons = parseMenuIcons(xmlDocument);
        state.menuItems = parseMenuItems(xmlDocument);
        state.selectedMenuId = resolveSelectedMenuId();

        applyTopPanel();
        populateMenuDialogLookups();
        renderMenuTree();
        setText("loadStatus", "Loaded", "");
    } catch (error) {
        console.error("Failed to load menu editor data", error);
        state.currentDoc = null;
        state.menuItems = [];
        state.userRoles = [];
        state.selectedMenuId = null;
        renderMenuTree();
        setText("loadStatus", "Error", "");
        showEmptyState(`Could not load menu items. ${error.message || "Unknown error."}`);
    } finally {
        state.loading = false;
    }
}

function applyTopPanel() {
    applyPageHeader(state.currentDoc, {
        customerName: "customerName",
        projectName: "projectName",
        userName: "userName",
        workspaceEyebrow: "pageEyebrow",
        workspaceHeading: "pageHeading",
        workspaceHelpText: "pageHelpText"
    });
}

function parseUserRoles(xmlDocument) {
    const lookupsElement = getDirectChild(xmlDocument.documentElement, "lookups");
    const lookupNodes = getDirectChildren(lookupsElement, "lookup");
    const lookupNode = lookupNodes.find((node) => (node.getAttribute("name") || "").toLowerCase() === "userroles");

    if (!lookupNode) {
        return [];
    }

    return getDirectChildren(lookupNode, "option").map((optionNode) => ({
        value: getAttribute(optionNode, "code") || getAttribute(optionNode, "value"),
        label: getAttribute(optionNode, "label") || getAttribute(optionNode, "code")
    })).filter((option) => option.value);
}

function parseMenuItemTypes(xmlDocument) {
    return parseNamedLookupOptions(xmlDocument, "menuItemTypes");
}

function parseSubscriptions(xmlDocument) {
    return parseNamedLookupOptions(xmlDocument, "subscriptions");
}

function parseMenuIcons(xmlDocument) {
    const lookupsElement = getDirectChild(xmlDocument.documentElement, "lookups");
    const menuIconsElement = getDirectChild(lookupsElement, "menuIcons");

    if (!menuIconsElement) {
        return [];
    }

    return getDirectChildren(menuIconsElement, "menuIcon").map((iconNode) => ({
        value: getChildText(iconNode, "Id", ""),
        label: getChildText(iconNode, "Name", ""),
        svg: getChildText(iconNode, "SvgCode", "")
    })).filter((icon) => icon.value);
}

function parseNamedLookupOptions(xmlDocument, lookupName) {
    const lookupsElement = getDirectChild(xmlDocument.documentElement, "lookups");
    const lookupNodes = getDirectChildren(lookupsElement, "lookup");
    const lookupNode = lookupNodes.find((node) => (node.getAttribute("name") || "").toLowerCase() === String(lookupName || "").toLowerCase());

    if (!lookupNode) {
        return [];
    }

    return getDirectChildren(lookupNode, "option").map((optionNode) => ({
        value: getAttribute(optionNode, "code") || getAttribute(optionNode, "value"),
        label: getAttribute(optionNode, "label") || getAttribute(optionNode, "code")
    })).filter((option) => option.value);
}

function parseMenuItems(xmlDocument) {
    const menuItemsElement = getDirectChild(xmlDocument.documentElement, "menuItems");
    const menuNodes = menuItemsElement
        ? getDirectChildren(menuItemsElement, "menuItem")
        : Array.from(xmlDocument.getElementsByTagName("menuItem"));

    return menuNodes.map((node) => ({
        menuId: parseNullableInt(getChildText(node, "MenuId", "")),
        menuItemText: getChildText(node, "MenuItemText", ""),
        menuItemUrl: getChildText(node, "MenuItemUrl", ""),
        description: getChildText(node, "Description", ""),
        parentMenuId: parseNullableInt(getChildText(node, "ParentMenuId", "")),
        menuItemType: getChildText(node, "MenuItemType", ""),
        subscriptionCode: getChildText(node, "SubscriptionCode", ""),
        iconId: parseNullableInt(getChildText(node, "IconId", "")),
        displayOrder: parseNullableInt(getChildText(node, "DisplayOrder", "")),
        customerIdRequired: parseBoolean(getChildText(node, "CustomerIdRequired", "false")),
        projectIdRequired: parseBoolean(getChildText(node, "ProjectIdRequired", "false")),
        userRoles: getChildText(node, "UserRoles", ""),
        active: parseBoolean(getChildText(node, "Active", "true"))
    })).filter((row) => row.menuId !== null);
}

function renderMenuTree() {
    const tbody = els.menuBody;
    const totalCount = els.menuTableCount;

    if (!tbody) {
        return;
    }

    const parents = getParentRows();
    const renderedCount = countRenderedRows(parents);

    if (totalCount) {
        totalCount.textContent = `${renderedCount} of ${state.menuItems.length}`;
    }

    if (!parents.length) {
        tbody.innerHTML = "";
        showEmptyState("No menu items found.");
        return;
    }

    hideEmptyState();
    tbody.innerHTML = parents.map((parentRow) => renderMenuRow(parentRow, false)).join("");
    applySelectionState();
}

function countRenderedRows(rows = []) {
    return rows.reduce((count, row) => {
        if (!row) {
            return count;
        }

        let nextCount = count + 1;

        if (isExpanded(row.menuId)) {
            nextCount += countRenderedRows(getChildRows(row.menuId));
        }

        return nextCount;
    }, 0);
}

function getParentRows() {
    return [...state.menuItems]
        .filter((row) => row.parentMenuId === null)
        .sort(compareDisplayOrder);
}

function getChildRows(parentMenuId) {
    return [...state.menuItems]
        .filter((row) => row.parentMenuId === parentMenuId)
        .sort(compareDisplayOrder);
}

function renderMenuRow(menuItem, isChild) {
    const rowClassNames = [
        "menu-editor-row",
        isChild ? "is-child" : "is-parent",
        menuItem.active ? "" : "is-inactive",
        state.selectedMenuId === menuItem.menuId ? "is-selected" : ""
    ].filter(Boolean).join(" ");

    const childRows = !isChild && isExpanded(menuItem.menuId)
        ? getChildRows(menuItem.menuId).map((childRow) => renderMenuRow(childRow, true)).join("")
        : "";

    return `
        <tr
                class="${rowClassNames}"
                data-menu-id="${escapeHtml(String(menuItem.menuId))}"
                data-parent-menu-id="${escapeHtml(menuItem.parentMenuId === null ? "" : String(menuItem.parentMenuId))}"
                tabindex="0"
        >
            <td title="${escapeHtml(menuItem.menuItemText || "")}">
                <div class="menu-editor-name-cell ${isChild ? "is-child" : ""}">
                    ${renderExpandButton(menuItem, isChild)}
                    ${isChild ? '<span class="menu-editor-indent" aria-hidden="true"></span>' : ""}
                    ${renderMenuRowIcon(menuItem)}
                    <span class="menu-editor-item-label">${escapeHtml(menuItem.menuItemText || "")}</span>
                </div>
            </td>
            <td class="menu-editor-url-cell" title="${escapeHtml(menuItem.menuItemUrl || "")}">${escapeHtml(menuItem.menuItemUrl || "")}</td>
            <td class="menu-editor-role-cell" title="${escapeHtml(renderRoleSummary(menuItem.userRoles))}">${escapeHtml(renderRoleSummary(menuItem.userRoles))}</td>
            <td class="menu-editor-subscription-cell" title="${escapeHtml(renderSubscriptionLabel(menuItem.subscriptionCode))}">${renderSubscriptionPill(menuItem.subscriptionCode)}</td>
            <td>${renderPill(menuItem.customerIdRequired ? "Yes" : "No", menuItem.customerIdRequired ? "is-yes" : "is-no")}</td>
            <td>${renderPill(menuItem.projectIdRequired ? "Yes" : "No", menuItem.projectIdRequired ? "is-yes" : "is-no")}</td>
            <td class="menu-editor-active-cell">${renderActiveIcon(menuItem.active)}</td>
            <td title="${escapeHtml(displayOrderText(menuItem.displayOrder))}">${escapeHtml(displayOrderText(menuItem.displayOrder))}</td>
        </tr>
        ${childRows}
    `;
}

function renderExpandButton(menuItem, isChild) {
    if (isChild) {
        return "";
    }

    const hasChildren = getChildRows(menuItem.menuId).length > 0;

    if (!hasChildren) {
        return '<span class="menu-editor-indent" aria-hidden="true"></span>';
    }

    const expanded = isExpanded(menuItem.menuId);
    const label = expanded ? "Collapse" : "Expand";
    const glyph = expanded ? "&#9662;" : "&#9656;";

    return `
        <button
                type="button"
                class="menu-editor-toggle-button"
                data-action="toggle-parent"
                data-menu-id="${escapeHtml(String(menuItem.menuId))}"
                aria-label="${escapeHtml(label)}"
                title="${escapeHtml(label)}"
                aria-expanded="${expanded ? "true" : "false"}"
        >${glyph}</button>
    `;
}

function renderMenuRowIcon(menuItem) {
    const icon = findMenuIcon(menuItem.iconId);

    if (!icon) {
        return "";
    }

    return `<span class="menu-editor-row-icon" title="${escapeHtml(icon.label || "")}">${icon.svg || ""}</span>`;
}

function renderRoleSummary(roleList) {
    const roleIds = normalizeRoleList(roleList);

    if (!roleIds.length) {
        return "";
    }

    return roleIds
        .map((roleId) => state.userRoles.find((role) => role.value === roleId)?.label || roleId)
        .join(", ");
}

function renderPill(textValue, className) {
    return `<span class="menu-editor-pill ${escapeHtml(className)}">${escapeHtml(textValue)}</span>`;
}

function renderActiveIcon(value) {
    return value
        ? '<span class="menu-editor-active-state" title="Active" aria-label="Active"><span class="menu-editor-active-dot is-active" aria-hidden="true"></span></span>'
        : '<span class="menu-editor-active-state" title="Inactive" aria-label="Inactive"><span class="menu-editor-active-dot is-inactive" aria-hidden="true"></span></span>';
}

function renderSubscriptionPill(subscriptionCode) {
    const label = renderSubscriptionLabel(subscriptionCode);

    if (!label) {
        return "";
    }

    return renderPill(label, "is-subscription");
}

function renderSubscriptionLabel(subscriptionCode) {
    const code = String(subscriptionCode || "").trim();

    if (!code) {
        return "";
    }

    return state.subscriptions.find((entry) => entry.value === code)?.label || code;
}

function onTreeClick(event) {
    const toggleButton = event.target.closest("[data-action='toggle-parent']");

    if (toggleButton) {
        event.preventDefault();
        event.stopPropagation();
        const menuId = parseNullableInt(toggleButton.getAttribute("data-menu-id"));
        if (menuId !== null) {
            toggleParent(menuId);
        }
        return;
    }

    const row = event.target.closest("tr[data-menu-id]");
    if (!row) {
        return;
    }

    const menuId = parseNullableInt(row.getAttribute("data-menu-id"));
    if (menuId !== null) {
        selectRow(menuId);
    }
}

function onTreeDoubleClick(event) {
    const row = event.target.closest("tr[data-menu-id]");
    if (!row) {
        return;
    }

    const menuId = parseNullableInt(row.getAttribute("data-menu-id"));
    if (menuId !== null) {
        openMenuDialog("edit", menuId, null);
    }
}

function onTreeKeyDown(event) {
    const row = event.target.closest("tr[data-menu-id]");
    if (!row) {
        return;
    }

    const menuId = parseNullableInt(row.getAttribute("data-menu-id"));
    if (menuId === null) {
        return;
    }

    if (event.key === "Enter") {
        event.preventDefault();
        openMenuDialog("edit", menuId, null);
        return;
    }

    if (event.key === " ") {
        event.preventDefault();
        selectRow(menuId);
    }
}

function onTreeContextMenu(event) {
    const row = event.target.closest("tr[data-menu-id]");
    if (!row) {
        return;
    }

    event.preventDefault();

    const menuId = parseNullableInt(row.getAttribute("data-menu-id"));
    const parentMenuId = parseNullableInt(row.getAttribute("data-parent-menu-id"));
    const menuItem = findMenuItem(menuId);

    if (menuItem) {
        openContextMenu(event.clientX, event.clientY, buildContextActions(menuItem, parentMenuId));
    }
}

function onTreeFrameContextMenu(event) {
    if (event.target.closest("tr[data-menu-id]")) {
        return;
    }

    event.preventDefault();
    openContextMenu(event.clientX, event.clientY, buildRootContextActions());
}

function buildContextActions(menuItem, parentMenuId) {
    const isParent = parentMenuId === null;
    const actions = [];

    if (canMoveUp(menuItem)) {
        actions.push({
            label: "Move up",
            action: () => moveMenuItem(menuItem.menuId, "up")
        });
    }

    if (canMoveDown(menuItem)) {
        actions.push({
            label: "Move down",
            action: () => moveMenuItem(menuItem.menuId, "down")
        });
    }

    if (actions.length) {
        actions.push({ separator: true });
    }

    if (isParent) {
        actions.push({
            label: "Add child",
            action: () => openMenuDialog("create", null, menuItem.menuId)
        });
    }

    actions.push({
        label: "Edit",
        action: () => openMenuDialog("edit", menuItem.menuId, null)
    });

    return actions;
}

function buildRootContextActions() {
    return [{
        label: "Add parent",
        action: () => openMenuDialog("create", null, null)
    }];
}

function openContextMenu(x, y, actions) {
    const menu = els.menuEditorContextMenu;

    if (!menu) {
        return;
    }

    const html = actions.map((entry) => {
        if (entry.separator) {
            return '<div class="menu-editor-context-separator" role="separator"></div>';
        }

        return `<button type="button" data-action="${escapeHtml(entry.label)}">${escapeHtml(entry.label)}</button>`;
    }).join("");

    menu.innerHTML = html;
    menu.hidden = false;
    menu.setAttribute("aria-hidden", "false");
    menu.style.left = "0px";
    menu.style.top = "0px";

    const menuRect = menu.getBoundingClientRect();
    const nextLeft = Math.min(x, window.innerWidth - menuRect.width - 8);
    const nextTop = Math.min(y, window.innerHeight - menuRect.height - 8);

    menu.style.left = `${Math.max(8, nextLeft)}px`;
    menu.style.top = `${Math.max(8, nextTop)}px`;

    menu.querySelectorAll("button[data-action]").forEach((button) => {
        const actionLabel = button.getAttribute("data-action");
        const action = actions.find((entry) => entry.label === actionLabel)?.action;

        button.addEventListener("click", () => {
            closeContextMenu();
            if (action) {
                action();
            }
        });
    });
}

function closeContextMenu() {
    const menu = els.menuEditorContextMenu;

    if (!menu) {
        return;
    }

    menu.hidden = true;
    menu.setAttribute("aria-hidden", "true");
    menu.innerHTML = "";
}

function toggleParent(menuId) {
    if (state.expandedParentIds.includes(menuId)) {
        state.expandedParentIds = state.expandedParentIds.filter((value) => value !== menuId);
    } else {
        state.expandedParentIds = [...state.expandedParentIds, menuId];
    }

    persistExpandedParentIds();
    renderMenuTree();
}

function isExpanded(menuId) {
    return state.expandedParentIds.includes(menuId);
}

function selectRow(menuId) {
    state.selectedMenuId = menuId;
    applySelectionState();
}

function applySelectionState() {
    els.menuBody?.querySelectorAll("tr[data-menu-id]").forEach((row) => {
        const rowId = parseNullableInt(row.getAttribute("data-menu-id"));
        const isSelected = rowId !== null && rowId === state.selectedMenuId;
        row.classList.toggle("is-selected", isSelected);
        row.setAttribute("aria-selected", isSelected ? "true" : "false");
    });
}

function openMenuDialog(mode, menuId, parentMenuId) {
    if (els.menuDialog?.open && state.dirty && !window.confirm("There are unsaved changes. Continue anyway?")) {
        return;
    }

    state.dialogMode = mode;
    state.dialogParentMenuId = parentMenuId;
    state.currentItem = null;
    state.dirty = false;

    const url = new URL(API_URL, window.location.origin);
    url.searchParams.set("cmd", mode === "edit" ? "edit" : "create");

    if (mode === "edit") {
        url.searchParams.set("id", String(menuId || ""));
    } else if (parentMenuId !== null && parentMenuId !== undefined) {
        url.searchParams.set("parentId", String(parentMenuId));
    }

    showDialog(els.menuDialog);
    setDialogStatus("Loading...", "is-loading");
    setMenuDialogModeLabel(mode, parentMenuId, menuId);
    clearMenuDialogFields();

    fetchXml(url.toString(), {
        cache: "no-store",
        credentials: "same-origin"
    }).then((xmlDocument) => {
        if (hasXmlParseError(xmlDocument)) {
            throw new Error("Menu detail endpoint returned invalid XML.");
        }

        const detailNode = getDirectChild(xmlDocument.documentElement, "menuItemDetail");
        const menuItem = detailNode ? parseMenuItem(detailNode) : buildEmptyMenuItem(parentMenuId);
        state.currentItem = menuItem;
        populateMenuDialogLookups();
        fillMenuDialog(menuItem, mode);
        setDialogStatus("Loaded.", "is-ok");
    }).catch((error) => {
        console.error("Failed to open menu dialog", error);
        setDialogStatus("Could not load menu item.", "is-error");
        closeMenuDialog("error");
        window.alert(error.message || "Could not load menu item.");
    });
}

function parseMenuItem(node) {
    return {
        menuId: parseNullableInt(getChildText(node, "MenuId", "")),
        menuItemText: getChildText(node, "MenuItemText", ""),
        menuItemUrl: getChildText(node, "MenuItemUrl", ""),
        description: getChildText(node, "Description", ""),
        parentMenuId: parseNullableInt(getChildText(node, "ParentMenuId", "")),
        menuItemType: getChildText(node, "MenuItemType", ""),
        subscriptionCode: getChildText(node, "SubscriptionCode", ""),
        iconId: parseNullableInt(getChildText(node, "IconId", "")),
        displayOrder: parseNullableInt(getChildText(node, "DisplayOrder", "")),
        customerIdRequired: parseBoolean(getChildText(node, "CustomerIdRequired", "false")),
        projectIdRequired: parseBoolean(getChildText(node, "ProjectIdRequired", "false")),
        userRoles: getChildText(node, "UserRoles", ""),
        active: parseBoolean(getChildText(node, "Active", "true"))
    };
}

function buildEmptyMenuItem(parentMenuId) {
    return {
        menuId: null,
        menuItemText: "",
        menuItemUrl: "",
        description: "",
        parentMenuId: parentMenuId ?? null,
        menuItemType: "",
        subscriptionCode: "",
        iconId: null,
        displayOrder: null,
        customerIdRequired: false,
        projectIdRequired: false,
        userRoles: "",
        active: true
    };
}

function fillMenuDialog(menuItem, mode) {
    setValue("fieldMenuId", menuItem.menuId);
    setValue("fieldParentMenuId", menuItem.parentMenuId);
    setValue("fieldMenuItemType", menuItem.menuItemType);
    setValue("fieldSubscriptionCode", menuItem.subscriptionCode);
    setValue("fieldIconId", menuItem.iconId);
    setValue("fieldMenuItemText", menuItem.menuItemText);
    setValue("fieldMenuItemUrl", menuItem.menuItemUrl);
    setValue("fieldDescription", menuItem.description);
    setChecked("fieldCustomerIdRequired", menuItem.customerIdRequired);
    setChecked("fieldProjectIdRequired", menuItem.projectIdRequired);
    setChecked("fieldActive", menuItem.active);
    renderUserRoleChecklist(menuItem.userRoles);
    setMenuDialogModeLabel(mode, menuItem.parentMenuId, menuItem.menuId);
    setDialogStatus(mode === "create" ? "Create menu item." : "Edit menu item.", "");
    updateMenuIconPreview();
    state.dirty = false;
}

function renderUserRoleChecklist(selectedRoles) {
    const container = els.userRolesGrid;

    if (!container) {
        return;
    }

    const selected = new Set(normalizeRoleList(selectedRoles));
    container.innerHTML = state.userRoles.map((role) => {
        const checkboxId = `role-${role.value}`;
        const checked = selected.has(role.value) ? " checked" : "";
        return `
            <div class="menu-editor-role-option">
                <input type="checkbox" id="${escapeHtml(checkboxId)}" value="${escapeHtml(role.value)}"${checked} />
                <label for="${escapeHtml(checkboxId)}">${escapeHtml(role.label)}</label>
            </div>
        `;
    }).join("");

    container.querySelectorAll("input[type='checkbox']").forEach((checkbox) => {
        checkbox.addEventListener("change", markDirty);
    });
}

function clearMenuDialogFields() {
    setValue("fieldMenuId", "");
    setValue("fieldParentMenuId", "");
    setValue("fieldMenuItemType", "");
    setValue("fieldSubscriptionCode", "");
    setValue("fieldIconId", "");
    setValue("fieldMenuItemText", "");
    setValue("fieldMenuItemUrl", "");
    setValue("fieldDescription", "");
    setChecked("fieldCustomerIdRequired", false);
    setChecked("fieldProjectIdRequired", false);
    setChecked("fieldActive", true);

    if (els.userRolesGrid) {
        clear(els.userRolesGrid);
    }

    updateMenuIconPreview();
}

function closeMenuDialog(reason) {
    if (reason !== "saved" && state.dirty && !window.confirm("There are unsaved changes. Close the dialog?")) {
        return;
    }

    closeDialogElement(els.menuDialog);
    state.dirty = false;
}

async function saveMenuItem() {
    if (state.saving) {
        return;
    }

    const menuItemText = getValue("fieldMenuItemText").trim();
    const menuItemUrl = getValue("fieldMenuItemUrl").trim();
    const description = getValue("fieldDescription").trim();
    const menuItem = state.currentItem || buildEmptyMenuItem(parseNullableInt(getValue("fieldParentMenuId")));
    const selectedRoles = Array.from(els.userRolesGrid?.querySelectorAll("input[type='checkbox']:checked") || [])
        .map((checkbox) => String(checkbox.value || "").trim())
        .filter(Boolean);
    const menuItemType = getValue("fieldMenuItemType").trim();
    const subscriptionCode = getValue("fieldSubscriptionCode").trim();
    const iconId = parseNullableInt(getValue("fieldIconId"));

    if (!menuItemText) {
        window.alert("MenuItemText is required.");
        return;
    }

    const payload = `
        <menuSave>
            <menuItem>
                <MenuId>${escapeXml(getValue("fieldMenuId"))}</MenuId>
                <ParentMenuId>${escapeXml(getValue("fieldParentMenuId"))}</ParentMenuId>
                <MenuItemType>${escapeXml(menuItemType)}</MenuItemType>
                <SubscriptionCode>${escapeXml(subscriptionCode)}</SubscriptionCode>
                <IconId>${escapeXml(iconId === null ? "" : String(iconId))}</IconId>
                <MenuItemText>${escapeXml(menuItemText)}</MenuItemText>
                <MenuItemUrl>${escapeXml(menuItemUrl)}</MenuItemUrl>
                <Description>${escapeXml(description)}</Description>
                <CustomerIdRequired>${escapeXml(getChecked("fieldCustomerIdRequired") ? "true" : "false")}</CustomerIdRequired>
                <ProjectIdRequired>${escapeXml(getChecked("fieldProjectIdRequired") ? "true" : "false")}</ProjectIdRequired>
                <UserRoles>${escapeXml(selectedRoles.join(","))}</UserRoles>
                <Active>${escapeXml(getChecked("fieldActive") ? "true" : "false")}</Active>
            </menuItem>
        </menuSave>
    `.trim();

    state.saving = true;
    setDialogStatus("Saving...", "is-loading");

    try {
        const response = await postXml(`${API_URL}?cmd=save`, payload, {
            credentials: "same-origin"
        });

        if (!response) {
            throw new Error("Empty response from menu save.");
        }

        state.dirty = false;
        closeMenuDialog("saved");
        await loadMenuData(true);
    } catch (error) {
        console.error("Failed to save menu item", error);
        setDialogStatus(error.message || "Menu item could not be saved.", "is-error");
        window.alert(error.message || "Menu item could not be saved.");
    } finally {
        state.saving = false;
    }
}

async function moveMenuItem(menuId, direction) {
    if (!menuId || (direction !== "up" && direction !== "down")) {
        return;
    }

    try {
        setText("loadStatus", "Saving", "");
        await postXml(`${API_URL}?cmd=${direction === "up" ? "moveUp" : "moveDown"}&id=${encodeURIComponent(String(menuId))}`, "", {
            credentials: "same-origin"
        });
        await loadMenuData(true);
    } catch (error) {
        console.error("Failed to move menu item", error);
        setText("loadStatus", "Error", "");
        window.alert(error.message || "Menu item could not be moved.");
    }
}

function canMoveUp(menuItem) {
    const siblings = getSiblingRows(menuItem.parentMenuId);
    return siblings.length > 0 && siblings[0].menuId !== menuItem.menuId;
}

function canMoveDown(menuItem) {
    const siblings = getSiblingRows(menuItem.parentMenuId);
    return siblings.length > 0 && siblings[siblings.length - 1].menuId !== menuItem.menuId;
}

function getSiblingRows(parentMenuId) {
    return [...state.menuItems]
        .filter((row) => row.parentMenuId === parentMenuId)
        .sort(compareDisplayOrder);
}

function compareDisplayOrder(left, right) {
    const leftOrder = left.displayOrder === null || left.displayOrder === undefined ? Number.MAX_SAFE_INTEGER : Number(left.displayOrder);
    const rightOrder = right.displayOrder === null || right.displayOrder === undefined ? Number.MAX_SAFE_INTEGER : Number(right.displayOrder);

    if (leftOrder !== rightOrder) {
        return leftOrder - rightOrder;
    }

    return Number(left.menuId || 0) - Number(right.menuId || 0);
}

function resolveSelectedMenuId() {
    const firstParent = getParentRows()[0];
    return firstParent ? firstParent.menuId : null;
}

function loadExpandedParentIds() {
    try {
        const raw = localStorage.getItem(STORAGE_EXPANDED_KEY);
        const parsed = raw ? JSON.parse(raw) : [];
        return Array.isArray(parsed) ? parsed.filter((value) => Number.isFinite(Number(value))).map((value) => Number(value)) : [];
    } catch {
        return [];
    }
}

function persistExpandedParentIds() {
    try {
        localStorage.setItem(STORAGE_EXPANDED_KEY, JSON.stringify(state.expandedParentIds));
    } catch {
        // Ignore storage failures.
    }
}

function normalizeRoleList(roleList) {
    if (!roleList) {
        return [];
    }

    return String(roleList)
        .split(",")
        .map((value) => value.trim())
        .filter(Boolean)
        .filter((value) => value !== "-1");
}

function setMenuDialogModeLabel(mode, parentMenuId, menuId) {
    const label = mode === "create"
        ? (parentMenuId === null || parentMenuId === undefined ? "Create parent item" : `Create child under ${parentMenuId}`)
        : `Edit item ${menuId || ""}`;

    setText("menuDialogMode", label, "");
}

function getValue(id) {
    return byId(id)?.value ?? "";
}

function setValue(id, value) {
    const element = byId(id);
    if (element) {
        element.value = value === null || value === undefined ? "" : String(value);
    }
}

function setChecked(id, value) {
    const element = byId(id);
    if (element) {
        element.checked = Boolean(value);
    }
}

function getChecked(id) {
    return Boolean(byId(id)?.checked);
}

function byId(id) {
    return document.getElementById(id);
}

function parseNullableInt(value) {
    const normalized = String(value || "").trim();

    if (!normalized) {
        return null;
    }

    const parsed = Number.parseInt(normalized, 10);
    return Number.isFinite(parsed) ? parsed : null;
}

function parseBoolean(value) {
    const normalized = String(value || "").trim().toLowerCase();
    return ["true", "1", "yes", "ja", "y", "on"].includes(normalized);
}

function displayOrderText(value) {
    return value === null || value === undefined ? "" : String(value);
}

function findMenuItem(menuId) {
    return state.menuItems.find((row) => row.menuId === menuId) || null;
}

function markDirty() {
    state.dirty = true;
}

function setDialogStatus(textValue, className) {
    if (!els.menuDialogStatus) {
        return;
    }

    els.menuDialogStatus.classList.remove("is-error", "is-ok", "is-loading");
    els.menuDialogStatus.textContent = textValue || "";
    els.menuDialogStatus.setAttribute("role", className === "is-error" ? "alert" : "status");
    els.menuDialogStatus.setAttribute("aria-live", className === "is-error" ? "assertive" : "polite");

    if (className) {
        els.menuDialogStatus.classList.add(className);
    }
}

function populateMenuDialogLookups() {
    populateParentMenuSelect();
    populateSelect("fieldMenuItemType", state.menuItemTypes, true);
    populateSelect("fieldSubscriptionCode", state.subscriptions, true);
    populateSelect("fieldIconId", state.menuIcons, true);
    updateMenuIconPreview();
}

function populateParentMenuSelect() {
    const select = byId("fieldParentMenuId");

    if (!select) {
        return;
    }

    const currentValue = select.value;
    const currentMenuId = state.currentItem?.menuId ?? null;
    const parentRows = getParentRows().filter((row) => row.menuId !== currentMenuId);

    select.innerHTML = [
        '<option value=""></option>',
        ...parentRows.map((row) => {
            const value = String(row.menuId ?? "");
            const labelParts = [row.menuItemText || `Menu ${value}`];
            const typeLabel = row.menuItemType ? row.menuItemType : "";

            if (typeLabel) {
                labelParts.push(typeLabel);
            }

            if (!row.active) {
                labelParts.push("inactive");
            }

            return `<option value="${escapeHtml(value)}">${escapeHtml(labelParts.join(" - "))}</option>`;
        })
    ].join("");

    if (currentValue) {
        select.value = currentValue;
    }
}

function populateSelect(id, options, includeBlank = false) {
    const select = byId(id);

    if (!select) {
        return;
    }

    const currentValue = select.value;
    const entries = Array.isArray(options) ? options : [];

    select.innerHTML = [
        includeBlank ? '<option value=""></option>' : "",
        ...entries.map((option) => {
            const value = String(option.value ?? "");
            const label = String(option.label ?? value);
            return `<option value="${escapeHtml(value)}">${escapeHtml(label)}</option>`;
        })
    ].join("");

    if (currentValue) {
        select.value = currentValue;
    }
}

function updateMenuIconPreview() {
    const preview = els.menuIconPreview;
    const select = byId("fieldIconId");

    if (!preview || !select) {
        return;
    }

    const icon = findMenuIcon(parseNullableInt(select.value));

    if (!icon) {
        preview.innerHTML = "";
        preview.classList.add("is-empty");
        preview.textContent = "No icon selected.";
        return;
    }

    preview.classList.remove("is-empty");
    preview.innerHTML = icon.svg || "";
}

function findMenuIcon(iconId) {
    return state.menuIcons.find((icon) => parseNullableInt(icon.value) === iconId) || null;
}

function showEmptyState(message) {
    if (!els.menuEmptyState) {
        return;
    }

    els.menuEmptyState.textContent = message;
    els.menuEmptyState.classList.add("is-visible");
    els.menuEmptyState.hidden = false;
}

function hideEmptyState() {
    if (!els.menuEmptyState) {
        return;
    }

    els.menuEmptyState.classList.remove("is-visible");
    els.menuEmptyState.hidden = true;
}
