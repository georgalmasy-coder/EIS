package com.bepa.eis.common.providers;

import com.bepa.eis.common.dto.WebSession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class UserPreferenceProvider extends GenericProvider {

    private static final String SELECT_SELECTED_PROJECT_ID_SQL = """
            SELECT Preferences.value(
                '(/UserPreferences/SelectedProjectId/text())[1]',
                'int'
            ) AS SelectedProjectId
            FROM [dbo].[USER_PREFERENCE]
            WHERE UserId = ?
            """;

    private static final String UPDATE_SELECTED_PROJECT_ID_SQL = """
            DECLARE @UserId INT = ?;
            DECLARE @SelectedProjectId INT = ?;

            MERGE INTO [dbo].[USER_PREFERENCE] WITH (HOLDLOCK) AS target
            USING (SELECT @UserId AS UserId) AS source
                ON target.UserId = source.UserId
            WHEN NOT MATCHED THEN
                INSERT (UserId, Preferences, ChangedAt)
                VALUES (source.UserId, CONVERT(XML, N'<UserPreferences />'), SYSUTCDATETIME());

            IF @SelectedProjectId IS NULL
            BEGIN
                UPDATE [dbo].[USER_PREFERENCE]
                SET Preferences.modify('delete (/UserPreferences/SelectedProjectId)[1]'),
                    ChangedAt = SYSUTCDATETIME()
                WHERE UserId = @UserId;
            END
            ELSE IF EXISTS (
                SELECT 1
                FROM [dbo].[USER_PREFERENCE]
                WHERE UserId = @UserId
                  AND Preferences.exist('/UserPreferences/SelectedProjectId') = 1
            )
            BEGIN
                UPDATE [dbo].[USER_PREFERENCE]
                SET Preferences.modify(
                        'replace value of (/UserPreferences/SelectedProjectId/text())[1]
                         with sql:variable("@SelectedProjectId")'
                    ),
                    ChangedAt = SYSUTCDATETIME()
                WHERE UserId = @UserId;
            END
            ELSE
            BEGIN
                UPDATE [dbo].[USER_PREFERENCE]
                SET Preferences.modify(
                        'insert <SelectedProjectId>{sql:variable("@SelectedProjectId")}</SelectedProjectId>
                         as last into (/UserPreferences)[1]'
                    ),
                    ChangedAt = SYSUTCDATETIME()
                WHERE UserId = @UserId;
            END;
            """;

    public UserPreferenceProvider(WebSession webSession) {
        super(webSession);
    }

    public Integer getSelectedProjectId(Integer userId) throws SQLException {
        if (userId == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_SELECTED_PROJECT_ID_SQL)) {
            statement.setInt(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                int projectId = resultSet.getInt("SelectedProjectId");
                return resultSet.wasNull() ? null : projectId;
            }
        }
    }

    public void setSelectedProjectId(Integer userId, Integer projectId) throws SQLException {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SELECTED_PROJECT_ID_SQL)) {
            statement.setInt(1, userId);
            if (projectId == null) {
                statement.setNull(2, Types.INTEGER);
            } else {
                statement.setInt(2, projectId);
            }
            statement.executeUpdate();
        }
    }
}
