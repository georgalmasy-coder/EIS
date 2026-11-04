export function createRelationDiagramLoader({snapshotId, request, onExpired}) {
    const cache = new Map(), pending = new Map();
    let active = 0, disposed = false, edgeCount = 0;
    const keyOf = (kind, key) => `${kind}:${key}`;
    function pump() {
        if (disposed) return;
        while (active < 2) {
            const job = [...pending.values()].find(item => !item.started);
            if (!job) break;
            job.started = true; active++;
            (async () => {
                try {
                    const reply = await request(job.kind, job.key);
                    if (disposed) return;
                    if (reply.snapshotId !== snapshotId) throw new Error("Invalid relation diagram snapshot response");
                    let data = reply.data;
                    if (job.kind === "trace") data = {visibleIds: new Set(data.visibleIds), relations: new Set(data.relations)};
                    const edges = job.kind === "trace" ? data.relations.size : 0;
                    // Bound retained traces, details and search results together.
                    while (cache.size && (cache.size >= 64 || edgeCount + edges > 100_000)) {
                        const first = cache.keys().next().value;
                        edgeCount -= cache.get(first).edges; cache.delete(first);
                    }
                    cache.set(job.id, {data, edges}); edgeCount += edges;
                    job.resolve(data);
                } catch (error) {
                    job.reject(error);
                    if (!disposed && error.status === 409) { dispose(); onExpired(); }
                } finally { active--; pending.delete(job.id); pump(); }
            })();
        }
    }
    function peek(kind, key) {
        const id = keyOf(kind, key), hit = cache.get(id);
        if (!hit) return null;
        cache.delete(id); cache.set(id, hit); return hit.data;
    }
    function load(kind, key) {
        if (disposed) return Promise.reject(new Error("Diagram snapshot changed"));
        const hit = peek(kind, key);
        if (hit) return Promise.resolve(hit);
        const id = keyOf(kind, key);
        if (pending.has(id)) return pending.get(id).promise;
        // Skip traces for cards the pointer has already left before dispatch.
        if (kind === "trace") for (const [oldId, job] of pending) {
            if (job.kind === "trace" && !job.started) { pending.delete(oldId); job.reject(new Error("Superseded trace")); }
        }
        let resolve, reject;
        const promise = new Promise((ok, fail) => {resolve = ok; reject = fail;});
        pending.set(id, {id, kind, key, promise, resolve, reject, started:false}); pump(); return promise;
    }
    function dispose() { disposed = true; for (const job of pending.values()) job.reject(new Error("Diagram snapshot changed")); pending.clear(); cache.clear(); }
    return {peek, load, dispose};
}
