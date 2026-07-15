package com.bepa.eis.server.api.web.application.views.basis.stakeholder;

import com.bepa.eis.server.dataprovider.fields.lookups.common.ChangedBy;

import java.time.LocalDateTime;

public record StakeholderExportRow(
        String name,
        String description,
        String contactName,
        String contactEmail,
        String contactPhone,
        ChangedBy changedBy,
        LocalDateTime changed,
        Boolean active
) {
}
