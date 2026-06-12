package com.bepa.eis.server.api.web.application.views.basis.traceability;

public class RuleResult {

    private final String value;
    private final String style;

    protected RuleResult(String value, String style) {
        this.value = value;
        this.style = style;
    }

    protected String getValue() {
        return value;
    }

    protected String getStyle() {
        return style;
    }
}

