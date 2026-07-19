package com.bepa.eis.server.dataprovider.users;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.user.UserRoles;
import com.bepa.eis.common.providers.UserProvider;
import com.bepa.eis.common.providers.UserProvider.UserProjectAccessRow;
import com.bepa.eis.server.api.generic.GenericDataProviderServlet;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.enums.EntityRequestType;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.w3c.dom.Element;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@WebServlet(name = "UserMainServlet", urlPatterns = {
        "/api/user-main"
})
public class UserMainServlet extends GenericDataProviderServlet {

    @Override
    public void handleImport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) {
        throw new UnsupportedOperationException("User import is not supported.");
    }

    @Override
    public void handleSave(WebSession webSession, HttpServletRequest request, Element rootElement) {
        ensureCanAccess(webSession);

        UserProvider userProvider = new UserProvider(webSession);
        UserProvider.UserAdministrationRow user = parseUser(rootElement);
        List<Integer> customerIds = parseCustomerIds(rootElement);
        List<UserProjectAccessRow> projectAccessRows = parseProjectAccessRows(rootElement);

        if (user == null) {
            throw new IllegalArgumentException("Missing user in save payload.");
        }

        if (user.userId() == null && customerIds.isEmpty() && webSession != null && webSession.getCustomerId() != null) {
            customerIds = List.of(webSession.getCustomerId());
        }

        boolean saved = userProvider.saveUserAdministration(user, customerIds, projectAccessRows);

        if (!saved) {
            throw new IllegalStateException("User could not be saved.");
        }
    }

    @Override
    public GenericXmlDocument handleListOfEntities(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Throwable {
        ensureCanAccess(webSession);
        return new UserMainList(webSession);
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

        if (entityId == null) {
            throw new IllegalArgumentException("Missing user id.");
        }

        return new UserMainInfo(webSession, EntityRequestType.EDIT_ENTITY, entityId);
    }

    @Override
    public GenericXmlDocument handleCreateEntity(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response,
            Integer parentEntityId
    ) throws Throwable {
        ensureCanAccess(webSession);
        return new UserMainInfo(webSession, EntityRequestType.CREATE_ENTITY, null);
    }

    @Override
    public void handleExport(WebSession webSession, HttpServletRequest request, HttpServletResponse response) {
        throw new UnsupportedOperationException("User export is not supported.");
    }

    @Override
    public GenericXmlDocument handleOverview(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Throwable {
        return handleListOfEntities(webSession, request, response);
    }

    private void ensureCanAccess(WebSession webSession) {
        if (!canViewUserMain(webSession)) {
            throw new SecurityException("Forbidden");
        }
    }

    private boolean canViewUserMain(WebSession webSession) {
        if (webSession == null || webSession.getUserId() == null) {
            return false;
        }

        UserProvider userProvider = new UserProvider(webSession);
        UserProvider.UserAdministrationRow currentUser = userProvider.getUserAdministrationRow(webSession.getUserId());

        if (currentUser == null || currentUser.userRole() == null) {
            return false;
        }

        return currentUser.userRole() == UserRoles.BEPA_SYSTEM_ADMINISTRATOR
                || currentUser.userRole() == UserRoles.CUSTOMER_ADMINISTRATOR;
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
                UserRoles.fromIdOrDefault(integerValue(fieldValue(userElement, "UserRole")), UserRoles.CUSTOMER_ADMINISTRATOR),
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

    private List<Integer> parseCustomerIds(Element root) {
        Element linkedCustomers = firstElement(root, "linkedCustomers");
        List<Integer> customerIds = new ArrayList<>();

        if (linkedCustomers == null) {
            return customerIds;
        }

        LinkedHashSet<Integer> uniqueIds = new LinkedHashSet<>();

        for (int index = 0; index < linkedCustomers.getElementsByTagName("customerId").getLength(); index++) {
            org.w3c.dom.Node node = linkedCustomers.getElementsByTagName("customerId").item(index);
            Integer customerId = integerValue(node == null ? null : node.getTextContent());

            if (customerId != null) {
                uniqueIds.add(customerId);
            }
        }

        customerIds.addAll(uniqueIds);
        return customerIds;
    }

    private List<UserProjectAccessRow> parseProjectAccessRows(Element root) {
        Element userProjects = firstElement(root, "userProjects");
        List<UserProjectAccessRow> rows = new ArrayList<>();

        if (userProjects == null) {
            return rows;
        }

        for (int index = 0; index < userProjects.getElementsByTagName("project").getLength(); index++) {
            org.w3c.dom.Node node = userProjects.getElementsByTagName("project").item(index);

            if (!(node instanceof Element projectElement)) {
                continue;
            }

            rows.add(new UserProjectAccessRow(
                    integerValue(fieldValue(projectElement, "ProjectId")),
                    elementText(projectElement, "ProjectName"),
                    booleanValue(fieldValue(projectElement, "Selected"), false)
            ));
        }

        return rows;
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
