package com.bepa.eis.server.dataprovider.entities;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Entities  {

    private String elementName;
    private final List<Entity> listOfEntities = new ArrayList<>();
    private WebSession webSession;

    public Entities(WebSession webSession, String elementName) {
        setElementName(webSession, elementName);
    }

    private void setElementName(WebSession webSession, String elementName) {
        this.webSession = webSession;
        this.elementName = elementName;
    }


    public WebSession getWebSession() {
        return webSession;
    }

    public Entities getEntityElements() {
        return this;
    }

    public ListOfElements getListOfEntities() {

        ListOfElements list = new ListOfElements(getWebSession(), elementName);

        for (Entity entity :listOfEntities) {
            list.addElement(entity);
        }

        return list;
    }

    public Entity getNewEntity(String elementName) {
        return new Entity(getWebSession(), elementName);
    }

    public void addEntity(Entity entity) {
        listOfEntities.add(entity);
    }

    public void sortBy(Comparator<Entity> comparator) {
        listOfEntities.sort(comparator);
    }
}
