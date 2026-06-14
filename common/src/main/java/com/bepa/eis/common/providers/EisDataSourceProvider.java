package com.bepa.eis.common.providers;

import com.bepa.eis.common.GlobalConfiguration;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public final class EisDataSourceProvider {

    private static volatile DataSource jdbcDataSource;

    private EisDataSourceProvider() {
    }

    public static DataSource getDataSource() {
        if (GlobalConfiguration.isDatabaseJdbcMode()) {
            return getJdbcDataSource();
        }

        if (GlobalConfiguration.isDatabaseJndiMode()) {
            return getJndiDataSource();
        }

        throw new IllegalStateException(
                "Unsupported database datasource mode: " + GlobalConfiguration.getDatabaseDatasourceMode()
                        + ". Expected 'jndi' or 'jdbc'."
        );
    }

    private static DataSource getJndiDataSource() {
        try {
            InitialContext context = new InitialContext();

            return (DataSource) context.lookup(GlobalConfiguration.getJndiName());
        } catch (NamingException e) {
            throw new IllegalStateException(
                    "Failed to lookup DataSource via JNDI name: " + GlobalConfiguration.getJndiName(),
                    e
            );
        }
    }

    private static DataSource getJdbcDataSource() {
        DataSource currentDataSource = jdbcDataSource;

        if (currentDataSource != null) {
            return currentDataSource;
        }

        synchronized (EisDataSourceProvider.class) {
            currentDataSource = jdbcDataSource;

            if (currentDataSource != null) {
                return currentDataSource;
            }

            jdbcDataSource = createSqlServerDataSource();

            return jdbcDataSource;
        }
    }

    private static DataSource createSqlServerDataSource() {
        String jdbcUrl = GlobalConfiguration.getDatabaseJdbcUrl();
        String username = GlobalConfiguration.getDatabaseJdbcUsername();
        String password = GlobalConfiguration.getDatabaseJdbcPassword();

        if (isBlank(jdbcUrl)) {
            throw new IllegalStateException(
                    "Database datasource mode is jdbc, but database.jdbc.url is empty for application: "
                            + GlobalConfiguration.getApplicationName()
            );
        }

        if (isBlank(username)) {
            throw new IllegalStateException(
                    "Database datasource mode is jdbc, but database.jdbc.username is empty for application: "
                            + GlobalConfiguration.getApplicationName()
            );
        }

        SQLServerDataSource dataSource = new SQLServerDataSource();

        dataSource.setURL(jdbcUrl);
        dataSource.setUser(username);
        dataSource.setPassword(password);

        return dataSource;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}