package com.bepa.eis.server.api.web.application.views.basis.stakeholderrequirement;

import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.dataprovider.entities.StakeholderRequirementProvider;
import com.bepa.eis.server.dataprovider.entities.Entities;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;

public class StakeholderRequirementList extends GenericXmlDocument {

    private final ListOfElements rootElement;
    private final TopPanel topPanel;

    public StakeholderRequirementList(WebSession webSession) throws Exception {

        super(webSession);

        rootElement = initXmlDocument(this.getClass().getSimpleName());

        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        topPanel = topPanelProvider.getTopPanelBySession(PageType.STAKEHOLDER_REQUIREMENT_MAIN_PAGE);
        rootElement.addElement(topPanel.getTopPanelElements());

        StakeholderRequirementProvider requirementListProvider = new StakeholderRequirementProvider(webSession);
        Entities basisRequirement = requirementListProvider.getListOfBasisRequirements();
        rootElement.addElement(basisRequirement.getListOfEntities());
    }

}
