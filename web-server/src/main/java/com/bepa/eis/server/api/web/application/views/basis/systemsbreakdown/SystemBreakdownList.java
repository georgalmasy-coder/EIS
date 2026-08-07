package com.bepa.eis.server.api.web.application.views.basis.systemsbreakdown;

import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.enums.PageType;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.dataprovider.entities.Entities;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;
import com.bepa.eis.server.dataprovider.entities.SystemBreakdownProvider;

public class SystemBreakdownList extends GenericXmlDocument {

    private final ListOfElements rootElement;
    private final TopPanel topPanel;

    public SystemBreakdownList(WebSession webSession) throws Exception {

        super(webSession);

        rootElement = initXmlDocument(this.getClass().getSimpleName());

        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        topPanel = topPanelProvider.getTopPanelBySession(PageType.SYSTEMS_BREAKDOWN_MAIN_PAGE);
        rootElement.addElement(topPanel.getTopPanelElements());

        SystemBreakdownProvider systemBreakdownProvider = new SystemBreakdownProvider(webSession);
        Entities systemBreakdowns = systemBreakdownProvider.getListOfSystemsBreakdown();
        rootElement.addElement(systemBreakdowns.getListOfEntities());
    }

}
