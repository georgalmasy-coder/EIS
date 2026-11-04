const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.resolve(__dirname, '../../main/webapp/js/components/traceability-block-loader.js'), 'utf8').replace('export function', 'function');
const sandbox = vm.createContext({});
vm.runInContext(source, sandbox);
const create = sandbox.createTraceabilityBlockLoader;
const tick = () => new Promise(resolve => setImmediate(resolve));
const matrix = (rows = 96, columns = 96) => ({snapshotId:'s1', blockSize:32,
    rows:Array(rows), columns:Array(columns), cellsByRow:new Map(), loadedBlocks:new Set()});
function reply(m, r, c, cells = []) {
    return {snapshotId:m.snapshotId, rowStart:r, columnStart:c,
        rowEnd:Math.min(r+32,m.rows.length), columnEnd:Math.min(c+32,m.columns.length), cells};
}
const range = {rowStart:0,rowEnd:12,colStart:0,colEnd:8};

test('load sparse and empty blocks once; export fills every block', async () => {
    const m=matrix(65,67), calls=[], progress=[];
    const loader=create({matrix:m,fetchBlock:async (r,c)=>{calls.push(`${r}:${c}`);return reply(m,r,c,r===0&&c===0?[{rowIndex:1,columnIndex:2,style:'green',value:'X'}]:[]);},
        onUpdate(){},onError(error){throw error;},onExpired(){throw Error('expired');}});
    loader.requestRange(range); await tick();
    assert.equal(m.loadedBlocks.size,4);
    assert.equal(m.cellsByRow.get(1).get(2).style,'green');
    loader.requestRange(range);await tick();assert.equal(calls.length,4);
    await loader.ensureAll((done,total)=>progress.push([done,total]));
    assert.equal(m.loadedBlocks.size,9);assert.equal(calls.length,9);
    assert.deepEqual(progress.at(-1),[9,9]);
});

test('viewport work is prioritized, deduplicated and limited to two concurrent calls', async () => {
    const m=matrix(320,320), pending=[], calls=[];
    const loader=create({matrix:m,fetchBlock:(r,c)=>new Promise(resolve=>{calls.push(`${r}:${c}`);pending.push(()=>resolve(reply(m,r,c)));}),
        onUpdate(){},onError(){},onExpired(){}});
    loader.requestRange(range);loader.requestRange(range);
    assert.equal(calls.length,2);
    loader.requestRange({rowStart:288,rowEnd:300,colStart:288,colEnd:300});
    pending.shift()();await tick();
    assert.equal(calls[2],'288:288');
    pending.shift()();await tick();
    assert.equal(calls.length,4);
    loader.dispose();while(pending.length)pending.shift()();await tick();
    assert.equal(calls.length,4);
});

test('late replies after disposal never change cells or repaint', async () => {
    const m=matrix(1,1);let resolve,updates=0;
    const loader=create({matrix:m,fetchBlock:()=>new Promise(ok=>resolve=ok),onUpdate(){updates++;},onError(){},onExpired(){}});
    loader.requestRange(range);loader.dispose();resolve(reply(m,0,0));await tick();
    assert.equal(m.loadedBlocks.size,0);assert.equal(updates,0);
});

test('snapshot mismatch remains unloaded; failed requests do not loop on refresh', async () => {
    const m=matrix(1,1);let calls=0,errors=0;
    const loader=create({matrix:m,fetchBlock:async()=>{calls++;return {...reply(m,0,0),snapshotId:'other'};},onUpdate(){},onError(){errors++;},onExpired(){}});
    loader.requestRange(range);await tick();loader.requestRange(range);await tick();
    assert.equal(m.loadedBlocks.size,0);assert.equal(errors,1);assert.equal(calls,1);
    await assert.rejects(loader.ensureAll(),/Invalid traceability block response/);
});

test('expired snapshots trigger one restart and stop the old queue', async () => {
    const m=matrix();let expired=0;
    const loader=create({matrix:m,fetchBlock:async()=>{const error=Error('expired');error.status=409;throw error;},onUpdate(){},onError(){throw Error('unexpected');},onExpired(){expired++;}});
    loader.requestRange(range);await tick();assert.equal(expired,1);assert.equal(m.loadedBlocks.size,0);
    await assert.rejects(loader.ensureAll(),/no longer available/);
});

test('a relation write preserves loaded blocks and ignores old in-flight responses', async () => {
    const m=matrix(1,65);let expired=0;const pending=[];
    m.loadedBlocks.add('0:0');
    const loader=create({matrix:m,fetchBlock:(r,c)=>new Promise((resolve,reject)=>pending.push({resolve,reject,r,c})),onUpdate(){},onError(error){throw error;},onExpired(){expired++;}});
    loader.requestRange({rowStart:0,rowEnd:1,colStart:32,colEnd:40});
    loader.pause();
    loader.rebase('s2');
    assert.equal(m.snapshotId,'s2');assert.equal(m.loadedBlocks.has('0:0'),true);
    const error=Error('old snapshot');error.status=409;
    for(const job of pending.splice(0))job.reject(error);
    await tick();assert.equal(expired,0);
    loader.resume();loader.requestRange({rowStart:0,rowEnd:1,colStart:32,colEnd:40});
    for(const job of pending.splice(0))job.resolve(reply(m,job.r,job.c));
    await tick();assert.equal(m.loadedBlocks.size,3);
});
