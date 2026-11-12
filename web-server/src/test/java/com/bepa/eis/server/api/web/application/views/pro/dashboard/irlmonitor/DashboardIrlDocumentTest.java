package com.bepa.eis.server.api.web.application.views.pro.dashboard.irlmonitor;

import com.bepa.eis.server.api.web.application.views.pro.interfacematrix.InterfaceMatrixProvider.InterfaceRecord;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Standalone regression test; no database or application server required. */
public final class DashboardIrlDocumentTest {
    public static void main(String[] args) {
        var first = new DashboardIrlDocument.PhysicalStructureWrapper(null);
        var second = new DashboardIrlDocument.PhysicalStructureWrapper(null);
        Map<Integer, DashboardIrlDocument.PhysicalStructureWrapper> systems = new HashMap<>();
        systems.put(10, first);
        systems.put(20, second);

        DashboardIrlDocument.calculateIrlCounts(systems, List.of(
                record(10, 20, 3),       // Both endpoints exist.
                record(10, 99, 4),       // Missing destination: original failure.
                record(99, 20, 5),       // Missing source must also be handled.
                record(98, 99, 6),       // Both endpoints missing.
                record(10, null, 7),     // Null destination.
                record(null, 20, 8),     // Null source.
                record(10, 10, 9)));     // Preserve two endpoint counts for self interfaces.

        check(first.getIrlCounts().get(3) == 1, "Valid source counted");
        check(second.getIrlCounts().get(3) == 1, "Valid destination counted");
        check(first.getIrlCounts().get(4) == 1, "Source counted despite missing destination");
        check(second.getIrlCounts().get(5) == 1, "Destination counted despite missing source");
        check(first.getIrlCounts().get(6) == null && second.getIrlCounts().get(6) == null,
                "Missing endpoints do not contribute counts");
        check(first.getIrlCounts().get(7) == 1, "Null destination handled");
        check(second.getIrlCounts().get(8) == 1, "Null source handled");
        check(first.getIrlCounts().get(9) == 2, "Self-interface behavior unchanged");
        DashboardIrlDocument.calculateIrlCounts(new HashMap<>(), List.of(record(10, 20, 3)));
        DashboardIrlDocument.calculateIrlCounts(systems, List.of());
        System.out.println("Dashboard IRL endpoint regression tests passed.");
    }

    private static InterfaceRecord record(Integer from, Integer to, Integer irl) {
        return new InterfaceRecord(1, 1, 1, null, 1, true, 1, null, from, to, irl, null, null);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
