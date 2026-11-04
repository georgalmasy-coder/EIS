package com.bepa.eis.server.api.web.application.views.basis.traceability;

import com.bepa.eis.common.providers.entityrelation.EntityRelationRecord;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.bepa.eis.common.enums.entity.EntityType.STAKEHOLDER_REQUIREMENT;
import static com.bepa.eis.common.enums.entity.EntityType.SYSTEM_REQUIREMENT;

/** Request-local index; both stored relation directions use stakeholder/system id order. */
final class TraceabilityRelationIndex {
    private final Map<Integer, Map<Integer, EntityRelationRecord>> byStakeholder = new HashMap<>();

    TraceabilityRelationIndex(List<EntityRelationRecord> relations) {
        for (EntityRelationRecord relation : relations) {
            Integer stakeholderId;
            Integer systemId;
            if (relation.getEntityType() == STAKEHOLDER_REQUIREMENT
                    && relation.getRelatedEntityType() == SYSTEM_REQUIREMENT) {
                stakeholderId = relation.getEntityId();
                systemId = relation.getRelatedEntityId();
            } else if (relation.getEntityType() == SYSTEM_REQUIREMENT
                    && relation.getRelatedEntityType() == STAKEHOLDER_REQUIREMENT) {
                stakeholderId = relation.getRelatedEntityId();
                systemId = relation.getEntityId();
            } else {
                continue;
            }
            // Preserve the previous index's last-record-wins behavior for duplicates.
            byStakeholder.computeIfAbsent(stakeholderId, ignored -> new HashMap<>()).put(systemId, relation);
        }
    }

    EntityRelationRecord get(Integer stakeholderId, Integer systemId) {
        Map<Integer, EntityRelationRecord> row = byStakeholder.get(stakeholderId);
        return row == null ? null : row.get(systemId);
    }
}
