package com.bepa.eis.server.api.web.application.views.basis.system;

import com.bepa.eis.server.dataprovider.fields.lookups.common.ChangedBy;
import com.bepa.eis.server.dataprovider.fields.lookups.requirement.RequirementBusinessPriority;
import com.bepa.eis.server.dataprovider.fields.lookups.requirement.RequirementOwner;
import com.bepa.eis.server.dataprovider.fields.lookups.requirement.RequirementStatus;
import com.bepa.eis.server.dataprovider.fields.lookups.requirement.RequirementVerificationStatus;

import java.time.LocalDateTime;

public record SystemRequirementExportRow(
        String id,
        Integer level,
        String name,
        String description,
        RequirementVerificationStatus verificationStatus,
        String rationalStatement,
        String requirementCaptured,
        RequirementStatus status,
        RequirementBusinessPriority businessPriority,
        RequirementOwner owner,
        ChangedBy changedBy,
        LocalDateTime changed,
        Boolean active
) {
}