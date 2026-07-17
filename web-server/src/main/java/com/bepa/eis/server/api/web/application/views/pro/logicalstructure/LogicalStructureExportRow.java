package com.bepa.eis.server.api.web.application.views.pro.logicalstructure;

import com.bepa.eis.server.dataprovider.fields.lookups.common.ChangedBy;

import java.time.LocalDateTime;

public record LogicalStructureExportRow(
        String id,
        Integer level,
        String name,
        String description,
        ChangedBy changedBy,
        LocalDateTime changed,
        Boolean active
) {
}
