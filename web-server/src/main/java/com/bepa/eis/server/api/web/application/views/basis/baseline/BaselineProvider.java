package com.bepa.eis.server.api.web.application.views.basis.baseline;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.common.providers.GenericProvider;


import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.entities.*;
import com.bepa.eis.server.dataprovider.entities.common.EntityRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.*;

import static com.bepa.eis.common.enums.entity.EntityType.*;

public class BaselineProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(BaselineProvider.class);

    private static final String SELECT_BASELINES_SQL =
            "SELECT " +
                    "BaselinePK, " +
                    "CustomerId, " +
                    "ProjectId, " +
                    "TagName, " +
                    "Description, " +
                    "ChangedByUserId, " +
                    "ChangedDateTime " +
                    "FROM [dbo].[BASELINE] " +
                    "WHERE CustomerId = ? " +
                    "  AND ProjectId = ? " +
                    "ORDER BY ChangedDateTime DESC, BaselinePK DESC";

    private static final String SELECT_BASELINE_BY_PK_SQL =
            "SELECT " +
                    "BaselinePK, " +
                    "CustomerId, " +
                    "ProjectId, " +
                    "TagName, " +
                    "Description, " +
                    "ChangedByUserId, " +
                    "ChangedDateTime " +
                    "FROM [dbo].[BASELINE] " +
                    "WHERE BaselinePK = ? " +
                    "  AND CustomerId = ? " +
                    "  AND ProjectId = ?";

    private static final String SELECT_PROJECT_CREATED_DATE_SQL =
            "SELECT TOP 1 ChangedDateTime " +
                    "FROM [dbo].[PROJECT] " +
                    "WHERE CustomerId = ? " +
                    "  AND ProjectId = ? " +
            "ORDER BY ProjectPK ASC";

    private static final String SELECT_PREV_BASELINE_BY_PK_SQL =
            "SELECT TOP 1 ChangedDateTime " +
                    "FROM [dbo].[BASELINE] " +
                    "WHERE CustomerId = ? " +
                    "  AND ProjectId = ? " +
                    "  AND BaselinePK < ? " +
                    "ORDER BY ChangedDateTime DESC";

    private static final String BASELINE_EXISTS_SQL =
            "SELECT TOP (1) BaselinePK " +
                    "FROM [dbo].[BASELINE] " +
                    "WHERE CustomerId = ? " +
                    "  AND ProjectId = ? " +
                    "  AND LOWER(LTRIM(RTRIM(TagName))) = LOWER(LTRIM(RTRIM(?)))";

    private static final String INSERT_BASELINE_SQL =
            "INSERT INTO [dbo].[BASELINE] ( " +
                    "CustomerId, " +
                    "ProjectId, " +
                    "TagName, " +
                    "Description, " +
                    "ChangedByUserId, " +
                    "ChangedDateTime " +
                    ") " +
                    "VALUES (?, ?, ?, ?, ?, SYSUTCDATETIME())";

    public BaselineProvider(WebSession webSession) {
        super(webSession);

    }

    public List<Baseline> getBaselines() {
        WebSession webSession = getWebSession();

        if (webSession == null || webSession.getCustomerId() == null || webSession.getProjectId() == null) {
            return List.of();
        }

        List<Baseline> baselines = new ArrayList<>();

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BASELINES_SQL)) {

            setInt(statement, webSession.getCustomerId(), 1);
            setInt(statement, webSession.getProjectId(), 2);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    baselines.add(toBaseline(resultSet));
                }
            }
        } catch (SQLException e) {
            log.error(
                    "Error getting baselines. customerId={}, projectId={}",
                    webSession.getCustomerId(),
                    webSession.getProjectId(),
                    e
            );
            throw new RuntimeException(e);
        }

        return baselines;
    }

    public Baseline getBaselineById(Integer baselineId) {
        WebSession webSession = getWebSession();

        if (baselineId == null || webSession == null ||
                        webSession.getCustomerId() == null || webSession.getProjectId() == null
        ) {
            return null;
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BASELINE_BY_PK_SQL)) {

            setInt(statement, baselineId, 1);
            setInt(statement, webSession.getCustomerId(), 2);
            setInt(statement, webSession.getProjectId(), 3);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Baseline baseline = toBaseline(resultSet);
                    baseline.setPreviousBaselineDateTime(getPreviousBaselineDateTime(baselineId));
                    return baseline;
                }
            }
        } catch (SQLException e) {
            log.error( "Error getting baseline. baselinePK={}, customerId={}, projectId={}",
                        baselineId,
                        webSession.getCustomerId(),
                        webSession.getProjectId(),e );
            throw new RuntimeException(e);
        }

        return null;
    }

    public Integer createBaseline(
            String tagName,
            String description
    ) {
        WebSession webSession = getWebSession();

        validateWebSession(webSession);

        String normalizedTagName = safeText(tagName);
        String normalizedDescription = safeText(description);

        validateBaselineInput(
                normalizedTagName,
                normalizedDescription
        );

        if (baselineExists(normalizedTagName)) {
            throw new IllegalArgumentException("A baseline with this tag-name already exists for the selected customer and project.");
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_BASELINE_SQL,
                     Statement.RETURN_GENERATED_KEYS
             )) {

            setInt(statement, webSession.getCustomerId(), 1);
            setInt(statement, webSession.getProjectId(), 2);
            setString(statement, normalizedTagName, 3);
            setString(statement, normalizedDescription, 4);
            setInt(statement, webSession.getUserId(), 5);

            int rows = statement.executeUpdate();

            if (rows == 0) {
                return null;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }

            return null;
        } catch (SQLException e) {
            log.error(
                    "Error creating baseline. customerId={}, projectId={}, tagName={}",
                    webSession.getCustomerId(),
                    webSession.getProjectId(),
                    normalizedTagName,
                    e
            );
            throw new RuntimeException(e);
        }
    }

    public List<BaselineChangeRow> getStakeholderRequirementChanges(Baseline baseline) {
        EntityProvider entityProvider = new StakeholderRequirementProvider(getWebSession());
        return getEntityChanges( entityProvider, STAKEHOLDER_REQUIREMENT, baseline);
    }

    public List<BaselineChangeRow> getSystemRequirementChanges(Baseline baseline) {
        EntityProvider entityProvider = new SystemRequirementProvider(getWebSession());
        return getEntityChanges( entityProvider, SYSTEM_REQUIREMENT, baseline);
    }

    public List<BaselineChangeRow> getFunctionalStructureChanges(Baseline baseline) {
        EntityProvider entityProvider = new SystemRequirementProvider(getWebSession());
        return getEntityChanges( entityProvider, FUNCTIONAL_STRUCTURE, baseline);
    }

    public List<BaselineChangeRow> getLogicalStructureChanges(Baseline baseline) {
        EntityProvider entityProvider = new SystemRequirementProvider(getWebSession());
        return getEntityChanges( entityProvider, LOGICAL_STRUCTURE, baseline);
    }

    public List<BaselineChangeRow> getPhysicalStructureChanges(Baseline baseline) {
        EntityProvider entityProvider = new SystemBreakdownProvider(getWebSession());
        return getEntityChanges( entityProvider, SYSTEMS_BREAKDOWN, baseline);
    }

    private List<BaselineChangeRow> getEntityChanges(EntityProvider entityProvider, EntityType entityType, Baseline baseline) {

        List<EntityRecord> entityRecords;
        List<BaselineChangeRow> listOfChanges = new ArrayList<>();

        try {
            entityRecords = entityProvider.getEntityRecords(entityType, baseline);

            Map<Integer, BaselineChangeRow> mapOfChanges = new HashMap<>();

            for (EntityRecord entityRecord : entityRecords) {

                BaselineChangeRow changeRow = mapOfChanges.get(entityRecord.getEntityId());

                if ( changeRow == null ) {
                    changeRow = new BaselineChangeRow(entityType, entityRecord.getEntityId(), entityRecord.getVersion());
                    mapOfChanges.put(entityRecord.getEntityId(), changeRow);
                }

                if (entityRecord.getVersion() == 1) {
                    changeRow.setNew();
                } else {
                    changeRow.setActive(entityRecord.isActive());
                }

                changeRow.setLastModified(entityRecord.getChangedDateTime());
                changeRow.setLastModifiedById(entityRecord.getChangedByUserId());
            }

            listOfChanges = buildListOfDecoratedAndSortedChanges( entityProvider, entityType, mapOfChanges);

        } catch (SQLException e) {
            throw new RuntimeException("Error getting entity records for baseline: " + baseline);
        }

        return listOfChanges;
    }

    private List<BaselineChangeRow> buildListOfDecoratedAndSortedChanges(EntityProvider entityProvider, EntityType entityType, Map<Integer, BaselineChangeRow> mapOfChanges) throws SQLException{

        List<BaselineChangeRow> listOfChanges = new ArrayList<>();

        for (BaselineChangeRow changeRow : mapOfChanges.values()) {

            changeRow.setLastModifiedById(changeRow.getLastModifiedById());
            String lastModifiedBy = CustomerLookupCache.getUserLookupValue(getWebSession(), changeRow.getLastModifiedById()).getLookupCode();
            changeRow.setLastModifiedBy(lastModifiedBy);

            if (changeRow.isNew()) {
                changeRow.setActivity("Created");
            } else {
                changeRow.setActivity(changeRow.isActive() ? "Modified" : "Inactivated");
            }

            String code = entityProvider.getEntityColumnValue(entityType, entityType.getEntityCodeColumn(), changeRow.getEntityId());
            String name = entityProvider.getEntityColumnValue(entityType, entityType.getEntityNameColumn(), changeRow.getEntityId());
            changeRow.setId(code);;
            changeRow.setName(name);

            listOfChanges.add(changeRow);

        }

        return listOfChanges;
    }

    private boolean baselineExists(String tagName) {
        WebSession webSession = getWebSession();

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(BASELINE_EXISTS_SQL)) {

            setInt(statement, webSession.getCustomerId(), 1);
            setInt(statement, webSession.getProjectId(), 2);
            setString(statement, tagName, 3);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            log.error(
                    "Error checking baseline uniqueness. customerId={}, projectId={}, tagName={}",
                    webSession.getCustomerId(),
                    webSession.getProjectId(),
                    tagName,
                    e
            );
            throw new RuntimeException(e);
        }
    }

    private Baseline toBaseline(ResultSet resultSet) throws SQLException {
        Baseline baseline = new Baseline();

        baseline.setBaselineId(resultSet.getInt("BaselinePK"));
        baseline.setCustomerId(resultSet.getInt("CustomerId"));
        baseline.setProjectId(resultSet.getInt("ProjectId"));
        baseline.setTagName(resultSet.getString("TagName"));
        baseline.setDescription(resultSet.getString("Description"));
        baseline.setChangedByUserId(resultSet.getInt("ChangedByUserId"));
        baseline.setChangedDateTime(resultSet.getTimestamp("ChangedDateTime"));
        baseline.setChangedBy(getUserNameByUserId(baseline.getChangedByUserId()));

        return baseline;
    }

    private void validateWebSession(WebSession webSession) {
        if (webSession == null) {
            throw new IllegalArgumentException("WebSession is required.");
        }

        if (webSession.getCustomerId() == null) {
            throw new IllegalArgumentException("CustomerId is required.");
        }

        if (webSession.getProjectId() == null) {
            throw new IllegalArgumentException("ProjectId is required.");
        }

        if (webSession.getUserId() == null) {
            throw new IllegalArgumentException("ChangedByUserId is required.");
        }
    }

    private void validateBaselineInput(
            String tagName,
            String description
    ) {
        if (tagName.isBlank()) {
            throw new IllegalArgumentException("Tag-name is required.");
        }

        if (tagName.length() > 150) {
            throw new IllegalArgumentException("Tag-name must be maximum 150 characters.");
        }

        if (description.isBlank()) {
            throw new IllegalArgumentException("Description is required.");
        }
    }

    private Timestamp getPreviousBaselineDateTime(Integer baselineId) {

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_PREV_BASELINE_BY_PK_SQL)) {

            setInt(statement, getWebSession().getCustomerId(), 1);
            setInt(statement, getWebSession().getProjectId(), 2);
            setInt(statement, baselineId, 3);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getTimestamp("ChangedDateTime");
                }
            }
        } catch (SQLException e) {
            log.error( "Error getting previous baseline. baselinePK={}, customerId={}, projectId={}",
                    baselineId,
                    getWebSession().getCustomerId(),
                    getWebSession().getProjectId(),e );
            throw new RuntimeException(e);
        }

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_PROJECT_CREATED_DATE_SQL)) {

            setInt(statement, getWebSession().getCustomerId(), 1);
            setInt(statement, getWebSession().getProjectId(), 2);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getTimestamp("ChangedDateTime");
                }
            }
        } catch (SQLException e) {
            log.error( "Error getting previous baseline. baselinePK={}, customerId={}, projectId={}",
                    baselineId,
                    getWebSession().getCustomerId(),
                    getWebSession().getProjectId(),e );
            throw new RuntimeException(e);
        }

        return null;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String getUserNameByUserId(Integer userId) {
        LookupValue lookupValue = CustomerLookupCache.getUserLookupValue(getWebSession(), userId);
        return lookupValue.getLookupCode();
    }

}