package com.bepa.eis.server.api.web.application.views.pro.interfacematrix;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.dataprovider.entities.LogicalStructureProvider;
import com.bepa.eis.server.dataprovider.entities.SystemBreakdownProvider;
import com.bepa.eis.server.entites.logical.LogicalStructureEntity;
import com.bepa.eis.server.entites.systembreakdown.SystemBreakdownEntity;
import org.w3c.dom.Element;

import java.util.List;

public class LsysInterfaceMatrixDocument extends InterfaceMatrixDocument {

    private final EntityType entityType;

    private List<LogicalStructureEntity> listOfLogicalStructuresEntities = null;

    @Override
    public PageType getPageType() {
        return PageType.LSYS_INTERFACE_MANAGEMENT_PAGE;
    }

    @Override
    public EntityType getEntityType() {
        return entityType != null ? entityType : EntityType.LOGICAL_STRUCTURE;
    }

    @Override
    public String getTitle() {
        return "Interface Management";
    }

    @Override
    public String getColumnGroupLabel() {
        return "To Logical Design";
    }

    @Override
    public String getRowGroupLabel() {
        return "From Logical Design";
    }


    @Override
    public Element getEntityElements() {
        Element physicalStructuresElement = getDoc().createElement(ENTITIES_ELEMENT_NAME);

        for (LogicalStructureEntity entity : getLogicalStructures()) {
            Element entityElement = getDoc().createElement(ENTITY_ELEMENT_NAME);
            addEntityElement(entityElement, "entityId", entity.getEntityId());
            addEntityElement(entityElement, "id", entity.getLogicalCode());
            addEntityElement(entityElement, "name", entity.getLogicalName());
            physicalStructuresElement.appendChild(entityElement);
        }

        return physicalStructuresElement;
    }

    @Override
    public String getEntityCount() {
        return String.valueOf(getLogicalStructures().size());
    }

    protected LsysInterfaceMatrixDocument(WebSession webSession, EntityType entityType) throws Exception {
        super(webSession);
        this.entityType = entityType;
    }

    private List<LogicalStructureEntity> getLogicalStructures() {
        if (listOfLogicalStructuresEntities == null) {
            LogicalStructureProvider logicalStructureProvider = new LogicalStructureProvider(getWebSession());
            List<LogicalStructureEntity> listOfLogicalEntities = logicalStructureProvider.getAllLogicalStructures(false);
            sortBySortKey(listOfLogicalEntities);
            listOfLogicalStructuresEntities = listOfLogicalEntities;
        }
        return listOfLogicalStructuresEntities;
    }

}
