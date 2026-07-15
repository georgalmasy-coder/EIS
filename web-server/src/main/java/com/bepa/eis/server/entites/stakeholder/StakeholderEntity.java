package com.bepa.eis.server.entites.stakeholder;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityDataElement;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.dataprovider.entities.Entity;
import com.bepa.eis.server.dataprovider.entities.common.EntityElementRecord;
import com.bepa.eis.server.dataprovider.entities.common.EntityRecord;
import com.bepa.eis.server.dataprovider.fields.strings.*;
import com.bepa.eis.server.dataprovider.fields.strings.email.ContactEmail;
import com.bepa.eis.server.dataprovider.fields.strings.phone.ContactPhone;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.server.entites.datatypes.StringDataElement;

import static com.bepa.eis.common.enums.entity.EntityType.STAKEHOLDER;

public class StakeholderEntity extends AbstractEntity {

    private StakeholderName stakeholderName;
    private StakeholderDescription stakeholderDescription;
    private ContactName contactName;
    private ContactEmail contactEmail;
    private ContactPhone contactPhone;

    @Override
    public EntityType getEntityType() {
        return STAKEHOLDER;
    }

    @Override
    public String getCode() {
        return "";
    }

    @Override
    public String getName() {
        return stakeholderName.getValue();
    }

    @Override
    public String getDescription() {
        return stakeholderDescription.getValue();
    }

    @Override
    public void initializeFields() {
        stakeholderName = new StakeholderName();
        stakeholderDescription = new StakeholderDescription();
        contactName = new ContactName();
        contactEmail = new ContactEmail();
        contactPhone = new ContactPhone();
        setChangedByUserId(getWebSession().getUserId());
    }

    @Override
    public void addAllFieldElementsForList(Entity entityElement) {
        entityElement.addElement(stakeholderName);
        entityElement.addElement(stakeholderDescription);
        entityElement.addElement(contactName);
        entityElement.addElement(contactEmail);
        entityElement.addElement(contactPhone);
    }

    @Override
    public void addAllFieldElementsForEdit(Entity entityElement) {
        entityElement.addElement(stakeholderName);
        entityElement.addElement(stakeholderDescription);
        entityElement.addElement(contactName);
        entityElement.addElement(contactEmail);
        entityElement.addElement(contactPhone);
    }

    @Override
    public void addAllFieldElementsForCreate(Entity entityElement, Integer parentEntityId) {
        entityElement.addElement(stakeholderName);
        entityElement.addElement(stakeholderDescription);
        entityElement.addElement(contactName);
        entityElement.addElement(contactEmail);
        entityElement.addElement(contactPhone);
    }

    public StakeholderEntity(WebSession webSession) {
        super(webSession);
    }

    public StakeholderEntity(WebSession webSession, EntityRecord entityRecord) {
        super(webSession, entityRecord);

        for (EntityElementRecord elementRecord : entityRecord.getEntityElementRecords()) {

            EntityDataElement entityDataElement = EntityDataElement.valueOf(elementRecord.getEntityDataElementType());

            if (entityDataElement != null) {
                switch (entityDataElement) {
                    case STAKEHOLDERNAME:
                        stakeholderName.setValue(elementRecord.getStringValue());
                        break;
                    case STAKEHOLDERDESCRIPTION:
                        stakeholderDescription.setValue(elementRecord.getStringValue());
                        break;
                    case CONTACTNAME:
                        contactName.setValue(elementRecord.getStringValue());
                        break;
                    case CONTACTEMAIL:
                        contactEmail.setValue(elementRecord.getStringValue());
                        break;
                    case CONTACTPHONE:
                        contactPhone.setValue(elementRecord.getStringValue());
                        break;
                }
            }
        }
    }

    public void setStakeholderName(String stakeholderName) {
        this.stakeholderName.setValue(stakeholderName);
    }
    public String getStakeholderName() {
        return stakeholderName.getValue();
    }
    public void setStakeholderDescription(String stakeholderDescription) {
        this.stakeholderDescription.setValue(stakeholderDescription);
    }
    public String getStakeholderDescription() {
        return stakeholderDescription.getValue();
    }
    public void setStakeholderContactName(String stakeholderContactName) {
        this.contactName.setValue(stakeholderContactName);
    }
    public String getStakeholderContactName() {
        return contactName.getValue();
    }
    public void setStakeholderContactEmail(String stakeholderContactEmail) {
        this.contactEmail.setValue(stakeholderContactEmail);
    }
    public String getStakeholderContactEmail() {
        return contactEmail.getValue();
    }
    public void setStakeholderContactPhone(String stakeholderContactPhone) {
        this.contactPhone.setValue(stakeholderContactPhone);
    }
    public String getStakeholderContactPhone() {
        return contactPhone.getValue();
    }

    public void addAllDataElements() {
        addDataElement(new StringDataElement(StakeholderName.FIELD_NAME, stakeholderName.getValue()));
        addDataElement(new StringDataElement(StakeholderDescription.FIELD_NAME, stakeholderDescription.getValue()));
        addDataElement(new StringDataElement(ContactName.FIELD_NAME, contactName.getValue()));
        addDataElement(new StringDataElement(ContactEmail.FIELD_NAME, contactEmail.getValue()));
        addDataElement(new StringDataElement(ContactPhone.FIELD_NAME, contactPhone.getValue()));
    }

}
