package com.bepa.eis.server.api.web.application.cache;


public class LogicalRedundancyTypeCache extends GenericLookup {

    public LogicalRedundancyTypeCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(28);
        reloadCache();
    }

}
