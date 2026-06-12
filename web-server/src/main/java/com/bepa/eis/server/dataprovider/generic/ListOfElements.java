package com.bepa.eis.server.dataprovider.generic;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.enums.FieldControl;
import com.bepa.eis.server.dataprovider.fields.AbstractField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ListOfElements extends AbstractField {

    private static final Logger log = LoggerFactory.getLogger(ListOfElements.class);
    private String elementName;
    private final List<AbstractField> elements;

    private final WebSession webSession;

    public ListOfElements(WebSession webSession) {
        this.webSession = webSession;
        this.elementName = "UNKNOWN";
        this.elements = new ArrayList<>();
    }

    public ListOfElements(WebSession webSession, String elementName) {
        this.webSession = webSession;
        this.elementName = elementName;
        this.elements = new ArrayList<>();
    }

    public WebSession getWebSession() {
        return webSession;
    }

    public void setElementName(String elementName) {
        this.elementName = elementName;
    }

    public void addElement(AbstractField element) {

        if (element != null) {
            elements.add(element);
        } else {
            log.error("Element is null - looks spooky");
        }
    }

    public String getElementName() {
        return elementName;
    }

    public List<AbstractField> getElements() {
        return elements;
    }

    @Override
    public String getFieldName() {
        return elementName;
    }

    @Override
    public String getFieldLabelName() {
        return "";
    }

    @Override
    public String getFieldHeaderName() {
        return "";
    }

    @Override
    public String toString() {
        return "";
    }

    @Override
    public FieldControl getFieldControl() {
        return null;
    }

    @Override
    public Integer getFieldMinLength() {
        return null;
    }

    @Override
    public Integer getFieldMaxLength() {
        return null;
    }

    @Override
    public Integer getFieldDisplayLength() {
        return null;
    }

    @Override
    public Integer getFieldRow() {
        return null;
    }

    @Override
    public Integer getFieldCol() {
        return null;
    }
}

