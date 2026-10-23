package com.bepa.eis.server.api.web.application.misc;

import com.bepa.eis.common.GlobalConfiguration;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.customer.Subscription;
import com.bepa.eis.common.enums.menu.MenuIcon;
import com.bepa.eis.common.enums.menu.MenuItemType;
import com.bepa.eis.common.enums.user.UserRoles;
import com.bepa.eis.common.providers.SessionProvider;
import com.bepa.eis.common.providers.UserPreferenceProvider;
import com.bepa.eis.common.providers.UserProvider;
import com.bepa.eis.common.providers.misc.AuditEventProvider;
import com.bepa.eis.server.api.DTO.CustomerProject;
import com.bepa.eis.server.api.DTO.Menu;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.generic.GenericServlet;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.dataprovider.misc.CustomerProjectProvider;
import com.bepa.eis.server.dataprovider.misc.MenuProvider;
import com.bepa.eis.server.dataprovider.misc.MenuProvider.MenuRow;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Endpoint: GET /Menu
 * Returns XML with variable number of menu items and admin menu-editor data.
 */
@WebServlet(name = "MenuServlet", urlPatterns = "/Menu")
@MultipartConfig
public class MenuServlet extends GenericServlet {

    private static final Logger log = LoggerFactory.getLogger(MenuServlet.class);
    private static final String CMD_SELECT_PROJECT = "selectproject";
    private static final String PROJECT_OVERVIEW_URL = "/web/view?page=projectoverview";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String command = safeText(req.getParameter("cmd")).toLowerCase();

        WebSession webSession = getSession(req);

        if (command.isBlank()) {
            setXmlResponse(resp);

            try {
                MenuProvider menuProvider = new MenuProvider(webSession);
                Menu menu = menuProvider.getMenuItems(getSessionId(req));

                Document document = menu.toXmlDocument();
                document.getDocumentElement().setAttribute("development-mode", String.valueOf(GlobalConfiguration.isUdvMode()));
                appendTopPanel(document, webSession);

                appendProjects(document, webSession);

                String xml = toXmlString(document);
                resp.getWriter().write(xml);

            } catch (ParserConfigurationException | TransformerException e) {
                log.error("Error loading menu: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
            return;
        }

        if (CMD_SELECT_PROJECT.equals(command)) {
            handleSelectProject(req, resp);
            return;
        }

        if (!isSystemAdministrator(webSession)) {
            writeXml(resp, HttpServletResponse.SC_FORBIDDEN, errorXml("Forbidden"));
            return;
        }

        try {
            handleAdminGet(req, resp, webSession, command);
        } catch (Exception e) {
            log.error("Error loading menu editor data: {}", e.getMessage(), e);
            writeXml(resp, HttpServletResponse.SC_BAD_REQUEST, errorXml(e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String command = safeText(req.getParameter("cmd")).toLowerCase();

        if (command.isBlank()) {
            writeXml(resp, HttpServletResponse.SC_BAD_REQUEST, errorXml("cmd is required."));
            return;
        }

        WebSession webSession = getSession(req);

        if (CMD_SELECT_PROJECT.equals(command)) {
            handleSelectProject(req, resp);
            return;
        }

        if (!isSystemAdministrator(webSession)) {
            writeXml(resp, HttpServletResponse.SC_FORBIDDEN, errorXml("Forbidden"));
            return;
        }

        try {
            handleAdminPost(req, resp, webSession, command);
        } catch (Exception e) {
            log.error("Error processing menu editor request: {}", e.getMessage(), e);
            writeXml(resp, HttpServletResponse.SC_BAD_REQUEST, errorXml(e.getMessage()));
        }
    }

    private void handleAdminGet(
            HttpServletRequest req,
            HttpServletResponse resp,
            WebSession webSession,
            String command
    ) throws IOException, ParserConfigurationException, TransformerException {
        MenuProvider menuProvider = new MenuProvider(webSession);

        switch (command) {
            case "export" -> {
                String format = safeText(req.getParameter("format"));
                if (!format.isEmpty() && !"xml".equalsIgnoreCase(format)) {
                    throw new IllegalArgumentException("Menu export only supports XML.");
                }
                StringBuilder xml = new StringBuilder();
                appendXmlHeader(xml);
                xml.append("<menuExport>");
                appendMenuItems(xml, menuProvider.getAllMenuRows());
                xml.append("</menuExport>");
                resp.setHeader("Content-Disposition", "attachment; filename=\"Menu Editor.xml\"");
                writeXml(resp, HttpServletResponse.SC_OK, xml.toString());
            }
            case "list" -> writeXml(resp, HttpServletResponse.SC_OK, buildListXml(webSession, menuProvider));
            case "edit" -> {
                Integer menuId = intValue(req.getParameter("id"));
                MenuRow menuRow = menuProvider.getMenuRow(menuId);

                if (menuRow == null) {
                    throw new IllegalArgumentException("Menu item was not found.");
                }

                writeXml(resp, HttpServletResponse.SC_OK, buildDetailXml(webSession, menuProvider, menuRow));
            }
            case "create" -> {
                Integer parentMenuId = intValue(req.getParameter("parentId"));
                MenuRow menuRow = buildBlankMenuRow(parentMenuId);
                writeXml(resp, HttpServletResponse.SC_OK, buildDetailXml(webSession, menuProvider, menuRow));
            }
            default -> throw new IllegalArgumentException("Invalid request : " + command);
        }
    }

    private void handleAdminPost(
            HttpServletRequest req,
            HttpServletResponse resp,
            WebSession webSession,
            String command
    ) throws Exception {
        MenuProvider menuProvider = new MenuProvider(webSession);

        switch (command) {
            case "import" -> {
                Part file = req.getPart("file");
                if (file == null || file.getSize() == 0) {
                    throw new IllegalArgumentException("Import file is required.");
                }
                List<MenuRow> rows;
                try (InputStream input = file.getInputStream()) {
                    rows = parseMenuImport(input);
                }
                menuProvider.validateMenuImport(rows, menuProvider.getAllMenuRows());
                String phase = safeText(req.getParameter("phase"));
                if ("preview".equalsIgnoreCase(phase)) {
                    resp.setContentType("application/json; charset=UTF-8");
                    resp.setHeader("Cache-Control", "no-store");
                    new ObjectMapper().writeValue(resp.getWriter(), Map.of(
                            "title", "Import Menu Editor (matching IDs are updated)",
                            "rowCount", rows.size(), "errorCount", 0, "allValid", true,
                            "executeButtonText", "Import menus",
                            "columns", List.of(Map.of("key", "id", "label", "MenuId"),
                                    Map.of("key", "text", "label", "Menu item"),
                                    Map.of("key", "parent", "label", "ParentMenuId")),
                            "rows", rows.stream().map(row -> Map.of("valid", true, "error", "", "values",
                                    List.of(row.menuId().toString(), row.menuItemText(),
                                            row.parentMenuId() == null ? "" : row.parentMenuId().toString()))).toList()));
                } else if ("execute".equalsIgnoreCase(phase)) {
                    menuProvider.importMenuRows(rows);
                    writeXml(resp, HttpServletResponse.SC_OK, buildSaveResultXml(true, null, "Menu items imported."));
                } else {
                    throw new IllegalArgumentException("Import phase must be preview or execute.");
                }
            }
            case "save" -> {
                MenuRow menuRow = parseMenuRow(req.getInputStream());
                MenuRow savedRow = menuProvider.saveMenuRow(menuRow);
                writeXml(resp, HttpServletResponse.SC_OK, buildSaveResultXml(true, savedRow == null ? null : savedRow.menuId(), "Menu item saved."));
            }
            case "moveup", "movedown" -> {
                Integer menuId = intValue(req.getParameter("id"));
                if (menuId == null) {
                    throw new IllegalArgumentException("id is required.");
                }

                boolean moved = menuProvider.moveMenuRow(menuId, "moveup".equals(command));
                String message = moved ? "Menu item moved." : "Menu item is already at the edge.";
                writeXml(resp, HttpServletResponse.SC_OK, buildSaveResultXml(true, menuId, message));
            }
            default -> throw new IllegalArgumentException("Invalid request : " + command);
        }
    }

    private void handleSelectProject(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws IOException {
        String projectIdParameter = req.getParameter("projectId");
        Integer projectId = intValue(projectIdParameter);
        String sessionId = safeText(getSessionId(req));

        if (projectIdParameter == null || (!projectIdParameter.isBlank() && projectId == null)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (GlobalConfiguration.isUdvMode()) {
            getSession(req).setProjectId(projectId);
            writeXml(resp, HttpServletResponse.SC_OK, buildSelectProjectResultXml(PROJECT_OVERVIEW_URL));
            return;
        }

        if (sessionId.isBlank()) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        SessionProvider sessionProvider = new SessionProvider(null);

        try {
            WebSession webSession = sessionProvider.getBySessionId(sessionId);

            if (webSession == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            webSession.setSessionId(sessionId);
            webSession.setProjectId(projectId);

            boolean updated = sessionProvider.updateSessionInfo(webSession);

            if (updated) {
                new UserPreferenceProvider(webSession).setSelectedProjectId(webSession.getUserId(), projectId);
            }

            if (!updated) {
                log.warn(
                        "Project selection did not update any session rows. sessionId={}, customerId={}, projectId={}",
                        sessionId,
                        webSession.getCustomerId(),
                        projectId
                );
                writeXml(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorXml("Project selection could not be saved."));
                return;
            }

            resp.setHeader("Cache-Control", "no-store");
            resp.setHeader("Pragma", "no-cache");
            writeXml(
                    resp,
                    HttpServletResponse.SC_OK,
                    buildSelectProjectResultXml(PROJECT_OVERVIEW_URL)
            );
        } catch (SQLException e) {
            log.error(
                    "Error updating selected project in menu api. sessionId={}, customerId={}, projectId={}",
                    sessionId,
                    null,
                    projectId,
                    e
                );
            throw new RuntimeException(e);
        }
    }

    private String buildSelectProjectResultXml(String redirectUrl) {
        StringBuilder xml = new StringBuilder();
        appendXmlHeader(xml);
        xml.append("<projectSelectionResult>");
        appendElement(xml, "redirectUrl", redirectUrl);
        xml.append("</projectSelectionResult>");
        return xml.toString();
    }

    private String buildListXml(
            WebSession webSession,
            MenuProvider menuProvider
    ) throws ParserConfigurationException, TransformerException {
        StringBuilder xml = new StringBuilder();
        appendXmlHeader(xml);
        xml.append("<menuEditor>");
        appendTopPanel(xml, webSession, PageType.ADMIN_MENU_EDITOR_PAGE);
        appendLookups(xml, menuProvider);
        appendMenuItems(xml, menuProvider.getAllMenuRows());
        xml.append("</menuEditor>");
        return xml.toString();
    }

    private String buildDetailXml(
            WebSession webSession,
            MenuProvider menuProvider,
            MenuRow menuRow
    ) throws ParserConfigurationException, TransformerException {
        StringBuilder xml = new StringBuilder();
        appendXmlHeader(xml);
        xml.append("<menuEditor>");
        appendTopPanel(xml, webSession, PageType.ADMIN_MENU_EDITOR_PAGE);
        appendLookups(xml, menuProvider);
        appendMenuItemDetail(xml, menuRow);
        xml.append("</menuEditor>");
        return xml.toString();
    }

    private void appendTopPanel(
            StringBuilder xml,
            WebSession webSession,
            PageType pageType
    ) {
        xml.append("<TopPanel>");

        if (webSession != null) {
            try {
                TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
                TopPanel topPanel = topPanelProvider.getTopPanelBySession(pageType);

                if (topPanel != null && topPanel.getTopPanelElements() != null) {
                    topPanel.getTopPanelElements().getElements().forEach(field -> {
                        if (field == null || field.getFieldName() == null || field.getFieldName().isBlank()) {
                            return;
                        }

                        appendElement(xml, field.getFieldName(), field.toString());
                    });
                }
            } catch (Exception ignored) {
                // Best-effort for the admin page.
            }
        }

        xml.append("</TopPanel>");
    }

    private void appendProjects(
            Document document,
            WebSession webSession
    ) {
        if (document == null) {
            return;
        }

        Element root = document.getDocumentElement();

        if (root == null) {
            return;
        }

        if (webSession == null) {
            return;
        }

        try {
            CustomerProjectProvider customerProjectProvider = new CustomerProjectProvider(webSession);
            CustomerProject project = customerProjectProvider.getProjectsByUserId(webSession.getCustomerId(), webSession.getUserId());

            if (project == null) {
                return;
            }

            Document projectsDocument = project.toXmlProjectDocument();
            if (projectsDocument == null || projectsDocument.getDocumentElement() == null) {
                return;
            }

            Element projects = (Element) document.importNode(projectsDocument.getDocumentElement(), true);
            root.appendChild(projects);
        } catch (Exception e) {
            log.error("Error loading project data for menu: {}", e.getMessage(), e);
        }

    }

    private void appendTopPanel(
            Document document,
            WebSession webSession
    ) {
        if (document == null) {
            return;
        }

        Element root = document.getDocumentElement();

        if (root == null) {
            return;
        }

        Element topPanelElement = document.createElement("TopPanel");
        root.insertBefore(topPanelElement, root.getFirstChild());

        if (webSession == null) {
            return;
        }

        try {
            TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
            appendElement(topPanelElement, "CustomerId", webSession.getCustomerId());
            appendElement(topPanelElement, "ProjectId", webSession.getProjectId());
            appendElement(topPanelElement, "CustomerName", topPanelProvider.getCustomerName());
            appendElement(topPanelElement, "ProjectName", topPanelProvider.getProjectName());
            appendElement(topPanelElement, "UserName", topPanelProvider.getUserName());
        } catch (Exception ignored) {
            // Best-effort for the public menu payload.
        }
    }

    private void appendLookups(
            StringBuilder xml,
            MenuProvider menuProvider
    ) {
        xml.append("<lookups>");
        xml.append("<lookup name=\"userRoles\">");

        for (UserRoles role : UserRoles.values()) {
            if (role == UserRoles.INVALID_USER_ROLE || !role.isExternalUserRole() || !role.isActive()) {
                continue;
            }

            xml.append("<option");
            xml.append(" code=\"").append(escapeXml(String.valueOf(role.getId()))).append("\"");
            xml.append(" label=\"").append(escapeXml(role.getLabel())).append("\"");
            xml.append(" />");
        }

        xml.append("</lookup>");
        xml.append("<lookup name=\"subscriptions\">");

        for (Subscription subscription : Subscription.values()) {
            if (!subscription.isActive()) {
                continue;
            }

            xml.append("<option");
            xml.append(" code=\"").append(escapeXml(subscription.getModuleCode())).append("\"");
            xml.append(" label=\"").append(escapeXml(subscription.getLabel())).append("\"");
            xml.append(" />");
        }

        xml.append("</lookup>");
        xml.append("<lookup name=\"menuItemTypes\">");

        for (MenuItemType type : MenuItemType.values()) {
            xml.append("<option");
            xml.append(" code=\"").append(escapeXml(String.valueOf(type.getId()))).append("\"");
            xml.append(" label=\"").append(escapeXml(type.getLabel())).append("\"");
            xml.append(" />");
        }

        xml.append("</lookup>");
        xml.append("<menuIcons>");

        for (MenuIcon icon : MenuIcon.values()) {
            xml.append("<menuIcon>");
            appendElement(xml, "Id", icon.getId());
            appendElement(xml, "Name", icon.getName());
            appendElement(xml, "SvgCode", icon.getSvgCode());
            xml.append("</menuIcon>");
        }

        xml.append("</menuIcons>");
        xml.append("</lookups>");
    }

    private void appendMenuItems(
            StringBuilder xml,
            List<MenuRow> menuRows
    ) {
        xml.append("<menuItems>");

        if (menuRows != null) {
            for (MenuRow menuRow : menuRows) {
                appendMenuItem(xml, menuRow);
            }
        }

        xml.append("</menuItems>");
    }

    private void appendMenuItemDetail(
            StringBuilder xml,
            MenuRow menuRow
    ) {
        xml.append("<menuItemDetail>");
        appendMenuItemFields(xml, menuRow, true);
        xml.append("</menuItemDetail>");
    }

    private void appendMenuItem(
            StringBuilder xml,
            MenuRow menuRow
    ) {
        xml.append("<menuItem>");
        appendMenuItemFields(xml, menuRow, false);
        xml.append("</menuItem>");
    }

    private void appendMenuItemFields(
            StringBuilder xml,
            MenuRow menuRow,
            boolean includeDisplayOrder
    ) {
        appendElement(xml, "MenuId", menuRow == null ? null : menuRow.menuId());
        appendElement(xml, "MenuItemText", menuRow == null ? null : menuRow.menuItemText());
        appendElement(xml, "MenuItemUrl", menuRow == null ? null : menuRow.menuItemUrl());
        appendElement(xml, "ParentMenuId", menuRow == null ? null : menuRow.parentMenuId());
        appendElement(xml, "MenuItemType", menuRow == null ? null : menuRow.menuItemType());
        appendElement(xml, "SubscriptionCode", menuRow == null ? null : menuRow.subscriptionCode());
        appendElement(xml, "IconId", menuRow == null ? null : menuRow.iconId());
        appendElement(xml, "Description", menuRow == null ? null : menuRow.description());

        if (includeDisplayOrder) {
            appendElement(xml, "DisplayOrder", menuRow == null ? null : menuRow.displayOrder());
        } else {
            appendElement(xml, "DisplayOrder", menuRow == null ? null : menuRow.displayOrder());
        }

        appendElement(xml, "CustomerIdRequired", menuRow != null && Boolean.TRUE.equals(menuRow.customerIdRequired()));
        appendElement(xml, "ProjectIdRequired", menuRow != null && Boolean.TRUE.equals(menuRow.projectIdRequired()));
        appendElement(xml, "UserRoles", menuRow == null ? null : menuRow.userRoles());
        appendElement(xml, "Active", menuRow == null || menuRow.active() == null || menuRow.active());
    }

    private MenuRow parseMenuRow(InputStream inputStream) throws Exception {
        Document document = parseDocument(inputStream);
        Element root = document.getDocumentElement();
        Element menuElement = firstElement(root, "menuItem");

        if (menuElement == null && "menuItem".equalsIgnoreCase(root.getTagName())) {
            menuElement = root;
        }

        if (menuElement == null) {
            throw new IllegalArgumentException("Menu item data is required.");
        }

        return parseMenuElement(menuElement);
    }

    private MenuRow parseMenuElement(Element menuElement) {
        return new MenuRow(
                intValue(text(menuElement, "MenuId")),
                text(menuElement, "MenuItemText"),
                text(menuElement, "MenuItemUrl"),
                intValue(text(menuElement, "ParentMenuId")),
                intValue(text(menuElement, "MenuItemType")),
                text(menuElement, "SubscriptionCode"),
                intValue(text(menuElement, "IconId")),
                intValue(text(menuElement, "DisplayOrder")),
                boolValue(text(menuElement, "CustomerIdRequired"), false),
                boolValue(text(menuElement, "ProjectIdRequired"), false),
                text(menuElement, "UserRoles"),
                boolValue(text(menuElement, "Active"), true),
                text(menuElement, "Description")
        );
    }

    List<MenuRow> parseMenuImport(InputStream input) throws Exception {
        Element root = parseDocument(input).getDocumentElement();
        if (root == null || !"menuExport".equals(root.getTagName())) {
            throw new IllegalArgumentException("Import file must be a menu export XML file.");
        }
        List<MenuRow> rows = new ArrayList<>();
        Element items = firstElement(root, "menuItems");
        if (items != null) {
            for (var node = items.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node instanceof Element element && "menuItem".equals(element.getTagName())) {
                    // Reject malformed integers rather than silently dropping a parent or setting defaults.
                    for (String field : List.of("MenuId", "ParentMenuId", "MenuItemType", "IconId", "DisplayOrder")) {
                        String value = text(element, field);
                        if (!value.isEmpty() && intValue(value) == null) {
                            throw new IllegalArgumentException("Invalid integer in " + field);
                        }
                    }
                    for (String field : List.of("CustomerIdRequired", "ProjectIdRequired", "Active")) {
                        String value = text(element, field);
                        if (!value.isEmpty() && !"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                            throw new IllegalArgumentException("Invalid boolean in " + field);
                        }
                    }
                    rows.add(parseMenuElement(element));
                }
            }
        }
        return rows;
    }

    private MenuRow buildBlankMenuRow(Integer parentMenuId) {
        return new MenuRow(
                null,
                "",
                "",
                parentMenuId,
                MenuItemType.MENU_ITEM.getId(),
                "",
                null,
                null,
                Boolean.FALSE,
                Boolean.FALSE,
                "",
                Boolean.TRUE,
                ""
        );
    }

    private String buildSaveResultXml(
            boolean success,
            Integer menuId,
            String message
    ) {
        StringBuilder xml = new StringBuilder();
        appendXmlHeader(xml);
        xml.append("<menuEditorSaveResult>");
        appendElement(xml, "success", success);
        appendElement(xml, "menuId", menuId);
        appendElement(xml, "message", message);
        xml.append("</menuEditorSaveResult>");
        return xml.toString();
    }

    private void logAuditEvent(
            String actorEmail,
            String eventType,
            String description,
            String status
    ) {
        try {
            AuditEventProvider auditEventProvider = new AuditEventProvider(null);

            auditEventProvider.logEvent(new AuditEventProvider.AuditEvent(
                    safeActor(actorEmail),
                    eventType,
                    "SESSION",
                    safeActor(actorEmail),
                    description,
                    status
            ));
        } catch (Exception e) {
            log.warn("Could not write project selection audit event. eventType={}, actor={}", eventType, actorEmail, e);
        }
    }

    private String safeActor(String actorEmail) {
        if (actorEmail == null || actorEmail.isBlank()) {
            return "unknown";
        }

        return actorEmail;
    }

    private String errorXml(String message) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><error>" + escapeXml(message) + "</error>";
    }

    private boolean isSystemAdministrator(WebSession webSession) {
        if (webSession == null || webSession.getUserId() == null) {
            return false;
        }

        UserProvider userProvider = new UserProvider(webSession);
        UserProvider.UserAdministrationRow currentUser = userProvider.getUserAdministrationRow(webSession.getUserId());

        return currentUser != null && currentUser.userRole() == UserRoles.BEPA_SYSTEM_ADMINISTRATOR;
    }

    private Document parseDocument(InputStream inputStream) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

        return factory.newDocumentBuilder().parse(inputStream);
    }

    private void appendXmlHeader(StringBuilder xml) {
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    }

    private void appendElement(
            StringBuilder xml,
            String elementName,
            Object value
    ) {
        xml.append("<").append(elementName).append(">");

        if (value != null) {
            xml.append(escapeXml(String.valueOf(value)));
        }

        xml.append("</").append(elementName).append(">");
    }

    private void appendElement(
            Element parent,
            String elementName,
            Object value
    ) {
        if (parent == null || elementName == null || elementName.isBlank()) {
            return;
        }

        Document document = parent.getOwnerDocument();
        Element element = document.createElement(elementName);

        if (value != null) {
            element.setTextContent(String.valueOf(value));
        }

        parent.appendChild(element);
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String text(
            Element parent,
            String tagName
    ) {
        if (parent == null || tagName == null || tagName.isBlank()) {
            return "";
        }

        Element element = firstElement(parent, tagName);

        if (element == null || element.getTextContent() == null) {
            return "";
        }

        return element.getTextContent().trim();
    }

    private Element firstElement(
            Element parent,
            String tagName
    ) {
        if (parent == null || tagName == null || tagName.isBlank()) {
            return null;
        }

        org.w3c.dom.NodeList nodes = parent.getElementsByTagName(tagName);

        if (nodes.getLength() == 0 || !(nodes.item(0) instanceof Element element)) {
            return null;
        }

        return element;
    }

    private Integer intValue(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean boolValue(
            String value,
            boolean defaultValue
    ) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return Boolean.parseBoolean(value.trim());
    }

    private void writeXml(
            HttpServletResponse response,
            int status,
            String xml
    ) throws IOException {
        response.setStatus(status);
        response.setContentType("application/xml; charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        response.getWriter().write(xml == null ? "" : xml);
    }

    private String escapeXml(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
