package com.bepa.eis.server.dataprovider.project;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.project.ProjectRecord;
import com.bepa.eis.common.enums.project.ProjectStatus;
import com.bepa.eis.common.providers.GenericProvider;
import com.bepa.eis.server.dataprovider.entities.ProjectEntityProvider;
import com.bepa.eis.server.entites.project.ProjectEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProjectProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(ProjectProvider.class);

    private static final String SELECT_LATEST_BY_PROJECT_ID_SQL = """
            SELECT
                [ProjectPK],
                [ProjectId],
                [Version],
                [Latest],
                [ProjectName],
                [CustomerId],
                [OwnerId],
                [CategoryId],
                [PriorityId],
                [ProjectStatus],
                [StartDate],
                [EndDate],
                [BudgetInDays],
                [BudgetInValue],
                [DepartmentId],
                [ChangedByUserId],
                [ChangedDateTime]
            FROM [dbo].[PROJECT]
            WHERE [ProjectId] = ?
              AND [Latest] = 1
            """;

    private static final String SELECT_BY_PROJECT_ID_AND_VERSION_SQL = """
            SELECT
                [ProjectPK],
                [ProjectId],
                [Version],
                [Latest],
                [ProjectName],
                [CustomerId],
                [OwnerId],
                [CategoryId],
                [PriorityId],
                [ProjectStatus],
                [StartDate],
                [EndDate],
                [BudgetInDays],
                [BudgetInValue],
                [DepartmentId],
                [ChangedByUserId],
                [ChangedDateTime]
            FROM [dbo].[PROJECT]
            WHERE [ProjectId] = ?
              AND [Version] = ?
            """;

    private static final String SELECT_LATEST_BY_CUSTOMER_ID_SQL = """
            SELECT
                [ProjectPK],
                [ProjectId],
                [Version],
                [Latest],
                [ProjectName],
                [CustomerId],
                [OwnerId],
                [CategoryId],
                [PriorityId],
                [ProjectStatus],
                [StartDate],
                [EndDate],
                [BudgetInDays],
                [BudgetInValue],
                [DepartmentId],
                [ChangedByUserId],
                [ChangedDateTime]
            FROM [dbo].[PROJECT]
            WHERE [CustomerId] = ?
              AND [Latest] = 1
            ORDER BY [ProjectName] ASC, [ProjectId] ASC
            """;

    private static final String SELECT_LATEST_BY_CUSTOMER_AND_USER_ID_SQL = """
            SELECT
                P.[ProjectPK],
                P.[ProjectId],
                P.[Version],
                P.[Latest],
                P.[ProjectName],
                P.[CustomerId],
                P.[OwnerId],
                P.[CategoryId],
                P.[PriorityId],
                P.[ProjectStatus],
                P.[StartDate],
                P.[EndDate],
                P.[BudgetInDays],
                P.[BudgetInValue],
                P.[DepartmentId],
                P.[ChangedByUserId],
                P.[ChangedDateTime]
            FROM [dbo].[PROJECT] P
            INNER JOIN [dbo].[USER_PROJECT] UP
                ON UP.ProjectId = P.ProjectId
               AND UP.UserId = ?
            WHERE P.[CustomerId] = ?
              AND P.[Latest] = 1
            ORDER BY P.[ProjectName] ASC, P.[ProjectId] ASC
            """;

    private static final String SELECT_HISTORY_BY_PROJECT_ID_SQL = """
            SELECT
                [ProjectPK],
                [ProjectId],
                [Version],
                [Latest],
                [ProjectName],
                [CustomerId],
                [OwnerId],
                [CategoryId],
                [PriorityId],
                [ProjectStatus],
                [StartDate],
                [EndDate],
                [BudgetInDays],
                [BudgetInValue],
                [DepartmentId],
                [ChangedByUserId],
                [ChangedDateTime]
            FROM [dbo].[PROJECT]
            WHERE [ProjectId] = ?
            ORDER BY [Version] DESC
            """;

    private static final String SELECT_NEXT_PROJECT_ID_SQL = """
            SELECT ISNULL(MAX([ProjectId]), 0) + 1 AS NextProjectId
            FROM [dbo].[PROJECT]
            """;

    private static final String UPDATE_LATEST_FALSE_SQL = """
            UPDATE [dbo].[PROJECT]
            SET [Latest] = 0
            WHERE [ProjectId] = ?
              AND [Latest] = 1
            """;

    private static final String INSERT_PROJECT_SQL = """
            INSERT INTO [dbo].[PROJECT] (
                [ProjectId],
                [Version],
                [Latest],
                [ProjectName],
                [CustomerId],
                [OwnerId],
                [CategoryId],
                [PriorityId],
                [ProjectStatus],
                [StartDate],
                [EndDate],
                [BudgetInDays],
                [BudgetInValue],
                [DepartmentId],
                [ChangedByUserId],
                [ChangedDateTime]
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSUTCDATETIME())
            """;

    public ProjectProvider(WebSession webSession) {
        super(webSession);
    }

    public ProjectRecord getLatestProjectByProjectId(Integer projectId) throws SQLException {
        if (projectId == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_LATEST_BY_PROJECT_ID_SQL)) {

            setInt(statement, projectId, 1);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapProjectRecord(resultSet);
                }
            }
        }

        return null;
    }

    public ProjectRecord getProjectByProjectIdAndVersion(
            Integer projectId,
            Integer version
    ) throws SQLException {
        if (projectId == null || version == null) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_PROJECT_ID_AND_VERSION_SQL)) {

            setInt(statement, projectId, 1);
            setInt(statement, version, 2);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapProjectRecord(resultSet);
                }
            }
        }

        return null;
    }

    public List<ProjectRecord> getLatestProjectsByCustomerId(Integer customerId) throws SQLException {
        List<ProjectRecord> projects = new ArrayList<>();

        if (customerId == null) {
            return projects;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_LATEST_BY_CUSTOMER_ID_SQL)) {

            setInt(statement, customerId, 1);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    projects.add(mapProjectRecord(resultSet));
                }
            }
        }

        return projects;
    }

    public List<ProjectRecord> getLatestProjectsByCustomerAndUserId(
            Integer customerId,
            Integer userId
    ) throws SQLException {
        List<ProjectRecord> projects = new ArrayList<>();

        if (customerId == null || userId == null) {
            return projects;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_LATEST_BY_CUSTOMER_AND_USER_ID_SQL)) {

            setInt(statement, userId, 1);
            setInt(statement, customerId, 2);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    projects.add(mapProjectRecord(resultSet));
                }
            }
        }

        return projects;
    }

    public ProjectRecord persist( ProjectEntity projectEntity, ProjectRecord projectRecord) throws Exception {
        validateProjectRecord(projectRecord);

        if (!projectRecord.hasValidProjectId()) {
            projectRecord.setNextProjectId(getNextProjectId());
        }

        ProjectEntity enrichedProjectEntity =  enrichProjectEntity(projectEntity, projectRecord);

        ProjectEntityProvider projectEntityProvider = new ProjectEntityProvider(getWebSession());
        projectEntityProvider.persist(enrichedProjectEntity);

        ProjectRecord persistedProject = null;
        return persistedProject;
    }

    public void persistProject(ProjectRecord projectRecord) throws SQLException {
        ProjectRecord persistedProject;
        if (projectRecord.hasValidProjectId()) {
            persistedProject = updateProject(getDataSource().getConnection(), projectRecord);
        } else {
            persistedProject = createProject(getDataSource().getConnection(), projectRecord);
        }

    }

    private ProjectRecord createProject(
            Connection connection,
            ProjectRecord projectRecord
    ) throws SQLException {

        projectRecord.setProjectId(projectRecord.getNextProjectId());
        projectRecord.setVersion(1);
        projectRecord.setLatest(true);
        projectRecord.setChangedByUserId(resolveChangedByUserId(projectRecord));

        Integer projectPK = insertProject(connection, projectRecord);
        projectRecord.setProjectPK(projectPK);

        log.info(
                "Created project. projectId={}, projectPK={}, version={}",
                projectRecord.getProjectId(),
                projectRecord.getProjectPK(),
                projectRecord.getVersion()
        );

        InstallDefaultConfiguration installDefaultConfiguration = new InstallDefaultConfiguration(getWebSession());
        installDefaultConfiguration.installDefaultTrlConfiguration(projectRecord.getCustomerId(), projectRecord.getProjectId());
        installDefaultConfiguration.installDefaultIrlConfiguration(projectRecord.getCustomerId(), projectRecord.getProjectId());
        installDefaultConfiguration.installDefaultSrlConfiguration(projectRecord.getCustomerId(), projectRecord.getProjectId());
        installDefaultConfiguration.installDefaultClassConfiguration(projectRecord.getCustomerId(), projectRecord.getProjectId());

        return getLatestProjectByProjectId(connection, projectRecord.getProjectId());
    }

    private ProjectRecord updateProject(
            Connection connection,
            ProjectRecord projectRecord
    ) throws SQLException {
        ProjectRecord latestProject = getLatestProjectByProjectId(
                connection,
                projectRecord.getProjectId()
        );

        if (latestProject == null) {
            throw new SQLException("No latest project found for projectId: " + projectRecord.getProjectId());
        }

        projectRecord.setVersion(latestProject.getVersion() + 1);
        projectRecord.setLatest(true);
        projectRecord.setChangedByUserId(resolveChangedByUserId(projectRecord));

        markLatestAsHistorical(connection, projectRecord.getProjectId());

        Integer projectPK = insertProject(connection, projectRecord);
        projectRecord.setProjectPK(projectPK);

        log.info("Updated project. projectId={}, projectPK={}, version={}",
                 projectRecord.getProjectId(),
                 projectRecord.getProjectPK(),
                 projectRecord.getVersion()
        );

        return getLatestProjectByProjectId(connection, projectRecord.getProjectId());
    }

    private Integer getNextProjectId() throws SQLException {
        try (PreparedStatement statement = getDataSource().getConnection().prepareStatement(SELECT_NEXT_PROJECT_ID_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return resultSet.getInt("NextProjectId");
            }
        }

        return 1;
    }

    private void markLatestAsHistorical(
            Connection connection,
            Integer projectId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_LATEST_FALSE_SQL)) {
            setInt(statement, projectId, 1);
            statement.executeUpdate();
        }
    }

    private Integer insertProject(
            Connection connection,
            ProjectRecord projectRecord
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                INSERT_PROJECT_SQL,
                Statement.RETURN_GENERATED_KEYS
        )) {
            int index = 1;

            setInt(statement, projectRecord.getProjectId(), index++);
            setInt(statement, projectRecord.getVersion(), index++);
            statement.setBoolean(index++, projectRecord.isLatest());
            setString(statement, projectRecord.getProjectName(), index++);
            setInt(statement, projectRecord.getCustomerId(), index++);
            setInt(statement, projectRecord.getOwnerId(), index++);
            setInt(statement, projectRecord.getCategoryId(), index++);
            setInt(statement, projectRecord.getPriorityId(), index++);
            setInt(statement, projectRecord.getProjectStatusId(), index++);

            setLocalDate(statement, projectRecord.getStartDate(), index++);
            setLocalDate(statement, projectRecord.getEndDate(), index++);

            setInt(statement, projectRecord.getBudgetInDays(), index++);

            if (projectRecord.getBudgetInValue() == null) {
                statement.setNull(index++, Types.DECIMAL);
            } else {
                statement.setBigDecimal(index++, projectRecord.getBudgetInValue());
            }

            setInt(statement, projectRecord.getDepartmentId(), index++);
            setInt(statement, projectRecord.getChangedByUserId(), index);

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }

        throw new SQLException("Could not insert project record.");
    }

    private ProjectRecord getLatestProjectByProjectId(
            Connection connection,
            Integer projectId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_LATEST_BY_PROJECT_ID_SQL)) {
            setInt(statement, projectId, 1);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapProjectRecord(resultSet);
                }
            }
        }

        return null;
    }

    private void validateProjectRecord(ProjectRecord projectRecord) {
        if (projectRecord == null) {
            throw new IllegalArgumentException("Project record is required.");
        }

        if (!projectRecord.hasProjectName()) {
            throw new IllegalArgumentException("Project name is required.");
        }

        if (projectRecord.getCustomerId() == null) {
            throw new IllegalArgumentException("Customer id is required.");
        }

        if (projectRecord.getProjectStatus() == null) {
            projectRecord.setProjectStatus(ProjectStatus.CREATED);
        }
    }

    private Integer resolveChangedByUserId(ProjectRecord projectRecord) {
        if (projectRecord.getChangedByUserId() != null) {
            return projectRecord.getChangedByUserId();
        }

        if (getWebSession() != null && getWebSession().getUserId() != null) {
            return getWebSession().getUserId();
        }

        return 1;
    }

    private ProjectRecord mapProjectRecord(ResultSet resultSet) throws SQLException {
        ProjectRecord projectRecord = new ProjectRecord();

        projectRecord.setProjectPK(resultSet.getInt("ProjectPK"));
        projectRecord.setProjectId(resultSet.getInt("ProjectId"));
        projectRecord.setVersion(resultSet.getInt("Version"));
        projectRecord.setLatest(resultSet.getBoolean("Latest"));

        projectRecord.setProjectName(resultSet.getString("ProjectName"));
        projectRecord.setCustomerId(getNullableInteger(resultSet, "CustomerId"));
        projectRecord.setOwnerId(getNullableInteger(resultSet, "OwnerId"));
        projectRecord.setCategoryId(getNullableInteger(resultSet, "CategoryId"));
        projectRecord.setPriorityId(getNullableInteger(resultSet, "PriorityId"));
        projectRecord.setProjectStatusId(getNullableInteger(resultSet, "ProjectStatus"));
        projectRecord.setStartDate(getNullableLocalDate(resultSet, "StartDate"));
        projectRecord.setEndDate(getNullableLocalDate(resultSet, "EndDate"));
        projectRecord.setBudgetInDays(getNullableInteger(resultSet, "BudgetInDays"));
        projectRecord.setBudgetInValue(resultSet.getBigDecimal("BudgetInValue"));
        projectRecord.setDepartmentId(getNullableInteger(resultSet, "DepartmentId"));

        projectRecord.setChangedByUserId(getNullableInteger(resultSet, "ChangedByUserId"));
        projectRecord.setChangedDateTime(resultSet.getTimestamp("ChangedDateTime"));

        return projectRecord;
    }

    private LocalDate getNullableLocalDate(
            ResultSet resultSet,
            String columnName
    ) throws SQLException {
        Date date = resultSet.getDate(columnName);
        return resultSet.wasNull() ? null : date.toLocalDate();
    }

    private void setLocalDate(
            PreparedStatement statement,
            LocalDate value,
            int index
    ) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.DATE);
        } else {
            statement.setDate(index, Date.valueOf(value));
        }
    }

    private Integer getNullableInteger(
            ResultSet resultSet,
            String columnName
    ) throws SQLException {
        int value = resultSet.getInt(columnName);

        return resultSet.wasNull() ? null : value;
    }

    private ProjectEntity enrichProjectEntity(ProjectEntity projectEntity,ProjectRecord projectRecord) {

        Integer projectId = projectRecord.hasValidProjectId() ? projectRecord.getProjectId() : projectRecord.getNextProjectId();

        projectEntity.setCustomerId(projectRecord.getCustomerId());
        projectEntity.setProjectId(projectId);
        projectEntity.setVersion(projectRecord.getVersion());
        projectEntity.setEntityId(projectRecord.getProjectId());
        projectEntity.setChangedByUserId(projectRecord.getChangedByUserId());
        projectEntity.setDateOfChange(projectRecord.getChangedDateTime());
        projectEntity.setActive(projectRecord.getProjectStatus().isActiveStatus());
        projectEntity.setProjectName(projectRecord.getProjectName());

        projectEntity.setProjectProvider(this, projectRecord);

        return projectEntity;

    }


    private void createOrUpdateProjectEntity(Connection connection, ProjectRecord projectRecord) throws Exception {

        ProjectEntity projectEntity = new ProjectEntity(getWebSession());

        projectEntity.setCustomerId(projectRecord.getCustomerId());
        projectEntity.setProjectId(projectRecord.getProjectId());
        projectEntity.setVersion(projectRecord.getVersion());
        projectEntity.setEntityId(projectRecord.getProjectId());
        projectEntity.setChangedByUserId(projectRecord.getChangedByUserId());
        projectEntity.setDateOfChange(projectRecord.getChangedDateTime());
        projectEntity.setActive(projectRecord.getProjectStatus().isActiveStatus());

/*
        projectEntity.parseNote
        parseNoteDocument(projectEntity, noteSection);
        parseAttachmentDocument(projectEntity, attachmentSection);
*/
        ProjectEntityProvider projectEntityProvider = new ProjectEntityProvider(getWebSession());
        projectEntityProvider.persist(projectEntity);

    }


}
