package com.bepa.eis.common.providers.security;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class LoginActivityLogger {

    private final Connection connection;

    public LoginActivityLogger(Connection connection) {
        this.connection = connection;
    }

    public void logLoginAttempt(LoginAttempt attempt) throws SQLException {
        String sql = """
                INSERT INTO [dbo].[USER_LOGIN_ACTIVITY] (
                    UserId,
                    Email,
                    SessionId,
                    LoginTime,
                    Success,
                    FailureReason,
                    IpAddress,
                    UserAgent,
                    CountryCode,
                    CountryName,
                    RegionName,
                    City,
                    Latitude,
                    Longitude,
                    MfaRequired,
                    MfaPassed,
                    RiskScore,
                    RiskReason
                )
                VALUES (?, ?, ?, GETDATE(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableInt(statement, 1, attempt.userId());
            statement.setString(2, attempt.email());
            statement.setString(3, attempt.sessionId());
            statement.setBoolean(4, attempt.success());
            statement.setString(5, attempt.failureReason());
            statement.setString(6, attempt.ipAddress());
            statement.setString(7, attempt.userAgent());
            statement.setString(8, attempt.countryCode());
            statement.setString(9, attempt.countryName());
            statement.setString(10, attempt.regionName());
            statement.setString(11, attempt.city());
            setNullableDouble(statement, 12, attempt.latitude());
            setNullableDouble(statement, 13, attempt.longitude());
            statement.setBoolean(14, attempt.mfaRequired());
            setNullableBoolean(statement, 15, attempt.mfaPassed());
            setNullableInt(statement, 16, attempt.riskScore());
            statement.setString(17, attempt.riskReason());

            statement.executeUpdate();
        }
    }

    public boolean tryLogLoginAttempt(LoginAttempt attempt) {
        try {
            logLoginAttempt(attempt);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private void setNullableInt(
            PreparedStatement statement,
            int parameterIndex,
            Integer value
    ) throws SQLException {
        if (value == null) {
            statement.setNull(parameterIndex, Types.INTEGER);
        } else {
            statement.setInt(parameterIndex, value);
        }
    }

    private void setNullableDouble(
            PreparedStatement statement,
            int parameterIndex,
            Double value
    ) throws SQLException {
        if (value == null) {
            statement.setNull(parameterIndex, Types.DECIMAL);
        } else {
            statement.setDouble(parameterIndex, value);
        }
    }

    private void setNullableBoolean(
            PreparedStatement statement,
            int parameterIndex,
            Boolean value
    ) throws SQLException {
        if (value == null) {
            statement.setNull(parameterIndex, Types.BIT);
        } else {
            statement.setBoolean(parameterIndex, value);
        }
    }

    public record LoginAttempt(
            Integer userId,
            String email,
            String sessionId,
            boolean success,
            String failureReason,
            String ipAddress,
            String userAgent,
            String countryCode,
            String countryName,
            String regionName,
            String city,
            Double latitude,
            Double longitude,
            boolean mfaRequired,
            Boolean mfaPassed,
            Integer riskScore,
            String riskReason
    ) {
        public static LoginAttempt successful(
                Integer userId,
                String email,
                String sessionId,
                String ipAddress,
                String userAgent,
                GeoIpService.GeoIpResult geoIpResult,
                boolean mfaRequired,
                Boolean mfaPassed
        ) {
            GeoIpService.GeoIpResult safeGeoIpResult = geoIpResult != null
                    ? geoIpResult
                    : GeoIpService.GeoIpResult.unknown();

            return new LoginAttempt(
                    userId,
                    email,
                    sessionId,
                    true,
                    null,
                    ipAddress,
                    userAgent,
                    safeGeoIpResult.countryCode(),
                    safeGeoIpResult.countryName(),
                    safeGeoIpResult.regionName(),
                    safeGeoIpResult.city(),
                    safeGeoIpResult.latitude(),
                    safeGeoIpResult.longitude(),
                    mfaRequired,
                    mfaPassed,
                    null,
                    null
            );
        }

        public static LoginAttempt failed(
                Integer userId,
                String email,
                String sessionId,
                String failureReason,
                String ipAddress,
                String userAgent,
                GeoIpService.GeoIpResult geoIpResult,
                Integer riskScore,
                String riskReason
        ) {
            GeoIpService.GeoIpResult safeGeoIpResult = geoIpResult != null
                    ? geoIpResult
                    : GeoIpService.GeoIpResult.unknown();

            return new LoginAttempt(
                    userId,
                    email,
                    sessionId,
                    false,
                    failureReason,
                    ipAddress,
                    userAgent,
                    safeGeoIpResult.countryCode(),
                    safeGeoIpResult.countryName(),
                    safeGeoIpResult.regionName(),
                    safeGeoIpResult.city(),
                    safeGeoIpResult.latitude(),
                    safeGeoIpResult.longitude(),
                    false,
                    null,
                    riskScore,
                    riskReason
            );
        }
    }
}