package com.bepa.eis.server.api.web.application.cache;

import com.bepa.eis.common.dto.WebSession;

public class ProjectCategoryCache extends GenericLookup {

    public ProjectCategoryCache(WebSession webSession) {
        setLookupSqlByType(1);
        reloadCache();
    }

}
