package com.bepa.eis.common.enums.entity;

import static com.bepa.eis.common.enums.entity.EntityElementType.*;

public enum EntityDataElement {

//    ACTIVE(1, "Active", "Active", BOOLEAN, new Active()),
    SBSCODE(2, "SBS code", "SBSCode", STRING),
    CODELEVEL(3, "Code level", "CodeLevel", INTEGER),
    SYSTEMNAME(4, "System Name", "SystemName", STRING),
    SYSTEMOWNERID(5, "System Owner", "SystemOwnerId", INTEGER),
    DEPARTMENTID(6, "Department", "DepartmentId", INTEGER),
    //SUPPLIERID(7, "Supplier", "SupplierId", INTEGER, new Supplier(null)),
    //CONTRACTORID(8, "Contractor", "ContractorId", INTEGER, new Contractor(null)),
    TRLID(9, "Trl", "TrlId", INTEGER),
    DEADLINENEXTTRL(10, "Deadline Next TRL", "DeadlineNextTRL", LOCAL_DATE),
    DEADLINEFINALIZED(11, "Deadline Finalized", "DeadlineFinalized", LOCAL_DATE),

    PROJECTNAME(12, "Project Name", "ProjectName", STRING),

//    RBSCODE(13,"RBS code", "RBSCode", STRING),
//    REQRELEVANTTOSTAKEHOLDER(14,"Is Requirement Relevant To Stakeholder", "RelevantToStakeholderRequirement", BOOLEAN),
    REQNAME(15, "Name", "RequirementName", STRING),
    REQDESCRIPTION(16,"Description", "RequirementDescription", STRING),
    REQHIGHLEVELCAPABILITY(17, "High Level Capability", "RequirementHighlevelCapability", STRING),
    REQTYPEID(18, "Type", "RequirementTypeId", INTEGER),
    REQFREQUENCYID(19, "Frequency", "RequirementFrequencyId", INTEGER),
    REQPERFORMANCE(20,"Performance", "RequirementPerformance", STRING),
    REQVERIFICATIONSTATEMENTID(21, "Verification Statement", "RequirementVerificationStatementId", INTEGER),
    REQVERIFICATIONSTATUSID(22, "Verification Status", "RequirementVerificationStatusId", INTEGER),
    REQRATIONALESTATEMENT(23, "Rationale Statement", "RationaleStatement", STRING),
    REQBUSINESSPRIORITYID(24,"Business Priority", "RequirementBusinessPriorityId", INTEGER),
    REQTECHNICALPRIORITYID(25,"Technical Priority", "RequirementTechnicalPriorityId", INTEGER),
    REQCAPTUREDATE(26,"Capture Date", "RequirementCaptureDate", LOCAL_DATE),
    REQOWNERID(27, "Requirement Owner", "RequirementOwnerId", INTEGER),
    REQSTATUSID(28, "Requirement Status", "RequirementStatusId", INTEGER),

    SUPPLIERNAME(29, "Supplier", "SupplierName", STRING),
    CONTRACTORNAME(30, "Contractor", "ContractorName", STRING),
    STAKEHOLDERNAME(31, "Stakeholder Name", "StakeholderName", STRING),
    STAKEHOLDERDESCRIPTION(32,"Stakeholder Description", "StakeholderDescription", STRING),
    CONTACTNAME(34, "Stakeholder Contact", "ContactName", STRING),
    CONTACTEMAIL(35, "Stakeholder Contact Email", "ContactEmail", STRING),
    CONTACTPHONE(36, "Stakeholder Contact Phone", "ContactPhone", STRING),
    STAKEHOLDER(37, "Stakeholder", "Stakeholder", INTEGER),

    BASISREQCODE(40,"Stakeholder Requirement Code", "StakeholderReqCode", STRING),
    SYSTEMREQCODE(41,"Systems Requirement Code", "SystemReqCode", STRING),

    LOGICALCODE(50,"Logical Code", "LogicalCode", STRING),
    LOGICALNAME(51,"Logical Name", "LogicalName", STRING),
    LOGICALDESCRIPTION(52,"Logical Description", "LogicalDescription", STRING),

    FUNCTIONALCODE(60,"Functional Code", "FunctionalCode", STRING),
    FUNCTIONALNAME(61,"Functional Name", "FunctionalName", STRING),
    FUNCTIONALDESCRIPTION(62,"Functional Description", "FunctionalDescription", STRING),
    ;

    private final int id;
    private final String description;
    private final String fieldName;
    private final EntityElementType entityElementType;

    // Constructor
    EntityDataElement(int id, String description, String fieldName, EntityElementType entityElementType) {
        this.id = id;
        this.description = description;
        this.fieldName = fieldName;
        this.entityElementType = entityElementType;
    }

    // Getters
    public int getId() {
        return id;
    }
    public String getDescription() {
        return description;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFieldNameUpperCase() {
        return fieldName.toUpperCase();
    }

    public EntityElementType getEntityElementType() {
        return entityElementType;
    }

    public static EntityDataElement valueOf(int value) {
        for (EntityDataElement entityDataElement : EntityDataElement.values()) {
            if (entityDataElement.id == value) return entityDataElement;
        }
        return null;
    }
}
