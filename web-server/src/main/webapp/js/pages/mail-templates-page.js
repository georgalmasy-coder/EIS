import {initMenu} from '../components/menu.js';
import {mountTopbar} from '../components/topbar.js';

const api = '/api/admin/mail-templates';
const el = Object.fromEntries([...document.querySelectorAll('[id]')].map(node => [node.id, node]));
let templates = [], current = null, csrfToken = '', dirty = false, saving = false, opening = false;
let savedRange = null, fieldTarget = 'body', ready = false, testSending = false, previewGeneration = 0;
let pendingImport = null, importing = false;


function message(node, text, error = false) { node.textContent = text; node.classList.toggle('error', error); }
async function request(url, options = {}) {
    const response = await fetch(url, {credentials:'same-origin', ...options});
    if (!response.headers.get('content-type')?.includes('application/json')) throw new Error('Unable to load data. Please check that you are signed in.');
    const data = await response.json();
    if (!response.ok) throw new Error(data.message || 'The action could not be completed.');
    return data;
}
function renderList() {
    const filter = el.templateSearch.value.toLocaleLowerCase();
    const rows = templates.filter(row => `${row.name} ${row.subject}`.toLocaleLowerCase().includes(filter));
    el.templateRows.replaceChildren();
    for (const row of rows) {
        const tr = document.createElement('tr'); tr.tabIndex = 0;
        for (const value of [row.name, row.subject || '(no subject)', new Date(row.updatedAt).toLocaleString('en-GB'), row.revision]) {
            const td = document.createElement('td'); td.textContent = value; tr.append(td);
        }
        const cell = document.createElement('td'), button = document.createElement('button');
        button.type = 'button'; button.textContent = 'Edit'; button.setAttribute('aria-label', `Edit ${row.name}`);
        button.addEventListener('click', () => openTemplate(row.key)); cell.append(button); tr.append(cell);
        tr.addEventListener('dblclick', () => openTemplate(row.key));
        tr.addEventListener('keydown', event => { if (event.target === tr && event.key === 'Enter') openTemplate(row.key); });
        el.templateRows.append(tr);
    }
    el.templateCount.textContent = `${rows.length} of ${templates.length} templates`;
}
async function load() {
    el.refreshTemplates.disabled = true; message(el.listMessage, 'Loading templates...');
    try { const data = await request(api); templates = data.templates; csrfToken = data.csrfToken; renderList(); message(el.listMessage, ''); }
    catch (error) { message(el.listMessage, error.message, true); }
    finally { el.refreshTemplates.disabled = false; }
}

// Mail HTML always runs in a sandbox without scripts. CSP also blocks external content.
function safeDocument(html) {
    const doc = new DOMParser().parseFromString(html, 'text/html');
    doc.querySelectorAll('script,iframe,object,embed,base,form,link,meta[http-equiv]').forEach(node => node.remove());
    for (const node of doc.querySelectorAll('*')) {
        for (const attr of [...node.attributes]) {
            if (/^on/i.test(attr.name) || ['srcdoc','contenteditable','autofocus'].includes(attr.name)) node.removeAttribute(attr.name);
            if (['href','src','xlink:href'].includes(attr.name) && /^\s*(javascript|vbscript):/i.test(attr.value)) node.removeAttribute(attr.name);
        }
    }
    const csp = doc.createElement('meta'); csp.httpEquiv = 'Content-Security-Policy';
    csp.content = "default-src 'none'; style-src 'unsafe-inline'; img-src data:; font-src 'none'; form-action 'none'; base-uri 'none'";
    doc.head.prepend(csp);
    return doc;
}
function serialize(doc) { return '<!doctype html>\n' + doc.documentElement.outerHTML; }

async function openTemplate(key) {
    if (opening || el.templateDialog.open) return;
    opening = true;
    try {
        current = await request(`${api}?key=${encodeURIComponent(key)}`);
        dirty = false; ready = false; savedRange = null; fieldTarget = 'body';
        el.dialogTitle.textContent = current.name;
        el.templateSubject.value = current.subject;
        message(el.editMessage, current.sourceFileName === 'system-notification.mail' && !current.subject ? 'This template was empty. Enter a subject and content before saving.' : '');
        el.templatePlaceholder.replaceChildren(new Option('Choose field...', ''));
        for (const token of JSON.parse(current.placeholdersJson)) el.templatePlaceholder.append(new Option(token, token));
        el.saveTemplate.disabled = true; el.showTemplate.disabled = true;
        el.templateEditor.srcdoc = serialize(safeDocument(current.bodyHtml));
        el.templateDialog.showModal();
    } catch (error) { message(el.listMessage, error.message, true); }
    finally { opening = false; }
}

el.templateEditor.addEventListener('load', () => {
    if (!current || !el.templateDialog.open) return;
    const doc = el.templateEditor.contentDocument;
    doc.body.contentEditable = 'true';
    doc.body.setAttribute('aria-label', 'Email content');
    doc.body.addEventListener('input', () => { dirty = true; });
    doc.addEventListener('selectionchange', () => {
        const selection = doc.getSelection();
        if (selection?.rangeCount && doc.body.contains(selection.anchorNode)) { savedRange = selection.getRangeAt(0).cloneRange(); fieldTarget = 'body'; }
    });
    doc.addEventListener('click', event => { if (event.target.closest?.('a')) event.preventDefault(); });
    doc.body.addEventListener('paste', event => {
        // Paste text without importing Word/browser markup or dividing placeholders.
        event.preventDefault(); doc.execCommand('insertText', false, event.clipboardData.getData('text/plain'));
    });
    ready = true; el.saveTemplate.disabled = false; el.showTemplate.disabled = false;
});

function editorCommand(command, value = null) {
    if (!ready || saving) return;
    const doc = el.templateEditor.contentDocument;
    doc.body.focus();
    if (savedRange) { const selection = doc.getSelection(); selection.removeAllRanges(); selection.addRange(savedRange); }
    doc.execCommand(command, false, value); dirty = true;
}
function draftHtml() {
    const clone = el.templateEditor.contentDocument.documentElement.cloneNode(true);
    clone.querySelector('body').removeAttribute('contenteditable');
    clone.querySelector('body').removeAttribute('aria-label');
    clone.querySelectorAll('meta[http-equiv="Content-Security-Policy"]').forEach(node => node.remove());
    return '<!doctype html>\n' + clone.outerHTML;
}
function draft(action, extra = {}) {
    return {key:current.key,subject:el.templateSubject.value,bodyHtml:draftHtml(),revision:current.revision,action,...extra};
}
function postDraft(data) {
    return request(api, {method:'POST', headers:{'Content-Type':'application/json','X-CSRF-Token':csrfToken},body:JSON.stringify(data)});
}
async function showPreview() {
    if (!ready) return;
    const generation = ++previewGeneration;
    const data = draft('preview');
    el.templatePreview.hidden = true;
    el.previewSubject.textContent = '';
    message(el.previewMessage, 'Loading preview...');
    el.previewDialog.showModal();
    try {
        const rendered = await postDraft(data);
        if (!el.previewDialog.open || generation !== previewGeneration) return;
        const doc = safeDocument(rendered.bodyHtml);
        doc.querySelectorAll('a').forEach(node => node.removeAttribute('href'));
        doc.body.dataset.previewReady = 'true';
        el.previewSubject.textContent = rendered.subject;
        // Load the frame after the dialog is visible and verify actual rendered content.
        el.templatePreview.onload = () => {
            const body = el.templatePreview.contentDocument?.body;
            if (body?.dataset.previewReady !== 'true') return;
            if (!body.textContent.trim()) { message(el.previewMessage, 'The preview has no content.', true); return; }
            el.templatePreview.hidden = false;
            message(el.previewMessage, '');
        };
        el.templatePreview.srcdoc = serialize(doc);
        setTimeout(() => {
            if (generation === previewGeneration && el.previewDialog.open && el.templatePreview.hidden) {
                message(el.previewMessage, 'The browser could not display the preview. Close it and try again.', true);
            }
        }, 8000);
    } catch (error) { message(el.previewMessage, error.message, true); }
}
function openTest() {
    if (!ready || saving || testSending) return;
    el.testRecipient.value = ''; message(el.testMessage, ''); el.sendTest.disabled = false;
    el.testDialog.showModal(); el.testRecipient.focus();
}
async function sendTest(event) {
    event.preventDefault();
    if (testSending || !ready || !el.testForm.reportValidity()) return;
    testSending = true; el.sendTest.disabled = true; el.testRecipient.disabled = true;
    message(el.testMessage, 'Queueing test email...');
    try {
        const result = await postDraft(draft('test', {recipientEmail:el.testRecipient.value.trim()}));
        message(el.testMessage, result.message + ' Delivery is handled by the integration server.');
    } catch (error) { message(el.testMessage, error.message, true); }
    finally { testSending = false; el.sendTest.disabled = false; el.testRecipient.disabled = false; }
}
async function save() {
    if (!ready || saving) return;
    saving = true; el.saveTemplate.disabled = true;
    el.templateSubject.disabled = true; el.templateEditor.contentDocument.body.contentEditable = 'false';
    message(el.editMessage, 'Saving...');
    try {
        const bodyHtml = draftHtml();
        const updated = await request(api, {method:'POST', headers:{'Content-Type':'application/json','X-CSRF-Token':csrfToken},
            body:JSON.stringify({key:current.key,subject:el.templateSubject.value,bodyHtml,revision:current.revision})});
        current = updated; dirty = false;
        templates = templates.map(row => row.key === current.key ? {...current,bodyHtml:null} : row); renderList();
        message(el.editMessage, 'Template saved.');
    } catch (error) { message(el.editMessage, error.message, true); }
    finally { saving = false; el.saveTemplate.disabled = false; el.templateSubject.disabled = false; el.templateEditor.contentDocument.body.contentEditable = 'true'; }
}
function closeTemplate() {
    if (saving) return;
    if (dirty && !window.confirm('Close without saving your changes?')) return;
    el.templateDialog.close(); current = null; ready = false; dirty = false;
}
el.templateDialog.addEventListener('cancel', event => { event.preventDefault(); closeTemplate(); });
for (const id of ['closeTemplate','cancelTemplate']) el[id].addEventListener('click', closeTemplate);
for (const id of ['closePreview','backToTemplate']) el[id].addEventListener('click', () => el.previewDialog.close());
el.templateSubject.addEventListener('input', () => { dirty = true; });
el.templateSubject.addEventListener('focus', () => { fieldTarget = 'subject'; });
document.querySelectorAll('[data-command]').forEach(button => {
    button.addEventListener('mousedown', event => event.preventDefault());
    button.addEventListener('click', () => editorCommand(button.dataset.command));
});
el.textFormat.addEventListener('change', () => editorCommand('formatBlock', el.textFormat.value));
el.templatePlaceholder.addEventListener('change', () => {
    if (saving || !ready) return;
    const token = el.templatePlaceholder.value; if (!token) return;
    if (fieldTarget === 'subject') {
        const field = el.templateSubject;
        field.setRangeText(`{{${token}}}`, field.selectionStart, field.selectionEnd, 'end'); field.focus(); dirty = true;
    } else editorCommand('insertText', `{{${token}}}`);
    el.templatePlaceholder.value = '';
});
el.editLink.addEventListener('click', () => {
    if (!ready || saving) return;
    let node = savedRange?.startContainer;
    if (node?.nodeType === Node.TEXT_NODE) node = node.parentElement;
    el.linkUrl.value = node?.closest?.('a')?.getAttribute('href') ?? '';
    message(el.linkMessage, ''); el.linkDialog.showModal();
});
el.applyLink.addEventListener('click', () => {
    const value = el.linkUrl.value.trim();
    const tokens = JSON.parse(current.placeholdersJson);
    const match = /^\{\{(\w+)\}\}$/.exec(value);
    if (!/^https?:\/\//i.test(value) && !/^mailto:[^\s]+$/i.test(value) && !(match && tokens.includes(match[1]) && /Link$/.test(match[1]))) {
        message(el.linkMessage, 'Enter an https:// address, a mailto: address or an available link field.', true); return;
    }
    el.linkDialog.close(); editorCommand('createLink', value);
});
el.cancelLink.addEventListener('click', () => el.linkDialog.close());
el.showTemplate.addEventListener('click', showPreview);
el.testTemplate.addEventListener('click', openTest);
el.testPreview.addEventListener('click', openTest);
el.testForm.addEventListener('submit', sendTest);
el.cancelTest.addEventListener('click', () => { if (!testSending) el.testDialog.close(); });
el.testDialog.addEventListener('cancel', event => { if (testSending) event.preventDefault(); });
el.saveTemplate.addEventListener('click', save);
el.refreshTemplates.addEventListener('click', load);
el.exportTemplates.addEventListener('click', async () => {
    el.exportTemplates.disabled = true;
    try {
        const bundle = await request(`${api}?action=export`);
        const url = URL.createObjectURL(new Blob([JSON.stringify(bundle, null, 2)], {type:'application/json'}));
        const link = document.createElement('a'); link.href = url; link.download = 'eis-mail-templates.json';
        link.click(); setTimeout(() => URL.revokeObjectURL(url), 60000);
        message(el.listMessage, `${bundle.templates.length} templates exported.`);
    } catch (error) { message(el.listMessage, error.message, true); }
    finally { el.exportTemplates.disabled = false; }
});
el.importTemplates.addEventListener('click', () => { el.templateImportFile.value = ''; el.templateImportFile.click(); });
el.templateImportFile.addEventListener('change', async () => {
    const file = el.templateImportFile.files[0]; if (!file) return;
    pendingImport = null;
    try {
        if (file.size > 20_000_000) throw new Error('The import file is too large (maximum 20 MB).');
        const bundle = JSON.parse(await file.text());
        if (bundle.format !== 'eis-mail-templates' || bundle.version !== 1 || !Array.isArray(bundle.templates) || !bundle.templates.length) throw new Error('Invalid or unsupported mail template export file.');
        pendingImport = bundle;
        el.importSummary.textContent = `${file.name}: ${bundle.templates.length} templates.`;
        message(el.importMessage, ''); el.applyImport.disabled = false; el.importDialog.showModal();
    } catch (error) { message(el.listMessage, error instanceof SyntaxError ? 'The file is not valid JSON.' : error.message, true); }
});
el.applyImport.addEventListener('click', async () => {
    if (!pendingImport || importing) return;
    importing = true; el.applyImport.disabled = true; el.cancelImport.disabled = true; message(el.importMessage, 'Importing templates...');
    try {
        const result = await request(`${api}?action=import`, {method:'POST',headers:{'Content-Type':'application/json','X-CSRF-Token':csrfToken},body:JSON.stringify(pendingImport)});
        el.importDialog.close(); pendingImport = null; await load(); message(el.listMessage, result.message);
    } catch (error) { message(el.importMessage, error.message, true); }
    finally { importing = false; el.applyImport.disabled = false; el.cancelImport.disabled = false; }
});
el.cancelImport.addEventListener('click', () => { if (!importing) { el.importDialog.close(); pendingImport = null; } });
el.importDialog.addEventListener('cancel', event => { if (importing) event.preventDefault(); else pendingImport = null; });
el.templateSearch.addEventListener('input', renderList);
window.addEventListener('beforeunload', event => { if (dirty) { event.preventDefault(); event.returnValue = ''; } });
initMenu(); mountTopbar(document); load();
