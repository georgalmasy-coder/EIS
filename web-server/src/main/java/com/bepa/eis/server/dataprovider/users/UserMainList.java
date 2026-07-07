package com.bepa.eis.server.dataprovider.users;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.UserProvider;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import org.w3c.dom.Element;

import java.sql.Timestamp;

public class UserMainList extends GenericXmlDocument {

    public UserMainList(WebSession webSession) throws Exception {
        super(webSession);

        initXmlDocument("UserMain");
        appendTopPanel(webSession);
        appendUsers(webSession);
    }

    private void appendTopPanel(WebSession webSession) throws Exception {
        Element topPanelElement = getDoc().createElement("TopPanel");
        getRoot().appendChild(topPanelElement);

        if (webSession == null) {
            return;
        }

        try {
            TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
            TopPanel topPanel = topPanelProvider.getTopPanelBySession();

            if (topPanel != null && topPanel.getTopPanelElements() != null) {
                topPanel.getTopPanelElements().getElements().forEach(field -> {
                    if (field == null || field.getFieldName() == null || field.getFieldName().isBlank()) {
                        return;
                    }

                    appendElement(topPanelElement, field.getFieldName(), field.toString());
                });
            }
        } catch (Exception ignored) {
            // Best effort only.
        }
    }

    private void appendUsers(WebSession webSession) {
        Element usersElement = getDoc().createElement("users");
        getRoot().appendChild(usersElement);

        UserProvider userProvider = new UserProvider(webSession);
        for (UserProvider.UserAdministrationRow user : userProvider.getUserMainRows(webSession == null ? null : webSession.getCustomerId())) {
            Element userElement = getDoc().createElement("user");
            usersElement.appendChild(userElement);

            appendElement(userElement, "userId", user == null ? null : user.userId());
            appendElement(userElement, "name", user == null ? null : user.name());
            appendElement(userElement, "userRole", user == null || user.userRole() == null ? null : user.userRole().getId());
            appendElement(userElement, "userRoleLabel", user == null || user.userRole() == null ? null : user.userRole().getLabel());
            appendElement(userElement, "email", user == null ? null : user.email());
            appendElement(userElement, "phone", user == null ? null : user.phone());
            appendElement(userElement, "departmentDescription", user == null ? null : user.departmentDescription());
            appendElement(userElement, "lastLoginAt", timestampText(user == null ? null : user.lastLoginAt()));
            appendElement(userElement, "active", user != null && user.active());
        }
    }

    private void appendElement(
            Element parent,
            String elementName,
            Object value
    ) {
        Element element = getDoc().createElement(elementName);

        if (value != null) {
            element.setTextContent(String.valueOf(value));
        }

        parent.appendChild(element);
    }

    private String timestampText(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }

        return timestamp.toLocalDateTime().toString();
    }
}
