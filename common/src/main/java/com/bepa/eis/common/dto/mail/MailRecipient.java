package com.bepa.eis.common.dto.mail;

public class MailRecipient {

    private String name;
    private String email;

    public MailRecipient() {
        name = "";
        email = "";
    }

    public MailRecipient(String name, String email) {
        this.name = safeText(name);
        this.email = safeText(email);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = safeText(name);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = safeText(email);
    }

    public boolean hasEmail() {
        return email != null && !email.trim().isEmpty();
    }

    public boolean isValid() {
        return hasEmail()
                && email.contains("@")
                && email.indexOf("@") > 0
                && email.indexOf("@") < email.length() - 1;
    }

    public String getDisplayName() {
        if (name == null || name.trim().isEmpty()) {
            return email;
        }

        if (email == null || email.trim().isEmpty()) {
            return name;
        }

        return name + " <" + email + ">";
    }

    public static MailRecipient of(String name, String email) {
        return new MailRecipient(name, email);
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public String toString() {
        return "MailRecipient [name=" + name + ", email=" + email + "]";
    }
}