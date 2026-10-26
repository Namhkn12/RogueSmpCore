package com.roguesmp.region.flag;

import com.roguesmp.utils.Utils;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;

public final class SpawnPointFlag extends TextValueFlag<SpawnPoint> {

    public SpawnPointFlag(String name) {
        super(name);
    }

    @Override
    protected SpawnPoint parse(String[] args) throws InvalidFlagFormat {
        if (args.length != 3 && args.length != 5) throw new InvalidFlagFormat("Usage: <x> <y> <z> [yaw pitch]");
        return new SpawnPoint(
                parseDouble(args[0], "x"), parseDouble(args[1], "y"), parseDouble(args[2], "z"),
                args.length == 5 ? (float) parseDouble(args[3], "yaw") : 0f,
                args.length == 5 ? (float) parseDouble(args[4], "pitch") : 0f);
    }

    @Override
    protected String format(SpawnPoint value) {
        return Utils.compactNumber(value.x()) + " " + Utils.compactNumber(value.y()) + " " + Utils.compactNumber(value.z()) + " " + Utils.compactNumber(value.yaw()) + " " + Utils.compactNumber(value.pitch());
    }
}
