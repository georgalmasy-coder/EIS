package com.bepa.eis.server.api.web.application.views.pro.dashboard.irlmonitor;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

public class DashboardIrlMetaData {

    private final List<MetaTrlLookup> trlList = new ArrayList<>() ;
    private final List<MetaIrlLookup> irlList = new ArrayList<>() ;

    protected DashboardIrlMetaData(WebSession webSession) {
        loadTrlLookup(webSession);
        loadIrlLookup(webSession);
    }

    private void loadTrlLookup(WebSession webSession) {
        List<LookupValue> lookupValues = CustomerLookupCache.getTrlLookupValues(webSession);
        for (LookupValue lookupValue : lookupValues) {
            if (lookupValue.isActive()) {
                trlList.add(new MetaTrlLookup(lookupValue.getLookupId(), lookupValue.getLookupCode(), lookupValue.getLookupDescription(), lookupValue.getLookupColor()));
            }
        }
    }

    private void loadIrlLookup(WebSession webSession) {
        List<LookupValue> lookupValues = CustomerLookupCache.getIrlLookupValues(webSession);
        for (LookupValue lookupValue : lookupValues) {
            if (lookupValue.isActive() && lookupValue.getLookupId() >= 3) {
                irlList.add(new MetaIrlLookup(lookupValue.getLookupId(), lookupValue.getLookupCode(), lookupValue.getLookupDescription(), lookupValue.getLookupColor()));
            }
        }
    }

    protected Element getTrlElement(Document doc) {
        Element trlsElement = doc.createElement("trlMeta");
        for (MetaTrlLookup trl : trlList) {
            addTrlLookupElement(doc, trlsElement, trl);
        }
        return trlsElement;
    }

    protected Element getIrlElement(Document doc) {
        Element irlsElement = doc.createElement("irlMeta");
        for (MetaIrlLookup irl : irlList) {
            addIrlLookupElement(doc, irlsElement, irl);
        }
        return irlsElement;
    }

    private void addTrlLookupElement(Document doc, Element parentElement, MetaTrlLookup metaDataLookup) {
        Element trlElement = doc.createElement("trl");
        addLookupAttribute(trlElement, "trlId", metaDataLookup.id.toString());
        addLookupAttribute(trlElement, "code", metaDataLookup.code);
        addLookupAttribute(trlElement, "description", metaDataLookup.description);
        addLookupAttribute(trlElement, "color", metaDataLookup.color);
        parentElement.appendChild(trlElement);
    }

    private void addIrlLookupElement(Document doc, Element parentElement, MetaIrlLookup metaDataLookup) {
        Element trlElement = doc.createElement("irl");
        addLookupAttribute(trlElement, "irlId", metaDataLookup.id.toString());
        addLookupAttribute(trlElement, "code", metaDataLookup.code);
        addLookupAttribute(trlElement, "description", metaDataLookup.description);
        addLookupAttribute(trlElement, "color", metaDataLookup.color);
        parentElement.appendChild(trlElement);
    }

    private void addLookupAttribute(Element lookupElement, String name, String value) {
        if (value != null) {
            lookupElement.setAttribute(name, value);
        }
    }

    private static class MetaTrlLookup {
        Integer id;
        String code;
        String description;
        String color;
        public MetaTrlLookup(Integer id, String code, String description, String color) {
            this.id = id != null ? id : 0;
            this.code = code != null && !code.isEmpty() ? code.substring(0, 1) : "?";
            this.description = code;
            this.color = color;
        }
    }

    private static class MetaIrlLookup {
        Integer id;
        String code;
        String description;
        String color;
        public MetaIrlLookup(Integer id, String code, String description, String color) {
            this.id = id != null ? id : 0;
            this.code = code != null && !code.isEmpty() ? code.substring(0, 1) : "?";;
            this.description = code;
            this.color = color;
        }
    }


}


