package com.bepa.eis.common.scheduling;

import java.time.*;
import java.util.BitSet;

/** Standard five-field cron: minute hour day-of-month month day-of-week. */
public final class CronSchedule {
    private final BitSet minutes, hours, days, months, weekdays;
    private final boolean anyDay, anyWeekday;
    private final ZoneId zone;

    public CronSchedule(String expression, ZoneId zone) {
        String[] fields = expression.trim().split("\\s+");
        if (fields.length != 5) throw new IllegalArgumentException("Cron requires five fields: minute hour day month weekday");
        minutes = parse(fields[0], 0, 59);
        hours = parse(fields[1], 0, 23);
        days = parse(fields[2], 1, 31);
        months = parse(fields[3], 1, 12);
        weekdays = parse(fields[4], 0, 7);
        if (weekdays.get(7)) weekdays.set(0);
        anyDay = fields[2].startsWith("*");
        anyWeekday = fields[4].startsWith("*");
        this.zone = zone;
    }

    public Instant nextAfter(Instant after) {
        // Advancing actual instants handles skipped and repeated DST minutes.
        Instant candidate = after.truncatedTo(java.time.temporal.ChronoUnit.MINUTES).plusSeconds(60);
        Instant limit = candidate.plus(Duration.ofDays(366 * 8L));
        while (candidate.isBefore(limit)) {
            ZonedDateTime local = candidate.atZone(zone);
            if (!months.get(local.getMonthValue()) || !matchesDay(local)) {
                candidate = local.toLocalDate().plusDays(1).atStartOfDay(zone).toInstant();
                continue;
            }
            if (!hours.get(local.getHour())) {
                candidate = candidate.plusSeconds(3600 - local.getMinute() * 60L);
                continue;
            }
            // A single daily time runs once in the autumn overlap; frequent jobs continue every minute.
            boolean singleDailyTime = minutes.cardinality() == 1 && hours.cardinality() == 1;
            if (minutes.get(local.getMinute()) && (!singleDailyTime
                    || local.withEarlierOffsetAtOverlap().toInstant().equals(candidate))) return candidate;
            candidate = candidate.plusSeconds(60);
        }
        throw new IllegalArgumentException("Cron has no occurrence in the next eight years");
    }

    private boolean matchesDay(ZonedDateTime date) {
        boolean day = days.get(date.getDayOfMonth());
        boolean weekday = weekdays.get(date.getDayOfWeek().getValue() % 7);
        return !anyDay && !anyWeekday ? day || weekday : day && weekday;
    }

    private static BitSet parse(String field, int min, int max) {
        BitSet result = new BitSet(max + 1);
        for (String part : field.split(",", -1)) {
            String[] stepped = part.split("/", -1);
            if (stepped.length > 2) throw new IllegalArgumentException("Invalid cron field: " + field);
            int step = stepped.length == 2 ? Integer.parseInt(stepped[1]) : 1;
            if (step < 1 || step > max - min + 1) throw new IllegalArgumentException("Invalid cron step: " + field);
            String range = stepped[0];
            int from, to;
            if (range.equals("*")) { from = min; to = max; }
            else {
                String[] bounds = range.split("-", -1);
                if (bounds.length > 2) throw new IllegalArgumentException("Invalid cron range: " + field);
                from = Integer.parseInt(bounds[0]);
                to = bounds.length == 2 ? Integer.parseInt(bounds[1]) : stepped.length == 2 ? max : from;
            }
            if (from < min || to > max || from > to) throw new IllegalArgumentException("Cron field out of range: " + field);
            for (int value = from; value <= to; value += step) result.set(value);
        }
        return result;
    }
}
