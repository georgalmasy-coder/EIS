package com.bepa.eis.server.api.web.application.cache;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.DTO.User;
import com.bepa.eis.server.dataprovider.cache.EhcacheProvider;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class CustomerLookupCache {

    private static final Logger log = LoggerFactory.getLogger(CustomerLookupCache.class);

    private static final String CACHE_ALIAS = "LookupCache";
    private static final ReentrantLock BULK_LOAD_LOCK = new ReentrantLock();

    public static LookupValue getRequirementBusinessPriorityLookupValue(WebSession webSession, Integer lookupId) {
        return getLookupCache(webSession).getRequirementBusinessPriorityLookupValue(lookupId);
    }

    public static List<LookupValue> getRequirementBusinessPriorityLookupValues(WebSession webSession) {
        return getLookupCache(webSession).getRequirementBusinessPriorityLookupValues();
    }

    public static LookupValue getRequirementVerificationLookupValue(WebSession webSession, Integer lookupId) {
        return getLookupCache(webSession).getRequirementVerificationLookupValue(lookupId);
    }

    public static List<LookupValue> getRequirementVerificationLookupValues(WebSession webSession) {
        return getLookupCache(webSession).getRequirementVerificationLookupValues();
    }

    public static LookupValue getTrlLookupValue(WebSession webSession, Integer lookupId) {
        return getLookupCache(webSession).getTrlLookupValue(lookupId);
    }

    public static List<LookupValue> getTrlLookupValues(WebSession webSession) {
        return getLookupCache(webSession).getTrlLookupValues();
    }

    public static LookupValue getProjectCategoryLookupValue(WebSession webSession, Integer categoryId) {
        return getLookupCache(webSession).getProjectCategoryLookupValue(categoryId);
    }

    public static List<LookupValue> getProjectCategoryLookupValues(WebSession webSession) {
        return getLookupCache(webSession).getProjectPriorityLookupValues();
    }

    public static LookupValue getProjectPriorityLookupValue(WebSession webSession, Integer priorityId) {
        return getLookupCache(webSession).getProjectPriorityLookupValue(priorityId);
    }

    public static List<LookupValue> getProjectPriorityLookupValues(WebSession webSession) {
        return getLookupCache(webSession).getProjectPriorityLookupValues();
    }

    public static LookupValue getRequirementStatusLookupValue(WebSession webSession, Integer statusId) {
        return getLookupCache(webSession).getRequirementStatusLookupValue(statusId);
    }

    public static List<LookupValue> getRequirementStatusLookupValues(WebSession webSession) {
        return getLookupCache(webSession).getRequirementStatusLookupValues();
    }

    public static LookupValue getProjectStatusLookupValue(WebSession webSession, Integer statusId) {
        return getLookupCache(webSession).getProjectStatusLookupValue(statusId);
    }

    public static List<LookupValue> getProjectStatusLookupValues(WebSession webSession) {
        return getLookupCache(webSession).getProjectStatusLookupValues();
    }

    public static LookupValue getUserLookupValue(WebSession webSession, Integer userId) {
        return getLookupCache(webSession).getUserLookupValue(userId);
    }

    public static List<LookupValue> getUserLookupValues(WebSession webSession) {
        return getLookupCache(webSession).getUserLookupValues();
    }

    public static User getUser(WebSession webSession, Integer userId) {
        return getLookupCache(webSession).getUser(userId);
    }

    public static LookupValue getDepartmentLookupValue(WebSession webSession, Integer departmentId) {
        return getLookupCache(webSession).getDepartmentLookupValue(departmentId);
    }

    public static List<LookupValue> getDepartmentLookupValues(WebSession webSession) {
        return getLookupCache(webSession).getDepartmentLookupValues();
    }

    public static LookupValue getRequirementTypeLookupValue(WebSession webSession, Integer departmentId) {
        return getLookupCache(webSession).getRequirementTypeLookupValue(departmentId);
    }

    public static List<LookupValue> getRequirementTypeLookupValues(WebSession webSession) {
        return getLookupCache(webSession).getRequirementTypeLookupValues();
    }

    public static LookupValue getRequirementFrequencyLookupValue(WebSession webSession, Integer departmentId) {
        return getLookupCache(webSession).getRequirementFrequencyLookupValue(departmentId);
    }

    public static List<LookupValue> getRequirementFrequencyLookupValues(WebSession webSession) {
        return getLookupCache(webSession).getRequirementFrequencyLookupValues();
    }

    public static LookupValue getRequirementTechnicalPriorityLookupValue(WebSession webSession, Integer departmentId) {
        return getLookupCache(webSession).getRequirementTechnicalPriorityLookupValue(departmentId);
    }

    public static List<LookupValue> getRequirementTechnicalPriorityLookupValues(WebSession webSession) {
        return getLookupCache(webSession).getRequirementTechnicalPriorityLookupValues();
    }

    public static LookupValue getRequirementVerificationStatementLookupValue(WebSession webSession, Integer departmentId) {
        return getLookupCache(webSession).getRequirementVerificationStatementLookupValue(departmentId);
    }

    public static List<LookupValue> getRequirementVerificationStatementLookupValues(WebSession webSession) {
        return getLookupCache(webSession).getRequirementVerificationStatementLookupValues();
    }

    private static LookupCache getLookupCache(WebSession webSession) {
        LookupCache lookupCache;
        try {
            lookupCache = getCache().get(webSession.getCustomerId());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load cache: " + CACHE_ALIAS);
        }

        if (lookupCache == null) {
            reloadCache(webSession);
            lookupCache = getCache().get(webSession.getCustomerId());
            if (lookupCache == null) {
                throw new IllegalStateException("Failed to load requirement business priority lookup cache for customer: " + webSession.getCustomerId());
            }
        }
        return lookupCache;
    }

    private static void reloadCache(WebSession webSession) {
        BULK_LOAD_LOCK.lock();
        try {
            if ( webSession != null /*&& ! isCacheLoaded()*/ ) {
                getCache().put(webSession.getCustomerId(), new LookupCache(webSession));
            } else {
                log.debug("Cache for requirement business priority {} found.", webSession.getCustomerId());
            }

        } finally {
            BULK_LOAD_LOCK.unlock();
        }

    }

    private static Cache<Integer, LookupCache> getCache() {
        CacheManager cacheManager = EhcacheProvider.getCacheManager();
        Cache<Integer, LookupCache> cache = cacheManager.getCache(CACHE_ALIAS, Integer.class, LookupCache.class);
        if (cache == null) {
            throw new IllegalStateException(
                    "Ehcache cache alias not found: " + CACHE_ALIAS + " (check ehcache.xml)"
            );
        }
        return cache;
    }
}
