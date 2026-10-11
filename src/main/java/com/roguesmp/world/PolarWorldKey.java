package com.roguesmp.world;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;

import java.util.regex.Pattern;

/**
 * Identifies a polar world along with its manager: the namespace is the
 * {@link PolarWorldManager} that owns it and the name is unique inside that manager. Written as {@code namespace:name},
 * for example {@code template:test_dungeon}. {@link #value()} is the actual PolarPaper world key and file name, so it
 * never needs to be built by hand.
 */
public record PolarWorldKey(String namespace, String name) {

    public static final Codec<PolarWorldKey> CODEC = Codec.STRING.comapFlatMap(PolarWorldKey::parse, PolarWorldKey::asString);

    static final char VALUE_SEPARATOR = '_';

    private static final Pattern PART = Pattern.compile("[a-z0-9._-]+");

    public PolarWorldKey {
        if (!PART.matcher(namespace).matches()) throw new IllegalArgumentException("Invalid world namespace '" + namespace + "', use a-z, 0-9, '.', '_' and '-'");
        if (!PART.matcher(name).matches()) throw new IllegalArgumentException("Invalid world name '" + name + "', use a-z, 0-9, '.', '_' and '-'");
    }

    public static DataResult<PolarWorldKey> parse(String text) {
        int separator = text.indexOf(':');
        if (separator < 0 || separator != text.lastIndexOf(':')) {
            return DataResult.error("Invalid world key '" + text + "', expected namespace:name");
        }

        try {
            return DataResult.success(new PolarWorldKey(text.substring(0, separator), text.substring(separator + 1)));
        } catch (IllegalArgumentException e) {
            return DataResult.error(e.getMessage());
        }
    }

    /**
     * The PolarPaper world key and file name.
     */
    public String value() {
        return namespace + VALUE_SEPARATOR + name;
    }

    public String asString() {
        return namespace + ":" + name;
    }

    @Override
    public String toString() {
        return asString();
    }
}
