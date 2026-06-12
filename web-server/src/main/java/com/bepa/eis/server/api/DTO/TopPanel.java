package com.bepa.eis.server.api.DTO;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;

public class TopPanel {

    private ListOfElements topPanelElements = null;

    private WebSession webSession;

    public TopPanel(WebSession webSession) {
        this.webSession = webSession;
    }

    public ListOfElements getTopPanelElements() {
        if (topPanelElements == null) {
            topPanelElements = new ListOfElements(webSession, this.getClass().getSimpleName());
        }
        return topPanelElements;
    }

}
