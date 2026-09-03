package com.bepa.eis.server.api.web.application.views.pro.interfacematrix;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.dataprovider.entities.FunctionalStructureProvider;
import com.bepa.eis.server.dataprovider.entities.LogicalStructureProvider;
import com.bepa.eis.server.entites.functional.FunctionalStructureEntity;
import com.bepa.eis.server.entites.logical.LogicalStructureEntity;
import org.w3c.dom.Element;

import java.util.List;

public class FsysInterfaceMatrixDocument extends InterfaceMatrixDocument {

    private List<FunctionalStructureEntity> listOfFunctionalStructuresEntities = null;

    protected FsysInterfaceMatrixDocument(WebSession webSession) throws Exception {
        super(webSession);
    }

    @Override
    public PageType getPageType() {
        return PageType.FSYS_INTERFACE_MANAGEMENT_PAGE;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.FUNCTIONAL_STRUCTURE;
    }

    @Override
    public String getTitle() {
        return "Interface Management";
    }

    @Override
    public String getColumnGroupLabel() {
        return "To Function";
    }

    @Override
    public String getRowGroupLabel() {
        return "From Function";
    }


    @Override
    public Element getEntityElements() {
        Element physicalStructuresElement = getDoc().createElement(ENTITIES_ELEMENT_NAME);

        for (FunctionalStructureEntity entity : getFunctionalStructures()) {
            Element entityElement = getDoc().createElement(ENTITY_ELEMENT_NAME);
            addEntityElement(entityElement, "entityId", entity.getEntityId());
            addEntityElement(entityElement, "id", entity.getFunctionalCode());
            addEntityElement(entityElement, "name", entity.getFunctionalName());
            physicalStructuresElement.appendChild(entityElement);
        }

        return physicalStructuresElement;
    }

    @Override
    public String getEntityCount() {
        return String.valueOf(getFunctionalStructures().size());
    }

    private List<FunctionalStructureEntity> getFunctionalStructures() {
        if (listOfFunctionalStructuresEntities == null) {
            FunctionalStructureProvider functionalStructureProvider = new FunctionalStructureProvider(getWebSession());
            List<FunctionalStructureEntity> listOfFunctionalEntities = functionalStructureProvider.getAllFunctionalStructure(false);
            sortBySortKey(listOfFunctionalEntities);
            listOfFunctionalStructuresEntities = listOfFunctionalEntities;
        }
        return listOfFunctionalStructuresEntities;
    }

}
