package com.bepa.eis.server.api.web.application.cache;

public class ClassLookupValue extends LookupValue {

    private String example;
    private String usageExample;

    public String getExample() {
        return example;
    }

    public String getUsageExample() {
        return usageExample;
    }

    public ClassLookupValue(Integer customerId, Integer projectId, Integer lookupId, String lookupCode, String lookupDescription, String example, String usageExample, Boolean active) {
        super(customerId, projectId, lookupId, lookupCode, lookupDescription, active);
        this.example = example;
        this.usageExample = usageExample;
    }

}
