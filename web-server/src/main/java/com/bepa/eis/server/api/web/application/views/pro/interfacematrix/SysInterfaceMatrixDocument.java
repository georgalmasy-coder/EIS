package com.bepa.eis.server.api.web.application.views.pro.interfacematrix;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.dataprovider.entities.SystemRequirementProvider;
import com.bepa.eis.server.entites.systemrequirement.SystemRequirementEntity;
import org.w3c.dom.Element;

import java.util.List;

public class SysInterfaceMatrixDocument extends InterfaceMatrixDocument {

    private List<SystemRequirementEntity> listOfSystemRequirementEntities = null;

    protected SysInterfaceMatrixDocument(WebSession webSession) throws Exception {
        super(webSession);
    }

    @Override
    public PageType getPageType() {
        return PageType.SYS_INTERFACE_MANAGEMENT_PAGE;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SYSTEM_REQUIREMENT;
    }

    @Override
    public String getTitle() {
        return "Interface Management";
    }

    @Override
    public String getColumnGroupLabel() {
        return "To Systems Requirement";
    }

    @Override
    public String getRowGroupLabel() {
        return "From Systems Requirement";
    }


    @Override
    public Element getEntityElements() {
        Element physicalStructuresElement = getDoc().createElement(ENTITIES_ELEMENT_NAME);

        for (SystemRequirementEntity entity : getStakeholderRequirements()) {
            Element entityElement = getDoc().createElement(ENTITY_ELEMENT_NAME);
            addEntityElement(entityElement, "entityId", entity.getEntityId());
            addEntityElement(entityElement, "id", entity.getRequirementCode());
            addEntityElement(entityElement, "name", entity.getRequirementName());
            physicalStructuresElement.appendChild(entityElement);
        }

        return physicalStructuresElement;
    }

    @Override
    public String getEntityCount() {
        return String.valueOf(getStakeholderRequirements().size());
    }

    private List<SystemRequirementEntity> getStakeholderRequirements() {
        if (listOfSystemRequirementEntities == null) {
            SystemRequirementProvider systemRequirementProvider = new SystemRequirementProvider(getWebSession());
            List<SystemRequirementEntity> listOfEntities = systemRequirementProvider.getAllSystemRequirement(false);
            sortBySortKey(listOfEntities);
            listOfSystemRequirementEntities = listOfEntities;
        }
        return listOfSystemRequirementEntities;
    }

}
