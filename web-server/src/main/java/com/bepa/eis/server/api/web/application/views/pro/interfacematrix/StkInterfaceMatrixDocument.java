package com.bepa.eis.server.api.web.application.views.pro.interfacematrix;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.dataprovider.entities.StakeholderRequirementProvider;
import com.bepa.eis.server.entites.stakeholderrequirement.StakeholderRequirementEntity;
import org.w3c.dom.Element;

import java.util.List;

public class StkInterfaceMatrixDocument extends InterfaceMatrixDocument {

    private List<StakeholderRequirementEntity> listOfStakeholderRequirementEntities = null;

    protected StkInterfaceMatrixDocument(WebSession webSession) throws Exception {
        super(webSession);
    }

    @Override
    public PageType getPageType() {
        return PageType.STK_INTERFACE_MANAGEMENT_PAGE;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.STAKEHOLDER_REQUIREMENT;
    }

    @Override
    public String getTitle() {
        return "Interface Management";
    }

    @Override
    public String getColumnGroupLabel() {
        return "To Stakeholder Requirement";
    }

    @Override
    public String getRowGroupLabel() {
        return "From Stakeholder Requirement";
    }


    @Override
    public Element getEntityElements() {
        Element physicalStructuresElement = getDoc().createElement(ENTITIES_ELEMENT_NAME);

        for (StakeholderRequirementEntity entity : getStakeholderRequirements()) {
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

    private List<StakeholderRequirementEntity> getStakeholderRequirements() {
        if (listOfStakeholderRequirementEntities == null) {
            StakeholderRequirementProvider stakeholderRequirementProvider = new StakeholderRequirementProvider(getWebSession());
            List<StakeholderRequirementEntity> listOfEntities = stakeholderRequirementProvider.getAllStakeholderRequirement(false);
            sortBySortKey(listOfEntities);
            listOfStakeholderRequirementEntities = listOfEntities;
        }
        return listOfStakeholderRequirementEntities;
    }

}
