package com.bepa.eis.server.api.web.application.views.basis.traceability;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

public class MatrixStylesData {

    private final List<Style> styleList = new ArrayList<>() ;

    protected MatrixStylesData() {
        addStyleElement("normal", "Normal", "#FFFFFF", "#000000", null, null);
        addStyleElement("red", "No relations", "#FFC7CE", "#000000", null, null);
        addStyleElement("yellow", "Possible relations", "#FFFF99", "#000000", null, null);
        addStyleElement("green", "Confirmed relations", "#9dcca8", "#000000", true, null);
        addStyleElement("grayItalic", "Not relevant", "#d4d5d6", "#000000", null, true);


    }

    private void addStyleElement(String id, String name, String backgroundColor, String textColor, Boolean bold, Boolean italic) {
        styleList.add(new Style(id, name, backgroundColor, textColor, bold, italic));
    }

    protected Element getStyleElement(Document doc) {
        Element stylesElement = doc.createElement("styles");
        for (Style style : styleList) {
            Element styleElement = doc.createElement("style");
            addStyleAttribute(styleElement, "id", style.id);
            addStyleAttribute(styleElement, "name", style.name);
            addStyleAttribute(styleElement, "backgroundColor", style.backgroundColor);
            addStyleAttribute(styleElement, "textColor", style.textColor);
            addStyleAttribute(styleElement, "bold", style.bold != null && style.bold ? "true" : null);
            addStyleAttribute(styleElement, "italic", style.italic != null && style.italic ? "true" : null);
            stylesElement.appendChild(styleElement);
        }
        return stylesElement;
    }

    private void addStyleAttribute(Element styleElement, String name, String value) {
        if (value != null) {
            styleElement.setAttribute(name, value);
        }
    }

    private static class Style {
        String id;
        String name;
        String backgroundColor;
        String textColor;
        Boolean bold;
        Boolean italic;
        public Style(String id, String name, String backgroundColor, String textColor, Boolean bold, Boolean italic) {
            this.id = id;
            this.name = name;
            this.backgroundColor = backgroundColor;
            this.textColor = textColor;
            this.bold = bold;
            this.italic = italic;
        }
    }
}
