package com.bepa.eis.server.api.web.application.views.pro.functionalstructure;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.dataprovider.entities.Entities;
import com.bepa.eis.server.dataprovider.entities.FunctionalStructureProvider;
import com.bepa.eis.server.dataprovider.entities.StakeholderRequirementProvider;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;

public class FunctionalStructureList extends GenericXmlDocument {

    private final ListOfElements rootElement;
    private final TopPanel topPanel;

    public FunctionalStructureList(WebSession webSession) throws Exception {

        super(webSession);

        rootElement = initXmlDocument(this.getClass().getSimpleName());

        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        topPanel = topPanelProvider.getTopPanelBySession();
        rootElement.addElement(topPanel.getTopPanelElements());

        FunctionalStructureProvider functionalStructureProvider = new FunctionalStructureProvider(webSession);
        Entities basisRequirement = functionalStructureProvider.getListOfFunctionalStructures();
        rootElement.addElement(basisRequirement.getListOfEntities());
    }

}

