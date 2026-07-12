const state = {
    customers: [],
    selectedCustomerId: null,
    selectedCustomer: null,
    activities: [],
    activitiesFilter: 'active',
    view: 'dashboard',
    year: new Date().getFullYear(),
    month: new Date().getMonth() + 1,
    calendar: null,
    openDayDialogDate: null,
    invoice: null,
    companyFooter: null
};

const storageKey = 'eis.selectedCustomerId';

const api = {
    bootstrap: 'api/bootstrap',
    customers: 'api/customers',
    selection: 'api/selection',
    calendar: 'api/calendar',
    invoice: 'api/invoice',
    invoicePdf: 'api/invoice/pdf'
};

document.addEventListener('DOMContentLoaded', init);

async function init() {
    bindIcons();
    bindChrome();
    const entryDialog = document.getElementById('entryDialog');
    const timeDialog = document.getElementById('timeDialog');
    entryDialog.addEventListener('close', () => {
        state.openDayDialogDate = null;
    });
    entryDialog.addEventListener('click', safeAsync(async (event) => {
        if (!(event.target instanceof Element)) return;
        const button = event.target.closest('[data-dialog-action]');
        if (!button || !entryDialog.contains(button)) return;

        const action = button.dataset.dialogAction;
        const entryId = Number(button.dataset.id || '0');
        const date = state.openDayDialogDate;
        if (!date) return;

        if (action === 'new-time-entry') {
            await timeEntryDialog(date);
        } else if (action === 'edit-time') {
            const entry = (state.calendar?.days ?? []).flatMap((item) => item.entries ?? []).find((item) => item.id === entryId);
            await timeEntryDialog(date, entry);
        } else if (action === 'delete-time') {
            await deleteEntity(`api/time-entries/${entryId}`, 'Delete time entry?');
            entryDialog.close();
            state.openDayDialogDate = null;
            await reloadCurrentView();
        }
    }));
    document.getElementById('timeDialogClose').addEventListener('click', () => {
        timeDialog.close();
    });
    await reloadBootstrap();
}

function bindIcons() {
    document.querySelectorAll('[data-icon]').forEach((node) => {
        node.innerHTML = iconSvg(node.dataset.icon);
    });
}

function bindChrome() {
    document.getElementById('refreshButton').addEventListener('click', safeAsync(reloadCurrentView));
    document.getElementById('customerSelect').addEventListener('change', safeAsync(async (event) => {
        const id = event.target.value ? Number(event.target.value) : null;
        await selectCustomer(id, true);
    }));

    document.querySelectorAll('.nav-item').forEach((button) => {
        button.addEventListener('click', safeAsync(async () => {
            await setView(button.dataset.view);
        }));
    });
}

async function reloadBootstrap() {
    const payload = await fetchJson(api.bootstrap);
    state.customers = payload.customers ?? [];
    state.companyFooter = payload.companyFooter ?? null;

    const storedId = Number(localStorage.getItem(storageKey) || 0) || null;
    const sessionId = payload.selectedCustomerId ?? null;
    const candidate = state.customers.some((c) => c.id === storedId && !c.inactive) ? storedId
        : state.customers.some((c) => c.id === sessionId && !c.inactive) ? sessionId
        : null;

    await populateCustomerSelect();
    await selectCustomer(candidate, false);
}

async function populateCustomerSelect() {
    const select = document.getElementById('customerSelect');
    select.innerHTML = '';

    const placeholder = document.createElement('option');
    placeholder.value = '';
    placeholder.textContent = 'Select customer';
    select.append(placeholder);

    state.customers.filter((customer) => !customer.inactive).forEach((customer) => {
        const option = document.createElement('option');
        option.value = customer.id;
        option.textContent = customer.companyName;
        select.append(option);
    });

    select.value = state.selectedCustomerId ? String(state.selectedCustomerId) : '';
}

async function selectCustomer(customerId, persist) {
    state.selectedCustomerId = customerId;
    state.selectedCustomer = state.customers.find((customer) => customer.id === customerId) ?? null;
    state.activities = [];
    state.activitiesFilter = 'active';
    state.calendar = null;
    state.invoice = null;

    const label = document.getElementById('selectedCustomerLabel');
    label.textContent = state.selectedCustomer
        ? `${state.selectedCustomer.companyName} - ${state.selectedCustomer.contactName}`
        : 'No customer selected';

    if (persist) {
        if (customerId) {
            localStorage.setItem(storageKey, String(customerId));
        } else {
            localStorage.removeItem(storageKey);
        }
        await fetchJson(api.selection, {
            method: 'POST',
            body: { customerId }
        });
    }

    if (state.selectedCustomerId) {
        const payload = await fetchJson(`${api.customers}/${state.selectedCustomerId}/activities`);
        state.activities = payload.activities ?? [];
    }

    await populateCustomerSelect();
    await reloadCurrentView();
}

async function setView(view) {
    state.view = view;
    document.querySelectorAll('.nav-item').forEach((button) => {
        button.classList.toggle('active', button.dataset.view === view);
    });
    await reloadCurrentView();
}

async function reloadCurrentView() {
    const root = document.getElementById('viewRoot');
    if (!state.selectedCustomer && state.view !== 'customers') {
        renderNoCustomer(root);
        return;
    }

    if (state.view === 'customers') return renderCustomersView(root);
    if (state.view === 'activities') return renderActivitiesView(root);
    if (state.view === 'time') return renderTimeView(root);
    if (state.view === 'materials') return renderMaterialsView(root);
    if (state.view === 'invoicing') return renderInvoiceView(root);
    return renderDashboardView(root);
}

function renderNoCustomer(root) {
    root.innerHTML = `
        <div class="section">
            <div class="section-body">
                <h1 class="page-title">Select a customer</h1>
                <p class="page-meta">Time entries and materials can only be registered after a customer has been selected.</p>
            </div>
        </div>
    `;
    setDisabledWorkTabs(true);
}

function setDisabledWorkTabs(disabled) {
    document.querySelectorAll('.nav-item').forEach((button) => {
        if (!['customers', 'dashboard'].includes(button.dataset.view)) {
            button.disabled = disabled;
        }
    });
}

function renderDashboardView(root) {
    setDisabledWorkTabs(!state.selectedCustomer);
    root.innerHTML = `
        <div class="page-head">
            <div>
                <h1 class="page-title">Overview</h1>
                <div class="page-meta">${state.selectedCustomer ? state.selectedCustomer.companyName : 'No customer selected'}</div>
            </div>
            <div class="toolbar">
                <button class="btn-secondary" data-action="jump-customers">Customer list</button>
                <button class="btn" data-action="new-customer">New customer</button>
            </div>
        </div>
        <div class="kpi-row">
            <div class="kpi"><div class="kpi-label">Hourly rate</div><div class="kpi-value">${money(state.selectedCustomer?.hourlyRate ?? 0)}</div></div>
            <div class="kpi"><div class="kpi-label">VAT</div><div class="kpi-value">${formatPercent(state.selectedCustomer?.vatRate ?? 25)}</div></div>
            <div class="kpi"><div class="kpi-label">Active activities</div><div class="kpi-value">${state.activities.filter((activity) => !activity.inactive).length}</div></div>
            <div class="kpi"><div class="kpi-label">Current period</div><div class="kpi-value">${monthName(state.year, state.month)}</div></div>
        </div>
        <div class="section">
            <div class="section-head"><h2 class="section-title">Shortcuts</h2></div>
            <div class="section-body">
                <p>Use the menu to manage customers, activities, time entries, materials, and invoicing.</p>
            </div>
        </div>
    `;
    bindActions(root);
    bindIcons();
}

function renderCustomersView(root) {
    setDisabledWorkTabs(false);
    root.innerHTML = `
        <div class="page-head">
            <div>
                <h1 class="page-title">Customers</h1>
                <div class="page-meta">Create, edit, and delete customer data.</div>
            </div>
            <div class="toolbar">
                <button class="btn" data-action="new-customer">New customer</button>
            </div>
        </div>
        <div class="section">
            <div class="section-head"><h2 class="section-title">Customer list</h2></div>
            <div class="section-body" style="overflow:auto;">
                <table class="table">
                    <thead>
                        <tr>
                            <th>Company</th><th>Contact</th><th>Email</th><th>Phone</th><th>Address</th><th>Hourly rate</th><th></th>
                        </tr>
                    </thead>
                    <tbody>${state.customers.map(renderCustomerRow).join('')}</tbody>
                </table>
            </div>
        </div>
    `;
    bindActions(root);
    bindIcons();
}

function renderCustomerRow(customer) {
    return `
        <tr class="${customer.inactive ? 'inactive-row' : ''}">
            <td>${escapeHtml(customer.companyName)}</td>
            <td>${escapeHtml(customer.contactName)}</td>
            <td>${escapeHtml(customer.contactEmail)}</td>
            <td>${escapeHtml(customer.phoneNumber ?? '')}</td>
            <td>${escapeHtml(customer.addressLine)}, ${escapeHtml(customer.postalCode)} ${escapeHtml(customer.city)}</td>
            <td>${money(customer.hourlyRate)}</td>
            <td class="action-cell">
                <div class="toolbar toolbar-right toolbar-nowrap">
                    <button type="button" class="icon-button icon-button-inline" data-action="edit-customer" data-id="${customer.id}" title="Edit customer" aria-label="Edit customer">
                        <span class="icon">${iconSvg('edit')}</span>
                    </button>
                    <button type="button" class="icon-button icon-button-inline danger" data-action="delete-customer" data-id="${customer.id}" title="Delete customer" aria-label="Delete customer">
                        <span class="icon">${iconSvg('trash')}</span>
                    </button>
                </div>
            </td>
        </tr>
    `;
}

async function renderActivitiesView(root) {
    if (!state.selectedCustomerId) return renderNoCustomer(root);
    const payload = await fetchJson(`${api.customers}/${state.selectedCustomerId}/activities`);
    state.activities = payload.activities ?? [];
    const visibleActivities = state.activitiesFilter === 'all'
        ? state.activities
        : state.activities.filter((activity) => !activity.inactive);

    root.innerHTML = `
        <div class="page-head">
            <div>
                <h1 class="page-title">Activities</h1>
                <div class="page-meta">${state.selectedCustomer.companyName}</div>
            </div>
            <div class="toolbar">
                <button class="btn-secondary ${state.activitiesFilter === 'active' ? 'active' : ''}" data-action="filter-activities-active">Active only</button>
                <button class="btn-secondary ${state.activitiesFilter === 'all' ? 'active' : ''}" data-action="filter-activities-all">All</button>
                <button class="btn" data-action="new-activity">New activity</button>
            </div>
        </div>
        <div class="section">
            <div class="section-head"><h2 class="section-title">Activities for selected customer</h2></div>
            <div class="section-body" style="overflow:auto;">
                <table class="table">
                    <thead><tr><th>Id</th><th>Short description</th><th>Long description</th><th>Status</th><th></th></tr></thead>
                    <tbody>
                        ${visibleActivities.map((activity) => `
                            <tr class="${activity.inactive ? 'inactive-row' : ''}">
                                <td>${activity.id}</td>
                                <td>${escapeHtml(activity.shortDescription)}</td>
                                <td>${escapeHtml(activity.longDescription ?? '')}</td>
                                <td class="status-cell">${activity.inactive ? 'Inactive' : 'Active'}</td>
                                <td class="action-cell">
                                    <div class="toolbar toolbar-right toolbar-nowrap">
                                        <button type="button" class="icon-button icon-button-inline" data-action="edit-activity" data-id="${activity.id}" title="Edit activity" aria-label="Edit activity">
                                            <span class="icon">${iconSvg('edit')}</span>
                                        </button>
                                        <button type="button" class="icon-button icon-button-inline danger ${activity.inactive ? 'action-placeholder' : ''}" ${activity.inactive ? 'tabindex="-1" aria-hidden="true"' : ''} data-action="delete-activity" data-id="${activity.id}" title="Delete activity" aria-label="Delete activity">
                                            <span class="icon">${iconSvg('trash')}</span>
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        </div>
    `;
    bindActions(root);
    bindIcons();
}

async function renderTimeView(root) {
    await renderCalendarSection(root, 'time');
}

async function renderMaterialsView(root) {
    await renderCalendarSection(root, 'materials');
}

async function renderInvoiceView(root) {
    state.invoice = await fetchJson(`${api.invoice}?customerId=${state.selectedCustomerId}&year=${state.year}&month=${state.month}`);
    const timeAmount = (state.invoice.timeRows ?? []).reduce((sum, row) => sum + Number(row.amount ?? 0), 0);
    const materialAmount = (state.invoice.materialRows ?? []).reduce((sum, row) => sum + Number(row.amount ?? 0), 0);
    const subtotal = Number(state.invoice.subtotal ?? 0);
    const vatAmount = Number(state.invoice.vatAmount ?? 0);
    const total = Number(state.invoice.total ?? 0);
    root.innerHTML = `
        <div class="page-head">
            <div>
                <h1 class="page-title">Invoicing</h1>
                <div class="page-meta">${monthName(state.year, state.month)} - ${state.selectedCustomer.companyName}</div>
            </div>
            <div class="toolbar">
                <button type="button" class="icon-button icon-button-inline danger" data-action="invoice-pdf" title="Create PDF" aria-label="Create PDF">
                    <span class="icon">${iconSvg('pdf')}</span>
                </button>
                <button class="btn-secondary" data-action="prev-month">Previous month</button>
                <button class="btn-secondary" data-action="next-month">Next month</button>
            </div>
        </div>
        <div class="kpi-row">
            <div class="kpi"><div class="kpi-label">Total hours</div><div class="kpi-value">${formatHours(state.invoice.monthHours)}</div></div>
            <div class="kpi"><div class="kpi-label">Subtotal</div><div class="kpi-value">${money(state.invoice.subtotal)}</div></div>
            <div class="kpi"><div class="kpi-label">VAT</div><div class="kpi-value">${money(state.invoice.vatAmount)}</div></div>
            <div class="kpi"><div class="kpi-label">Total</div><div class="kpi-value">${money(state.invoice.total)}</div></div>
        </div>
        <div class="invoice-summary">
            <div class="section">
                <div class="section-head"><h2 class="section-title">Time by activity</h2></div>
                <div class="section-body" style="overflow:auto;">
                    <table class="table">
                        <thead><tr><th>Activity</th><th>Hours</th><th class="money">Amount</th></tr></thead>
                        <tbody>
                            ${state.invoice.timeRows.map((row) => `
                                <tr>
                                    <td>${escapeHtml(row.activityShortDescription)}</td>
                                    <td>${formatHours(row.hours)}</td>
                                    <td class="money">${money(row.amount)}</td>
                                </tr>
                            `).join('')}
                        </tbody>
                        <tfoot>
                            <tr>
                                <td><strong>Total</strong></td>
                                <td><strong>${formatHours(state.invoice.monthHours)}</strong></td>
                                <td class="money"><strong>${money(timeAmount)}</strong></td>
                            </tr>
                        </tfoot>
                    </table>
                </div>
            </div>
            <div class="section">
                <div class="section-head"><h2 class="section-title">Materials</h2></div>
                <div class="section-body" style="overflow:auto;">
                    <table class="table">
                        <thead><tr><th>Description</th><th>Quantity</th><th>Unit</th><th>Unit price</th><th class="money">Total</th></tr></thead>
                        <tbody>
                            ${state.invoice.materialRows.map((row) => `
                                <tr>
                                    <td>${escapeHtml(row.shortDescription)}</td>
                                    <td>${formatQuantity(row.quantity)}</td>
                                    <td>${escapeHtml(row.unit)}</td>
                                    <td>${money(row.unitPrice)}</td>
                                    <td class="money">${money(row.amount)}</td>
                                </tr>
                            `).join('')}
                        </tbody>
                        <tfoot>
                            <tr>
                                <td colspan="4"><strong>Total</strong></td>
                                <td class="money"><strong>${money(materialAmount)}</strong></td>
                            </tr>
                        </tfoot>
                    </table>
                </div>
            </div>
            <div class="invoice-box">
                <table class="table invoice-total-table">
                    <tbody>
                        <tr>
                            <th>Subtotal excl. VAT</th>
                            <td class="money">${money(subtotal)}</td>
                        </tr>
                        <tr>
                            <th>VAT</th>
                            <td class="money">${money(vatAmount)}</td>
                        </tr>
                        <tr>
                            <th>Total incl. VAT</th>
                            <td class="money">${money(total)}</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
    `;
    bindActions(root);
    bindIcons();
}

async function renderCalendarSection(root, kind) {
    const label = kind === 'time' ? 'Time entries' : 'Materials';
    state.calendar = await fetchJson(`${api.calendar}?customerId=${state.selectedCustomerId}&year=${state.year}&month=${state.month}`);

    root.innerHTML = `
        <div class="page-head">
            <div>
                <h1 class="page-title">${label}</h1>
                <div class="page-meta">${state.selectedCustomer.companyName} - ${monthName(state.year, state.month)}</div>
            </div>
            <div class="toolbar">
                ${kind === 'time' ? '<button class="btn" data-action="add-time-today"><span class="icon" data-icon="plus"></span><span>Add time today</span></button>' : ''}
                <button class="btn-secondary" data-action="prev-month">Previous month</button>
                <button class="btn-secondary" data-action="next-month">Next month</button>
            </div>
        </div>
        <div class="kpi-row">
            <div class="kpi"><div class="kpi-label">Month total</div><div class="kpi-value">${formatHours(state.calendar.monthHours)}</div></div>
            <div class="kpi"><div class="kpi-label">Customer</div><div class="kpi-value">${escapeHtml(state.selectedCustomer.companyName)}</div></div>
        </div>
        <div class="section">
            <div class="section-head"><h2 class="section-title">${monthName(state.year, state.month)}</h2></div>
            <div class="section-body" style="overflow:auto;">
                ${kind === 'time' ? renderCalendarTable() : renderMaterialMonthTable()}
            </div>
        </div>
        ${kind === 'material' ? `
        <div class="section">
            <div class="section-head"><h2 class="section-title">Register materials</h2></div>
            <div class="section-body">
                ${renderMaterialForm()}
            </div>
        </div>
        ` : ''}
    `;

    bindActions(root);
    bindIcons();

    if (kind === 'material') {
        bindMaterialForm();
    }
}

function renderCalendarTable() {
    const weeks = buildCalendarGrid(state.calendar.days ?? []);
    return `
        <table class="calendar">
            <thead>
                <tr>
                    <th class="week-number-col">Week</th><th>Mon</th><th>Tue</th><th>Wed</th><th>Thu</th><th>Fri</th><th>Sat</th><th>Sun</th><th>Week total</th>
                </tr>
            </thead>
            <tbody>
                ${weeks.map((week) => `
                    <tr>
                        <td class="week-number-cell">${week.weekNumber ?? ''}</td>
                        ${week.cells.map((cell) => `<td>${cell ? renderDayCell(cell) : ''}</td>`).join('')}
                        <td class="week-total">${formatHours(week.hours)}</td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
}

function renderDayCell(day) {
    const cls = day.hours > 0 ? 'calendar-day has-hours' : 'calendar-day';
    return `
        <div class="${cls}">
            <div class="calendar-day-number">${day.dayOfMonth}</div>
            <div class="calendar-hours ${day.hours > 0 ? '' : 'zero'}">${day.hours > 0 ? formatHours(day.hours) : '0.0'}</div>
            <button type="button" class="day-details-button" data-action="open-day" data-date="${day.date}">
                <span class="icon">${iconSvg('edit')}</span>
                <span>Open entries</span>
            </button>
        </div>
    `;
}

function renderMaterialMonthTable() {
    return `
        <table class="table">
            <thead><tr><th>Date</th><th>Description</th><th>Quantity</th><th>Unit</th><th>Unit price</th><th class="money">Total</th><th></th></tr></thead>
            <tbody>
                ${(state.calendar.materials ?? []).map((item) => `
                    <tr>
                        <td>${item.entryDate}</td>
                        <td>${escapeHtml(item.shortDescription)}</td>
                        <td>${formatQuantity(item.quantity)}</td>
                        <td>${escapeHtml(item.unit)}</td>
                        <td>${money(item.unitPrice)}</td>
                        <td class="money">${money(Number(item.quantity ?? 0) * Number(item.unitPrice ?? 0))}</td>
                        <td class="action-cell">
                <div class="toolbar toolbar-right toolbar-nowrap">
                    <button type="button" class="icon-button icon-button-inline" data-action="edit-material" data-id="${item.id}" title="Edit material" aria-label="Edit material">
                                    <span class="icon">${iconSvg('edit')}</span>
                                </button>
                                <button type="button" class="icon-button icon-button-inline danger" data-action="delete-material" data-id="${item.id}" title="Delete material" aria-label="Delete material">
                                    <span class="icon">${iconSvg('trash')}</span>
                                </button>
                            </div>
                        </td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
}

function renderTimeForm(date = todayIso(), formId = 'timeForm') {
    const activeActivities = state.activities.filter((activity) => !activity.inactive);
    return `
        <form id="${formId}" class="grid-4">
            <div class="field"><label>Date</label><input name="entryDate" type="date" value="${date}"></div>
            <div class="field">
                <label>Activity</label>
                <select name="activityId" required>
                    <option value="">Select activity</option>
                    ${activeActivities.map((activity) => `<option value="${activity.id}">${escapeHtml(activity.shortDescription)}</option>`).join('')}
                </select>
            </div>
            <div class="field">
                <label>Hours</label>
                <select name="hours" required>${halfHourOptions()}</select>
            </div>
            <div class="field">
                <label>Note</label>
                <input name="note" placeholder="Optional">
            </div>
            <div class="field" style="grid-column:1/-1;"><button class="btn" type="submit">Save time</button></div>
        </form>
    `;
}


function renderMaterialForm() {
    return `
        <form id="materialForm" class="grid-4">
            <div class="field"><label>Date</label><input name="entryDate" type="date" value="${todayIso()}"></div>
            <div class="field"><label>Quantity</label><input name="quantity" type="number" step="0.01" min="0" required></div>
            <div class="field"><label>Unit</label><input name="unit" placeholder="pcs, m, kg" required></div>
            <div class="field"><label>Unit price</label><input name="unitPrice" type="number" step="0.01" min="0" required></div>
            <div class="field" style="grid-column:1/-1;"><label>Description</label><input name="shortDescription" required></div>
            <div class="field" style="grid-column:1/-1;"><button class="btn" type="submit">Save material</button></div>
        </form>
    `;
}

function bindActions(root) {
    root.querySelectorAll('[data-action]').forEach((button) => {
        button.addEventListener('click', safeAsync(handleAction));
    });
}

async function handleAction(event) {
    const action = event.currentTarget.dataset.action;
    const id = event.currentTarget.dataset.id ? Number(event.currentTarget.dataset.id) : null;
    const date = event.currentTarget.dataset.date ?? null;

    switch (action) {
        case 'jump-customers':
            await setView('customers');
            break;
        case 'new-customer':
            await customerDialog();
            break;
        case 'edit-customer':
            await customerDialog(state.customers.find((customer) => customer.id === id));
            break;
        case 'delete-customer':
            await deleteEntity(`${api.customers}/${id}`, 'Mark customer inactive? Related activities will also be marked inactive.');
            await reloadBootstrap();
            break;
        case 'new-activity':
            await activityDialog();
            break;
        case 'edit-activity':
            await activityDialog((await fetchJson(`${api.customers}/${state.selectedCustomerId}/activities`)).activities.find((activity) => activity.id === id));
            break;
        case 'delete-activity':
            await deleteEntity(`api/activities/${id}`, 'Mark activity inactive? Time entries on this activity will remain in history.');
            await reloadCurrentView();
            break;
        case 'filter-activities-active':
            state.activitiesFilter = 'active';
            await reloadCurrentView();
            break;
        case 'filter-activities-all':
            state.activitiesFilter = 'all';
            await reloadCurrentView();
            break;
        case 'prev-month':
            shiftMonth(-1);
            await reloadCurrentView();
            break;
        case 'next-month':
            shiftMonth(1);
            await reloadCurrentView();
            break;
        case 'add-time-today':
            await openTimeTodayDialog();
            break;
        case 'invoice-pdf':
            await createInvoicePdf();
            break;
        case 'new-material':
            await setView('materials');
            break;
        case 'open-day':
            await openDayDialog(date);
            break;
        case 'edit-material':
            await materialDialog((state.calendar.materials ?? []).find((material) => material.id === id));
            break;
        case 'delete-material':
            await deleteEntity(`api/materials/${id}`, 'Delete material?');
            await reloadCurrentView();
            break;
    }
}

async function customerDialog(customer = null) {
    const result = await promptDialog(customer ? 'Edit customer' : 'Customer', [
        { name: 'companyName', label: 'Company name', type: 'text', value: customer?.companyName ?? '', required: true },
        { name: 'contactName', label: 'Contact name', type: 'text', value: customer?.contactName ?? '', required: true },
        { name: 'contactEmail', label: 'Contact email', type: 'email', value: customer?.contactEmail ?? '', required: true },
        { name: 'phoneNumber', label: 'Phone number', type: 'text', value: customer?.phoneNumber ?? '' },
        { name: 'addressLine', label: 'Address', type: 'text', value: customer?.addressLine ?? '', required: true },
        { name: 'postalCode', label: 'Postal code', type: 'text', value: customer?.postalCode ?? '', required: true },
        { name: 'city', label: 'City', type: 'text', value: customer?.city ?? '', required: true },
        { name: 'hourlyRate', label: 'Hourly rate', type: 'number', step: '0.01', value: customer?.hourlyRate ?? 0, required: true },
        { name: 'inactive', label: 'Status', type: 'select', options: [
            { value: 'false', label: 'Active' },
            { value: 'true', label: 'Inactive' }
        ], value: String(customer?.inactive ?? false), required: true }
    ]);
    if (!result) return;

    const body = { ...result, hourlyRate: Number(result.hourlyRate), inactive: result.inactive === 'true' };

    if (customer) {
        await fetchJson(`${api.customers}/${customer.id}`, { method: 'PUT', body });
        showBanner('Customer updated successfully.');
        await reloadBootstrap();
    } else {
        const created = await fetchJson(api.customers, { method: 'POST', body });
        localStorage.setItem(storageKey, String(created.id));
        showBanner('Customer created successfully.');
        await reloadBootstrap();
    }
}

async function activityDialog(activity = null) {
    const result = await promptDialog(activity ? 'Edit activity' : 'Activity', [
        { name: 'shortDescription', label: 'Short description', type: 'text', value: activity?.shortDescription ?? '', required: true },
        { name: 'longDescription', label: 'Long description', type: 'textarea', value: activity?.longDescription ?? '' },
        { name: 'inactive', label: 'Status', type: 'select', options: [
            { value: 'false', label: 'Active' },
            { value: 'true', label: 'Inactive' }
        ], value: String(activity?.inactive ?? false), required: true }
    ]);
    if (!result) return;

    const body = { ...result, inactive: result.inactive === 'true' };

    if (activity) {
        await fetchJson(`api/activities/${activity.id}`, { method: 'PUT', body });
    } else {
        await fetchJson(`${api.customers}/${state.selectedCustomerId}/activities`, { method: 'POST', body });
    }
    await reloadCurrentView();
}

async function openDayDialog(date) {
    const dialog = document.getElementById('entryDialog');
    state.openDayDialogDate = date;

    const hasDay = await refreshDayDialog(date);
    if (!hasDay) {
        state.openDayDialogDate = null;
        return;
    }
    dialog.showModal();
}

async function openTimeTodayDialog() {
    const dialog = document.getElementById('timeDialog');
    document.getElementById('timeDialogBody').innerHTML = `
        <div class="dialog-section">
            ${renderTimeForm(todayIso(), 'timeQuickForm')}
        </div>
    `;
    bindTimeForm('timeQuickForm', 'timeDialog');
    dialog.showModal();
}

function renderDayDialog(day, date) {
    document.getElementById('dialogDateTitle').textContent = `Entries for ${date}`;
    document.getElementById('dialogDateMeta').textContent = `${formatHours(day.hours)} hour(s) registered`;
    document.getElementById('dialogEntries').innerHTML = `
        <div class="dialog-section">
            <div class="dialog-scroll">
                <table class="table dialog-table">
                    <thead>
                        <tr>
                            <th>Activity</th>
                            <th>Hours</th>
                            <th>Note</th>
                            <th class="actions-col"></th>
                        </tr>
                    </thead>
                    <tbody>
                        ${(day.entries ?? []).map((entry) => `
                            <tr data-entry-id="${entry.id}">
                                <td>${escapeHtml(entry.activityShortDescription)}</td>
                                <td>${formatHours(entry.hours)}</td>
                                <td>${escapeHtml(entry.note ?? '')}</td>
                                <td>
                                    <div class="toolbar toolbar-right">
                                        <button type="button" class="icon-button icon-button-inline" data-dialog-action="edit-time" data-id="${entry.id}" title="Edit">
                                            <span class="icon">${iconSvg('edit')}</span>
                                        </button>
                                        <button type="button" class="icon-button icon-button-inline danger" data-dialog-action="delete-time" data-id="${entry.id}" title="Delete">
                                            <span class="icon">${iconSvg('trash')}</span>
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        </div>
    `;
    bindIcons();
    document.getElementById('dialogFormSlot').innerHTML = '';
}

async function refreshDayDialog(date) {
    state.calendar = await fetchJson(`${api.calendar}?customerId=${state.selectedCustomerId}&year=${state.year}&month=${state.month}`);
    const day = (state.calendar?.days ?? []).find((item) => item.date === date);
    if (!day) return false;
    renderDayDialog(day, date);
    return true;
}

function refreshOpenDayDialogFromState(date) {
    const day = (state.calendar?.days ?? []).find((item) => item.date === date);
    if (!day) return false;
    renderDayDialog(day, date);
    return true;
}

function activityShortDescription(activityId, fallback = 'Activity') {
    const activity = state.activities.find((item) => Number(item.id) === Number(activityId));
    return activity?.shortDescription ?? fallback;
}

function updateDayDialogState(savedEntry, previousEntry = null) {
    const days = state.calendar?.days ?? [];
    if (!days.length || !savedEntry) return;

    const previousDate = previousEntry?.entryDate ?? null;
    const previousHours = previousEntry ? Number(previousEntry.hours ?? 0) : 0;
    const savedDate = savedEntry.entryDate ?? null;
    const savedHours = Number(savedEntry.hours ?? 0);

    if (previousDate) {
        const previousDay = days.find((item) => item.date === previousDate);
        if (previousDay) {
            previousDay.entries = (previousDay.entries ?? []).filter((item) => item.id !== previousEntry.id);
            previousDay.hours = Number(previousDay.hours ?? 0) - previousHours;
        }
    }

    if (savedDate) {
        const savedDay = days.find((item) => item.date === savedDate);
        if (savedDay) {
            const nextEntry = {
                ...savedEntry,
                activityShortDescription: activityShortDescription(savedEntry.activityId, savedEntry.activityShortDescription)
            };
            const entries = (savedDay.entries ?? []).filter((item) => item.id !== nextEntry.id);
            entries.push(nextEntry);
            entries.sort((left, right) => String(left.activityShortDescription ?? '').localeCompare(String(right.activityShortDescription ?? '')));
            savedDay.entries = entries;
            savedDay.hours = Number(savedDay.hours ?? 0) + savedHours;
        }
    }
}

async function timeEntryDialog(date, entry = null) {
    const activityOptions = state.activities
        .filter((activity) => !activity.inactive)
        .map((activity) => ({ value: activity.id, label: activity.shortDescription }));
    if (entry?.activityId && !activityOptions.some((option) => Number(option.value) === Number(entry.activityId))) {
        activityOptions.unshift({
            value: entry.activityId,
            label: `${entry.activityShortDescription ?? 'Activity'} (inactive)`
        });
    }
    const result = await promptDialog(entry ? 'Edit time' : 'New time', [
        { name: 'entryDate', label: 'Date', type: 'date', value: entry?.entryDate ?? date, required: true },
        { name: 'activityId', label: 'Activity', type: 'select', options: activityOptions, value: entry?.activityId ?? '', required: true },
        { name: 'hours', label: 'Hours', type: 'select', options: halfHourValueOptions(), value: entry?.hours ?? '0.5', required: true },
        { name: 'note', label: 'Note', type: 'text', value: entry?.note ?? '' }
    ]);
    if (!result) return;

    const body = {
        ...result,
        hours: Number(result.hours),
        activityId: Number(result.activityId),
        customerId: state.selectedCustomerId
    };

    const previousEntry = entry ? { ...entry } : null;
    if (entry) {
        await fetchJson(`api/time-entries/${entry.id}`, { method: 'PUT', body });
    } else {
        const created = await fetchJson(`${api.customers}/${state.selectedCustomerId}/time-entries`, { method: 'POST', body });
        entry = { id: created.id };
    }
    const savedEntry = {
        ...body,
        id: entry.id,
        activityShortDescription: activityShortDescription(body.activityId, previousEntry?.activityShortDescription ?? 'Activity')
    };
    updateDayDialogState(savedEntry, previousEntry);

    if (state.openDayDialogDate) {
        refreshOpenDayDialogFromState(state.openDayDialogDate);
    }
    await reloadCurrentView();
    if (state.openDayDialogDate) {
        refreshOpenDayDialogFromState(state.openDayDialogDate);
    }
}

async function bindTimeForm(formId = 'timeForm', dialogId = null) {
    const form = document.getElementById(formId);
    if (!form) return;
    const activeActivityIds = new Set(state.activities.filter((activity) => !activity.inactive).map((activity) => String(activity.id)));
    form.addEventListener('submit', safeAsync(async (event) => {
        event.preventDefault();
        const body = Object.fromEntries(new FormData(form).entries());
        body.hours = Number(body.hours);
        body.activityId = Number(body.activityId);
        body.customerId = state.selectedCustomerId;
        if (!activeActivityIds.has(String(body.activityId))) {
            throw new Error('Select an active activity.');
        }
        await fetchJson(`${api.customers}/${state.selectedCustomerId}/time-entries`, { method: 'POST', body });
        form.reset();
        form.querySelector('[name="entryDate"]').value = todayIso();
        if (dialogId) {
            document.getElementById(dialogId).close();
        }
        await reloadCurrentView();
    }));
}

async function bindMaterialForm() {
    const form = document.getElementById('materialForm');
    if (!form) return;
    form.addEventListener('submit', safeAsync(async (event) => {
        event.preventDefault();
        const body = Object.fromEntries(new FormData(form).entries());
        body.quantity = Number(body.quantity);
        body.unitPrice = Number(body.unitPrice);
        body.customerId = state.selectedCustomerId;
        await fetchJson(`${api.customers}/${state.selectedCustomerId}/materials`, { method: 'POST', body });
        form.reset();
        form.querySelector('[name="entryDate"]').value = todayIso();
        await reloadCurrentView();
    }));
}

async function materialDialog(entry = null) {
    const result = await promptDialog(entry ? 'Edit material' : 'New material', [
        { name: 'entryDate', label: 'Date', type: 'date', value: entry?.entryDate ?? todayIso(), required: true },
        { name: 'quantity', label: 'Quantity', type: 'number', step: '0.01', value: entry?.quantity ?? '', required: true },
        { name: 'unit', label: 'Unit', type: 'text', value: entry?.unit ?? '', required: true },
        { name: 'unitPrice', label: 'Unit price', type: 'number', step: '0.01', value: entry?.unitPrice ?? '', required: true },
        { name: 'shortDescription', label: 'Description', type: 'text', value: entry?.shortDescription ?? '', required: true }
    ]);
    if (!result) return;

    const body = {
        ...result,
        quantity: Number(result.quantity),
        unitPrice: Number(result.unitPrice),
        customerId: state.selectedCustomerId
    };

    if (entry) {
        await fetchJson(`api/materials/${entry.id}`, { method: 'PUT', body });
    } else {
        await fetchJson(`${api.customers}/${state.selectedCustomerId}/materials`, { method: 'POST', body });
    }
    await reloadCurrentView();
}

function promptDialog(title, fields) {
    const dialog = document.getElementById('promptDialog');
    document.getElementById('promptTitle').textContent = title;
    document.getElementById('promptSubtitle').textContent = '';

    const formId = 'promptForm';
    document.getElementById('promptBody').innerHTML = `
        <div class="dialog-section">
            <form id="${formId}" class="grid-2"></form>
        </div>
    `;
    const form = document.getElementById(formId);
    form.innerHTML = `${fields.map(renderPromptField).join('')}
        <div class="field" style="grid-column:1/-1;">
            <div class="toolbar">
                <button type="submit" class="btn">Save</button>
                <button type="button" class="btn-secondary" id="cancelPrompt">Cancel</button>
            </div>
        </div>`;

    form.querySelectorAll('[data-prompt-select]').forEach((select) => {
        select.value = select.dataset.current || '';
    });

    dialog.showModal();

    return new Promise((resolve) => {
        let settled = false;
        const finish = (value) => {
            if (settled) return;
            settled = true;
            resolve(value);
        };

        document.getElementById('promptClose').onclick = () => {
            dialog.close();
            finish(null);
        };

        document.getElementById('cancelPrompt').addEventListener('click', () => {
            dialog.close();
            finish(null);
        }, { once: true });

        dialog.addEventListener('cancel', (event) => {
            event.preventDefault();
            dialog.close();
            finish(null);
        }, { once: true });

        form.addEventListener('submit', (event) => {
            event.preventDefault();
            const result = Object.fromEntries(new FormData(form).entries());
            finish(result);
            dialog.close();
        }, { once: true });
    });
}

function renderPromptField(field) {
    const current = field.value ?? '';
    if (field.type === 'textarea') {
        return `
            <div class="field" style="grid-column:1/-1;">
                <label>${escapeHtml(field.label)}</label>
                <textarea name="${field.name}">${escapeHtml(current)}</textarea>
            </div>
        `;
    }
    if (field.type === 'select') {
        const normalizedCurrent = normalizeSelectValue(current, field.options ?? []);
        return `
            <div class="field">
                <label>${escapeHtml(field.label)}</label>
                <select name="${field.name}" ${field.required ? 'required' : ''} data-prompt-select data-current="${escapeHtml(normalizedCurrent)}">
                    <option value="">Select</option>
                    ${field.options.map((option) => `<option value="${option.value}" ${String(option.value) === normalizedCurrent ? 'selected' : ''}>${escapeHtml(option.label)}</option>`).join('')}
                </select>
            </div>
        `;
    }
    return `
        <div class="field">
            <label>${escapeHtml(field.label)}</label>
            <input name="${field.name}" type="${field.type}" value="${escapeHtml(String(current))}" ${field.step ? `step="${field.step}"` : ''} ${field.required ? 'required' : ''}>
        </div>
    `;
}

async function deleteEntity(url, confirmText) {
    if (!window.confirm(confirmText)) return;
    await fetchJson(url, { method: 'DELETE' });
}

function halfHourOptions() {
    return Array.from({ length: 48 }, (_, index) => {
        const value = ((index + 1) * 0.5).toFixed(1);
        return `<option value="${value}">${formatHours(Number(value))}</option>`;
    }).join('');
}

function halfHourValueOptions() {
    return Array.from({ length: 48 }, (_, index) => ({
        value: ((index + 1) * 0.5).toFixed(1),
        label: formatHours((index + 1) * 0.5)
    }));
}

function normalizeSelectValue(value, options) {
    if (value === null || value === undefined || value === '') {
        return '';
    }
    const optionValues = options.map((option) => String(option.value));
    if (!optionValues.every((optionValue) => optionValue !== '' && !Number.isNaN(Number(optionValue)))) {
        return String(value);
    }
    const precision = optionValues.reduce((max, optionValue) => {
        const decimalIndex = optionValue.indexOf('.');
        return Math.max(max, decimalIndex >= 0 ? optionValue.length - decimalIndex - 1 : 0);
    }, 0);
    return Number(value).toFixed(precision);
}

function buildCalendarGrid(days) {
    const firstDayIndex = (new Date(state.year, state.month - 1, 1).getDay() + 6) % 7;
    const padded = [...Array(firstDayIndex).fill(null), ...days];
    while (padded.length % 7 !== 0) padded.push(null);

    const weeks = [];
    for (let i = 0; i < padded.length; i += 7) {
        const cells = padded.slice(i, i + 7);
        const hours = cells.reduce((sum, cell) => sum + Number(cell?.hours ?? 0), 0);
        const firstDate = cells.find((cell) => cell)?.date;
        weeks.push({ cells, hours, weekNumber: firstDate ? isoWeekNumber(new Date(firstDate)) : null });
    }
    return weeks;
}

function shiftMonth(delta) {
    const date = new Date(state.year, state.month - 1 + delta, 1);
    state.year = date.getFullYear();
    state.month = date.getMonth() + 1;
}

async function fetchJson(url, options = {}) {
    const response = await fetch(url, {
        credentials: 'same-origin',
        cache: 'no-store',
        headers: { 'Content-Type': 'application/json' },
        ...options,
        body: options.body ? JSON.stringify(options.body) : undefined
    });

    if (!response.ok) {
        const text = await response.text();
        let message = text || response.statusText;
        try {
            const parsed = JSON.parse(text);
            message = parsed.error || message;
        } catch {
            // ignore
        }
        throw new Error(message || 'Request failed');
    }

    if (response.status === 204) return null;
    return response.json();
}

function safeAsync(fn) {
    return async (...args) => {
        try {
            return await fn(...args);
        } catch (error) {
            reportError(error);
        }
    };
}

function reportError(error) {
    console.error(error);
    const message = error instanceof Error ? error.message : String(error ?? 'Unknown error');
    showBanner(message || 'An unexpected error occurred.');
}

function showBanner(message) {
    const banner = document.getElementById('statusBanner');
    banner.textContent = message;
    banner.classList.remove('hidden');
}

function money(value) {
    const amount = new Intl.NumberFormat('da-DK', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    }).format(Number(value ?? 0));
    return `DKK ${amount}`;
}

function formatHours(value) {
    return new Intl.NumberFormat('da-DK', { minimumFractionDigits: 1, maximumFractionDigits: 1 }).format(Number(value ?? 0));
}

function formatQuantity(value) {
    return new Intl.NumberFormat('da-DK', { minimumFractionDigits: 0, maximumFractionDigits: 2 }).format(Number(value ?? 0));
}

function formatPercent(value) {
    return new Intl.NumberFormat('da-DK', { minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(Number(value ?? 0)) + '%';
}

function isoWeekNumber(date) {
    const utcDate = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
    const dayNumber = (utcDate.getUTCDay() + 6) % 7;
    utcDate.setUTCDate(utcDate.getUTCDate() - dayNumber + 3);
    const firstThursday = new Date(Date.UTC(utcDate.getUTCFullYear(), 0, 4));
    const firstThursdayDayNumber = (firstThursday.getUTCDay() + 6) % 7;
    firstThursday.setUTCDate(firstThursday.getUTCDate() - firstThursdayDayNumber + 3);
    return 1 + Math.round((utcDate - firstThursday) / 604800000);
}

function monthName(year, month) {
    return new Date(year, month - 1, 1).toLocaleDateString('da-DK', { month: 'long', year: 'numeric' });
}

function addDays(date, days) {
    const result = new Date(date);
    result.setDate(result.getDate() + Number(days ?? 0));
    return result;
}

function formatDateDa(date) {
    return new Intl.DateTimeFormat('da-DK').format(date);
}

function todayIso() {
    return new Date().toISOString().slice(0, 10);
}

function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}

function iconSvg(name) {
    const icons = {
        refresh: '<svg viewBox="0 0 24 24"><path d="M21 12a9 9 0 1 1-3-6.7"/><path d="M21 3v6h-6"/></svg>',
        close: '<svg viewBox="0 0 24 24"><path d="M18 6 6 18"/><path d="M6 6l12 12"/></svg>',
        edit: '<svg viewBox="0 0 24 24"><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z"/></svg>',
        trash: '<svg viewBox="0 0 24 24"><path d="M3 6h18"/><path d="M8 6V4h8v2"/><path d="M19 6l-1 14H6L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/></svg>',
        pdf: '<svg viewBox="0 0 24 24"><path d="M6 2h9l3 3v17H6z"/><path d="M15 2v5h3"/><text x="12" y="16" text-anchor="middle" font-size="6" font-family="Arial, sans-serif" font-weight="700" fill="currentColor">PDF</text></svg>',
        plus: '<svg viewBox="0 0 24 24"><path d="M12 5v14"/><path d="M5 12h14"/></svg>'
    };
    return icons[name] || '';
}

async function createInvoicePdf() {
    if (!state.selectedCustomerId || !state.selectedCustomer || !state.invoice) {
        showBanner('Select a customer and open the invoice view first.');
        return;
    }

    const previewWindow = window.open('', '_blank');
    try {
        showBanner('Generating PDF...');

        const invoiceNumber = `${state.selectedCustomerId}${String(state.year).padStart(4, '0')}${String(state.month).padStart(2, '0')}`;
        const invoiceDate = new Date(state.year, state.month, 0);
        const timeAmount = (state.invoice.timeRows ?? []).reduce((sum, row) => sum + Number(row.amount ?? 0), 0);
        const materialAmount = (state.invoice.materialRows ?? []).reduce((sum, row) => sum + Number(row.amount ?? 0), 0);
        const summary = {
            timeAmount,
            materialAmount,
            subtotal: Number(state.invoice.subtotal ?? 0),
            vatAmount: Number(state.invoice.vatAmount ?? 0),
            total: Number(state.invoice.total ?? 0)
        };

        const pdfBlob = await buildInvoicePdfBlob({
            customer: state.selectedCustomer,
            invoice: state.invoice,
            invoiceNumber,
            invoiceDate,
            summary
        });

        await uploadInvoicePdf(pdfBlob, invoiceNumber);

        const objectUrl = URL.createObjectURL(pdfBlob);
        if (previewWindow) {
            previewWindow.location = objectUrl;
            previewWindow.focus();
        } else {
            window.open(objectUrl, '_blank', 'noopener');
        }
        setTimeout(() => URL.revokeObjectURL(objectUrl), 60000);
        showBanner(`PDF created and transferred as invoice-${invoiceNumber}.pdf.`);
    } catch (error) {
        if (previewWindow) previewWindow.close();
        reportError(error);
    }
}

async function uploadInvoicePdf(blob, invoiceNumber) {
    const response = await fetch(`${api.invoicePdf}?customerId=${state.selectedCustomerId}&year=${state.year}&month=${state.month}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/pdf',
            'X-File-Name': `invoice-${invoiceNumber}.pdf`
        },
        body: blob
    });

    if (!response.ok) {
        const text = await response.text();
        let message = text || response.statusText;
        try {
            const parsed = JSON.parse(text);
            message = parsed.error || message;
        } catch {
            // ignore
        }
        throw new Error(message || 'Unable to transfer PDF');
    }

    return response.json().catch(() => null);
}

async function buildInvoicePdfBlob({ customer, invoice, invoiceNumber, invoiceDate, summary }) {
    const logoImage = await loadImageElement('assets/img/eis-tech.png');
    const footerConfig = state.companyFooter || {};
    const dueDays = Number(footerConfig.dueDateDays ?? 14);
    const dueDate = formatDateDa(addDays(new Date(), dueDays));
    const writer = new PdfWriter(logoImage, { customer, invoiceNumber, invoiceDate }, {
        ...footerConfig,
        dueDate
    });
    const hourlyRate = Number(customer?.hourlyRate ?? 0);
    const materialRows = invoice.materialRows ?? [];
    writer.startPage();
    drawTimeSection(writer, (invoice.timeRows ?? []).map((row) => ({
        activity: row.activityShortDescription,
        hours: formatHours(row.hours)
    })), {
        totalHours: formatHours(invoice.monthHours),
        rate: money(hourlyRate),
        totalAmount: money(summary.timeAmount)
    });

    if (materialRows.length > 0) {
        writer.cursorY += 14;
        drawInvoiceSection(writer, 'Materials', [
            { label: 'Product description', width: 276, align: 'left' },
            { label: 'Price', width: 78, align: 'right' },
            { label: 'Qty', width: 55, align: 'right' },
            { label: 'Total price', width: 114, align: 'right' }
        ], materialRows.map((row) => ([
            { text: row.shortDescription, align: 'left' },
            { text: money(row.unitPrice), align: 'right' },
            { text: formatQuantity(row.quantity), align: 'right' },
            { text: money(Number(row.quantity ?? 0) * Number(row.unitPrice ?? 0)), align: 'right' }
        ])), [
            { text: 'Total', align: 'left' },
            { text: '', align: 'right' },
            { text: '', align: 'right' },
            { text: money(summary.materialAmount), align: 'right' }
        ]);

        writer.cursorY += 16;
    } else {
        writer.cursorY += 8;
    }
    drawSummaryBox(writer, [
        { label: 'Subtotal excl. VAT', value: money(summary.subtotal) },
        { label: 'VAT', value: money(summary.vatAmount) },
        { label: 'Total incl. VAT', value: money(summary.total), bold: true }
    ]);

    writer.drawPageNumbers(writer.pages.length);
    return new Blob([writer.buildPdfBytes()], { type: 'application/pdf' });
}

async function loadImageElement(path) {
    const image = new Image();
    image.decoding = 'async';
    const loaded = new Promise((resolve, reject) => {
        image.onload = resolve;
        image.onerror = reject;
    });
    image.src = path;
    await loaded;
    return image;
}

function drawInvoiceHeader(writer, customer, invoiceNumber, invoiceDate, options = {}) {
    const logoWidth = 117;
    const headerTop = 36;
    const billToX = 36;
    const billToWidth = 190;
    const logoX = 239;
    const invoiceX = 554;
    let billToY = headerTop;

    const sourceWidth = writer.logoImage?.naturalWidth || writer.logoImage?.width || logoWidth;
    const sourceHeight = writer.logoImage?.naturalHeight || writer.logoImage?.height || logoWidth;
    const logoHeight = Math.round(logoWidth * (sourceHeight / sourceWidth));

    writer.image(writer.logoImage, logoX, headerTop - 10, logoWidth, logoHeight);

    if (options.showBillTo !== false) {
        billToY -= 2;
        billToY = drawPdfWrappedText(writer, customer.companyName ?? '', billToX, billToY, billToWidth, { size: 11, bold: true, lineHeight: 13 });
        billToY += 2;
        billToY = drawPdfWrappedText(writer, customer.addressLine ?? '', billToX, billToY, billToWidth, { size: 9, lineHeight: 12 });
        billToY = drawPdfWrappedText(writer, `${customer.postalCode ?? ''} ${customer.city ?? ''}`.trim(), billToX, billToY, billToWidth, { size: 9, lineHeight: 12 });
        billToY = drawPdfWrappedText(writer, `Contact: ${customer.contactName ?? ''}`, billToX, billToY, billToWidth, { size: 9, lineHeight: 12 });
        drawPdfWrappedText(writer, `Email: ${customer.contactEmail ?? ''}`, billToX, billToY, billToWidth, { size: 9, lineHeight: 12 });
    }

    writer.text('INVOICE', invoiceX, headerTop, { size: 20, bold: true, align: 'right' });
    writer.text(`Invoice no. ${invoiceNumber}`, invoiceX, headerTop + 26, { size: 10, bold: true, align: 'right' });
    writer.text(`Invoice date: ${invoiceDate.toLocaleDateString('en-GB')}`, invoiceX, headerTop + 42, { size: 9, align: 'right' });

    writer.line(36, 186, 559, 186, { color: '#d7e0e7' });
}

function drawTimeSection(writer, rows, total) {
    const title = 'Time by Activity';
    const tableX = 36;
    const tableYGap = 2;
    const titleHeight = 16;
    const headerHeight = 20;
    const rowGap = 0;
    const activityWidth = 260;
    const hoursWidth = 65;
    const rateWidth = 90;
    const totalWidth = 108;
    const tableWidth = activityWidth + hoursWidth + rateWidth + totalWidth;
    const hoursX = tableX + activityWidth;
    const rateX = hoursX + hoursWidth;
    const totalX = rateX + rateWidth;

    const pageBottom = writer.contentBottom ?? 730;

    if (writer.cursorY + titleHeight + headerHeight > pageBottom) {
        writer.newPage();
    }

    writer.text(title, tableX, writer.cursorY, { size: 14, bold: true });
    writer.cursorY += titleHeight;
    writer.cursorY += tableYGap;

    const drawHeader = () => {
        writer.rect(tableX, writer.cursorY, tableWidth, headerHeight, { fill: '#f8fafb', stroke: '#d7e0e7' });
        writer.line(tableX, writer.cursorY, tableX, writer.cursorY + headerHeight, { color: '#d7e0e7' });
        writer.line(hoursX, writer.cursorY, hoursX, writer.cursorY + headerHeight, { color: '#d7e0e7' });
        writer.line(rateX, writer.cursorY, rateX, writer.cursorY + headerHeight, { color: '#d7e0e7' });
        writer.line(totalX, writer.cursorY, totalX, writer.cursorY + headerHeight, { color: '#d7e0e7' });
        writer.line(tableX + tableWidth, writer.cursorY, tableX + tableWidth, writer.cursorY + headerHeight, { color: '#d7e0e7' });
        writer.text('Activity', tableX + 5, writer.cursorY + 6, { size: 9, bold: true, align: 'left' });
        writer.text('Hours', hoursX + hoursWidth - 5, writer.cursorY + 6, { size: 9, bold: true, align: 'right' });
        writer.text('Rate', rateX + rateWidth - 5, writer.cursorY + 6, { size: 9, bold: true, align: 'right' });
        writer.text('Total', totalX + totalWidth - 5, writer.cursorY + 6, { size: 9, bold: true, align: 'right' });
        writer.line(tableX, writer.cursorY + headerHeight, tableX + tableWidth, writer.cursorY + headerHeight, { color: '#d7e0e7' });
        writer.cursorY += headerHeight;
    };

    drawHeader();

    rows.forEach((row) => {
        const activityLines = wrapPdfText(String(row.activity ?? ''), activityWidth - 10, 9, false);
        const rowHeight = Math.max(16, (activityLines.length * 10) + 4);

        if (writer.cursorY + rowHeight > pageBottom) {
            writer.newPage();
            writer.text(title, tableX, writer.cursorY, { size: 14, bold: true });
            writer.cursorY += titleHeight;
            writer.cursorY += tableYGap;
            drawHeader();
        }

        writer.rect(tableX, writer.cursorY, tableWidth, rowHeight, { fill: '#ffffff', stroke: '#d7e0e7' });
        writer.line(tableX, writer.cursorY, tableX, writer.cursorY + rowHeight, { color: '#d7e0e7' });
        writer.line(hoursX, writer.cursorY, hoursX, writer.cursorY + rowHeight, { color: '#d7e0e7' });
        writer.line(rateX, writer.cursorY, rateX, writer.cursorY + rowHeight, { color: '#d7e0e7' });
        writer.line(totalX, writer.cursorY, totalX, writer.cursorY + rowHeight, { color: '#d7e0e7' });
        writer.line(tableX + tableWidth, writer.cursorY, tableX + tableWidth, writer.cursorY + rowHeight, { color: '#d7e0e7' });

        drawPdfWrappedText(writer, row.activity ?? '', tableX + 5, writer.cursorY + 2, activityWidth - 10, { size: 9, lineHeight: 10 });
        writer.text(row.hours ?? '', hoursX + hoursWidth - 5, writer.cursorY + 2, { size: 9, align: 'right' });

        writer.cursorY += rowHeight + rowGap;
    });

    const totalHeight = 24;
    if (writer.cursorY + totalHeight > pageBottom) {
        writer.newPage();
        writer.text(title, tableX, writer.cursorY, { size: 14, bold: true });
        writer.cursorY += titleHeight;
        writer.cursorY += tableYGap;
        drawHeader();
    }

    writer.rect(tableX, writer.cursorY, tableWidth, totalHeight, { fill: '#eef6ff', stroke: '#d7e0e7' });
    writer.line(tableX, writer.cursorY, tableX, writer.cursorY + totalHeight, { color: '#d7e0e7' });
    writer.line(hoursX, writer.cursorY, hoursX, writer.cursorY + totalHeight, { color: '#d7e0e7' });
    writer.line(rateX, writer.cursorY, rateX, writer.cursorY + totalHeight, { color: '#d7e0e7' });
    writer.line(totalX, writer.cursorY, totalX, writer.cursorY + totalHeight, { color: '#d7e0e7' });
    writer.line(tableX + tableWidth, writer.cursorY, tableX + tableWidth, writer.cursorY + totalHeight, { color: '#d7e0e7' });
    writer.text('Total', tableX + 5, writer.cursorY + 6, { size: 9, bold: true });
    writer.text(total.totalHours ?? '', hoursX + hoursWidth - 5, writer.cursorY + 6, { size: 9, bold: true, align: 'right' });
    writer.text(total.rate ?? '', rateX + rateWidth - 5, writer.cursorY + 6, { size: 9, bold: true, align: 'right' });
    writer.text(total.totalAmount ?? '', totalX + totalWidth - 5, writer.cursorY + 6, { size: 9, bold: true, align: 'right' });
    writer.cursorY += totalHeight;
}

function drawInvoiceSection(writer, title, columns, rows, totalRow) {
    const titleHeight = 20;
    const headerHeight = 20;
    const rowGap = 2;
    const pageBottom = writer.contentBottom ?? 730;

    const drawContinuation = () => {
        writer.text(title, 36, writer.cursorY, { size: 14, bold: true });
        writer.cursorY += titleHeight;
        drawTableHeader(writer, columns);
    };

    if (writer.cursorY + titleHeight + headerHeight > pageBottom) {
        writer.newPage();
    }

    writer.text(title, 36, writer.cursorY, { size: 14, bold: true });
    writer.cursorY += titleHeight;
    drawTableHeader(writer, columns);

    rows.forEach((row) => {
        const rowHeight = measureRowHeight(row, columns);
        if (writer.cursorY + rowHeight > pageBottom) {
            writer.newPage();
            drawContinuation();
        }
        drawTableRow(writer, columns, row, rowHeight, false);
        writer.cursorY += rowHeight + rowGap;
    });

    const totalHeight = 22;
    if (writer.cursorY + totalHeight > pageBottom) {
        writer.newPage();
        drawContinuation();
    }
    drawTableRow(writer, columns, totalRow, totalHeight, true);
    writer.cursorY += totalHeight + rowGap;
}

function drawPdfWrappedText(writer, text, x, topY, maxWidth, options = {}) {
    const fontSize = options.size ?? 9;
    const bold = !!options.bold;
    const lineHeight = options.lineHeight ?? Math.round(fontSize + 3);
    const lines = wrapPdfText(String(text ?? ''), maxWidth, fontSize, bold);
    lines.forEach((line, index) => {
        writer.text(line, x, topY + (index * lineHeight), {
            size: fontSize,
            bold,
            align: 'left'
        });
    });
    return topY + (lines.length * lineHeight);
}

function drawTableHeader(writer, columns) {
    const height = 20;
    const startY = writer.cursorY;
    writer.rect(36, startY, 523, height, { fill: '#f8fafb', stroke: '#d7e0e7' });
    let x = 36;
    columns.forEach((column) => {
        writer.line(x, startY, x, startY + height, { color: '#d7e0e7' });
        const padding = 5;
        const textX = column.align === 'right' ? x + column.width - padding : x + padding;
        writer.text(column.label, textX, startY + 6, { size: 9, bold: true, align: column.align });
        x += column.width;
    });
    writer.line(559, startY, 559, startY + height, { color: '#d7e0e7' });
    writer.line(36, startY + height, 559, startY + height, { color: '#d7e0e7' });
    writer.cursorY += height;
}

function drawTableRow(writer, columns, cells, rowHeight, totalRow) {
    const startY = writer.cursorY;
    writer.rect(36, startY, 523, rowHeight, {
        fill: totalRow ? '#eef6ff' : '#ffffff',
        stroke: '#d7e0e7'
    });

    let x = 36;
    columns.forEach((column, index) => {
        const cell = cells[index] ?? { text: '', align: column.align };
        writer.line(x, startY, x, startY + rowHeight, { color: '#d7e0e7' });
        const padding = 5;
        const lines = wrapPdfTextWithBreaks(String(cell.text ?? ''), column.width - (padding * 2), 9, totalRow);
        const lineHeight = 11;
        const baseY = startY + 4;
        const textX = cell.align === 'right' ? x + column.width - padding : x + padding;
        lines.forEach((line, lineIndex) => {
            writer.text(line, textX, baseY + (lineIndex * lineHeight), {
                size: 9,
                bold: totalRow,
                align: cell.align
            });
        });
        x += column.width;
    });
    writer.line(559, startY, 559, startY + rowHeight, { color: '#d7e0e7' });
    writer.line(36, startY + rowHeight, 559, startY + rowHeight, { color: '#d7e0e7' });
}

function drawSummaryBox(writer, rows) {
    const boxWidth = 260;
    const boxHeight = 58;
    const pageBottom = writer.contentBottom ?? 730;
    if (writer.cursorY + boxHeight > pageBottom) {
        writer.newPage();
    }
    const x = 299;
    const y = writer.cursorY;
    writer.rect(x, y, boxWidth, boxHeight, { fill: '#f8fafb', stroke: '#d7e0e7' });
    rows.forEach((row, index) => {
        const rowY = y + 14 + (index * 14);
        writer.text(row.label, x + 12, rowY, { size: 9, bold: !!row.bold });
        writer.text(row.value, x + boxWidth - 12, rowY, { size: 9, bold: !!row.bold, align: 'right' });
    });
    writer.cursorY += boxHeight;
}

function measureRowHeight(row, columns) {
    let maxLines = 1;
    row.forEach((cell, index) => {
        const column = columns[index];
        if (!column) return;
        const lines = wrapPdfTextWithBreaks(String(cell?.text ?? ''), column.width - 10, 9, false);
        maxLines = Math.max(maxLines, lines.length);
    });
    return Math.max(20, (maxLines * 11) + 8);
}

function wrapPdfTextWithBreaks(text, maxWidth, fontSize, bold) {
    return String(text ?? '')
        .split('\n')
        .flatMap((part) => wrapPdfText(part, maxWidth, fontSize, bold));
}

function wrapPdfText(text, maxWidth, fontSize, bold) {
    const words = String(text ?? '').trim().split(/\s+/).filter(Boolean);
    if (words.length === 0) {
        return [''];
    }

    const lines = [];
    let current = '';
    for (const word of words) {
        const candidate = current ? `${current} ${word}` : word;
        if (measurePdfTextWidth(candidate, fontSize, bold) <= maxWidth) {
            current = candidate;
            continue;
        }

        if (current) {
            lines.push(current);
            current = word;
            if (measurePdfTextWidth(current, fontSize, bold) <= maxWidth) {
                continue;
            }
        }

        const fragments = breakPdfWord(word, maxWidth, fontSize, bold);
        fragments.slice(0, -1).forEach((fragment) => lines.push(fragment));
        current = fragments.at(-1) ?? '';
    }

    if (current) {
        lines.push(current);
    }
    return lines.length ? lines : [''];
}

function breakPdfWord(word, maxWidth, fontSize, bold) {
    const fragments = [];
    let current = '';
    for (const char of String(word ?? '')) {
        const candidate = current + char;
        if (measurePdfTextWidth(candidate, fontSize, bold) <= maxWidth || current === '') {
            current = candidate;
        } else {
            fragments.push(current);
            current = char;
        }
    }
    if (current) {
        fragments.push(current);
    }
    return fragments.length ? fragments : [''];
}

function measurePdfTextWidth(text, fontSize, bold) {
    const context = measurePdfTextWidth.context || (measurePdfTextWidth.context = document.createElement('canvas').getContext('2d'));
    if (!context) {
        return String(text ?? '').length * fontSize * 0.45;
    }
    context.font = `${bold ? '700' : '400'} ${fontSize * 2}px Arial, sans-serif`;
    return context.measureText(String(text ?? '')).width / 2;
}

class PdfWriter {
    constructor(logoImage, headerData = null, footerData = null) {
        this.logoImage = logoImage;
        this.headerData = headerData;
        this.footerData = footerData;
        this.pages = [];
        this.currentPage = null;
        this.scale = 2;
        this.pageWidth = 595;
        this.pageHeight = 842;
        this.contentTop = 198;
        this.contentBottom = 730;
    }

    startPage() {
        const canvas = document.createElement('canvas');
        canvas.width = this.pageWidth * this.scale;
        canvas.height = this.pageHeight * this.scale;
        const ctx = canvas.getContext('2d');
        if (!ctx) {
            throw new Error('Unable to create PDF canvas.');
        }
        ctx.setTransform(this.scale, 0, 0, this.scale, 0, 0);
        ctx.fillStyle = '#ffffff';
        ctx.fillRect(0, 0, this.pageWidth, this.pageHeight);
        ctx.textBaseline = 'top';
        ctx.imageSmoothingEnabled = true;

        const page = { canvas, ctx, cursorY: this.contentTop };
        this.pages.push(page);
        this.currentPage = page;
        if (this.headerData) {
            drawInvoiceHeader(this, this.headerData.customer, this.headerData.invoiceNumber, this.headerData.invoiceDate, {
                showBillTo: this.pages.length === 1
            });
        }
        return page;
    }

    newPage() {
        return this.startPage();
    }

    get cursorY() {
        return this.currentPage?.cursorY ?? 36;
    }

    set cursorY(value) {
        if (this.currentPage) {
            this.currentPage.cursorY = value;
        }
    }

    text(text, x, topY, options = {}) {
        const fontSize = options.size ?? 9;
        const bold = !!options.bold;
        const align = options.align ?? 'left';
        const ctx = this.currentPage.ctx;
        ctx.font = `${bold ? '700' : '400'} ${fontSize}px Arial, sans-serif`;
        ctx.fillStyle = '#13202b';
        ctx.textAlign = align;
        ctx.fillText(String(text ?? ''), x, topY);
    }

    line(x1, topY1, x2, topY2, options = {}) {
        const ctx = this.currentPage.ctx;
        const [r, g, b] = hexToRgb(options.color ?? '#d7e0e7');
        ctx.strokeStyle = `rgb(${Math.round(r * 255)}, ${Math.round(g * 255)}, ${Math.round(b * 255)})`;
        ctx.lineWidth = Number(options.width ?? 1);
        ctx.beginPath();
        ctx.moveTo(x1, topY1);
        ctx.lineTo(x2, topY2);
        ctx.stroke();
    }

    rect(x, topY, width, height, options = {}) {
        const ctx = this.currentPage.ctx;
        if (options.fill) {
            const [r, g, b] = hexToRgb(options.fill);
            ctx.fillStyle = `rgb(${Math.round(r * 255)}, ${Math.round(g * 255)}, ${Math.round(b * 255)})`;
            ctx.fillRect(x, topY, width, height);
        }
        if (options.stroke !== false) {
            const [r, g, b] = hexToRgb(options.stroke ?? '#d7e0e7');
            ctx.strokeStyle = `rgb(${Math.round(r * 255)}, ${Math.round(g * 255)}, ${Math.round(b * 255)})`;
            ctx.lineWidth = 1;
            ctx.strokeRect(x, topY, width, height);
        }
    }

    image(image, x, topY, width, height, sourceCrop = null) {
        if (!image) return;
        if (sourceCrop) {
            this.currentPage.ctx.drawImage(image, sourceCrop.x, sourceCrop.y, sourceCrop.width, sourceCrop.height, x, topY, width, height);
            return;
        }
        this.currentPage.ctx.drawImage(image, x, topY, width, height);
    }

    drawPageNumbers(totalPages) {
        this.pages.forEach((page, index) => {
            const ctx = page.ctx;
            ctx.save();
            ctx.font = '400 9px Arial, sans-serif';
            ctx.fillStyle = '#13202b';
            ctx.textAlign = 'right';
            ctx.fillText(`Page ${index + 1} of ${totalPages}`, 554, 92);
            ctx.restore();
        });
    }

    drawFooters() {
        if (!this.footerData) return;
        this.pages.forEach((page, index) => {
            this.currentPage = page;
            drawPdfFooter(this, this.footerData, index === this.pages.length - 1);
        });
    }

    buildPdfBytes() {
        this.drawFooters();
        let objectNumber = 1;
        const pageEntries = this.pages.map((page) => ({
            page,
            imageObj: objectNumber++,
            contentObj: objectNumber++,
            pageObj: objectNumber++
        }));
        const pagesObj = objectNumber++;
        const catalogObj = objectNumber++;
        const objects = new Array(catalogObj + 1).fill(null);

        pageEntries.forEach(({ page, imageObj, contentObj, pageObj }) => {
            const jpegDataUrl = page.canvas.toDataURL('image/jpeg', 0.96);
            const jpegBytes = dataUrlToBytes(jpegDataUrl);
            const imageHeader = latin1Bytes(`${imageObj} 0 obj\n<< /Type /XObject /Subtype /Image /Width ${page.canvas.width} /Height ${page.canvas.height} /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${jpegBytes.length} >>\nstream\n`);
            const imageFooter = latin1Bytes(`\nendstream\nendobj\n`);
            objects[imageObj] = concatUint8Arrays([imageHeader, jpegBytes, imageFooter]);

            const contentBytes = latin1Bytes(`q 595 0 0 842 0 0 cm /Im${imageObj} Do Q\n`);
            objects[contentObj] = concatUint8Arrays([
                latin1Bytes(`${contentObj} 0 obj\n<< /Length ${contentBytes.length} >>\nstream\n`),
                contentBytes,
                latin1Bytes('endstream\nendobj\n')
            ]);

            objects[pageObj] = latin1Bytes(`${pageObj} 0 obj\n<< /Type /Page /Parent ${pagesObj} 0 R /MediaBox [0 0 595 842] /Resources << /XObject << /Im${imageObj} ${imageObj} 0 R >> >> /Contents ${contentObj} 0 R >>\nendobj\n`);
        });

        const kids = pageEntries.map(({ pageObj }) => `${pageObj} 0 R`).join(' ');
        objects[pagesObj] = latin1Bytes(`${pagesObj} 0 obj\n<< /Type /Pages /Kids [ ${kids} ] /Count ${pageEntries.length} >>\nendobj\n`);
        objects[catalogObj] = latin1Bytes(`${catalogObj} 0 obj\n<< /Type /Catalog /Pages ${pagesObj} 0 R >>\nendobj\n`);

        const header = latin1Bytes('%PDF-1.4\n%\u00ff\u00ff\u00ff\u00ff\n');
        const parts = [header];
        const offsets = [0];
        let offset = header.length;
        for (let objectIndex = 1; objectIndex <= catalogObj; objectIndex += 1) {
            const bytes = objects[objectIndex];
            if (!bytes) continue;
            offsets[objectIndex] = offset;
            parts.push(bytes);
            offset += bytes.length;
        }

        const xrefStart = offset;
        let xref = `xref\n0 ${catalogObj + 1}\n0000000000 65535 f \n`;
        for (let i = 1; i <= catalogObj; i += 1) {
            xref += `${String(offsets[i] ?? 0).padStart(10, '0')} 00000 n \n`;
        }
        xref += `trailer\n<< /Size ${catalogObj + 1} /Root ${catalogObj} 0 R >>\nstartxref\n${xrefStart}\n%%EOF`;
        parts.push(latin1Bytes(xref));

        return concatUint8Arrays(parts);
    }
}

function drawPdfFooter(writer, footerData, isLastPage) {
    const lineY = Number(footerData.footerLineY ?? 736);
    const lineColor = String(footerData.footerLineColor ?? '#d7e0e7');
    const lineWidth = Number(footerData.footerLineWidth ?? 1);
    const textY = 744;
    const dueDateY = lineY - 34;
    const leftX = 36;
    const middleX = 298;
    const rightX = 559;

    if (isLastPage && footerData.dueDate) {
        writer.text(`Due date : ${footerData.dueDate}`, leftX, dueDateY, {
            size: 9,
            bold: true,
            align: 'left'
        });
    }

    writer.line(36, lineY, 559, lineY, { color: lineColor, width: lineWidth });

    const leftLines = [
        footerData.leftCompanyName ?? '',
        ...(footerData.leftAddressLines ?? [])
    ];
    const middleLines = footerData.middleInfoLines ?? [];
    const rightLines = footerData.rightBankLines ?? [];

    leftLines.forEach((line, index) => {
        writer.text(line, leftX, textY + (index * 12), {
            size: index === 0 ? 9 : 8,
            bold: index === 0,
            align: 'left'
        });
    });

    middleLines.forEach((line, index) => {
        writer.text(line, middleX, textY + (index * 12), {
            size: 8,
            align: 'center'
        });
    });

    rightLines.forEach((line, index) => {
        writer.text(line, rightX, textY + (index * 12), {
            size: index === 0 ? 9 : 8,
            bold: index === 0,
            align: 'right'
        });
    });
}

function latin1Bytes(text) {
    const bytes = new Uint8Array(String(text ?? '').length);
    for (let i = 0; i < String(text ?? '').length; i += 1) {
        bytes[i] = String(text ?? '').charCodeAt(i) & 0xff;
    }
    return bytes;
}

function concatUint8Arrays(parts) {
    const length = parts.reduce((sum, part) => sum + part.length, 0);
    const output = new Uint8Array(length);
    let offset = 0;
    parts.forEach((part) => {
        output.set(part, offset);
        offset += part.length;
    });
    return output;
}

function dataUrlToBytes(dataUrl) {
    const base64 = String(dataUrl ?? '').split(',')[1] || '';
    const binary = atob(base64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i += 1) {
        bytes[i] = binary.charCodeAt(i);
    }
    return bytes;
}

function hexToRgb(hex) {
    const normalized = String(hex ?? '#000000').replace('#', '');
    const value = normalized.length === 3
        ? normalized.split('').map((part) => part + part).join('')
        : normalized.padEnd(6, '0');
    const red = parseInt(value.slice(0, 2), 16) / 255;
    const green = parseInt(value.slice(2, 4), 16) / 255;
    const blue = parseInt(value.slice(4, 6), 16) / 255;
    return [red, green, blue];
}
