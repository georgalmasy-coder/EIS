package com.bepa.eis.server.api.web.application.cache;


public class FunctionStatusCache extends GenericLookup {

    public FunctionStatusCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(31);
        reloadCache();
    }

}
