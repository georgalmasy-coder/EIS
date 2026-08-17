package com.bepa.eis.server.dataprovider.project;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.project.ProjectRecord;
import com.bepa.eis.common.providers.SessionProvider;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.DTO.TrlRecord;
import com.bepa.eis.server.api.generic.GenericDataProviderServlet;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.views.basis.baseline.BaselineXmlDocument;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.api.web.application.views.projectstatus.overview.NotificationProvider;
import com.bepa.eis.server.dataprovider.entities.EntityProvider;
import com.bepa.eis.server.dataprovider.entities.StakeholderRequirementProvider;
import com.bepa.eis.server.dataprovider.entities.SystemBreakdownProvider;
import com.bepa.eis.server.dataprovider.entities.SystemRequirementProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "MyProjectsServlet", urlPatterns = { "/project/myprojects" })
public class MyProjectsServlet extends GenericDataProviderServlet {

    private static final DateTimeFormatter XML_DATE_FORMAT = DateTimeFormatter.ofPattern("ddMMyyyy");
    private static final Logger log = LoggerFactory.getLogger(MyProjectsServlet.class);

    private static final String CMD_SELECT_PROJECT = "select";
    private static final String PROJECT_OVERVIEW_URL = "/web/view?page=projectoverview";

    @Override
    public void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException {
        String cmd = value(request.getParameter("cmd"));

        if (CMD_SELECT_PROJECT.equalsIgnoreCase(cmd)) {
            handleSelectProject(request, response);
            return;
        }

        super.doGet(
                request,
                response
        );
    }

    private void handleSelectProject(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Integer projectId = stringToInteger(request.getParameter("projectId"));

        if (projectId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        WebSession webSession = null;
        String sessionId = getSessionIdFromRequest(request);

        SessionProvider sessionProvider = new SessionProvider(null);
        try {
            webSession = sessionProvider.getBySessionId(sessionId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (webSession == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if (sessionId == null || sessionId.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            webSession.setSessionId(sessionId);
            webSession.setProjectId(projectId);

            boolean updated = sessionProvider.updateSessionInfo(webSession);

            if (!updated) {
                log.warn(
                        "Project selection did not update any session rows. sessionId={}, customerId={}, projectId={}",
                        sessionId,
                        webSession.getCustomerId(),
                        projectId
                );
            }

            response.setHeader(
                    "Cache-Control",
                    "no-store"
            );

            response.setHeader(
                    "Pragma",
                    "no-cache"
            );

            response.sendRedirect(
                    request.getContextPath() + PROJECT_OVERVIEW_URL
            );
        } catch (SQLException exception) {
            log.error(
                    "Error updating selected project in session. sessionId={}, projectId={}",
                    sessionId,
                    projectId,
                    exception
            );

            throw new RuntimeException(exception);
        } catch (IOException exception) {
            log.error(
                    "Error redirecting after selected project update. sessionId={}, projectId={}",
                    sessionId,
                    projectId,
                    exception
            );

            throw new RuntimeException(exception);
        }
    }

    @Override
    public void handleImport(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        throw new UnsupportedOperationException("My Projects import is not supported.");
    }

    @Override
    public void handleSave(
            WebSession webSession,
            HttpServletRequest request,
            Element rootElement
    ) {
        throw new UnsupportedOperationException("My Projects save is not supported.");
    }

    @Override
    public GenericXmlDocument handleListOfEntities(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Throwable {
        return createMyProjectsDocument(webSession);
    }

    @Override
    public GenericXmlDocument handleEditEntity(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response,
            Integer entityId,
            Integer version
    ) {
        throw new UnsupportedOperationException("My Projects edit is not supported.");
    }

    @Override
    public GenericXmlDocument handleCreateEntity(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response,
            Integer parentEntityId
    ) {
        throw new UnsupportedOperationException("My Projects create is not supported.");
    }

    @Override
    public void handleExport(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        throw new UnsupportedOperationException("My Projects export is not supported.");
    }

    @Override
    public GenericXmlDocument handleOverview(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Throwable {
        return createMyProjectsDocument(webSession);
    }

    private GenericXmlDocument createMyProjectsDocument(WebSession webSession) throws Exception {
        BaselineXmlDocument xmlDocument = new BaselineXmlDocument(
                webSession,
                "MyProjects"
        );

        appendTopPanel(
                webSession,
                xmlDocument,
                xmlDocument.root()
        );

        Element projectsElement = xmlDocument.appendElement(
                xmlDocument.root(),
                "projects"
        );

        ProjectProvider projectProvider = new ProjectProvider(webSession);
        List<ProjectRecord> projects = projectProvider.getLatestProjectsByCustomerAndUserId(
                webSession.getCustomerId(),
                webSession.getUserId()
        );

        for (ProjectRecord project : projects) {
            appendProject(
                    xmlDocument,
                    projectsElement,
                    project
            );
        }

        return xmlDocument;
    }

    private void appendTopPanel(
            WebSession webSession,
            BaselineXmlDocument xmlDocument,
            Element rootElement
    ) throws Exception {
        Element topPanelElement = xmlDocument.appendElement(
                rootElement,
                "TopPanel"
        );

        try {
            TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
            TopPanel topPanel = topPanelProvider.getTopPanelBySession(PageType.MY_PROJECTS_PAGE);

            if (topPanel != null && topPanel.getTopPanelElements() != null) {
                topPanel.getTopPanelElements().getElements().forEach(field -> {
                    if (field == null || field.getFieldName() == null || field.getFieldName().isBlank()) {
                        return;
                    }

                    xmlDocument.appendTextElement(
                            topPanelElement,
                            field.getFieldName(),
                            field.toString()
                    );
                });
            }
        } catch (Exception exception) {
            xmlDocument.appendTextElement(
                    topPanelElement,
                    "CustomerName",
                    "—"
            );

            xmlDocument.appendTextElement(
                    topPanelElement,
                    "ProjectName",
                    "My Projects"
            );

            xmlDocument.appendTextElement(
                    topPanelElement,
                    "UserName",
                    "—"
            );
        }
    }

    private void appendProject(
            BaselineXmlDocument xmlDocument,
            Element projectsElement,
            ProjectRecord project
    ) {
        Element projectElement = xmlDocument.appendElement(
                projectsElement,
                "project"
        );

        List<TrlRecord> trlRecordList = getActiveTrlRecords(
                project.getCustomerId(),
                project.getProjectId()
        );

        xmlDocument.appendTextElement(projectElement, "projectpk", value(project.getProjectPK()));
        xmlDocument.appendTextElement(projectElement, "customerid", value(project.getCustomerId()));
        xmlDocument.appendTextElement(projectElement, "projectid", value(project.getProjectId()));
        xmlDocument.appendTextElement(projectElement, "version", value(project.getVersion()));
        xmlDocument.appendTextElement(projectElement, "latest", String.valueOf(project.isLatest()));

        xmlDocument.appendTextElement(projectElement, "projectname", value(project.getProjectName()));

        xmlDocument.appendTextElement(projectElement, "projectownerid", value(project.getOwnerId()));
        xmlDocument.appendTextElement(projectElement, "OwnerId", value(getProjectOwner(project.getOwnerId())));

        xmlDocument.appendTextElement(projectElement, "projectcategoryid", value(project.getCategoryId()));
        xmlDocument.appendTextElement(projectElement, "projectcategory", value(getProjectCategory(project.getCategoryId())));

        xmlDocument.appendTextElement(projectElement, "projectpriorityid", value(project.getPriorityId()));

        xmlDocument.appendTextElement(projectElement, "projectstatusid", value(project.getProjectStatusId()));
        xmlDocument.appendTextElement(projectElement, "projectstatuscode", value(project.getProjectStatusCode()));
        xmlDocument.appendTextElement(projectElement, "projectstatus", value(project.getProjectStatusLabel()));

        xmlDocument.appendTextElement(projectElement, "StartDate", formatDate(project.getStartDate()));
        xmlDocument.appendTextElement(projectElement, "EndDate", formatDate(project.getEndDate()));

        xmlDocument.appendTextElement(projectElement, "budgetindays", value(project.getBudgetInDays()));
        xmlDocument.appendTextElement(projectElement, "budgetinvalue", value(project.getBudgetInValue()));

        xmlDocument.appendTextElement(projectElement, "departmentid", value(project.getDepartmentId()));

        xmlDocument.appendTextElement(projectElement, "changedbyuserid", value(project.getChangedByUserId()));
        xmlDocument.appendTextElement(projectElement, "changeddatetime", formatDateTime(project.getChangedDateTime()));

        appendProjectDashboardPlaceholders(
                xmlDocument,
                projectElement,
                project,
                trlRecordList
        );

        appendTrls(
                xmlDocument,
                projectElement,
                project,
                trlRecordList
        );
    }

    private String getProjectOwner(Integer projectOwnerId) {
        LookupValue lookupValue = CustomerLookupCache.getUserLookupValue(
                getWebSession(),
                projectOwnerId
        );

        return lookupValue != null ? lookupValue.getLookupCode() : null;
    }

    private String getProjectCategory(Integer categoryId) {
        LookupValue lookupValue = CustomerLookupCache.getProjectCategoryLookupValue(
                getWebSession(),
                categoryId
        );

        return lookupValue != null ? lookupValue.getLookupCode() : null;
    }

    private Integer getActiveStakeholderRequirementCount(ProjectRecord project) {
        return getActiveEntityCount(
                project,
                new StakeholderRequirementProvider(getWebSession())
        );
    }

    private Integer getActiveSystemRequirementCount(ProjectRecord project) {
        return getActiveEntityCount(
                project,
                new SystemRequirementProvider(getWebSession())
        );
    }

    private Integer getActiveSystemsBreakDownCount(ProjectRecord project) {
        return getActiveEntityCount(
                project,
                new SystemBreakdownProvider(getWebSession())
        );
    }

    private List<TrlRecord> getActiveTrlRecords(
            Integer customerId,
            Integer projectId
    ) {
        SystemBreakdownProvider systemBreakdownProvider = new SystemBreakdownProvider(getWebSession());

        return systemBreakdownProvider.getListOfTrlRecords(
                customerId,
                projectId
        );
    }

    private int getActiveEntityCount(
            ProjectRecord project,
            EntityProvider entityProvider
    ) {
        return entityProvider.getActiveEntityCount(
                project.getCustomerId(),
                project.getProjectId(),
                entityProvider.getEntityType()
        );
    }

    private Integer getMyNotificationCount(ProjectRecord project) {
        NotificationProvider notificationProvider = new NotificationProvider(getWebSession());

        return notificationProvider.getMyNotificationCount(
                project.getCustomerId(),
                project.getProjectId(),
                getWebSession().getUserId()
        );
    }

    private String getNextTrlDeadLine(List<TrlRecord> trlRecordList) {
        Timestamp nextTrlDeadline = null;

        for (TrlRecord trlRecord : trlRecordList) {
            Timestamp deadline = trlRecord.getNextTrlDeadline();

            if (deadline == null || !deadline.after(now())) {
                continue;
            }

            if (nextTrlDeadline == null || deadline.before(nextTrlDeadline)) {
                nextTrlDeadline = deadline;
            }
        }

        if (trlRecordList.isEmpty()) {
            return "-";
        }

        if (nextTrlDeadline == null) {
            return "Over due";
        }

        return daysUntil(nextTrlDeadline) + " days";
    }

    private Long daysUntil(Timestamp nextTrlDeadline) {
        if (nextTrlDeadline == null) {
            return 0L;
        }

        LocalDate today = now().toLocalDateTime().toLocalDate();
        LocalDate deadlineDate = nextTrlDeadline.toLocalDateTime().toLocalDate();

        return ChronoUnit.DAYS.between(
                today,
                deadlineDate
        );
    }

    private Timestamp now() {
        return Timestamp.from(Instant.now());
    }

    private void appendProjectDashboardPlaceholders(
            BaselineXmlDocument xmlDocument,
            Element projectElement,
            ProjectRecord project,
            List<TrlRecord> trlRecordList
    ) {
        xmlDocument.appendTextElement(projectElement, "countnotifications", getMyNotificationCount(project));
        xmlDocument.appendTextElement(projectElement, "countstakeholderrequirement", getActiveStakeholderRequirementCount(project));
        xmlDocument.appendTextElement(projectElement, "countsystemrequirement", getActiveSystemRequirementCount(project));
        xmlDocument.appendTextElement(projectElement, "countsystembreakdown", getActiveSystemsBreakDownCount(project));
        xmlDocument.appendTextElement(projectElement, "nexttrldeadline", getNextTrlDeadLine(trlRecordList));
    }

    private void appendTrls(
            BaselineXmlDocument xmlDocument,
            Element projectElement,
            ProjectRecord project,
            List<TrlRecord> trlRecordList
    ) {
        Element trlsElement = xmlDocument.appendElement(
                projectElement,
                "trls"
        );

        Map<Integer, Integer> trlCountMap = new HashMap<>();

        for (TrlRecord trlRecord : trlRecordList) {
            trlCountMap.put(
                    trlRecord.getTrlId(),
                    trlCountMap.getOrDefault(
                            trlRecord.getTrlId(),
                            0
                    ) + 1
            );
        }

        for (int index = 1; index <= 9; index++) {
            Element trlElement = xmlDocument.appendElement(
                    trlsElement,
                    "trl"
            );

            LookupValue lookupValue = CustomerLookupCache.getTrlLookupValue(
                    project.getCustomerId(),
                    project.getProjectId(),
                    index
            );

            if (lookupValue == null) {
                log.info("No lookup value found for trl id: {}", index);
                continue;
            }

            trlElement.setAttribute(
                    "code",
                    lookupValue.getLookupId().toString()
            );

            trlElement.setAttribute(
                    "description",
                    lookupValue.getLookupCode()
            );

            Integer count = trlCountMap.get(index);

            trlElement.setAttribute(
                    "count",
                    count != null ? count.toString() : "0"
            );

            trlElement.setAttribute(
                    "enabled",
                    lookupValue.isActive() ? "true" : "false"
            );
        }
    }

    private String formatDate(LocalDate localDate) {
        if (localDate == null) {
            return "";
        }

        return localDate.format(XML_DATE_FORMAT);
    }

    private String formatDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }

        return timestamp.toLocalDateTime().toString();
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static Integer stringToInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
