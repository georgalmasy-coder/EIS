const {readFileSync}=require('node:fs');
const path=require('node:path');
const assert=require('node:assert/strict');
const test=require('node:test');
const source=readFileSync(path.join(__dirname,'../../main/webapp/js/components/interface-block-loader.js'),'utf8');
const createInterfaceBlockLoader=new Function(source.replace('export ','')+';return createInterfaceBlockLoader;')();
const structures=Array.from({length:130},(_,i)=>({entityId:String(i+1)}));
const tick=()=>new Promise(resolve=>setImmediate(resolve));

test('bounds parallel calls, replaces queued viewport work, and caches empty blocks',async()=>{
    const calls=[];let updates=0;
    const loader=createInterfaceBlockLoader({snapshotId:'s',structures,onUpdate:()=>updates++,onError:e=>{throw e;},onExpired:()=>{},
        request:(command,args)=>new Promise(resolve=>calls.push({command,args,resolve}))});
    loader.want([structures[0],structures[32]],[structures[0],structures[32]]);
    assert.equal(calls.length,2);
    loader.want([structures[96]],[structures[96]]);
    const reply=job=>job.resolve({snapshotId:'s',data:{...job.args,cells:[]}});
    reply(calls[0]);await tick();
    assert.equal(calls.length,3);assert.deepEqual(calls[2].args,{row:96,column:96});
    reply(calls[1]);reply(calls[2]);await tick();
    assert.equal(updates,1);assert(loader.isLoaded('97','97'));assert.equal(loader.getCell('97','97'),null);
    loader.want([structures[96]],[structures[96]]);assert.equal(calls.length,3);
    loader.dispose();
});

test('rejects late replies from a disposed snapshot',async()=>{
    let reply,updates=0;
    const loader=createInterfaceBlockLoader({snapshotId:'s',structures,onUpdate:()=>updates++,onError:()=>{},onExpired:()=>{},
        request:()=>new Promise(resolve=>reply=resolve)});
    loader.want([structures[0]],[structures[0]]);loader.dispose();
    reply({snapshotId:'s',data:{row:0,column:0,cells:[]}});await tick();
    assert.equal(updates,0);assert(!loader.isLoaded('1','1'));
});

test('expiry is reported once and export can fetch never-viewed cells',async()=>{
    let expired=0;
    const loader=createInterfaceBlockLoader({snapshotId:'s',structures,onUpdate:()=>{},onError:()=>{},onExpired:()=>expired++,
        request:async(command,args)=>{
            if(args.row===32){const e=new Error('Expired');e.status=409;throw e;}
            return {snapshotId:'s',data:{...args,cells:[{fromEntityId:'1',toEntityId:'2',irlId:'4'}]}};
        }});
    await loader.loadRows([structures[0]],[structures[1]]);
    assert.equal(loader.getCell('1','2').irlId,'4');
    loader.want([structures[32]],[structures[0],structures[32]]);await tick();
    assert.equal(expired,1);assert(!loader.isLoaded('1','2'));
});

test('refresh preserves caches, patches both directions and ignores old in-flight responses',async()=>{
    let late,expired=0,current='s';
    const loader=createInterfaceBlockLoader({snapshotId:'s',structures,onUpdate:()=>{},onError:e=>{throw e;},onExpired:()=>expired++,
        request:async(command,args)=>{
            if(args.row===32 && current==='s')return new Promise(resolve=>late=resolve);
            return {snapshotId:current,data:{...args,cells:[{fromEntityId:'1',toEntityId:'2',irlId:'4'},{fromEntityId:'2',toEntityId:'1',irlId:'4'}]}};
        }});
    await loader.loadRows([structures[0]],[structures[1]]);
    loader.want([structures[32]],[structures[0]]);
    loader.pause();current='next';
    loader.rotate('next',[{fromEntityId:'1',toEntityId:'2',cell:{fromEntityId:'1',toEntityId:'2',irlId:'9'}},
        {fromEntityId:'2',toEntityId:'1',cell:null}]);
    assert.equal(loader.getCell('1','2').irlId,'9');assert.equal(loader.getCell('2','1'),null);
    late({snapshotId:'s',data:{row:32,column:0,cells:[]}});await tick();
    assert.equal(loader.getCell('1','2').irlId,'9');assert.equal(expired,0);
    assert(!loader.isLoaded('33','1')); // Late response must not populate the new cache.
    loader.dispose();
});
