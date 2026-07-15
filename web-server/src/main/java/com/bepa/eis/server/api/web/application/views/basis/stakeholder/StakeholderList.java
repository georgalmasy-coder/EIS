package com.bepa.eis.server.api.web.application.views.basis.stakeholder;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.dataprovider.entities.Entities;
import com.bepa.eis.server.dataprovider.entities.StakeholderProvider;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;

public class StakeholderList extends GenericXmlDocument {

    private final ListOfElements rootElement;
    private final TopPanel topPanel;

    public StakeholderList(WebSession webSession) throws Exception {

        super(webSession);

        rootElement = initXmlDocument(this.getClass().getSimpleName());

        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        topPanel = topPanelProvider.getTopPanelBySession();
        rootElement.addElement(topPanel.getTopPanelElements());

        StakeholderProvider stakeholderProvider = new StakeholderProvider(webSession);
        Entities stakeholderEntities = stakeholderProvider.getListOfStakeholders();
        rootElement.addElement(stakeholderEntities.getListOfEntities());
    }

}
