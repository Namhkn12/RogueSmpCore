package com.roguesmp.quest.board;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.utils.DateUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Splits time into fixed windows of {@code period}, written as an ISO-8601 duration ({@code "P1D"} daily,
 * {@code "P2D"} every 2 days, {@code "P7D"} weekly). Windows are counted from Monday 2026-01-05 00:00 server time, so
 * every quest with the same period resets at the same moment.
 */
public record ResetSchedule(Duration period) {

    private static final long ANCHOR = LocalDateTime.of(2026, 1, 5, 0, 0).atZone(DateUtils.ZONE_VN).toInstant().toEpochMilli();

    public static final Codec<ResetSchedule> CODEC = Codec.STRING.comapFlatMap(text -> {
        try {
            Duration duration = Duration.parse(text);
            return duration.isPositive() ? DataResult.success(new ResetSchedule(duration)) : DataResult.error("Reset period must be positive: " + text);
        } catch (DateTimeParseException e) {
            return DataResult.error("Invalid reset period: " + text);
        }
    }, schedule -> schedule.period().toString());

    public long cycleAt(long time) {
        return Math.floorDiv(time - ANCHOR, period.toMillis());
    }

    public long cycleEnd(long cycle) {
        return ANCHOR + (cycle + 1) * period.toMillis();
    }
}
