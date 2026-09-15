package com.bepa.eis.server.api.web.application.cache;


public class LogicalElementCategoryCache extends GenericLookup {

    public LogicalElementCategoryCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(18);
        reloadCache();
    }

}
