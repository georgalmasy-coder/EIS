package com.bepa.eis.server.api.web.application.cache;


public class LogicalLevelCache extends GenericLookup {

    public LogicalLevelCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(19);
        reloadCache();
    }

}
