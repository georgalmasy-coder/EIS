package com.bepa.eis.server.api.web.application.views.admin.department;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.generic.GenericXmlDocument;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

public class DepartmentMaintenanceXmlDocument extends GenericXmlDocument {

    private final Document document;
    private final Element rootElement;

    public DepartmentMaintenanceXmlDocument(WebSession webSession, String rootElementName) {
        super(webSession);

        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(false);

            this.document = documentBuilderFactory.newDocumentBuilder().newDocument();
            this.rootElement = document.createElement(rootElementName);
            this.document.appendChild(rootElement);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public Element root() {
        return rootElement;
    }

    public Element appendElement(Element parent, String elementName) {
        if (parent == null) {
            throw new IllegalArgumentException("Parent element is required.");
        }

        if (elementName == null || elementName.isBlank()) {
            throw new IllegalArgumentException("Element name is required.");
        }

        Element element = document.createElement(elementName);
        parent.appendChild(element);
        return element;
    }

    public Element appendTextElement(Element parent, String elementName, Object value) {
        Element element = appendElement(parent, elementName);

        if (value != null) {
            element.setTextContent(String.valueOf(value));
        }

        return element;
    }

    public Element appendImportedElement(Element parent, Element sourceElement) {
        if (parent == null) {
            throw new IllegalArgumentException("Parent element is required.");
        }

        if (sourceElement == null) {
            throw new IllegalArgumentException("Source element is required.");
        }

        Node importedNode = document.importNode(sourceElement, true);
        parent.appendChild(importedNode);
        return (Element) importedNode;
    }

    @Override
    public String toXmlString(boolean prettyPrint) throws ParserConfigurationException, TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        if (prettyPrint) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        }

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }

    @Override
    public String toXmlString() throws ParserConfigurationException, TransformerException {
        return toXmlString(true);
    }
}
