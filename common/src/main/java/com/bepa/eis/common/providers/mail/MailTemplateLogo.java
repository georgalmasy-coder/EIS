package com.bepa.eis.common.providers.mail;

import jakarta.activation.DataHandler;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.IOException;
import java.util.Base64;

/** One PNG for database editing, preview, and embedded MIME delivery. */
public final class MailTemplateLogo {
    private MailTemplateLogo() {}
    private static final byte[] PNG = readLogo();
    public static final String DATA_URI = "data:image/png;base64," + Base64.getEncoder().encodeToString(PNG);
    public static final String CONTENT_ID = "eis-logo-light@mail.eis";

    public static String imageHtml() {
        return "<img src=\"" + DATA_URI + "\" alt=\"EIS - Engineering in Systems\" width=\"110\" height=\"26\" style=\"display:block;border:0;\">";
    }
    public static void setHtmlContent(MimeMessage message, String html) throws MessagingException {
        if (!html.contains(DATA_URI)) { message.setContent(html, "text/html; charset=UTF-8"); return; }
        MimeBodyPart body = new MimeBodyPart();
        body.setContent(html.replace(DATA_URI, "cid:" + CONTENT_ID), "text/html; charset=UTF-8");
        MimeBodyPart image = new MimeBodyPart();
        image.setDataHandler(new DataHandler(new ByteArrayDataSource(PNG, "image/png")));
        image.setHeader("Content-ID", "<" + CONTENT_ID + ">");
        image.setDisposition(Part.INLINE);
        image.setFileName("eis-logo-light.png");
        MimeMultipart related = new MimeMultipart("related");
        related.addBodyPart(body); related.addBodyPart(image);
        message.setContent(related);
    }
    private static byte[] readLogo() {
        try (var input = MailTemplateLogo.class.getResourceAsStream("/mail-assets/eis-logo-light.png")) {
            if (input == null) throw new IllegalStateException("Missing mail PNG logo resource");
            return input.readAllBytes();
        } catch (IOException e) { throw new IllegalStateException("Could not read mail PNG logo", e); }
    }
}
