package com.bepa.eis.server.api.web.application.cache;


public class FunctionBehaviorTypeCache extends GenericLookup {

    public FunctionBehaviorTypeCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(13);
        reloadCache();
    }

}
