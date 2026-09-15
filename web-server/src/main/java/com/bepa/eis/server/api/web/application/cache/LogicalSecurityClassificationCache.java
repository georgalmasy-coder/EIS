package com.bepa.eis.server.api.web.application.cache;


public class LogicalSecurityClassificationCache extends GenericLookup {

    public LogicalSecurityClassificationCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(27);
        reloadCache();
    }

}
