package com.bepa.eis.server.api.web.application.cache;

import java.util.List;

public class LookupProjectCache {

    private final TrlCache trlCache;
    private final StakeholderCache stakeholderCache;

    public LookupProjectCache(Integer customerId, Integer projectId) {
        trlCache = new TrlCache(customerId, projectId);
        stakeholderCache = new StakeholderCache(customerId, projectId);
    }

    public LookupValue getTrlLookupValue(Integer lookupId) {
        return lookupId != null ? trlCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getTrlLookupValues() {
        return trlCache.getListOfActiveLookupValues();
    }

    public LookupValue getStakeholderLookupValue(Integer lookupId) {
        return lookupId != null ? stakeholderCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getStakeholderLookupValues() {
        return stakeholderCache.getListOfActiveLookupValues();
    }
}
