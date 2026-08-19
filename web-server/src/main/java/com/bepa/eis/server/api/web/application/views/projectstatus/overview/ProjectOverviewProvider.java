package com.bepa.eis.server.api.web.application.views.projectstatus.overview;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.dto.project.ProjectRecord;
import com.bepa.eis.common.providers.GenericProvider;
import com.bepa.eis.server.api.DTO.Project;
import com.bepa.eis.server.api.DTO.TrlRecord;
import com.bepa.eis.server.dataprovider.entities.*;
import com.bepa.eis.server.dataprovider.fields.bigdecimals.BudgetInValue;
import com.bepa.eis.server.dataprovider.fields.integers.AbstractInteger;
import com.bepa.eis.server.dataprovider.fields.integers.BudgetInDays;
import com.bepa.eis.server.dataprovider.fields.integers.Version;
import com.bepa.eis.server.dataprovider.fields.integers.ids.CustomerId;
import com.bepa.eis.server.dataprovider.fields.integers.ids.ProjectId;
import com.bepa.eis.server.dataprovider.fields.lookups.customer.CustomerDepartment;
import com.bepa.eis.server.dataprovider.fields.lookups.project.ProjectCategory;
import com.bepa.eis.server.dataprovider.fields.lookups.project.ProjectOwner;
import com.bepa.eis.server.dataprovider.fields.lookups.project.ProjectPriority;
import com.bepa.eis.server.dataprovider.fields.lookups.project.ProjectStatus;
import com.bepa.eis.server.dataprovider.fields.strings.AbstractString;
import com.bepa.eis.server.dataprovider.fields.strings.ProjectName;
import com.bepa.eis.server.dataprovider.fields.timestamp.ChangedDateTime;
import com.bepa.eis.server.dataprovider.fields.timestamp.EndDate;
import com.bepa.eis.server.dataprovider.fields.timestamp.StartDate;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

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

        List<TrlRecord> trlRecords = getActiveTrlRecords(getWebSession().getCustomerId(), getWebSession().getProjectId());

        getNextTrlDeadLine(trlRecords);

        NextTrlReview nextTrlReview = new NextTrlReview();
        nextTrlReview.setValue(getNextTrlDeadLine(trlRecords));
        getProjectElement().addElement(nextTrlReview);


        int stakeholderRequirementCount = getActiveStakeholderRequirementCount();
        int systemRequirementCount = getActiveSystemRequirementCount();
        CountRequirement countRequirement = new CountRequirement();
        countRequirement.setValue(stakeholderRequirementCount + systemRequirementCount);
        getProjectElement().addElement(countRequirement);

        int logicalStructureCount = getActiveLogicalStructureCount();
        CountLogicalStructure countLogicalStructure = new CountLogicalStructure();
        countLogicalStructure.setValue(logicalStructureCount);
        getProjectElement().addElement(countLogicalStructure);

        int functionalStructureCount = getActiveFunctionalStructureCount();
        CountFunctionalStructure countFunctionalStructure = new CountFunctionalStructure();
        countFunctionalStructure.setValue(functionalStructureCount);
        getProjectElement().addElement(countFunctionalStructure);

        int physicalStructureCount = getActivePhysicalStructureCount();
        CountPhysicalStructure countPhysicalStructure = new CountPhysicalStructure();
        countPhysicalStructure.setValue(physicalStructureCount);
        getProjectElement().addElement(countPhysicalStructure);

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


    private List<TrlRecord> getActiveTrlRecords(
            Integer customerId,
            Integer projectId
    ) {
        SystemBreakdownProvider systemBreakdownProvider = new SystemBreakdownProvider(getWebSession());

        return systemBreakdownProvider.getListOfTrlRecords(
                customerId,
                projectId
        );
    }

    private String getNextTrlDeadLine(List<TrlRecord> trlRecordList) {
        Timestamp nextTrlDeadline = null;

        if (trlRecordList != null) {
            for (TrlRecord trlRecord : trlRecordList) {
                Timestamp deadline = trlRecord.getNextTrlDeadline();

                if (deadline == null || !deadline.after(now())) {
                    continue;
                }

                if (nextTrlDeadline == null || deadline.before(nextTrlDeadline)) {
                    nextTrlDeadline = deadline;
                }
            }

            if (trlRecordList.isEmpty()) {
                return "-";
            }

            if (nextTrlDeadline == null) {
                return "Over due";
            }

        }
        return daysUntil(nextTrlDeadline) + " days";
    }

    private Long daysUntil(Timestamp nextTrlDeadline) {
        if (nextTrlDeadline == null) {
            return 0L;
        }

        LocalDate today = now().toLocalDateTime().toLocalDate();
        LocalDate deadlineDate = nextTrlDeadline.toLocalDateTime().toLocalDate();

        return ChronoUnit.DAYS.between(
                today,
                deadlineDate
        );
    }

    private Timestamp now() {
        return Timestamp.from(Instant.now());
    }

    private void getTrlRecords() {
        List<TrlRecord> trlRecords = getActiveTrlRecords(
                getWebSession().getCustomerId(),
                getWebSession().getProjectId()
        );

    }

    private Integer getActiveStakeholderRequirementCount() {
        return getActiveEntityCount(new StakeholderRequirementProvider(getWebSession()));
    }

    private Integer getActiveSystemRequirementCount() {
        return getActiveEntityCount(new SystemRequirementProvider(getWebSession()));
    }

    private Integer getActiveLogicalStructureCount() {
        return getActiveEntityCount(new LogicalStructureProvider(getWebSession()));
    }

    private Integer getActiveFunctionalStructureCount() {
        return getActiveEntityCount(new FunctionalStructureProvider(getWebSession()));
    }

    private Integer getActivePhysicalStructureCount() {
        return getActiveEntityCount(new SystemBreakdownProvider(getWebSession()));
    }

    private int getActiveEntityCount(EntityProvider entityProvider) {
        return entityProvider.getActiveEntityCount(
                getWebSession().getCustomerId(),
                getWebSession().getProjectId(),
                entityProvider.getEntityType()
        );
    }

    private static class NextTrlReview extends AbstractString {

        @Override
        public String getFieldName() {
            return "NextTrlReview";
        }

        @Override
        public String getFieldLabelName() {
            return "Next Trl Review";
        }

        @Override
        public String getFieldHeaderName() {
            return "Next Trl Review";
        }
    }

    private static class CountRequirement extends AbstractInteger {

        @Override
        public String getFieldName() {
            return "CountRequirement";
        }

        @Override
        public String getFieldLabelName() {
            return "CountRequirement";
        }

        @Override
        public String getFieldHeaderName() {
            return "CountRequirement";
        }
    }

    private static class CountLogicalStructure extends AbstractInteger {

        @Override
        public String getFieldName() {
            return "CountLogicalStructure";
        }

        @Override
        public String getFieldLabelName() {
            return "Count Logical Structure";
        }

        @Override
        public String getFieldHeaderName() {
            return "Count Logical Structure";
        }
    }

    private static class CountFunctionalStructure extends AbstractInteger {

        @Override
        public String getFieldName() {
            return "CountFunctionalStructure";
        }

        @Override
        public String getFieldLabelName() {
            return "Count Functional Structure";
        }

        @Override
        public String getFieldHeaderName() {
            return "Count Functional Structure";
        }
    }

    private static class CountPhysicalStructure extends AbstractInteger {

        @Override
        public String getFieldName() {
            return "CountPhysicalStructure";
        }

        @Override
        public String getFieldLabelName() {
            return "Count Physical Structure";
        }

        @Override
        public String getFieldHeaderName() {
            return "Count Physical Structure";
        }
    }
}