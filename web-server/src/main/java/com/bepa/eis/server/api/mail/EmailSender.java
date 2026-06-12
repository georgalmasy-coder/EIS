package com.bepa.eis.server.api.mail;

import com.bepa.eis.server.api.DTO.User;
import com.bepa.eis.common.enums.entity.EntityType;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    public void sendNotificationEmail(User toEmail, String code, String name, EntityType entityType, User changedByUser) throws Exception {

        String template = """
                        #ENT# '#CODE# #SYSTEM#' has been changed by #BY-USER#.
        
                        Kind regards,
                        EIS Team
                        """;
        template = template.replace("#ENT#", entityType.getDescription());
        template = template.replace("#CODE#", code);
        template = template.replace("#SYSTEM#", name);
        template = template.replace("#BY-USER#", changedByUser != null && changedByUser.getName() != null ? changedByUser.getName() : "Unknown");

        String subject = "#ENT# #CODE# has been changed by #BY-USER#";
        subject = subject.replace("#ENT#", entityType.getDescription());
        subject = subject.replace("#CODE#", code);
        subject = subject.replace("#BY-USER#", changedByUser != null && changedByUser.getName() != null ? changedByUser.getName() : "Unknown");

        sendEmail(toEmail.getEmail(), subject, template);
    }

    public void sendEmail(String toEmail, String subject, String messageText)  {
        //String smtpHost = "mail.almasy.dk";
        //Integer smtpPort = 587;

        //String username = "eis@almasy.dk";
        //String password = "Caroline1433!";

        //String from = "eis@almasy.dk";

//        String to = "mhs@haldborg.com";
        toEmail = "georg.almasy@gmail.com";

        Properties properties = getProperties(getSmtpHost(), getSmtpPort());

        /*
        Properties properties = new Properties);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", smtpHost);
        properties.put("mail.smtp.port", String.valueOf(smtpPort));

         */

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(getAuthUsername(),  getAuthPassword());
            }
        });

        Message message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress(getNotifierEmail(), getNotifierName()));

            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(toEmail)
            );

            message.setSubject(subject);
            message.setText(messageText);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
//        message.setSubject("System Requirement 1.1.1 has been changed");

/*
        messageText = """
                System Requirement '1.1.1 Multi-tier Redundancy' has been changed by System Administrator.

                Kind regards,
                EIS Team
                """;
*/

        try {
            Transport.send(message);
        } catch (SendFailedException  e) {
            log.warn("Failed to send email to {}", toEmail);
            throw new RuntimeException(e);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    private Properties getProperties(String smtpHost, Integer smtpPort) {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", smtpHost);
        properties.put("mail.smtp.port", smtpPort.toString());
        return properties;
    }

    private String getSmtpHost() {
        return "mail.almasy.dk";
    }

    private Integer getSmtpPort() {
        return 587;
    }

    private String getAuthUsername() {
        return "eis@almasy.dk";
    }

    private String getAuthPassword() {
        return "Caroline1433!";
    }

    private String getNotifierEmail() {
        return "eis@almasy.dk";
    }

    private String getNotifierName() {
        return "EIS Notification";
    }
}