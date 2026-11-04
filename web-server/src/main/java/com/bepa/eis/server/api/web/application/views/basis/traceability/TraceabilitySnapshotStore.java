package com.bepa.eis.server.api.web.application.views.basis.traceability;

import com.bepa.eis.common.dto.WebSession;
import java.util.*;

/** Bounded, process-local snapshots. Never share a snapshot across login sessions or projects. */
final class TraceabilitySnapshotStore {
    interface Source {
        int rowCount();
        int columnCount();
        List<TraceabilityMatrixDocument.BlockCell> calculateBlock(int rowStart, int columnStart, int rowEnd, int columnEnd);
        default void validatePair(int rowId, int columnId) { throw new UnsupportedOperationException(); }
        default CellUpdate updateRelation(int rowId, int columnId, com.bepa.eis.common.providers.entityrelation.EntityRelationRecord relation) { throw new UnsupportedOperationException(); }
    }
    static final int BLOCK_SIZE = 32;
    private static final int MAX_SNAPSHOTS = 4;
    private static final int MAX_BLOCKS = 256;
    private static final long TTL_MS = 15 * 60_000L;
    private final Map<String, Entry> entries = new LinkedHashMap<>(16, .75f, true);
    private final java.util.function.LongSupplier clock;
    TraceabilitySnapshotStore() { this(System::currentTimeMillis); }
    TraceabilitySnapshotStore(java.util.function.LongSupplier clock) { this.clock = clock; }

    record Owner(String sessionId, Integer customerId, Integer projectId, Integer userId) {
        static Owner of(WebSession session) {
            return new Owner(session.getSessionId(), session.getCustomerId(), session.getProjectId(), session.getUserId());
        }
    }
    record Block(String snapshotId, int rowStart, int columnStart, int rowEnd, int columnEnd,
                 List<TraceabilityMatrixDocument.BlockCell> cells, boolean cacheHit) {}
    private record Key(int row, int column) {}
    record CellUpdate(int rowIndex, int columnIndex, String value, String style, String rowStyle, String columnStyle) {}
    record Update(String snapshotId, CellUpdate cell) {}
    private static final class Entry {
        final Owner owner;
        final Source document;
        final long created;
        final Map<Key, List<TraceabilityMatrixDocument.BlockCell>> blocks = new LinkedHashMap<>(16, .75f, true);
        Entry(Owner owner, Source document, long created) { this.owner = owner; this.document = document; this.created = created; }
    }

    synchronized String put(WebSession session, Source document) {
        entries.values().removeIf(entry -> clock.getAsLong() - entry.created > TTL_MS);
        while (entries.size() >= MAX_SNAPSHOTS) entries.remove(entries.keySet().iterator().next());
        String id = UUID.randomUUID().toString();
        entries.put(id, new Entry(Owner.of(session), document, clock.getAsLong()));
        return id;
    }

    Block block(WebSession session, String id, int row, int column) {
        Entry entry;
        synchronized (this) {
            entry = entries.get(id);
            if (entry == null || !entry.owner.equals(Owner.of(session))
                    || clock.getAsLong() - entry.created > TTL_MS) {
                throw new ExpiredSnapshotException();
            }
        }
        synchronized (entry) {
            if (row < 0 || column < 0 || row % BLOCK_SIZE != 0 || column % BLOCK_SIZE != 0
                    || row >= entry.document.rowCount() || column >= entry.document.columnCount()) {
                throw new IllegalArgumentException("Invalid traceability block range");
            }
            int rowEnd = Math.min(row + BLOCK_SIZE, entry.document.rowCount());
            int columnEnd = Math.min(column + BLOCK_SIZE, entry.document.columnCount());
            Key key = new Key(row, column);
            var cells = entry.blocks.get(key);
            boolean hit = cells != null;
            if (!hit) {
                cells = entry.document.calculateBlock(row, column, rowEnd, columnEnd);
                while (entry.blocks.size() >= MAX_BLOCKS) entry.blocks.remove(entry.blocks.keySet().iterator().next());
                entry.blocks.put(key, cells);
            }
            return new Block(id, row, column, rowEnd, columnEnd, cells, hit);
        }
    }

    synchronized void invalidate(WebSession session) {
        entries.values().removeIf(entry -> Objects.equals(entry.owner.customerId(), session.getCustomerId())
                && Objects.equals(entry.owner.projectId(), session.getProjectId()));
    }

    private synchronized Entry owned(WebSession session, String id) {
        Entry entry = entries.get(id);
        if (entry == null || !entry.owner.equals(Owner.of(session)) || clock.getAsLong() - entry.created > TTL_MS) throw new ExpiredSnapshotException();
        return entry;
    }

    void validatePair(WebSession session, String id, int rowId, int columnId) {
        Entry entry = owned(session, id);
        synchronized (entry) { entry.document.validatePair(rowId, columnId); }
    }

    Update update(WebSession session, String id, int rowId, int columnId,
                  com.bepa.eis.common.providers.entityrelation.EntityRelationRecord relation) {
        Entry entry = owned(session, id);
        synchronized (entry) {
            CellUpdate cell = entry.document.updateRelation(rowId, columnId, relation);
            entry.blocks.remove(new Key(cell.rowIndex() / BLOCK_SIZE * BLOCK_SIZE, cell.columnIndex() / BLOCK_SIZE * BLOCK_SIZE));
            synchronized (this) {
                if (entries.get(id) != entry) throw new ExpiredSnapshotException();
                invalidate(session); // Other tabs/users must refresh rather than mix revisions.
                String nextId = UUID.randomUUID().toString();
                entries.put(nextId, entry); // Preserve other computed blocks and original expiry.
                return new Update(nextId, cell);
            }
        }
    }

    static final class ExpiredSnapshotException extends RuntimeException {
        ExpiredSnapshotException() { super("Traceability snapshot expired. Reload the matrix."); }
    }
}
