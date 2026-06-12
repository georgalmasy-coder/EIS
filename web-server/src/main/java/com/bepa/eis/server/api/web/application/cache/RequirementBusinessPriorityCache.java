package com.bepa.eis.server.api.web.application.cache;

import com.bepa.eis.common.dto.WebSession;

public class RequirementBusinessPriorityCache extends GenericLookup {

    public RequirementBusinessPriorityCache(WebSession webSession) {
        setLookupSqlByType(5);
        reloadCache();
    }

}
