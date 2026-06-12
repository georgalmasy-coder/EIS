package com.bepa.eis.server.api.web.application.views.common;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;

public class EntityAttachments extends ListOfElements {

    public EntityAttachments(WebSession webSession) {
        super(webSession);
        setElementName(this.getClass().getSimpleName());
    }

    public EntityAttachments(WebSession webSession, String elementName) {
        super(webSession, elementName);
    }

    public EntityAttachments getEntityAttachmentElements() {
        return this;
    }

    public EntityAttachment getNewEntityAttachment() {
        return new EntityAttachment(getWebSession());
    }

    public void addEntityAttachment(EntityAttachment entityAttachment) {
        this.addElement(entityAttachment);
    }

}
