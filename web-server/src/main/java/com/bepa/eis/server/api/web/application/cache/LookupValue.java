package com.bepa.eis.server.api.web.application.cache;

public class LookupValue {

    private Integer customerId;
    private Integer projectId;
    private Integer lookupId;
    private String lookupCode;
    private String lookupDescription;
    private String lookupColor;
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

    public String getLookupColor() {
        return lookupColor;
    }

    public boolean isActive() {
        return active != null && active;
    }

    public LookupValue(Integer customerId, Integer projectId, Integer lookupId, String lookupCode, String lookupDescription, Boolean active) {
        this.customerId = customerId;
        this.projectId = projectId;
        this.lookupId = lookupId;
        this.lookupCode = lookupCode;
        this.lookupDescription = lookupDescription;
        this.active = active;
    }

    public LookupValue(Integer customerId, Integer projectId, Integer lookupId, String lookupCode, String lookupDescription, String lookupColor, Boolean active) {
        this.customerId = customerId;
        this.projectId = projectId;
        this.lookupId = lookupId;
        this.lookupCode = lookupCode;
        this.lookupDescription = lookupDescription;
        this.lookupColor = lookupColor;
        this.active = active;
    }

}
