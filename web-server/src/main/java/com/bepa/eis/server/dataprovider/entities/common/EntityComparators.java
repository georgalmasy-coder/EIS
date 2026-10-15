package com.bepa.eis.server.dataprovider.entities.common;

import com.bepa.eis.server.dataprovider.entities.Entity;

import java.util.Comparator;
import java.util.function.Function;

public final class EntityComparators {

    private EntityComparators() {
    }

    public static Comparator<Entity> bySortKey() {
        return bySortKey(Entity::getSortKey);
    }

    public static <T> Comparator<T> bySortKey(Function<T, String> sortKey) {
        return Comparator.comparing(
                sortKey,
                Comparator.nullsLast(String::compareToIgnoreCase)
        );
    }
}
