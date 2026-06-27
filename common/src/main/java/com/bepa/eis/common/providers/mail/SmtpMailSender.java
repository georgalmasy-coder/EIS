package com.bepa.eis.common.providers.mail;

import com.bepa.eis.common.GlobalConfiguration;
import com.bepa.eis.common.dto.mail.MailQueueItem;
import com.bepa.eis.common.dto.mail.MailSendResult;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.SendFailedException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;

public class SmtpMailSender implements MailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailSender.class);

    public SmtpMailSender() {
    }

    @Override
    public MailSendResult send(MailQueueItem mail) {
        if (mail == null) {
            return MailSendResult.failed("Mail item is missing.");
        }

        String validationError = validateMail(mail);

        if (!validationError.isEmpty()) {
            return MailSendResult.failed(validationError);
        }

        try {
            Session session = createSession();
            MimeMessage message = createMessage(session, mail);

            Transport.send(message);

            String smtpMessageId = getMessageId(message);

            log.info(
                    "Mail sent. mailId={}, to={}, subject={}",
                    mail.getMailId(),
                    mail.getToEmail(),
                    mail.getSubject()
            );

            return MailSendResult.success(smtpMessageId);
        } catch (SendFailedException e) {
            log.warn(
                    "Failed to send mail. mailId={}, to={}, subject={}",
                    mail.getMailId(),
                    mail.getToEmail(),
                    mail.getSubject(),
                    e
            );

            return MailSendResult.failed(
                    "Failed to send mail: " + safeExceptionMessage(e),
                    e
            );
        } catch (MessagingException e) {
            log.warn(
                    "SMTP error while sending mail. mailId={}, to={}, subject={}",
                    mail.getMailId(),
                    mail.getToEmail(),
                    mail.getSubject(),
                    e
            );

            return MailSendResult.failed(
                    "SMTP error while sending mail: " + safeExceptionMessage(e),
                    e
            );
        } catch (Exception e) {
            log.warn(
                    "Unexpected error while sending mail. mailId={}, to={}, subject={}",
                    mail.getMailId(),
                    mail.getToEmail(),
                    mail.getSubject(),
                    e
            );

            return MailSendResult.failed(
                    "Unexpected error while sending mail: " + safeExceptionMessage(e),
                    e
            );
        }
    }

    private Session createSession() {
        Properties properties = createSmtpProperties();

        if (!GlobalConfiguration.isMailSmtpAuthEnabled()) {
            return Session.getInstance(properties);
        }

        return Session.getInstance(
                properties,
                new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                GlobalConfiguration.getMailSmtpUsername(),
                                GlobalConfiguration.getMailSmtpPassword()
                        );
                    }
                }
        );
    }

    private Properties createSmtpProperties() {
        Properties properties = new Properties();

        properties.put("mail.smtp.host", GlobalConfiguration.getMailSmtpHost());
        properties.put("mail.smtp.port", String.valueOf(GlobalConfiguration.getMailSmtpPort()));
        properties.put("mail.smtp.auth", String.valueOf(GlobalConfiguration.isMailSmtpAuthEnabled()));
        properties.put("mail.smtp.starttls.enable", String.valueOf(GlobalConfiguration.isMailSmtpStartTlsEnabled()));
        properties.put("mail.smtp.ssl.enable", String.valueOf(GlobalConfiguration.isMailSmtpSslEnabled()));
        properties.put("mail.smtp.connectiontimeout", String.valueOf(GlobalConfiguration.getMailSmtpConnectionTimeoutMillis()));
        properties.put("mail.smtp.timeout", String.valueOf(GlobalConfiguration.getMailSmtpReadTimeoutMillis()));

        return properties;
    }

    private MimeMessage createMessage(
            Session session,
            MailQueueItem mail
    ) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = new MimeMessage(session);

        message.setFrom(createInternetAddress(
                mail.getFromEmail(),
                mail.getFromName()
        ));

        message.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(mail.getToEmail(), false)
        );

        if (mail.getCcEmails() != null && !mail.getCcEmails().trim().isEmpty()) {
            message.setRecipients(
                    Message.RecipientType.CC,
                    InternetAddress.parse(mail.getCcEmails(), false)
            );
        }

        if (mail.getBccEmails() != null && !mail.getBccEmails().trim().isEmpty()) {
            message.setRecipients(
                    Message.RecipientType.BCC,
                    InternetAddress.parse(mail.getBccEmails(), false)
            );
        }

        message.setSubject(mail.getSubject(), "UTF-8");
        message.setSentDate(new Date());

        if (mail.hasHtmlBody()) {
            message.setContent(mail.getBodyHtml(), "text/html; charset=UTF-8");
        } else {
            message.setText(mail.getBodyText(), "UTF-8");
        }

        message.saveChanges();

        return message;
    }

    private InternetAddress createInternetAddress(
            String email,
            String name
    ) throws AddressException, UnsupportedEncodingException {
        if (name == null || name.trim().isEmpty()) {
            return new InternetAddress(email);
        }

        return new InternetAddress(
                email,
                name,
                "UTF-8"
        );
    }

    private String validateMail(MailQueueItem mail) {
        if (mail.getFromEmail() == null || mail.getFromEmail().trim().isEmpty()) {
            return "Sender email is missing.";
        }

        if (mail.getToEmail() == null || mail.getToEmail().trim().isEmpty()) {
            return "Recipient email is missing.";
        }

        if (mail.getSubject() == null || mail.getSubject().trim().isEmpty()) {
            return "Mail subject is missing.";
        }

        if (!mail.hasHtmlBody() && !mail.hasTextBody()) {
            return "Mail body is missing.";
        }

        return "";
    }

    private String getMessageId(MimeMessage message) {
        try {
            String[] messageIds = message.getHeader("Message-ID");

            if (messageIds != null && messageIds.length > 0 && messageIds[0] != null) {
                return messageIds[0];
            }
        } catch (MessagingException ignored) {
            // Message-ID is optional for our queue handling.
        }

        return "";
    }

    private String safeExceptionMessage(Exception exception) {
        if (exception == null) {
            return "Unknown error";
        }

        if (exception.getMessage() == null || exception.getMessage().trim().isEmpty()) {
            return exception.getClass().getName();
        }

        return exception.getMessage().trim();
    }
}