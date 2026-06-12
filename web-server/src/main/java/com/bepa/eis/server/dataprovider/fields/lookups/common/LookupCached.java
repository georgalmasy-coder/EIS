package com.bepa.eis.server.dataprovider.fields.lookups.common;

public class LookupCached {
    private Integer lookupId;
    private String lookupCode;
    private String lookupDescription;
    private boolean active;

    public LookupCached() {}

    public LookupCached(Integer lookupId, String lookupCode, String lookupDescription, boolean active) {
        this.lookupId = lookupId;
        this.lookupCode = lookupCode;
        this.lookupDescription = lookupDescription;
        this.active = active;
    }
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
        return active;
    }
}
