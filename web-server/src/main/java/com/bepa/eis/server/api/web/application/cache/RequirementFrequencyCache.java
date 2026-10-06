package com.bepa.eis.server.api.web.application.cache;

public class RequirementFrequencyCache extends GenericLookup {

    public RequirementFrequencyCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(6);
        reloadCache();
    }

}
