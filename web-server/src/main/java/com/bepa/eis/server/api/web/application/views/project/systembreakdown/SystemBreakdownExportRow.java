package com.bepa.eis.server.api.web.application.views.project.systembreakdown;

import com.bepa.eis.server.dataprovider.fields.lookups.common.ChangedBy;
import com.bepa.eis.server.dataprovider.fields.lookups.system.SystemDepartment;
import com.bepa.eis.server.dataprovider.fields.lookups.system.SystemOwner;
import com.bepa.eis.server.dataprovider.fields.lookups.system.TRL;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SystemBreakdownExportRow(
        String id,
        Integer level,
        String name,
        SystemOwner systemOwner,
        SystemDepartment systemDepartment,
        TRL trl,
        LocalDate deadlineNextTRL,
        LocalDate deadlineFinalized,
        ChangedBy changedBy,
        LocalDateTime changed,
        Boolean active
) {
}