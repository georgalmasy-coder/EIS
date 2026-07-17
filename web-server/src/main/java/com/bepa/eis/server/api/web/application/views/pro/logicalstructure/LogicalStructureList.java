package com.bepa.eis.server.api.web.application.views.pro.logicalstructure;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.DTO.TopPanel;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import com.bepa.eis.server.api.web.application.views.common.TopPanelProvider;
import com.bepa.eis.server.dataprovider.entities.Entities;
import com.bepa.eis.server.dataprovider.entities.LogicalStructureProvider;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;

public class LogicalStructureList extends GenericXmlDocument {

    private final ListOfElements rootElement;
    private final TopPanel topPanel;

    public LogicalStructureList(WebSession webSession) throws Exception {

        super(webSession);

        rootElement = initXmlDocument(this.getClass().getSimpleName());

        TopPanelProvider topPanelProvider = new TopPanelProvider(webSession);
        topPanel = topPanelProvider.getTopPanelBySession();
        rootElement.addElement(topPanel.getTopPanelElements());

        LogicalStructureProvider logicalStructureProvider = new LogicalStructureProvider(webSession);
        Entities functionalStructures = logicalStructureProvider.getListOfLogicalStructures();
        rootElement.addElement(functionalStructures.getListOfEntities());
    }

}
