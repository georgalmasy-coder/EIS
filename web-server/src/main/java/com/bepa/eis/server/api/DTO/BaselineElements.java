package com.bepa.eis.server.api.DTO;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.views.basis.baseline.Baseline;
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
import com.bepa.eis.server.dataprovider.fields.strings.AbstractString;
import com.bepa.eis.server.dataprovider.fields.strings.ProjectName;
import com.bepa.eis.server.dataprovider.fields.timestamp.ChangedDateTime;
import com.bepa.eis.server.dataprovider.fields.timestamp.EndDate;
import com.bepa.eis.server.dataprovider.fields.timestamp.StartDate;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;

public class BaselineElements {

    private ListOfElements baselineElements = null;

    private final WebSession webSession;

    private CustomerId customerId;
    private ProjectId projectId;

    private BaselineTagName baselineTagName;
    private ChangedDateTime changedDateTime;

    public BaselineElements(WebSession webSession, Baseline baseline) {
        this.webSession = webSession;
        baselineElements = new ListOfElements(
                getWebSession(),
                this.getClass().getSimpleName()
        );

        customerId = new CustomerId(baseline.getCustomerId());
        projectId = new ProjectId(baseline.getProjectId());

        baselineTagName = new BaselineTagName(baseline.getTagName());
        changedDateTime = new ChangedDateTime(baseline.getChangedDateTime());

        baselineElements.addElement(baselineTagName);
        baselineElements.addElement(changedDateTime);
    }

    private WebSession getWebSession() {
        return webSession;
    }

    public ListOfElements getBaselineElements() {
        return baselineElements;
    }

    public void setProjectId(ProjectId projectId) {
        this.projectId = projectId;
    }

    public ProjectId getProjectId() {
        return projectId;
    }

    public void setBaselineTagName(BaselineTagName baselineTagName) {
        this.baselineTagName = baselineTagName;
    }

    public BaselineTagName getBaselineTagName() {
        return baselineTagName;
    }

    public void setChangedDateTime(ChangedDateTime changedDateTime) {
        this.changedDateTime = changedDateTime;
    }

    public ChangedDateTime getChangedDateTime() {
        return changedDateTime;
    }

    private static class BaselineTagName extends AbstractString {
        public BaselineTagName(String value) {
            super(value);
        }

        @Override
        public String getFieldName() {
            return "baselineTagName";
        }

        @Override
        public String getFieldLabelName() {
            return "Tag Name";
        }

        @Override
        public String getFieldHeaderName() {
            return "Tag Name";
        }
    }

}