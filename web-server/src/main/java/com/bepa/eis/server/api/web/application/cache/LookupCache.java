package com.bepa.eis.server.api.web.application.cache;

import com.bepa.eis.server.api.DTO.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LookupCache {

    private final RequirementBusinessPriorityCache requirementBusinessPriorityCache;
    private final RequirementVerificationCache requirementVerificationCache;
    private final ProjectCategoryCache projectCategoryCache;
    private final ProjectPriorityCache projectPriorityCache;
    private final RequirementStatusCache requirementStatusCache;
    private final ProjectStatusCache projectStatusCache;
    private final UserCache userCache;
    private final UserDetailCache userDetailCache;
    private final DepartmentCache departmentCache;
    private final RequirementTypeCache requirementTypeCache;
    private final RequirementFrequencyCache requirementFrequencyCache;
    private final RequirementTechnicalPriorityCache requirementTechnicalPriorityCache;
    private final RequirementVerificationStatementCache requirementVerificationStatementCache;

    private final Map<Integer, LookupProjectCache> lookupProjectCacheMap;

    public LookupCache(Integer customerId, Integer projectId) {
        requirementBusinessPriorityCache = new RequirementBusinessPriorityCache(customerId, projectId);
        requirementVerificationCache = new RequirementVerificationCache(customerId, projectId);
        projectCategoryCache = new ProjectCategoryCache(customerId, projectId);
        projectPriorityCache = new ProjectPriorityCache(customerId, projectId);
        requirementStatusCache = new RequirementStatusCache(customerId, projectId);
        projectStatusCache = new ProjectStatusCache(customerId, projectId);
        userCache = new UserCache(customerId, projectId);
        userDetailCache = new UserDetailCache(customerId, projectId) ;
        departmentCache = new DepartmentCache(customerId, projectId);
        requirementTypeCache = new RequirementTypeCache(customerId, projectId);
        requirementFrequencyCache = new RequirementFrequencyCache(customerId, projectId);
        requirementTechnicalPriorityCache = new RequirementTechnicalPriorityCache(customerId, projectId);
        requirementVerificationStatementCache = new RequirementVerificationStatementCache(customerId, projectId);

        lookupProjectCacheMap = new HashMap<>();
    }

    public LookupValue getRequirementBusinessPriorityLookupValue(Integer lookupId) {
        return lookupId != null ? requirementBusinessPriorityCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getRequirementBusinessPriorityLookupValues() {
        return requirementBusinessPriorityCache.getListOfActiveLookupValues();
    }

    public LookupValue getRequirementVerificationLookupValue(Integer lookupId) {
        return lookupId != null ? requirementVerificationCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getRequirementVerificationLookupValues() {
        return requirementVerificationCache.getListOfActiveLookupValues();
    }

    public LookupValue getTrlLookupValue(Integer customerId, Integer projectId, Integer lookupId) {

        if (customerId != null && projectId != null && lookupId != null) {
            LookupProjectCache projectCache = getLookupProjectCache(customerId, projectId);
            return projectCache.getTrlLookupValue(lookupId);
        }

        return null;
    }

    public List<LookupValue> getTrlLookupValues(Integer customerId, Integer projectId) {

        if (customerId != null && projectId != null ) {
            LookupProjectCache projectCache = getLookupProjectCache(customerId, projectId);
            return projectCache.getTrlLookupValues();
        }
        return new ArrayList<>();
    }

    public LookupValue getStakeholderLookupValue(Integer customerId, Integer projectId, Integer lookupId) {

        if (customerId != null && projectId != null && lookupId != null) {
            LookupProjectCache projectCache = getLookupProjectCache(customerId, projectId);
            return projectCache.getStakeholderLookupValue(lookupId);
        }

        return null;
    }

    public List<LookupValue> getStakeholderLookupValues(Integer customerId, Integer projectId) {

        if (customerId != null && projectId != null ) {
            LookupProjectCache projectCache = getLookupProjectCache(customerId, projectId);
            return projectCache.getStakeholderLookupValues();
        }
        return new ArrayList<>();
    }

    public LookupValue getProjectCategoryLookupValue(Integer lookupId) {
        return lookupId != null ? projectCategoryCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getProjectCategoryLookupValues() {
        return projectCategoryCache.getListOfActiveLookupValues();
    }

    public LookupValue getProjectPriorityLookupValue(Integer lookupId) {
        return lookupId != null ? projectPriorityCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getProjectPriorityLookupValues() {
        return projectPriorityCache.getListOfActiveLookupValues();
    }

    public LookupValue getRequirementStatusLookupValue(Integer lookupId) {
        return lookupId != null ? requirementStatusCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getRequirementStatusLookupValues() {
        return requirementStatusCache.getListOfActiveLookupValues();
    }

    public LookupValue getProjectStatusLookupValue(Integer lookupId) {
        return lookupId != null ? projectStatusCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getProjectStatusLookupValues() {
        return projectStatusCache.getListOfActiveLookupValues();
    }

    public LookupValue getUserLookupValue(Integer userId) {
        return userId != null ? userCache.getLookupValueById(userId) : null;
    }

    public List<LookupValue> getUserLookupValues() {
        return userCache.getListOfAllLookupValues();
    }

    public User getUser(Integer userId) {
        return userId != null ? userDetailCache.getUser(userId) : null;
    }

    public LookupValue getDepartmentLookupValue(Integer departmentId) {
        return departmentId != null ? departmentCache.getLookupValueById(departmentId) : null;
    }

    public List<LookupValue> getDepartmentLookupValues() {
        return departmentCache.getListOfAllLookupValues();
    }

    public LookupValue getRequirementTypeLookupValue(Integer typeId) {
        return typeId != null ? requirementTypeCache.getLookupValueById(typeId) : null;
    }

    public List<LookupValue> getRequirementTypeLookupValues() {
        return requirementTypeCache.getListOfAllLookupValues();
    }

    public LookupValue getRequirementFrequencyLookupValue(Integer typeId) {
        return typeId != null ? requirementFrequencyCache.getLookupValueById(typeId) : null;
    }

    public List<LookupValue> getRequirementFrequencyLookupValues() {
        return requirementFrequencyCache.getListOfAllLookupValues();
    }

    public LookupValue getRequirementTechnicalPriorityLookupValue(Integer priorityId) {
        return priorityId != null ? requirementTechnicalPriorityCache.getLookupValueById(priorityId) : null;
    }

    public List<LookupValue> getRequirementTechnicalPriorityLookupValues() {
        return requirementTechnicalPriorityCache.getListOfAllLookupValues();
    }

    public LookupValue getRequirementVerificationStatementLookupValue(Integer statementId) {
        return statementId != null ? requirementVerificationStatementCache.getLookupValueById(statementId) : null;
    }

    public List<LookupValue> getRequirementVerificationStatementLookupValues() {
        return requirementVerificationStatementCache.getListOfAllLookupValues();
    }

    private LookupProjectCache getLookupProjectCache(Integer customerId, Integer projectId) {
        LookupProjectCache lookupProjectCache = lookupProjectCacheMap.get(projectId);
        if (lookupProjectCache == null) {
            lookupProjectCache = new LookupProjectCache(customerId, projectId);
            lookupProjectCacheMap.put(projectId, lookupProjectCache);
        }

        return lookupProjectCache;
    }

}
