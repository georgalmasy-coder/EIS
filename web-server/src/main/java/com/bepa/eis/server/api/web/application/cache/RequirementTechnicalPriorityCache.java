package com.bepa.eis.server.api.web.application.cache;

import com.bepa.eis.common.dto.WebSession;

public class RequirementTechnicalPriorityCache extends GenericLookup {

    public RequirementTechnicalPriorityCache(WebSession webSession) {
        setLookupSqlByType(5);
        reloadCache();
    }

}
