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

@WebServlet(name = "MyProfileServlet", urlPatterns = {
        "/api/myprofile"
})
public class MyProfileServlet extends GenericDataProviderServlet {

    @Override
    public void handleImport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) {
        throw new UnsupportedOperationException("My Profile import is not supported.");
    }

    @Override
    public void handleSave(WebSession webSession, HttpServletRequest request, Element rootElement) {
        ensureAuthenticated(webSession);

        UserProvider userProvider = new UserProvider(webSession);
        UserProvider.UserAdministrationRow existingUser = userProvider.getUserAdministrationRow(webSession.getUserId());
        UserProvider.UserAdministrationRow submittedUser = parseUser(rootElement);

        if (existingUser == null) {
            throw new IllegalStateException("Current user could not be loaded.");
        }

        if (submittedUser == null) {
            throw new IllegalArgumentException("Missing user in save payload.");
        }

        UserProvider.UserAdministrationRow userToSave = new UserProvider.UserAdministrationRow(
                existingUser.userId(),
                submittedUser.initials(),
                submittedUser.name(),
                existingUser.email(),
                submittedUser.phone(),
                submittedUser.departmentId(),
                existingUser.active(),
                existingUser.userRole(),
                submittedUser.themeId(),
                existingUser.lockedUntil(),
                existingUser.mfaEnabled(),
                existingUser.mfaVerified(),
                existingUser.mfaSecretEncrypted(),
                existingUser.userMfaPolicy(),
                existingUser.mfaResetRequired(),
                existingUser.mfaResetAt(),
                existingUser.mfaResetByUserId(),
                existingUser.password(),
                existingUser.lastLoginAt(),
                existingUser.departmentName(),
                existingUser.departmentDescription(),
                existingUser.customerNames()
        );

        boolean saved = userProvider.saveUserAdministration(userToSave, webSession.getCustomerId());

        if (!saved) {
            throw new IllegalStateException("My Profile could not be saved.");
        }

        if (webSession.getCustomerId() != null) {
            EhcacheProvider.clearCacheEntry(webSession.getCustomerId());
        }
    }

    @Override
    public GenericXmlDocument handleListOfEntities(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Throwable {
        return handleEditEntity(webSession, request, response, null, null);
    }

    @Override
    public GenericXmlDocument handleEditEntity(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response,
            Integer entityId,
            Integer version
    ) throws Throwable {
        ensureAuthenticated(webSession);

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
    ) throws Throwable {
        return handleEditEntity(webSession, request, response, null, null);
    }

    @Override
    public void handleExport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) {
        throw new UnsupportedOperationException("My Profile export is not supported.");
    }

    @Override
    public GenericXmlDocument handleOverview(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Throwable {
        return handleEditEntity(webSession, request, response, null, null);
    }

    private void ensureAuthenticated(WebSession webSession) {
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
                integerValue(fieldValue(userElement, "UserId")),
                elementText(userElement, "Initials"),
                elementText(userElement, "Name"),
                firstNonBlank(elementText(userElement, "UserEmail"), elementText(userElement, "Email")),
                firstNonBlank(elementText(userElement, "UserPhone"), elementText(userElement, "Phone")),
                integerValue(fieldValue(userElement, "DepartmentId")),
                true,
                null,
                integerValue(fieldValue(userElement, "ThemeId")),
                null,
                false,
                false,
                "",
                "",
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
}
