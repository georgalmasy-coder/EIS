package com.bepa.eis.common.enums.entity;

import static com.bepa.eis.common.enums.entity.EntityDataElement.*;

public enum EntityType {
    SYSTEMS_BREAKDOWN(2, "SBS", "Systems breakdown", "systemBreakdownDocument", "systembreakdowns", "systembreakdown",
            new EntityDataElement[]{
                    SBSCODE,
                    CODELEVEL,
                    SYSTEMNAME,
                    SYSTEMOWNERID,
                    DEPARTMENTID,
                    TRLID,
                    DEADLINENEXTTRL,
                    DEADLINEFINALIZED},
            SBSCODE,
            SYSTEMNAME
    ),

     SUPPLIER(3, "SUP", "Supplier", "supplierDocument", "suppliers", "supplier",
            new EntityDataElement[]{
                    SUPPLIERNAME
            },
            null,
             SUPPLIERNAME
     ),

    CONTRACTOR(4, "CON","Contractor", "contractorDocument", "contractors", "contractor",
            new EntityDataElement[]{
                    CONTRACTORNAME
            },
            null,
            CONTRACTORNAME
    ),

    STAKEHOLDER_REQUIREMENT(5, "STHREQ", "Stakeholder requirement", "stakeholderRequirementDocument", "stakeholderRequirements", "stakeholderRequirement",
            new EntityDataElement[]{
                    BASISREQCODE,
                    CODELEVEL,
                    REQNAME,
                    REQDESCRIPTION},
            BASISREQCODE,
            REQNAME
    ),

    SYSTEM_REQUIREMENT(6, "SYSREQ", "System requirement", "systemRequirementDocument", "systemRequirements", "systemRequirement",
            new EntityDataElement[]{
                    SYSTEMREQCODE,
                    CODELEVEL,
                    REQNAME,
                    REQDESCRIPTION,
                    REQHIGHLEVELCAPABILITY,
                    REQTYPEID,
                    REQFREQUENCYID,
                    REQPERFORMANCE,
                    REQVERIFICATIONSTATEMENTID,
                    REQTECHNICALPRIORITYID,
                    REQVERIFICATIONSTATUSID,
                    REQRATIONALESTATEMENT,
                    REQBUSINESSPRIORITYID,
                    REQCAPTUREDATE,
                    REQOWNERID,
                    REQSTATUSID,
                    REQRELEVANTTOSTAKEHOLDER
            },
            SYSTEMREQCODE,
            REQNAME
    );


    private final int id;
    private final String shortDescription;
    private final String description;
    private final String singleRootElementName;
    private final String multipleRootElementName;
    private final String entityElementName;
    private final EntityDataElement[] dataElements;
    private final EntityDataElement entityCodeColumn;
    private final EntityDataElement entityNameColumn;

    // Constructor
    EntityType(int id, String shortDescription, String description, String singleRootElementName, String multipleRootElementName, String entityElementName,
               EntityDataElement[] dataElements,
               EntityDataElement entityCodeColumn,
               EntityDataElement entityNameColumn) {
        this.id = id;
        this.shortDescription = shortDescription;
        this.description = description;
        this.singleRootElementName = singleRootElementName;
        this.multipleRootElementName = multipleRootElementName;
        this.entityElementName = entityElementName;
        this.dataElements = dataElements;
        this.entityCodeColumn = entityCodeColumn;
        this.entityNameColumn = entityNameColumn;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public String getDescription() {
        return description;
    }

    public String getSingleRootElementName() {
        return singleRootElementName;
    }

    public String getMultipleRootElementName() {
        return multipleRootElementName;
    }

    public String getEntityElementName() {
        return entityElementName;
    }

    public EntityDataElement[] getDataElements() {
        return dataElements;
    }

    public EntityDataElement getEntityCodeColumn() {
        return entityCodeColumn;
    }

    public EntityDataElement getEntityNameColumn() {
        return entityNameColumn;
    }

    public static EntityType fromId(int id) {
        for (EntityType type : EntityType.values()) {
            if (type.getId() == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown EntityType id: " + id);
    }
}
