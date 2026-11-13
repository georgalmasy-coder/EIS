package com.bepa.eis.server.api.web.application.views.common;

import com.bepa.eis.server.api.web.application.views.pro.interfacematrix.InterfaceMatrixProvider.EditInterfaceRecord;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Standalone regression test for outgoing/incoming interface lists. */
public final class EditInterfaceElementsTest {
    public static void main(String[] args) throws Exception {
        var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element left = doc.createElement("leftInterfaces");
        Element right = doc.createElement("rightInterfaces");
        // Direction, rather than matrix position, determines the list.
        Map<Integer, Integer> positions = Map.of(90, 0, 40, 1, 30, 2, 20, 3, 10, 4);
        var records = new ArrayList<>(List.of(record(10, "2", "E_, AA"), record(90, "7", "AA, AB"),
                record(20, "0", ""), record(30, "6", "BN, BO, BP"), record(40, "9", "self"),
                record(999, "9", "not in matrix"),
                new EditInterfaceRecord(90, 40, "source-90", "Source 90", 3, 7,
                        "2026-11-12", "incoming", "outgoing", 8, 4),
                new EditInterfaceRecord(10, 40, "source-10", "Source 10", 5, null,
                        "2026-11-13", "incoming-only", "", 6, 4)));
        EditInterfaceElements.appendInterfaceRows(doc, left, right, 40, records, positions);
        check(left.getChildNodes().getLength() == 4, "All outgoing interfaces belong above");
        check(right.getChildNodes().getLength() == 2, "All incoming interfaces belong below");
        for (int index = 0; index < 4; index++) {
            Element row = (Element) left.getChildNodes().item(index);
            check(text(row, "toEntityId").equals(List.of("90", "30", "20", "10").get(index)), "Matrix order");
            check(text(row, "fromEntityId").equals("40"), "Preserve actual FROM direction");
        }
        check(text((Element) left.getChildNodes().item(2), "fromIrlId").equals("0"), "Include IRL 0");
        Element incoming = (Element) right.getFirstChild();
        check(text(incoming, "fromEntityId").equals("90"), "Incoming source");
        check(text(incoming, "toEntityId").equals("40"), "Incoming target is current entity");
        check(text(incoming, "fromEntityCode").equals("source-90"), "Incoming source code");
        check(text(incoming, "fromEntityName").equals("Source 90"), "Incoming source name");
        check(text(incoming, "fromIrlId").equals("3"), "Incoming IRL, not reverse IRL");
        check(text(incoming, "fromClassificationIds").equals("incoming"), "Incoming classification");
        check(text(incoming, "fromTrlId").equals("8"), "Incoming source TRL");
        check(text(incoming, "toTrlId").equals("4"), "Incoming target TRL");
        records.clear();
        Element emptyLeft = doc.createElement("leftInterfaces");
        Element emptyRight = doc.createElement("rightInterfaces");
        EditInterfaceElements.appendInterfaceRows(doc, emptyLeft, emptyRight, 40, records, positions);
        check(!emptyLeft.hasChildNodes() && !emptyRight.hasChildNodes(), "Empty row");
        System.out.println("Outgoing/incoming interface tests passed.");
    }

    private static EditInterfaceRecord record(int targetId, String irlId, String classifications) {
        return new EditInterfaceRecord(40, targetId, "code-" + targetId, "name-" + targetId,
                Integer.valueOf(irlId), null, "2026-10-01", classifications, "", 4, 8);
    }

    private static String text(Element row, String name) {
        return row.getElementsByTagName(name).item(0).getTextContent();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
