import assert from "node:assert/strict";
import {existsSync} from "node:fs";
import {readFile, writeFile, mkdtemp, rm} from "node:fs/promises";
import {tmpdir} from "node:os";
import {join} from "node:path";
import {pathToFileURL} from "node:url";
import {spawn} from "node:child_process";
import {test} from "node:test";
import vm from "node:vm";

const chrome = [process.env.CHROME_BINARY,
    "C:/Program Files/Google/Chrome/Application/chrome.exe",
    "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe",
    "/usr/bin/google-chrome", "/usr/bin/chromium"].find(file => file && existsSync(file));
const pause = ms => new Promise(resolve => setTimeout(resolve, ms));
const source = name => readFile(new URL(`../../main/webapp/${name}`, import.meta.url), "utf8");
const plain = code => code.replace(/^import[^\n]+\r?\n/gm, "").replaceAll("export function", "function");

test("matrix download supports both existing table callers and model snapshots", async () => {
    const requests = [];
    const sandbox = {fetch: async (url, options) => {
        requests.push({url, body: JSON.parse(options.body)});
        return {ok: false};
    }};
    vm.createContext(sandbox);
    vm.runInContext(plain(await source("js/components/interface-xlsx-export.js"))
        .replaceAll("export async function", "async function"), sandbox);
    sandbox.snapshotInterfaceMatrix = (table, title, project) => ({table, title, project});
    const options = {endpoint: "/export", title: "Title", project: "Project"};
    await assert.rejects(sandbox.downloadInterfaceXlsx({...options, table: "legacy table"}), /Could not export/);
    assert.deepEqual(requests[0], {url: "/export?cmd=export", body: {
        table: "legacy table", title: "Title", project: "Project"
    }});
    sandbox.snapshotInterfaceMatrix = () => {throw new Error("must not inspect the virtual DOM");};
    const snapshot = {title: "Model", cells: [{row: 100, column: 200}]};
    await assert.rejects(sandbox.downloadInterfaceXlsx({...options, snapshot}), /Could not export/);
    assert.deepEqual(requests[1].body, snapshot);
});

test("traceability: sparse parsing, two-axis viewport, interactions and full-model export", {
    skip: !chrome && "Set CHROME_BINARY to run the browser regression test", timeout: 30000
}, async () => {
    const [page, view, xml, cssHelpers, css] = await Promise.all([
        source("js/pages/basistraceability-page.js"), source("js/components/traceability-matrix-view.js"),
        source("js/core/xml.js"), source("js/core/css.js"), source("css/basistraceability.css")
    ]);
    const functions = page.slice(page.indexOf("function parseTraceabilityMatrixDocument("), page.indexOf("function applyTopPanel("))
        + page.slice(page.indexOf("async function snapshotTraceabilityMatrix("), page.indexOf("function openSystemRequirementEditPage("));
    const directory = await mkdtemp(join(tmpdir(), "traceability-regression-"));
    const file = join(directory, "test.html");
    await writeFile(file, `<!doctype html><html><head><style>
        :root {--app-table-header-background:#ddd;--app-text-primary:#111;--app-border-default:#aaa;}
        ${css}
        #scroll {width:1000px;height:500px;flex:none;max-height:none;}
        body {margin:0;}
        </style></head><body><span id="projectName">Project</span>
        <div id="scroll" class="traceability-table-scroll"><div id="grid" role="grid"></div></div>
        <pre id="result"></pre><script>
        ${plain(xml)} ${plain(cssHelpers)} ${plain(view)} ${functions}
        const FALLBACK_STYLE_ID = "normal";
        const STATUS_ICONS = {yellow: {symbol:"?",color:"#996600"}};
        const state = {viewMode:"icons"};
        const checks = [];
        function check(condition, message) {if (!condition) throw Error(message); checks.push(message);}
        const root = document.getElementById("grid"), scroll = document.getElementById("scroll");
        let opened, context, rowEdit, columnEdit;
        const matrixView = createTraceabilityMatrixView({root, scroll,
            createIcon: style => {
                if (style !== "yellow") return null;
                const icon = document.createElement("span");
                icon.className = "traceability-status-icon";
                icon.setAttribute("aria-label", "Possible relations");
                return icon;
            },
            cellTooltip: (row, column) => row.id + ":" + column.id,
            rowTooltip: row => row.id, columnTooltip: (group, column) => column.id,
            onCellDoubleClick: (row, column) => opened = [row.id, column.id],
            onCellContextMenu: (event, row, column, cell) => context = [row.id, column.id, cell.style],
            onRowEdit: row => rowEdit = row.id, onColumnEdit: column => columnEdit = column.id
        });
        (async () => {try {
            const parse = text => parseCells(new DOMParser().parseFromString(text, "application/xml").documentElement);
            const sparse = parse('<traceabilityMatrix><cells><cell row="2" col="4" style="normal" value=""/>'
                + '<cell row="2" col="8" style="yellow" value=""/><cell row="-1" col="4" style="yellow"/>'
                + '</cells><cellRows><cellRow row="6"><cell col="8" value="text"/></cellRow></cellRows></traceabilityMatrix>');
            check(sparse.size === 2 && sparse.get(2).size === 1 && sparse.get(6).get(8).value === "text", "flat/grouped sparse cells and invalid indices");
            const overridden = parse('<traceabilityMatrix><cells><cell row="2" col="4" style="yellow"/></cells>'
                + '<cellRows><cellRow row="2"><cell col="4" style="normal"/></cellRow></cellRows></traceabilityMatrix>');
            check(overridden.size === 0, "later default clears earlier override");
            const custom = parse('<traceabilityMatrix defaultCellStyle="yellow" defaultCellValue="default"><cells>'
                + '<cell row="2" col="4"/><cell row="2" col="8" value=""/></cells></traceabilityMatrix>');
            check(custom.get(2).size === 1 && custom.get(2).get(8).value === "", "custom defaults and explicit empty override");
            const matrix = {
                rows: Array.from({length:1885}, (_, i) => ({index:i*2,id:"R"+i,label:"Row "+i,style:"normal"})),
                columns: Array.from({length:1900}, (_, i) => ({index:i*3,id:"C"+i,label:"Column "+i,style:"normal"})),
                cellsByRow: new Map([[0,new Map([[0,{rowIndex:0,columnIndex:0,style:"yellow",value:""}]])]]),
                defaultCellStyle:"normal", defaultCellValue:"", rowGroupLabel:"Stakeholder", columnGroupLabel:"System"
            };
            matrixView.setMatrix(matrix, "icons");
            check(root.querySelectorAll('.traceability-cell').length < 1000, "full-size matrix has bounded DOM");
            check(root.querySelector('.traceability-status-icon'), "icon rendering");
            const cell = root.querySelector('.traceability-cell');
            cell.dispatchEvent(new MouseEvent('dblclick', {bubbles:true}));
            check(opened.join() === "R0,C0", "delegated double-click");
            cell.dispatchEvent(new MouseEvent('contextmenu', {bubbles:true,cancelable:true}));
            check(context.join() === "R0,C0,yellow", "delegated context menu");
            cell.dispatchEvent(new MouseEvent('mouseover', {bubbles:true}));
            check(cell.title === "R0:C0\\nPossible relations", "lazy tooltip includes status");
            root.querySelector('.traceability-row-header').dispatchEvent(new MouseEvent('dblclick', {bubbles:true}));
            root.querySelector('.traceability-column-header').dispatchEvent(new MouseEvent('dblclick', {bubbles:true}));
            check(rowEdit === "R0" && columnEdit === "C0", "delegated header edits");
            scroll.scrollTop = scroll.scrollHeight; scroll.scrollLeft = scroll.scrollWidth;
            scroll.dispatchEvent(new Event('scroll'));
            await new Promise(resolve => setTimeout(resolve, 100));
            const last = root.querySelector('[data-row-position="1884"][data-column-position="1899"]');
            check(last, "scroll reaches last row and column");
            last.dispatchEvent(new MouseEvent('dblclick', {bubbles:true}));
            check(opened.join() === "R1884,C1899", "non-contiguous XML indices map correctly");
            const bounds = scroll.getBoundingClientRect(), corner = root.querySelector('.traceability-virtual-corner').getBoundingClientRect();
            check(Math.abs(corner.left-bounds.left) < 2 && Math.abs(corner.top-bounds.top) < 2, "corner stays pinned");
            check(Math.abs(root.querySelector('.traceability-row-header').getBoundingClientRect().left-bounds.left-44) < 2, "row headers stay pinned");
            check(Math.abs(root.querySelector('.traceability-column-header').getBoundingClientRect().top-bounds.top) < 2, "column headers stay pinned");
            const lastBounds = last.getBoundingClientRect();
            check(lastBounds.right <= bounds.right+1 && lastBounds.bottom <= bounds.bottom+1, "scroll extent includes full last cell");
            const left = scroll.scrollLeft, top = scroll.scrollTop;
            matrixView.setMatrix(matrix, "colors");
            check(scroll.scrollLeft === left && scroll.scrollTop === top && !root.querySelector('.traceability-status-icon'), "mode switch preserves scroll and removes icons");
            scroll.scrollLeft=0; scroll.scrollTop=0; matrixView.setMatrix(matrix, "icons");
            const previousCount = root.querySelectorAll('.traceability-cell').length;
            scroll.style.width="700px"; scroll.style.height="350px";
            await new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)));
            check(root.querySelectorAll('.traceability-cell').length < previousCount, "resize recalculates viewport");
            const small = {...matrix, rows:matrix.rows.slice(0,2), columns:matrix.columns.slice(0,30)};
            matrixView.setMatrix(small, "icons");
            const snapshot = await snapshotTraceabilityMatrix(small);
            check(snapshot.widths.length === 32 && snapshot.heights.length === 3, "export dimensions use complete model");
            check(snapshot.cells.filter(cell => cell.row>0 && cell.column>=2).length === 60, "export includes offscreen cells");
            check(snapshot.cells.some(cell => cell.column===31 && cell.row===2), "last offscreen cell exported");
            check(snapshot.cells.some(cell => cell.text==="?" && cell.row>0), "export preserves status symbols");
            const exportButton = document.createElement("button");
            exportButton.id = "traceabilityExcelButton";
            document.body.appendChild(exportButton);
            const boundary = {...matrix, rows:matrix.rows.slice(0,150), columns:matrix.columns.slice(0,150)};
            updateTraceabilityExportButton(boundary);
            check(!exportButton.disabled, "export allowed at exactly 22500 data cells");
            updateTraceabilityExportButton({...boundary, columns:matrix.columns.slice(0,151)});
            check(!exportButton.disabled, "export remains available above the confirmation threshold");
            check(!canExportTraceabilityMatrix({rows:{length:1},columns:{length:22501}}), "exact cell-count limit");
            const getSizes = matrixView.getSizes;
            matrixView.getSizes = () => {throw Error("export layout must not be read");};
            let rejected=false;
            try {await snapshotTraceabilityMatrix({...matrix,rows:{length:1000},columns:{length:1000}});} catch(error) {rejected=error.message.includes("1,000,000");}
            check(rejected, "oversized export stops before layout, DOM or snapshot work");
            matrixView.getSizes = getSizes;
            updateTraceabilityExportButton({...matrix, rows:[]});
            check(exportButton.disabled, "empty matrix disables export");
            matrixView.setMatrix({...matrix, rows:[], columns:[]}, "colors");
            check(!root.querySelector('.traceability-cell'), "empty matrix clears viewport");
            document.getElementById('result').textContent=JSON.stringify({passed:true,checks});
        } catch(error) {document.getElementById('result').textContent=JSON.stringify({passed:false,error:error.stack,checks});}})();
        </script></body></html>`);
    const browser = spawn(chrome, ["--headless", "--disable-gpu", "--no-first-run", "--remote-debugging-port=0",
        `--user-data-dir=${join(directory, "profile")}`, "--window-size=1200,800", pathToFileURL(file).href],
    {stdio: "ignore", windowsHide: true});
    let socket;
    let call;
    try {
        let port;
        for (let i = 0; i < 100; i++) {
            try {port = (await readFile(join(directory, "profile", "DevToolsActivePort"), "utf8")).split("\n")[0]; break;}
            catch {await pause(50);}
        }
        assert.ok(port, "browser exposes debugging port");
        const pages = await (await fetch(`http://127.0.0.1:${port}/json`)).json();
        socket = new WebSocket(pages.find(page => page.type === "page").webSocketDebuggerUrl);
        await new Promise(resolve => socket.addEventListener("open", resolve, {once: true}));
        let id = 0;
        const pending = new Map();
        socket.addEventListener("message", event => {
            const message = JSON.parse(event.data);
            if (message.id) {pending.get(message.id)?.(message); pending.delete(message.id);}
        });
        call = (method, params = {}) => new Promise(resolve => {
            const nextId = ++id;
            pending.set(nextId, resolve);
            socket.send(JSON.stringify({id: nextId, method, params}));
        });
        let result;
        for (let i = 0; i < 200; i++) {
            const response = await call("Runtime.evaluate", {
                expression: "document.getElementById('result')?.textContent", returnByValue: true
            });
            if (response.result?.result?.value) {result = JSON.parse(response.result.result.value); break;}
            await pause(50);
        }
        assert.ok(result, "browser test completed");
        assert.equal(result.passed, true, JSON.stringify(result, null, 2));
    } finally {
        if (socket?.readyState === WebSocket.OPEN) {
            socket.send(JSON.stringify({id: 999999, method: "Browser.close"}));
            socket.close();
        } else {
            browser.kill();
        }
        for (let i = 0; i < 30 && browser.exitCode === null; i++) await pause(50);
        if (browser.exitCode === null) browser.kill();
        await rm(directory, {recursive: true, force: true, maxRetries: 10, retryDelay: 100});
    }
});
