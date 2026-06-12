package com.bepa.eis.server.api.web.application.views.common;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;

public class EntityNote extends ListOfElements {

    private ListOfElements entityNotesElements = null;

    public EntityNote(WebSession webSession) {
        super(webSession);
        super.setElementName(this.getClass().getSimpleName());
    }

    public ListOfElements getEntityNotesElements() {
        if (entityNotesElements == null) {
            entityNotesElements = new ListOfElements(getWebSession(), this.getClass().getSimpleName());
        }
        return entityNotesElements;
    }

}
