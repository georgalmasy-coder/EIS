const {readFileSync}=require('node:fs');
const assert=require('node:assert/strict');
const test=require('node:test');
const source = readFileSync(require('node:path').join(__dirname,'../../main/webapp/js/components/relation-diagram-loader.js'), 'utf8');
const createRelationDiagramLoader = new Function(source.replace('export ', '') + ';return createRelationDiagramLoader;')();
const tick = () => new Promise(resolve => setImmediate(resolve));

test('deduplicates requests, bounds concurrency and skips superseded queued traces', async () => {
    const calls=[];
    const loader=createRelationDiagramLoader({snapshotId:'s',onExpired:()=>{},request:(kind,key)=>new Promise(resolve=>calls.push({kind,key,resolve}))});
    const a=loader.load('trace','a'), duplicate=loader.load('trace','a');
    assert.equal(a,duplicate);
    const b=loader.load('details','b');
    const old=loader.load('trace','old'); const rejected=assert.rejects(old,/Superseded/);
    const latest=loader.load('trace','latest');
    assert.equal(calls.length,2);
    calls[0].resolve({snapshotId:'s',data:{visibleIds:['a'],relations:[]}});await a;await tick();
    assert.deepEqual(calls.map(c=>c.key),['a','b','latest']);await rejected;
    calls[1].resolve({snapshotId:'s',data:{description:'b'}});
    calls[2].resolve({snapshotId:'s',data:{visibleIds:['latest'],relations:[]}});await Promise.all([b,latest]);
    assert(loader.peek('trace','a').visibleIds.has('a'));assert.equal(await loader.load('trace','a'),await a);
    loader.dispose();assert.equal(loader.peek('trace','a'),null);
});

test('expires once and discards late replies after disposal',async()=>{
    let expire=0,resolveLate;
    const loader=createRelationDiagramLoader({snapshotId:'s',onExpired:()=>expire++,request:async(kind,key)=>{
        if(key==='late')return new Promise(resolve=>resolveLate=resolve);
        const error=new Error('expired');error.status=409;throw error;
    }});
    const late=loader.load('trace','late');const rejected=assert.rejects(late,/snapshot changed/);
    await assert.rejects(loader.load('details','expired'),/expired/);await rejected;
    resolveLate({snapshotId:'s',data:{visibleIds:['late'],relations:[]}});await tick();
    assert.equal(expire,1);assert.equal(loader.peek('trace','late'),null);
});

test('evicts old entries and rejects responses from another snapshot',async()=>{
    const loader=createRelationDiagramLoader({snapshotId:'s',onExpired:()=>{},request:async(kind,key)=>({snapshotId:key==='wrong'?'other':'s',data:[key]})});
    for(let i=0;i<65;i++)await loader.load('search',String(i));
    assert.equal(loader.peek('search','0'),null);assert.deepEqual(loader.peek('search','64'),['64']);
    await assert.rejects(loader.load('search','wrong'),/Invalid.*snapshot/);
});
