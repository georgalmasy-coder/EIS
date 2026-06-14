package com.bepa.eis.common.providers.mail;

import com.bepa.eis.common.GlobalConfiguration;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.mail.MailQueueItem;
import com.bepa.eis.common.dto.mail.MailQueueStatistics;
import com.bepa.eis.common.dto.mail.MailRecipient;
import com.bepa.eis.common.dto.mail.MailTemplate;
import com.bepa.eis.common.enums.mail.MailStatus;
import com.bepa.eis.common.enums.mail.MailTemplateType;
import com.bepa.eis.common.providers.GenericProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MailProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(MailProvider.class);

    private static final String DEFAULT_FROM_EMAIL = "no-reply@example.com";
    private static final String DEFAULT_FROM_NAME = "BEPA EIS";

    private static final String INSERT_MAIL_SQL =
            "INSERT INTO [dbo].[MAIL_QUEUE] ( " +
                    "TemplateType, " +
                    "FromName, " +
                    "FromEmail, " +
                    "ToName, " +
                    "ToEmail, " +
                    "CcEmails, " +
                    "BccEmails, " +
                    "Subject, " +
                    "BodyText, " +
                    "BodyHtml, " +
                    "ParametersJson,  " +
                    "Status, " +
                    "AttemptCount, " +
                    "MaxAttempts, " +
                    "CreatedByUserId " +
                    ") " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?) ";

    private static final String SELECT_MAIL_BY_ID_SQL =
            "SELECT " +
                    "MailId, " +
                    "TemplateType, " +
                    "FromName, " +
                    "FromEmail, " +
                    "ToName, " +
                    "ToEmail, " +
                    "CcEmails, " +
                    "BccEmails, " +
                    "Subject, " +
                    "BodyText, " +
                    "BodyHtml, " +
                    "ParametersJson, " +
                    "Status, " +
                    "AttemptCount, " +
                    "MaxAttempts, " +
                    "NextAttemptAt, " +
                    "CreatedAt, " +
                    "CreatedByUserId, " +
                    "LastAttemptAt, " +
                    "SentAt, " +
                    "LastError, " +
                    "SmtpMessageId, " +
                    "LockedAt, " +
                    "LockedBy " +
                    "FROM [dbo].[MAIL_QUEUE] " +
                    "WHERE MailId = ? ";

    private static final String SELECT_PENDING_MAILS_SQL =
            "SELECT TOP (?) " +
                    "MailId, " +
                    "TemplateType, " +
                    "FromName, " +
                    "FromEmail, " +
                    "ToName, " +
                    "ToEmail, " +
                    "CcEmails, " +
                    "BccEmails, " +
                    "Subject, " +
                    "BodyText, " +
                    "BodyHtml, " +
                    "ParametersJson, " +
                    "Status, " +
                    "AttemptCount, " +
                    "MaxAttempts, " +
                    "NextAttemptAt, " +
                    "CreatedAt, " +
                    "CreatedByUserId, " +
                    "LastAttemptAt, " +
                    "SentAt, " +
                    "LastError, " +
                    "SmtpMessageId, " +
                    "LockedAt, " +
                    "LockedBy " +
                    "FROM [dbo].[MAIL_QUEUE] " +
                    "WHERE Status IN ('QUEUED', 'FAILED') " +
                    "  AND NextAttemptAt <= SYSUTCDATETIME() " +
                    "  AND AttemptCount < MaxAttempts " +
                    "ORDER BY NextAttemptAt ASC, MailId ASC ";

    private static final String MARK_AS_SENDING_SQL =
            "UPDATE [dbo].[MAIL_QUEUE] " +
                    "SET " +
                    "Status = 'SENDING', " +
                    "LockedAt = SYSUTCDATETIME(), " +
                    "LockedBy = ?, " +
                    "LastAttemptAt = SYSUTCDATETIME() " +
                    "WHERE MailId = ? " +
                    "  AND Status IN ('QUEUED', 'FAILED') " +
                    "  AND NextAttemptAt <= SYSUTCDATETIME() " +
                    "  AND AttemptCount < MaxAttempts ";

    private static final String MARK_AS_SENT_SQL =
            "UPDATE [dbo].[MAIL_QUEUE] " +
                    "SET " +
                    "Status = 'SENT', " +
                    "SentAt = SYSUTCDATETIME(), " +
                    "SmtpMessageId = ?, " +
                    "LastError = NULL, " +
                    "LockedAt = NULL, " +
                    "LockedBy = NULL " +
                    "WHERE MailId = ? ";

    private static final String MARK_AS_FAILED_SQL =
            "UPDATE [dbo].[MAIL_QUEUE] " +
                    "SET " +
                    "Status = CASE " +
                    "    WHEN AttemptCount + 1 >= MaxAttempts THEN 'UNDELIVERED' " +
                    "    ELSE 'FAILED' " +
                    "END, " +
                    "AttemptCount = AttemptCount + 1, " +
                    "LastError = ?, " +
                    "NextAttemptAt = DATEADD(MINUTE, ?, SYSUTCDATETIME()), " +
                    "LockedAt = NULL, " +
                    "LockedBy = NULL " +
                    "WHERE MailId = ? ";

    private static final String MARK_AS_UNDELIVERED_SQL =
            "UPDATE [dbo].[MAIL_QUEUE] " +
                    "SET " +
                    "Status = 'UNDELIVERED', " +
                    "LastError = ?, " +
                    "LockedAt = NULL, " +
                    "LockedBy = NULL " +
                    "WHERE MailId = ? ";

    private static final String CANCEL_MAIL_SQL =
            "UPDATE [dbo].[MAIL_QUEUE] " +
                    "SET " +
                    "Status = 'CANCELLED', " +
                    "LastError = ?, " +
                    "LockedAt = NULL, " +
                    "LockedBy = NULL " +
                    "WHERE MailId = ? " +
                    "  AND Status IN ('QUEUED', 'FAILED') ";

    private static final String RELEASE_STUCK_SENDING_MAILS_SQL =
            "UPDATE [dbo].[MAIL_QUEUE] " +
                    "SET " +
                    "Status = 'FAILED', " +
                    "LastError = 'Mail sending lock timed out', " +
                    "NextAttemptAt = SYSUTCDATETIME(), " +
                    "LockedAt = NULL, " +
                    "LockedBy = NULL " +
                    "WHERE Status = 'SENDING' " +
                    "  AND LockedAt < DATEADD(MINUTE, -?, SYSUTCDATETIME()) ";

    private static final String COUNT_BY_STATUS_SQL =
            "SELECT COUNT(*) AS MailCount " +
                    "FROM [dbo].[MAIL_QUEUE] " +
                    "WHERE Status = ?";

    private static final String COUNT_SENT_LAST_24_HOURS_SQL =
            "SELECT COUNT(*) AS MailCount " +
                    "FROM [dbo].[MAIL_QUEUE] " +
                    "WHERE Status = 'SENT' " +
                    "  AND SentAt >= DATEADD(HOUR, -24, SYSUTCDATETIME()) ";

    private static final String COUNT_SENT_LAST_7_DAYS_SQL =
            "SELECT COUNT(*) AS MailCount " +
                    "FROM [dbo].[MAIL_QUEUE] " +
                    "WHERE Status = 'SENT' " +
                    "  AND SentAt >= DATEADD(DAY, -7, SYSUTCDATETIME()) ";

    private static final String COUNT_ERRORS_LAST_7_DAYS_SQL =
            "SELECT COUNT(*) AS MailCount " +
                    "FROM [dbo].[MAIL_QUEUE] " +
                    "WHERE Status IN ('FAILED', 'UNDELIVERED') " +
                    "  AND COALESCE(LastAttemptAt, CreatedAt) >= DATEADD(DAY, -7, SYSUTCDATETIME()) ";

    private static final String SELECT_MAIL_HOURLY_STATUS_LAST_24_HOURS_SQL =
            "WITH Hours AS ( " +
                    "    SELECT 23 AS HourOffset " +
                    "    UNION ALL " +
                    "    SELECT HourOffset - 1 FROM Hours WHERE HourOffset > 0 " +
                    "), HourBuckets AS ( " +
                    "    SELECT " +
                    "        DATEADD(HOUR, DATEDIFF(HOUR, 0, DATEADD(HOUR, -HourOffset, SYSUTCDATETIME())), 0) AS BucketStart " +
                    "    FROM Hours " +
                    ") " +
                    "SELECT " +
                    "    CONVERT(VARCHAR(16), hb.BucketStart, 120) AS HourLabel, " +
                    "    SUM(CASE WHEN mq.Status = 'SENT' THEN 1 ELSE 0 END) AS SentCount, " +
                    "    SUM(CASE WHEN mq.Status IN ('FAILED', 'UNDELIVERED') THEN 1 ELSE 0 END) AS ErrorCount " +
                    "FROM HourBuckets hb " +
                    "LEFT JOIN [dbo].[MAIL_QUEUE] mq " +
                    "    ON COALESCE(mq.SentAt, mq.LastAttemptAt, mq.CreatedAt) >= hb.BucketStart " +
                    "   AND COALESCE(mq.SentAt, mq.LastAttemptAt, mq.CreatedAt) < DATEADD(HOUR, 1, hb.BucketStart) " +
                    "   AND mq.Status IN ('SENT', 'FAILED', 'UNDELIVERED') " +
                    "GROUP BY hb.BucketStart " +
                    "ORDER BY hb.BucketStart ASC " +
                    "OPTION (MAXRECURSION 24) ";

    private static final String OLDEST_QUEUED_AT_SQL =
            "SELECT MIN(CreatedAt) AS OldestQueuedAt " +
                    "FROM [dbo].[MAIL_QUEUE] " +
                    "WHERE Status = 'QUEUED'";

    private static final String SELECT_LATEST_SENT_MAILS_SQL =
            "SELECT TOP (?) " +
                    "MailId, " +
                    "TemplateType, " +
                    "FromName, " +
                    "FromEmail, " +
                    "ToName, " +
                    "ToEmail, " +
                    "CcEmails, " +
                    "BccEmails, " +
                    "Subject, " +
                    "BodyText, " +
                    "BodyHtml, " +
                    "ParametersJson, " +
                    "Status, " +
                    "AttemptCount, " +
                    "MaxAttempts, " +
                    "NextAttemptAt, " +
                    "CreatedAt, " +
                    "CreatedByUserId, " +
                    "LastAttemptAt, " +
                    "SentAt, " +
                    "LastError, " +
                    "SmtpMessageId, " +
                    "LockedAt, " +
                    "LockedBy " +
                    "FROM [dbo].[MAIL_QUEUE] " +
                    "WHERE Status = 'SENT' " +
                    "ORDER BY SentAt DESC, MailId DESC";

    private static final String SELECT_LATEST_FAILED_MAILS_SQL =
            "SELECT TOP (?) " +
                    "MailId, " +
                    "TemplateType, " +
                    "FromName, " +
                    "FromEmail, " +
                    "ToName, " +
                    "ToEmail, " +
                    "CcEmails, " +
                    "BccEmails, " +
                    "Subject, " +
                    "BodyText, " +
                    "BodyHtml, " +
                    "ParametersJson, " +
                    "Status, " +
                    "AttemptCount, " +
                    "MaxAttempts, " +
                    "NextAttemptAt, " +
                    "CreatedAt, " +
                    "CreatedByUserId, " +
                    "LastAttemptAt, " +
                    "SentAt, " +
                    "LastError, " +
                    "SmtpMessageId, " +
                    "LockedAt, " +
                    "LockedBy " +
                    "FROM [dbo].[MAIL_QUEUE] " +
                    "WHERE Status = 'FAILED' " +
                    "ORDER BY LastAttemptAt DESC, MailId DESC";

    private static final String SELECT_LATEST_UNDELIVERED_MAILS_SQL =
            "SELECT TOP (?) " +
                    "MailId, " +
                    "TemplateType, " +
                    "FromName, " +
                    "FromEmail, " +
                    "ToName, " +
                    "ToEmail, " +
                    "CcEmails, " +
                    "BccEmails, " +
                    "Subject, " +
                    "BodyText, " +
                    "BodyHtml, " +
                    "ParametersJson, " +
                    "Status, " +
                    "AttemptCount, " +
                    "MaxAttempts, " +
                    "NextAttemptAt, " +
                    "CreatedAt, " +
                    "CreatedByUserId, " +
                    "LastAttemptAt, " +
                    "SentAt, " +
                    "LastError, " +
                    "SmtpMessageId, " +
                    "LockedAt, " +
                    "LockedBy " +
                    "FROM [dbo].[MAIL_QUEUE] " +
                    "WHERE Status = 'UNDELIVERED' " +
                    "ORDER BY LastAttemptAt DESC, MailId DESC";

    private static final String SELECT_LATEST_ERROR_MAILS_SQL =
            "SELECT TOP (?) " +
                    "MailId, " +
                    "TemplateType, " +
                    "FromName, " +
                    "FromEmail, " +
                    "ToName, " +
                    "ToEmail, " +
                    "CcEmails, " +
                    "BccEmails, " +
                    "Subject, " +
                    "BodyText, " +
                    "BodyHtml, " +
                    "ParametersJson, " +
                    "Status, " +
                    "AttemptCount, " +
                    "MaxAttempts, " +
                    "NextAttemptAt, " +
                    "CreatedAt, " +
                    "CreatedByUserId, " +
                    "LastAttemptAt, " +
                    "SentAt, " +
                    "LastError, " +
                    "SmtpMessageId, " +
                    "LockedAt, " +
                    "LockedBy " +
                    "FROM [dbo].[MAIL_QUEUE] " +
                    "WHERE Status IN ('FAILED', 'UNDELIVERED') " +
                    "ORDER BY LastAttemptAt DESC, MailId DESC";

    private final MailTemplateRenderer mailTemplateRenderer;

    public MailProvider(WebSession webSession) {
        super(webSession);
        mailTemplateRenderer = new MailTemplateRenderer();
    }

    public Integer createMail(
            MailRecipient from,
            MailRecipient to,
            MailTemplateType templateType,
            Map<String, Object> parameters
    ) {
        if (to == null || !to.isValid()) {
            log.warn("Mail could not be created because recipient is missing or invalid: {}", to);
            return null;
        }

        MailRecipient safeFrom = normalizeSender(from);
        MailTemplateType safeTemplateType = templateType == null
                ? MailTemplateType.SYSTEM_NOTIFICATION
                : templateType;

        Map<String, Object> safeParameters = buildDefaultParameters(
                safeFrom,
                to,
                parameters
        );

        try {
            MailTemplate renderedTemplate = mailTemplateRenderer.render(
                    safeTemplateType,
                    safeParameters
            );

            if (!renderedTemplate.hasSubject()) {
                log.warn("Mail template {} has no subject.", safeTemplateType);
                return null;
            }

            if (!renderedTemplate.hasBody()) {
                log.warn("Mail template {} has no body.", safeTemplateType);
                return null;
            }

            return insertMail(
                    safeFrom,
                    to,
                    safeTemplateType,
                    renderedTemplate,
                    safeParameters
            );
        } catch (Exception e) {
            log.error("Error creating mail for template {} to {}", safeTemplateType, to.getEmail(), e);
            return null;
        }
    }

    public Integer createMail(
            MailRecipient to,
            MailTemplateType templateType,
            Map<String, Object> parameters
    ) {
        return createMail(
                null,
                to,
                templateType,
                parameters
        );
    }

    public List<Integer> createMails(
            MailRecipient from,
            List<MailRecipient> recipients,
            MailTemplateType templateType,
            Map<String, Object> parameters
    ) {
        List<Integer> mailIds = new ArrayList<>();

        if (recipients == null || recipients.isEmpty()) {
            return mailIds;
        }

        for (MailRecipient recipient : recipients) {
            Integer mailId = createMail(
                    from,
                    recipient,
                    templateType,
                    parameters
            );

            if (mailId != null) {
                mailIds.add(mailId);
            }
        }

        return mailIds;
    }

    public MailQueueItem getMailById(Integer mailId) {
        if (mailId == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_MAIL_BY_ID_SQL)) {

            statement.setInt(1, mailId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapMailQueueItem(resultSet);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading mail by id. mailId={}", mailId, e);
        }

        return null;
    }

    public List<MailQueueItem> getPendingMails(int maxRows) {
        List<MailQueueItem> mails = new ArrayList<>();
        int safeMaxRows = Math.max(1, Math.min(maxRows, 500));

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_PENDING_MAILS_SQL)) {

            statement.setInt(1, safeMaxRows);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    mails.add(mapMailQueueItem(resultSet));
                }
            }
        } catch (SQLException e) {
            log.error("Error loading pending mails", e);
        }

        return mails;
    }

    public boolean markAsSending(
            Integer mailId,
            String workerId
    ) {
        if (mailId == null) {
            return false;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(MARK_AS_SENDING_SQL)) {

            statement.setString(1, safeText(workerId, "unknown-worker"));
            statement.setInt(2, mailId);

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error marking mail as sending. mailId={}", mailId, e);
            return false;
        }
    }

    public boolean markAsSent(
            Integer mailId,
            String smtpMessageId
    ) {
        if (mailId == null) {
            return false;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(MARK_AS_SENT_SQL)) {

            statement.setString(1, safeText(smtpMessageId, ""));
            statement.setInt(2, mailId);

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error marking mail as sent. mailId={}", mailId, e);
            return false;
        }
    }

    public boolean markAsFailed(
            Integer mailId,
            String errorMessage,
            int retryDelayMinutes
    ) {
        if (mailId == null) {
            return false;
        }

        int safeRetryDelayMinutes = Math.max(1, Math.min(retryDelayMinutes, 1440));

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(MARK_AS_FAILED_SQL)) {

            statement.setString(1, truncate(safeText(errorMessage, "Unknown mail delivery error"), 4000));
            statement.setInt(2, safeRetryDelayMinutes);
            statement.setInt(3, mailId);

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error marking mail as failed. mailId={}", mailId, e);
            return false;
        }
    }

    public boolean markAsUndelivered(
            Integer mailId,
            String errorMessage
    ) {
        if (mailId == null) {
            return false;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(MARK_AS_UNDELIVERED_SQL)) {

            statement.setString(1, truncate(safeText(errorMessage, "Mail could not be delivered"), 4000));
            statement.setInt(2, mailId);

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error marking mail as undelivered. mailId={}", mailId, e);
            return false;
        }
    }

    public boolean cancelMail(
            Integer mailId,
            String reason
    ) {
        if (mailId == null) {
            return false;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(CANCEL_MAIL_SQL)) {

            statement.setString(1, truncate(safeText(reason, "Mail cancelled"), 4000));
            statement.setInt(2, mailId);

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error cancelling mail. mailId={}", mailId, e);
            return false;
        }
    }

    public int releaseStuckSendingMails(int stuckTimeoutMinutes) {
        int safeTimeoutMinutes = Math.max(1, Math.min(stuckTimeoutMinutes, 1440));

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(RELEASE_STUCK_SENDING_MAILS_SQL)) {

            statement.setInt(1, safeTimeoutMinutes);

            return statement.executeUpdate();
        } catch (SQLException e) {
            log.error("Error releasing stuck sending mails", e);
            return 0;
        }
    }

    public int getQueuedMailCount() {
        return getCountByStatus(MailStatus.QUEUED);
    }

    public int getSendingMailCount() {
        return getCountByStatus(MailStatus.SENDING);
    }

    public int getFailedMailCount() {
        return getCountByStatus(MailStatus.FAILED);
    }

    public int getUndeliveredMailCount() {
        return getCountByStatus(MailStatus.UNDELIVERED);
    }

    public int getCancelledMailCount() {
        return getCountByStatus(MailStatus.CANCELLED);
    }

    public int getSentLast7DaysCount() {
        return getSingleCount(COUNT_SENT_LAST_7_DAYS_SQL, "sent mails last 7 days");
    }

    public int getErrorsLast7DaysCount() {
        return getSingleCount(COUNT_ERRORS_LAST_7_DAYS_SQL, "mail errors last 7 days");
    }

    public MailQueueStatistics getMailQueueStatistics() {
        MailQueueStatistics statistics = new MailQueueStatistics();

        statistics.setQueuedCount(getCountByStatus(MailStatus.QUEUED));
        statistics.setSendingCount(getCountByStatus(MailStatus.SENDING));
        statistics.setFailedCount(getCountByStatus(MailStatus.FAILED));
        statistics.setUndeliveredCount(getCountByStatus(MailStatus.UNDELIVERED));
        statistics.setCancelledCount(getCountByStatus(MailStatus.CANCELLED));
        statistics.setSentLast24HoursCount(getSentLast24HoursCount());
        statistics.setOldestQueuedAt(getOldestQueuedAt());

        return statistics;
    }

    public List<MailHourlyStatusPoint> getMailHourlyStatusLast24Hours() {
        List<MailHourlyStatusPoint> points = new ArrayList<>();

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_MAIL_HOURLY_STATUS_LAST_24_HOURS_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                points.add(new MailHourlyStatusPoint(
                        resultSet.getString("HourLabel"),
                        resultSet.getInt("SentCount"),
                        resultSet.getInt("ErrorCount")
                ));
            }
        } catch (SQLException e) {
            log.error("Error loading mail hourly status last 24 hours", e);
        }

        return points;
    }

    public List<MailQueueItem> getLatestSentMails(int maxRows) {
        return getLatestMails(SELECT_LATEST_SENT_MAILS_SQL, maxRows);
    }

    public List<MailQueueItem> getLatestFailedMails(int maxRows) {
        return getLatestMails(SELECT_LATEST_FAILED_MAILS_SQL, maxRows);
    }

    public List<MailQueueItem> getLatestUndeliveredMails(int maxRows) {
        return getLatestMails(SELECT_LATEST_UNDELIVERED_MAILS_SQL, maxRows);
    }

    public List<MailQueueItem> getLatestErrorMails(int maxRows) {
        return getLatestMails(SELECT_LATEST_ERROR_MAILS_SQL, maxRows);
    }

    private Integer insertMail(
            MailRecipient from,
            MailRecipient to,
            MailTemplateType templateType,
            MailTemplate renderedTemplate,
            Map<String, Object> parameters
    ) throws SQLException {
        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_MAIL_SQL,
                     Statement.RETURN_GENERATED_KEYS
             )) {

            statement.setString(1, templateType.name());
            statement.setString(2, from.getName());
            statement.setString(3, from.getEmail());
            statement.setString(4, to.getName());
            statement.setString(5, to.getEmail());
            statement.setString(6, "");
            statement.setString(7, "");
            statement.setString(8, renderedTemplate.getSubject());

            if (renderedTemplate.isHtml()) {
                statement.setString(9, "");
                statement.setString(10, renderedTemplate.getBody());
            } else {
                statement.setString(9, renderedTemplate.getBody());
                statement.setString(10, "");
            }

            statement.setString(11, toJson(parameters));
            statement.setString(12, MailStatus.QUEUED.name());
            statement.setInt(13, GlobalConfiguration.getMailQueueMaxAttempts());

            Integer createdByUserId = getCreatedByUserId();

            if (createdByUserId == null) {
                statement.setNull(14, Types.INTEGER);
            } else {
                statement.setInt(14, createdByUserId);
            }

            int updatedRows = statement.executeUpdate();

            if (updatedRows == 0) {
                return null;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }

            return null;
        }
    }

    private MailRecipient normalizeSender(MailRecipient from) {
        if (from != null && from.isValid()) {
            return from;
        }

        return new MailRecipient(
                GlobalConfiguration.getMailDefaultFromName(),
                GlobalConfiguration.getMailDefaultFromEmail()
        );
    }

    private Map<String, Object> buildDefaultParameters(
            MailRecipient from,
            MailRecipient to,
            Map<String, Object> parameters
    ) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (parameters != null) {
            result.putAll(parameters);
        }

        putIfMissing(result, "fromName", from.getName());
        putIfMissing(result, "fromEmail", from.getEmail());
        putIfMissing(result, "toName", to.getName());
        putIfMissing(result, "toEmail", to.getEmail());

        return result;
    }

    private void putIfMissing(
            Map<String, Object> parameters,
            String key,
            Object value
    ) {
        if (!parameters.containsKey(key)) {
            parameters.put(key, value);
        }
    }

    private Integer getCreatedByUserId() {
        WebSession webSession = getWebSession();

        if (webSession == null) {
            return null;
        }

        return webSession.getUserId();
    }

    private int getCountByStatus(MailStatus status) {
        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(COUNT_BY_STATUS_SQL)) {

            statement.setString(1, status.name());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("MailCount");
                }
            }
        } catch (SQLException e) {
            log.error("Error counting mails by status {}", status, e);
        }

        return 0;
    }

    private int getSingleCount(String sql, String operationName) {
        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return resultSet.getInt("MailCount");
            }
        } catch (SQLException e) {
            log.error("Error counting {}", operationName, e);
        }

        return 0;
    }

    private int getSentLast24HoursCount() {
        return getSingleCount(COUNT_SENT_LAST_24_HOURS_SQL, "sent mails last 24 hours");
    }

    private Timestamp getOldestQueuedAt() {
        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(OLDEST_QUEUED_AT_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return resultSet.getTimestamp("OldestQueuedAt");
            }
        } catch (SQLException e) {
            log.error("Error finding oldest queued mail", e);
        }

        return null;
    }

    private List<MailQueueItem> getLatestMails(String sql, int maxRows) {
        List<MailQueueItem> mails = new ArrayList<>();
        int safeMaxRows = Math.max(1, Math.min(maxRows, 500));

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, safeMaxRows);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    mails.add(mapMailQueueItem(resultSet));
                }
            }
        } catch (SQLException e) {
            log.error("Error loading latest mails", e);
        }

        return mails;
    }

    private MailQueueItem mapMailQueueItem(ResultSet resultSet) throws SQLException {
        MailQueueItem item = new MailQueueItem();

        int mailId = resultSet.getInt("MailId");
        item.setMailId(resultSet.wasNull() ? null : mailId);

        item.setTemplateTypeName(resultSet.getString("TemplateType"));
        item.setFromName(resultSet.getString("FromName"));
        item.setFromEmail(resultSet.getString("FromEmail"));
        item.setToName(resultSet.getString("ToName"));
        item.setToEmail(resultSet.getString("ToEmail"));
        item.setCcEmails(resultSet.getString("CcEmails"));
        item.setBccEmails(resultSet.getString("BccEmails"));
        item.setSubject(resultSet.getString("Subject"));
        item.setBodyText(resultSet.getString("BodyText"));
        item.setBodyHtml(resultSet.getString("BodyHtml"));
        item.setParametersJson(resultSet.getString("ParametersJson"));
        item.setStatusName(resultSet.getString("Status"));

        int attemptCount = resultSet.getInt("AttemptCount");
        item.setAttemptCount(resultSet.wasNull() ? null : attemptCount);

        int maxAttempts = resultSet.getInt("MaxAttempts");
        item.setMaxAttempts(resultSet.wasNull() ? null : maxAttempts);

        item.setNextAttemptAt(resultSet.getTimestamp("NextAttemptAt"));
        item.setCreatedAt(resultSet.getTimestamp("CreatedAt"));

        int createdByUserId = resultSet.getInt("CreatedByUserId");
        item.setCreatedByUserId(resultSet.wasNull() ? null : createdByUserId);

        item.setLastAttemptAt(resultSet.getTimestamp("LastAttemptAt"));
        item.setSentAt(resultSet.getTimestamp("SentAt"));
        item.setLastError(resultSet.getString("LastError"));
        item.setSmtpMessageId(resultSet.getString("SmtpMessageId"));
        item.setLockedAt(resultSet.getTimestamp("LockedAt"));
        item.setLockedBy(resultSet.getString("LockedBy"));

        return item;
    }

    private String toJson(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return "{}";
        }

        StringBuilder json = new StringBuilder();
        json.append("{");

        boolean first = true;

        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }

            if (!first) {
                json.append(",");
            }

            json.append("\"")
                    .append(escapeJson(entry.getKey()))
                    .append("\":");

            if (entry.getValue() == null) {
                json.append("null");
            } else {
                json.append("\"")
                        .append(escapeJson(String.valueOf(entry.getValue())))
                        .append("\"");
            }

            first = false;
        }

        json.append("}");

        return json.toString();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder escaped = new StringBuilder();

        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);

            switch (character) {
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (character < 32) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                    break;
            }
        }

        return escaped.toString();
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }

    public record MailHourlyStatusPoint(
            String hour,
            int sent,
            int error
    ) {
    }
}