package com.bepa.eis.common.providers.misc;

import com.bepa.eis.common.scheduling.CleanupJobSettings.Type;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import javax.sql.DataSource;

/** Simulated JDBC validates batching, parameters and strict cutoff without deleting live data. */
public class DataCleanupProviderTest {
    public static void main(String[] args) throws Exception {
        for (Type type : Type.values()) {
            FakeDatabase database = new FakeDatabase(type.defaultDays);
            DataCleanupProvider provider = new DataCleanupProvider(database.source());
            long deleted = provider.deleteExpired(type, type.defaultDays);
            check(deleted == 2501, "All expired rows deleted across batches");
            check(database.rows.size() == 3, "Cutoff equality, newer and null timestamps preserved");
            check(database.deleteCalls == 3, "Three bounded batches");
            check(database.deleteSql.equals("DELETE TOP (1000) FROM [dbo].[" + type.table + "] WHERE [" + type.dateColumn + "] < ?"), "Fixed target and strict age predicate");
            database.fail = true;
            try { provider.deleteExpired(type, type.defaultDays); throw new AssertionError("Deletion failure hidden"); }
            catch (SQLException expected) { }
            for (int invalid : new int[]{0, -1, 36501}) {
                try { provider.deleteExpired(type, invalid); throw new AssertionError("Unsafe retention accepted"); }
                catch (IllegalArgumentException expected) { }
            }
        }
        System.out.println("Data cleanup tests passed.");
    }

    private static class FakeDatabase {
        final Timestamp cutoff;
        final int retention;
        final List<Timestamp> rows = new ArrayList<>();
        String deleteSql;
        int deleteCalls;
        boolean fail;
        FakeDatabase(int retention) {
            this.retention = retention;
            cutoff = Timestamp.valueOf(LocalDateTime.of(2026, 10, 9, 2, 0).minusDays(retention));
            for (int i = 0; i < 2501; i++) rows.add(Timestamp.valueOf(cutoff.toLocalDateTime().minusSeconds(1)));
            rows.add(cutoff); rows.add(Timestamp.valueOf(cutoff.toLocalDateTime().plusSeconds(1))); rows.add(null);
        }
        DataSource source() {
            return proxy(DataSource.class, (name, args) -> name.equals("getConnection") ? connection() : null);
        }
        Connection connection() {
            return proxy(Connection.class, (name, args) -> name.equals("prepareStatement") ? statement((String) args[0]) : null);
        }
        PreparedStatement statement(String sql) {
            return proxy(PreparedStatement.class, (name, args) -> {
                if (name.equals("setInt")) check((Integer) args[1] == retention, "Retention bound as parameter");
                if (name.equals("setTimestamp")) check(cutoff.equals(args[1]), "Stable cutoff reused");
                if (name.equals("executeQuery")) return proxy(ResultSet.class,
                        (method, ignored) -> method.equals("next") ? true : method.equals("getTimestamp") ? cutoff : null);
                if (name.equals("executeUpdate")) {
                    if (fail) throw new SQLException("Simulated deletion failure");
                    deleteSql = sql; deleteCalls++;
                    int count = 0;
                    Iterator<Timestamp> iterator = rows.iterator();
                    while (iterator.hasNext() && count < 1000) {
                        Timestamp row = iterator.next();
                        if (row != null && row.before(cutoff)) { iterator.remove(); count++; }
                    }
                    return count;
                }
                return null;
            });
        }
    }
    private interface Call { Object invoke(String method, Object[] args) throws Throwable; }
    private static <T> T proxy(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (object, method, args) -> call.invoke(method.getName(), args)));
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
