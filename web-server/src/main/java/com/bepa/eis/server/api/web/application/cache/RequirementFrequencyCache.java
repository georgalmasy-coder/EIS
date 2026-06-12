package com.bepa.eis.server.api.web.application.cache;

import com.bepa.eis.common.dto.WebSession;

public class RequirementFrequencyCache extends GenericLookup {

    public RequirementFrequencyCache(WebSession webSession) {
        setLookupSqlByType(6);
        reloadCache();
    }

}
