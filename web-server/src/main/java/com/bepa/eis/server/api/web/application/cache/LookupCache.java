package com.bepa.eis.server.api.web.application.cache;

import com.bepa.eis.server.api.DTO.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LookupCache {

    private final CustomerBasisInfo customerInfo;
    private final RequirementBusinessPriorityCache requirementBusinessPriorityCache;
    private final RequirementVerificationCache requirementVerificationCache;

    private final FunctionVerificationCache functionVerificationCache;
    private final FunctionBehaviorTypeCache functionBehaviorTypeCache;
    private final FunctionCategoryCache functionCategoryCache;
    private final FunctionCriticalityCache functionCriticalityCache;
    private final FunctionStatusCache functionStatusCache;
    private final FunctionLevelCache functionLevelCache;
    private final FunctionOperatingModeCache functionOperatingModeCache;
    private final FunctionResponsibleDomainCache functionResponsibleDomainCache;
    private final FunctionConfigurationVariantCache functionConfigurationVariantCache;
    private final FunctionApplicabilityCache functionApplicabilityCache;

    private final LogicalVerificationCache logicalVerificationCache;
    private final LogicalCriticalityCache logicalCriticalityCache;
    private final LogicalElementCategoryCache logicalElementCategoryCache;
    private final LogicalLevelCache logicalLevelCache;
    private final LogicalTypeCache logicalTypeCache;
    private final LogicalResponsibleDomainCache logicalResponsibleDomainCache;
    private final LogicalLifecycleStatusCache logicalLifecycleStatusCache;
    private final LogicalMaturityCache logicalMaturityCache;
    private final LogicalAllocationStatusCache logicalAllocationStatusCache;
    private final LogicalRealizationStatusCache logicalRealizationStatusCache;
    private final LogicalSafetyClassificationCache logicalSafetyClassificationCache;
    private final LogicalSecurityClassificationCache logicalSecurityClassificationCache;
    private final LogicalRedundancyTypeCache logicalRedundancyTypeCache;
    private final LogicalConfigurationVariantCache logicalConfigurationVariantCache;
    private final LogicalApplicabilityCache logicalApplicabilityCache;


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
        customerInfo = new CustomerInfoProvider().getCustomerInfo(customerId);
        requirementBusinessPriorityCache = new RequirementBusinessPriorityCache(customerId, projectId);
        requirementVerificationCache = new RequirementVerificationCache(customerId, projectId);

        functionVerificationCache = new FunctionVerificationCache(customerId, projectId);
        functionBehaviorTypeCache = new FunctionBehaviorTypeCache(customerId, projectId);
        functionCategoryCache = new FunctionCategoryCache(customerId, projectId);
        functionCriticalityCache = new FunctionCriticalityCache(customerId, projectId);
        functionStatusCache = new FunctionStatusCache(customerId, projectId);
        functionLevelCache = new FunctionLevelCache(customerId, projectId);
        functionOperatingModeCache = new FunctionOperatingModeCache(customerId, projectId);
        functionResponsibleDomainCache = new FunctionResponsibleDomainCache(customerId, projectId);
        functionConfigurationVariantCache = new FunctionConfigurationVariantCache(customerId, projectId);
        functionApplicabilityCache = new FunctionApplicabilityCache(customerId, projectId);

        logicalVerificationCache = new LogicalVerificationCache(customerId, projectId);
        logicalCriticalityCache = new LogicalCriticalityCache(customerId, projectId);
        logicalElementCategoryCache = new LogicalElementCategoryCache(customerId, projectId);
        logicalLevelCache = new LogicalLevelCache(customerId, projectId);
        logicalTypeCache = new LogicalTypeCache(customerId, projectId);
        logicalResponsibleDomainCache = new LogicalResponsibleDomainCache(customerId, projectId);
        logicalLifecycleStatusCache = new LogicalLifecycleStatusCache(customerId, projectId);
        logicalMaturityCache = new LogicalMaturityCache(customerId, projectId);
        logicalAllocationStatusCache = new LogicalAllocationStatusCache(customerId, projectId);
        logicalRealizationStatusCache = new LogicalRealizationStatusCache(customerId, projectId);
        logicalSafetyClassificationCache = new LogicalSafetyClassificationCache(customerId, projectId);
        logicalSecurityClassificationCache = new LogicalSecurityClassificationCache(customerId, projectId);
        logicalRedundancyTypeCache = new LogicalRedundancyTypeCache(customerId, projectId);
        logicalConfigurationVariantCache = new LogicalConfigurationVariantCache(customerId, projectId);
        logicalApplicabilityCache = new LogicalApplicabilityCache(customerId, projectId);

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

    public CustomerBasisInfo getCustomerInfo() {
        return customerInfo;
    }

    public ProjectBasisInfo getProjectInfo(Integer customerId, Integer projectId) {

        if (customerId != null && projectId != null ) {
            LookupProjectCache projectCache = getLookupProjectCache(customerId, projectId);
            return projectCache.getProjectBasisInfo();
        }

        return null;
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

    public LookupValue getFunctionVerificationLookupValue(Integer lookupId) {
        return lookupId != null ? functionVerificationCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getFunctionVerificationLookupValues() {
        return functionVerificationCache.getListOfActiveLookupValues();
    }

    public LookupValue getLogicalVerificationLookupValue(Integer lookupId) {
        return lookupId != null ? logicalVerificationCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getLogicalVerificationLookupValues() {
        return logicalVerificationCache.getListOfActiveLookupValues();
    }

    public LookupValue getFunctionBehaviorTypeLookupValue(Integer lookupId) {
        return lookupId != null ? functionBehaviorTypeCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getFunctionBehaviorTypeLookupValues() {
        return functionBehaviorTypeCache.getListOfActiveLookupValues();
    }

    public LookupValue getFunctionCategoryLookupValue(Integer lookupId) {
        return lookupId != null ? functionCategoryCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getFunctionCategoryLookupValues() {
        return functionCategoryCache.getListOfActiveLookupValues();
    }

    public LookupValue getFunctionCriticalityLookupValue(Integer lookupId) {
        return lookupId != null ? functionCriticalityCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getFunctionCriticalityLookupValues() {
        return functionCriticalityCache.getListOfActiveLookupValues();
    }

    public LookupValue getFunctionStatusLookupValue(Integer lookupId) {
        return lookupId != null ? functionStatusCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getFunctionStatusLookupValues() {
        return functionStatusCache.getListOfActiveLookupValues();
    }

    public LookupValue getFunctionLevelLookupValue(Integer lookupId) {
        return lookupId != null ? functionLevelCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getFunctionLevelLookupValues() {
        return functionLevelCache.getListOfActiveLookupValues();
    }

    public LookupValue getFunctionOperatingModeLookupValue(Integer lookupId) {
        return lookupId != null ? functionOperatingModeCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getFunctionOperatingModeLookupValues() {
        return functionOperatingModeCache.getListOfActiveLookupValues();
    }

    public LookupValue getFunctionResponsibleDomainLookupValue(Integer lookupId) {
        return lookupId != null ? functionResponsibleDomainCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getFunctionResponsibleDomainLookupValues() {
        return functionResponsibleDomainCache.getListOfActiveLookupValues();
    }

    public LookupValue getFunctionConfigurationVariantLookupValue(Integer lookupId) {
        return lookupId != null ? functionConfigurationVariantCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getFunctionConfigurationVariantLookupValues() {
        return functionConfigurationVariantCache.getListOfActiveLookupValues();
    }

    public LookupValue getFunctionApplicabilityLookupValue(Integer lookupId) {
        return lookupId != null ? functionApplicabilityCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getFunctionApplicabilityLookupValues() {
        return functionApplicabilityCache.getListOfActiveLookupValues();
    }

    public LookupValue getLogicalCriticalityLookupValue(Integer lookupId) {
        return lookupId != null ? logicalCriticalityCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getLogicalCriticalityLookupValues() {
        return logicalCriticalityCache.getListOfActiveLookupValues();
    }

    public LookupValue getLogicalElementCategoryLookupValue(Integer lookupId) {
        return lookupId != null ? logicalElementCategoryCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getLogicalElementCategoryLookupValues() {
        return logicalElementCategoryCache.getListOfActiveLookupValues();
    }

    public LookupValue getLogicalLevelLookupValue(Integer lookupId) {
        return lookupId != null ? logicalLevelCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getLogicalLevelLookupValues() {
        return logicalLevelCache.getListOfActiveLookupValues();
    }

    public LookupValue getLogicalTypeLookupValue(Integer lookupId) {
        return lookupId != null ? logicalTypeCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getLogicalTypeLookupValues() {
        return logicalTypeCache.getListOfActiveLookupValues();
    }

    public LookupValue getLogicalResponsibleDomainLookupValue(Integer lookupId) {
        return lookupId != null ? logicalResponsibleDomainCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getLogicalResponsibleDomainLookupValues() {
        return logicalResponsibleDomainCache.getListOfActiveLookupValues();
    }

    public LookupValue getLogicalLifecycleStatusLookupValue(Integer lookupId) {
        return lookupId != null ? logicalLifecycleStatusCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getLogicalLifecycleStatusLookupValues() {
        return logicalLifecycleStatusCache.getListOfActiveLookupValues();
    }

    public LookupValue getLogicalMaturityLookupValue(Integer lookupId) {
        return lookupId != null ? logicalMaturityCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getLogicalMaturityLookupValues() {
        return logicalMaturityCache.getListOfActiveLookupValues();
    }

    public LookupValue getLogicalAllocationStatusLookupValue(Integer lookupId) {
        return lookupId != null ? logicalAllocationStatusCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getLogicalAllocationStatusLookupValues() {
        return logicalAllocationStatusCache.getListOfActiveLookupValues();
    }

    public LookupValue getLogicalRealizationStatusLookupValue(Integer lookupId) {
        return lookupId != null ? logicalRealizationStatusCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getLogicalRealizationStatusLookupValues() {
        return logicalRealizationStatusCache.getListOfActiveLookupValues();
    }

    public LookupValue getLogicalSafetyClassificationLookupValue(Integer lookupId) {
        return lookupId != null ? logicalSafetyClassificationCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getLogicalSafetyClassificationLookupValues() {
        return logicalSafetyClassificationCache.getListOfActiveLookupValues();
    }

    public LookupValue getLogicalSecurityClassificationLookupValue(Integer lookupId) {
        return lookupId != null ? logicalSecurityClassificationCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getLogicalSecurityClassificationLookupValues() {
        return logicalSecurityClassificationCache.getListOfActiveLookupValues();
    }

    public LookupValue getLogicalRedundancyTypeLookupValue(Integer lookupId) {
        return lookupId != null ? logicalRedundancyTypeCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getLogicalRedundancyTypeLookupValues() {
        return logicalRedundancyTypeCache.getListOfActiveLookupValues();
    }

    public LookupValue getLogicalConfigurationVariantLookupValue(Integer lookupId) {
        return lookupId != null ? logicalConfigurationVariantCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getLogicalConfigurationVariantLookupValues() {
        return logicalConfigurationVariantCache.getListOfActiveLookupValues();
    }

    public LookupValue getLogicalApplicabilityLookupValue(Integer lookupId) {
        return lookupId != null ? logicalApplicabilityCache.getLookupValueById(lookupId) : null;
    }

    public List<LookupValue> getLogicalApplicabilityLookupValues() {
        return logicalApplicabilityCache.getListOfActiveLookupValues();
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

    public LookupValue getIrlLookupValue(Integer customerId, Integer projectId, Integer lookupId) {

        if (customerId != null && projectId != null && lookupId != null) {
            LookupProjectCache projectCache = getLookupProjectCache(customerId, projectId);
            return projectCache.getIrlLookupValue(lookupId);
        }

        return null;
    }

    public List<LookupValue> getIrlLookupValues(Integer customerId, Integer projectId) {

        if (customerId != null && projectId != null ) {
            LookupProjectCache projectCache = getLookupProjectCache(customerId, projectId);
            return projectCache.getIrlLookupValues();
        }
        return new ArrayList<>();
    }

    public LookupValue getClassificationLookupValue(Integer customerId, Integer projectId, Integer lookupId) {

        if (customerId != null && projectId != null && lookupId != null) {
            LookupProjectCache projectCache = getLookupProjectCache(customerId, projectId);
            return projectCache.getClassificationLookupValue(lookupId);
        }

        return null;
    }

    public List<ClassLookupValue> getClassificationLookupValues(Integer customerId, Integer projectId) {

        if (customerId != null && projectId != null ) {
            LookupProjectCache projectCache = getLookupProjectCache(customerId, projectId);
            return projectCache.getClassificationLookupValues();
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
