package com.bepa.eis.server.api.generic;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.fields.AbstractField;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import com.bepa.eis.server.dataprovider.generic.Attribute;
import com.bepa.eis.server.dataprovider.generic.AttributeElement;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

public class GenericXmlDocument {

    private static final Logger log = LoggerFactory.getLogger(GenericXmlDocument.class);

    private final DocumentBuilderFactory dbf;
    private final DocumentBuilder db;
    private final Document doc;
    private Element root;
    private WebSession webSession;

    private ListOfElements listOfElements = null;

/*
    private GenericXmlDocument() {
        try {
            dbf = DocumentBuilderFactory.newInstance();
            db = dbf.newDocumentBuilder();
            doc = db.newDocument();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

 */

    public GenericXmlDocument(WebSession webSession) {
        try {
            this.webSession = webSession;
            dbf = DocumentBuilderFactory.newInstance();
            db = dbf.newDocumentBuilder();
            doc = db.newDocument();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public ListOfElements initXmlDocument(String nameOfRootElement) throws Exception {
        listOfElements = new ListOfElements(getWebSession(), nameOfRootElement);
        root = doc.createElement(nameOfRootElement);
        doc.appendChild(root);
        return listOfElements;
    }

    public WebSession getWebSession() {
        return webSession;
    }

    public Document getDoc() {
        return doc;
    }

    public Element getRoot() {
        return root;
    }

    /**
     * Convenience method that serializes into an XML string.
     *
     * @param prettyPrint if true, indents the output (human-readable)
     */
    public String toXmlString(boolean prettyPrint) throws ParserConfigurationException, TransformerException {

        buildXmlDocument(root, listOfElements);

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        if (prettyPrint) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            // Common Xalan property (works in the usual JDK transformer)
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        }

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));

        String xmlAsString = writer.toString();
        log.info("Created XML : {}", xmlAsString);

        return xmlAsString;
    }

    public String toXmlString() throws ParserConfigurationException, TransformerException {
        return toXmlString(true);
    }

    private void buildXmlDocument(Element rootElement, ListOfElements elements) {
        if (rootElement == null || elements == null) {

            try {
                initXmlDocument("empty");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return;
        }

        for (AbstractField field : elements.getElements()) {

            //log.debug("Field: {}", field.getFieldName());

            if (field instanceof ListOfElements subElements) {
                Element element = doc.createElement(field.getFieldName());
                rootElement.appendChild(element);

                buildXmlDocument(element, subElements);
            } else if (field instanceof AttributeElement) {

                Element element = doc.createElement(field.getFieldName());
                rootElement.appendChild(element);
                for (Attribute attribute : field.getAttributes()) {
                    element.setAttribute(attribute.getName(), attribute.getValue());
                }


            } else {
                Element element = doc.createElement(field.getFieldName());

                addAttribute(element, "control", field.getFieldControl().getDescription());
                addAttribute(element, "visible", field.getFieldVisibleAsString());

                if (field.isFieldVisible()) {
                    addAttribute(element, "label", field.getFieldLabelName());
                    addAttribute(element, "header", field.isFieldEditable() ? field.getFieldLabelName() : field.getFieldHeaderName());

                    addAttribute(element, "editable", field.getFieldEditableAsString());
                    addAttribute(element, "required", field.getFieldRequiredAsString());

                    addAttribute(element, "minlength", field.getFieldMinLength() != null ? field.getFieldMinLength().toString() : null );
                    addAttribute(element, "maxlength", field.getFieldMaxLength() != null ? field.getFieldMaxLength().toString() : null );
                    addAttribute(element, "size", field.getFieldDisplayLength() != null ? field.getFieldDisplayLength().toString() : null );

                    if (field.isFieldVisible() && field.getTableWidth() != null) {
                        addAttribute(element, "tableWidth", field.getTableWidth());
                    }

                    addAttribute(element, "rows", field.getFieldRow() != null ? field.getFieldRow().toString() : null );
                    addAttribute(element, "cols", field.getFieldCol() != null ? field.getFieldCol().toString() : null );
                }

                rootElement.appendChild(element);

                if (field instanceof AbstractLookup lookup) {

                    lookup.setWebSession(getWebSession());
                    if (field.isFieldEditable()) {
                        lookup.addAllActiveOptions(getWebSession());
                    } else {
                        lookup.addCurrentOptions(getWebSession());
                    }

                    Element selectValueElement = doc.createElement("Value");

                    selectValueElement.setTextContent(lookup.getLookupIdAsString());
                    element.appendChild(selectValueElement);

                    lookup.getOptions().forEach(option -> {
                        Element optionElement = doc.createElement("Option");
                        optionElement.setTextContent(option.getLabel());
                        optionElement.setAttribute("value", option.getValue() != null ? option.getValue().toString() : "" );
                        if (option.isSelected()) {
                            optionElement.setAttribute("selected", "true");
                        }
                        element.appendChild(optionElement);
                    });

                } else {
                    element.setTextContent(String.valueOf(field.toString()));
                }
            }

        }
    }

    private void addAttribute(Element element, String attributeName, String attributeValue) {
        if (element != null && attributeName != null && attributeValue != null) {
            element.setAttribute(attributeName, attributeValue);
        }
    }

}
