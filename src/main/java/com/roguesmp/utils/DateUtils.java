package com.roguesmp.utils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class DateUtils {

    public static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private DateUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Gets the current ZonedDateTime in Vietnam Time.
     */
    public static ZonedDateTime now() {
        return ZonedDateTime.now(ZONE_VN);
    }

    /**
     * Gets today's LocalDate in Vietnam Time.
     */
    public static LocalDate today() {
        return LocalDate.now(ZONE_VN);
    }

    /**
     * Calculates the start-of-day boundary (00:00:00) epoch millis for today.
     */
    public static long getTodayStartOfDayTimestamp() {
        return today().atStartOfDay(ZONE_VN).toInstant().toEpochMilli();
    }

    /**
     * Converts epoch milliseconds to a ZonedDateTime in Vietnam Time.
     */
    public static ZonedDateTime toZonedDateTime(long epochMillis) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZONE_VN);
    }

    /**
     * Gets the duration remaining until the next 00:00:00 reset boundary.
     */
    public static Duration getDurationUntilNextReset() {
        ZonedDateTime current = now();
        ZonedDateTime nextReset = current.toLocalDate().plusDays(1).atStartOfDay(ZONE_VN);
        return Duration.between(current, nextReset);
    }

    /**
     * Returns a human-readable string for the time remaining until midnight (e.g. "5 giờ 30 phút" or "15 phút 20 giây").
     */
    public static String getFormattedTimeUntilReset() {
        Duration duration = getDurationUntilNextReset();
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();

        if (hours > 0) {
            return hours + " giờ " + minutes + " phút";
        } else {
            long seconds = duration.toSecondsPart();
            return minutes + " phút " + seconds + " giây";
        }
    }
}
