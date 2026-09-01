package com.bepa.eis.server.entites.functional;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityDataElement;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.dataprovider.entities.Entity;
import com.bepa.eis.server.dataprovider.entities.common.EntityElementRecord;
import com.bepa.eis.server.dataprovider.entities.common.EntityRecord;
import com.bepa.eis.server.dataprovider.fields.integers.CodeLevel;
import com.bepa.eis.server.dataprovider.fields.lookups.codeselector.FunctionalStructureParentCodeSelector;
import com.bepa.eis.server.dataprovider.fields.strings.*;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.server.entites.datatypes.IntegerDataElement;
import com.bepa.eis.server.entites.datatypes.StringDataElement;

import static com.bepa.eis.common.enums.entity.EntityType.FUNCTIONAL_STRUCTURE;

public class FunctionalStructureEntity extends AbstractEntity {

    private FunctionalCode functionalCode;
    private CodeLevel functionalCodeLevel;
    private FunctionalName functionalName;
    private FunctionalDescription functionalDescription;

    @Override
    public EntityType getEntityType() {
        return FUNCTIONAL_STRUCTURE;
    }

    @Override
    public String getCode() {
        return functionalCode.getValue();
    }

    @Override
    public String getName() {
        return functionalName.getValue();
    }

    @Override
    public String getDescription() {
        return functionalDescription.getValue();
    }

    @Override
    public String getSortKey() {
        return getSortKeyValue(getCode());
    }

    @Override
    public void initializeFields() {
        functionalCode = new FunctionalCode();
        functionalCodeLevel = new CodeLevel();
        functionalName = new FunctionalName();
        functionalDescription = new FunctionalDescription();
    }

    @Override
    public void addAllFieldElementsForList(Entity entityElement) {
        entityElement.addElement(functionalCode);
        entityElement.addElement(functionalCodeLevel);
        entityElement.addElement(functionalName);
        entityElement.addElement(functionalDescription);
    }

    @Override
    public void addAllFieldElementsForEdit(Entity entityElement) {
        functionalCode.setFieldNotEditable();
        entityElement.addElement(functionalCode);

        functionalCodeLevel.setFieldNotVisible();
        entityElement.addElement(functionalCodeLevel);

        functionalName.setFieldEditable();
        entityElement.addElement(functionalName);

        functionalDescription.setFieldEditable();
        functionalDescription.setFieldRequired();
        entityElement.addElement(functionalDescription);
    }

    @Override
    public void addAllFieldElementsForCreate(Entity entityElement, Integer parentEntityId) {
        FunctionalStructureParentCodeSelector parentCodeSelector = new FunctionalStructureParentCodeSelector(getWebSession());

        String nextCode = parentCodeSelector.getNextAvailableCodeValue(getWebSession(), parentEntityId);
        functionalCode = new FunctionalCode(true);
        functionalCode.setValue(nextCode);
        functionalCode.setFieldNotEditable();
        functionalCode.setFieldRequired();
        entityElement.addElement(functionalCode);

        functionalName.setFieldEditable();
        entityElement.addElement(functionalName);

        functionalDescription.setFieldEditable();
        functionalDescription.setFieldRequired();
        entityElement.addElement(functionalDescription);
    }

    public FunctionalStructureEntity() {}

    public FunctionalStructureEntity(WebSession webSession) {
        super(webSession);
    }

    public FunctionalStructureEntity(WebSession webSession, EntityRecord entityRecord) {
        super(webSession, entityRecord);

        for (EntityElementRecord elementRecord : entityRecord.getEntityElementRecords()) {

            EntityDataElement entityDataElement = EntityDataElement.valueOf(elementRecord.getEntityDataElementType());

            if (entityDataElement != null) {
                switch (entityDataElement) {
                    case FUNCTIONALCODE:
                        functionalCode.setValue(elementRecord.getStringValue());
                        break;
                    case CODELEVEL:
                        functionalCodeLevel.setValue(elementRecord.getIntegerValue());
                        break;
                    case FUNCTIONALNAME:
                        functionalName.setValue(elementRecord.getStringValue());
                        break;
                    case FUNCTIONALDESCRIPTION:
                        functionalDescription.setValue(elementRecord.getStringValue());
                        break;
                }
            }
        }
    }

    public void setFunctionalCode(String functionalCode) {
        this.functionalCode.setValue(functionalCode);
    }

    public FunctionalCode getFunctionalCode() {
        return functionalCode;
    }

    public String getFunctionalCodeString() {
        return functionalCode.getValue();
    }

    public void setFunctionalCodeLevel(Integer functionalCodeLevel) {
        this.functionalCodeLevel.setValue(functionalCodeLevel);
    }

    public CodeLevel geFunctionalCodeLevel() {
        return functionalCodeLevel;
    }

    public void setFunctionalName(String functionalName) {
        this.functionalName.setValue(functionalName);
    }

    public FunctionalName getFunctionalName() {
        return functionalName;
    }

    public void setFunctionalDescription(String functionalDescription) {
        this.functionalDescription.setValue(functionalDescription);
    }

    public FunctionalDescription getFunctionalDescription() {
        return functionalDescription;
    }

    public void addAllDataElements() {
        addDataElement(new StringDataElement(FunctionalCode.FIELD_NAME, getFunctionalCode().getValue()));
        addDataElement(new IntegerDataElement(CodeLevel.FIELD_NAME, geFunctionalCodeLevel().getValue()));
        addDataElement(new StringDataElement(FunctionalName.FIELD_NAME, getFunctionalName().getValue()));
        addDataElement(new StringDataElement(FunctionalDescription.FIELD_NAME, getFunctionalDescription().getValue()));
    }

}
