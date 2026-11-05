package com.bepa.eis.server.api.web.application.views.pro.dashboard.systemsteamwork;

import com.bepa.eis.server.api.web.application.views.pro.interfacematrix.InterfaceMatrixProvider.InterfaceRecord;
import com.bepa.eis.server.entites.systembreakdown.SystemBreakdownEntity;
import org.w3c.dom.Element;

import java.util.List;
import java.util.ArrayList;

/** Standalone regression test; no database required. */
public class DashboardSystemsTeamworkDocumentTest {
    public static void main(String[] args) throws Exception {
        var first = new SystemBreakdownEntity(null);
        first.setEntityId(1);
        var second = new SystemBreakdownEntity(null);
        second.setEntityId(2);
        var document = new DashboardSystemsTeamworkDocument(null, List.of(first, second), List.of(
                record(1, 99, 3), record(99, 1, 4), record(98, 99, 5),
                record(1, 2, 6), record(2, 1, 7)));
        var rows = document.getDoc().getElementsByTagName("interface");
        check(rows.getLength() == 1, "Skip absent endpoints; merge valid reverse pair");
        Element row = (Element) rows.item(0);
        check(text(row, "fromEntityId").equals("1") && text(row, "toEntityId").equals("2"),
                "Keep both valid endpoint IDs");
        check(text(row, "fromIrlId").equals("6") && text(row, "toIrlId").equals("7"),
                "Preserve both directional IRLs");
        var empty = new DashboardSystemsTeamworkDocument(null, List.of(), List.of(record(1, 2, 3)));
        check(empty.getDoc().getElementsByTagName("interface").getLength() == 0,
                "No active systems must produce an empty dashboard");
        testLargeInterfaceSet();
        System.out.println("Systems teamwork: missing endpoints, directional merging, order and large dataset passed");
    }

    private static void testLargeInterfaceSet() throws Exception {
        var systems = new ArrayList<SystemBreakdownEntity>();
        var interfaces = new ArrayList<InterfaceRecord>();
        for (int id = 1; id <= 500; id++) {
            var system = new SystemBreakdownEntity(null);
            system.setEntityId(id);
            systems.add(system);
        }
        // All forward records first: exercise the formerly expensive growing-list search.
        for (int from = 1; from <= 500; from++)
            for (int to = from + 1; to <= Math.min(500, from + 40); to++)
                interfaces.add(record(from, to, 6));
        int pairs = interfaces.size();
        for (int i = 0; i < pairs; i++) {
            var forward = interfaces.get(i);
            interfaces.add(record(forward.toEntityId(), forward.fromEntityId(), 7));
        }
        long start = System.nanoTime();
        var document = new DashboardSystemsTeamworkDocument(null, systems, interfaces);
        var rows = document.getDoc().getElementsByTagName("interface");
        check(rows.getLength() == pairs, "One row per bidirectional pair");
        for (int i = 0; i < pairs; i++) {
            Element row = (Element) rows.item(i);
            var original = interfaces.get(i);
            check(text(row, "fromEntityId").equals(original.fromEntityId().toString())
                    && text(row, "toEntityId").equals(original.toEntityId().toString()), "Preserve input order");
            check(text(row, "fromIrlId").equals("6") && text(row, "toIrlId").equals("7"),
                    "Merge reverse values into original row");
        }
        System.out.println("Teamwork: " + interfaces.size() + " directional interfaces, " + pairs
                + " rows; build and verification " + (System.nanoTime() - start) / 1_000_000 + " ms");
    }

    private static InterfaceRecord record(int from, int to, int irl) {
        return new InterfaceRecord(1, 1, 1, 1, 1, true, 1, null, from, to, irl, null, null);
    }

    private static String text(Element row, String name) {
        return row.getElementsByTagName(name).item(0).getTextContent();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
