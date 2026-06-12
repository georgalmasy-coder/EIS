package com.bepa.eis.server.dataprovider.entities;

import com.bepa.eis.common.enums.entity.EntityDataElement;
import com.bepa.eis.server.dataprovider.fields.AbstractField;
import com.bepa.eis.server.dataprovider.fields.booleans.RelevantToStakeholderRequirement;
import com.bepa.eis.server.dataprovider.fields.integers.CodeLevel;
import com.bepa.eis.server.dataprovider.fields.lookups.requirement.*;
import com.bepa.eis.server.dataprovider.fields.lookups.system.SystemDepartment;
import com.bepa.eis.server.dataprovider.fields.lookups.system.SystemOwner;
import com.bepa.eis.server.dataprovider.fields.lookups.system.TRL;
import com.bepa.eis.server.dataprovider.fields.strings.*;
import com.bepa.eis.server.dataprovider.fields.timestamp.DeadlineFinalized;
import com.bepa.eis.server.dataprovider.fields.timestamp.DeadlineNextTRL;
import com.bepa.eis.server.dataprovider.fields.timestamp.RequirementCaptureDate;

public class MapDataElementType {

    public static AbstractField toFieldObject(EntityDataElement entityDataElement) {

        return switch (entityDataElement) {
            case SBSCODE -> new SBSCode();
            case CODELEVEL -> new CodeLevel();
            case SYSTEMNAME -> new SystemName();
            case SYSTEMOWNERID -> new SystemOwner();
            case DEPARTMENTID -> new SystemDepartment();
            case TRLID -> new TRL();
            case DEADLINENEXTTRL -> new DeadlineNextTRL();
            case DEADLINEFINALIZED -> new DeadlineFinalized();
//GFA            case RBSCODE -> null;
            case REQRELEVANTTOSTAKEHOLDER -> new RelevantToStakeholderRequirement();
            case REQNAME -> new RequirementName();
            case REQDESCRIPTION -> new RequirementDescription();
            case REQHIGHLEVELCAPABILITY -> new RequirementHighlevelCapability();
            case REQTYPEID -> new RequirementType();
            case REQFREQUENCYID -> new RequirementFrequency();
            case REQPERFORMANCE -> new RequirementPerformance();
            case REQVERIFICATIONSTATEMENTID -> new RequirementVerificationStatement();
            case REQVERIFICATIONSTATUSID -> new RequirementVerificationStatus();
            case REQRATIONALESTATEMENT -> new RequirementRationaleStatement();
            case REQBUSINESSPRIORITYID -> new RequirementBusinessPriority();
            case REQTECHNICALPRIORITYID -> new RequirementTechnicalPriority();
            case REQCAPTUREDATE -> new RequirementCaptureDate();
            case REQOWNERID -> new RequirementOwner();
            case REQSTATUSID -> new RequirementStatus();
            case SUPPLIERNAME -> new SupplierName();
            case CONTRACTORNAME -> new ContractorName();
            case BASISREQCODE -> new StakeholderRequirementCode();
            case SYSTEMREQCODE -> new SystemRequirementCode();
            default -> throw new IllegalArgumentException("Unknown EntityDataElement: " + entityDataElement);
        };
    }
}
