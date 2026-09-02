package com.bepa.eis.server.api.web.application.cache;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.user.UserRoles;
import com.bepa.eis.server.api.DTO.User;
import com.bepa.eis.server.dataprovider.cache.EhcacheProvider;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

public class CustomerLookupCache {

    private static final Logger log = LoggerFactory.getLogger(CustomerLookupCache.class);

    private static final String CACHE_ALIAS = EhcacheProvider.LOOKUP_CACHE_ALIAS;
    private static final ReentrantLock BULK_LOAD_LOCK = new ReentrantLock();
    private static final List<PhoneCountryRule> PHONE_COUNTRY_RULES = buildPhoneCountryRules();

    public static CustomerBasisInfo getCustomerInfo(WebSession webSession) {
        if (webSession != null && webSession.getCustomerId() != null) {
            return getLookupCache(webSession).getCustomerInfo();
        }
        return null;
    }

    public static ProjectBasisInfo getProjectInfo(WebSession webSession) {
        if (webSession != null && webSession.getCustomerId() != null && webSession.getProjectId() != null) {
            return getLookupCache(webSession.getCustomerId(), webSession.getProjectId()).getProjectInfo(webSession.getCustomerId(), webSession.getProjectId());
        }
        return null;
    }

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
        return getIrlLookupValue(webSession.getCustomerId(), webSession.getProjectId(), lookupId);
    }

    public static LookupValue getTrlLookupValue(Integer customerId, Integer projectId, Integer lookupId) {
        return getLookupCache(customerId, projectId).getTrlLookupValue(customerId, projectId, lookupId);
    }

    public static List<LookupValue> getTrlLookupValues(WebSession webSession) {
        return getLookupCache(webSession).getTrlLookupValues(webSession.getCustomerId(), webSession.getProjectId());
    }

    public static LookupValue getIrlLookupValue(WebSession webSession, Integer lookupId) {
        return getIrlLookupValue(webSession.getCustomerId(), webSession.getProjectId(), lookupId);
    }

    public static LookupValue getIrlLookupValue(Integer customerId, Integer projectId, Integer lookupId) {
        return getLookupCache(customerId, projectId).getIrlLookupValue(customerId, projectId, lookupId);
    }

    public static List<LookupValue> getIrlLookupValues(WebSession webSession) {
        return getLookupCache(webSession).getIrlLookupValues(webSession.getCustomerId(), webSession.getProjectId());
    }

    public static LookupValue getClassificationLookupValue(WebSession webSession, Integer lookupId) {
        return getClassificationLookupValue(webSession.getCustomerId(), webSession.getProjectId(), lookupId);
    }

    public static LookupValue getClassificationLookupValue(Integer customerId, Integer projectId, Integer lookupId) {
        return getLookupCache(customerId, projectId).getClassificationLookupValue(customerId, projectId, lookupId);
    }

    public static List<ClassLookupValue> getClassificationLookupValues(WebSession webSession) {
        return getLookupCache(webSession).getClassificationLookupValues(webSession.getCustomerId(), webSession.getProjectId());
    }

    public static LookupValue getStakeholderLookupValue(WebSession webSession, Integer lookupId) {
        return getStakeholderLookupValue(webSession.getCustomerId(), webSession.getProjectId(), lookupId);
    }

    public static LookupValue getStakeholderLookupValue(Integer customerId, Integer projectId, Integer lookupId) {
        return getLookupCache(customerId, projectId).getStakeholderLookupValue(customerId, projectId, lookupId);
    }

    public static List<LookupValue> getStakeholderLookupValues(WebSession webSession) {
        return getLookupCache(webSession).getStakeholderLookupValues(webSession.getCustomerId(), webSession.getProjectId());
    }

    public static LookupValue getProjectCategoryLookupValue(WebSession webSession, Integer categoryId) {
        return getLookupCache(webSession).getProjectCategoryLookupValue(categoryId);
    }

    public static List<LookupValue> getProjectCategoryLookupValues(WebSession webSession) {
        return getLookupCache(webSession).getProjectCategoryLookupValues();
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

    public static UserRoles getUserRole(WebSession webSession) {
        if (webSession == null) {
            return UserRoles.INVASIVE_USER_ROLE;
        }
        User user = getLookupCache(webSession).getUser(webSession.getUserId());
        if (user == null) {
            return UserRoles.INVASIVE_USER_ROLE;
        }
        return user.getUserRole();
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

    public static List<PhoneCountryRule> getPhoneCountryRules() {
        return PHONE_COUNTRY_RULES;
    }

    public static List<LookupValue> getSystemAdminUserRolesLookupValues() {
        return getRolesLookupValues(true);
    }

    public static LookupValue getCustomerAdminUserRolesLookupValue(WebSession webSession, Integer roleId) {
        UserRoles userRole = UserRoles.fromId(roleId);
        return new LookupValue(null, null, userRole.getId(), userRole.getLabel(), userRole.getDescription(), true);
    }

    public static List<LookupValue> getCustomerAdminUserRolesLookupValue() {
        return getRolesLookupValues(false);
    }

    private static List<LookupValue> getRolesLookupValues(boolean isSystemAdmin) {
        List<LookupValue> roles = new ArrayList<>();

        for (UserRoles role : UserRoles.values()) {
            if (isSystemAdmin || role.isExternalUserRole()) {
                roles.add(new LookupValue(null, null, role.getId(), role.getLabel(), role.getDescription(), true));
            }
        }
        return roles;
    }

    private static PhoneCountryRule phoneCountryRule(
            String country,
            String code,
            int minDigits,
            int maxDigits,
            String example
    ) {
        return new PhoneCountryRule(country, code, minDigits, maxDigits, example);
    }

    private static List<PhoneCountryRule> buildPhoneCountryRules() {
        List<PhoneCountryRule> rules = new ArrayList<>();

        addPhoneCountryRule(rules, 10, 10, "(123) 456-7890", "+1",
                "US", "CA", "BS", "BB", "AI", "AG", "VG", "VI", "KY", "BM",
                "GD", "TC", "JM", "MS", "MP", "GU", "AS", "SX", "LC", "DM",
                "VC", "PR", "DO", "TT", "KN"
        );

        addPhoneCountryRule(rules, 8, 8, "12 34 56 78", "+45", "DK");
        addPhoneCountryRule(rules, 7, 10, "70 123 45 67", "+46", "SE");
        addPhoneCountryRule(rules, 8, 8, "123 45 678", "+47", "NO", "SJ", "BV");
        addPhoneCountryRule(rules, 7, 13, "151 23456789", "+49", "DE");
        addPhoneCountryRule(rules, 10, 10, "7123 456789", "+44", "GB", "GG", "IM", "JE");
        addPhoneCountryRule(rules, 9, 9, "6 12 34 56 78", "+33", "FR");
        addPhoneCountryRule(rules, 9, 9, "6 12345678", "+31", "NL");
        addPhoneCountryRule(rules, 8, 9, "470 12 34 56", "+32", "BE");
        addPhoneCountryRule(rules, 9, 9, "612 34 56 78", "+34", "ES");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+30", "GR");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+36", "HU");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+350", "GI");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+355", "AL");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+356", "MT");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+357", "CY");
        addPhoneCountryRule(rules, 8, 11, "312 345 6789", "+39", "IT", "VA");
        addPhoneCountryRule(rules, 7, 10, "40 1234567", "+358", "FI", "AX");
        addPhoneCountryRule(rules, 9, 9, "123 456 789", "+48", "PL");
        addPhoneCountryRule(rules, 9, 9, "912 345 678", "+351", "PT");
        addPhoneCountryRule(rules, 9, 9, "79 123 45 67", "+41", "CH");
        addPhoneCountryRule(rules, 7, 13, "664 1234567", "+43", "AT");
        addPhoneCountryRule(rules, 7, 9, "85 123 4567", "+353", "IE");
        addPhoneCountryRule(rules, 7, 7, "123 4567", "+354", "IS");
        addPhoneCountryRule(rules, 6, 6, "123456", "+298", "FO");
        addPhoneCountryRule(rules, 6, 6, "123456", "+299", "GL");

        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+20", "EG");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+27", "ZA");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+211", "SS");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+212", "MA", "EH");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+213", "DZ");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+216", "TN");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+218", "LY");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+220", "GM");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+221", "SN");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+222", "MR");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+223", "ML");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+224", "GN");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+225", "CI");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+226", "BF");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+227", "NE");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+228", "TG");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+229", "BJ");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+230", "MU");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+231", "LR");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+232", "SL");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+233", "GH");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+234", "NG");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+235", "TD");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+236", "CF");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+237", "CM");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+238", "CV");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+239", "ST");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+240", "GQ");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+241", "GA");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+242", "CG");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+243", "CD");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+244", "AO");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+245", "GW");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+246", "IO");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+247", "AC");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+248", "SC");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+249", "SD");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+250", "RW");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+251", "ET");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+252", "SO");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+253", "DJ");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+254", "KE");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+255", "TZ");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+256", "UG");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+257", "BI");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+258", "MZ");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+260", "ZM");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+261", "MG");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+262", "RE", "YT", "TF");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+263", "ZW");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+264", "NA");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+265", "MW");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+266", "LS");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+267", "BW");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+268", "SZ");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+269", "KM");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+290", "SH", "TA");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+291", "ER");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+297", "AW");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+352", "LU");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+359", "BG");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+370", "LT");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+371", "LV");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+372", "EE");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+373", "MD");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+374", "AM");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+375", "BY");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+376", "AD");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+377", "MC");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+378", "SM");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+380", "UA");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+381", "RS");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+382", "ME");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+383", "XK");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+385", "HR");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+386", "SI");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+387", "BA");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+389", "MK");

        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+40", "RO");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+41", "CH");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+43", "AT");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+420", "CZ");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+421", "SK");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+423", "LI");

        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+500", "FK");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+501", "BZ");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+502", "GT");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+503", "SV");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+504", "HN");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+505", "NI");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+506", "CR");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+507", "PA");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+508", "PM");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+509", "HT");

        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+51", "PE");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+52", "MX");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+53", "CU");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+54", "AR");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+55", "BR");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+56", "CL");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+57", "CO");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+58", "VE");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+590", "GP", "BL", "MF");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+591", "BO");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+592", "GY");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+593", "EC");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+594", "GF");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+595", "PY");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+596", "MQ");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+597", "SR");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+598", "UY");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+599", "BQ", "CW");

        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+60", "MY");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+61", "AU", "CX", "CC");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+62", "ID");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+63", "PH");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+64", "NZ", "PN");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+65", "SG");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+66", "TH");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+670", "TL");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+672", "NF", "AQ");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+673", "BN");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+674", "NR");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+675", "PG");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+676", "TO");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+677", "SB");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+678", "VU");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+679", "FJ");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+680", "PW");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+681", "WF");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+682", "CK");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+683", "NU");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+685", "WS");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+686", "KI");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+687", "NC");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+688", "TV");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+689", "PF");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+690", "TK");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+691", "FM");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+692", "MH");

        addGenericPhoneCountryRule(rules, 10, 10, "1234567890", "+7", "RU", "KZ");

        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+81", "JP");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+82", "KR");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+84", "VN");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+86", "CN");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+880", "BD");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+886", "TW");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+850", "KP");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+852", "HK");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+853", "MO");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+855", "KH");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+856", "LA");

        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+90", "TR");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+91", "IN");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+92", "PK");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+93", "AF");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+94", "LK");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+95", "MM");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+98", "IR");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+960", "MV");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+961", "LB");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+962", "JO");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+963", "SY");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+964", "IQ");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+965", "KW");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+966", "SA");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+967", "YE");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+968", "OM");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+970", "PS");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+971", "AE");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+972", "IL", "PS");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+973", "BH");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+974", "QA");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+975", "BT");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+976", "MN");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+977", "NP");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+992", "TJ");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+993", "TM");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+994", "AZ");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+995", "GE");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+996", "KG");
        addGenericPhoneCountryRule(rules, 8, 11, "123456789", "+998", "UZ");

        rules.sort(Comparator
                .comparing(PhoneCountryRule::country, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(PhoneCountryRule::code)
        );

        return List.copyOf(rules);
    }

    private static void addPhoneCountryRule(
            List<PhoneCountryRule> rules,
            int minDigits,
            int maxDigits,
            String example,
            String code,
            String... regionCodes
    ) {
        for (String regionCode : regionCodes) {
            String countryName = regionName(regionCode);
            rules.add(phoneCountryRule(countryName, code, minDigits, maxDigits, example));
        }
    }

    private static void addGenericPhoneCountryRule(
            List<PhoneCountryRule> rules,
            int minDigits,
            int maxDigits,
            String example,
            String code,
            String... regionCodes
    ) {
        addPhoneCountryRule(rules, minDigits, maxDigits, example, code, regionCodes);
    }

    private static String regionName(String regionCode) {
        if (regionCode == null || regionCode.isBlank()) {
            return "";
        }

        return switch (regionCode.trim().toUpperCase(Locale.ROOT)) {
            case "AC" -> "Ascension Island";
            case "AQ" -> "Antarctica";
            case "AX" -> "Aland Islands";
            case "BQ" -> "Caribbean Netherlands";
            case "CT" -> "Cyprus (CT)";
            case "EH" -> "Western Sahara";
            case "GG" -> "Guernsey";
            case "IM" -> "Isle of Man";
            case "JE" -> "Jersey";
            case "MF" -> "Saint Martin";
            case "PS" -> "Palestine";
            case "RE" -> "Reunion";
            case "SH" -> "Saint Helena";
            case "SJ" -> "Svalbard and Jan Mayen";
            case "SX" -> "Sint Maarten";
            case "TA" -> "Tristan da Cunha";
            case "TF" -> "French Southern Territories";
            case "UN" -> "Universal service";
            case "VA" -> "Vatican City";
            case "XK" -> "Kosovo";
            default -> {
                String displayName = new Locale("", regionCode).getDisplayCountry(Locale.ENGLISH);
                yield displayName == null || displayName.isBlank() ? regionCode : displayName;
            }
        };
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
                throw new IllegalStateException("Failed to load cache: " + CACHE_ALIAS);
            }
        }
        return lookupCache;
    }

    private static LookupCache getLookupCache(Integer customerId, Integer projectId) {
        LookupCache lookupCache;
        try {
            lookupCache = getCache().get(customerId);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load cache: " + CACHE_ALIAS);
        }

        if (lookupCache == null) {
            reloadCache(customerId, projectId);
            lookupCache = getCache().get(customerId);
            if (lookupCache == null) {
                throw new IllegalStateException("Failed to load cache: " + CACHE_ALIAS);
            }
        }
        return lookupCache;
    }


    private static void reloadCache(WebSession webSession) {
        if ( webSession != null  ) {
            reloadCache(webSession.getCustomerId(), webSession.getProjectId());
        }
    }

    private static void reloadCache(Integer customerId, Integer projectId) {
        BULK_LOAD_LOCK.lock();
        try {
            if ( customerId != null  ) {
                getCache().put(customerId, new LookupCache(customerId, projectId));
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

    public record PhoneCountryRule(
            String country,
            String code,
            int minDigits,
            int maxDigits,
            String example
    ) {
    }
}
