package com.bepa.eis.server.api.web.application.cache;

import java.util.List;

public class LookupProjectCache {

    private final TrlCache trlCache;
    private final IrlCache irlCache;
    private final SrlCache srlCache;
    private final ClassificationCache classificationCache;

    private final StakeholderCache stakeholderCache;

    public LookupProjectCache(Integer customerId, Integer projectId) {
        trlCache = new TrlCache(customerId, projectId);
        irlCache = new IrlCache(customerId, projectId);
        srlCache = new SrlCache(customerId, projectId);
        classificationCache = new ClassificationCache(customerId, projectId);
        stakeholderCache = new StakeholderCache(customerId, projectId);
    }

    public LookupValue getTrlLookupValue(Integer lookupId) {
        return lookupId != null ? trlCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getTrlLookupValues() {
        return trlCache.getListOfActiveLookupValues();
    }

    public LookupValue getIrlLookupValue(Integer lookupId) {
        return lookupId != null ? irlCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getIrlLookupValues() {
        return irlCache.getListOfActiveLookupValues();
    }

    public LookupValue getSrlLookupValue(Integer lookupId) {
        return lookupId != null ? srlCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getSrlLookupValues() {
        return srlCache.getListOfActiveLookupValues();
    }

    public LookupValue getClassificationLookupValue(Integer lookupId) {
        return lookupId != null ? classificationCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getClassificationLookupValues() {
        return classificationCache.getListOfActiveLookupValues();
    }

    public LookupValue getStakeholderLookupValue(Integer lookupId) {
        return lookupId != null ? stakeholderCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getStakeholderLookupValues() {
        return stakeholderCache.getListOfActiveLookupValues();
    }
}
