package com.bepa.eis.server.api.web.application.cache;


public class LogicalConfigurationVariantCache extends GenericLookup {

    public LogicalConfigurationVariantCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(29);
        reloadCache();
    }

}
