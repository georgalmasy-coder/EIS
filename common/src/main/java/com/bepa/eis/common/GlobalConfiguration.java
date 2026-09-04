package com.bepa.eis.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

public final class GlobalConfiguration {

    private static final String CONFIG_FILE_NAME = "eis-global.properties";

    private static final String CONFIG_FILE_SYSTEM_PROPERTY = "eis.config.file";
    private static final String CONFIG_FILE_ENVIRONMENT_VARIABLE = "EIS_CONFIG_FILE";
    private static final String CONFIG_DIR_SYSTEM_PROPERTY = "eis.config.dir";
    private static final String CONFIG_DIR_ENVIRONMENT_VARIABLE = "EIS_CONFIG_DIR";

    private static final String APPLICATION_NAME_SYSTEM_PROPERTY = "eis.application.name";
    private static final String APPLICATION_NAME_ENVIRONMENT_VARIABLE = "EIS_APPLICATION_NAME";

    private static final String DEFAULT_APPLICATION_NAME = "web-server";
    private static final String INTEGRATION_SERVER_APPLICATION_NAME = "integration-server";
    private static final String WEB_SERVER_APPLICATION_NAME = "web-server";

    private static final String DEFAULT_EIS_HOME = "C:/EIS";
    private static final String DEFAULT_EIS_CONF_DIR = "C:/EIS/conf";
    private static final String DEFAULT_EIS_LOGS_DIR = "C:/EIS/logs";

    private static final boolean DEFAULT_UDV_MODE = false;
    private static final int DEFAULT_CUSTOMER_ID = 1;
    private static final int DEFAULT_PROJECT_ID = 1;
    private static final String DEFAULT_LOGIN_PAGE = "index.html";
    private static final int DEFAULT_SESSION_TIMEOUT_MINUTES = 30;

    private static final String DEFAULT_DATABASE_DATASOURCE_MODE = "jndi";
    private static final String DEFAULT_JNDI_NAME = "java:comp/env/jdbc/EISDatabase";
    private static final String DEFAULT_DATABASE_JDBC_URL = "";
    private static final String DEFAULT_DATABASE_JDBC_USERNAME = "";
    private static final String DEFAULT_DATABASE_JDBC_PASSWORD = "";

    private static final boolean DEFAULT_MAIL_QUEUE_JOB_ENABLED = true;
    private static final int DEFAULT_MAIL_QUEUE_JOB_INTERVAL_SECONDS = 60;
    private static final int DEFAULT_MAIL_QUEUE_BATCH_SIZE = 25;
    private static final int DEFAULT_MAIL_QUEUE_RETRY_DELAY_MINUTES = 60;
    private static final int DEFAULT_MAIL_QUEUE_MAX_ATTEMPTS = 5;
    private static final int DEFAULT_MAIL_QUEUE_STUCK_SENDING_TIMEOUT_MINUTES = 15;

    private static final boolean DEFAULT_CUSTOMER_WORKFLOW_JOB_ENABLED = true;
    private static final int DEFAULT_CUSTOMER_WORKFLOW_JOB_INTERVAL_SECONDS = 600;
    private static final String DEFAULT_CUSTOMER_WORKFLOW_PORTAL_BASE_URL = "http://localhost:8080";
    private static final int DEFAULT_CUSTOMER_WORKFLOW_TRIAL_DAYS = 14;
    private static final int DEFAULT_CUSTOMER_WORKFLOW_TRIAL_REMINDER_DAYS_BEFORE_EXPIRY = 2;
    private static final int DEFAULT_CUSTOMER_WORKFLOW_PAYMENT_GRACE_PERIOD_DAYS = 14;
    private static final int DEFAULT_CUSTOMER_WORKFLOW_CONFIRMATION_TOKEN_VALID_DAYS = 7;
    private static final int DEFAULT_CUSTOMER_WORKFLOW_PAYMENT_TOKEN_VALID_DAYS = 14;
    private static final int DEFAULT_CUSTOMER_WORKFLOW_REACTIVATION_TOKEN_VALID_DAYS = 14;
    private static final int DEFAULT_CUSTOMER_WORKFLOW_SUBSCRIPTION_RENEWAL_REMINDER_DAYS_BEFORE_EXPIRY = 14;

    private static final boolean DEFAULT_CUSTOMER_REGISTRATION_CVR_LOOKUP_ENABLED = true;

    private static final boolean DEFAULT_VIES_VALIDATION_ENABLED = true;
    private static final int DEFAULT_VIES_VALIDATION_TIMEOUT_SECONDS = 60;

    private static final String DEFAULT_MAIL_SMTP_HOST = "localhost";
    private static final int DEFAULT_MAIL_SMTP_PORT = 25;
    private static final String DEFAULT_MAIL_SMTP_USERNAME = "";
    private static final String DEFAULT_MAIL_SMTP_PASSWORD = "";
    private static final boolean DEFAULT_MAIL_SMTP_AUTH = false;
    private static final boolean DEFAULT_MAIL_SMTP_STARTTLS = false;
    private static final boolean DEFAULT_MAIL_SMTP_SSL = false;
    private static final int DEFAULT_MAIL_SMTP_CONNECTION_TIMEOUT_MS = 10000;
    private static final int DEFAULT_MAIL_SMTP_READ_TIMEOUT_MS = 10000;

    private static final String DEFAULT_MAIL_FROM_EMAIL = "no-reply@example.com";
    private static final String DEFAULT_MAIL_FROM_NAME = "BEPA EIS";
    private static final String DEFAULT_MAIL_TEMPLATE_FOLDER = "C:/EIS/conf/mail-templates";
    private static final String DEFAULT_MAIL_CONTENT_TYPE = "text/html; charset=UTF-8";

    private static final long FILE_LISTENER_INTERVAL_MILLIS = 5000L;

    private static final AtomicReference<Properties> PROPERTIES = new AtomicReference<>(new Properties());
    private static volatile String currentThemeName = "LIGHT";

    private static final File CONFIG_FILE = resolveConfigurationFile();

    private static volatile long lastKnownModified = -1L;
    private static volatile boolean fileListenerStarted = false;

    static {
        ensureConfigurationFileExists();
        reload();
        startFileListener();
    }

    private GlobalConfiguration() {
    }

    public static String getApplicationName() {
        String configuredApplicationName = System.getProperty(APPLICATION_NAME_SYSTEM_PROPERTY);

        if (!isEmpty(configuredApplicationName)) {
            return configuredApplicationName.trim();
        }

        configuredApplicationName = System.getenv(APPLICATION_NAME_ENVIRONMENT_VARIABLE);

        if (!isEmpty(configuredApplicationName)) {
            return configuredApplicationName.trim();
        }

        return getString("eis.application.name", DEFAULT_APPLICATION_NAME);
    }

    public static boolean isIntegrationServerApplication() {
        return INTEGRATION_SERVER_APPLICATION_NAME.equalsIgnoreCase(getApplicationName());
    }

    public static boolean isWebServerApplication() {
        return WEB_SERVER_APPLICATION_NAME.equalsIgnoreCase(getApplicationName());
    }

    public static String getCurrentThemeName() {
        return currentThemeName;
    }

    public static synchronized void setCurrentThemeName(String themeName) {
        if (isEmpty(themeName)) {
            currentThemeName = "LIGHT";
            return;
        }

        currentThemeName = themeName.trim().toUpperCase(Locale.ENGLISH);
    }

    public static int getThemeId() {
        return getInt("ui.theme.id", 1, 1, 3);
    }

    public static String getLoginPage() {
        return getString("ui.login.page", DEFAULT_LOGIN_PAGE);
    }

    public static int getSessionTimeoutMinutes() {
        return getInt("security.session.timeout.minutes", DEFAULT_SESSION_TIMEOUT_MINUTES, 1, 1440);
    }

    public static String getEisHome() {
        return getString("eis.home", DEFAULT_EIS_HOME);
    }

    public static File getEisHomeDirectory() {
        return new File(getEisHome());
    }

    public static String getEisConfigurationDirectoryPath() {
        return getString("eis.conf.dir", DEFAULT_EIS_CONF_DIR);
    }

    public static File getEisConfigurationDirectory() {
        return new File(getEisConfigurationDirectoryPath());
    }

    public static String getEisLogsDirectoryPath() {
        return getString("eis.logs.dir", DEFAULT_EIS_LOGS_DIR);
    }

    public static File getEisLogsDirectory() {
        return new File(getEisLogsDirectoryPath());
    }

    public static boolean isUdvMode() {
        return getBoolean("udv.mode", DEFAULT_UDV_MODE);
    }

    public static int getDefaultCustomerId() {
        return getInt("udv.customerid", DEFAULT_CUSTOMER_ID, 1, 100);
    }

    public static int getDefaultProjectId() {
        return getInt("udv.projectid", DEFAULT_PROJECT_ID, 1, 100);
    }

    public static String getJndiName() {
        return getApplicationSpecificString(
                "database.jndi.name",
                DEFAULT_JNDI_NAME
        );
    }

    public static String getDatabaseDatasourceMode() {
        return getApplicationSpecificString(
                "database.datasource.mode",
                DEFAULT_DATABASE_DATASOURCE_MODE
        );
    }

    public static String getDatabaseJdbcUrl() {
        return getApplicationSpecificString(
                "database.jdbc.url",
                DEFAULT_DATABASE_JDBC_URL
        );
    }

    public static String getDatabaseJdbcUsername() {
        return getApplicationSpecificString(
                "database.jdbc.username",
                DEFAULT_DATABASE_JDBC_USERNAME
        );
    }

    public static String getDatabaseJdbcPassword() {
        return getApplicationSpecificString(
                "database.jdbc.password",
                DEFAULT_DATABASE_JDBC_PASSWORD
        );
    }

    public static boolean isDatabaseJdbcMode() {
        return "jdbc".equalsIgnoreCase(getDatabaseDatasourceMode());
    }

    public static boolean isDatabaseJndiMode() {
        return "jndi".equalsIgnoreCase(getDatabaseDatasourceMode());
    }

    public static boolean isMailQueueJobEnabled() {
        return getBoolean("mail.queue.job.enabled", DEFAULT_MAIL_QUEUE_JOB_ENABLED);
    }

    public static int getMailQueueJobIntervalSeconds() {
        return getInt("mail.queue.job.interval.seconds", DEFAULT_MAIL_QUEUE_JOB_INTERVAL_SECONDS, 10, 86400);
    }

    public static int getMailQueueBatchSize() {
        return getInt("mail.queue.batch.size", DEFAULT_MAIL_QUEUE_BATCH_SIZE, 1, 500);
    }

    public static int getMailQueueRetryDelayMinutes() {
        return getInt("mail.queue.retry.delay.minutes", DEFAULT_MAIL_QUEUE_RETRY_DELAY_MINUTES, 1, 1440);
    }

    public static int getMailQueueMaxAttempts() {
        return getInt("mail.queue.max.attempts", DEFAULT_MAIL_QUEUE_MAX_ATTEMPTS, 1, 50);
    }

    public static int getMailQueueStuckSendingTimeoutMinutes() {
        return getInt("mail.queue.stuck.sending.timeout.minutes", DEFAULT_MAIL_QUEUE_STUCK_SENDING_TIMEOUT_MINUTES, 1, 1440);
    }

    public static boolean isCustomerWorkflowJobEnabled() {
        return getBoolean("customer.workflow.job.enabled", DEFAULT_CUSTOMER_WORKFLOW_JOB_ENABLED);
    }

    public static int getCustomerWorkflowJobIntervalSeconds() {
        return getInt("customer.workflow.job.interval.seconds", DEFAULT_CUSTOMER_WORKFLOW_JOB_INTERVAL_SECONDS, 60, 86400);
    }

    public static String getCustomerWorkflowPortalBaseUrl() {
        return getString("customer.workflow.portal.base.url", DEFAULT_CUSTOMER_WORKFLOW_PORTAL_BASE_URL);
    }

    public static int getCustomerWorkflowTrialDays() {
        return getInt("customer.workflow.trial.days", DEFAULT_CUSTOMER_WORKFLOW_TRIAL_DAYS, 1, 365);
    }

    public static int getCustomerWorkflowTrialReminderDaysBeforeExpiry() {
        return getInt("customer.workflow.trial.reminder.days.before.expiry", DEFAULT_CUSTOMER_WORKFLOW_TRIAL_REMINDER_DAYS_BEFORE_EXPIRY, 0, 365);
    }

    public static int getCustomerWorkflowPaymentGracePeriodDays() {
        return getInt("customer.workflow.payment.grace.period.days", DEFAULT_CUSTOMER_WORKFLOW_PAYMENT_GRACE_PERIOD_DAYS, 0, 365);
    }

    public static int getCustomerWorkflowConfirmationTokenValidDays() {
        return getInt("customer.workflow.confirmation.token.valid.days", DEFAULT_CUSTOMER_WORKFLOW_CONFIRMATION_TOKEN_VALID_DAYS, 1, 365);
    }

    public static int getCustomerWorkflowPaymentTokenValidDays() {
        return getInt("customer.workflow.payment.token.valid.days", DEFAULT_CUSTOMER_WORKFLOW_PAYMENT_TOKEN_VALID_DAYS, 1, 365);
    }

    public static int getCustomerWorkflowReactivationTokenValidDays() {
        return getInt("customer.workflow.reactivation.token.valid.days", DEFAULT_CUSTOMER_WORKFLOW_REACTIVATION_TOKEN_VALID_DAYS, 1, 365);
    }

    public static int getCustomerWorkflowSubscriptionRenewalReminderDaysBeforeExpiry() {
        return getInt("customer.workflow.subscription.renewal.reminder.days.before.expiry", DEFAULT_CUSTOMER_WORKFLOW_SUBSCRIPTION_RENEWAL_REMINDER_DAYS_BEFORE_EXPIRY, 0, 365);
    }

    public static boolean isCustomerRegistrationCvrLookupEnabled() {
        return getBoolean("customer.registration.cvr.lookup.enabled", DEFAULT_CUSTOMER_REGISTRATION_CVR_LOOKUP_ENABLED);
    }

    public static boolean isViesValidationEnabled() {
        return getBoolean("vies.validation.enabled", DEFAULT_VIES_VALIDATION_ENABLED);
    }

    public static int getViesValidationTimeoutSeconds() {
        return getInt("vies.validation.timeout.seconds", DEFAULT_VIES_VALIDATION_TIMEOUT_SECONDS, 1, 300);
    }

    public static String getMailSmtpHost() {
        return getString("mail.smtp.host", DEFAULT_MAIL_SMTP_HOST);
    }

    public static int getMailSmtpPort() {
        return getInt("mail.smtp.port", DEFAULT_MAIL_SMTP_PORT, 1, 65535);
    }

    public static String getMailSmtpUsername() {
        return getString("mail.smtp.username", DEFAULT_MAIL_SMTP_USERNAME);
    }

    public static String getMailSmtpPassword() {
        return getString("mail.smtp.password", DEFAULT_MAIL_SMTP_PASSWORD);
    }

    public static boolean isMailSmtpAuthEnabled() {
        return getBoolean("mail.smtp.auth", DEFAULT_MAIL_SMTP_AUTH);
    }

    public static boolean isMailSmtpStartTlsEnabled() {
        return getBoolean("mail.smtp.starttls", DEFAULT_MAIL_SMTP_STARTTLS);
    }

    public static boolean isMailSmtpSslEnabled() {
        return getBoolean("mail.smtp.ssl", DEFAULT_MAIL_SMTP_SSL);
    }

    public static int getMailSmtpConnectionTimeoutMillis() {
        return getInt("mail.smtp.connection.timeout.ms", DEFAULT_MAIL_SMTP_CONNECTION_TIMEOUT_MS, 1000, 120000);
    }

    public static int getMailSmtpReadTimeoutMillis() {
        return getInt("mail.smtp.read.timeout.ms", DEFAULT_MAIL_SMTP_READ_TIMEOUT_MS, 1000, 120000);
    }

    public static String getMailDefaultFromEmail() {
        return getString("mail.default.from.email", DEFAULT_MAIL_FROM_EMAIL);
    }

    public static String getMailDefaultFromName() {
        return getString("mail.default.from.name", DEFAULT_MAIL_FROM_NAME);
    }

    public static String getMailTemplateFolder() {
        return getString("mail.template.folder", DEFAULT_MAIL_TEMPLATE_FOLDER);
    }

    public static File getMailTemplateDirectory() {
        return resolveConfigurationRelativeFile(getMailTemplateFolder());
    }

    public static String getMailDefaultContentType() {
        return getString("mail.default.content.type", DEFAULT_MAIL_CONTENT_TYPE);
    }

    public static File getConfigurationFile() {
        return CONFIG_FILE;
    }

    public static File getConfigurationDirectory() {
        File parentDirectory = CONFIG_FILE.getParentFile();

        if (parentDirectory != null) {
            return parentDirectory;
        }

        return new File(".");
    }

    public static File resolveConfigurationRelativeFile(String path) {
        if (isEmpty(path)) {
            return getConfigurationDirectory();
        }

        File file = new File(path);

        if (file.isAbsolute()) {
            return file;
        }

        return new File(getConfigurationDirectory(), path);
    }

    public static synchronized void reload() {
        ensureConfigurationFileExists();

        Properties loadedProperties = new Properties();

        if (CONFIG_FILE.isFile()) {
            InputStream inputStream = null;

            try {
                inputStream = new FileInputStream(CONFIG_FILE);
                loadedProperties.load(inputStream);
                lastKnownModified = CONFIG_FILE.lastModified();
            } catch (IOException ignored) {
                loadedProperties = new Properties();
            } finally {
                closeQuietly(inputStream);
            }
        } else {
            lastKnownModified = -1L;
        }

        PROPERTIES.set(loadedProperties);
    }

    public static String getString(String key, String defaultValue) {
        String value = getProperties().getProperty(key);

        if (isEmpty(value)) {
            return defaultValue;
        }

        return value.trim();
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = getProperties().getProperty(key);

        if (isEmpty(value)) {
            return defaultValue;
        }

        String normalizedValue = value.trim().toLowerCase(Locale.ENGLISH);

        if ("true".equals(normalizedValue)
                || "yes".equals(normalizedValue)
                || "1".equals(normalizedValue)
                || "on".equals(normalizedValue)) {
            return true;
        }

        if ("false".equals(normalizedValue)
                || "no".equals(normalizedValue)
                || "0".equals(normalizedValue)
                || "off".equals(normalizedValue)) {
            return false;
        }

        return defaultValue;
    }

    public static int getInt(String key, int defaultValue, int minValue, int maxValue) {
        String value = getProperties().getProperty(key);

        if (isEmpty(value)) {
            return defaultValue;
        }

        try {
            int parsedValue = Integer.parseInt(value.trim());

            if (parsedValue < minValue || parsedValue > maxValue) {
                return defaultValue;
            }

            return parsedValue;
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    public static <T extends Enum<T>> T getEnum(String key, Class<T> enumType, T defaultValue) {
        String value = getProperties().getProperty(key);

        if (isEmpty(value)) {
            return defaultValue;
        }

        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException ignored) {
            return defaultValue;
        }
    }

    private static String getApplicationSpecificString(String baseKey, String defaultValue) {
        String applicationName = getApplicationName();

        if (!isEmpty(applicationName)) {
            String applicationSpecificKey = applicationName + "." + baseKey;
            String applicationSpecificValue = getProperties().getProperty(applicationSpecificKey);

            if (!isEmpty(applicationSpecificValue)) {
                return applicationSpecificValue.trim();
            }
        }

        return getString(baseKey, defaultValue);
    }

    private static Properties getProperties() {
        return PROPERTIES.get();
    }

    private static File resolveConfigurationFile() {
        String configuredFile = System.getProperty(CONFIG_FILE_SYSTEM_PROPERTY);

        if (!isEmpty(configuredFile)) {
            return new File(configuredFile.trim());
        }

        configuredFile = System.getenv(CONFIG_FILE_ENVIRONMENT_VARIABLE);

        if (!isEmpty(configuredFile)) {
            return new File(configuredFile.trim());
        }

        String configuredDirectory = System.getProperty(CONFIG_DIR_SYSTEM_PROPERTY);

        if (!isEmpty(configuredDirectory)) {
            return new File(configuredDirectory.trim(), CONFIG_FILE_NAME);
        }

        configuredDirectory = System.getenv(CONFIG_DIR_ENVIRONMENT_VARIABLE);

        if (!isEmpty(configuredDirectory)) {
            return new File(configuredDirectory.trim(), CONFIG_FILE_NAME);
        }

        File tomcatConfDirectory = resolveTomcatConfDirectory();

        if (tomcatConfDirectory != null) {
            return new File(tomcatConfDirectory, CONFIG_FILE_NAME);
        }

        return new File("conf", CONFIG_FILE_NAME);
    }

    private static File resolveTomcatConfDirectory() {
        String catalinaBase = System.getProperty("catalina.base");

        if (!isEmpty(catalinaBase)) {
            return new File(catalinaBase, "conf");
        }

        String catalinaHome = System.getProperty("catalina.home");

        if (!isEmpty(catalinaHome)) {
            return new File(catalinaHome, "conf");
        }

        return null;
    }

    private static synchronized void ensureConfigurationFileExists() {
        if (CONFIG_FILE.isFile()) {
            return;
        }

        File parentDirectory = CONFIG_FILE.getParentFile();

        if (parentDirectory != null && !parentDirectory.isDirectory()) {
            parentDirectory.mkdirs();
        }

        OutputStream outputStream = null;

        try {
            outputStream = new FileOutputStream(CONFIG_FILE);
            outputStream.write(getDefaultConfigurationFileContent().getBytes("ISO-8859-1"));
            outputStream.flush();
            lastKnownModified = CONFIG_FILE.lastModified();
        } catch (IOException ignored) {
            // If the file cannot be created, defaults from code will still be used.
        } finally {
            closeQuietly(outputStream);
        }
    }

    private static synchronized void startFileListener() {
        if (fileListenerStarted) {
            return;
        }

        fileListenerStarted = true;

        Thread listenerThread = new Thread(
                new Runnable() {
                    public void run() {
                        watchConfigurationFile();
                    }
                },
                "eis-global-configuration-listener"
        );

        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private static void watchConfigurationFile() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                long currentModified;

                if (CONFIG_FILE.isFile()) {
                    currentModified = CONFIG_FILE.lastModified();
                } else {
                    ensureConfigurationFileExists();

                    if (CONFIG_FILE.isFile()) {
                        currentModified = CONFIG_FILE.lastModified();
                    } else {
                        currentModified = -1L;
                    }
                }

                if (currentModified != lastKnownModified) {
                    reload();
                }

                Thread.sleep(FILE_LISTENER_INTERVAL_MILLIS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException ignored) {
                // Configuration reload is best effort.
            }
        }
    }

    private static String getDefaultConfigurationFileContent() {
        String lineSeparator = System.getProperty("line.separator");
        StringBuilder content = new StringBuilder();

        content.append("###############################################################################").append(lineSeparator);
        content.append("# EIS Global Configuration").append(lineSeparator);
        content.append("###############################################################################").append(lineSeparator);
        content.append(lineSeparator);

        appendGeneralConfiguration(content, lineSeparator);
        appendPathConfiguration(content, lineSeparator);
        appendDatabaseConfiguration(content, lineSeparator);
        appendMailConfiguration(content, lineSeparator);
        appendCustomerRegistrationConfiguration(content, lineSeparator);
        appendCustomerWorkflowConfiguration(content, lineSeparator);
        appendViesConfiguration(content, lineSeparator);
        appendMfaConfiguration(content, lineSeparator);

        return content.toString();
    }

    private static void appendGeneralConfiguration(StringBuilder content, String lineSeparator) {
        content.append("###############################################################################").append(lineSeparator);
        content.append("# General configuration").append(lineSeparator);
        content.append("###############################################################################").append(lineSeparator);
        content.append(lineSeparator);
        content.append("eis.application.name=web-server").append(lineSeparator);
        content.append(lineSeparator);
        content.append("udv.mode=false").append(lineSeparator);
        content.append("udv.customerid=1").append(lineSeparator);
        content.append("udv.projectid=1").append(lineSeparator);
        content.append("ui.theme.id=1").append(lineSeparator);
        content.append(lineSeparator);
    }

    private static void appendPathConfiguration(StringBuilder content, String lineSeparator) {
        content.append("###############################################################################").append(lineSeparator);
        content.append("# Path configuration").append(lineSeparator);
        content.append("###############################################################################").append(lineSeparator);
        content.append(lineSeparator);
        content.append("eis.home=C:/EIS").append(lineSeparator);
        content.append("eis.conf.dir=C:/EIS/conf").append(lineSeparator);
        content.append("eis.logs.dir=C:/EIS/logs").append(lineSeparator);
        content.append(lineSeparator);
    }

    private static void appendDatabaseConfiguration(StringBuilder content, String lineSeparator) {
        content.append("###############################################################################").append(lineSeparator);
        content.append("# Database configuration").append(lineSeparator);
        content.append("###############################################################################").append(lineSeparator);
        content.append(lineSeparator);

        content.append("# Legacy fallback").append(lineSeparator);
        content.append("database.datasource.mode=jndi").append(lineSeparator);
        content.append("database.jndi.name=java:comp/env/jdbc/EISDatabase").append(lineSeparator);
        content.append("database.jdbc.url=").append(lineSeparator);
        content.append("database.jdbc.username=").append(lineSeparator);
        content.append("database.jdbc.password=").append(lineSeparator);
        content.append(lineSeparator);

        content.append("# Web server database configuration").append(lineSeparator);
        content.append("web-server.database.datasource.mode=jndi").append(lineSeparator);
        content.append("web-server.database.jndi.name=java:comp/env/jdbc/EISDatabase").append(lineSeparator);
        content.append("web-server.database.jdbc.url=").append(lineSeparator);
        content.append("web-server.database.jdbc.username=").append(lineSeparator);
        content.append("web-server.database.jdbc.password=").append(lineSeparator);
        content.append(lineSeparator);

        content.append("# Integration server database configuration").append(lineSeparator);
        content.append("integration-server.database.datasource.mode=jdbc").append(lineSeparator);
        content.append("integration-server.database.jdbc.url=jdbc:sqlserver://localhost:1433;databaseName=EIS;encrypt=true;trustServerCertificate=true").append(lineSeparator);
        content.append("integration-server.database.jdbc.username=CHANGE_ME").append(lineSeparator);
        content.append("integration-server.database.jdbc.password=CHANGE_ME").append(lineSeparator);
        content.append(lineSeparator);
    }

    private static void appendMailConfiguration(StringBuilder content, String lineSeparator) {
        content.append("###############################################################################").append(lineSeparator);
        content.append("# Mail queue configuration").append(lineSeparator);
        content.append("###############################################################################").append(lineSeparator);
        content.append(lineSeparator);
        content.append("mail.queue.job.enabled=true").append(lineSeparator);
        content.append("mail.queue.job.interval.seconds=60").append(lineSeparator);
        content.append("mail.queue.batch.size=25").append(lineSeparator);
        content.append("mail.queue.retry.delay.minutes=60").append(lineSeparator);
        content.append("mail.queue.max.attempts=5").append(lineSeparator);
        content.append("mail.queue.stuck.sending.timeout.minutes=15").append(lineSeparator);
        content.append(lineSeparator);

        content.append("###############################################################################").append(lineSeparator);
        content.append("# SMTP configuration").append(lineSeparator);
        content.append("###############################################################################").append(lineSeparator);
        content.append(lineSeparator);
        content.append("mail.smtp.host=localhost").append(lineSeparator);
        content.append("mail.smtp.port=25").append(lineSeparator);
        content.append("mail.smtp.username=").append(lineSeparator);
        content.append("mail.smtp.password=").append(lineSeparator);
        content.append("mail.smtp.auth=false").append(lineSeparator);
        content.append("mail.smtp.starttls=false").append(lineSeparator);
        content.append("mail.smtp.ssl=false").append(lineSeparator);
        content.append("mail.smtp.connection.timeout.ms=10000").append(lineSeparator);
        content.append("mail.smtp.read.timeout.ms=10000").append(lineSeparator);
        content.append(lineSeparator);
        content.append("mail.default.from.email=no-reply@example.com").append(lineSeparator);
        content.append("mail.default.from.name=BEPA EIS").append(lineSeparator);
        content.append("mail.template.folder=C:/EIS/conf/mail-templates").append(lineSeparator);
        content.append("mail.default.content.type=text/html; charset=UTF-8").append(lineSeparator);
        content.append(lineSeparator);
    }

    private static void appendCustomerRegistrationConfiguration(StringBuilder content, String lineSeparator) {
        content.append("###############################################################################").append(lineSeparator);
        content.append("# Customer registration configuration").append(lineSeparator);
        content.append("###############################################################################").append(lineSeparator);
        content.append(lineSeparator);
        content.append("customer.registration.cvr.lookup.enabled=true").append(lineSeparator);
        content.append(lineSeparator);
    }

    private static void appendCustomerWorkflowConfiguration(StringBuilder content, String lineSeparator) {
        content.append("###############################################################################").append(lineSeparator);
        content.append("# Customer workflow configuration").append(lineSeparator);
        content.append("###############################################################################").append(lineSeparator);
        content.append(lineSeparator);
        content.append("customer.workflow.job.enabled=true").append(lineSeparator);
        content.append("customer.workflow.job.interval.seconds=600").append(lineSeparator);
        content.append("customer.workflow.portal.base.url=http://localhost:8080").append(lineSeparator);
        content.append("customer.workflow.trial.days=14").append(lineSeparator);
        content.append("customer.workflow.trial.reminder.days.before.expiry=2").append(lineSeparator);
        content.append("customer.workflow.payment.grace.period.days=14").append(lineSeparator);
        content.append("customer.workflow.confirmation.token.valid.days=7").append(lineSeparator);
        content.append("customer.workflow.payment.token.valid.days=14").append(lineSeparator);
        content.append("customer.workflow.reactivation.token.valid.days=14").append(lineSeparator);
        content.append("customer.workflow.subscription.renewal.reminder.days.before.expiry=14").append(lineSeparator);
        content.append(lineSeparator);
    }

    private static void appendViesConfiguration(StringBuilder content, String lineSeparator) {
        content.append("###############################################################################").append(lineSeparator);
        content.append("# VIES validation configuration").append(lineSeparator);
        content.append("###############################################################################").append(lineSeparator);
        content.append(lineSeparator);
        content.append("vies.validation.enabled=true").append(lineSeparator);
        content.append("vies.validation.timeout.seconds=60").append(lineSeparator);
        content.append(lineSeparator);
    }

    private static void appendMfaConfiguration(StringBuilder content, String lineSeparator) {
        content.append("###############################################################################").append(lineSeparator);
        content.append("# MFA configuration").append(lineSeparator);
        content.append("###############################################################################").append(lineSeparator);
        content.append(lineSeparator);
        content.append("mfa.issuer=BEPA EIS").append(lineSeparator);
        content.append("mfa.issuer.dev=BEPA EIS DEV").append(lineSeparator);
        content.append("mfa.mode=OPTIONAL").append(lineSeparator);
        content.append("mfa.code.length=6").append(lineSeparator);
        content.append("mfa.code.valid.seconds=30").append(lineSeparator);
        content.append("mfa.pre.auth.token.valid.minutes=5").append(lineSeparator);
        content.append("mfa.max.verification.attempts=5").append(lineSeparator);
        content.append("mfa.lockout.minutes=10").append(lineSeparator);
        content.append("mfa.recovery.code.count=10").append(lineSeparator);
        content.append("mfa.allowed.time.window.drift=1").append(lineSeparator);
        content.append("mfa.secret.bytes=20").append(lineSeparator);
    }

    private static void closeQuietly(InputStream inputStream) {
        if (inputStream == null) {
            return;
        }

        try {
            inputStream.close();
        } catch (IOException ignored) {
            // Ignore close error.
        }
    }

    private static void closeQuietly(OutputStream outputStream) {
        if (outputStream == null) {
            return;
        }

        try {
            outputStream.close();
        } catch (IOException ignored) {
            // Ignore close error.
        }
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().length() == 0;
    }
}
