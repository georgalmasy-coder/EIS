package com.bepa.eis.server.api.web.application.settings;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.*;

/** Immutable XML snapshot: cached documents are never modified by an editor. */
public final class CustomerSettings {
    private final String xml;

    public CustomerSettings(String xml) {
        this.xml = xml == null || xml.isBlank() ? "<settings/>" : xml;
        if (!parse().getDocumentElement().getTagName().equals("settings")) {
            throw new IllegalArgumentException("Expected settings XML");
        }
    }

    public Map<String, String> values(int customerId, Integer projectId) {
        Document document = parse();
        Map<String, String> result = new LinkedHashMap<>();
        for (SettingDefinition definition : SettingDefinition.ALL) {
            Integer id = definition.scope().equals("customer") ? Integer.valueOf(customerId) : projectId;
            Element scope = id == null ? null : scope(document, definition.scope(), id, false);
            Element group = child(scope, definition.group());
            Element value = child(group, definition.key());
            String effective = definition.defaultValue();
            if (value != null) {
                try { effective = definition.validate(value.getTextContent()); }
                catch (IllegalArgumentException ignored) { /* Same fallback as GlobalConfiguration. */ }
            }
            result.put(definition.key(), effective);
        }
        return Collections.unmodifiableMap(result);
    }

    public String getCustomerMfaPolicy(int customerId) {
        return values(customerId, null).get("mfaPolicy");
    }

    public TraceabilityMatching getTraceabilityMatching(int customerId, int projectId) {
        Map<String, String> values = values(customerId, projectId);
        int shortLength = Integer.parseInt(values.get("shortWordMaxLength"));
        return new TraceabilityMatching(Boolean.parseBoolean(values.get("fuzzyEnabled")),
                Integer.parseInt(values.get("minimumMatchingWords")), shortLength,
                Math.max(shortLength, Integer.parseInt(values.get("mediumWordMaxLength"))),
                Integer.parseInt(values.get("mediumWordMaxEdits")), Integer.parseInt(values.get("longWordMaxEdits")), values.get("stopwords"));
    }

    public record TraceabilityMatching(boolean fuzzyEnabled, int minimumMatchingWords, int shortWordMaxLength,
                                       int mediumWordMaxLength, int mediumWordMaxEdits, int longWordMaxEdits, String stopwords) { }

    public CustomerSettings update(int customerId, Integer projectId, Map<String, String> values) {
        Map<String, String> validated = new LinkedHashMap<>();
        for (var entry : values.entrySet()) {
            SettingDefinition definition = SettingDefinition.ALL.stream().filter(d -> d.key().equals(entry.getKey())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown setting: " + entry.getKey()));
            if (definition.scope().equals("project") && projectId == null) throw new IllegalArgumentException("Select a project first");
            validated.put(entry.getKey(), definition.validate(entry.getValue()));
        }
        Map<String, String> effective = new LinkedHashMap<>(values(customerId, projectId));
        effective.putAll(validated);
        if (Integer.parseInt(effective.get("mediumWordMaxLength")) < Integer.parseInt(effective.get("shortWordMaxLength"))) {
            throw new IllegalArgumentException("Medium word maximum length must be at least the short word maximum length");
        }
        Document document = parse();
        for (SettingDefinition definition : SettingDefinition.ALL) {
            if (!validated.containsKey(definition.key())) continue;
            int id = definition.scope().equals("customer") ? customerId : projectId;
            Element scope = scope(document, definition.scope(), id, true);
            Element group = getOrCreate(document, scope, definition.group());
            getOrCreate(document, group, definition.key()).setTextContent(validated.get(definition.key()));
        }
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            var transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return new CustomerSettings(writer.toString());
        } catch (TransformerException e) { throw new IllegalStateException("Could not serialize settings", e); }
    }

    public String toXml() { return xml; }

    private Document parse() {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) { throw new IllegalStateException("Could not parse settings XML", e); }
    }

    private static Element scope(Document document, String name, int id, boolean create) {
        Element root = document.getDocumentElement();
        for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && element.getTagName().equals(name) && element.getAttribute("id").equals(Integer.toString(id))) return element;
        }
        if (!create) return null;
        Element element = document.createElement(name);
        element.setAttribute("id", Integer.toString(id));
        root.appendChild(element);
        return element;
    }

    private static Element child(Element parent, String name) {
        if (parent == null) return null;
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && element.getTagName().equals(name)) return element;
        }
        return null;
    }

    private static Element getOrCreate(Document document, Element parent, String name) {
        Element element = child(parent, name);
        if (element == null) { element = document.createElement(name); parent.appendChild(element); }
        return element;
    }
}
