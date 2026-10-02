package com.bepa.eis.server.api.web.application.views.basis.traceability;

import com.bepa.eis.common.GlobalConfiguration;
import java.nio.file.Files;
import java.nio.file.Path;

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
        Path configuration = Files.createTempFile(Path.of("web-server/target"), "traceability-test-", ".properties");
        try {
            System.setProperty("eis.config.file", configuration.toAbsolutePath().toString());
            check(new RequirementTextMatcher(), "system chocolate", "sytem chocolate", true);
            check(new RequirementTextMatcher(), "system", "system", false);
            check(new RequirementTextMatcher(), "the and og at på", "THE AND OG AT PÅ", false);
            check(new RequirementTextMatcher(), "the cooling and chocolate", "cooling og chocolate", true);
            var stopwordHighlights = new RequirementTextMatcher().matchingWords(
                    RequirementTextMatcher.words("Solar and cooling", null),
                    RequirementTextMatcher.words("Solar and cooling", null));
            if (stopwordHighlights.size() != 2 || stopwordHighlights.stream()
                    .anyMatch(pair -> pair.source().equals("and") || pair.target().equals("and"))) {
                throw new AssertionError("Stopwords must not be highlighted");
            }
            Files.writeString(configuration, "traceability.match.fuzzy.enabled=false\n"
                    + "traceability.match.minimum-matching-words=1\n");
            GlobalConfiguration.reload();
            check(new RequirementTextMatcher(), "system", "system", true);
            check(new RequirementTextMatcher(), "system", "sytem", false);
            Files.writeString(configuration, "traceability.match.stopwords= AND , solar , solar\n");
            GlobalConfiguration.reload();
            check(new RequirementTextMatcher(), "solar and cooling", "solar and cooling", false);
            check(new RequirementTextMatcher(), "the cooling", "the cooling", true);
            Files.writeString(configuration, "traceability.match.stopwords=\n");
            GlobalConfiguration.reload();
            check(new RequirementTextMatcher(), "and the", "and the", true);
            Files.writeString(configuration, "traceability.match.stopwords=p\\u00e5,and\n");
            GlobalConfiguration.reload();
            check(new RequirementTextMatcher(), "på and cooling", "PÅ AND cooling", false);
            RequirementTextMatcher filtered = new RequirementTextMatcher(true, 1, 4, 8, 1, 2, "their");
            check(filtered, "their", "theirs", false);
            check(filtered, "theirs", "their", false);
            Files.writeString(configuration, "traceability.match.minimum-matching-words=invalid\n"
                    + "traceability.match.fuzzy.medium-word-max-edits=999\n");
            GlobalConfiguration.reload();
            check(new RequirementTextMatcher(), "system chocolate", "sytem chocolate", true);
            check(new RequirementTextMatcher(), "system", "system", false);
        } finally {
            Files.deleteIfExists(configuration);
        }
        System.out.println("RequirementTextMatcher regression checks passed");
    }

    private static void check(RequirementTextMatcher matcher, String left, String right, boolean expected) {
        boolean actual = matcher.matches(RequirementTextMatcher.words(left, null),
                RequirementTextMatcher.words(right, null));
        if (actual != expected) {
            throw new AssertionError(left + " / " + right + ": expected " + expected + ", got " + actual);
        }
    }
}
