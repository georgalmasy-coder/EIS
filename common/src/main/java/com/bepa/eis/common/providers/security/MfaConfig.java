package com.bepa.eis.common.providers.security;

import com.bepa.eis.common.GlobalConfiguration;

public final class MfaConfig {

    private static final String DEFAULT_ISSUER = "BEPA EIS";
    private static final String DEFAULT_ISSUER_DEV = "BEPA EIS DEV";

    private static final MfaMode DEFAULT_MFA_MODE = MfaMode.OPTIONAL;
    private static final CustomerMfaPolicy DEFAULT_CUSTOMER_MFA_POLICY = CustomerMfaPolicy.OPTIONAL;

    private static final int DEFAULT_CODE_LENGTH = 6;
    private static final int DEFAULT_CODE_VALID_SECONDS = 30;
    private static final int DEFAULT_PRE_AUTH_TOKEN_VALID_MINUTES = 5;
    private static final int DEFAULT_MAX_VERIFICATION_ATTEMPTS = 5;
    private static final int DEFAULT_LOCKOUT_MINUTES = 10;
    private static final int DEFAULT_RECOVERY_CODE_COUNT = 10;
    private static final int DEFAULT_ALLOWED_TIME_WINDOW_DRIFT = 1;
    private static final int DEFAULT_SECRET_BYTES = 20;

    private MfaConfig() {
    }

    public static String getIssuer() {
        if (isDevelopmentMode()) {
            return GlobalConfiguration.getString("mfa.issuer.dev", DEFAULT_ISSUER_DEV);
        }

        return GlobalConfiguration.getString("mfa.issuer", DEFAULT_ISSUER);
    }

    public static MfaMode getMfaMode() {
        Object value = GlobalConfiguration.getEnum("mfa.mode", MfaMode.class, DEFAULT_MFA_MODE);

        if (value instanceof MfaMode) {
            return (MfaMode) value;
        }

        return DEFAULT_MFA_MODE;
    }

    public static boolean isMfaDisabled() {
        return getMfaMode() == MfaMode.DISABLED;
    }

    public static boolean isMfaOptional() {
        return getMfaMode() == MfaMode.OPTIONAL;
    }

    public static boolean isMfaRequiredGlobally() {
        return getMfaMode() == MfaMode.REQUIRED;
    }

    public static int getCodeLength() {
        return GlobalConfiguration.getInt("mfa.code.length", DEFAULT_CODE_LENGTH, 4, 10);
    }

    public static int getCodeValidSeconds() {
        return GlobalConfiguration.getInt(
                "mfa.code.valid.seconds",
                DEFAULT_CODE_VALID_SECONDS,
                10,
                300
        );
    }

    public static int getPreAuthTokenValidMinutes() {
        return GlobalConfiguration.getInt(
                "mfa.pre.auth.token.valid.minutes",
                DEFAULT_PRE_AUTH_TOKEN_VALID_MINUTES,
                1,
                60
        );
    }

    public static int getMaxVerificationAttempts() {
        return GlobalConfiguration.getInt(
                "mfa.max.verification.attempts",
                DEFAULT_MAX_VERIFICATION_ATTEMPTS,
                1,
                20
        );
    }

    public static int getLockoutMinutes() {
        return GlobalConfiguration.getInt(
                "mfa.lockout.minutes",
                DEFAULT_LOCKOUT_MINUTES,
                1,
                1440
        );
    }

    public static int getRecoveryCodeCount() {
        return GlobalConfiguration.getInt(
                "mfa.recovery.code.count",
                DEFAULT_RECOVERY_CODE_COUNT,
                1,
                50
        );
    }

    public static int getAllowedTimeWindowDrift() {
        return GlobalConfiguration.getInt(
                "mfa.allowed.time.window.drift",
                DEFAULT_ALLOWED_TIME_WINDOW_DRIFT,
                0,
                10
        );
    }

    public static int getSecretBytes() {
        return GlobalConfiguration.getInt(
                "mfa.secret.bytes",
                DEFAULT_SECRET_BYTES,
                16,
                64
        );
    }

    public static boolean isDevelopmentMode() {
        return GlobalConfiguration.isUdvMode();
    }

    public static boolean isMfaRequired(
            UserMfaPolicy userMfaPolicy,
            boolean userMfaEnabled
    ) {
        return isMfaRequired(
                DEFAULT_CUSTOMER_MFA_POLICY,
                userMfaPolicy,
                userMfaEnabled
        );
    }

    public static boolean isMfaRequired(
            CustomerMfaPolicy customerMfaPolicy,
            UserMfaPolicy userMfaPolicy,
            boolean userMfaEnabled
    ) {
        MfaMode mfaMode = getMfaMode();

        if (mfaMode == MfaMode.DISABLED) {
            return false;
        }

        if (mfaMode == MfaMode.REQUIRED) {
            return true;
        }

        CustomerMfaPolicy safeCustomerMfaPolicy = customerMfaPolicy == null
                ? DEFAULT_CUSTOMER_MFA_POLICY
                : customerMfaPolicy;

        UserMfaPolicy safeUserMfaPolicy = userMfaPolicy == null
                ? UserMfaPolicy.DEFAULT
                : userMfaPolicy;

        if (safeUserMfaPolicy == UserMfaPolicy.DISABLED) {
            return false;
        }

        if (safeUserMfaPolicy == UserMfaPolicy.REQUIRED) {
            return true;
        }

        if (safeCustomerMfaPolicy == CustomerMfaPolicy.DISABLED) {
            return false;
        }

        if (safeCustomerMfaPolicy == CustomerMfaPolicy.REQUIRED) {
            return true;
        }

        if (userMfaEnabled) {
            return true;
        }

        return false;
    }

    public static CustomerMfaPolicy parseCustomerMfaPolicy(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_CUSTOMER_MFA_POLICY;
        }

        try {
            return CustomerMfaPolicy.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return DEFAULT_CUSTOMER_MFA_POLICY;
        }
    }

    public enum MfaMode {
        DISABLED,
        OPTIONAL,
        REQUIRED
    }

    public enum CustomerMfaPolicy {
        DISABLED,
        OPTIONAL,
        REQUIRED
    }

    public enum UserMfaPolicy {
        DEFAULT,
        REQUIRED,
        DISABLED
    }
}