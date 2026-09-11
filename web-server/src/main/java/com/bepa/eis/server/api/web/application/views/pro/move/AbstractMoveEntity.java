package com.bepa.eis.server.api.web.application.views.pro.move;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.dataprovider.entities.*;
import com.bepa.eis.server.api.web.application.views.pro.interfacematrix.InterfaceMatrixProvider;
import com.bepa.eis.server.api.web.application.views.common.EntityRelationProvider;
import com.bepa.eis.server.entites.AbstractEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

abstract public class AbstractMoveEntity {

    private static final Logger log = LoggerFactory.getLogger(AbstractMoveEntity.class);
    private final WebSession webSession;
    private final MoveRequest moveRequest;
    private final List<AbstractEntity> entitiesToMove = new ArrayList<>();

    abstract void getAllEntities();
    abstract void getEntitiesToMove();
    abstract EntityProvider getEntityProvider();

    public AbstractMoveEntity(WebSession webSession, MoveRequest moveRequest) {
        this.webSession = webSession;
        this.moveRequest = moveRequest;
    }

    public WebSession getWebSession() {
        return webSession;
    }

    public void addEntityToMove(AbstractEntity entity) {
        entitiesToMove.add(entity);
    }

    public MoveRequest getMoveRequest() {
        return moveRequest;
    }

    private List<AbstractEntity> getSortedEntitiesToMove() {
        entitiesToMove.sort(
                Comparator.comparing(
                        AbstractEntity::getCode,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                )
        );
        return entitiesToMove;
    }


    public void moveEntitiesInHierarchy(Connection connection) throws Exception {
        List<AbstractEntity> sortedEntities = getSortedEntitiesToMove();
        if (sortedEntities.isEmpty()) {
            return;
        }

        AbstractEntity sourceParent = sortedEntities.get(0);
        String newParentCode = moveRequest.toEntityId() == null
                ? getEntityProvider().getNextAvailableRootEntityCode(connection, sourceParent)
                : getEntityProvider().getNextAvailableChildEntityCode(
                        connection, sourceParent, moveRequest.toCode());
        Map<Integer, Integer> entityIdMapping = new LinkedHashMap<>();

        for (AbstractEntity entity : sortedEntities) {


            log.info("Moving entity {} {} {}", entity.getEntityId(), entity.getCode(), entity.getName());

            Integer versionNo = getEntityProvider().createInactiveEntityVersion(connection, entity);
            log.info("Entity {} version {} created and set to inactive", entity.getEntityId(), versionNo);


            Integer newEntityId = getEntityProvider().copyEntityToNewEntityId(connection, entity);
            entityIdMapping.put(entity.getEntityId().getValue(), newEntityId);
            log.info("Entity {} name {} copied to new entityId {}", entity.getCode(), entity.getName(), newEntityId);

            String newCode = calculateNewCode(entity.getCode(), moveRequest.fromCode(), newParentCode);
            log.info("MOVING ENTITY - current code {} - new code {}", entity.getCode(), newCode);

            getEntityProvider().updateCopiedEntityCode(connection, entity, newEntityId, newCode);

        }

        getInterfaceMatrixProvider(sourceParent.getEntityType()).copyInterfacesForMovedEntities(connection, entityIdMapping);
        getEntityRelationProvider().copyRelationsForMovedEntities(connection, sourceParent.getEntityType(), entityIdMapping);

    }

    private InterfaceMatrixProvider getInterfaceMatrixProvider(EntityType entityType) {
        return  new InterfaceMatrixProvider(webSession, entityType);
    }

    private EntityRelationProvider getEntityRelationProvider() {
        return new EntityRelationProvider(webSession);
    }

    private String calculateNewCode(String currentCode, String sourceParentCode, String newParentCode) {
        if (currentCode.equals(sourceParentCode)) {
            return newParentCode;
        }

        String sourceChildPrefix = sourceParentCode + ".";
        if (!currentCode.startsWith(sourceChildPrefix)) {
            throw new IllegalArgumentException("Code " + currentCode
                    + " is not the source parent or one of its children: " + sourceParentCode);
        }

        return newParentCode + currentCode.substring(sourceParentCode.length());
    }

}
