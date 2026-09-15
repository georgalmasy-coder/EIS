package com.bepa.eis.server.api.web.application.cache;


public class LogicalLifecycleStatusCache extends GenericLookup {

    public LogicalLifecycleStatusCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(22);
        reloadCache();
    }

}
