package com.bepa.eis.server.dataprovider.entities;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;

public class Entity extends ListOfElements {

    ListOfElements entityElements = null;
    private String sortKey;

    public Entity(WebSession webSession) {
        super(webSession);
        super.setElementName(this.getClass().getSimpleName());
    }

    public Entity(WebSession webSession, String elementName) {
        super(webSession);
        super.setElementName(elementName);
    }

    public ListOfElements getEntitiesElements() {
        if (entityElements == null) {
            entityElements = new ListOfElements(getWebSession(), this.getClass().getSimpleName());
        }

        return entityElements;
    }

    public String getSortKey() {
        return sortKey;
    }

    public void setSortKey(String sortKey) {
        this.sortKey = sortKey;
    }

}
