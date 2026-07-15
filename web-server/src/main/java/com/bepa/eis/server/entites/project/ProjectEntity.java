package com.bepa.eis.server.entites.project;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.project.ProjectRecord;
import com.bepa.eis.common.enums.entity.EntityDataElement;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.dataprovider.entities.Entity;
import com.bepa.eis.server.dataprovider.entities.common.EntityElementRecord;
import com.bepa.eis.server.dataprovider.entities.common.EntityRecord;
import com.bepa.eis.server.dataprovider.fields.integers.CodeLevel;
import com.bepa.eis.server.dataprovider.fields.strings.*;
import com.bepa.eis.server.dataprovider.fields.strings.email.ContactEmail;
import com.bepa.eis.server.dataprovider.fields.strings.phone.ContactPhone;
import com.bepa.eis.server.dataprovider.project.ProjectProvider;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.server.entites.datatypes.IntegerDataElement;
import com.bepa.eis.server.entites.datatypes.StringDataElement;

import static com.bepa.eis.common.enums.entity.EntityType.PROJECT;
import static com.bepa.eis.common.enums.entity.EntityType.STAKEHOLDER_REQUIREMENT;

public class ProjectEntity extends AbstractEntity {

    private ProjectName projectName;
    private ProjectProvider projectProvider;
    private ProjectRecord projectRecord;

    @Override
    public EntityType getEntityType() {
        return PROJECT;
    }

    @Override
    public String getCode() {
        return "";
    }

    @Override
    public String getName() {
        return projectName.getValue();
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public void initializeFields() {
        projectName = new ProjectName();
    }

    @Override
    public void addAllFieldElementsForList(Entity entityElement) {
        entityElement.addElement(projectName);
    }

    @Override
    public void addAllFieldElementsForEdit(Entity entityElement) {
        entityElement.addElement(projectName);
    }

    @Override
    public void addAllFieldElementsForCreate(Entity entityElement, Integer parentEntityId) {
        entityElement.addElement(projectName);
    }

    public ProjectEntity() {}

    public ProjectEntity(WebSession session) {
        super(session);
    }

    public ProjectEntity(WebSession webSession, EntityRecord entityRecord) {
        super(webSession, entityRecord);

        for (EntityElementRecord elementRecord : entityRecord.getEntityElementRecords()) {
            EntityDataElement entityDataElement = EntityDataElement.valueOf(elementRecord.getEntityDataElementType());
            if (entityDataElement != null) {
                switch (entityDataElement) {
                    case PROJECTNAME:
                        projectName.setValue(elementRecord.getStringValue());
                        break;
                }
            }
        }
    }

    public void setProjectName(String projectName) {
        this.projectName.setValue(projectName);
    }

    public ProjectName getProjectName() {
        return projectName;
    }

    public void addAllDataElements() {
        addDataElement(new StringDataElement(ProjectName.FIELD_NAME, getProjectName().getValue()));
    }

    public void setProjectProvider(ProjectProvider projectProvider, ProjectRecord projectRecord) {
        this.projectProvider = projectProvider;
        this.projectRecord = projectRecord;
    }

    public static ProjectEntity map(EntityRecord entity) {

        ProjectEntity projectEntity = null;

        if (entity != null) {

            projectEntity = new ProjectEntity(entity.getWebSession());

            projectEntity.setEntityId(entity.getEntityId());
            projectEntity.setCustomerId(entity.getCustomerId());
            projectEntity.setProjectId(entity.getProjectId());
            projectEntity.setVersion(entity.getVersion());
            projectEntity.setChangedByUserId(entity.getChangedByUserId());
            projectEntity.setDateOfChange(entity.getChangedDateTime());
            projectEntity.setActive(entity.isActive());

            for (EntityElementRecord elementRecord : entity.getEntityElementRecords()) {

                EntityDataElement entityDataElement = EntityDataElement.valueOf(elementRecord.getEntityDataElementType());

                if (entityDataElement != null) {
                    switch (entityDataElement) {
                        case PROJECTNAME :
                            projectEntity.setProjectName(elementRecord.getStringValue());
                            break;
                    }
                }
            }

        }
        return projectEntity;
    }

    public void persistProject() throws Exception {
        if (projectProvider != null && projectRecord != null) {
            projectProvider.persistProject(projectRecord);
        }
    }


}
