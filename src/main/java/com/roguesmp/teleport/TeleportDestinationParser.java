package com.roguesmp.teleport;

import com.roguesmp.codec.DataResult;
import com.roguesmp.registry.Registries;
import com.roguesmp.teleport.destination.*;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;

import java.util.List;

public final class TeleportDestinationParser {

    public static final String USAGE = "region <id> [world] | spawn [world] | hub | warp <name> | player <name> | [world] <x> <y> <z> [yaw pitch]";
    public static final List<String> KEYWORDS = List.of("region", "spawn", "hub", "warp", "player");

    private TeleportDestinationParser() {
    }

    public static DataResult<TeleportDestination> parse(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return DataResult.error("Destination cannot be empty. Usage: " + USAGE);
        return parse(trimmed.split("\\s+"));
    }

    public static DataResult<TeleportDestination> parse(String[] args) {
        try {
            return DataResult.success(parseOrThrow(args));
        } catch (IllegalArgumentException e) {
            return DataResult.error(e.getMessage());
        }
    }

    private static TeleportDestination parseOrThrow(String[] args) {
        String head = args[0].toLowerCase();
        return switch (head) {
            case "region" -> {
                expectLength(args, 2, 3);
                yield new RegionDestination(args[1], args.length == 3 ? world(args[2]) : null);
            }
            case "spawn" -> {
                expectLength(args, 1, 2);
                yield new WorldSpawnDestination(args.length == 2 ? world(args[1]) : null);
            }
            case "hub" -> {
                expectLength(args, 1, 1);
                yield HubDestination.INSTANCE;
            }
            case "warp" -> {
                expectLength(args, 2, 2);
                yield new WarpDestination(Registries.WARP.getHolder(args[1]));
            }
            case "player" -> {
                expectLength(args, 2, 2);
                yield new PlayerDestination(args[1]);
            }
            default -> {
                if (args.length == 1 && !Utils.isNumber(args[0]) && args[0].indexOf(':') < 0) yield new PlayerDestination(args[0]);
                yield coordinates(args);
            }
        };
    }

    private static LocationDestination coordinates(String[] args) {
        boolean sameWorld = Utils.isNumber(args[0]);
        int offset = sameWorld ? 0 : 1;
        int numbers = args.length - offset;
        if (numbers != 3 && numbers != 5) throw new IllegalArgumentException("Usage: " + USAGE);
        return new LocationDestination(sameWorld ? null : world(args[0]),
                number(args[offset], "x"), number(args[offset + 1], "y"), number(args[offset + 2], "z"),
                numbers == 5 ? (float) number(args[offset + 3], "yaw") : null,
                numbers == 5 ? (float) number(args[offset + 4], "pitch") : null);
    }

    private static void expectLength(String[] args, int min, int max) {
        if (args.length < min || args.length > max) throw new IllegalArgumentException("Usage: " + USAGE);
    }

    private static Key world(String text) {
        try {
            return Key.key(text);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException("'" + text + "' is not a valid world key like minecraft:overworld.");
        }
    }

    private static double number(String text, String field) {
        Double value = Utils.parseDouble(text);
        if (value == null) throw new IllegalArgumentException("'" + text + "' is not a valid " + field + ".");
        return value;
    }
}
