package com.bepa.eis.server.api.web.application.views.pro.interfacematrix;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.dataprovider.entities.FunctionalStructureProvider;
import com.bepa.eis.server.dataprovider.entities.LogicalStructureProvider;
import com.bepa.eis.server.dataprovider.entities.StakeholderRequirementProvider;
import com.bepa.eis.server.dataprovider.entities.SystemBreakdownProvider;
import com.bepa.eis.server.dataprovider.entities.SystemRequirementProvider;
import com.bepa.eis.server.entites.AbstractEntity;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class InterfaceMatrixEntityOrder {
    private InterfaceMatrixEntityOrder() {}

    public static <T extends AbstractEntity> void sort(List<T> entities) {
        entities.sort(Comparator.comparing(AbstractEntity::getSortKey,
                Comparator.nullsLast(String::compareToIgnoreCase)));
    }

    public static Map<Integer, Integer> getPositions(WebSession session, EntityType entityType) {
        List<? extends AbstractEntity> entities = switch (entityType) {
            case SYSTEMS_BREAKDOWN -> new SystemBreakdownProvider(session).getAllSystemBreakdown(false);
            case STAKEHOLDER_REQUIREMENT -> new StakeholderRequirementProvider(session).getAllStakeholderRequirement(false);
            case SYSTEM_REQUIREMENT -> new SystemRequirementProvider(session).getAllSystemRequirement(false);
            case LOGICAL_STRUCTURE -> new LogicalStructureProvider(session).getAllLogicalStructures(false);
            case FUNCTIONAL_STRUCTURE -> new FunctionalStructureProvider(session).getAllFunctionalStructure(false);
            default -> throw new IllegalArgumentException("Unsupported interface entity type: " + entityType);
        };
        sort(entities);
        Map<Integer, Integer> positions = new HashMap<>();
        for (int index = 0; index < entities.size(); index++) {
            positions.put(entities.get(index).getEntityId().getValue(), index);
        }
        return positions;
    }
}
