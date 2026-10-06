package com.bepa.eis.server.api.web.application.cache;

public class RequirementStatusCache extends GenericLookup {

    public RequirementStatusCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(4);
        reloadCache();
    }

}
