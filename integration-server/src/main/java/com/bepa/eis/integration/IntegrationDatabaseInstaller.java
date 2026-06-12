package com.bepa.eis.integration;

import com.bepa.eis.common.GlobalConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class IntegrationDatabaseInstaller {

    private static final Logger log = LoggerFactory.getLogger(IntegrationDatabaseInstaller.class);

    private static final String[] INSTALLATION_SCRIPTS = {
            "com/bepa/eis/common/XX-create-mail-queue-table.sql",
            "com/bepa/eis/common/XX-create-customer-master-tables.sql",
            "com/bepa/eis/common/XX-create-customer-workflow-tables.sql"
    };

    public IntegrationDatabaseInstaller() {
    }

    public void installIfAvailable() {
        DataSource dataSource = getDataSource();

        for (String script : INSTALLATION_SCRIPTS) {
            installScriptIfAvailable(
                    dataSource,
                    script
            );
        }
    }

    private void installScriptIfAvailable(
            DataSource dataSource,
            String resourceName
    ) {
        String scriptContent = readResource(resourceName);

        if (scriptContent == null || scriptContent.trim().isEmpty()) {
            log.info("Database installation script not found on classpath: {}", resourceName);
            return;
        }

        List<String> batches = splitSqlBatches(scriptContent);

        if (batches.isEmpty()) {
            log.info("Database installation script contained no executable statements: {}", resourceName);
            return;
        }

        log.info("Executing database installation script: {}", resourceName);

        try (Connection connection = dataSource.getConnection()) {
            for (String batch : batches) {
                executeBatch(
                        connection,
                        batch,
                        resourceName
                );
            }

            log.info("Database installation script completed: {}", resourceName);
        } catch (SQLException e) {
            log.error("Database installation script failed: {}", resourceName, e);
        }
    }

    private void executeBatch(
            Connection connection,
            String batch,
            String resourceName
    ) {
        if (batch == null || batch.trim().isEmpty()) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute(batch);
        } catch (SQLException e) {
            log.error(
                    "Failed to execute SQL batch from script {}. Batch: {}",
                    resourceName,
                    truncate(batch, 1000),
                    e
            );
        }
    }

    private String readResource(String resourceName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        InputStream inputStream = classLoader == null
                ? IntegrationDatabaseInstaller.class.getClassLoader().getResourceAsStream(resourceName)
                : classLoader.getResourceAsStream(resourceName);

        if (inputStream == null) {
            inputStream = IntegrationDatabaseInstaller.class.getClassLoader().getResourceAsStream(resourceName);
        }

        if (inputStream == null) {
            return null;
        }

        StringBuilder content = new StringBuilder();

        try (
                InputStream autoCloseInputStream = inputStream;
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(autoCloseInputStream, StandardCharsets.UTF_8)
                )
        ) {
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            log.error("Could not read database installation script: {}", resourceName, e);
            return null;
        }

        return content.toString();
    }

    private List<String> splitSqlBatches(String scriptContent) {
        List<String> batches = new ArrayList<>();

        if (scriptContent == null || scriptContent.trim().isEmpty()) {
            return batches;
        }

        String[] lines = normalizeLineEndings(scriptContent).split("\n");
        StringBuilder currentBatch = new StringBuilder();

        for (String line : lines) {
            if ("GO".equalsIgnoreCase(line.trim())) {
                addBatch(
                        batches,
                        currentBatch
                );
                currentBatch.setLength(0);
                continue;
            }

            currentBatch.append(line).append("\n");
        }

        addBatch(
                batches,
                currentBatch
        );

        return batches;
    }

    private void addBatch(
            List<String> batches,
            StringBuilder batch
    ) {
        if (batch == null) {
            return;
        }

        String sql = batch.toString().trim();

        if (!sql.isEmpty()) {
            batches.add(sql);
        }
    }

    private String normalizeLineEndings(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\r\n", "\n")
                .replace("\r", "\n");
    }

    private DataSource getDataSource() {
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

    private String truncate(
            String value,
            int maxLength
    ) {
        if (value == null) {
            return "";
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }
}