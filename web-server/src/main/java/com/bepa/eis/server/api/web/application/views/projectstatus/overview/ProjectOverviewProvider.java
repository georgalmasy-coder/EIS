package com.bepa.eis.server.api.web.application.views.projectstatus.overview;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.providers.GenericProvider;
import com.bepa.eis.server.api.DTO.Project;
import com.bepa.eis.server.dataprovider.fields.bigdecimals.BudgetInValue;
import com.bepa.eis.server.dataprovider.fields.integers.BudgetInDays;
import com.bepa.eis.server.dataprovider.fields.integers.Version;
import com.bepa.eis.server.dataprovider.fields.integers.ids.CustomerId;
import com.bepa.eis.server.dataprovider.fields.integers.ids.ProjectId;
import com.bepa.eis.server.dataprovider.fields.lookups.customer.CustomerDepartment;
import com.bepa.eis.server.dataprovider.fields.lookups.project.ProjectCategory;
import com.bepa.eis.server.dataprovider.fields.lookups.project.ProjectOwner;
import com.bepa.eis.server.dataprovider.fields.lookups.project.ProjectPriority;
import com.bepa.eis.server.dataprovider.fields.lookups.project.ProjectStatus;
import com.bepa.eis.server.dataprovider.fields.strings.ProjectName;
import com.bepa.eis.server.dataprovider.fields.timestamp.ChangedDateTime;
import com.bepa.eis.server.dataprovider.fields.timestamp.EndDate;
import com.bepa.eis.server.dataprovider.fields.timestamp.StartDate;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ProjectOverviewProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(ProjectOverviewProvider.class);

    private final Project project = new Project(getWebSession());

    private static final String GET_LATEST_PROJECT_BY_PROJECT_ID_SQL = """
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

    public ProjectOverviewProvider(WebSession webSession) {
        super(webSession);
    }

    /**
     * Retrieves the latest Project version for the project id stored in the current web session.
     *
     * @return Project DTO containing XML field elements for the overview page
     * @throws Exception if no project is selected, no project is found, or the database lookup fails
     */
    public Project getProjectByProjectId() throws Exception {
        if (getWebSession() == null || getWebSession().getProjectId() == null) {
            log.error("No webSession or projectId found: {}", getWebSession());
            throw new SQLException("No project selected in web session.");
        }

        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(GET_LATEST_PROJECT_BY_PROJECT_ID_SQL)) {

            setInt(ps, getWebSession().getProjectId(), 1);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    log.error("No latest project found for projectId: {}", getWebSession().getProjectId());
                    throw new SQLException("No latest project found for projectId: " + getWebSession().getProjectId());
                }

                addProjectFields(rs);
            }
        }

        return project;
    }

    private void addProjectFields(ResultSet rs) throws SQLException {
        ProjectId projectId = new ProjectId(rs.getInt(ProjectId.FIELD_NAME));
        projectId.setFieldNotEditable();
        getProjectElement().addElement(projectId);

        Version version = new Version(rs.getInt(Version.FIELD_NAME));
        version.setFieldNotEditable();
        getProjectElement().addElement(version);

        ProjectName projectName = new ProjectName(rs.getString(ProjectName.FIELD_NAME));
        projectName.setFieldNotEditable();
        getProjectElement().addElement(projectName);

        CustomerId customerId = new CustomerId(rs.getInt(CustomerId.FIELD_NAME));
        customerId.setFieldNotEditable();
        getProjectElement().addElement(customerId);

        ProjectOwner projectOwner = new ProjectOwner(getWebSession());
        projectOwner.setValue(getNullableInteger(rs, ProjectOwner.FIELD_NAME));
        projectOwner.setFieldNotEditable();
        getProjectElement().addElement(projectOwner);

        ProjectCategory projectCategory = new ProjectCategory(getWebSession());
        projectCategory.setValue(getNullableInteger(rs, ProjectCategory.FIELD_NAME));
        projectCategory.setFieldNotEditable();
        getProjectElement().addElement(projectCategory);

        ProjectPriority projectPriority = new ProjectPriority(getWebSession());
        projectPriority.setValue(getNullableInteger(rs, ProjectPriority.FIELD_NAME));
        projectPriority.setFieldNotEditable();
        getProjectElement().addElement(projectPriority);

        ProjectStatus projectStatus = new ProjectStatus(getWebSession());
        projectStatus.setValue(getNullableInteger(rs, ProjectStatus.FIELD_NAME));
        projectStatus.setFieldNotEditable();
        getProjectElement().addElement(projectStatus);

        StartDate startDate = new StartDate(rs.getTimestamp(StartDate.FIELD_NAME));
        startDate.setFieldNotEditable();
        getProjectElement().addElement(startDate);

        EndDate endDate = new EndDate(rs.getTimestamp(EndDate.FIELD_NAME));
        endDate.setFieldNotEditable();
        getProjectElement().addElement(endDate);

        BudgetInDays budgetInDays = new BudgetInDays(getNullableInteger(rs, BudgetInDays.FIELD_NAME));
        budgetInDays.setFieldNotEditable();
        getProjectElement().addElement(budgetInDays);

        BudgetInValue budgetInValue = new BudgetInValue(rs.getBigDecimal(BudgetInValue.FIELD_NAME));
        budgetInValue.setFieldNotEditable();
        getProjectElement().addElement(budgetInValue);

        CustomerDepartment customerDepartment = new CustomerDepartment(getWebSession());
        customerDepartment.setValue(getNullableInteger(rs, CustomerDepartment.FIELD_NAME));
        customerDepartment.setFieldNotEditable();
        getProjectElement().addElement(customerDepartment);

        ChangedDateTime changedDateTime = new ChangedDateTime(rs.getTimestamp(ChangedDateTime.FIELD_NAME));
        changedDateTime.setFieldNotEditable();
        getProjectElement().addElement(changedDateTime);
    }

    private Integer getNullableInteger(
            ResultSet resultSet,
            String columnName
    ) throws SQLException {
        int value = resultSet.getInt(columnName);

        return resultSet.wasNull() ? null : value;
    }

    private ListOfElements getProjectElement() {
        return project.getProjectElements();
    }
}