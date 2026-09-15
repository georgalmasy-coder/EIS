package com.bepa.eis.server.api.web.application.cache;


public class LogicalTypeCache extends GenericLookup {

    public LogicalTypeCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(20);
        reloadCache();
    }

}
