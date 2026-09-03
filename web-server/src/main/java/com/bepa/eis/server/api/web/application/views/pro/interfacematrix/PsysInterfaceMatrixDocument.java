package com.bepa.eis.server.api.web.application.views.pro.interfacematrix;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.dataprovider.entities.SystemBreakdownProvider;
import com.bepa.eis.server.entites.systembreakdown.SystemBreakdownEntity;
import org.w3c.dom.Element;

import java.util.List;

public class PsysInterfaceMatrixDocument extends InterfaceMatrixDocument {

    private final EntityType entityType;

    private List<SystemBreakdownEntity> listOfPhysicalStructuresEntities = null;

    @Override
    public PageType getPageType() {
        return PageType.PSYS_INTERFACE_MANAGEMENT_PAGE;
    }

    @Override
    public EntityType getEntityType() {
        return entityType != null ? entityType : EntityType.SYSTEMS_BREAKDOWN;
    }

    @Override
    public String getTitle() {
        return "Interface Management";
    }

    @Override
    public String getColumnGroupLabel() {
        return "To Physical Structure";
    }

    @Override
    public String getRowGroupLabel() {
        return "From Physical Structure";
    }


    @Override
    public Element getEntityElements() {
        Element physicalStructuresElement = getDoc().createElement(ENTITIES_ELEMENT_NAME);

        for (SystemBreakdownEntity entity : getPhysicalStructures()) {
            Element entityElement = getDoc().createElement(ENTITY_ELEMENT_NAME);
            addEntityElement(entityElement, "entityId", entity.getEntityId());
            addEntityElement(entityElement, "id", entity.getSbsCode());
            addEntityElement(entityElement, "name", entity.getSystemName());
            addEntityElement(entityElement, "systemOwner", entity.getSystemOwner());
            addEntityElement(entityElement, "trlId", entity.getTrl());
            addEntityElement(entityElement, "departmentId", entity.getSystemDepartment());
            addEntityElement(entityElement, "deadlineNextTrl", entity.getDeadlineNextTrlField());
            physicalStructuresElement.appendChild(entityElement);
        }

        return physicalStructuresElement;
    }

    @Override
    public String getEntityCount() {
        return String.valueOf(getPhysicalStructures().size());
    }

    protected PsysInterfaceMatrixDocument(WebSession webSession, EntityType entityType) throws Exception {
        super(webSession);
        this.entityType = entityType;
    }

    private List<SystemBreakdownEntity> getPhysicalStructures() {
        if (listOfPhysicalStructuresEntities == null) {
            SystemBreakdownProvider systemBreakdownProvider = new SystemBreakdownProvider(getWebSession());
            List<SystemBreakdownEntity> listOfSystemBreakdownEntities = systemBreakdownProvider.getAllSystemBreakdown(false);
            sortBySortKey(listOfSystemBreakdownEntities);
            listOfPhysicalStructuresEntities = listOfSystemBreakdownEntities;
        }
        return listOfPhysicalStructuresEntities;
    }

}
