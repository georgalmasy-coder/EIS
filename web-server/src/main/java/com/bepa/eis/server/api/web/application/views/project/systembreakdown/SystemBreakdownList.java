package com.bepa.eis.server.api.web.application.views.project.systembreakdown;

import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.dataprovider.entities.Entities;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;
import com.bepa.eis.server.dataprovider.entities.SystemBreakdownProvider;

public class SystemBreakdownList extends GenericXmlDocument {

    private final ListOfElements rootElement;
    private final TopPanel topPanel;
    private Entities systemBreakdowns;

    public SystemBreakdownList(WebSession webSession) throws Exception {

        super(webSession);

        rootElement = initXmlDocument(this.getClass().getSimpleName());

        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        topPanel = topPanelProvider.getTopPanelBySession();
        rootElement.addElement(topPanel.getTopPanelElements());

        SystemBreakdownProvider systemBreakdownProvider = new SystemBreakdownProvider(webSession);
        Entities systemBreakdowns = systemBreakdownProvider.getListOfSystemsBreakdown();
        rootElement.addElement(systemBreakdowns.getListOfEntities());
    }

}
