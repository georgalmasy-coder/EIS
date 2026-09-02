package com.bepa.eis.server.api.web.application.views.pro.interfacematrix;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.api.web.application.cache.ClassLookupValue;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MatrixLookupMetaData {

    private final List<MetaDataLookup> trlList = new ArrayList<>() ;
    private final List<MetaDataLookup> irlList = new ArrayList<>() ;
    private final List<MetaDataLookup> classificationList = new ArrayList<>() ;
    private final List<MetaDataLookup> userList = new ArrayList<>() ;
    private final List<MetaDataLookup> departmentList = new ArrayList<>() ;

    protected MatrixLookupMetaData(WebSession webSession) {
        loadTrlLookup(webSession);
        loadIrlLookup(webSession);
        loadClassificationLookup(webSession);
        loadUserLookup(webSession);
        loadDepartmentLookup(webSession);
    }

    private void loadTrlLookup(WebSession webSession) {
        List<LookupValue> lookupValues = CustomerLookupCache.getTrlLookupValues(webSession);
        for (LookupValue lookupValue : lookupValues) {
            if (lookupValue.isActive()) {
                trlList.add(new MetaDataLookup(lookupValue.getLookupId(), lookupValue.getLookupCode(), lookupValue.getLookupDescription(), lookupValue.getLookupColor()));
            }
        }
    }

    private void loadIrlLookup(WebSession webSession) {
        List<LookupValue> lookupValues = CustomerLookupCache.getIrlLookupValues(webSession);
        for (LookupValue lookupValue : lookupValues) {
            if (lookupValue.isActive()) {
                irlList.add(new MetaDataLookup(lookupValue.getLookupId(), lookupValue.getLookupCode(), lookupValue.getLookupDescription(), lookupValue.getLookupColor()));
            }
        }
    }

    private void loadClassificationLookup(WebSession webSession) {
        List<ClassLookupValue> lookupValues = CustomerLookupCache.getClassificationLookupValues(webSession);
        lookupValues.stream()
                .filter(LookupValue::isActive)
                .sorted(Comparator.comparing(
                        LookupValue::getLookupCode,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                ))
                .forEach(lookupValue -> classificationList.add(new MetaDataSrlLookup(
                        lookupValue.getLookupId(),
                        lookupValue.getLookupCode(),
                        lookupValue.getLookupDescription(),
                        lookupValue.getLookupColor(),
                        lookupValue.getExample(),
                        lookupValue.getUsageExample()
                )));
    }

    private void loadUserLookup(WebSession webSession) {
        List<LookupValue> lookupValues = CustomerLookupCache.getUserLookupValues(webSession);
        for (LookupValue lookupValue : lookupValues) {
            userList.add(new MetaDataLookup(lookupValue.getLookupId(), lookupValue.getLookupCode(), lookupValue.getLookupDescription(), lookupValue.getLookupColor()));
        }
    }

    private void loadDepartmentLookup(WebSession webSession) {
        List<LookupValue> lookupValues = CustomerLookupCache.getDepartmentLookupValues(webSession);
        for (LookupValue lookupValue : lookupValues) {
            if (lookupValue.isActive()) {
                departmentList.add(new MetaDataLookup(lookupValue.getLookupId(), lookupValue.getLookupCode(), lookupValue.getLookupDescription(), lookupValue.getLookupColor()));
            }
        }
    }

    protected Element getTrlElement(Document doc) {
        Element trlsElement = doc.createElement("trlMeta");
        for (MetaDataLookup trl : trlList) {
            addLookupElement(doc, "trl", trlsElement, "trlId", trl);
        }
        return trlsElement;
    }

    protected Element getIrlElement(Document doc) {
        Element irlsElement = doc.createElement("irlMeta");
        for (MetaDataLookup irl : irlList) {
            addLookupElement(doc, "irl", irlsElement, "irlId", irl);
        }
        return irlsElement;
    }

    protected Element getClassificationElement(Document doc) {
        Element classificationsElement = doc.createElement("classificationMeta");
        for (MetaDataLookup classification : classificationList) {
            addLookupElement(doc, "classification", classificationsElement, "classId", classification);
        }
        return classificationsElement;
    }

    protected Element getUserElement(Document doc) {
        Element usersElement = doc.createElement("userMeta");
        for (MetaDataLookup user : userList) {
            addLookupElement(doc, "user", usersElement, "userId", user);
        }
        return usersElement;
    }

    protected Element getDepartmentElement(Document doc) {
        Element departmentsElement = doc.createElement("departmentMeta");
        for (MetaDataLookup department : departmentList) {
            addLookupElement(doc, "department", departmentsElement, "departmentId", department);
        }
        return departmentsElement;
    }

    private void addLookupElement(Document doc, String elementName, Element parentElement,String ieElementName,  MetaDataLookup metaDataLookup) {
        Element trlElement = doc.createElement(elementName);
        addLookupAttribute(trlElement, ieElementName, metaDataLookup.id.toString());
        addLookupAttribute(trlElement, "code", metaDataLookup.code);
        addLookupAttribute(trlElement, "description", metaDataLookup.description);
        addLookupAttribute(trlElement, "color", metaDataLookup.color);
        if (metaDataLookup instanceof MetaDataSrlLookup classification) {
            addLookupAttribute(trlElement, "example", classification.example);
            addLookupAttribute(trlElement, "usageExample", classification.usageExample);
        }
        parentElement.appendChild(trlElement);
    }

    private void addLookupAttribute(Element lookupElement, String name, String value) {
        if (value != null) {
            lookupElement.setAttribute(name, value);
        }
    }

    private static class MetaDataLookup {
        Integer id;
        String code;
        String description;
        String color;
        public MetaDataLookup(Integer id, String code, String description, String color) {
            this.id = id != null ? id : 0;
            this.code = code;
            this.description = description;
            this.color = color;
        }
    }

    private static class MetaDataSrlLookup extends  MetaDataLookup {
        String example;
        String usageExample;

        public MetaDataSrlLookup(Integer id, String code, String description, String color, String example, String usageExample) {
            super(id, code, description, color);
            this.example = example;
            this.usageExample = usageExample;
        }
    }

}
