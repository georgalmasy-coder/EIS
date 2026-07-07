package dk.eis.tech.timesheet.data;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public final class Database {

    private static volatile DataSource dataSource;

    private Database() {
    }

    public static Connection connection() throws SQLException {
        try {
            return dataSource().getConnection();
        } catch (SQLException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SQLException("Unable to open JNDI connection", ex);
        }
    }

    private static DataSource dataSource() throws NamingException {
        DataSource cached = dataSource;
        if (cached != null) {
            return cached;
        }
        synchronized (Database.class) {
            if (dataSource == null) {
                dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/TimesheetDB");
            }
            return dataSource;
        }
    }
}
