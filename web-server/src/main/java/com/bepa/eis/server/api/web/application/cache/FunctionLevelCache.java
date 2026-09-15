package com.bepa.eis.server.api.web.application.cache;


public class FunctionLevelCache extends GenericLookup {

    public FunctionLevelCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(33);
        reloadCache();
    }

}
