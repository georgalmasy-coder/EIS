package com.bepa.eis.server.api.web.application.cache;

import com.bepa.eis.common.dto.WebSession;

public class ProjectStatusCache extends GenericLookup {

    public ProjectStatusCache(WebSession webSession) {
        setLookupSqlByType(3);
        reloadCache();
    }

}
