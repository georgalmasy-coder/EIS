package com.bepa.eis.server.entites.project;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.project.ProjectRecord;
import com.bepa.eis.common.enums.entity.EntityDataElement;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.dataprovider.entities.common.EntityElementRecord;
import com.bepa.eis.server.dataprovider.entities.common.EntityRecord;
import com.bepa.eis.server.dataprovider.fields.integers.CodeLevel;
import com.bepa.eis.server.dataprovider.fields.strings.ProjectName;
import com.bepa.eis.server.dataprovider.fields.strings.RequirementDescription;
import com.bepa.eis.server.dataprovider.fields.strings.RequirementName;
import com.bepa.eis.server.dataprovider.fields.strings.StakeholderRequirementCode;
import com.bepa.eis.server.dataprovider.project.ProjectProvider;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.server.entites.datatypes.IntegerDataElement;
import com.bepa.eis.server.entites.datatypes.StringDataElement;

import static com.bepa.eis.common.enums.entity.EntityType.PROJECT;
import static com.bepa.eis.common.enums.entity.EntityType.STAKEHOLDER_REQUIREMENT;

public class ProjectEntity extends AbstractEntity {

    private String projectName;
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
        return projectName;
    }

    @Override
    public String getDescription() {
        return "";
    }

    public ProjectEntity() {}

    public ProjectEntity(WebSession session) {
        super(session);
        setChangedByUserId(session.getUserId());
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void addAllDataElements() {
        addDataElement(new StringDataElement(ProjectName.FIELD_NAME, getProjectName()));
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
