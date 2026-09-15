package com.bepa.eis.server.api.web.application.views.pro.functionalstructure;

import com.bepa.eis.server.dataprovider.fields.lookups.common.ChangedBy;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;

import java.time.LocalDateTime;

public record FunctionalStructureExportRow(
        String id,
        Integer level,
        String name,
        String description,
        AbstractLookup owner,
        AbstractLookup status,
        AbstractLookup behaviorType,
        AbstractLookup category,
        AbstractLookup criticality,
        AbstractLookup verificationStatus,
        AbstractLookup functionLevel,
        AbstractLookup operatingMode,
        AbstractLookup responsibleDomain,
        AbstractLookup configurationVariant,
        AbstractLookup applicability,
        ChangedBy changedBy,
        LocalDateTime changed,
        Boolean active
) {
}

