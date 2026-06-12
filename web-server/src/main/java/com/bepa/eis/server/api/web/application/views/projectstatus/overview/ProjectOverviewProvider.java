package com.bepa.eis.server.api.web.application.views.projectstatus.overview;

import com.bepa.eis.server.api.DTO.Project;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.fields.bigdecimals.BudgetInValue;
import com.bepa.eis.server.dataprovider.fields.booleans.Active;
import com.bepa.eis.server.dataprovider.fields.integers.BudgetInDays;
import com.bepa.eis.server.dataprovider.fields.integers.ids.CustomerId;
import com.bepa.eis.server.dataprovider.fields.integers.ids.ProjectId;
import com.bepa.eis.server.dataprovider.fields.lookups.*;
import com.bepa.eis.server.dataprovider.fields.lookups.project.ProjectCategory;
import com.bepa.eis.server.dataprovider.fields.lookups.project.ProjectOwner;
import com.bepa.eis.server.dataprovider.fields.lookups.project.ProjectPriority;
import com.bepa.eis.server.dataprovider.fields.lookups.project.ProjectStatus;
import com.bepa.eis.server.dataprovider.fields.strings.Notes;
import com.bepa.eis.server.dataprovider.fields.strings.ProjectName;
import com.bepa.eis.server.dataprovider.fields.timestamp.EndDate;
import com.bepa.eis.server.dataprovider.fields.timestamp.StartDate;
import com.bepa.eis.common.providers.GenericProvider;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class ProjectOverviewProvider extends GenericProvider {

    private static final Logger log = LoggerFactory.getLogger(ProjectOverviewProvider.class);

    Project project = new Project(getWebSession());

    private static final String GET_PROJECT_BY_PROJECT_ID_SQL =
            "SELECT * FROM Project WHERE ProjectId = ?";

    public ProjectOverviewProvider(WebSession webSession) {
        super(webSession);
    }


    /**
     * Retrieves a {@code Project} object associated with the specified project ID
     * from the database. The project ID is obtained from the provided {@code webSession}.
     * If no project is found for the provided project ID, an exception is thrown.
     *
     * @return the {@code Project} object corresponding to the specified project ID
     * @throws Exception if there is an error during the database operation or if no project is found
     */
    public Project getProjectByProjectId() throws Exception {

        if (getWebSession() != null && getWebSession().getProjectId() != null) {

            try (Connection con = getDataSource().getConnection();
                 PreparedStatement ps = con.prepareStatement(GET_PROJECT_BY_PROJECT_ID_SQL)) {

                setInt(ps, getWebSession().getProjectId(), 1);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        log.error("No project found for projectId: {}", getWebSession().getProjectId());
                        throw new SQLException("No project found for projectId: " + getWebSession().getProjectId());
                    }

                    getProjectElement().addElement(new ProjectId(rs.getInt(ProjectId.FIELD_NAME)));
                    getProjectElement().addElement(new ProjectName(rs.getString(ProjectName.FIELD_NAME)));
                    getProjectElement().addElement(new CustomerId(rs.getInt(CustomerId.FIELD_NAME)));
                    getProjectElement().addElement(new Active( rs.getBoolean(Active.FIELD_NAME)));

                    ProjectOwner projectOwner = new ProjectOwner(getWebSession());
                    projectOwner.setValue(rs.getInt(ProjectOwner.FIELD_NAME));
                    projectOwner.setFieldNotEditable();
                    getProjectElement().addElement(projectOwner);

                    ProjectCategory projectCategory = new ProjectCategory(getWebSession());
                    projectCategory.setValue(rs.getInt(ProjectCategory.FIELD_NAME));
                    projectCategory.setFieldNotEditable();
                    getProjectElement().addElement(projectCategory);

/*WEBX */
                    ProjectPriority projectPriority = new ProjectPriority(getWebSession());
                    projectPriority.setValue(rs.getInt(ProjectPriority.FIELD_NAME));
                    projectPriority.setFieldNotEditable();
                    getProjectElement().addElement(projectPriority);

                    ProjectStatus projectStatus = new ProjectStatus(getWebSession());
                    projectStatus.setValue(rs.getInt(ProjectStatus.FIELD_NAME));
                    projectStatus.setFieldNotEditable();
                    getProjectElement().addElement(projectStatus);

                    getProjectElement().addElement(new StartDate(rs.getTimestamp( StartDate.FIELD_NAME)));
                    getProjectElement().addElement(new EndDate(rs.getTimestamp(EndDate.FIELD_NAME)));
                    getProjectElement().addElement(new BudgetInDays(rs.getInt( BudgetInDays.FIELD_NAME)));
                    getProjectElement().addElement(new BudgetInValue(rs.getBigDecimal( BudgetInValue.FIELD_NAME)));
                    getProjectElement().addElement(new Notes(rs.getString( Notes.FIELD_NAME)));

                    CustomerDepartment customerDepartment = new CustomerDepartment(getWebSession());
                    customerDepartment.setValue(rs.getInt(CustomerDepartment.FIELD_NAME));
                    customerDepartment.setFieldNotEditable();
                    getProjectElement().addElement(customerDepartment);
                }
            }

        } else {
            log.error("No webSession found : {}", getWebSession());
        }

        return project;
    }

    private ListOfElements getProjectElement() {
        return project.getProjectElements();
    }
}
