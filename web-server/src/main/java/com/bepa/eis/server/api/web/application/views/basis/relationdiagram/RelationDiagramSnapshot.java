package com.bepa.eis.server.api.web.application.views.basis.relationdiagram;

import java.util.*;

/** Immutable graph; cached traces preserve separate upstream/downstream traversal. */
public final class RelationDiagramSnapshot {
    public record Node(String internalId, int entityType, int entityId, String visibleId, String name, String description) {}
    public record Edge(String id, String from, String to, String type) {}
    public record Trace(List<String> visibleIds, List<Edge> relations) {}
    public record Details(String internalId, String description, List<String> relatedIds) {}
    private final Map<String, Node> nodes = new LinkedHashMap<>();
    private final Map<String, List<Edge>> adjacency = new HashMap<>();
    private final Map<String, Trace> traces = new LinkedHashMap<>(16, .75f, true);
    private int cachedEdges;

    public RelationDiagramSnapshot(List<Node> nodeList, List<Edge> edges) {
        for (Node node : nodeList) nodes.put(node.internalId(), node);
        for (Edge edge : edges) {
            if (!nodes.containsKey(edge.from()) || !nodes.containsKey(edge.to())) continue;
            adjacency.computeIfAbsent(edge.from(), ignored -> new ArrayList<>()).add(edge);
            if (!edge.from().equals(edge.to())) adjacency.computeIfAbsent(edge.to(), ignored -> new ArrayList<>()).add(edge);
        }
    }
    public boolean hasRelations(String id) { return adjacency.containsKey(id); }
    private Node node(String id) {
        Node node = nodes.get(id);
        if (node == null) throw new IllegalArgumentException("Entity is not in this diagram snapshot");
        return node;
    }
    private int order(String id) {
        return switch (node(id).entityType()) { case 5 -> 0; case 6 -> 1; case 9 -> 2; case 8 -> 3; case 2 -> 4; default -> throw new IllegalArgumentException("Unsupported diagram type"); };
    }
    public synchronized Trace trace(String id) {
        node(id);
        Trace cached = traces.get(id);
        if (cached != null) return cached;
        Set<String> visible = new LinkedHashSet<>(); visible.add(id);
        Set<Edge> edges = new LinkedHashSet<>();
        for (int direction : new int[]{-1, 1}) {
            Set<String> visited = new HashSet<>(); visited.add(id);
            List<String> pending = new ArrayList<>(); pending.add(id);
            for (int i = 0; i < pending.size(); i++) {
                String current = pending.get(i);
                for (Edge edge : adjacency.getOrDefault(current, List.of())) {
                    String next = edge.from().equals(current) ? edge.to() : edge.from();
                    if ((order(next) - order(current)) * direction < 0) continue;
                    edges.add(edge);
                    if (visited.add(next)) { visible.add(next); pending.add(next); }
                }
            }
        }
        Trace result = new Trace(List.copyOf(visible), List.copyOf(edges));
        // Dense same-type graphs can return very large traces: do not retain many copies.
        if (edges.size() <= 100_000) {
            while (!traces.isEmpty() && (traces.size() >= 32 || cachedEdges + edges.size() > 100_000)) {
                cachedEdges -= traces.remove(traces.keySet().iterator().next()).relations().size();
            }
            traces.put(id, result); cachedEdges += edges.size();
        }
        return result;
    }
    public Details details(String id) {
        Node selected = node(id);
        Set<String> related = new LinkedHashSet<>();
        for (Edge edge : adjacency.getOrDefault(id, List.of())) related.add(edge.from().equals(id) ? edge.to() : edge.from());
        return new Details(id, selected.description(), List.copyOf(related));
    }
    public List<String> search(String text) {
        String query = text.trim().toLowerCase(Locale.ROOT);
        if (query.length() > 1000) throw new IllegalArgumentException("Search text is too long");
        return nodes.values().stream().filter(n -> contains(n.visibleId(), query) || contains(n.name(), query) || contains(n.description(), query))
                .map(Node::internalId).toList();
    }
    private boolean contains(String value, String query) { return value != null && value.toLowerCase(Locale.ROOT).contains(query); }
}
