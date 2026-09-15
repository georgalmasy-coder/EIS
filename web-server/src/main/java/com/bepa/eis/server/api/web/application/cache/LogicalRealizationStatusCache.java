package com.bepa.eis.server.api.web.application.cache;


public class LogicalRealizationStatusCache extends GenericLookup {

    public LogicalRealizationStatusCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(25);
        reloadCache();
    }

}
