package com.bepa.eis.server.entites.logical;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityDataElement;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.dataprovider.entities.Entity;
import com.bepa.eis.server.dataprovider.entities.common.EntityElementRecord;
import com.bepa.eis.server.dataprovider.entities.common.EntityRecord;
import com.bepa.eis.server.dataprovider.fields.integers.CodeLevel;
import com.bepa.eis.server.dataprovider.fields.lookups.codeselector.LogicalStructureParentCodeSelector;
import com.bepa.eis.server.dataprovider.fields.strings.*;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.server.entites.datatypes.IntegerDataElement;
import com.bepa.eis.server.entites.datatypes.StringDataElement;

import static com.bepa.eis.common.enums.entity.EntityType.LOGICAL_STRUCTURE;

public class LogicalStructureEntity extends AbstractEntity {

    private LogicalCode logicalCode;
    private CodeLevel logicalCodeLevel;
    private LogicalName logicalName;
    private LogicalDescription logicalDescription;

    @Override
    public EntityType getEntityType() {
        return LOGICAL_STRUCTURE;
    }

    @Override
    public String getCode() {
        return logicalCode.getValue();
    }

    @Override
    public String getName() {
        return logicalName.getValue();
    }

    @Override
    public String getDescription() {
        return logicalDescription.getValue();
    }

    @Override
    public String getSortKey() {
        return getSortKeyValue(getCode());
    }

    @Override
    public void initializeFields() {
        logicalCode = new LogicalCode();
        logicalCodeLevel = new CodeLevel();
        logicalName = new LogicalName();
        logicalDescription = new LogicalDescription();
    }

    @Override
    public void addAllFieldElementsForList(Entity entityElement) {
        entityElement.addElement(logicalCode);
        entityElement.addElement(logicalCodeLevel);
        entityElement.addElement(logicalName);
        entityElement.addElement(logicalDescription);
    }

    @Override
    public void addAllFieldElementsForEdit(Entity entityElement) {
        logicalCode.setFieldNotEditable();
        entityElement.addElement(logicalCode);

        logicalCodeLevel.setFieldNotVisible();
        entityElement.addElement(logicalCodeLevel);

        logicalName.setFieldEditable();
        entityElement.addElement(logicalName);

        logicalDescription.setFieldEditable();
        logicalDescription.setFieldRequired();
        entityElement.addElement(logicalDescription);
    }

    @Override
    public void addAllFieldElementsForCreate(Entity entityElement, Integer parentEntityId) {
        LogicalStructureParentCodeSelector parentCodeSelector = new LogicalStructureParentCodeSelector(getWebSession());

        String nextCode = parentCodeSelector.getNextAvailableCodeValue(getWebSession(), parentEntityId);
        logicalCode = new LogicalCode(true);
        logicalCode.setValue(nextCode);
        logicalCode.setFieldNotEditable();
        logicalCode.setFieldRequired();
        entityElement.addElement(logicalCode);

        logicalName.setFieldEditable();
        entityElement.addElement(logicalName);

        logicalDescription.setFieldEditable();
        logicalDescription.setFieldRequired();
        entityElement.addElement(logicalDescription);
    }

    public LogicalStructureEntity() {}

    public LogicalStructureEntity(WebSession webSession) {
        super(webSession);
    }

    public LogicalStructureEntity(WebSession webSession, EntityRecord entityRecord) {
        super(webSession, entityRecord);

        for (EntityElementRecord elementRecord : entityRecord.getEntityElementRecords()) {

            EntityDataElement entityDataElement = EntityDataElement.valueOf(elementRecord.getEntityDataElementType());

            if (entityDataElement != null) {
                switch (entityDataElement) {
                    case LOGICALCODE:
                        logicalCode.setValue(elementRecord.getStringValue());
                        break;
                    case CODELEVEL:
                        logicalCodeLevel.setValue(elementRecord.getIntegerValue());
                        break;
                    case LOGICALNAME:
                        logicalName.setValue(elementRecord.getStringValue());
                        break;
                    case LOGICALDESCRIPTION:
                        logicalDescription.setValue(elementRecord.getStringValue());
                        break;
                }
            }
        }
    }

    public void setLogicalCode(String logicalCode) {
        this.logicalCode.setValue(logicalCode);
    }

    public LogicalCode getLogicalCode() {
        return logicalCode;
    }

    public String getLogicalCodeString() {
        return getCode();
    }

    public void setLogicalCodeLevel(Integer logicalCodeCodeLevel) {
        this.logicalCodeLevel.setValue(logicalCodeCodeLevel);
    }

    public CodeLevel getLogicalCodeLevel() {
        return logicalCodeLevel;
    }

    public void setLogicalName(String logicalName) {
        this.logicalName.setValue(logicalName);
    }

    public LogicalName getLogicalName() {
        return logicalName;
    }

    public void setLogicalDescription(String logicalDescription) {
        this.logicalDescription.setValue(logicalDescription);
    }

    public LogicalDescription getLogicalDescription() {
        return logicalDescription;
    }

    public void addAllDataElements() {
        addDataElement(new StringDataElement(LogicalCode.FIELD_NAME, getLogicalCode().getValue()));
        addDataElement(new IntegerDataElement(CodeLevel.FIELD_NAME, getLogicalCodeLevel().getValue()));
        addDataElement(new StringDataElement(LogicalName.FIELD_NAME, getLogicalName().getValue()));
        addDataElement(new StringDataElement(LogicalDescription.FIELD_NAME, getLogicalDescription().getValue()));
    }

}
