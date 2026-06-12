package com.bepa.eis.server.dataprovider.entities.common;

import com.bepa.eis.server.dataprovider.entities.Entity;

import java.util.Comparator;

public final class EntityComparators {

    private EntityComparators() {
    }

    public static Comparator<Entity> bySortKey() {
        return Comparator.comparing(
                Entity::getSortKey,
                Comparator.nullsLast(String::compareToIgnoreCase)
        );
    }
}