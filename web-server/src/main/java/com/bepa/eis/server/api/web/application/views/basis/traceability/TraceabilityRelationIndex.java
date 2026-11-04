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
    private final Map<Integer, Integer> systemCounts = new HashMap<>();

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
            put(stakeholderId, systemId, relation);
        }
    }

    void put(int stakeholderId, int systemId, EntityRelationRecord relation) {
        var row = byStakeholder.computeIfAbsent(stakeholderId, ignored -> new HashMap<>());
        boolean existed = row.containsKey(systemId);
        if (relation.getRelationType() == com.bepa.eis.common.enums.entity.RelationType.DELETED) {
            row.remove(systemId);
            if (existed) systemCounts.compute(systemId, (key, count) -> count == null || count <= 1 ? null : count - 1);
            if (row.isEmpty()) byStakeholder.remove(stakeholderId);
        } else {
            row.put(systemId, relation);
            if (!existed) systemCounts.merge(systemId, 1, Integer::sum);
        }
    }

    boolean hasStakeholderRelations(int id) { return byStakeholder.containsKey(id); }
    boolean hasSystemRelations(int id) { return systemCounts.containsKey(id); }

    EntityRelationRecord get(Integer stakeholderId, Integer systemId) {
        Map<Integer, EntityRelationRecord> row = byStakeholder.get(stakeholderId);
        return row == null ? null : row.get(systemId);
    }
}
