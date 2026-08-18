package com.bepa.eis.server.api.web.application.admin;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.customer.CustomerRecord;
import com.bepa.eis.common.enums.user.UserRoles;
import com.bepa.eis.common.providers.UserProvider;
import com.bepa.eis.common.providers.UserProvider.UserProjectAccessRow;
import com.bepa.eis.common.providers.customer.CustomerRecordProvider;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.api.web.application.enums.theme.Theme;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.dataprovider.cache.EhcacheProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;

@WebServlet(name = "AdminUserAdministrationServlet", urlPatterns = {
        "/api/admin/users"
})
public class AdminUserAdministrationServlet extends AbstractAdminServlet {

    private static final Logger log = LoggerFactory.getLogger(AdminUserAdministrationServlet.class);

    @Override
    public void processGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        WebSession webSession = getWebSessionFromRequest(request, false);

        if (!isSystemAdministrator(webSession)) {
            writeXml(response, HttpServletResponse.SC_FORBIDDEN, errorXml("Forbidden"));
            return;
        }

        Integer userId = intValue(request.getParameter("userId"));
        Integer customerId = resolveCustomerId(webSession, request);

        writeXml(
                response,
                HttpServletResponse.SC_OK,
                userId == null ? buildListXml(webSession, customerId) : buildDetailXml(webSession, userId)
        );
    }

    @Override
    public void processPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        WebSession webSession = getWebSessionFromRequest(request, false);

        if (!isSystemAdministrator(webSession)) {
            writeXml(response, HttpServletResponse.SC_FORBIDDEN, errorXml("Forbidden"));
            return;
        }

        String action = safeText(request.getParameter("action"));

        if (!action.isBlank()) {
            writeXml(
                    response,
                    HttpServletResponse.SC_OK,
                    handleAction(webSession, request, action)
            );
            return;
        }

        writeXml(
                response,
                HttpServletResponse.SC_OK,
                saveUserAdministration(webSession, request)
        );
    }

    private String buildListXml(WebSession webSession, Integer customerId) {
        UserProvider userProvider = new UserProvider(webSession);

        StringBuilder xml = new StringBuilder();
        appendXmlHeader(xml);
        xml.append("<userAdministration>");
        appendElement(xml, "customerId", customerId);
        appendTopPanel(xml, webSession);

        appendLookups(xml);

        xml.append("<users>");
        for (UserProvider.UserAdministrationRow user : userProvider.getUserAdministrationRows()) {
            appendUserRow(xml, user);
        }
        xml.append("</users>");

        xml.append("</userAdministration>");
        return xml.toString();
    }

    private String buildDetailXml(WebSession webSession, Integer userId) {
        UserProvider userProvider = new UserProvider(webSession);
        CustomerRecordProvider customerRecordProvider = new CustomerRecordProvider(webSession);

        UserProvider.UserAdministrationRow user = userProvider.getUserAdministrationRow(userId);
        Integer customerId = resolveUserCustomerId(userProvider, userId);
        List<UserProvider.DepartmentOption> departments = userProvider.getDepartmentOptions();
        List<CustomerRecord> allCustomers = customerRecordProvider.getAllLatestCustomers();
        List<UserProjectAccessRow> projectAccessRows = userProvider.getUserProjectAccessRows(customerId, userId);

        StringBuilder xml = new StringBuilder();
        appendXmlHeader(xml);
        xml.append("<userAdministration>");
        appendElement(xml, "customerId", customerId);
        appendTopPanel(xml, webSession);

        appendLookups(xml);

        xml.append("<userDetail>");
        appendUserDetail(xml, user);

        xml.append("<customers>");
        if (allCustomers != null) {
            for (CustomerRecord customer : allCustomers) {
                appendCustomerOption(xml, customer);
            }
        }
        xml.append("</customers>");

        xml.append("<departments>");
        for (UserProvider.DepartmentOption department : departments) {
            appendDepartmentOption(xml, department);
        }
        xml.append("</departments>");

        xml.append("<userProjects>");
        for (UserProjectAccessRow projectAccessRow : projectAccessRows) {
            appendProjectAccessRow(xml, projectAccessRow);
        }
        xml.append("</userProjects>");

        xml.append("</userDetail>");
        xml.append("</userAdministration>");

        return xml.toString();
    }

    private Integer resolveUserCustomerId(
            UserProvider userProvider,
            Integer userId
    ) {
        List<CustomerRecord> customers = userProvider.getCustomersByUserId(userId);

        if (customers == null || customers.isEmpty()) {
            throw new IllegalStateException("User has no customer relation.");
        }

        if (customers.size() > 1) {
            throw new IllegalStateException("User has more than one customer relation.");
        }

        CustomerRecord customer = customers.get(0);
        Integer customerId = customer == null ? null : customer.getCustomerId();

        if (customerId == null) {
            throw new IllegalStateException("User customer relation is missing customerId.");
        }

        return customerId;
    }

    private String saveUserAdministration(
            WebSession webSession,
            HttpServletRequest request
    ) {
        try {
            Document document = parseDocument(request.getInputStream());
            Element root = document.getDocumentElement();

            UserProvider.UserAdministrationRow user = parseUser(root);
            Integer customerId = intValue(text(root, "customerId"));
            List<UserProjectAccessRow> projectAccessRows = parseProjectAccessRows(root);

            UserProvider userProvider = new UserProvider(webSession);
            boolean saved = userProvider.saveUserAdministration(user, customerId, projectAccessRows);

            if (saved) {
                clearCustomerCacheEntry(webSession, customerId);
            }

            return buildSaveResultXml(
                    saved,
                    user == null ? null : user.userId(),
                    saved ? "User saved." : "User could not be saved."
            );
        } catch (Exception e) {
            log.error("Error saving user: {}", e.getMessage());
            return buildSaveResultXml(false, null, "User could not be saved: " + e.getMessage());
        }
    }

    private String handleAction(
            WebSession webSession,
            HttpServletRequest request,
            String action
    ) {
        Integer userId = intValue(request.getParameter("userId"));
        UserProvider userProvider = new UserProvider(webSession);

        if (userId == null) {
            return buildActionResultXml(false, null, "userId is required.");
        }

        boolean success;
        String message;

        switch (action.toLowerCase()) {
            case "sendpasswordresetlink" -> {
                String baseUrl = request.getScheme() + "://" + request.getServerName()
                        + (request.getServerPort() > 0 ? ":" + request.getServerPort() : "")
                        + request.getContextPath();
                success = userProvider.sendPasswordResetLink(userId, webSession == null ? null : webSession.getUserId(), baseUrl);
                message = success ? "Password reset link queued." : "Password reset link could not be queued.";
            }
            case "resetmfa" -> {
                success = userProvider.resetMfa(userId, webSession == null ? null : webSession.getUserId());
                message = success ? "MFA was reset." : "MFA could not be reset.";
            }
            case "disablemfa" -> {
                success = userProvider.disableMfaForUser(userId);
                message = success ? "MFA was disabled." : "MFA could not be disabled.";
            }
            case "markmfaresetrequired" -> {
                success = userProvider.markMfaResetRequired(userId, webSession == null ? null : webSession.getUserId());
                message = success ? "MFA reset flag was set." : "MFA reset flag could not be set.";
            }
            case "clearmfaresetrequired" -> {
                success = userProvider.clearMfaResetRequired(userId);
                message = success ? "MFA reset flag was cleared." : "MFA reset flag could not be cleared.";
            }
            default -> {
                return buildActionResultXml(false, userId, "Unknown action: " + action);
            }
        }

        return buildActionResultXml(success, userId, message);
    }

    private void clearCustomerCacheEntry(
            WebSession webSession,
            Integer customerId
    ) {
        Integer cacheCustomerId = customerId != null ? customerId : webSession == null ? null : webSession.getCustomerId();

        if (cacheCustomerId != null) {
            EhcacheProvider.clearCacheEntry(cacheCustomerId);
        }
    }

    private UserProvider.UserAdministrationRow parseUser(Element root) {
        Element userElement = firstElement(root, "user");

        if (userElement == null) {
            return null;
        }

        return new UserProvider.UserAdministrationRow(
                intValue(text(userElement, "userId")),
                text(userElement, "initials"),
                text(userElement, "name"),
                text(userElement, "email"),
                text(userElement, "phone"),
                intValue(text(userElement, "departmentId")),
                booleanValue(text(userElement, "active"), true),
                UserRoles.fromIdOrDefault(intValue(text(userElement, "userRole")), UserRoles.BEPA_SYSTEM_ADMINISTRATOR),
                intValue(text(userElement, "themeId")),
                timestampValue(text(userElement, "lockedUntil")),
                false,
                false,
                "",
                text(userElement, "userMfaPolicy"),
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

    private List<UserProjectAccessRow> parseProjectAccessRows(Element root) {
        List<UserProjectAccessRow> projectAccessRows = new ArrayList<>();
        Element userProjects = firstElement(root, "userProjects");

        if (userProjects == null) {
            return projectAccessRows;
        }

        NodeList projectNodes = userProjects.getElementsByTagName("project");

        for (int index = 0; index < projectNodes.getLength(); index++) {
            org.w3c.dom.Node node = projectNodes.item(index);

            if (!(node instanceof Element projectElement)) {
                continue;
            }

            projectAccessRows.add(new UserProjectAccessRow(
                    intValue(text(projectElement, "ProjectId")),
                    text(projectElement, "ProjectName"),
                    booleanValue(text(projectElement, "Selected"), false)
            ));
        }

        return projectAccessRows;
    }

    private void appendLookups(StringBuilder xml) {
        xml.append("<lookups>");
        appendCountryCodeLookup(xml);
        appendStaticLookup(
                xml,
                "userMfaPolicy",
                new LookupOption("DEFAULT", "Default"),
                new LookupOption("REQUIRED", "Required"),
                new LookupOption("DISABLED", "Disabled")
        );
        appendStaticLookup(
                xml,
                "userRole",
                new LookupOption(String.valueOf(UserRoles.BEPA_SYSTEM_ADMINISTRATOR.getId()), UserRoles.BEPA_SYSTEM_ADMINISTRATOR.getLabel()),
                new LookupOption(String.valueOf(UserRoles.CUSTOMER_ADMINISTRATOR.getId()), UserRoles.CUSTOMER_ADMINISTRATOR.getLabel()),
                new LookupOption(String.valueOf(UserRoles.PROJECT_MEMBER.getId()), UserRoles.PROJECT_MEMBER.getLabel()),
                new LookupOption(String.valueOf(UserRoles.PROJECT_VIEWER.getId()), UserRoles.PROJECT_VIEWER.getLabel())
        );
        appendStaticLookup(
                xml,
                "theme",
                new LookupOption("", "—"),
                new LookupOption(String.valueOf(Theme.LIGHT.getCssId()), "Light"),
                new LookupOption(String.valueOf(Theme.BLUE.getCssId()), "Blue"),
                new LookupOption(String.valueOf(Theme.BLACK.getCssId()), "Black")
        );
        xml.append("</lookups>");
    }

    private void appendCountryCodeLookup(StringBuilder xml) {
        xml.append("<lookup name=\"countryCode\">");

        for (CustomerLookupCache.PhoneCountryRule rule : CustomerLookupCache.getPhoneCountryRules()) {
            xml.append("<option");
            xml.append(" code=\"").append(escapeXml(rule.code())).append("\"");
            xml.append(" country=\"").append(escapeXml(rule.country())).append("\"");
            xml.append(" label=\"").append(escapeXml(rule.country())).append("\"");
            xml.append(" min=\"").append(rule.minDigits()).append("\"");
            xml.append(" max=\"").append(rule.maxDigits()).append("\"");
            xml.append(" example=\"").append(escapeXml(rule.example())).append("\"");
            xml.append(" />");
        }

        xml.append("</lookup>");
    }

    private void appendTopPanel(StringBuilder xml, WebSession webSession) {
        xml.append("<TopPanel>");

        if (webSession != null) {
            try {
                TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
            TopPanel topPanel = topPanelProvider.getTopPanelBySession(PageType.ADMIN_USER_ADMINISTRATION_PAGE);

                if (topPanel != null && topPanel.getTopPanelElements() != null) {
                    topPanel.getTopPanelElements().getElements().forEach(field -> {
                        if (field == null || field.getFieldName() == null || field.getFieldName().isBlank()) {
                            return;
                        }

                        appendElement(xml, field.getFieldName(), field.toString());
                    });
                }
            } catch (Exception ignored) {
                // Top panel is best-effort for this admin page.
            }
        }

        xml.append("</TopPanel>");
    }

    private void appendUserRow(
            StringBuilder xml,
            UserProvider.UserAdministrationRow user
    ) {
        xml.append("<user>");
        appendElement(xml, "userId", user == null ? null : user.userId());
        appendElement(xml, "initials", user == null ? null : user.initials());
        appendElement(xml, "name", user == null ? null : user.name());
        appendElement(xml, "email", user == null ? null : user.email());
        appendElement(xml, "phone", user == null ? null : user.phone());
        appendElement(xml, "departmentId", user == null ? null : user.departmentId());
        appendElement(xml, "departmentName", user == null ? null : user.departmentName());
        appendElement(xml, "departmentDescription", user == null ? null : user.departmentDescription());
        appendElement(xml, "customerNames", user == null ? null : user.customerNames());
        appendElement(xml, "active", user != null && user.active());
        appendElement(xml, "userRole", user == null || user.userRole() == null ? null : user.userRole().getId());
        appendElement(xml, "userRoleLabel", user == null || user.userRole() == null ? null : user.userRole().getLabel());
        appendElement(xml, "themeId", user == null ? null : user.themeId());
        appendElement(xml, "lockedUntil", timestampText(user == null ? null : user.lockedUntil()));
        appendElement(xml, "mfaEnabled", user != null && user.mfaEnabled());
        appendElement(xml, "mfaVerified", user != null && user.mfaVerified());
        appendElement(xml, "mfaSecret", user != null && user.hasMfaSecret());
        appendElement(xml, "userMfaPolicy", user == null ? null : user.userMfaPolicy());
        appendElement(xml, "mfaResetRequired", user != null && user.mfaResetRequired());
        appendElement(xml, "mfaResetAt", timestampText(user == null ? null : user.mfaResetAt()));
        appendElement(xml, "mfaResetByUserId", user == null ? null : user.mfaResetByUserId());
        appendElement(xml, "passwordSet", user != null && user.hasPassword());
        appendElement(xml, "lastLoginAt", timestampText(user == null ? null : user.lastLoginAt()));
        xml.append("</user>");
    }

    private void appendUserDetail(
            StringBuilder xml,
            UserProvider.UserAdministrationRow user
    ) {
        appendUserRow(xml, user);
    }

    private void appendProjectAccessRow(
            StringBuilder xml,
            UserProjectAccessRow projectAccessRow
    ) {
        xml.append("<project>");
        appendElement(xml, "ProjectId", projectAccessRow == null ? null : projectAccessRow.projectId());
        appendElement(xml, "ProjectName", projectAccessRow == null ? null : projectAccessRow.projectName());
        appendElement(xml, "Selected", projectAccessRow != null && projectAccessRow.selected());
        xml.append("</project>");
    }

    private void appendCustomerOption(
            StringBuilder xml,
            CustomerRecord customer
    ) {
        xml.append("<customer>");
        appendElement(xml, "customerId", customer == null ? null : customer.getCustomerId());
        appendElement(xml, "customerName", customer == null ? null : customer.getCustomerName());
        appendElement(xml, "country", customer == null ? null : customer.getCountry());
        appendElement(xml, "customerStatus", customer == null ? null : customer.getCustomerStatusCode());
        appendElement(xml, "contactEmail", customer == null ? null : customer.getContactEmail());
        xml.append("</customer>");
    }

    private void appendDepartmentOption(
            StringBuilder xml,
            UserProvider.DepartmentOption department
    ) {
        xml.append("<department>");
        appendElement(xml, "departmentId", department == null ? null : department.departmentId());
        appendElement(xml, "customerId", department == null ? null : department.customerId());
        appendElement(xml, "customerName", department == null ? null : department.customerName());
        appendElement(xml, "departmentName", department == null ? null : department.departmentName());
        appendElement(xml, "departmentDescription", department == null ? null : department.departmentDescription());
        appendElement(xml, "active", department != null && department.active());
        appendElement(xml, "displayName", department == null ? null : department.getDisplayName());
        xml.append("</department>");
    }

    private String buildSaveResultXml(
            boolean success,
            Integer userId,
            String message
    ) {
        StringBuilder xml = new StringBuilder();
        appendXmlHeader(xml);
        xml.append("<userAdministrationSaveResult>");
        appendElement(xml, "success", success);
        appendElement(xml, "userId", userId);
        appendElement(xml, "message", message);
        xml.append("</userAdministrationSaveResult>");
        return xml.toString();
    }

    private String buildActionResultXml(
            boolean success,
            Integer userId,
            String message
    ) {
        StringBuilder xml = new StringBuilder();
        appendXmlHeader(xml);
        xml.append("<userAdministrationActionResult>");
        appendElement(xml, "success", success);
        appendElement(xml, "userId", userId);
        appendElement(xml, "message", message);
        xml.append("</userAdministrationActionResult>");
        return xml.toString();
    }

    private Document parseDocument(InputStream inputStream) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

        return factory.newDocumentBuilder().parse(inputStream);
    }

    private boolean isSystemAdministrator(WebSession webSession) {
        if (webSession == null || webSession.getUserId() == null) {
            return false;
        }

        UserProvider userProvider = new UserProvider(webSession);
        UserProvider.UserAdministrationRow currentUser = userProvider.getUserAdministrationRow(webSession.getUserId());

        return currentUser != null && currentUser.userRole() == UserRoles.BEPA_SYSTEM_ADMINISTRATOR;
    }

    private Element firstElement(
            Element parent,
            String tagName
    ) {
        if (parent == null || tagName == null || tagName.isBlank()) {
            return null;
        }

        NodeList nodes = parent.getElementsByTagName(tagName);

        if (nodes.getLength() == 0 || !(nodes.item(0) instanceof Element element)) {
            return null;
        }

        return element;
    }

    private String text(
            Element parent,
            String tagName
    ) {
        if (parent == null || tagName == null || tagName.isBlank()) {
            return "";
        }

        NodeList nodes = parent.getElementsByTagName(tagName);

        if (nodes.getLength() == 0 || nodes.item(0) == null) {
            return "";
        }

        return nodes.item(0).getTextContent() == null ? "" : nodes.item(0).getTextContent().trim();
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

    private boolean booleanValue(
            String value,
            boolean defaultValue
    ) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return Boolean.parseBoolean(value.trim());
    }

    private Timestamp timestampValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Timestamp.valueOf(LocalDateTime.parse(value.trim()));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String timestampText(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }

        return timestamp.toLocalDateTime().toString();
    }

    private void appendXmlHeader(StringBuilder xml) {
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    }

    private Integer resolveCustomerId(WebSession webSession, HttpServletRequest request) {
        Integer customerId = intValue(request.getParameter("customerId"));

        if (customerId != null) {
            return customerId;
        }

        return webSession == null ? null : webSession.getCustomerId();
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

    private void appendStaticLookup(
            StringBuilder xml,
            String name,
            LookupOption... options
    ) {
        xml.append("<lookup name=\"").append(escapeXml(name)).append("\">");

        if (options != null) {
            for (LookupOption option : options) {
                xml.append("<option");
                xml.append(" code=\"").append(escapeXml(option.code())).append("\"");
                xml.append(" label=\"").append(escapeXml(option.label())).append("\"");
                xml.append(" />");
            }
        }

        xml.append("</lookup>");
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

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String errorXml(String message) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><error>" + escapeXml(message) + "</error>";
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

    private record LookupOption(
            String code,
            String label
    ) {
    }
}
