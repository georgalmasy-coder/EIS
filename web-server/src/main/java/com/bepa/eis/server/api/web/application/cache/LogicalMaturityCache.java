package com.bepa.eis.server.api.web.application.cache;


public class LogicalMaturityCache extends GenericLookup {

    public LogicalMaturityCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(23);
        reloadCache();
    }

}
