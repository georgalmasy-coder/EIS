package com.bepa.eis.server.api.web.application.views.basis.relationdiagram;

import java.nio.file.*;
import java.util.*;
import javax.xml.stream.*;

/** Optional large-file benchmark. Produces a compact wire-format fixture without a database. */
public class RelationDiagramDatasetTest {
    public static void main(String[] args) throws Exception {
        var nodes = new ArrayList<RelationDiagramSnapshot.Node>();
        var edges = new ArrayList<RelationDiagramSnapshot.Edge>();
        var factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
        try (var input = Files.newInputStream(Path.of(args[0]))) {
            var xml = factory.createXMLStreamReader(input);
            Map<String,String> fields = null; String id = null; int type = 0, entityId = 0;
            while (xml.hasNext()) {
                int event = xml.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String tag = xml.getLocalName();
                    if (tag.equals("requirement") || tag.equals("systemsBreakdown")) {
                        fields = new HashMap<>(); id = xml.getAttributeValue(null,"id");
                        type = Integer.parseInt(xml.getAttributeValue(null,"entityType"));
                        entityId = Integer.parseInt(xml.getAttributeValue(null,"entityId"));
                    } else if (tag.equals("relation")) fields = new HashMap<>();
                    else if (fields != null && Set.of("code","name","description","from","to","type").contains(tag)) fields.put(tag,xml.getElementText());
                } else if (event == XMLStreamConstants.END_ELEMENT && fields != null) {
                    String tag = xml.getLocalName();
                    if (tag.equals("requirement") || tag.equals("systemsBreakdown")) {
                        nodes.add(new RelationDiagramSnapshot.Node(id,type,entityId,fields.get("code"),fields.get("name"),fields.get("description"))); fields = null;
                    } else if (tag.equals("relation")) {
                        edges.add(new RelationDiagramSnapshot.Edge("relation-"+edges.size(),fields.get("from"),fields.get("to"),fields.get("type"))); fields = null;
                    }
                }
            }
            xml.close();
        }
        long start = System.nanoTime();
        var graph = new RelationDiagramSnapshot(nodes,edges);
        double graphMs = (System.nanoTime()-start)/1e6;
        start = System.nanoTime(); var trace = graph.trace(nodes.get(0).internalId());
        double traceMs = (System.nanoTime()-start)/1e6;
        Path output = Path.of(args[1]); Files.createDirectories(output.toAbsolutePath().getParent());
        try (var stream = Files.newOutputStream(output)) {
            var xml = XMLOutputFactory.newFactory().createXMLStreamWriter(stream,"UTF-8");
            xml.writeStartDocument("UTF-8","1.0"); xml.writeStartElement("RelationDiagramDocument");
            xml.writeStartElement("relationDiagram"); xml.writeAttribute("snapshotId","fixture");
            int[] types = {5,6,2}; String[] groups = {"stakeholderRequirements","systemRequirements","systemsBreakdowns"};
            for(int i=0;i<types.length;i++) {
                xml.writeStartElement(groups[i]);
                for(var node:nodes) if(node.entityType()==types[i]) {
                    xml.writeStartElement(i==2?"systemsBreakdown":"requirement");
                    xml.writeAttribute("id",node.internalId()); xml.writeAttribute("entityType",String.valueOf(node.entityType()));
                    xml.writeAttribute("entityId",String.valueOf(node.entityId())); xml.writeAttribute("hasRelations",String.valueOf(graph.hasRelations(node.internalId())));
                    xml.writeStartElement("code"); xml.writeCharacters(Objects.toString(node.visibleId(),"")); xml.writeEndElement();
                    xml.writeStartElement("name"); xml.writeCharacters(Objects.toString(node.name(),"")); xml.writeEndElement(); xml.writeEndElement();
                }
                xml.writeEndElement();
            }
            xml.writeEndElement(); xml.writeEndElement(); xml.writeEndDocument(); xml.close();
        }
        if(nodes.size()!=5685 || edges.size()!=608855 || trace.relations().size()!=347) throw new AssertionError("Unexpected large fixture graph");
        System.out.printf(Locale.ROOT,"Dataset: %d nodes, %d edges; compact %d -> %d bytes; graph %.1f ms; first trace %.1f ms (%d nodes, %d edges)%n", nodes.size(),edges.size(),Files.size(Path.of(args[0])),Files.size(output),graphMs,traceMs,trace.visibleIds().size(),trace.relations().size());
    }
}
