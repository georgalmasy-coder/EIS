package com.bepa.eis.server.api.web.application.views.basis.traceability;

import com.bepa.eis.common.dto.WebSession;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Database-free regression for snapshot ownership, block boundaries and bounded caches. */
public class TraceabilitySnapshotStoreTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void rejected(Runnable action, Class<? extends Throwable> expected) {
        try { action.run(); } catch (Throwable error) {
            if (expected.isInstance(error)) return;
            throw new AssertionError(error);
        }
        throw new AssertionError("Expected " + expected.getSimpleName());
    }
    private static WebSession session(String id, int project, int user) {
        WebSession session = new WebSession();
        session.setSessionId(id); session.setCustomerId(1); session.setProjectId(project); session.setUserId(user);
        return session;
    }
    public static void main(String[] args) throws Exception {
        AtomicLong clock = new AtomicLong();
        var store = new TraceabilitySnapshotStore(clock::get);
        var owner = session("session-a", 6, 1);
        AtomicInteger calculations = new AtomicInteger();
        var source = new TraceabilitySnapshotStore.Source() {
            public int rowCount() { return 4097; }
            public int columnCount() { return 65; }
            public List<TraceabilityMatrixDocument.BlockCell> calculateBlock(int r, int c, int endR, int endC) {
                calculations.incrementAndGet();
                return List.of(new TraceabilityMatrixDocument.BlockCell(r,c,"X","green"));
            }
        };
        String id = store.put(owner, source);
        check(calculations.get() == 0, "snapshot must not calculate cells");
        var first = store.block(owner,id,0,0);
        check(!first.cacheHit() && first.cells().size() == 1, "first block miss");
        check(store.block(owner,id,0,0).cacheHit() && calculations.get() == 1, "block cache hit");
        var edge = store.block(owner,id,4096,64);
        check(edge.rowEnd() == 4097 && edge.columnEnd() == 65, "edge bounds");
        rejected(() -> store.block(owner,id,-32,0), IllegalArgumentException.class);
        rejected(() -> store.block(owner,id,1,0), IllegalArgumentException.class);
        rejected(() -> store.block(owner,id,0,96), IllegalArgumentException.class);
        rejected(() -> store.block(session("session-b",6,1),id,0,0), TraceabilitySnapshotStore.ExpiredSnapshotException.class);
        rejected(() -> store.block(session("session-a",7,1),id,0,0), TraceabilitySnapshotStore.ExpiredSnapshotException.class);
        rejected(() -> store.block(session("session-a",6,2),id,0,0), TraceabilitySnapshotStore.ExpiredSnapshotException.class);
        var otherCustomer = session("session-a",6,1); otherCustomer.setCustomerId(2);
        rejected(() -> store.block(otherCustomer,id,0,0), TraceabilitySnapshotStore.ExpiredSnapshotException.class);
        clock.set(16*60_000L);
        rejected(() -> store.block(owner,id,0,0), TraceabilitySnapshotStore.ExpiredSnapshotException.class);
        String fresh = store.put(owner,source);
        for(int i=0;i<4;i++) store.put(owner,source);
        rejected(() -> store.block(owner,fresh,0,0), TraceabilitySnapshotStore.ExpiredSnapshotException.class);
        String current = store.put(owner,source);
        String otherProject = store.put(session("session-a",7,1),source);
        store.invalidate(owner);
        rejected(() -> store.block(owner,current,0,0), TraceabilitySnapshotStore.ExpiredSnapshotException.class);
        check(!store.block(session("session-a",7,1),otherProject,0,0).cacheHit(), "project invalidation must be scoped");
        // Same-block requests share one computation, including when requested concurrently.
        String concurrent = store.put(owner,source);
        int before = calculations.get();
        Thread a = new Thread(() -> store.block(owner,concurrent,0,0));
        Thread b = new Thread(() -> store.block(owner,concurrent,0,0));
        a.start(); b.start(); a.join(); b.join();
        check(calculations.get() == before+1, "same block calculated twice");
        // More than 256 different blocks evict old results rather than growing forever.
        for (int r=0;r<4097;r+=32) for(int c=0;c<65;c+=32) store.block(owner,concurrent,r,c);
        check(!store.block(owner,concurrent,0,0).cacheHit(), "block cache must evict old results");
        var editable = new TraceabilitySnapshotStore.Source() {
            String style = "normal";
            public int rowCount() { return 64; }
            public int columnCount() { return 64; }
            public void validatePair(int r, int c) { if(r!=1||c!=2) throw new IllegalArgumentException(); }
            public List<TraceabilityMatrixDocument.BlockCell> calculateBlock(int r,int c,int re,int ce) {
                return List.of(new TraceabilityMatrixDocument.BlockCell(r,c,"",style));
            }
            public TraceabilitySnapshotStore.CellUpdate updateRelation(int r,int c,com.bepa.eis.common.providers.entityrelation.EntityRelationRecord relation) {
                validatePair(r,c);style="green";
                return new TraceabilitySnapshotStore.CellUpdate(0,0,"X",style,"normal","normal");
            }
        };
        String original=store.put(owner,editable);
        String anotherTab=store.put(session("tab-b",6,1),source);
        store.block(owner,original,0,0);store.block(owner,original,32,32);
        rejected(()->store.validatePair(owner,original,99,2),IllegalArgumentException.class);
        store.validatePair(owner,original,1,2);
        var update=store.update(owner,original,1,2,null);
        check(!update.snapshotId().equals(original),"write must rotate snapshot id");
        rejected(()->store.block(owner,original,0,0),TraceabilitySnapshotStore.ExpiredSnapshotException.class);
        rejected(()->store.block(session("tab-b",6,1),anotherTab,0,0),TraceabilitySnapshotStore.ExpiredSnapshotException.class);
        check(store.block(owner,update.snapshotId(),32,32).cacheHit(),"unaffected blocks must remain cached");
        var updatedBlock=store.block(owner,update.snapshotId(),0,0);
        check(!updatedBlock.cacheHit()&&updatedBlock.cells().get(0).style().equals("green"),"edited block must be recalculated");
        System.out.println("Passed: lazy snapshots, ownership, range validation, TTL, bounded snapshots, project invalidation and concurrent cache hits.");
    }
}
