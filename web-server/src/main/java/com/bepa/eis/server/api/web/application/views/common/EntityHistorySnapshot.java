package com.bepa.eis.server.api.web.application.views.common;

import com.bepa.eis.common.enums.entity.EntityType;

/** Resolves independently versioned rows at the end of an entity version's validity period. */
public record EntityHistorySnapshot(EntityType entityType, Integer entityId, Integer version) {
    public String cutoff(String alias) {
        // Only typed integer identifiers are interpolated; tenant scope comes from the outer row.
        // The exclusive next-version boundary also includes relations saved just after the
        // entity row in the same transaction. Latest versions include the current state.
        return "(SELECT COALESCE((SELECT MIN(N.ChangedDateTime) FROM ENTITY N "
                + "WHERE N.CustomerId = S.CustomerId AND N.ProjectId = S.ProjectId "
                + "AND N.EntityType = S.EntityType AND N.EntityId = S.EntityId AND N.Version > S.Version), CURRENT_TIMESTAMP) FROM ENTITY S "
                + "WHERE S.CustomerId = " + alias + ".CustomerId AND S.ProjectId = " + alias + ".ProjectId "
                + "AND S.EntityType = " + entityType.getId() + " AND S.EntityId = " + entityId
                + " AND S.Version = " + version + ")";
    }

    public String predicate(String table, String alias, String timeColumn, String... identityColumns) {
        StringBuilder sql = new StringBuilder(alias + "." + timeColumn + " < " + cutoff(alias));
        sql.append(" AND NOT EXISTS (SELECT 1 FROM ").append(table).append(" H WHERE ")
                .append("H.CustomerId = ").append(alias).append(".CustomerId AND H.ProjectId = ")
                .append(alias).append(".ProjectId");
        for (String column : identityColumns) {
            sql.append(" AND H.").append(column).append(" = ").append(alias).append('.').append(column);
        }
        sql.append(" AND H.").append(timeColumn).append(" < ").append(cutoff(alias))
                .append(" AND H.Version > ").append(alias).append(".Version)");
        String sourceLatest = "(SELECT S.Latest FROM ENTITY S WHERE S.CustomerId = " + alias + ".CustomerId "
                + "AND S.ProjectId = " + alias + ".ProjectId AND S.EntityType = " + entityType.getId()
                + " AND S.EntityId = " + entityId + " AND S.Version = " + version + ")";
        // In particular, do not revive interfaces whose final row has Latest=0 after removal.
        return "(CASE WHEN " + sourceLatest + " = 1 THEN CASE WHEN " + alias + ".Latest = 1 THEN 1 ELSE 0 END "
                + "ELSE CASE WHEN " + sql + " THEN 1 ELSE 0 END END = 1)";
    }

    public String entityPredicate(String alias) {
        return predicate("ENTITY", alias, "ChangedDateTime", "EntityType", "EntityId");
    }
}
