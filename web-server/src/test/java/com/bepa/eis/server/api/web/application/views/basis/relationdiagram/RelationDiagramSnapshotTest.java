package com.bepa.eis.server.api.web.application.views.basis.relationdiagram;

import com.bepa.eis.common.dto.WebSession;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** Database-free regression for traversal, descriptions and snapshot isolation. */
public class RelationDiagramSnapshotTest {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static void expired(Runnable action) {
        try { action.run(); } catch (RelationDiagramSnapshotStore.Expired expected) { return; }
        throw new AssertionError("Expected expired snapshot");
    }
    private static WebSession session(String id, int project, int user) {
        var s = new WebSession(); s.setSessionId(id); s.setCustomerId(1); s.setProjectId(project); s.setUserId(user); return s;
    }
    public static void main(String[] args) {
        Random random = new Random(87);
        int comparisons = 0;
        for (int sample = 0; sample < 40; sample++) {
            var nodes = new ArrayList<RelationDiagramSnapshot.Node>();
            var edges = new ArrayList<RelationDiagramSnapshot.Edge>();
            int[] types = sample % 2 == 0 ? new int[]{5,6,2} : new int[]{5,6,9,8,2};
            int nodeCount = types.length * 3;
            for (int i = 0; i < nodeCount; i++) nodes.add(new RelationDiagramSnapshot.Node("n"+i, types[i/3], i, "code"+i, "name", i==0?"description needle":""));
            for (int i = 0; i < 25; i++) edges.add(new RelationDiagramSnapshot.Edge("r"+i,"n"+random.nextInt(nodeCount),"n"+random.nextInt(nodeCount),"confirmed"));
            edges.add(new RelationDiagramSnapshot.Edge("dangling","n0","missing","confirmed"));
            var graph = new RelationDiagramSnapshot(nodes, edges);
            for (var selected : nodes) {
                Set<String> visible = new HashSet<>(), relations = new HashSet<>(); visible.add(selected.internalId());
                // Independent reference scans the edge list at each step, as the original client did.
                for (int direction : new int[]{-1,1}) {
                    Set<String> visited = new HashSet<>();
                    var queue = new ArrayDeque<String>(); queue.add(selected.internalId());
                    while (!queue.isEmpty()) {
                        String current = queue.remove(); if (!visited.add(current)) continue;
                        for (var edge : edges) {
                            if (edge.to().equals("missing")) continue;
                            String next = edge.from().equals(current)?edge.to():edge.to().equals(current)?edge.from():null;
                            if (next == null || (Integer.parseInt(next.substring(1))/3-Integer.parseInt(current.substring(1))/3)*direction<0) continue;
                            visible.add(next); relations.add(edge.id()); if (!visited.contains(next)) queue.add(next);
                        }
                    }
                }
                var trace = graph.trace(selected.internalId());
                check(new HashSet<>(trace.visibleIds()).equals(visible), "visible nodes differ");
                check(trace.relations().stream().map(RelationDiagramSnapshot.Edge::id).collect(java.util.stream.Collectors.toSet()).equals(relations), "trace edges differ");
                check(graph.trace(selected.internalId()) == trace, "trace cache miss"); comparisons++;
            }
            check(graph.search("NEEDLE").equals(List.of("n0")), "description search");
            check(graph.details("n0").description().equals("description needle"), "description details");
            check(!graph.details("n0").relatedIds().contains("missing"), "dangling neighbor");
        }
        var graph = new RelationDiagramSnapshot(List.of(), List.of());
        AtomicLong clock = new AtomicLong();
        var store = new RelationDiagramSnapshotStore(clock::get);
        var owner = session("a",6,1); String id = store.put(owner,graph);
        check(store.get(owner,id)==graph, "snapshot lookup");
        expired(()->store.get(session("b",6,1),id));
        expired(()->store.get(session("a",7,1),id));
        expired(()->store.get(session("a",6,2),id));
        var other = session("a",6,1); other.setCustomerId(2); expired(()->store.get(other,id));
        clock.set(16*60_000L); expired(()->store.get(owner,id));
        String old = store.put(owner,graph);
        for(int i=0;i<4;i++) store.put(owner,graph);
        expired(()->store.get(owner,old));
        System.out.println("Relation diagram: " + comparisons + " trace comparisons; search, details, ownership, expiry and eviction passed");
    }
}
