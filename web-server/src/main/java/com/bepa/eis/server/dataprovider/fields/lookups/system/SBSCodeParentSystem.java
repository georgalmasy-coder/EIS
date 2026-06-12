package com.bepa.eis.server.dataprovider.fields.lookups.system;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.dataprovider.fields.lookups.common.AbstractLookup;
import com.bepa.eis.common.enums.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.bepa.eis.common.enums.entity.EntityDataElement.*;

public class SBSCodeParentSystem extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(SBSCodeParentSystem.class);

    public static String FIELD_NAME = "SBSCodeParentSystem";

    private static final String SELECT_ALL_SBS_CODE_SQL =
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

    private static final ConcurrentHashMap<Integer, ParentSBSLookupValue> entities = new ConcurrentHashMap<>();

    public SBSCodeParentSystem() {
        super();
    }

    public SBSCodeParentSystem(WebSession webSession) {
        super(webSession);
    }

    public void setValue(Integer sbsCodeTypeId) {
//GFA        setActive(true);
//GFA        setLookupId(sbsCodeTypeId);
    }

    public Integer getValue() {
        return getLookupId();
    }

    public int getCodeLevel(String sbsCode) {
        int codeLevel = 0;
        if (sbsCode != null) {
            String[] strings = sbsCode.split("\\.");
            codeLevel = strings.length;
        }
        return codeLevel;
    }

    @Override
    public List<LookupValue> getListOfActiveLookupValues() {
        return List.of();
    }


    @Override
    public String getLookupName() {
        return "SBSCodeParentSystem";
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    @Override
    public String getFieldLabelName() {
        return "Parent SBS";
    }

    @Override
    public String getFieldHeaderName() {
        return "Parent SBS";
    }

    @Override
    public String getDropdownSelectText() {
        return "Select systems breakdown ...";
    }

    public String getNextAvailableSBSCode(WebSession webSession, String sbsCodeType, String sbsCodeParentSystem) {

        loadAllSystemEntities(webSession);

        log.debug("PARENT SYSTEM: {}", sbsCodeParentSystem);

        int nextAvailableSBSCodeIndex = 1;

        String parentCode = sbsCodeParentSystem != null ? sbsCodeParentSystem.substring(1) : "";
        int nextCodeLevel = getCodeLevel(parentCode) + 1;

        for (ParentSBSLookupValue parentSBSLookupValue : entities.values()) {
            String currCode = parentSBSLookupValue.SBSCode != null ? parentSBSLookupValue.SBSCode.substring(1) : "";
            int currCodeLevel = getCodeLevel(currCode);

            if (nextCodeLevel == currCodeLevel && currCode.startsWith(parentCode)) {
                log.debug("Fund SBSCodeParentSystem: {} {}",parentCode, currCode);
                nextAvailableSBSCodeIndex++;
            }

        }

        String newSBSCode = sbsCodeType + parentCode + "." + nextAvailableSBSCodeIndex;
        log.debug("NEXT SYSTEM: {}", newSBSCode);

        return newSBSCode;
    }

    private void loadAllSystemEntities(WebSession webSession) {
        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_ALL_SBS_CODE_SQL)) {

            ps.setInt(1, webSession.getCustomerId());
            ps.setInt(2, webSession.getProjectId());
            ps.setInt(3, EntityType.SYSTEMS_BREAKDOWN.getId());
            ps.setInt(4, SBSCODE.getId());

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                //log.debug("SBSCodeParentSystem: {}", rs.getString("StringValue"));
                Integer entityId = rs.getInt("EntityId");
                ParentSBSLookupValue parentSBSLookupValue = new ParentSBSLookupValue(entityId);
                parentSBSLookupValue.setSBSCode(rs.getString("StringValue"));
                entities.put(parentSBSLookupValue.getEntityId(), parentSBSLookupValue);
            }

            log.debug("Load {} parent system entities ", entities.size());

        } catch (SQLException e) {
            log.error("Error loading all system entities including SBS Code: {}", e.getMessage());
        }

        try (Connection con = getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_ALL_SBS_CODE_SQL)) {

            ps.setInt(1, webSession.getCustomerId());
            ps.setInt(2, webSession.getProjectId());
            ps.setInt(3, EntityType.SYSTEMS_BREAKDOWN.getId());
            ps.setInt(4, SYSTEMNAME.getId());

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                ParentSBSLookupValue parentSBSLookupValue;
                Integer entityId = rs.getInt("EntityId");
                parentSBSLookupValue = entities.get(entityId);
                if (parentSBSLookupValue != null) {
                    parentSBSLookupValue.setSystemName(rs.getString("StringValue"));
                }
            }

        } catch (SQLException e) {
            log.error("Error loading all system entities including system names : {}", e.getMessage());
        }
    }

    private List<ParentSBSLookupValue> sortListOfSystemEntries() {
        List<ParentSBSLookupValue> sortedEntities = new ArrayList<>(entities.values());
        sortedEntities.sort(Comparator.comparing(
                ParentSBSLookupValue::getSBSCode,
                Comparator.nullsLast(String::compareToIgnoreCase)
        ));
        return sortedEntities;
    }

    private static class ParentSBSLookupValue {
        private final Integer entityId;
        private String SBSCode;
        private String systemName;
        private long sortableValue;

        private ParentSBSLookupValue(Integer entityId) {
            this.entityId = entityId;
        }

        public Integer getEntityId() {
            return entityId;
        }

        private void setSBSCode(String SBSCode) {
            this.SBSCode = SBSCode;

            if (SBSCode != null) {
                String codeWithoutFunction = SBSCode.substring(1);
                String[] elementArray = codeWithoutFunction.split("\\.");
                sortableValue = 0;
                long factor = 1000;
                for (String element : elementArray) {
                    try {
                        sortableValue = (sortableValue * factor) + (Integer.parseInt(element));
                    } catch (NumberFormatException e) {
                        log.debug("Invalid sortable value {}", element);
                    }
                }
                //log.debug("Sortable value {}", sortableValue);
            }

        }

        private String getSBSCode() {
            return SBSCode;
        }

        private void setSystemName(String systemName) {
            this.systemName = systemName;
        }

        private String getSystemName() {
            return systemName;
        }
    }
}
