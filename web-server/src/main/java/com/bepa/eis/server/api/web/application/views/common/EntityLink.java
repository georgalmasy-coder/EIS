package com.bepa.eis.server.api.web.application.views.common;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;

public class EntityLink extends ListOfElements {

    private ListOfElements entityLinksElements = null;

    public EntityLink(WebSession webSession) {
        super(webSession);
        super.setElementName(this.getClass().getSimpleName());
    }

    public ListOfElements getEntityLinksElements() {
        if (entityLinksElements == null) {
            entityLinksElements = new ListOfElements(getWebSession(), this.getClass().getSimpleName());
        }
        return entityLinksElements;
    }

}
