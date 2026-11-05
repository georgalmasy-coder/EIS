export function createInterfaceBlockLoader({snapshotId, structures, request, onUpdate, onExpired, onError}) {
    const indexes=new Map(structures.map((s,i)=>[s.entityId,i]));
    const cache=new Map(),pending=new Map();let active=0,disposed=false,paused=false,revision=0,wanted=new Set();
    const key=(r,c)=>`${Math.floor(r/32)*32}:${Math.floor(c/32)*32}`;
    const pairKey=(a,b)=>{const r=indexes.get(a),c=indexes.get(b);return r===undefined||c===undefined?null:key(r,c);};
    function peek(id) {
        const hit=cache.get(id);if(hit){cache.delete(id);cache.set(id,hit);}return hit;
    }
    function pump() {
        if(disposed || paused) return;
        while(active<2) {
            const job=[...pending.values()].find(j=>!j.started);if(!job)break;
            job.started=true;job.revision=revision;active++;
            (async()=>{
                try {
                    const [row,column]=job.id.split(':').map(Number);
                    const reply=await request('block',{row,column});
                    if(disposed || job.revision!==revision)return;
                    if(reply.snapshotId!==snapshotId || reply.data.row!==row || reply.data.column!==column) throw new Error('Invalid interface block response');
                    const cells=new Map(reply.data.cells.map(c=>[`${c.fromEntityId}__${c.toEntityId}`,c]));
                    while(cache.size>=128) {
                        const evict=[...cache.keys()].find(id=>!wanted.has(id));
                        if(evict===undefined)break; // Never thrash blocks of a filtered, scattered viewport.
                        cache.delete(evict);
                    }
                    cache.set(job.id,cells);job.resolve();
                    if(wanted.has(job.id))onUpdate();
                } catch(error) {
                    job.reject(error);
                    if(!disposed && job.revision===revision && error.status===409){dispose();onExpired();}
                } finally {active--;if(pending.get(job.id)===job)pending.delete(job.id);pump();}
            })();
        }
    }
    function ensure(id) {
        if(disposed)return Promise.reject(new Error('Interface snapshot changed'));
        if(peek(id))return Promise.resolve();
        if(pending.has(id))return pending.get(id).promise;
        let resolve,reject;const promise=new Promise((ok,fail)=>{resolve=ok;reject=fail;});
        pending.set(id,{id,promise,resolve,reject,started:false});pump();return promise;
    }
    function keys(rows,columns) {
        const rs=new Set(rows.map(s=>Math.floor(indexes.get(s.entityId)/32)*32));
        const cs=new Set(columns.map(s=>Math.floor(indexes.get(s.entityId)/32)*32));
        return [...rs].flatMap(r=>[...cs].map(c=>`${r}:${c}`));
    }
    function want(rows,columns) {
        wanted=new Set(keys(rows,columns));
        // Drop queued viewport requests that scrolling has made obsolete.
        for(const [id,j]of pending)if(!j.started&&!wanted.has(id)&&!j.required){pending.delete(id);j.resolve();}
        for(const id of wanted) if(!cache.has(id)) ensure(id).catch(e=>{if(!disposed&&!e.cancelled&&e.status!==409)onError(e);});
    }
    async function loadRows(rows,columns) {
        // A single export row can contain at most 32 original column blocks under the export cell cap.
        for(const id of keys(rows,columns)) {
            const promise=ensure(id);if(pending.has(id))pending.get(id).required=true;await promise;
        }
    }
    function pause() {
        paused=true;revision++;
        const error=new Error('Interface snapshot changed');error.cancelled=true;
        for(const job of pending.values())job.reject(error);pending.clear();
    }
    function rotate(nextId,patches) {
        snapshotId=nextId;
        for(const patch of patches) {
            const cells=cache.get(pairKey(patch.fromEntityId,patch.toEntityId));
            if(!cells)continue;
            const id=`${patch.fromEntityId}__${patch.toEntityId}`;
            if(patch.cell)cells.set(id,patch.cell);else cells.delete(id);
        }
        paused=false;pump();
    }
    function dispose(){disposed=true;for(const j of pending.values())j.reject(new Error('Interface snapshot changed'));pending.clear();cache.clear();}
    return {want,loadRows,dispose,pause,rotate,isLoaded:(a,b)=>Boolean(peek(pairKey(a,b))),
        getCell:(a,b)=>peek(pairKey(a,b))?.get(`${a}__${b}`) || null};
}
