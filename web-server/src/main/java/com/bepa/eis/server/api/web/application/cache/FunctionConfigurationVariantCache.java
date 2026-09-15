package com.bepa.eis.server.api.web.application.cache;


public class FunctionConfigurationVariantCache extends GenericLookup {

    public FunctionConfigurationVariantCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(36);
        reloadCache();
    }

}
