package com.bepa.eis.server.api.web.application.misc;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.user.UserRoles;
import com.bepa.eis.common.providers.UserProvider;
import com.bepa.eis.server.api.DTO.Menu;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.generic.GenericServlet;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.dataprovider.misc.MenuProvider;
import com.bepa.eis.server.dataprovider.misc.MenuProvider.MenuRow;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import java.util.List;

/**
 * Endpoint: GET /Menu
 * Returns XML with variable number of menu items and admin menu-editor data.
 */
@WebServlet(name = "MenuServlet", urlPatterns = "/Menu")
public class MenuServlet extends GenericServlet {

    private static final Logger log = LoggerFactory.getLogger(MenuServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String command = safeText(req.getParameter("cmd")).toLowerCase();

        WebSession webSession = getSession(req);

        if (command.isBlank()) {
            setXmlResponse(resp);

            try {
                MenuProvider menuProvider = new MenuProvider(webSession);
                Menu menu = menuProvider.getMenuItems(getSessionId(req));
                String xml = toXmlString(menu.toXmlDocument());
                resp.getWriter().write(xml);
            } catch (ParserConfigurationException | TransformerException e) {
                log.error("Error loading menu: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
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

    private String buildListXml(
            WebSession webSession,
            MenuProvider menuProvider
    ) throws ParserConfigurationException, TransformerException {
        StringBuilder xml = new StringBuilder();
        appendXmlHeader(xml);
        xml.append("<menuEditor>");
        appendTopPanel(xml, webSession);
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
        appendTopPanel(xml, webSession);
        appendLookups(xml, menuProvider);
        appendMenuItemDetail(xml, menuRow);
        xml.append("</menuEditor>");
        return xml.toString();
    }

    private void appendTopPanel(
            StringBuilder xml,
            WebSession webSession
    ) {
        xml.append("<TopPanel>");

        if (webSession != null) {
            try {
                TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
                TopPanel topPanel = topPanelProvider.getTopPanelBySession();

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

    private void appendLookups(
            StringBuilder xml,
            MenuProvider menuProvider
    ) {
        xml.append("<lookups>");
        xml.append("<lookup name=\"userRoles\">");

        for (UserRoles role : UserRoles.values()) {
            if (role == UserRoles.INVASIVE_USER_ROLE) {
                continue;
            }

            xml.append("<option");
            xml.append(" code=\"").append(escapeXml(String.valueOf(role.getId()))).append("\"");
            xml.append(" label=\"").append(escapeXml(role.getLabel())).append("\"");
            xml.append(" />");
        }

        xml.append("</lookup>");
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

        return new MenuRow(
                intValue(text(menuElement, "MenuId")),
                text(menuElement, "MenuItemText"),
                text(menuElement, "MenuItemUrl"),
                intValue(text(menuElement, "ParentMenuId")),
                intValue(text(menuElement, "DisplayOrder")),
                boolValue(text(menuElement, "CustomerIdRequired"), false),
                boolValue(text(menuElement, "ProjectIdRequired"), false),
                text(menuElement, "UserRoles"),
                boolValue(text(menuElement, "Active"), true)
        );
    }

    private MenuRow buildBlankMenuRow(Integer parentMenuId) {
        return new MenuRow(
                null,
                "",
                "",
                parentMenuId,
                null,
                Boolean.FALSE,
                Boolean.FALSE,
                "",
                Boolean.TRUE
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
