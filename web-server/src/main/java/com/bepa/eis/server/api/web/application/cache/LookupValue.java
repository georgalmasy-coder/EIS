package com.bepa.eis.server.api.web.application.cache;

public class LookupValue {

    private Integer lookupId;
    private String lookupCode;
    private String lookupDescription;
    private Boolean active;

    public Integer getLookupId() {
        return lookupId;
    }

    public String getLookupCode() {
        return lookupCode;
    }

    public String getLookupDescription() {
        return lookupDescription;
    }

    public boolean isActive() {
        return active != null && active;
    }

    public LookupValue(Integer lookupId, String lookupCode, String lookupDescription, Boolean active) {
        this.lookupId = lookupId;
        this.lookupCode = lookupCode;
        this.lookupDescription = lookupDescription;
        this.active = active;
    }
}
