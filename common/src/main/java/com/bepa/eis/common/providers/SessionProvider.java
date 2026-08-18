package com.bepa.eis.common.providers;

import com.bepa.eis.common.dto.WebSession;
import java.sql.*;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(SessionProvider.class);

    private static final String DELETE_SESSION_SQL = """
            DELETE FROM [dbo].[SESSION]
            WHERE SessionId = ?
            """;

    private static final String SELECT_SESSION_BY_SESSION_ID_SQL = """
            SELECT
                ID,
                SessionId,
                CustomerId,
                ProjectId,
                U.UserId,
                U.ThemeId,
                S.Created,
                LastAccessed,
                IpAddress,
                UserAgent,
                CountryCode,
                CountryName,
                RegionName,
                City,
                Latitude,
                Longitude,
                LoginAt,
                LogoutAt,
                ExpiredAt,
                EndedReason
            FROM [dbo].[SESSION] S
            LEFT JOIN [dbo].[USERS] U
                ON U.UserId = S.UserId
            WHERE S.SessionId = ?
            """;

    private static final String UPDATE_SESSION_INFO_SQL = """
            UPDATE [dbo].[SESSION]
            SET
                CustomerId = ?,
                ProjectId = ?,
                UserId = (SELECT TOP 1 UserId FROM [dbo].[USERS] WHERE Email = ?),
                LastAccessed = GETDATE()
            WHERE SessionId = ?
            """;

    private static final String UPSERT_SESSION_SQL = """
            MERGE INTO [dbo].[SESSION] WITH (HOLDLOCK) AS tgt
            USING (
                VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                )
            ) AS src (
                SessionId,
                CustomerId,
                ProjectId,
                UserId,
                IpAddress,
                UserAgent,
                CountryCode,
                CountryName,
                RegionName,
                City,
                Latitude,
                Longitude,
                LoginAt,
                LastAccessed
            )
            ON tgt.SessionId = src.SessionId
            WHEN MATCHED THEN
                UPDATE SET
                    tgt.CustomerId = src.CustomerId,
                    tgt.ProjectId = src.ProjectId,
                    tgt.UserId = src.UserId,
                    tgt.IpAddress = src.IpAddress,
                    tgt.UserAgent = src.UserAgent,
                    tgt.CountryCode = src.CountryCode,
                    tgt.CountryName = src.CountryName,
                    tgt.RegionName = src.RegionName,
                    tgt.City = src.City,
                    tgt.Latitude = src.Latitude,
                    tgt.Longitude = src.Longitude,
                    tgt.LoginAt = ISNULL(tgt.LoginAt, src.LoginAt),
                    tgt.LastAccessed = GETDATE()
            WHEN NOT MATCHED THEN
                INSERT (
                    SessionId,
                    CustomerId,
                    ProjectId,
                    UserId,
                    Created,
                    LastAccessed,
                    IpAddress,
                    UserAgent,
                    CountryCode,
                    CountryName,
                    RegionName,
                    City,
                    Latitude,
                    Longitude,
                    LoginAt
                )
                VALUES (
                    src.SessionId,
                    src.CustomerId,
                    src.ProjectId,
                    src.UserId,
                    GETDATE(),
                    GETDATE(),
                    src.IpAddress,
                    src.UserAgent,
                    src.CountryCode,
                    src.CountryName,
                    src.RegionName,
                    src.City,
                    src.Latitude,
                    src.Longitude,
                    ISNULL(src.LoginAt, GETDATE())
                );
            """;

    private static final String END_SESSION_SQL = """
            UPDATE [dbo].[SESSION]
            SET
                LastAccessed = GETDATE(),
                LogoutAt = CASE WHEN ? = 'LOGOUT' THEN GETDATE() ELSE LogoutAt END,
                ExpiredAt = CASE WHEN ? <> 'LOGOUT' THEN GETDATE() ELSE ExpiredAt END,
                EndedReason = ?
            WHERE SessionId = ?
            """;

    private static final String VERIFY_PASSWORD_SQL = """
            SELECT [Password]
            FROM [dbo].[USERS]
            WHERE Email = ?
            """;

    public SessionProvider(WebSession webSession) {
        super(webSession);
    }

    public boolean deleteBySessionId(String sessionId) throws SQLException {
        if (sessionId == null || sessionId.isBlank()) {
            return false;
        }

        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(DELETE_SESSION_SQL)) {

            ps.setString(1, sessionId);
            return ps.executeUpdate() > 0;
        }
    }

    public WebSession getBySessionId(String sessionId) throws SQLException {
        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_SESSION_BY_SESSION_ID_SQL)) {

            ps.setString(1, sessionId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("No session found for sessionId: " + sessionId);
                }

                return mapWebSession(rs);
            }
        }
    }

    public boolean upsertSession(WebSession webSession) throws SQLException {
        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(UPSERT_SESSION_SQL)) {

            ps.setString(1, webSession.getSessionId());
            setNullableInt(ps, 2, webSession.getCustomerId());
            setNullableInt(ps, 3, webSession.getProjectId());
            setNullableInt(ps, 4, webSession.getUserId());
            ps.setString(5, webSession.getIpAddress());
            ps.setString(6, webSession.getUserAgent());
            ps.setString(7, webSession.getCountryCode());
            ps.setString(8, webSession.getCountryName());
            ps.setString(9, webSession.getRegionName());
            ps.setString(10, webSession.getCity());
            setNullableDouble(ps, 11, webSession.getLatitude());
            setNullableDouble(ps, 12, webSession.getLongitude());
            setNullableTimestamp(ps, 13, webSession.getLoginAt());
            setNullableTimestamp(ps, 14, webSession.getLastAccessed());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateSessionInfo(WebSession webSession) throws SQLException {
        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(UPDATE_SESSION_INFO_SQL)) {

            setNullableInt(ps, 1, webSession.getCustomerId());
            setNullableInt(ps, 2, webSession.getProjectId());
            ps.setString(3, webSession.getSessionId());
            ps.setString(4, webSession.getSessionId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean endSession(String sessionId, String endedReason) throws SQLException {
        if (sessionId == null || sessionId.isBlank()) {
            return false;
        }

        String safeEndedReason = endedReason == null || endedReason.isBlank()
                ? "UNKNOWN"
                : endedReason;

        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(END_SESSION_SQL)) {

            ps.setString(1, safeEndedReason);
            ps.setString(2, safeEndedReason);
            ps.setString(3, safeEndedReason);
            ps.setString(4, sessionId);

            return ps.executeUpdate() > 0;
        }
    }

    public boolean validateAgainstDb(String email, String passwordInput) {
        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(VERIFY_PASSWORD_SQL)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs != null && rs.next()) {
                    String passwordFromDb = rs.getString("Password");

                    if (passwordFromDb != null) {
                        return passwordFromDb.equals(passwordInput);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error validating password: {}", e.getMessage(), e);
        }

        return false;
    }

    private WebSession mapWebSession(ResultSet rs) throws SQLException {
        WebSession ws = new WebSession();

        ws.setId(rs.getInt("ID"));
        ws.setSessionId(rs.getString("SessionId"));
        ws.setCustomerId(getNullableInt(rs, "CustomerId"));
        ws.setProjectId(getNullableInt(rs, "ProjectId"));
        ws.setUserId(rs.getInt("UserId"));
        ws.setThemeId(getNullableInt(rs, "ThemeId"));
        ws.setCreated(toDate(rs.getTimestamp("Created")));
        ws.setLastAccessed(toDate(rs.getTimestamp("LastAccessed")));
        ws.setIpAddress(rs.getString("IpAddress"));
        ws.setUserAgent(rs.getString("UserAgent"));
        ws.setCountryCode(rs.getString("CountryCode"));
        ws.setCountryName(rs.getString("CountryName"));
        ws.setRegionName(rs.getString("RegionName"));
        ws.setCity(rs.getString("City"));
        ws.setLatitude(getNullableDouble(rs, "Latitude"));
        ws.setLongitude(getNullableDouble(rs, "Longitude"));
        ws.setLoginAt(toDate(rs.getTimestamp("LoginAt")));
        ws.setLogoutAt(toDate(rs.getTimestamp("LogoutAt")));
        ws.setExpiredAt(toDate(rs.getTimestamp("ExpiredAt")));
        ws.setEndedReason(rs.getString("EndedReason"));

        return ws;
    }

    private Date toDate(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }

        return new Date(timestamp.getTime());
    }

    private Double getNullableDouble(ResultSet rs, String columnName) throws SQLException {
        double value = rs.getDouble(columnName);

        if (rs.wasNull()) {
            return null;
        }

        return value;
    }

    private Integer getNullableInt(ResultSet rs, String columnName) throws SQLException {
        int intValue = rs.getInt(columnName);

        if (rs.wasNull()) {
            return null;
        }

        return intValue;
    }

    private void setNullableInt(
            PreparedStatement ps,
            int parameterIndex,
            Integer value
    ) throws SQLException {
        if (value == null) {
            ps.setNull(parameterIndex, Types.INTEGER);
        } else {
            ps.setInt(parameterIndex, value);
        }
    }

    private void setNullableDouble(
            PreparedStatement ps,
            int parameterIndex,
            Double value
    ) throws SQLException {
        if (value == null) {
            ps.setNull(parameterIndex, Types.DECIMAL);
        } else {
            ps.setDouble(parameterIndex, value);
        }
    }

    private void setNullableTimestamp(
            PreparedStatement ps,
            int parameterIndex,
            Date value
    ) throws SQLException {
        if (value == null) {
            ps.setNull(parameterIndex, Types.TIMESTAMP);
        } else {
            ps.setTimestamp(parameterIndex, new Timestamp(value.getTime()));
        }
    }
}
