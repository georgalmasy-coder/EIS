package com.bepa.eis.server.api.web.application.views.pro.dashboard.common;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.DTO.User;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

public class DashboardMetaData {

    private final List<MetaTrlLookup> trlList = new ArrayList<>() ;
    private final List<MetaIrlLookup> irlList = new ArrayList<>() ;
    private final List<MetaClassificationLookup> classificationList = new ArrayList<>() ;
    private final List<MetaUserLookup> userList = new ArrayList<>() ;
    private final List<MetaDepartmentLookup> departmentList = new ArrayList<>() ;

    public DashboardMetaData(WebSession webSession) {
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

    private void loadClassificationLookup(WebSession webSession) {
        List<LookupValue> lookupValues = CustomerLookupCache.getClassificationLookupValues(webSession);
        for (LookupValue lookupValue : lookupValues) {
            if (lookupValue.getLookupId() >= 0) {
                classificationList.add(new MetaClassificationLookup(lookupValue.getLookupId(), lookupValue.getLookupCode(), lookupValue.getLookupDescription(), lookupValue.getLookupColor()));
            }
        }
    }

    private void loadUserLookup(WebSession webSession) {
        List<LookupValue> lookupValues = CustomerLookupCache.getUserLookupValues(webSession);
        for (LookupValue lookupValue : lookupValues) {
            if (lookupValue.getLookupId() >= 0) {
                userList.add(new MetaUserLookup(lookupValue.getLookupId(), lookupValue.getLookupCode(), lookupValue.getLookupDescription(), lookupValue.getLookupColor()));
            }
        }
    }

    private void loadDepartmentLookup(WebSession webSession) {
        List<LookupValue> lookupValues = CustomerLookupCache.getDepartmentLookupValues(webSession);
        for (LookupValue lookupValue : lookupValues) {
            if (lookupValue.getLookupId() >= 0) {
                departmentList.add(new MetaDepartmentLookup(lookupValue.getLookupId(), lookupValue.getLookupDescription(), lookupValue.getLookupCode(), lookupValue.getLookupColor()));
            }
        }
    }

    public Element getTrlElement(Document doc) {
        Element trlsElement = doc.createElement("trlMeta");
        for (MetaTrlLookup trl : trlList) {
            addTrlLookupElement(doc, trlsElement, trl);
        }
        return trlsElement;
    }

    public Element getIrlElement(Document doc) {
        Element irlsElement = doc.createElement("irlMeta");
        for (MetaIrlLookup irl : irlList) {
            addIrlLookupElement(doc, irlsElement, irl);
        }
        return irlsElement;
    }

    public Element getClassificationElement(Document doc) {
        Element claasificationsElement = doc.createElement("classificationMeta");
        for (MetaClassificationLookup classification : classificationList) {
            addClassificationLookupElement(doc, claasificationsElement, classification);
        }
        return claasificationsElement;
    }

    public Element getUserElement(Document doc) {
        Element usersElement = doc.createElement("userMeta");
        for (MetaUserLookup user : userList) {
            addUserLookupElement(doc, usersElement, user);
        }
        return usersElement;
    }

    public Element getDepartmentElement(Document doc) {
        Element departmentsElement = doc.createElement("departmentMeta");
        for (MetaDepartmentLookup department : departmentList) {
            addDepartmentLookupElement(doc, departmentsElement, department);
        }
        return departmentsElement;
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

    private void addClassificationLookupElement(Document doc, Element parentElement, MetaClassificationLookup metaDataLookup) {
        Element classElement = doc.createElement("classification");
        addLookupAttribute(classElement, "classId", metaDataLookup.id.toString());
        addLookupAttribute(classElement, "code", metaDataLookup.code);
        addLookupAttribute(classElement, "description", metaDataLookup.description);
        addLookupAttribute(classElement, "color", metaDataLookup.color);
        parentElement.appendChild(classElement);
    }

    private void addUserLookupElement(Document doc, Element parentElement, MetaUserLookup user) {
        Element classElement = doc.createElement("user");
        addLookupAttribute(classElement, "userId", user.id.toString());
        addLookupAttribute(classElement, "code", user.code);
        addLookupAttribute(classElement, "description", user.description);
        parentElement.appendChild(classElement);
    }

    private void addDepartmentLookupElement(Document doc, Element parentElement, MetaDepartmentLookup department) {
        Element classElement = doc.createElement("user");
        addLookupAttribute(classElement, "userId", department.id.toString());
        addLookupAttribute(classElement, "code", department.code);
        addLookupAttribute(classElement, "description", department.description);
        addLookupAttribute(classElement, "color", department.color);
        parentElement.appendChild(classElement);
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

    private static class MetaClassificationLookup {
        Integer id;
        String code;
        String description;
        String color;
        public MetaClassificationLookup(Integer id, String code, String description, String color) {
            this.id = id != null ? id : 0;
            this.code = code != null && !code.isEmpty() ? code : "--";;
            this.description = description != null && !description.isEmpty() ? description : "--";
            this.color = color;
        }
    }

    private static class MetaUserLookup {
        Integer id;
        String code;
        String description;
        String color;
        public MetaUserLookup(Integer id, String code, String description, String color) {
            this.id = id != null ? id : 0;
            this.code = code != null && !code.isEmpty() ? code : "--";;
            this.description = description != null && !description.isEmpty() ? description : "--";
            this.color = color;
        }
    }

    private static class MetaDepartmentLookup {
        Integer id;
        String code;
        String description;
        String color;
        public MetaDepartmentLookup(Integer id, String code, String description, String color) {
            this.id = id != null ? id : 0;
            this.code = code != null && !code.isEmpty() ? code : "--";;
            this.description = description != null && !description.isEmpty() ? description : "--";
            this.color = color;
        }
    }


}


