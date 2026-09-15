package com.bepa.eis.server.api.web.application.cache;


public class LogicalAllocationStatusCache extends GenericLookup {

    public LogicalAllocationStatusCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(24);
        reloadCache();
    }

}
