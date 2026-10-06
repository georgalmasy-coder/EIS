import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import test from 'node:test';
import vm from 'node:vm';

const source = readFileSync(new URL('../../main/webapp/assets/js/app.js', import.meta.url), 'utf8');

function harness(reports) {
    const buttons = [-1, 1].map((shift) => ({
        dataset: { reportShift: String(shift) },
        addEventListener(type, callback) { this.click = callback; }
    }));
    const elements = Object.fromEntries(['promptDialog', 'promptTitle', 'promptSubtitle', 'promptBody', 'promptClose']
        .map((id) => [id, { innerHTML: '', textContent: '', querySelectorAll: () => buttons }]));
    elements.promptDialog.showModal = () => { elements.promptDialog.open = true; };
    elements.promptDialog.close = () => { elements.promptDialog.open = false; };
    const context = vm.createContext({
        document: { addEventListener() {}, getElementById: (id) => elements[id] },
        Intl, Date, console
    });
    vm.runInContext(source, context);
    context.report = reports;
    context.requests = [];
    vm.runInContext(`
        state.year = 2024;
        fetchJson = async (url) => { requests.push(url); return report; };
        safeAsync = (callback) => callback;
    `, context);
    return { context, elements, buttons, run: (code) => vm.runInContext(code, context) };
}

test('VAT dialog shows quarterly amounts, refunds and annual totals', async () => {
    const h = harness({ rows: [
        { quarter: 1, inputVat: 100, outputVat: 25, payableVat: -75 },
        { quarter: 2, inputVat: 0, outputVat: 0, payableVat: 0 },
        { quarter: 3, inputVat: 10, outputVat: 200, payableVat: 190 },
        { quarter: 4, inputVat: 0, outputVat: 0, payableVat: 0 }
    ] });
    await h.run("accountingReportDialog('vat')");
    assert.equal(h.elements.promptTitle.textContent, 'VAT return');
    assert.equal(h.elements.promptDialog.open, true);
    assert.equal(h.context.requests[0], 'api/accounting/vat?year=2024');
    const html = h.elements.promptBody.innerHTML;
    for (let quarter = 1; quarter <= 4; quarter++) assert.ok(html.includes(`Q${quarter} 2024`));
    assert.ok(html.includes(h.run('money(-75)')));
    for (const amount of [110, 225, 115]) assert.ok(html.includes(h.run(`money(${amount})`)));
    assert.ok(html.includes('excluding VAT payments'));
});

test('bank dialog displays opening balance and supplied month-end balances for all months', async () => {
    const h = harness({ openingBalance: 1000, rows: Array.from({ length: 12 }, (_, i) => ({
        month: i + 1,
        closingDate: new Date(Date.UTC(2024, i + 1, 0)).toISOString().slice(0, 10),
        movement: i === 0 ? -1500 : 0,
        closingBalance: -500
    })) });
    await h.run("accountingReportDialog('bank')");
    assert.equal(h.context.requests[0], 'api/accounting/bank?year=2024');
    const html = h.elements.promptBody.innerHTML;
    assert.ok(html.includes('2024-02-29'));
    assert.ok(html.includes('2024-12-31'));
    assert.ok(html.includes(h.run('money(1000)')));
    assert.equal(html.split(h.run('money(-500)')).length - 1, 12);
    assert.ok(html.includes('Month-end balance'));
    assert.ok(html.includes('February 2024'));
    assert.equal(h.elements.promptTitle.textContent, 'Bank reconciliation');
    h.elements.promptClose.onclick();
    assert.equal(h.elements.promptDialog.open, false);
});

test('year navigation reloads the report and keeps VAT and bank years independent', async () => {
    const h = harness({ openingBalance: 0, rows: [] });
    await h.run("accountingReportDialog('vat')");
    await h.buttons[0].click();
    assert.equal(h.context.requests.at(-1), 'api/accounting/vat?year=2023');
    assert.equal(h.elements.promptSubtitle.textContent, '2023');
    await h.run("accountingReportDialog('bank')");
    assert.equal(h.context.requests.at(-1), 'api/accounting/bank?year=2024');
    await h.buttons[1].click();
    assert.equal(h.context.requests.at(-1), 'api/accounting/bank?year=2025');
    await h.run("accountingReportDialog('vat')");
    assert.equal(h.context.requests.at(-1), 'api/accounting/vat?year=2023');
});
