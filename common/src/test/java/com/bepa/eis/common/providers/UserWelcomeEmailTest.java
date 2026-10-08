package com.bepa.eis.common.providers;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.mail.MailRecipient;
import com.bepa.eis.common.enums.mail.MailTemplateType;
import com.bepa.eis.common.enums.user.UserRoles;
import com.bepa.eis.common.providers.mail.MailProvider;
import com.bepa.eis.common.providers.mail.MailTemplateRenderer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/** Standalone welcome-flow tests; no real users or emails are created. */
public final class UserWelcomeEmailTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("eis-welcome-test-");
        Path config = directory.resolve("eis-global.properties");
        Files.writeString(config, "mail.template.folder=" + directory.toString().replace('\\', '/') + "\n");
        System.setProperty("eis.config.file", config.toString());

        WebSession administrator = new WebSession();
        administrator.setUserId(17);
        FakeUsers users = new FakeUsers(administrator);
        String baseUrl = "https://demo.bepa-se.com";
        var result = users.saveUserAdministrationWithInvitation(user(null), 1, List.of(), baseUrl);
        check(result.saved() && result.created() && result.invitationQueued(), "New user is saved and invited");
        check(result.userId() == 57 && users.invitedUserId == 57, "Invite targets the generated user ID");
        check(users.creatorId == 17, "Token records the administrator who created the account");
        check(users.mailCount == 1 && users.template == MailTemplateType.USER_CREATED,
                "Creation sends the distinct welcome template");
        check(users.link.equals(baseUrl + "/enter-new-password.html?token=welcome-token"),
                "Invitation reuses the password setup page and environment URL");
        check("USER_WELCOME_LINK_QUEUED".equals(users.auditEvent), "Welcome queueing is audited distinctly");

        result = users.saveUserAdministrationWithInvitation(user(57), 1, List.of(), null);
        check(result.saved() && !result.created() && users.mailCount == 1,
                "Editing a user does not resend the invitation");

        users.saveAllowed = false;
        result = users.saveUserAdministrationWithInvitation(user(null), 1, List.of(), baseUrl);
        check(!result.saved() && !result.invitationQueued() && users.mailCount == 1,
                "Failed user creation does not send an email");
        users.saveAllowed = true;
        users.queueAllowed = false;
        result = users.saveUserAdministrationWithInvitation(user(null), 1, List.of(), baseUrl);
        check(result.saved() && result.created() && !result.invitationQueued(),
                "A mail failure does not falsely report the persisted user as unsaved");
        check("USER_WELCOME_LINK_FAILED".equals(users.auditEvent), "Welcome mail failure is audited");

        users.queueAllowed = true;
        users.tokenAllowed = false;
        int mailCount = users.mailCount;
        result = users.saveUserAdministrationWithInvitation(user(null), 1, List.of(), baseUrl);
        check(result.saved() && !result.invitationQueued() && users.mailCount == mailCount,
                "Token creation failure does not queue a broken welcome email");
        users.tokenAllowed = true;
        check(users.sendPasswordResetLink(57, 17, baseUrl), "Existing reset path still works");
        check(users.template == MailTemplateType.PASSWORD_RESET, "Reset retains its separate template");

        MailTemplateRenderer renderer = new MailTemplateRenderer();
        var welcome = renderer.render(MailTemplateType.USER_CREATED, Map.of("resetLink", users.link));
        check(welcome.getSourceFileName().equals("classpath:/mail-templates/user-created.mail"),
                "Welcome template ships inside the application");
        check(welcome.getBody().contains("Your administrator has created an EIS account for you."),
                "Welcome email explains who created the account");
        check(welcome.getBody().contains("Set your password") && welcome.getBody().contains(users.link),
                "Welcome email asks the recipient to set their password with a usable link");
        check(!welcome.getBody().contains("{{resetLink}}"), "Welcome placeholder is substituted");
        Files.writeString(directory.resolve("user-created.mail"),
                "Subject: External welcome\nContent-Type: text/plain; charset=UTF-8\n\nSet password: {{resetLink}}\n");
        welcome = renderer.render(MailTemplateType.USER_CREATED, Map.of("resetLink", users.link));
        check("External welcome".equals(welcome.getSubject()), "External welcome template remains supported");
        Files.delete(directory.resolve("user-created.mail"));
        Files.delete(config);
        Files.delete(directory);
        System.out.println("User welcome email regression tests passed.");
    }

    private static UserProvider.UserAdministrationRow user(Integer id) {
        return new UserProvider.UserAdministrationRow(id, "NU", "New User", "new-user@example.test", null,
                null, true, UserRoles.PROJECT_MEMBER, null, null, false, false, null, "DEFAULT", false,
                null, null, null, null, null, null, null);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class FakeUsers extends UserProvider {
        boolean saveAllowed = true;
        boolean queueAllowed = true;
        boolean tokenAllowed = true;
        boolean persisted;
        int mailCount;
        Integer invitedUserId;
        Integer creatorId;
        MailTemplateType template;
        String link;
        String auditEvent;

        FakeUsers(WebSession session) { super(session); }

        @Override protected Integer persistUserAdministration(UserAdministrationRow user, Integer customerId,
                                                               List<UserProjectAccessRow> projects) {
            persisted = saveAllowed;
            return saveAllowed ? (user.userId() == null ? 57 : user.userId()) : null;
        }

        @Override public UserAdministrationRow getUserAdministrationRow(Integer id) { return user(id); }

        @Override public PasswordResetTokenResult createPasswordResetToken(Integer id, Integer creator, String baseUrl) {
            check(persisted, "User must be persisted before creating the invitation");
            invitedUserId = id;
            creatorId = creator;
            link = baseUrl + "/enter-new-password.html?token=welcome-token";
            return tokenAllowed ? new PasswordResetTokenResult(1, id, "welcome-token", "token-hash",
                    new Timestamp(System.currentTimeMillis() + 86_400_000), null, null, creator, link) : null;
        }

        @Override protected MailProvider createPasswordMailProvider() {
            return new MailProvider(getWebSession()) {
                @Override public Integer createMail(MailRecipient to, MailTemplateType type, Map<String, Object> parameters) {
                    mailCount++;
                    template = type;
                    check("new-user@example.test".equals(to.getEmail()), "Welcome targets the new user email");
                    check(link.equals(parameters.get("resetLink")), "The queued mail receives its setup link");
                    return queueAllowed ? 1 : null;
                }
            };
        }

        @Override protected void logUserAdministrationEvent(String type, String email, String description, String status) {
            auditEvent = type;
        }
    }
}
