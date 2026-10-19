package com.bepa.eis.server.api.web.application.views.basis.traceability;

import com.bepa.eis.server.api.web.application.settings.CustomerSettings;
import java.util.Map;

/** Standalone regression suite; run its main method without a test framework. */
public final class RequirementTextMatcherTest {
    public static void main(String[] args) throws Exception {
        RequirementTextMatcher fuzzy = new RequirementTextMatcher(true, 2, 4, 8, 1, 2);
        check(fuzzy, "Cool chocolate", "Chocolate cooling", false);
        check(fuzzy, "system system system", "system system system", false);
        check(fuzzy, "SYSTEM, chocolate!", "system chocolate", true);
        check(fuzzy, "system chocolate", "sytem chocolatte", true);
        check(fuzzy, "køling ventilator", "kølng ventilater", true);
        check(fuzzy, "cool chocolate", "coal chocolate", false);
        check(fuzzy, "system", "systemkrav", false);
        check(fuzzy, "system sytem", "systen", false);
        check(fuzzy, null, null, false);
        // Reassignment is necessary: abcde can match both, abcdf can only match abcdg.
        check(fuzzy, "abcde abcdf", "abcdg abcxe", true);
        check(fuzzy, "abcdg abcxe", "abcde abcdf", true);
        var highlighted = fuzzy.matchingWords(RequirementTextMatcher.words("Solar Trajectory Calculation", "Cooling"),
                RequirementTextMatcher.words("Solar Panel Position Monitoring", "Chocolate cooling"));
        if (!highlighted.equals(java.util.List.of(new RequirementTextMatcher.WordMatch("solar", "solar"),
                new RequirementTextMatcher.WordMatch("cooling", "cooling")))) {
            throw new AssertionError("Dialog must return the matching words from both fields: " + highlighted);
        }
        var fuzzyPairs = fuzzy.matchingWords(RequirementTextMatcher.words("system chocolate", null),
                RequirementTextMatcher.words("sytem chocolatte", null));
        if (fuzzyPairs.size() != 2 || !fuzzyPairs.contains(new RequirementTextMatcher.WordMatch("system", "sytem"))) {
            throw new AssertionError("Fuzzy pairs must retain both spellings");
        }
        if (fuzzy.matchingWords(RequirementTextMatcher.words("system system system", null),
                RequirementTextMatcher.words("system", null)).size() != 1) {
            throw new AssertionError("Highlighting should include a single match below the yellow-cell threshold");
        }
        check(new RequirementTextMatcher(false, 2, 4, 8, 1, 2),
                "system chocolate", "sytem chocolate", false);
        check(new RequirementTextMatcher(true, 3, 4, 8, 1, 2),
                "system chocolate", "system chocolate", false);
        if (!fuzzy.matches(RequirementTextMatcher.words("system", "chocolate"),
                RequirementTextMatcher.words("chocolate", "sytem"))) {
            throw new AssertionError("Name and description must be combined");
        }
        CustomerSettings settings = new CustomerSettings(null);
        check(matcher(settings, 1), "system chocolate", "sytem chocolate", true);
        check(matcher(settings, 1), "system", "system", false);
        check(matcher(settings, 1), "the and og at på", "THE AND OG AT PÅ", false);
        check(matcher(settings.update(1, 1, Map.of("mediumWordMaxEdits", "0")), 1),
                "system chocolate", "sytem chocolate", false);
        check(matcher(settings.update(1, 1, Map.of("longWordMaxEdits", "0")), 1),
                "system chocolate", "system chocolatte", false);
        check(matcher(settings.update(1, 1, Map.of("shortWordMaxLength", "6")), 1),
                "system chocolate", "sytem chocolate", false);
        check(matcher(settings.update(1, 1, Map.of("mediumWordMaxLength", "5", "mediumWordMaxEdits", "0")), 1),
                "system chocolate", "systen chocolate", true);
        settings = settings.update(1, 2, Map.of("fuzzyEnabled", "false", "minimumMatchingWords", "1"));
        check(matcher(settings, 2), "system", "system", true);
        check(matcher(settings, 2), "system", "sytem", false);
        check(matcher(settings, 1), "system chocolate", "sytem chocolate", true);
        settings = settings.update(1, 2, Map.of("minimumMatchingWords", "2", "stopwords", "AND,solar,solar"));
        check(matcher(settings, 2), "solar and cooling", "solar and cooling", false);
        check(matcher(settings, 2), "the cooling", "the cooling", true);
        var highlights = matcher(settings, 2).matchingWords(RequirementTextMatcher.words("and solar cooling", null),
                RequirementTextMatcher.words("and solar cooling", null));
        if (highlights.size() != 1 || !highlights.get(0).source().equals("cooling")) {
            throw new AssertionError("XML stopwords must also apply to highlighted words");
        }
        settings = settings.update(1, 2, Map.of("stopwords", ""));
        check(matcher(settings, 2), "and the", "and the", true);
        settings = settings.update(1, 2, Map.of("stopwords", "på,and"));
        check(matcher(settings, 2), "på and cooling", "PÅ AND cooling", false);
        RequirementTextMatcher filtered = new RequirementTextMatcher(true, 1, 4, 8, 1, 2, "their");
        check(filtered, "their", "theirs", false);
        check(filtered, "theirs", "their", false);
        System.out.println("RequirementTextMatcher regression checks passed");
    }

    private static RequirementTextMatcher matcher(CustomerSettings settings, int projectId) {
        return new RequirementTextMatcher(settings.getTraceabilityMatching(1, projectId));
    }

    private static void check(RequirementTextMatcher matcher, String left, String right, boolean expected) {
        boolean actual = matcher.matches(RequirementTextMatcher.words(left, null),
                RequirementTextMatcher.words(right, null));
        if (actual != expected) {
            throw new AssertionError(left + " / " + right + ": expected " + expected + ", got " + actual);
        }
    }
}
