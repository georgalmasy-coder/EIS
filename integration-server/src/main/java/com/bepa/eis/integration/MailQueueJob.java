package com.bepa.eis.integration;

import com.bepa.eis.common.GlobalConfiguration;
import com.bepa.eis.common.dto.mail.MailQueueItem;
import com.bepa.eis.common.dto.mail.MailSendResult;
import com.bepa.eis.common.providers.mail.MailProvider;
import com.bepa.eis.common.providers.mail.MailSender;
import com.bepa.eis.common.providers.mail.SmtpMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class MailQueueJob implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(MailQueueJob.class);

    private final MailProvider mailProvider;
    private final MailSender mailSender;
    private final String workerId;
    private final AtomicBoolean running;

    public MailQueueJob() {
        this(
                new MailProvider(null),
                new SmtpMailSender(),
                buildDefaultWorkerId()
        );
    }

    public MailQueueJob(
            MailProvider mailProvider,
            MailSender mailSender,
            String workerId
    ) {
        this.mailProvider = mailProvider;
        this.mailSender = mailSender;
        this.workerId = normalizeWorkerId(workerId);
        this.running = new AtomicBoolean(false);
    }

    @Override
    public void run() {
        runOnce();
    }

    public void runOnce() {
        if (!GlobalConfiguration.isMailQueueJobEnabled()) {
            log.debug("Mail queue job is disabled.");
            return;
        }

        if (!running.compareAndSet(false, true)) {
            log.warn("Mail queue job is already running. Skipping this execution.");
            return;
        }

        try {
            executeJob();
        } finally {
            running.set(false);
        }
    }

    private void executeJob() {
        int releasedCount = mailProvider.releaseStuckSendingMails(
                GlobalConfiguration.getMailQueueStuckSendingTimeoutMinutes()
        );

        if (releasedCount > 0) {
            log.warn("Released {} stuck mail(s) from SENDING state.", releasedCount);
        }

        List<MailQueueItem> pendingMails = mailProvider.getPendingMails(
                GlobalConfiguration.getMailQueueBatchSize()
        );

        if (pendingMails.isEmpty()) {
            log.debug("No pending mails found.");
            return;
        }

        log.info("Mail queue job found {} pending mail(s).", pendingMails.size());

        for (MailQueueItem mail : pendingMails) {
            processMail(mail);
        }
    }

    private void processMail(MailQueueItem mail) {
        if (mail == null || mail.getMailId() == null) {
            return;
        }

        boolean locked = mailProvider.markAsSending(
                mail.getMailId(),
                workerId
        );

        if (!locked) {
            log.debug("Mail could not be locked. mailId={}", mail.getMailId());
            return;
        }

        try {
            MailSendResult result = mailSender.send(mail);

            if (result != null && result.isSuccess()) {
                boolean updated = mailProvider.markAsSent(
                        mail.getMailId(),
                        result.getSmtpMessageId()
                );

                if (updated) {
                    log.info("Mail marked as SENT. mailId={}", mail.getMailId());
                } else {
                    log.warn("Mail was sent, but status could not be updated to SENT. mailId={}", mail.getMailId());
                }

                return;
            }

            String errorMessage = result == null
                    ? "Mail sender returned no result."
                    : result.getCombinedErrorMessage();

            markMailAsFailed(
                    mail,
                    errorMessage
            );
        } catch (Exception e) {
            markMailAsFailed(
                    mail,
                    safeExceptionMessage(e)
            );
        }
    }

    private void markMailAsFailed(
            MailQueueItem mail,
            String errorMessage
    ) {
        int retryDelayMinutes = GlobalConfiguration.getMailQueueRetryDelayMinutes();

        boolean updated = mailProvider.markAsFailed(
                mail.getMailId(),
                errorMessage,
                retryDelayMinutes
        );

        if (updated) {
            log.warn(
                    "Mail marked as FAILED or UNDELIVERED. mailId={}, retryDelayMinutes={}, error={}",
                    mail.getMailId(),
                    retryDelayMinutes,
                    errorMessage
            );
        } else {
            log.error(
                    "Mail failure could not be persisted. mailId={}, error={}",
                    mail.getMailId(),
                    errorMessage
            );
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public String getWorkerId() {
        return workerId;
    }

    private static String buildDefaultWorkerId() {
        return "integration-mail-worker-"
                + getHostName()
                + "-"
                + UUID.randomUUID();
    }

    private static String getHostName() {
        try {
            String hostName = InetAddress.getLocalHost().getHostName();

            if (hostName != null && !hostName.trim().isEmpty()) {
                return hostName.trim();
            }
        } catch (UnknownHostException ignored) {
            // Fallback below.
        }

        return "unknown-host";
    }

    private static String normalizeWorkerId(String workerId) {
        if (workerId == null || workerId.trim().isEmpty()) {
            return buildDefaultWorkerId();
        }

        return workerId.trim();
    }

    private String safeExceptionMessage(Exception exception) {
        if (exception == null) {
            return "Unknown mail queue job error";
        }

        if (exception.getMessage() == null || exception.getMessage().trim().isEmpty()) {
            return exception.getClass().getName();
        }

        return exception.getMessage().trim();
    }
}