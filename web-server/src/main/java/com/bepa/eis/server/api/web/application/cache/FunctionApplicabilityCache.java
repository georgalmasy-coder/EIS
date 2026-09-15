package com.bepa.eis.server.api.web.application.cache;


public class FunctionApplicabilityCache extends GenericLookup {

    public FunctionApplicabilityCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(37);
        reloadCache();
    }

}
