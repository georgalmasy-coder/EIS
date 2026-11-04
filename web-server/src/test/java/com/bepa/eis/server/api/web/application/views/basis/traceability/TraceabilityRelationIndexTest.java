package com.bepa.eis.server.api.web.application.views.basis.traceability;

import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.common.enums.entity.RelationType;
import com.bepa.eis.common.providers.entityrelation.EntityRelationRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.bepa.eis.common.enums.entity.EntityType.STAKEHOLDER_REQUIREMENT;
import static com.bepa.eis.common.enums.entity.EntityType.SYSTEM_REQUIREMENT;

/** Standalone regression suite comparing numeric lookup with the previous string index. */
public final class TraceabilityRelationIndexTest {
    public static void main(String[] args) {
        var confirmed = relation(12, 34, false, RelationType.CONFIRMED);
        var notRelevant = relation(56, 78, true, RelationType.NOT_RELEVANT);
        var swappedIds = relation(34, 12, true, RelationType.CONFIRMED);
        var sameIds = relation(42, 42, false, RelationType.NOT_RELEVANT);
        var maxIds = relation(Integer.MAX_VALUE, 128, true, RelationType.CONFIRMED);
        var index = new TraceabilityRelationIndex(List.of(confirmed, notRelevant, swappedIds, sameIds, maxIds));
        check(index.get(12, 34) == confirmed, "Forward confirmed relation");
        check(index.get(56, 78) == notRelevant, "Reverse not-relevant relation");
        check(index.get(34, 12) == swappedIds, "Swapped numeric ids remain distinct");
        check(index.get(42, 42) == sameIds, "Same numeric id in different entity types");
        check(index.get(Integer.MAX_VALUE, 128) == maxIds, "Full integer range and value-based lookup");
        check(index.get(12, 78) == null, "Missing column in existing row");
        check(index.get(999, 34) == null, "Missing row");
        check(new TraceabilityRelationIndex(List.of()).get(12, 34) == null, "Empty index");

        var replacement = relation(12, 34, true, RelationType.NOT_RELEVANT);
        check(new TraceabilityRelationIndex(List.of(confirmed, replacement)).get(12, 34) == replacement,
                "Last record wins across opposite storage directions");
        check(new TraceabilityRelationIndex(List.of(replacement, confirmed)).get(12, 34) == confirmed,
                "Last record wins in either order");
        var unrelated = new EntityRelationRecord(1, 1);
        unrelated.setEntityType(SYSTEM_REQUIREMENT);
        unrelated.setEntityId(12);
        unrelated.setRelatedEntityType(SYSTEM_REQUIREMENT);
        unrelated.setRelatedEntityId(34);
        check(new TraceabilityRelationIndex(List.of(confirmed, unrelated)).get(12, 34) == confirmed,
                "Other entity type pairs cannot overwrite the requirement relation");

        Random random = new Random(452891);
        List<EntityRelationRecord> relations = new ArrayList<>();
        for (int i = 0; i < 5000; i++) {
            relations.add(relation(128 + random.nextInt(100), 128 + random.nextInt(100), random.nextBoolean(),
                    random.nextBoolean() ? RelationType.CONFIRMED : RelationType.NOT_RELEVANT));
        }
        Map<String, EntityRelationRecord> previous = new HashMap<>();
        for (var relation : relations) {
            String first = endpoint(relation.getEntityId(), relation.getEntityType());
            String second = endpoint(relation.getRelatedEntityId(), relation.getRelatedEntityType());
            previous.put(first + "_" + second, relation);
            previous.put(second + "_" + first, relation);
        }
        var numeric = new TraceabilityRelationIndex(relations);
        for (int stakeholder = 128; stakeholder < 240; stakeholder++) {
            for (int system = 128; system < 240; system++) {
                var expected = previous.get(endpoint(stakeholder, STAKEHOLDER_REQUIREMENT)
                        + "_" + endpoint(system, SYSTEM_REQUIREMENT));
                check(numeric.get(stakeholder, system) == expected, "Previous lookup semantics preserved");
            }
        }
        var mutable = new TraceabilityRelationIndex(List.of());
        mutable.put(1,2,relation(1,2,false,RelationType.CONFIRMED));
        mutable.put(3,2,relation(3,2,false,RelationType.NOT_RELEVANT));
        check(mutable.hasStakeholderRelations(1)&&mutable.hasSystemRelations(2),"confirmed header state");
        mutable.put(1,2,relation(1,2,false,RelationType.DELETED));
        check(!mutable.hasStakeholderRelations(1)&&mutable.hasSystemRelations(2),"column remains normal while another relation exists");
        mutable.put(3,2,relation(3,2,false,RelationType.DELETED));
        check(!mutable.hasSystemRelations(2)&&mutable.get(3,2)==null,"last relation removal clears column state and cell lookup");
        System.out.println("TraceabilityRelationIndex checks passed; 12,544 lookups match the previous index; header status tracks relation edits");
    }

    private static String endpoint(Integer id, EntityType type) {
        return id + "_" + type.getId();
    }

    private static EntityRelationRecord relation(int stakeholder, int system, boolean reverse, RelationType type) {
        var relation = new EntityRelationRecord(1, 1);
        relation.setEntityType(reverse ? SYSTEM_REQUIREMENT : STAKEHOLDER_REQUIREMENT);
        relation.setEntityId(reverse ? system : stakeholder);
        relation.setRelatedEntityType(reverse ? STAKEHOLDER_REQUIREMENT : SYSTEM_REQUIREMENT);
        relation.setRelatedEntityId(reverse ? stakeholder : system);
        relation.setRelationType(type);
        return relation;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
