package com.bepa.eis.server.api.web.application.views.myprojects;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.project.ProjectRecord;
import com.bepa.eis.server.dataprovider.project.ProjectProvider;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;
import org.w3c.dom.Element;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ProjectList extends GenericXmlDocument {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("ddMMyyyy");

    private final ListOfElements rootElement;

    public ProjectList(WebSession webSession) throws Exception {
        super(webSession);

        rootElement = initXmlDocument(this.getClass().getSimpleName());

        appendTopPanel(webSession);
        appendProjects(webSession);
    }

    private void appendTopPanel(WebSession webSession) throws Exception {
        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        TopPanel topPanel = topPanelProvider.getTopPanelBySession();

        rootElement.addElement(topPanel.getTopPanelElements());
    }

    private void appendProjects(WebSession webSession) throws Exception {
        ProjectProvider projectProvider = new ProjectProvider(webSession);
        List<ProjectRecord> projects = projectProvider.getLatestProjectsByCustomerId(webSession.getCustomerId());

        Element projectsElement = getDoc().createElement("projects");
        getRoot().appendChild(projectsElement);

        for (ProjectRecord project : projects) {
            projectsElement.appendChild(createProjectElement(project));
        }
    }

    private Element createProjectElement(ProjectRecord project) {
        Element projectElement = getDoc().createElement("project");

        appendTextElement(projectElement, "projectpk", project.getProjectPK());
        appendTextElement(projectElement, "projectid", project.getProjectId());
        appendTextElement(projectElement, "version", project.getVersion());
        appendTextElement(projectElement, "latest", project.isLatest());

        appendTextElement(projectElement, "projectname", project.getProjectName());
        appendTextElement(projectElement, "customerid", project.getCustomerId());
        appendTextElement(projectElement, "ownerid", project.getOwnerId());
        appendTextElement(projectElement, "categoryid", project.getCategoryId());
        appendTextElement(projectElement, "priorityid", project.getPriorityId());

        appendTextElement(projectElement, "projectstatusid", project.getProjectStatusId());
        appendTextElement(projectElement, "projectstatuscode", project.getProjectStatusCode());
        appendTextElement(projectElement, "projectstatus", project.getProjectStatusLabel());

        appendTextElement(projectElement, "projectstartdate", formatDate(project.getStartDate()));
        appendTextElement(projectElement, "projectenddate", formatDate(project.getEndDate()));
        appendTextElement(projectElement, "budgetindays", project.getBudgetInDays());
        appendTextElement(projectElement, "budgetinvalue", project.getBudgetInValue());
        appendTextElement(projectElement, "departmentid", project.getDepartmentId());

        appendTextElement(projectElement, "changedbyuserid", project.getChangedByUserId());
        appendTextElement(projectElement, "changeddatetime", formatDateTime(project.getChangedDateTime()));

        return projectElement;
    }

    private void appendTextElement(
            Element parentElement,
            String elementName,
            Object value
    ) {
        Element element = getDoc().createElement(elementName);
        element.setTextContent(value == null ? "" : String.valueOf(value));
        parentElement.appendChild(element);
    }

    private String formatDate(LocalDate localDate) {
        if (localDate == null) {
            return "";
        }
        return localDate.format(DATE_FORMAT);
    }

    private String formatDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }

        return timestamp.toLocalDateTime().toString();
    }
}