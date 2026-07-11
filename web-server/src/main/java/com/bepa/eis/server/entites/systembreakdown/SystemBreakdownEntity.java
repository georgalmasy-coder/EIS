package com.bepa.eis.server.entites.systembreakdown;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.entities.common.EntityElementRecord;
import com.bepa.eis.server.dataprovider.entities.common.EntityRecord;
import com.bepa.eis.server.dataprovider.fields.integers.CodeLevel;
import com.bepa.eis.server.dataprovider.fields.lookups.system.SystemDepartment;
import com.bepa.eis.server.dataprovider.fields.lookups.system.SystemOwner;
import com.bepa.eis.server.dataprovider.fields.lookups.system.TRL;
import com.bepa.eis.server.dataprovider.fields.strings.SBSCode;
import com.bepa.eis.server.dataprovider.fields.strings.SystemName;
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

    private String sbsCode;
    private Integer sbsCodeLevel;
    private String systemName;
    private Integer systemOwnerId;
    private Integer departmentId;
    private Integer trlId;
    private LocalDate deadlineNextTrl;
    private LocalDate deadlineFinalized;

    @Override
    public EntityType getEntityType() {
        return SYSTEMS_BREAKDOWN;
    }

    @Override
    public String getCode() {
        return sbsCode;
    }

    @Override
    public String getName() {
        return systemName;
    }

    @Override
    public String getDescription() {
        return systemName;
    }

    @Override
    public String getSortKey() {
        return getSbsCode();
    }


    public SystemBreakdownEntity() {}

    public SystemBreakdownEntity(WebSession session) {
        super(session);
        setChangedByUserId(session.getUserId());
    }

    public void setSbsCode(String sbsCode) {
        this.sbsCode = sbsCode;
    }

    public String getSbsCode() {
        return sbsCode;
    }

    public void setSbsCodeLevel(Integer sbsCodeLevel) {
        this.sbsCodeLevel = sbsCodeLevel;
    }

    public Integer getSbsCodeLevel() {
        return sbsCodeLevel;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemOwnerId(Integer systemOwnerId) {
        this.systemOwnerId = systemOwnerId;
    }

    public void setSystemOwnerId(String systemOwnerId) {
        if (systemOwnerId != null) {
            try {
                this.systemOwnerId = Integer.parseInt(systemOwnerId);
            } catch (NumberFormatException e) {
                log.error("Invalid SystemOwnerId value : {}", systemOwnerId);
            }
        }
    }
    public SystemOwner getSystemOwner() {
        SystemOwner systemOwner = new SystemOwner(getWebSession());
        systemOwner.setValue(systemOwnerId);
        return systemOwner;
    }


    public Integer getSystemOwnerId() {
        return systemOwnerId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }
    public void setDepartmentId(String departmentId) {
        if (departmentId != null) {
            try {
                this.departmentId = Integer.parseInt(departmentId);
            } catch (NumberFormatException e) {
                log.error("Invalid departmentId value : {}", departmentId);
            }
        }
    }
    public SystemDepartment getSystemDepartment() {
        SystemDepartment systemDepartment = new SystemDepartment(getWebSession());
        systemDepartment.setValue(departmentId);
        return systemDepartment;
    }
    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setTrlId(Integer trlId) {
        this.trlId = trlId;
    }

    public void setTrlId(String trlId) {
        if (trlId != null) {
            try {
                this.trlId = Integer.parseInt(trlId);
            } catch (NumberFormatException e) {
                log.error("Invalid trlId value : {}", trlId);
            }
        }
    }
    public Integer getTrlId() {
        return trlId;
    }
    public TRL getTrl() {
        TRL trl = new TRL(getWebSession());
        trl.setValue(trlId);
        return trl;
    }

    public void setDeadlineNextTrl(LocalDate deadlineNextTrl) {
        this.deadlineNextTrl = deadlineNextTrl;
    }

    public void setDeadlineNextTrl(String deadlineNextTrl) {
        if (deadlineNextTrl != null && !deadlineNextTrl.isBlank()) {
            try {
                String str = deadlineNextTrl.length() > 10 ? deadlineNextTrl.substring(0,10) : deadlineNextTrl;
                this.deadlineNextTrl =  LocalDate.parse(str);
            } catch (Exception e) {
                log.error("Invalid deadlineNextTrl value : {}", deadlineNextTrl);
            }
        }
    }

    public LocalDate getDeadlineNextTrl() {
        return deadlineNextTrl;
    }

    public void setDeadlineFinalized(LocalDate deadlineFinalized) {
        this.deadlineFinalized = deadlineFinalized;
    }

    public void setDeadlineFinalized(String deadlineFinalized) {
        if (deadlineFinalized != null && !deadlineFinalized.isBlank()) {
            try {
                String str = deadlineFinalized.length() > 10 ? deadlineFinalized.substring(0,10) : deadlineFinalized;
                this.deadlineFinalized =  LocalDate.parse(str);
            } catch (Exception e) {
                log.error("Invalid deadlineFinalized value : {}", deadlineFinalized);
            }
        }
    }

    public LocalDate getDeadlineFinalized() {
        return deadlineFinalized;
    }

    public void addAllDataElements() {

        addDataElement(new StringDataElement(SBSCode.FIELD_NAME, getSbsCode()));
        addDataElement(new IntegerDataElement(CodeLevel.FIELD_NAME, getSbsCodeLevel()));
        addDataElement(new StringDataElement(SystemName.FIELD_NAME, getSystemName()));
        addDataElement(new IntegerDataElement(SystemOwner.FIELD_NAME,getSystemOwnerId()));
        addDataElement(new IntegerDataElement(SystemDepartment.FIELD_NAME, getDepartmentId()));
        addDataElement(new IntegerDataElement(TRL.FIELD_NAME, getTrlId()));
        addDataElement(new LocalDateDataElement(DeadlineNextTRL.FIELD_NAME, getDeadlineNextTrl()));
        addDataElement(new LocalDateDataElement(DeadlineFinalized.FIELD_NAME, getDeadlineFinalized()));

    }

    public static SystemBreakdownEntity map(EntityRecord entity) {

        SystemBreakdownEntity systemBreakdownEntity = null;

        if (entity != null) {

            systemBreakdownEntity = new SystemBreakdownEntity(entity.getWebSession());

            systemBreakdownEntity.setEntityId(entity.getEntityId());
            systemBreakdownEntity.setCustomerId(entity.getCustomerId());
            systemBreakdownEntity.setProjectId(entity.getProjectId());
            systemBreakdownEntity.setVersion(entity.getVersion());
            systemBreakdownEntity.setChangedByUserId(entity.getChangedByUserId());
            systemBreakdownEntity.setDateOfChange(entity.getChangedDateTime());
            systemBreakdownEntity.setActive(entity.isActive());

            for (EntityElementRecord elementRecord : entity.getEntityElementRecords()) {

                EntityDataElement entityDataElement = EntityDataElement.valueOf(elementRecord.getEntityDataElementType());

                if (entityDataElement != null) {
                    switch (entityDataElement) {
                        case SBSCODE :
                            systemBreakdownEntity.setSbsCode(elementRecord.getStringValue());
                            break;
                        case CODELEVEL :
                            systemBreakdownEntity.setSbsCodeLevel(elementRecord.getIntegerValue());
                            break;
                        case SYSTEMNAME :
                            systemBreakdownEntity.setSystemName(elementRecord.getStringValue());
                            break;
                        case SYSTEMOWNERID :
                            systemBreakdownEntity.setSystemOwnerId(elementRecord.getIntegerValue());
                            break;
                        case DEPARTMENTID :
                            systemBreakdownEntity.setDepartmentId(elementRecord.getIntegerValue());
                            break;
                        case TRLID :
                            systemBreakdownEntity.setTrlId(elementRecord.getIntegerValue());
                            break;
                        case DEADLINENEXTTRL :
                            systemBreakdownEntity.setDeadlineNextTrl(elementRecord.getLocalDateValue());
                            break;
                        case DEADLINEFINALIZED :
                            systemBreakdownEntity.setDeadlineFinalized(elementRecord.getLocalDateValue());
                            break;
                    }
                }
            }

        }

        return systemBreakdownEntity;
    }

}
