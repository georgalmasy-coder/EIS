package com.bepa.eis.server.api.web.application.views.common;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;

public class EntityNotes extends ListOfElements {

    public EntityNotes(WebSession webSession) {
        super(webSession);
        setElementName(this.getClass().getSimpleName());
    }

    public EntityNotes(WebSession webSession, String elementName) {
        super(webSession, elementName);
    }

    public EntityNotes getEntityNoteElements() {
        return this;
    }

    public EntityNote getNewEntityNote() {
        return new EntityNote(getWebSession());
    }

    public void addEntityNote(EntityNote entityNote) {
        this.addElement(entityNote);
    }

}
