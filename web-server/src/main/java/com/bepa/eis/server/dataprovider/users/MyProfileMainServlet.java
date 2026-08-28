package com.bepa.eis.server.dataprovider.users;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.UserProvider;
import com.bepa.eis.server.api.generic.GenericDataProviderServlet;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.enums.EntityRequestType;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.dataprovider.cache.EhcacheProvider;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.w3c.dom.Element;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@WebServlet(name = "MyProfileMainServlet", urlPatterns = {
        "/api/myprofile-main"
})
public class MyProfileMainServlet extends GenericDataProviderServlet {

    @Override
    public void handleImport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) {
        throw new UnsupportedOperationException("My Profile import is not supported.");
    }

    @Override
    public void handleSave(WebSession webSession, HttpServletRequest request, Element rootElement) {
        ensureCanAccess(webSession);

        UserProvider userProvider = new UserProvider(webSession);
        UserProvider.UserAdministrationRow user = parseUser(rootElement);

        if (user == null) {
            throw new IllegalArgumentException("Missing user in save payload.");
        }

        UserProvider.UserAdministrationRow currentUser = userProvider.getUserAdministrationRow(webSession.getUserId());

        if (currentUser == null) {
            throw new IllegalArgumentException("Current user was not found.");
        }

        UserProvider.UserAdministrationRow safeUser = new UserProvider.UserAdministrationRow(
                webSession.getUserId(),
                user.initials(),
                user.name(),
                currentUser.email(),
                user.phone(),
                user.departmentId(),
                currentUser.active(),
                currentUser.userRole(),
                user.themeId(),
                currentUser.lockedUntil(),
                currentUser.mfaEnabled(),
                currentUser.mfaVerified(),
                currentUser.mfaSecretEncrypted(),
                currentUser.userMfaPolicy(),
                currentUser.mfaResetRequired(),
                currentUser.mfaResetAt(),
                currentUser.mfaResetByUserId(),
                currentUser.password(),
                currentUser.lastLoginAt(),
                currentUser.departmentName(),
                currentUser.departmentDescription(),
                currentUser.customerNames()
        );

        Integer customerId = webSession.getCustomerId();
        boolean saved = userProvider.saveUserAdministration(safeUser, customerId, List.of());

        if (!saved) {
            throw new IllegalStateException("User could not be saved.");
        }

        if (customerId != null) {
            EhcacheProvider.clearCacheEntry(customerId);
        }
    }

    @Override
    public GenericXmlDocument handleListOfEntities(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        throw new UnsupportedOperationException("My profile list is not supported.");
    }

    @Override
    public GenericXmlDocument handleEditEntity(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response,
            Integer entityId,
            Integer version
    ) throws Throwable {
        ensureCanAccess(webSession);
        return new UserMainInfo(
                webSession,
                EntityRequestType.EDIT_ENTITY,
                webSession.getUserId(),
                PageType.MY_PROFILE_EDIT_PAGE,
                false,
                true
        );
    }

    @Override
    public GenericXmlDocument handleCreateEntity(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response,
            Integer parentEntityId
    ) {
        throw new UnsupportedOperationException("My profile create is not supported.");
    }

    @Override
    public void handleExport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) {
        throw new UnsupportedOperationException("My profile export is not supported.");
    }

    @Override
    public GenericXmlDocument handleOverview(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Throwable {
        return handleEditEntity(webSession, request, response, null, null);
    }

    private void ensureCanAccess(WebSession webSession) {
        if (webSession == null || webSession.getUserId() == null) {
            throw new SecurityException("Forbidden");
        }
    }

    private UserProvider.UserAdministrationRow parseUser(Element root) {
        Element userElement = firstElement(root, "userDocument");

        if (userElement != null) {
            userElement = firstElement(userElement, "user");
        }

        if (userElement == null) {
            userElement = firstElement(root, "user");
        }

        if (userElement == null) {
            return null;
        }

        return new UserProvider.UserAdministrationRow(
                integerValue(userElement, "UserId"),
                elementText(userElement, "Initials"),
                elementText(userElement, "Name"),
                firstNonBlank(elementText(userElement, "UserEmail"), elementText(userElement, "Email")),
                firstNonBlank(elementText(userElement, "UserPhone"), elementText(userElement, "Phone")),
                integerValue(fieldValue(userElement, "DepartmentId")),
                booleanValue(fieldValue(userElement, "Active"), true),
                null,
                integerValue(fieldValue(userElement, "ThemeId")),
                timestampValue(fieldValue(userElement, "LockedUntil")),
                false,
                false,
                "",
                fieldValue(userElement, "UserMfaPolicy"),
                false,
                null,
                null,
                "",
                null,
                "",
                "",
                ""
        );
    }

    private Element firstElement(Element parent, String tagName) {
        if (parent == null || tagName == null || tagName.isBlank()) {
            return null;
        }

        for (int index = 0; index < parent.getChildNodes().getLength(); index++) {
            org.w3c.dom.Node node = parent.getChildNodes().item(index);

            if (node instanceof Element element && tagName.equals(element.getTagName())) {
                return element;
            }
        }

        return null;
    }

    private String elementText(Element parent, String tagName) {
        Element element = firstElement(parent, tagName);
        return element == null || element.getTextContent() == null ? "" : element.getTextContent().trim();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }

        return second == null ? "" : second;
    }

    private String fieldValue(Element parent, String tagName) {
        Element element = firstElement(parent, tagName);

        if (element == null) {
            return "";
        }

        Element valueNode = firstElement(element, "Value");

        if (valueNode != null && valueNode.getTextContent() != null) {
            return valueNode.getTextContent().trim();
        }

        return element.getTextContent() == null ? "" : element.getTextContent().trim();
    }

    private Integer integerValue(Element parent, String tagName) {
        return integerValue(elementText(parent, tagName));
    }

    private Integer integerValue(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean booleanValue(String value, boolean defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return Boolean.parseBoolean(value.trim());
    }

    private Timestamp timestampValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().replace(' ', 'T');

        try {
            if (normalized.length() == 16) {
                normalized = normalized + ":00";
            }

            return Timestamp.valueOf(LocalDateTime.parse(normalized));
        } catch (Exception ignored) {
            return null;
        }
    }
}
