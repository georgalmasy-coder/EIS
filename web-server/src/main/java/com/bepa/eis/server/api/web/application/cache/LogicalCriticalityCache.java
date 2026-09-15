package com.bepa.eis.server.api.web.application.cache;


public class LogicalCriticalityCache extends GenericLookup {

    public LogicalCriticalityCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(17);
        reloadCache();
    }

}
