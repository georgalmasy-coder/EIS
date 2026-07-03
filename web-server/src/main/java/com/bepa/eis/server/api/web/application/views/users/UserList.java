package com.bepa.eis.server.api.web.application.views.users;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.project.ProjectRecord;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.dataprovider.entities.Entities;
import com.bepa.eis.server.dataprovider.entities.Entity;
import com.bepa.eis.server.dataprovider.fields.booleans.Latest;
import com.bepa.eis.server.dataprovider.fields.integers.Version;
import com.bepa.eis.server.dataprovider.fields.integers.ids.CustomerId;
import com.bepa.eis.server.dataprovider.fields.integers.ids.EntityId;
import com.bepa.eis.server.dataprovider.fields.integers.ids.ProjectId;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class UserList extends GenericXmlDocument {

    private static final Logger log = LoggerFactory.getLogger(UserList.class);

    private final ListOfElements rootElement;

    public UserList(WebSession webSession) throws Exception {
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

        EntityType entityType = EntityType.PROJECT;
        Entities entities = new Entities(getWebSession(), entityType.getMultipleRootElementName());

        ProjectProvider projectProvider = new ProjectProvider(webSession);
        List<ProjectRecord> projects = projectProvider.getLatestProjectsByCustomerId(webSession.getCustomerId());

        for (ProjectRecord project : projects) {

            /* Create new entity where all the entity data elements are added to */
            Entity entity = entities.getNewEntity(entityType.getEntityElementName());

            /* Default entity data elements */
            entity.addElement(new ProjectId(project.getProjectId()));
            entity.addElement(new CustomerId(project.getCustomerId()));
            entity.addElement(new EntityId(project.getEntityId()));

            Version version = new Version(project.getVersion());
            version.setFieldNotVisible();
            entity.addElement(version);

            Latest latest = new Latest(project.isLatest());
            latest.setFieldNotVisible();
            entity.addElement(latest);

            ProjectName projectName = new ProjectName(project.getProjectName());
            projectName.setFieldNotEditable();
            entity.addElement(projectName);

            ProjectOwner projectOwner = new ProjectOwner(webSession);
            projectOwner.setValue(project.getOwnerId());
            projectName.setFieldNotEditable();
            entity.addElement(projectOwner);

            ProjectCategory projectCategory = new ProjectCategory(webSession);
            projectCategory.setValue(project.getCategoryId());
            projectCategory.setFieldNotEditable();
            entity.addElement(projectCategory);

            ProjectPriority projectPriority = new ProjectPriority(webSession);
            projectPriority.setValue(project.getPriorityId());
            projectPriority.setFieldNotEditable();
            entity.addElement(projectPriority);

            ProjectStatus projectStatus = new ProjectStatus(webSession);
            projectStatus.setValue(project.getProjectStatusId());
            projectStatus.setFieldNotEditable();
            entity.addElement(projectStatus);

            StartDate startDate = new StartDate(project.getStartDate());
            startDate.setFieldNotEditable();
            entity.addElement(startDate);

            EndDate endDate = new EndDate(project.getEndDate());
            endDate.setFieldNotEditable();
            entity.addElement(endDate);

            /* And finally add another 3 default entity data elements */
            ChangedDateTime changedDateTime = new ChangedDateTime(project.getChangedDateTime());
            changedDateTime.setFieldNotEditable();

            ChangedBy changedBy = new ChangedBy(getWebSession());
            changedBy.setValue(project.getChangedByUserId());
            changedBy.setFieldNotEditable();

            changedBy.setTableWidth("175x");
            changedDateTime.setTableWidth("150px");
            entity.addElement(changedBy);
            entity.addElement(changedDateTime);

            entities.addEntity(entity);
        }

        rootElement.addElement(entities.getListOfEntities());
    }

}