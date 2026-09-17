package com.bepa.eis.common.logging;

import com.bepa.eis.common.enums.SeverityType;
import com.bepa.eis.common.providers.EisDataSourceProvider;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

@Plugin(
        name = "IncidentLog",
        category = "Core",
        elementType = "appender",
        printObject = true
)
public final class IncidentLogAppender extends AbstractAppender {

    private static final String INSERT_INCIDENT_SQL =
            "INSERT INTO INCIDENTS " +
                    " (CustomerId, ProjectId, UserId, ServiceId, SeverityId, Message, Module, ModuleInfo, Trace, LogCreated) " +
                    " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final int MESSAGE_MAX_LENGTH = 250;
    private static final int MODULE_MAX_LENGTH = 100;
    private static final int MODULE_INFO_MAX_LENGTH = 100;

    private IncidentLogAppender(
            String name,
            Filter filter,
            Layout<? extends Serializable> layout,
            boolean ignoreExceptions
    ) {
        super(name, filter, layout, ignoreExceptions, Property.EMPTY_ARRAY);
    }

    @PluginFactory
    public static IncidentLogAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Filter") Filter filter,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true) boolean ignoreExceptions
    ) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        return new IncidentLogAppender(name, filter, layout, ignoreExceptions);
    }

    @Override
    public void append(LogEvent event) {
        try {
            insertIncident(event);
        } catch (Exception ignored) {
            // Incident persistence must never trigger another log event or insert attempt.
        }
    }

    private void insertIncident(LogEvent event) throws SQLException {
        try (Connection connection = EisDataSourceProvider.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_INCIDENT_SQL)) {

            statement.setNull(1, Types.INTEGER);
            statement.setNull(2, Types.INTEGER);
            statement.setNull(3, Types.INTEGER);
            statement.setNull(4, Types.INTEGER);
            statement.setInt(5, SeverityType.HIGH.getId());
            statement.setString(6, truncate(getFormattedMessage(event), MESSAGE_MAX_LENGTH));
            statement.setString(7, truncate(event.getLoggerName(), MODULE_MAX_LENGTH));
            statement.setString(8, truncate(getModuleInfo(event), MODULE_INFO_MAX_LENGTH));
            statement.setString(9, throwableToString(event.getThrown()));
            statement.setTimestamp(10, new Timestamp(event.getTimeMillis()));

            statement.executeUpdate();
        }
    }

    private String getFormattedMessage(LogEvent event) {
        if (event.getMessage() == null) {
            return null;
        }

        return event.getMessage().getFormattedMessage();
    }

    private String getModuleInfo(LogEvent event) {
        if (event.getSource() == null) {
            return null;
        }

        StackTraceElement source = event.getSource();
        String fileName = source.getFileName();
        String classOrFileName = fileName != null
                ? fileName.replace(".java", "")
                : source.getClassName();

        return classOrFileName + " - " + source.getMethodName() + "(" + source.getLineNumber() + ")";
    }

    private String throwableToString(Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);

        return stringWriter.toString();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }
}
