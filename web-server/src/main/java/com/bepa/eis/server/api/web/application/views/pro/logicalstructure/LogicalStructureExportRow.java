package com.bepa.eis.server.api.web.application.views.pro.logicalstructure;

import com.bepa.eis.server.dataprovider.fields.lookups.common.ChangedBy;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;

import java.time.LocalDateTime;

public record LogicalStructureExportRow(
        String id,
        Integer level,
        String name,
        String description,
        AbstractLookup owner,
        AbstractLookup verificationStatus,
        AbstractLookup criticality,
        AbstractLookup elementCategory,
        AbstractLookup logicalLevel,
        AbstractLookup type,
        AbstractLookup responsibleDomain,
        AbstractLookup lifecycleStatus,
        AbstractLookup maturity,
        AbstractLookup allocationStatus,
        AbstractLookup realizationStatus,
        AbstractLookup safetyClassification,
        AbstractLookup securityClassification,
        AbstractLookup redundancyType,
        AbstractLookup configurationVariant,
        AbstractLookup applicability,
        ChangedBy changedBy,
        LocalDateTime changed,
        Boolean active
) {
}
