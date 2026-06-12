package com.bepa.eis.server.api.web.application.views.common;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;

public class EntityAttachment extends ListOfElements {

    ListOfElements entityAttachmentElements = new ListOfElements(getWebSession(), this.getClass().getSimpleName());

    public EntityAttachment(WebSession webSession) {
        super(webSession);
        super.setElementName(this.getClass().getSimpleName());
    }

    public ListOfElements getEntityAttachmentsElements() {
        return entityAttachmentElements;
    }

}
