package com.bepa.eis.server.api.web.application.views.common;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;

public class EntityRelations extends ListOfElements {

    public EntityRelations(WebSession webSession) {
        super(webSession);
        setElementName(this.getClass().getSimpleName());
    }

    public EntityRelations(WebSession webSession, String elementName) {
        super(webSession, elementName);
    }

    public EntityRelations getEntityRelationElements() {
        return this;
    }

    public EntityRelation getNewEntityRelation() {
        return new EntityRelation(getWebSession());
    }

    public void addEntityRelation(EntityRelation entityRelation) {
        this.addElement(entityRelation);
    }

}
