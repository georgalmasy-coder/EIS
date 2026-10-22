package com.bepa.eis.server.api.web.application.views.basis.lookupmaintenance;

import com.bepa.eis.common.enums.entity.EntityDataElement;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.api.web.application.views.basis.lookupmaintenance.LookupValueMaintenanceProvider.LookupRow;
import com.bepa.eis.server.api.web.application.views.basis.lookupmaintenance.LookupValueMaintenanceProvider.LookupType;

import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;

/** Standalone JDBC regression suite; does not connect to or modify a live database. */
public final class LookupValueMaintenanceProviderTest {
    public static void main(String[] args) throws Exception {
        for (LookupType type : List.of(LookupType.TRL, LookupType.IRL, LookupType.SRL)) {
            for (int level = 1; level <= 9; level++) {
                for (boolean active : List.of(false, true)) {
                    Database db = new Database(type, level);
                    db.save(active);
                    check(db.commits == 1 && db.rollbacks == 0, "Successful change commits once");
                    check(db.autoCommit, "Auto commit restored");
                    check(db.checks == (active ? 0 : 2), "Deactivation checks both TRL and IRL, including for SRL");
                    check(db.synchronizedTables.equals(Set.of("TRL", "IRL", "SRL")), "All three lookups synchronized");
                    for (LookupType target : List.of(LookupType.TRL, LookupType.IRL, LookupType.SRL)) {
                        for (int candidate = 1; candidate <= 9; candidate++) {
                            boolean expected = active ? candidate == level : candidate < level;
                            check(db.levels.get(target)[candidate] == expected, "Selected level and higher cascade are correct");
                        }
                    }
                }
                for (String used : List.of("TRL", "IRL")) {
                    Database db = new Database(type, level);
                    db.used = used;
                    try { db.save(false); throw new AssertionError("Used " + used + " accepted"); }
                    catch (IllegalArgumentException expected) { check(expected.getMessage().contains(used), "Explains usage conflict"); }
                    check(db.commits == 0 && db.rollbacks == 1 && db.updates == 0, "Usage rejects entire save before updates");
                }
            }
        }
        Database failure = new Database(LookupType.TRL, 7);
        failure.failSrl = true;
        try { failure.save(false); throw new AssertionError("SQL failure accepted"); }
        catch (SQLException expected) { }
        check(failure.commits == 0 && failure.rollbacks == 1 && failure.autoCommit, "Partial update rolls back");

        Database missing = new Database(LookupType.IRL, 8);
        missing.missing = true;
        try { missing.save(false); throw new AssertionError("Missing/out-of-scope row accepted"); }
        catch (IllegalArgumentException expected) { }
        check(missing.checks == 0 && missing.updates == 0 && missing.rollbacks == 1, "Out-of-scope row cannot affect lookups");

        for (String code : List.of("-", "0")) {
            Database special = new Database(LookupType.IRL, 0);
            special.code = code;
            special.save(false);
            check(special.checks == 1 && special.synchronizedTables.isEmpty(), "Special IRL ordering preserved");
        }
        System.out.println("LookupValueMaintenanceProviderTest passed");
    }

    private static final class Database {
        final LookupType type;
        final int level;
        final Map<LookupType, boolean[]> levels = new EnumMap<>(LookupType.class);
        final Set<String> synchronizedTables = new HashSet<>();
        String code;
        String used;
        boolean missing, failSrl, autoCommit = true;
        int checks, updates, commits, rollbacks;

        Database(LookupType type, int level) {
            this.type = type;
            this.level = level;
            code = Integer.toString(level);
        }

        void save(boolean active) throws SQLException {
            for (LookupType target : List.of(LookupType.TRL, LookupType.IRL, LookupType.SRL)) {
                boolean[] values = new boolean[10];
                Arrays.fill(values, !active);
                levels.put(target, values);
            }
            Connection connection = proxy(Connection.class, (name, args) -> switch (name) {
                case "prepareStatement" -> statement((String) args[0]);
                case "setAutoCommit" -> { autoCommit = (boolean) args[0]; yield null; }
                case "commit" -> { commits++; yield null; }
                case "rollback" -> { rollbacks++; yield null; }
                default -> null;
            });
            // Deliberately forged level/code: persisted values must control validation and synchronization.
            new LookupValueMaintenanceProvider(null).saveLookupRow(connection, 17, 23,
                    new LookupRow(type, 501, 1, "1", "Name", "Description", active, "blue"));
        }

        PreparedStatement statement(String sql) {
            Map<Integer, Object> parameters = new HashMap<>();
            return proxy(PreparedStatement.class, (name, args) -> {
                if (name.startsWith("set")) { parameters.put((int) args[0], args[1]); return null; }
                if (name.equals("executeQuery")) {
                    scope(sql, parameters, 1, 2);
                    if (sql.startsWith("SELECT *")) {
                        check(parameters.get(3).equals(501), "Row id scoped to customer/project");
                        return result(!missing, true);
                    }
                    checks++;
                    check(sql.contains(".Latest = 1") && !sql.contains("Active"), "Latest versions regardless of active state");
                    if (sql.contains("dbo.ENTITY E")) {
                        check(parameters.get(3).equals(EntityType.SYSTEMS_BREAKDOWN.getId()), "TRL only systems");
                        check(parameters.get(4).equals(EntityDataElement.TRLID.getId()), "TRL field only");
                        check(parameters.get(5).equals(level) && sql.contains("EE.IntegerValue >= ?"), "TRL includes selected and higher levels");
                        check(sql.contains("EE.Version = E.Version") && sql.contains("EE.EntityType = E.EntityType"), "Exact entity version joined");
                        return result("TRL".equals(used), false);
                    }
                    check(!sql.contains("EntityType"), "IRL includes every entity type");
                    check(sql.contains("R.CustomerId = I.CustomerId AND R.ProjectId = I.ProjectId AND R.IrlId = I.IrlId"), "IRL uses project-specific logical id");
                    check(parameters.get(3).equals(level > 0 ? level + 1 : code.equals("-") ? 0 : 1), "IRL threshold uses stored code order");
                    check(sql.endsWith(">= ?"), "IRL includes selected and higher levels");
                    return result("IRL".equals(used), false);
                }
                if (name.equals("executeUpdate")) {
                    check(!autoCommit, "Every update inside transaction");
                    updates++;
                    if (sql.startsWith("UPDATE dbo.SRL") && failSrl) throw new SQLException("Simulated write failure");
                    if (sql.contains("SET Active = ?")) {
                        scope(sql, parameters, 2, 3);
                        boolean active = (boolean) parameters.get(1);
                        check(parameters.get(4).equals(level), "Synchronization uses persisted level");
                        check(sql.endsWith(active ? " = ?" : " >= ?"), "Activation exact, deactivation cascading");
                        String table = sql.split(" ")[1].substring(4);
                        synchronizedTables.add(table);
                        for (int candidate = 1; candidate <= 9; candidate++) {
                            if (active ? candidate == level : candidate >= level) levels.get(LookupType.valueOf(table))[candidate] = active;
                        }
                    } else if (sql.contains("SET Active = 0")) {
                        scope(sql, parameters, 1, 2);
                    } else {
                        scope(sql, parameters, 6, 7);
                        check(parameters.get(5).equals(501), "Only requested row metadata updated");
                    }
                    return 1;
                }
                return null;
            });
        }

        ResultSet result(boolean exists, boolean stored) {
            boolean[] first = {true};
            return proxy(ResultSet.class, (name, args) -> switch (name) {
                case "next" -> { boolean next = first[0] && exists; first[0] = false; yield next; }
                case "getInt" -> args[0].toString().endsWith("Level") ? level : 501;
                case "getString" -> args[0].equals("IRLCode") ? code : "Stored text";
                case "getBoolean" -> stored;
                default -> null;
            });
        }
    }

    private static void scope(String sql, Map<Integer, Object> parameters, int customer, int project) {
        check(sql.contains("CustomerId = ?") && sql.contains("ProjectId = ?"), "SQL scopes customer and project");
        check(parameters.get(customer).equals(17) && parameters.get(project).equals(23), "Session scope bound correctly");
    }
    private interface Call { Object invoke(String name, Object[] args) throws Throwable; }
    private static <T> T proxy(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (object, method, args) -> call.invoke(method.getName(), args)));
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
