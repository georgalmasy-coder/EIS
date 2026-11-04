package com.bepa.eis.server.api.web.application.views.common;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Database-free JDBC boundary regression: one scoped query, minimal projection, preserved identities/directions. */
public class DiagramRelationProviderTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
    public static void main(String[] args) throws Exception {
        var session = new WebSession(); session.setCustomerId(1); session.setProjectId(6);
        var provider = new EntityRelationProvider(session);
        var types = Set.of(EntityType.STAKEHOLDER_REQUIREMENT, EntityType.SYSTEM_REQUIREMENT,
                EntityType.FUNCTIONAL_STRUCTURE, EntityType.LOGICAL_STRUCTURE, EntityType.SYSTEMS_BREAKDOWN);
        // Reverse, same-type, self and distinct database records sharing endpoints must all survive once each.
        int[][] rows = {{101,5,1,6,2}, {102,6,2,5,1}, {103,9,3,9,4}, {104,8,5,8,5}, {105,5,1,6,2}};
        AtomicInteger position = new AtomicInteger(-1), queries = new AtomicInteger(), prepared = new AtomicInteger();
        Set<String> closed = new HashSet<>(); Map<Integer,Integer> parameters = new TreeMap<>();
        ResultSet result = proxy(ResultSet.class, (p,m,a) -> switch (m.getName()) {
            case "next" -> position.incrementAndGet() < rows.length;
            case "getInt" -> rows[position.get()][(int)a[0]-1];
            case "close" -> { closed.add("result"); yield null; }
            default -> throw new AssertionError("Unexpected ResultSet call: " + m.getName());
        });
        PreparedStatement statement = proxy(PreparedStatement.class, (p,m,a) -> switch(m.getName()) {
            case "setInt" -> { parameters.put((int)a[0],(int)a[1]); yield null; }
            case "executeQuery" -> { queries.incrementAndGet(); yield result; }
            case "close" -> { closed.add("statement"); yield null; }
            default -> throw new AssertionError("Unexpected statement call: " + m.getName());
        });
        Connection connection = proxy(Connection.class, (p,m,a) -> {
            check(m.getName().equals("prepareStatement"), "Unexpected connection call"); prepared.incrementAndGet();
            String sql = (String)a[0];
            String projection = sql.substring(0,sql.indexOf("FROM"));
            check(!projection.contains("Created") && !projection.contains("RelationType"), "Unused metadata selected");
            check(sql.contains("ER.CustomerId = ? AND ER.ProjectId = ?"), "Tenant/project scope missing");
            for(String filter:List.of("ER.Latest = 1", "ER.RelationType = 1", "E1.Latest = 1", "E1.Active = 1", "E2.Latest = 1", "E2.Active = 1"))
                check(sql.contains(filter), "Missing filter " + filter);
            check(sql.contains("E1.EntityType = ER.EntityType") && sql.contains("E2.EntityType = ER.RelatedEntityType"), "Endpoint types missing");
            check(sql.contains("E1.EntityId = ER.EntityId") && sql.contains("E2.EntityId = ER.RelatedEntityId"), "Endpoint IDs missing");
            return statement;
        });
        var relations = provider.getConfirmedDiagramRelations(connection,types);
        check(prepared.get()==1 && queries.get()==1, "Must execute one query for all five types");
        check(new ArrayList<>(parameters.values()).equals(List.of(1,6,2,5,6,8,9,2,5,6,8,9)), "Scope/type parameter bindings");
        check(relations.size()==rows.length, "Relations lost or multiplied");
        for(int i=0;i<rows.length;i++) {
            var r=relations.get(i); var row=rows[i];
            check(r.equals(new EntityRelationProvider.DiagramRelation(row[0],row[1],row[2],row[3],row[4])), "Relation identity/direction changed");
        }
        check(closed.equals(Set.of("result","statement")), "JDBC resources not closed");
        check(provider.getConfirmedDiagramRelations(connection,Set.of()).isEmpty() && prepared.get()==1, "Empty types must skip query");
        parameters.clear();
        provider.getConfirmedDiagramRelations(connection, Set.of(EntityType.STAKEHOLDER_REQUIREMENT,
                EntityType.SYSTEM_REQUIREMENT, EntityType.SYSTEMS_BREAKDOWN));
        check(prepared.get()==2 && queries.get()==2, "Basis diagram must also use one query");
        check(new ArrayList<>(parameters.values()).equals(List.of(1,6,2,5,6,2,5,6)), "Basis scope/type parameter bindings");
        System.out.println("Diagram relation provider: single query, scope, endpoint filters, minimal fields, mapping and resources passed");
    }
}
