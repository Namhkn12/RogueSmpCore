package com.roguesmp.region.flag;

import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;
import org.bukkit.NamespacedKey;

import java.util.Arrays;

public final class EnterTeleportFlag extends TextValueFlag<EnterTeleport> {

    private static final String REGION_KEYWORD = "region";
    private static final String USAGE = "Usage: region <regionId> [namespace:world] [fx|none] or [namespace:world] <x> <y> <z> [yaw pitch] [fx|none]";

    public EnterTeleportFlag(String name) {
        super(name);
    }

    @Override
    protected EnterTeleport parse(String[] args) throws InvalidFlagFormat {
        boolean fx = true;
        String last = args[args.length - 1].toLowerCase();
        if (args.length > 1 && (last.equals("fx") || last.equals("none"))) {
            fx = last.equals("fx");
            args = Arrays.copyOf(args, args.length - 1);
        }
        return new EnterTeleport(parseTarget(args), fx);
    }

    private EnterTeleport.Target parseTarget(String[] args) throws InvalidFlagFormat {
        if (args[0].equalsIgnoreCase(REGION_KEYWORD)) {
            if (args.length != 2 && args.length != 3) throw new InvalidFlagFormat(USAGE);
            return new EnterTeleport.RegionTarget(args[1], args.length == 3 ? parseWorld(args[2]) : null);
        }

        boolean sameWorld = isNumber(args[0]);
        int offset = sameWorld ? 0 : 1;
        int numbers = args.length - offset;
        if (numbers != 3 && numbers != 5) throw new InvalidFlagFormat(USAGE);
        return new EnterTeleport.LocationTarget(sameWorld ? null : parseWorld(args[0]),
                parseDouble(args[offset], "x"), parseDouble(args[offset + 1], "y"), parseDouble(args[offset + 2], "z"),
                numbers == 5 ? (float) parseDouble(args[offset + 3], "yaw") : null,
                numbers == 5 ? (float) parseDouble(args[offset + 4], "pitch") : null);
    }

    private static NamespacedKey parseWorld(String text) throws InvalidFlagFormat {
        NamespacedKey key = text.indexOf(':') > 0 ? NamespacedKey.fromString(text.toLowerCase()) : null;
        if (key == null) throw new InvalidFlagFormat("'" + text + "' is not a full world key like minecraft:overworld.");
        return key;
    }

    private static boolean isNumber(String text) {
        try {
            Double.parseDouble(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    protected String format(EnterTeleport value) {
        return value.target().format() + (value.fx() ? "" : " none");
    }
}
