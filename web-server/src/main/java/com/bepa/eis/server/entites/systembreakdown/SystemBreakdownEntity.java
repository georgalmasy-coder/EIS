package com.bepa.eis.server.entites.systembreakdown;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.entities.Entity;
import com.bepa.eis.server.dataprovider.entities.common.EntityElementRecord;
import com.bepa.eis.server.dataprovider.entities.common.EntityRecord;
import com.bepa.eis.server.dataprovider.fields.integers.CodeLevel;
import com.bepa.eis.server.dataprovider.fields.lookups.codeselector.SystemsBreakdownParentCodeSelector;
import com.bepa.eis.server.dataprovider.fields.lookups.system.SBSCodeType;
import com.bepa.eis.server.dataprovider.fields.lookups.system.SystemDepartment;
import com.bepa.eis.server.dataprovider.fields.lookups.system.SystemOwner;
import com.bepa.eis.server.dataprovider.fields.lookups.system.TRL;
import com.bepa.eis.server.dataprovider.fields.strings.*;
import com.bepa.eis.server.dataprovider.fields.timestamp.DeadlineFinalized;
import com.bepa.eis.server.dataprovider.fields.timestamp.DeadlineNextTRL;
import com.bepa.eis.server.entites.AbstractEntity;
import com.bepa.eis.common.enums.entity.EntityDataElement;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.entites.datatypes.IntegerDataElement;
import com.bepa.eis.server.entites.datatypes.LocalDateDataElement;
import com.bepa.eis.server.entites.datatypes.StringDataElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

import static com.bepa.eis.common.enums.entity.EntityType.SYSTEMS_BREAKDOWN;

public class SystemBreakdownEntity extends AbstractEntity {

    private static final Logger log = LoggerFactory.getLogger(SystemBreakdownEntity.class);

    private SBSCode sbsCode;
    private CodeLevel sbsCodeLevel;
    private SystemName systemName;
    private SystemOwner systemOwner;
    private SystemDepartment systemDepartment;
    private TRL trl;
    private DeadlineNextTRL deadlineNextTrl;
    private DeadlineFinalized deadlineFinalized;

    @Override
    public EntityType getEntityType() {
        return SYSTEMS_BREAKDOWN;
    }

    @Override
    public String getCode() {
        return sbsCode.getValue();
    }

    @Override
    public String getName() {
        return systemName.getValue();
    }

    @Override
    public String getDescription() {
        return getName();
    }

    @Override
    public String getSortKey() {
        return getSbsCode().getValue();
    }

    @Override
    public void initializeFields() {
        sbsCode = new SBSCode();
        sbsCodeLevel = new CodeLevel();
        systemName = new SystemName();
        systemOwner = new SystemOwner(getWebSession());
        systemDepartment  = new SystemDepartment(getWebSession());
        trl = new TRL(getWebSession());
        deadlineNextTrl = new DeadlineNextTRL();
        deadlineFinalized = new DeadlineFinalized();
    }

    @Override
    public void addAllFieldElementsForList(Entity entityElement) {
        entityElement.addElement(sbsCode);
        entityElement.addElement(sbsCodeLevel);
        entityElement.addElement(systemName);
        entityElement.addElement(systemOwner);
        entityElement.addElement(systemDepartment);
        entityElement.addElement(trl);
        entityElement.addElement(deadlineNextTrl);
        entityElement.addElement(deadlineFinalized);
   }

    @Override
    public void addAllFieldElementsForEdit(Entity entityElement) {
        sbsCode.setFieldNotEditable();
        sbsCode.setFieldRequired();
        entityElement.addElement(sbsCode);

        sbsCodeLevel.setFieldNotVisible();
        entityElement.addElement(sbsCodeLevel);

        systemName.setFieldEditable();
        entityElement.addElement(systemName);

        systemOwner.setFieldEditable();
        entityElement.addElement(systemOwner);

        systemDepartment.setFieldEditable();
        entityElement.addElement(systemDepartment);

        trl.setFieldEditable();
        entityElement.addElement(trl);

        deadlineNextTrl.setFieldEditable();
        entityElement.addElement(deadlineNextTrl);

        deadlineFinalized.setFieldEditable();
        entityElement.addElement(deadlineFinalized);
    }

    @Override
    public void addAllFieldElementsForCreate(Entity entityElement, Integer parentEntityId) {

        SystemsBreakdownParentCodeSelector parentCodeSelector = new SystemsBreakdownParentCodeSelector(getWebSession());

        if ( parentEntityId == null) {
            SBSCodeType sbsCodeType = new SBSCodeType();
            sbsCodeType.setFieldEditable();
            sbsCodeType.setFieldRequired();
            entityElement.addElement(sbsCodeType);
        }

        String nextCode = parentCodeSelector.getNextAvailableCodeValue(getWebSession(), parentEntityId);
        sbsCode = new SBSCode(true);
        sbsCode.setValue(nextCode);
        sbsCode.setFieldNotEditable();
        sbsCode.setFieldRequired();
        entityElement.addElement(sbsCode);

        systemName.setFieldEditable();
        entityElement.addElement(systemName);

        systemOwner.setFieldEditable();
        entityElement.addElement(systemOwner);

        systemDepartment.setFieldEditable();
        entityElement.addElement(systemDepartment);

        trl.setFieldEditable();
        entityElement.addElement(trl);

        deadlineNextTrl.setFieldEditable();
        entityElement.addElement(deadlineNextTrl);

        deadlineFinalized.setFieldEditable();
        entityElement.addElement(deadlineFinalized);
    }

    public SystemBreakdownEntity() {}

    public SystemBreakdownEntity(WebSession session) {
        super(session);
    }

    public SystemBreakdownEntity(WebSession webSession, EntityRecord entityRecord) {
        super(webSession, entityRecord);

        for (EntityElementRecord elementRecord : entityRecord.getEntityElementRecords()) {

            EntityDataElement entityDataElement = EntityDataElement.valueOf(elementRecord.getEntityDataElementType());

            if (entityDataElement != null) {
                switch (entityDataElement) {
                    case SBSCODE:
                        sbsCode.setValue(elementRecord.getStringValue());
                        break;
                    case CODELEVEL:
                        sbsCodeLevel.setValue(elementRecord.getIntegerValue());
                        break;
                    case SYSTEMNAME:
                        systemName.setValue(elementRecord.getStringValue());
                        break;
                    case SYSTEMOWNERID:
                        systemOwner.setValue(elementRecord.getIntegerValue());
                        break;
                    case DEPARTMENTID:
                        systemDepartment.setValue(elementRecord.getIntegerValue());
                        break;
                    case TRLID:
                        trl.setValue(elementRecord.getIntegerValue());
                        break;
                    case DEADLINENEXTTRL:
                        deadlineNextTrl.setValue(elementRecord.getLocalDateValue());
                        break;
                    case DEADLINEFINALIZED:
                        deadlineFinalized.setValue(elementRecord.getLocalDateValue());
                        break;
                }
            }
        }
    }

    public void setSbsCode(String sbsCode) {
        this.sbsCode.setValue(sbsCode);
    }

    public SBSCode getSbsCode() {
        return sbsCode;
    }

    public void setSbsCodeLevel(Integer sbsCodeLevel) {
        this.sbsCodeLevel.setValue(sbsCodeLevel);
    }

    public CodeLevel getSbsCodeLevel() {
        return sbsCodeLevel;
    }

    public void setSystemName(String systemName) {
        this.systemName.setValue(systemName);
    }

    public SystemName getSystemName() {
        return systemName;
    }

    public void setSystemOwner(Integer systemOwnerId) {
        this.systemOwner.setValue(systemOwnerId);
    }

    public void setSystemOwner(String systemOwnerId) {
        if (systemOwnerId != null) {
            try {
                setSystemOwner(Integer.parseInt(systemOwnerId));
            } catch (NumberFormatException e) {
                log.error("Invalid SystemOwnerId value : {}", systemOwnerId);
            }
        } else {
            this.systemOwner.setValue(null);
        }
    }
    public SystemOwner getSystemOwner() {
        return systemOwner;
    }

    public Integer getSystemOwnerId() {
        return systemOwner.getValue();
    }

    public void setDepartment(Integer departmentId) {
        this.systemDepartment.setValue(departmentId);
    }

    public void setDepartment(String departmentId) {
        if (departmentId != null) {
            try {
                setDepartment(Integer.parseInt(departmentId));
            } catch (NumberFormatException e) {
                log.error("Invalid departmentId value : {}", departmentId);
            }
        } else {
            this.systemDepartment.setValue(null);
        }
    }
    public SystemDepartment getSystemDepartment() {
        return systemDepartment;
    }
    public Integer getDepartmentId() {
        return systemDepartment.getValue();
    }

    public void setTrlId(Integer trlId) {
        this.trl.setValue(trlId);
    }

    public void setTrlId(String trlId) {
        if (trlId != null) {
            try {
                setTrlId(Integer.parseInt(trlId));
            } catch (NumberFormatException e) {
                log.error("Invalid trlId value : {}", trlId);
            }
        } else {
            this.trl.setValue(null);
        }
    }
    public Integer getTrlId() {
        return trl.getValue();
    }
    public TRL getTrl() {
        return trl;
    }

    public void setDeadlineNextTrl(LocalDate deadlineNextTrl) {
        this.deadlineNextTrl.setValue(deadlineNextTrl);
    }

    public void setDeadlineNextTrl(String deadlineNextTrl) {
        if (deadlineNextTrl != null && !deadlineNextTrl.isBlank()) {
            try {
                String str = deadlineNextTrl.length() > 10 ? deadlineNextTrl.substring(0,10) : deadlineNextTrl;
                this.deadlineNextTrl.setValue(LocalDate.parse(str));
            } catch (Exception e) {
                log.error("Invalid deadlineNextTrl value : {}", deadlineNextTrl);
            }
        } else {
            this.deadlineNextTrl.setValue((LocalDate)null);
        }
    }

    public LocalDate getDeadlineNextTrl() {
        return deadlineNextTrl.getValue();
    }

    public void setDeadlineFinalized(LocalDate deadlineFinalized) {
        this.deadlineFinalized.setValue(deadlineFinalized);
    }

    public void setDeadlineFinalized(String deadlineFinalized) {
        if (deadlineFinalized != null && !deadlineFinalized.isBlank()) {
            try {
                String str = deadlineFinalized.length() > 10 ? deadlineFinalized.substring(0,10) : deadlineFinalized;
                this.deadlineFinalized.setValue(LocalDate.parse(str));
            } catch (Exception e) {
                log.error("Invalid deadlineFinalized value : {}", deadlineFinalized);
            }
        } else {
            this.deadlineFinalized.setValue((LocalDate) null);
        }
    }

    public LocalDate getDeadlineFinalized() {
        return deadlineFinalized.getValue();
    }

    public void addAllDataElements() {
        addDataElement(new StringDataElement(SBSCode.FIELD_NAME, getSbsCode().getValue()));
        addDataElement(new IntegerDataElement(CodeLevel.FIELD_NAME, getSbsCodeLevel().getValue()));
        addDataElement(new StringDataElement(SystemName.FIELD_NAME, getSystemName().getValue()));
        addDataElement(new IntegerDataElement(SystemOwner.FIELD_NAME,getSystemOwner().getValue()));
        addDataElement(new IntegerDataElement(SystemDepartment.FIELD_NAME, getSystemDepartment().getValue()));
        addDataElement(new IntegerDataElement(TRL.FIELD_NAME, getTrl().getValue()));
        addDataElement(new LocalDateDataElement(DeadlineNextTRL.FIELD_NAME, getDeadlineNextTrl()));
        addDataElement(new LocalDateDataElement(DeadlineFinalized.FIELD_NAME, getDeadlineFinalized()));
    }

}
