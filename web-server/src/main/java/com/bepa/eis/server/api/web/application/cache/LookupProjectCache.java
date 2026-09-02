package com.bepa.eis.server.api.web.application.cache;

import java.util.ArrayList;
import java.util.List;

public class LookupProjectCache {

    private final ProjectBasisInfo projectBasisInfo;
    private final TrlCache trlCache;
    private final IrlCache irlCache;
    private final SrlCache srlCache;
    private final ClassificationCache classificationCache;

    private final StakeholderCache stakeholderCache;

    public LookupProjectCache(Integer customerId, Integer projectId) {
        projectBasisInfo = new ProjectBasisInfoProvider().getProjectBasisInfo(customerId, projectId);
        trlCache = new TrlCache(customerId, projectId);
        irlCache = new IrlCache(customerId, projectId);
        srlCache = new SrlCache(customerId, projectId);
        classificationCache = new ClassificationCache(customerId, projectId);
        stakeholderCache = new StakeholderCache(customerId, projectId);
    }

    public ProjectBasisInfo getProjectBasisInfo() {
        return projectBasisInfo;
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

    public LookupValue getClassificationLookupValue(Integer lookupId) {
        return lookupId != null ? classificationCache.getLookupValueById(lookupId) : null;
    }

    public List<ClassLookupValue> getClassificationLookupValues() {
        List<LookupValue> listOfValues = classificationCache.getListOfActiveLookupValues();
        List<ClassLookupValue> listOfSrlValues = new ArrayList<>();

        for (LookupValue lookupValue : listOfValues) {
            listOfSrlValues.add((ClassLookupValue) lookupValue);
        }

        return listOfSrlValues;
    }

    public LookupValue getStakeholderLookupValue(Integer lookupId) {
        return lookupId != null ? stakeholderCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getStakeholderLookupValues() {
        return stakeholderCache.getListOfActiveLookupValues();
    }
}
