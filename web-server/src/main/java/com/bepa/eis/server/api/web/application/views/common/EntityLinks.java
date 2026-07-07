package com.bepa.eis.server.api.web.application.views.common;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;

public class EntityLinks extends ListOfElements {

    public EntityLinks(WebSession webSession) {
        super(webSession);
        setElementName(this.getClass().getSimpleName());
    }

    public EntityLinks(WebSession webSession, String elementName) {
        super(webSession, elementName);
    }

    public EntityLinks getEntityLinkElements() {
        return this;
    }

    public EntityLink getNewEntityLink() {
        return new EntityLink(getWebSession());
    }

    public void addEntityLink(EntityLink entityLink) {
        this.addElement(entityLink);
    }

}
