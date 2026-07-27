package com.bepa.eis.server.dataprovider.project;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.project.ProjectRecord;
import com.bepa.eis.server.api.generic.GenericDataProviderServlet;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.dataprovider.cache.EhcacheProvider;
import com.bepa.eis.server.dataprovider.fields.bigdecimals.BudgetInValue;
import com.bepa.eis.server.dataprovider.fields.integers.BudgetInDays;
import com.bepa.eis.server.dataprovider.fields.integers.Version;
import com.bepa.eis.server.dataprovider.fields.integers.ids.CustomerId;
import com.bepa.eis.server.dataprovider.fields.integers.ids.ProjectId;
import com.bepa.eis.server.dataprovider.fields.lookups.customer.CustomerDepartment;
import com.bepa.eis.server.dataprovider.fields.lookups.project.ProjectCategory;
import com.bepa.eis.server.dataprovider.fields.lookups.project.ProjectOwner;
import com.bepa.eis.server.dataprovider.fields.lookups.project.ProjectPriority;
import com.bepa.eis.server.dataprovider.fields.lookups.project.ProjectStatus;
import com.bepa.eis.server.dataprovider.fields.strings.ProjectName;
import com.bepa.eis.server.dataprovider.fields.timestamp.EndDate;
import com.bepa.eis.server.dataprovider.fields.timestamp.StartDate;
import com.bepa.eis.server.entites.project.ProjectEntity;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;

import static com.bepa.eis.server.api.web.application.enums.EntityRequestType.CREATE_ENTITY;
import static com.bepa.eis.server.api.web.application.enums.EntityRequestType.EDIT_ENTITY;

@WebServlet(name = "ProjectServlet", urlPatterns = {
        "/project"
})
@MultipartConfig
public class ProjectServlet extends GenericDataProviderServlet {

    private static final Logger log = LoggerFactory.getLogger(ProjectServlet.class);

    @Override
    public void handleImport(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        throw new UnsupportedOperationException("Project import is not supported.");
    }

    @Override
    public void handleSave(
            WebSession webSession,
            HttpServletRequest request,
            Element rootElement
    ) {
        try {
            Element projectDocument = firstChild(rootElement, "projectDocument");

            if (projectDocument == null) {
                throw new IllegalArgumentException("Missing projectDocument in save payload.");
            }

            Element projectElement = firstChild(projectDocument, "project");

            if (projectElement == null) {
                throw new IllegalArgumentException("Missing project in save payload.");
            }

            Element noteSection = firstChild(rootElement, "EntityNotes");
            Element attachmentSection = firstChild(rootElement, "EntityAttachments");

            ProjectRecord projectRecord = parseProjectDocument(webSession, projectElement);

            if (noteSection != null) {
                log.debug("Project notes section received.");
            }

            if (attachmentSection != null) {
                log.debug("Project attachments section received.");
            }

            ProjectEntity projectEntity = new ProjectEntity(webSession);

            projectEntity.setCustomerId(projectRecord.getCustomerId());
            projectEntity.setProjectId(projectRecord.getProjectId());
            projectEntity.setVersion(projectRecord.getVersion());
            projectEntity.setEntityId(projectRecord.getProjectId());
            projectEntity.setChangedByUserId(projectRecord.getChangedByUserId());
            projectEntity.setDateOfChange(projectRecord.getChangedDateTime());
            projectEntity.setActive(projectRecord.getProjectStatus().isActiveStatus());

            parseNoteDocument(projectEntity, noteSection);
            parseAttachmentDocument(projectEntity, attachmentSection);

            ProjectProvider projectProvider = new ProjectProvider(webSession);
            projectProvider.persist(projectEntity, projectRecord);
            EhcacheProvider.clearCacheEntry(projectRecord.getCustomerId());

        } catch (Exception exception) {
            throw new RuntimeException("Failed to save project XML.", exception);
        }
    }

    @Override
    public GenericXmlDocument handleListOfEntities(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Throwable {
        return listOfProjects(webSession);
    }

    @Override
    public GenericXmlDocument handleEditEntity(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response,
            Integer entityId,
            Integer version
    ) throws Throwable {
        return editProjectById(webSession, entityId, version);
    }

    @Override
    public GenericXmlDocument handleCreateEntity(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response,
            Integer parentEntityId
    ) throws Throwable {
        return createNewProject(webSession);
    }

    @Override
    public void handleExport(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        throw new UnsupportedOperationException("Project export is not supported.");
    }

    @Override
    public GenericXmlDocument handleOverview(
            WebSession webSession,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Throwable {
        return listOfProjects(webSession);
    }

    private GenericXmlDocument listOfProjects(WebSession webSession) {
        try {
            return new ProjectList(webSession);
        } catch (Exception exception) {
            log.error("Error getting project list: {}", exception.getMessage(), exception);
            throw new RuntimeException(exception);
        }
    }

    private GenericXmlDocument createNewProject(WebSession webSession) {
        try {
            return new ProjectInfo(webSession, CREATE_ENTITY);
        } catch (Exception exception) {
            log.error("Error creating project info document: {}", exception.getMessage(), exception);
            throw new RuntimeException(exception);
        }
    }

    private GenericXmlDocument editProjectById(
            WebSession webSession,
            Integer projectId,
            Integer version
    ) {
        try {
            if (version != null) {
                return new ProjectInfo(webSession, EDIT_ENTITY, projectId, version);
            }

            return new ProjectInfo(webSession, EDIT_ENTITY, projectId);
        } catch (Exception exception) {
            log.error("Error getting project info document: {}", exception.getMessage(), exception);
            throw new RuntimeException(exception);
        }
    }

    private ProjectRecord parseProjectDocument(
            WebSession webSession,
            Element projectElement
    ) {
        ProjectRecord projectRecord = new ProjectRecord();

        Integer projectId = intValue(projectElement, ProjectId.FIELD_NAME);
        Integer version = intValue(projectElement, Version.FIELD_NAME);
        Integer customerId = intValue(projectElement, CustomerId.FIELD_NAME);

        String projectName = textValue(projectElement, ProjectName.FIELD_NAME);

        Integer ownerId = intValue(projectElement, ProjectOwner.FIELD_NAME);
        Integer categoryId = intValue(projectElement, ProjectCategory.FIELD_NAME);
        Integer priorityId = intValue(projectElement, ProjectPriority.FIELD_NAME);
        Integer projectStatusId = intValue(projectElement, ProjectStatus.FIELD_NAME);

        LocalDate startDate = localDateValue(projectElement, StartDate.FIELD_NAME);
        LocalDate endDate = localDateValue(projectElement, EndDate.FIELD_NAME);

        Integer budgetInDays = intValue(projectElement, BudgetInDays.FIELD_NAME);
        BigDecimal budgetInValue = bigDecimalValue(projectElement, BudgetInValue.FIELD_NAME);

        Integer departmentId = intValue(projectElement, CustomerDepartment.FIELD_NAME);

        projectRecord.setProjectId(projectId);
        projectRecord.setVersion(version);
        projectRecord.setCustomerId(customerId == null ? webSession.getCustomerId() : customerId);

        projectRecord.setProjectName(projectName);
        projectRecord.setOwnerId(ownerId);
        projectRecord.setCategoryId(categoryId);
        projectRecord.setPriorityId(priorityId);
        projectRecord.setProjectStatusId(projectStatusId);

        projectRecord.setStartDate(startDate);
        projectRecord.setEndDate(endDate);
        projectRecord.setBudgetInDays(budgetInDays);
        projectRecord.setBudgetInValue(budgetInValue);
        projectRecord.setDepartmentId(departmentId);

        projectRecord.setChangedByUserId(webSession.getUserId());
        projectRecord.setChangedDateTime(new Timestamp(System.currentTimeMillis()));

        return projectRecord;
    }

    private LocalDate localDateValue(
            Element parent,
            String tagName
    ) {
        String value = textValue(parent, tagName);

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid date value for " + tagName + ": " + value, exception);
        }
    }

    private BigDecimal bigDecimalValue(
            Element parent,
            String tagName
    ) {
        String value = textValue(parent, tagName);

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return new BigDecimal(value.trim().replace(",", "."));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid decimal value for " + tagName + ": " + value, exception);
        }
    }
}
