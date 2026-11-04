// Queue only the current viewport and a small buffer. Loaded empty blocks are
// tracked separately from sparse cells, so they cannot look like unloaded data.
export function createTraceabilityBlockLoader({matrix, fetchBlock, onUpdate, onError, onExpired}) {
    const jobs = new Map();
    let active = 0;
    let disposed = false;
    let paused = false;
    let epoch = 0;
    const failed = new Set();
    const size = matrix.blockSize;
    const keyOf = (r, c) => `${r}:${c}`;

    function pump() {
        if (disposed || paused) return;
        while (active < 2) {
            const job = [...jobs.values()].filter(job => !job.started).sort((a, b) => a.rank - b.rank)[0];
            if (!job) break;
            job.started = true;
            active++;
            (async () => {
                try {
                    const block = await fetchBlock(job.r, job.c);
                    if (disposed || job.epoch !== epoch) { job.resolve(); return; }
                    if (block.snapshotId !== matrix.snapshotId || block.rowStart !== job.r || block.columnStart !== job.c) {
                        throw new Error("Invalid traceability block response");
                    }
                    if (block.rowEnd !== Math.min(job.r + size, matrix.rows.length)
                        || block.columnEnd !== Math.min(job.c + size, matrix.columns.length)
                        || !Array.isArray(block.cells) || block.cells.some(cell =>
                            cell.rowIndex < job.r || cell.rowIndex >= block.rowEnd
                            || cell.columnIndex < job.c || cell.columnIndex >= block.columnEnd)) {
                        throw new Error("Invalid traceability block boundaries");
                    }
                    for (const cell of block.cells) {
                        let row = matrix.cellsByRow.get(cell.rowIndex);
                        if (!row) matrix.cellsByRow.set(cell.rowIndex, row = new Map());
                        row.set(cell.columnIndex, cell);
                    }
                    matrix.loadedBlocks.add(job.key);
                    job.resolve();
                    onUpdate();
                } catch (error) {
                    if (!disposed && job.epoch === epoch) failed.add(job.key);
                    job.reject(error);
                    if (!disposed && job.epoch === epoch) {
                        if (error.status === 409) { dispose(); onExpired(); }
                        else onError(error);
                    }
                } finally {
                    active--;
                    if (jobs.get(job.key) === job) jobs.delete(job.key);
                    pump();
                }
            })();
        }
    }

    function enqueue(r, c, rank, exporting = false) {
        const key = keyOf(r, c);
        if (matrix.loadedBlocks.has(key)) return Promise.resolve();
        let job = jobs.get(key);
        if (!job) {
            let resolve, reject;
            const promise = new Promise((ok, fail) => { resolve = ok; reject = fail; });
            job = {key, r, c, rank, exporting, promise, resolve, reject, started: false, epoch};
            jobs.set(key, job);
        }
        job.rank = Math.min(job.rank, rank);
        job.exporting ||= exporting;
        return job.promise;
    }

    function requestRange({rowStart, rowEnd, colStart, colEnd}) {
        if (disposed || rowEnd <= rowStart || colEnd <= colStart) return;
        const wanted = new Map();
        const firstRow = Math.floor(rowStart / size) * size;
        const firstCol = Math.floor(colStart / size) * size;
        const lastRow = Math.floor((rowEnd - 1) / size) * size;
        const lastCol = Math.floor((colEnd - 1) / size) * size;
        for (let r = Math.max(0, firstRow - size); r <= lastRow + size && r < matrix.rows.length; r += size) {
            for (let c = Math.max(0, firstCol - size); c <= lastCol + size && c < matrix.columns.length; c += size) {
                wanted.set(keyOf(r, c), {r, c, rank: r >= firstRow && r <= lastRow && c >= firstCol && c <= lastCol ? 0 : 1});
            }
        }
        for (const [key, job] of jobs) {
            if (!job.started && !job.exporting && !wanted.has(key)) { jobs.delete(key); job.resolve(); }
        }
        for (const job of wanted.values()) {
            if (!failed.has(keyOf(job.r, job.c))) enqueue(job.r, job.c, job.rank).catch(() => {});
        }
        pump();
    }

    async function ensureAll(onProgress = () => {}) {
        if (disposed || paused) throw new Error("Traceability snapshot is no longer available");
        const pending = [];
        const total = Math.ceil(matrix.rows.length / size) * Math.ceil(matrix.columns.length / size);
        let completed = 0;
        for (let r = 0; r < matrix.rows.length; r += size) {
            for (let c = 0; c < matrix.columns.length; c += size) {
                pending.push(enqueue(r, c, 2, true).then(() => onProgress(++completed, total)));
            }
        }
        pump();
        try {
            await Promise.all(pending);
        } catch (error) {
            // A failed export must not keep computing the remaining matrix.
            for (const [key, job] of jobs) {
                job.exporting = false;
                if (!job.started && job.rank === 2) { jobs.delete(key); job.resolve(); }
            }
            throw error;
        }
        if (disposed || matrix.loadedBlocks.size < total) throw new Error("Traceability snapshot changed during export");
    }

    function dispose() {
        disposed = true;
        for (const job of jobs.values()) job.reject(new Error("Traceability snapshot changed"));
        jobs.clear();
    }
    function rebase(snapshotId) {
        epoch++;
        for (const job of jobs.values()) job.reject(new Error("Traceability snapshot changed"));
        jobs.clear();
        failed.clear();
        matrix.snapshotId = snapshotId;
    }
    function pause() {
        paused = true;
        rebase(matrix.snapshotId); // Ignore reads already in flight while the write commits.
    }
    function resume() { paused = false; pump(); }
    return {requestRange, ensureAll, dispose, rebase, pause, resume};
}
