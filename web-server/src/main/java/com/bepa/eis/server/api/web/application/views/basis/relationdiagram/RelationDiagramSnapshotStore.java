package com.bepa.eis.server.api.web.application.views.basis.relationdiagram;

import com.bepa.eis.common.dto.WebSession;
import java.util.*;

public final class RelationDiagramSnapshotStore {
    record Owner(String sessionId, Integer customer, Integer project, Integer user) {
        static Owner of(WebSession session) { return new Owner(session.getSessionId(), session.getCustomerId(), session.getProjectId(), session.getUserId()); }
    }
    record Entry(Owner owner, RelationDiagramSnapshot graph, long created) {}
    private final Map<String, Entry> entries = new LinkedHashMap<>(16, .75f, true);
    private final java.util.function.LongSupplier clock;
    public RelationDiagramSnapshotStore() { this(System::currentTimeMillis); }
    public RelationDiagramSnapshotStore(java.util.function.LongSupplier clock) { this.clock = clock; }
    public synchronized String put(WebSession session, RelationDiagramSnapshot graph) {
        entries.values().removeIf(entry -> clock.getAsLong() - entry.created() > 15 * 60_000L);
        while (entries.size() >= 4) entries.remove(entries.keySet().iterator().next());
        String id = UUID.randomUUID().toString(); entries.put(id, new Entry(Owner.of(session), graph, clock.getAsLong())); return id;
    }
    public synchronized RelationDiagramSnapshot get(WebSession session, String id) {
        Entry entry = entries.get(id);
        if (entry == null || !entry.owner().equals(Owner.of(session)) || clock.getAsLong() - entry.created() > 15 * 60_000L) throw new Expired();
        return entry.graph();
    }
    public static class Expired extends RuntimeException { Expired() { super("Relation diagram snapshot expired. Reload the diagram."); } }
}
