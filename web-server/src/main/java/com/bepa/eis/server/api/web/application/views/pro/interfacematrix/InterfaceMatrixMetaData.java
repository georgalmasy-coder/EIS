package com.bepa.eis.server.api.web.application.views.pro.interfacematrix;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class InterfaceMatrixMetaData {

    private final List<MeteData> metaDataList = new ArrayList<>() ;

    protected void setTitle(String title) {
        addMetaElement("title", title);
    }

    protected void setColumnGroupLabel(String label) {
        addMetaElement("columnGroupLabel", label);
    }

    protected void setRowGroupLabel(String label) {
        addMetaElement("rowGroupLabel", label);
    }

    protected void setGeneratedAt() {
        String generatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyy-MM-dd'T'HH:mm:ss"));
        addMetaElement("generatedAt", generatedAt);
    }

    protected Element getMetaElement(Document doc) {
        Element metaElement = doc.createElement("meta");
        for (MeteData meta : metaDataList) {
            Element metaDataElement = doc.createElement(meta.name);
            metaDataElement.setTextContent(meta.value);
            metaElement.appendChild(metaDataElement);
        }
        return metaElement;
    }

    private void addMetaElement(String name, String value) {
        metaDataList.add(new MeteData(name, value));
    }

    private static class MeteData {
        String name;
        String value;
        public MeteData(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }
}
