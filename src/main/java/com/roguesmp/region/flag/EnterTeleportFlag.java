package com.roguesmp.region.flag;

import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;

import java.util.Arrays;

public final class EnterTeleportFlag extends TextValueFlag<EnterTeleport> {

    private static final String REGION_PREFIX = "region:";
    private static final String USAGE = "Usage: region:[world:]<regionId> [fx|none] or [world] <x> <y> <z> [yaw pitch] [fx|none]";

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
        if (args.length == 1 && args[0].regionMatches(true, 0, REGION_PREFIX, 0, REGION_PREFIX.length())) {
            String[] parts = args[0].substring(REGION_PREFIX.length()).split(":", 2);
            if (Arrays.stream(parts).anyMatch(String::isEmpty)) throw new InvalidFlagFormat(USAGE);
            return parts.length == 1
                    ? new EnterTeleport.RegionTarget(null, parts[0])
                    : new EnterTeleport.RegionTarget(parts[0], parts[1]);
        }

        boolean sameWorld = isNumber(args[0]);
        int offset = sameWorld ? 0 : 1;
        int numbers = args.length - offset;
        if (numbers != 3 && numbers != 5) throw new InvalidFlagFormat(USAGE);
        return new EnterTeleport.LocationTarget(sameWorld ? null : args[0],
                parseDouble(args[offset], "x"), parseDouble(args[offset + 1], "y"), parseDouble(args[offset + 2], "z"),
                numbers == 5 ? (float) parseDouble(args[offset + 3], "yaw") : null,
                numbers == 5 ? (float) parseDouble(args[offset + 4], "pitch") : null);
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
