package com.bepa.eis.server.api.web.application.cache;

public class ProjectCategoryCache extends GenericLookup {

    public ProjectCategoryCache(Integer customerId, Integer projectId) {
        setLookupSqlByType(1);
        reloadCache();
    }

}
