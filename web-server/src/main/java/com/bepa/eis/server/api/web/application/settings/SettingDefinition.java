package com.bepa.eis.server.api.web.application.settings;

import java.util.List;

/** Shared editor metadata and defaults, independent of the active global configuration. */
public record SettingDefinition(String key, String scope, String group, String label, String type,
                                String defaultValue, Integer min, Integer max, String help) {
    public static final List<SettingDefinition> ALL = List.of(
            new SettingDefinition("mfaPolicy", "customer", "security", "Customer MFA policy", "policy", "OPTIONAL", null, null,
                    "OPTIONAL allows users to choose MFA. REQUIRED requires MFA for every user. DISABLED disables MFA."),
            new SettingDefinition("fuzzyEnabled", "project", "traceabilityMatching", "Enable fuzzy matching", "boolean", "true", null, null,
                    "Allow similar spellings to match, for example system and sytem. Short words still require an exact match."),
            new SettingDefinition("minimumMatchingWords", "project", "traceabilityMatching", "Minimum matching words", "integer", "2", 1, 1000,
                    "Number of distinct matching words required for a match. Repeating the same word does not increase the count."),
            new SettingDefinition("shortWordMaxLength", "project", "traceabilityMatching", "Short word maximum length", "integer", "4", 1, 100,
                    "Words up to this length must match exactly. With 4, coal and cool do not match."),
            new SettingDefinition("mediumWordMaxLength", "project", "traceabilityMatching", "Medium word maximum length", "integer", "8", 1, 100,
                    "Words longer than the short limit and up to this length use the medium edit limit. Longer words use the long edit limit."),
            new SettingDefinition("mediumWordMaxEdits", "project", "traceabilityMatching", "Medium word maximum edits", "integer", "1", 0, 5,
                    "Maximum character insertions, deletions or replacements for medium words. One edit allows system to match sytem."),
            new SettingDefinition("longWordMaxEdits", "project", "traceabilityMatching", "Long word maximum edits", "integer", "2", 0, 5,
                    "Maximum character insertions, deletions or replacements for words longer than the medium limit. Zero requires exact spelling."),
            new SettingDefinition("stopwords", "project", "traceabilityMatching", "Stopwords", "text", 
                    "og,i,jeg,det,at,en,den,til,er,som,på,de,med,han,af,for,der,var,mig,sig,men,et,har,om,vi,the,and,a,an,of,to,in,is,it,that,for,on,with,as,was,are,be,by,this,from,or,at,have,has,they", null, null,
                    "Comma-separated words excluded before matching, for example og,the,and. Leave empty to disable stopword filtering.")
    );

    public String validate(String value) {
        if (value == null) throw new IllegalArgumentException(label + " is required");
        if (type.equals("text")) {
            if (value.length() > 16000) throw new IllegalArgumentException(label + " is too long");
            return value.trim();
        }
        value = value.trim();
        if (type.equals("boolean") && (value.equals("true") || value.equals("false"))) return value;
        if (type.equals("policy") && List.of("OPTIONAL", "REQUIRED", "DISABLED").contains(value)) return value;
        if (type.equals("integer")) {
            try {
                int number = Integer.parseInt(value);
                if (number >= min && number <= max) return Integer.toString(number);
            } catch (NumberFormatException ignored) { }
        }
        throw new IllegalArgumentException("Invalid value for " + label);
    }
}
