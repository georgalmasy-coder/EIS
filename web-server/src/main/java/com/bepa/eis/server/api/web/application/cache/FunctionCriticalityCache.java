package com.bepa.eis.server.api.web.application.cache;


public class FunctionCriticalityCache extends GenericLookup {

    public FunctionCriticalityCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(15);
        reloadCache();
    }

}
