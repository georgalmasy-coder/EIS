package com.bepa.eis.server.dataprovider.fields.lookups.codeselector;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import com.bepa.eis.common.enums.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

abstract public class AbstractParentCodeSelector extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(AbstractParentCodeSelector.class);

    abstract public EntityType getEntityType();

    private static final String SELECT_COLUMN_FROM_ACTIVE_ENTITY_SQL =
            "SELECT E.EntityId, EE.StringValue AS StringValue " +
            "FROM ENTITY E, ENTITY_ELEMENT EE " +
            "WHERE E.CustomerId = EE.CustomerId " +
            "AND E.ProjectId = EE.ProjectId " +
            "AND E.EntityType = EE.EntityType " +
            "AND E.EntityId = EE.EntityId " +
            "AND E.Version = EE.Version " +
            "AND E.LATEST = 1 " +
            "AND E.ACTIVE = 1 " +
            "AND E.CustomerId = ? " +
            "AND E.ProjectId = ? " +
            "AND E.EntityType = ? " +
            "AND EE.EntityDataElementType = ?";

    private ConcurrentHashMap<Integer, ParentLookupIdValue> entities = new ConcurrentHashMap<>();

    public AbstractParentCodeSelector(WebSession webSession) {
        super(webSession);
    }

    public void setValue(Integer codeTypeId) {
    }

    public Integer getValue() {
        return getLookupId();
    }

    public int getCodeLevel(String codeValue) {
        int codeLevel = 0;
        if (codeValue != null) {
            String[] strings = codeValue.split("\\.");
            codeLevel = strings.length;
        }
        return codeLevel;
    }

    @Override
    public String getLookupName() {
        return getFieldName();
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return List.of();
    }

    @Override
    public boolean isFieldEditable() {
        return true;
    }

    @Override
    public boolean isFieldRequired() {
        return true;
    }

    public String getNextAvailableCodeValue(WebSession webSession, String parentCode) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getNextAvailableCodeValue(WebSession webSession, Integer parentEntityId) {

        loadAllEntities(webSession);

        log.debug("Parent Code (entityId) : {} for {}", parentEntityId, getEntityType().getDescription());

        String codeValueParentEntity = findCurrentCodeOnLoadedEntities(parentEntityId);

        if (codeValueParentEntity != null && codeValueParentEntity.isEmpty()) {
            codeValueParentEntity = null;
        }

        log.debug("Parent Code/entityId : {}/{} for {}",codeValueParentEntity, parentEntityId, getEntityType().getDescription());

        int nextAvailableCodeIndex = 1;

        String parentCode;
        String functionCode;
        int nextCodeLevel;

        if (codeValueParentEntity == null) {
            parentCode = "";
            functionCode = getEntityType().getIdPrefix();
            nextCodeLevel = 1;
        } else {
            parentCode = stripFunction(codeValueParentEntity);
            functionCode = getPrefixFromCode(codeValueParentEntity);
            nextCodeLevel = getCodeLevel(parentCode) + 1;
        }

        for (ParentLookupIdValue parentLookupValue : entities.values()) {
            String currCode = stripFunction(parentLookupValue.getCodeValue());
            int currCodeLevel = getCodeLevel(currCode);

            if (nextCodeLevel == currCodeLevel && currCode.startsWith(parentCode)) {
                //log.debug("Found codeValueParentEntity: {} {}",parentCode, currCode);
                nextAvailableCodeIndex++;
            }

        }

        String newCodeValue = parentCode.isEmpty() ? functionCode + nextAvailableCodeIndex : functionCode + parentCode + "." + nextAvailableCodeIndex;
        log.debug("Next Code : {} for {}", newCodeValue, getEntityType().getDescription());

        return newCodeValue;
    }

    private void loadAllEntities(WebSession webSession) {
        entities = new ConcurrentHashMap<>();
        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_COLUMN_FROM_ACTIVE_ENTITY_SQL)) {

            ps.setInt(1, webSession.getCustomerId());
            ps.setInt(2, webSession.getProjectId());
            ps.setInt(3, getEntityType().getId());
            ps.setInt(4, getEntityType().getEntityCodeColumn().getId());

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Integer entityId = rs.getInt("EntityId");
                ParentLookupIdValue parentLookupIdValue = new ParentLookupIdValue(entityId);
                parentLookupIdValue.setCodeValue(rs.getString("StringValue"));
                entities.put(parentLookupIdValue.getEntityId(), parentLookupIdValue);
            }

            log.debug("Loaded {} parent entities ", entities.size());

        } catch (SQLException e) {
            log.error("Error loading all entities including Code : {}", e.getMessage());
        }

        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_COLUMN_FROM_ACTIVE_ENTITY_SQL)) {

            ps.setInt(1, webSession.getCustomerId());
            ps.setInt(2, webSession.getProjectId());
            ps.setInt(3, getEntityType().getId());
            ps.setInt(4, getEntityType().getEntityNameColumn().getId());

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                ParentLookupIdValue parentLookupIdValue;
                Integer entityId = rs.getInt("EntityId");
                parentLookupIdValue = entities.get(entityId);
                if (parentLookupIdValue != null) {
                    parentLookupIdValue.setSystemName(rs.getString("StringValue"));
                }
            }

        } catch (SQLException e) {
            log.error("Error loading all entities including Name : {}", e.getMessage());
        }
    }

    private String findCurrentCodeOnLoadedEntities(Integer parentEntityId) {
        for (ParentLookupIdValue parentLookupValue : entities.values()) {
            if (parentLookupValue.getEntityId().equals(parentEntityId)) {
                return parentLookupValue.getCodeValue();
            }

        }
        return null;
    }

    private static class ParentLookupIdValue {
        private final Integer entityId;
        private String codeValue;
        private String systemName;
        private long sortableValue;

        private ParentLookupIdValue(Integer entityId) {
            this.entityId = entityId;
        }

        public Integer getEntityId() {
            return entityId;
        }

        private void setCodeValue(String codeValue) {
            this.codeValue = codeValue;

            if (codeValue != null) {
                String codeWithoutFunction = getCodeWithoutPrefix(codeValue);
                String[] elementArray = codeWithoutFunction.split("\\.");
                sortableValue = 0;
                long factor = 1000;
                for (String element : elementArray) {
                    try {
                        sortableValue = (sortableValue * factor) + (Integer.parseInt(element));
                    } catch (NumberFormatException e) {
                        log.info("Invalid sortable value {}", element);
                    }
                }
                //log.debug("Sortable value {}", sortableValue);
            }

        }

        private String getCodeValue() {
            return codeValue;
        }

        private void setSystemName(String systemName) {
            this.systemName = systemName;
        }

    }

    private String stripFunction(String codeValue) {
        if (codeValue == null) return "";
        return getCodeWithoutPrefix(codeValue);
    }

    private static String getCodeWithoutPrefix(String codeValue) {
        String codeWithoutPrefix = "";
        if (codeValue != null) {
            for (int pos = 0; pos < codeValue.length(); pos++) {
                if (!codeValue.isEmpty() && Character.isDigit(codeValue.charAt(pos))) {
                    codeWithoutPrefix = codeValue.substring(pos);
                    break;
                }
            }

        }
        return codeWithoutPrefix;
    }

    private static String getPrefixFromCode(String codeValue) {
        String prefix = "";
        if (codeValue != null) {
            for (int pos = 0; pos < codeValue.length(); pos++) {
                if (!codeValue.isEmpty() && Character.isDigit(codeValue.charAt(pos))) {
                    prefix = codeValue.substring(0, pos);
                    break;
                }
            }

        }
        return prefix;
    }
}
