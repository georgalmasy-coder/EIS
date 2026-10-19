package com.bepa.eis.server.api.web.application.settings;

import java.util.Map;

/** Standalone regression suite; run main after test-compile. */
public final class CustomerSettingsTest {
    public static void main(String[] args) {
        CustomerSettings empty = new CustomerSettings(null);
        var defaults = empty.getTraceabilityMatching(1, 2);
        check(defaults.fuzzyEnabled() && defaults.minimumMatchingWords() == 2, "Missing XML defaults");
        check(defaults.shortWordMaxLength() == 4 && defaults.mediumWordMaxLength() == 8
                && defaults.mediumWordMaxEdits() == 1 && defaults.longWordMaxEdits() == 2, "Numeric defaults");
        check(defaults.stopwords().contains("på"), "Unicode default stopwords");
        check(empty.getCustomerMfaPolicy(1).equals("OPTIONAL"), "Customer default");

        CustomerSettings original = new CustomerSettings("""
                <settings><customer id="1"><security><mfaPolicy>REQUIRED</mfaPolicy></security><future>keep</future></customer>
                <project id="1"><traceabilityMatching><minimumMatchingWords>7</minimumMatchingWords></traceabilityMatching></project>
                <project id="2"><traceabilityMatching><stopwords/></traceabilityMatching><future>keep</future></project></settings>
                """);
        CustomerSettings updated = original.update(1, 2, Map.of("minimumMatchingWords", "3", "mfaPolicy", "DISABLED", "stopwords", "på,&,<tag>"));
        check(updated.getTraceabilityMatching(1, 1).minimumMatchingWords() == 7, "Other projects preserved");
        check(updated.getTraceabilityMatching(1, 2).minimumMatchingWords() == 3, "Selected project updated");
        check(updated.getCustomerMfaPolicy(1).equals("DISABLED"), "Customer updated");
        check(updated.getTraceabilityMatching(1, 2).stopwords().equals("på,&,<tag>"), "XML escaping roundtrip");
        check(updated.toXml().contains("<future>keep</future>"), "Unknown settings preserved");
        check(original.getCustomerMfaPolicy(1).equals("REQUIRED"), "Cached snapshot remains immutable");
        check(original.getTraceabilityMatching(1, 2).stopwords().isEmpty(), "Empty stopwords do not fall back");
        check(updated.getTraceabilityMatching(5, 99).minimumMatchingWords() == 2, "Missing project defaults");
        CustomerSettings invalid = new CustomerSettings("""
                <settings><project id="2"><traceabilityMatching><minimumMatchingWords>oops</minimumMatchingWords>
                <longWordMaxEdits>9</longWordMaxEdits><fuzzyEnabled>invalid</fuzzyEnabled></traceabilityMatching></project></settings>
                """);
        check(invalid.getTraceabilityMatching(1, 2).minimumMatchingWords() == 2
                && invalid.getTraceabilityMatching(1, 2).longWordMaxEdits() == 2
                && invalid.getTraceabilityMatching(1, 2).fuzzyEnabled(), "Invalid persisted values use defaults");
        rejected(() -> empty.update(1, 2, Map.of("minimumMatchingWords", "0")));
        rejected(() -> empty.update(1, 2, Map.of("mediumWordMaxLength", "3")));
        rejected(() -> empty.update(1, 2, Map.of("mfaPolicy", "unknown")));
        rejected(() -> empty.update(1, null, Map.of("fuzzyEnabled", "true")));
        rejected(() -> empty.update(1, 2, Map.of("unknown", "true")));
        check(empty.update(1, null, Map.of("mfaPolicy", "REQUIRED")).getCustomerMfaPolicy(1).equals("REQUIRED"), "Customer editing without project");
        try {
            new CustomerSettings("<!DOCTYPE settings [<!ENTITY test SYSTEM 'file:///nonexistent'>]><settings>&test;</settings>");
            throw new AssertionError("DTD should be rejected");
        } catch (IllegalStateException expected) { }
        System.out.println("CustomerSettingsTest passed");
    }

    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void rejected(Runnable action) {
        try { action.run(); throw new AssertionError("Invalid change accepted"); }
        catch (IllegalArgumentException expected) { }
    }
}
