package com.bepa.eis.server.api.web.application.views.basis.traceability;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.settings.CustomerSettings;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

/** Language-independent, one-to-one matching of distinct words. */
final class RequirementTextMatcher {
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{M}\\p{N}]+");

    private final boolean fuzzyEnabled;
    private final int minimumMatches;
    private final int shortWordMaxLength;
    private final int mediumWordMaxLength;
    private final int mediumWordMaxEdits;
    private final int longWordMaxEdits;
    private final Set<String> stopwords;

    RequirementTextMatcher(WebSession session) {
        this(CustomerLookupCache.getSettings(session).getTraceabilityMatching(session.getCustomerId(), session.getProjectId()));
    }

    RequirementTextMatcher(CustomerSettings.TraceabilityMatching settings) {
        this(settings.fuzzyEnabled(), settings.minimumMatchingWords(), settings.shortWordMaxLength(),
                settings.mediumWordMaxLength(), settings.mediumWordMaxEdits(), settings.longWordMaxEdits(), settings.stopwords());
    }

    RequirementTextMatcher(boolean fuzzyEnabled, int minimumMatches, int shortWordMaxLength,
                           int mediumWordMaxLength, int mediumWordMaxEdits, int longWordMaxEdits) {
        this(fuzzyEnabled, minimumMatches, shortWordMaxLength, mediumWordMaxLength,
                mediumWordMaxEdits, longWordMaxEdits, "");
    }

    RequirementTextMatcher(boolean fuzzyEnabled, int minimumMatches, int shortWordMaxLength,
                           int mediumWordMaxLength, int mediumWordMaxEdits, int longWordMaxEdits,
                           String stopwords) {
        this.fuzzyEnabled = fuzzyEnabled;
        this.minimumMatches = minimumMatches;
        this.shortWordMaxLength = shortWordMaxLength;
        this.mediumWordMaxLength = Math.max(shortWordMaxLength, mediumWordMaxLength);
        this.mediumWordMaxEdits = mediumWordMaxEdits;
        this.longWordMaxEdits = longWordMaxEdits;
        this.stopwords = words(stopwords, null);
    }

    static Set<String> words(String name, String description) {
        String text = Objects.toString(name, "") + " " + Objects.toString(description, "");
        text = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFC);
        Set<String> words = new LinkedHashSet<>();
        var matcher = WORD.matcher(text);
        while (matcher.find()) {
            words.add(matcher.group());
        }
        return words;
    }

    boolean matches(Set<String> source, Set<String> target) {
        return matches(prepare(source), prepare(target));
    }

    static final class PreparedWords {
        private final Set<String> words;

        private PreparedWords(Set<String> words) {
            this.words = words;
        }
    }

    PreparedWords prepare(String name, String description) {
        Set<String> prepared = words(name, description);
        prepared.removeAll(stopwords);
        return new PreparedWords(prepared);
    }

    private PreparedWords prepare(Set<String> words) {
        Set<String> prepared = new LinkedHashSet<>(words);
        prepared.removeAll(stopwords);
        return new PreparedWords(prepared);
    }

    boolean matches(PreparedWords sourceWords, PreparedWords targetWords) {
        Set<String> source = sourceWords.words;
        Set<String> target = targetWords.words;
        if (Math.min(source.size(), target.size()) < minimumMatches) {
            return false;
        }
        Set<String> smaller = source.size() <= target.size() ? source : target;
        Set<String> larger = smaller == source ? target : source;
        int exactCount = 0;
        for (String word : smaller) {
            if (larger.contains(word) && ++exactCount >= minimumMatches) {
                return true;
            }
        }
        if (!fuzzyEnabled) return false;
        return findFuzzyMatches(unmatched(source, target), unmatched(target, source),
                minimumMatches - exactCount, null) >= minimumMatches - exactCount;
    }

    List<WordMatch> matchingWords(Set<String> source, Set<String> target) {
        Set<String> left = prepare(source).words;
        Set<String> right = prepare(target).words;
        List<WordMatch> result = new ArrayList<>();
        for (String word : left) {
            if (right.contains(word)) result.add(new WordMatch(word, word));
        }
        if (fuzzyEnabled) {
            findFuzzyMatches(unmatched(left, right), unmatched(right, left), Integer.MAX_VALUE, result);
        }
        return result;
    }

    record WordMatch(String source, String target) { }

    private List<String> unmatched(Set<String> source, Set<String> target) {
        List<String> result = new ArrayList<>();
        for (String word : source) {
            if (!target.contains(word)) result.add(word);
        }
        return result;
    }

    private int findFuzzyMatches(List<String> left, List<String> right, int stopAfter, List<WordMatch> result) {
        int count = 0;
        // Build candidate edges once; augmenting paths avoid order-dependent greedy matches.
        List<List<Integer>> candidates = new ArrayList<>();
        int[] owners = new int[right.size()];
        Arrays.fill(owners, -1);
        for (int i = 0; i < left.size(); i++) {
            List<Integer> edges = new ArrayList<>();
            for (int j = 0; j < right.size(); j++) {
                if (similar(left.get(i), right.get(j))) {
                    edges.add(j);
                }
            }
            candidates.add(edges);
            if (assign(i, candidates, owners, new boolean[right.size()]) && ++count >= stopAfter) {
                break;
            }
        }
        if (result != null) {
            for (int j = 0; j < owners.length; j++) {
                if (owners[j] != -1) {
                    result.add(new WordMatch(left.get(owners[j]), right.get(j)));
                }
            }
        }
        return count;
    }

    private boolean assign(int source, List<List<Integer>> candidates, int[] owners, boolean[] visited) {
        for (int target : candidates.get(source)) {
            if (!visited[target]) {
                visited[target] = true;
                if (owners[target] == -1 || assign(owners[target], candidates, owners, visited)) {
                    owners[target] = source;
                    return true;
                }
            }
        }
        return false;
    }

    private boolean similar(String left, String right) {
        int leftLength = left.codePointCount(0, left.length());
        int rightLength = right.codePointCount(0, right.length());
        int length = Math.min(leftLength, rightLength);
        int limit = length <= shortWordMaxLength ? 0
                : length <= mediumWordMaxLength ? mediumWordMaxEdits : longWordMaxEdits;
        if (limit == 0 || Math.abs(leftLength - rightLength) > limit) {
            return false;
        }
        int[] a = left.codePoints().toArray();
        int[] b = right.codePoints().toArray();
        int[] previous = new int[b.length + 1];
        int[] current = new int[b.length + 1];
        for (int j = 0; j <= b.length; j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length; i++) {
            Arrays.fill(current, limit + 1);
            current[0] = i;
            int rowMinimum = limit + 1;
            // Only evaluate the diagonal band that can be within the edit limit.
            for (int j = Math.max(1, i - limit); j <= Math.min(b.length, i + limit); j++) {
                current[j] = Math.min(Math.min(previous[j] + 1, current[j - 1] + 1),
                        previous[j - 1] + (a[i - 1] == b[j - 1] ? 0 : 1));
                rowMinimum = Math.min(rowMinimum, current[j]);
            }
            if (rowMinimum > limit) {
                return false;
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length] <= limit;
    }
}
