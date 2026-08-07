package com.bepa.eis.server.dataprovider.users;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.project.ProjectRecord;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.enums.EntityRequestType;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.api.web.application.views.common.*;
import com.bepa.eis.server.dataprovider.fields.bigdecimals.BudgetInValue;
import com.bepa.eis.server.dataprovider.fields.integers.BudgetInDays;
import com.bepa.eis.server.dataprovider.fields.integers.Version;
import com.bepa.eis.server.dataprovider.fields.integers.ids.CustomerId;
import com.bepa.eis.server.dataprovider.fields.integers.ids.ProjectId;
import com.bepa.eis.server.dataprovider.fields.lookups.customer.CustomerDepartment;
import com.bepa.eis.server.dataprovider.fields.lookups.common.ChangedBy;
import com.bepa.eis.server.dataprovider.fields.lookups.project.ProjectCategory;
import com.bepa.eis.server.dataprovider.fields.lookups.project.ProjectOwner;
import com.bepa.eis.server.dataprovider.fields.lookups.project.ProjectPriority;
import com.bepa.eis.server.dataprovider.fields.lookups.project.ProjectStatus;
import com.bepa.eis.server.dataprovider.fields.strings.ProjectName;
import com.bepa.eis.server.dataprovider.fields.timestamp.ChangedDateTime;
import com.bepa.eis.server.dataprovider.fields.timestamp.EndDate;
import com.bepa.eis.server.dataprovider.fields.timestamp.StartDate;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;
import com.bepa.eis.server.dataprovider.project.ProjectProvider;

import java.sql.SQLException;

import static com.bepa.eis.server.api.web.application.enums.EntityRequestType.CREATE_ENTITY;

public class UserInfo extends GenericXmlDocument {

    private final ListOfElements rootElement;
    private final EntityRequestType requestType;
    private final Integer projectId;
    private final Integer version;

    public UserInfo(
            WebSession webSession,
            EntityRequestType requestType
    ) throws Exception {
        this(webSession, requestType, null, null);
    }

    public UserInfo(
            WebSession webSession,
            EntityRequestType requestType,
            Integer projectId
    ) throws Exception {
        this(webSession, requestType, projectId, null);
    }

    public UserInfo(
            WebSession webSession,
            EntityRequestType requestType,
            Integer projectId,
            Integer version
    ) throws Exception {
        super(webSession);

        this.requestType = requestType;
        this.projectId = projectId;
        this.version = version;

        rootElement = initXmlDocument(this.getClass().getSimpleName());

        appendTopPanel(webSession);
        appendProjectDocument(resolveProjectRecord(webSession));
    }

    private void appendTopPanel(WebSession webSession) throws Exception {
        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        TopPanel topPanel = topPanelProvider.getTopPanelBySession(PageType.PROJECT_EDIT_PAGE);

        rootElement.addElement(topPanel.getTopPanelElements());
    }

    private ProjectRecord resolveProjectRecord(WebSession webSession) throws SQLException {
        if (requestType == CREATE_ENTITY) {
            ProjectRecord projectRecord = new ProjectRecord();

            projectRecord.setCustomerId(webSession.getCustomerId());
            projectRecord.setChangedByUserId(webSession.getUserId());
            projectRecord.setProjectStatus(com.bepa.eis.common.enums.project.ProjectStatus.CREATED);

            return projectRecord;
        }

        ProjectProvider projectProvider = new ProjectProvider(webSession);

        if (version != null) {
            return projectProvider.getProjectByProjectIdAndVersion(projectId, version);
        }

        return projectProvider.getLatestProjectByProjectId(projectId);
    }

    private void appendProjectDocument(ProjectRecord projectRecord) throws SQLException {
        WebSession webSession = getWebSession();
        webSession.setProjectId(projectRecord.getProjectId());

        ListOfElements projectDocument = new ListOfElements(
                webSession,
                "projectDocument"
        );

        ListOfElements project = new ListOfElements(
                webSession,
                "project"
        );

        addProjectFields(webSession, project, projectRecord);

        projectDocument.addElement(project);
        rootElement.addElement(projectDocument);

        appendNoteAndAttachmentSections(webSession, EntityType.PROJECT);
    }

    private void addProjectFields(
            WebSession webSession,
            ListOfElements project,
            ProjectRecord projectRecord
    ) {
        ProjectRecord safeProjectRecord = projectRecord == null ? new ProjectRecord() : projectRecord;

        ProjectId projectIdField = new ProjectId(safeProjectRecord.getProjectId());
        projectIdField.setFieldNotVisible();
        project.addElement(projectIdField);

        Version versionField = new Version(safeProjectRecord.getVersion());
        versionField.setFieldNotVisible();
        project.addElement(versionField);

        CustomerId customerIdField = new CustomerId(safeProjectRecord.getCustomerId());
        customerIdField.setFieldNotVisible();
        project.addElement(customerIdField);

        ProjectName projectName = new ProjectName(safeProjectRecord.getProjectName());
        projectName.setFieldEditable();
        project.addElement(projectName);

        ProjectOwner projectOwner = new ProjectOwner(webSession);
        projectOwner.setFieldNotRequired();
        projectOwner.setValue(safeProjectRecord.getOwnerId());
        projectOwner.setFieldEditable();
        project.addElement(projectOwner);

        ProjectCategory projectCategory = new ProjectCategory(webSession);
        projectCategory.setValue(safeProjectRecord.getCategoryId());
        projectCategory.setFieldEditable();
        project.addElement(projectCategory);

        ProjectPriority projectPriority = new ProjectPriority(webSession);
        projectPriority.setValue(safeProjectRecord.getPriorityId());
        projectPriority.setFieldEditable();
        project.addElement(projectPriority);

        ProjectStatus projectStatus = new ProjectStatus(webSession);
        projectStatus.setValue(safeProjectRecord.getProjectStatusId());

        if (projectRecord.isNew() ) {
            projectStatus.setFieldNotVisible();
        } else {
            projectStatus.setFieldEditable();
        }

        project.addElement(projectStatus);

        StartDate startDate = new StartDate(safeProjectRecord.getStartDateAsTimestamp());
        startDate.setFieldEditable();
        startDate.setFieldRequired();
        project.addElement(startDate);

        EndDate endDate = new EndDate(safeProjectRecord.getEndDateAsTimestamp());
        endDate.setFieldEditable();
        endDate.setFieldRequired();
        project.addElement(endDate);

        BudgetInDays budgetInDays = new BudgetInDays(safeProjectRecord.getBudgetInDays());
        budgetInDays.setFieldEditable();
        budgetInDays.setFieldRequired();
        project.addElement(budgetInDays);

        BudgetInValue budgetInValue = new BudgetInValue(safeProjectRecord.getBudgetInValue());
        budgetInValue.setFieldEditable();
        budgetInValue.setFieldRequired();
        project.addElement(budgetInValue);

        CustomerDepartment customerDepartment = new CustomerDepartment(webSession);
        customerDepartment.setValue(safeProjectRecord.getDepartmentId());
        customerDepartment.setFieldEditable();
        customerDepartment.setFieldNotRequired();
        project.addElement(customerDepartment);

        if (projectRecord.getProjectId() != null) {
            ChangedBy changedBy = new ChangedBy(webSession);
            changedBy.setValue(safeProjectRecord.getChangedByUserId());
            changedBy.setFieldNotEditable();
            project.addElement(changedBy);

            ChangedDateTime changedDateTime = new ChangedDateTime(safeProjectRecord.getChangedDateTime());
            changedDateTime.setFieldVisible();
            changedDateTime.setFieldNotEditable();
            project.addElement(changedDateTime);
        }
    }

    private void appendNoteAndAttachmentSections(WebSession webSession, EntityType entityType) throws SQLException {

        EntityNoteProvider entityNoteProvider = new EntityNoteProvider(webSession);
        EntityNotes entityNotes = entityNoteProvider.getEntityNotesByEntityId(entityType, projectId, version);
        rootElement.addElement(entityNotes.getEntityNoteElements());

        EntityAttachmentProvider entityAttachmentProvider = new EntityAttachmentProvider(webSession);
        EntityAttachments entityAttachments = entityAttachmentProvider.getEntityAttachmentsByEntityId(entityType, projectId);
        rootElement.addElement(entityAttachments.getEntityAttachmentElements());
    }
}
