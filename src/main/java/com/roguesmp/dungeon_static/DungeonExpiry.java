package com.roguesmp.dungeon_static;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.utils.DateUtils;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * How long a new instance lasts, written as an amount and a unit such as {@code 30m}, {@code 1h}, {@code 2d} or {@code 1w}.
 * Minutes and hours count from the moment of creation. Days and weeks are calendar based: {@code 2d} lasts until the end
 * of tomorrow, and {@code 1w} until the end of the current Monday-Sunday week.
 */
public record DungeonExpiry(int amount, Unit unit) {

    public enum Unit {
        MINUTES('m'),
        HOURS('h'),
        DAYS('d'),
        WEEKS('w');

        private final char symbol;

        Unit(char symbol) {
            this.symbol = symbol;
        }

        static Unit of(char symbol) {
            for (Unit unit : values()) {
                if (unit.symbol == symbol) return unit;
            }
            throw new IllegalArgumentException("Unknown unit: " + symbol);
        }
    }

    public static final DungeonExpiry DEFAULT = new DungeonExpiry(1, Unit.WEEKS);

    private static final Pattern FORMAT = Pattern.compile("(\\d+)([mhdw])");

    public static final Codec<DungeonExpiry> CODEC = Codec.STRING.comapFlatMap(DungeonExpiry::parse, DungeonExpiry::toString);

    public static DataResult<DungeonExpiry> parse(String text) {
        Matcher matcher = FORMAT.matcher(text.trim().toLowerCase());
        if (!matcher.matches()) return DataResult.error("Invalid expiry '" + text + "', expected e.g. 30m, 1h, 2d, 1w");

        int amount = Integer.parseInt(matcher.group(1));
        if (amount < 1) return DataResult.error("Expiry amount must be at least 1: " + text);
        return DataResult.success(new DungeonExpiry(amount, Unit.of(matcher.group(2).charAt(0))));
    }

    public long expiresAt(ZonedDateTime now) {
        ZonedDateTime expiry = switch (unit) {
            case MINUTES -> now.plusMinutes(amount);
            case HOURS -> now.plusHours(amount);
            case DAYS -> now.toLocalDate().plusDays(amount).atStartOfDay(DateUtils.ZONE_VN);
            case WEEKS -> now.toLocalDate().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(amount - 1L).atStartOfDay(DateUtils.ZONE_VN);
        };
        return expiry.toInstant().toEpochMilli();
    }

    @Override
    public String toString() {
        return amount + String.valueOf(unit.symbol);
    }
}
