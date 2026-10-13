package com.bepa.eis.server.api.web.application.views.common;

import com.bepa.eis.server.api.web.application.views.pro.interfacematrix.InterfaceMatrixProvider.EditInterfaceRecord;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Standalone regression test for the marked N2 example. */
public final class EditInterfaceElementsTest {
    public static void main(String[] args) throws Exception {
        var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element left = doc.createElement("leftInterfaces");
        Element right = doc.createElement("rightInterfaces");
        // Database IDs deliberately differ from matrix order. All four records originate in the same row.
        Map<Integer, Integer> positions = Map.of(90, 0, 40, 1, 30, 2, 20, 3, 10, 4);
        var records = new ArrayList<>(List.of(record(10, "2", "E_, AA"), record(90, "7", "AA, AB"),
                record(20, "0", ""), record(30, "6", "BN, BO, BP"), record(40, "9", "self"),
                record(999, "9", "not in matrix")));
        EditInterfaceElements.appendInterfaceRows(doc, left, right, 40, records, positions);
        check(left.getChildNodes().getLength() == 1, "Only the red interface belongs above");
        check(right.getChildNodes().getLength() == 3, "The three green interfaces belong below");
        check(text((Element) left.getFirstChild(), "toEntityId").equals("90"), "Left target");
        for (int index = 0; index < 3; index++) {
            Element row = (Element) right.getChildNodes().item(index);
            check(text(row, "toEntityId").equals(List.of("30", "20", "10").get(index)), "Matrix order");
            check(text(row, "fromEntityId").equals("40"), "Preserve actual FROM direction");
        }
        check(text((Element) right.getChildNodes().item(1), "fromIrlId").equals("0"), "Include IRL 0");
        records.clear();
        Element emptyLeft = doc.createElement("leftInterfaces");
        Element emptyRight = doc.createElement("rightInterfaces");
        EditInterfaceElements.appendInterfaceRows(doc, emptyLeft, emptyRight, 40, records, positions);
        check(!emptyLeft.hasChildNodes() && !emptyRight.hasChildNodes(), "Empty row");
        System.out.println("N2 row partition tests passed.");
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
