package com.bepa.eis.common.providers.mail;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;

/** MIME round-trip only. No SMTP connections or mail delivery. */
public class MailTemplateLogoTest {
    public static void main(String[] args) throws Exception {
        Session session = Session.getInstance(new Properties());
        MimeMessage message = new MimeMessage(session);
        MailTemplateLogo.setHtmlContent(message, "<html><body>Test " + MailTemplateLogo.imageHtml() + "</body></html>");
        message.saveChanges();
        var output = new ByteArrayOutputStream(); message.writeTo(output);
        var parsed = new MimeMessage(session, new ByteArrayInputStream(output.toByteArray()));
        check(parsed.isMimeType("multipart/related"), "Inline image uses multipart/related");
        var parts = (MimeMultipart) parsed.getContent();
        check(parts.getCount() == 2, "One HTML part and one image");
        String body = parts.getBodyPart(0).getContent().toString();
        check(body.contains("cid:" + MailTemplateLogo.CONTENT_ID) && !body.contains("data:image"), "Email HTML uses CID, not a data URI");
        var image = parts.getBodyPart(1);
        check(image.isMimeType("image/png") && "inline".equals(image.getDisposition()), "PNG is inline");
        check(image.getHeader("Content-ID")[0].equals("<" + MailTemplateLogo.CONTENT_ID + ">"), "CID matches the HTML");
        byte[] png = Files.readAllBytes(Path.of("common/src/main/resources/mail-assets/eis-logo-light.png"));
        check(Arrays.equals(png, image.getInputStream().readAllBytes()), "Email embeds exactly the preview PNG");
        MimeMessage ordinary = new MimeMessage(session);
        MailTemplateLogo.setHtmlContent(ordinary, "<p>Existing mail without logo</p>");
        ordinary.saveChanges();
        check(ordinary.isMimeType("text/html"), "Existing emails without this logo keep their original format");
        System.out.println("PNG logo MIME tests passed. No email sent.");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
