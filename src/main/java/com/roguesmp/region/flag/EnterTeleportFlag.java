package com.roguesmp.region.flag;

import com.roguesmp.codec.DataResult;
import com.roguesmp.teleport.TeleportDestination;
import com.roguesmp.teleport.TeleportDestinationParser;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;

import java.util.Arrays;

public final class EnterTeleportFlag extends TextValueFlag<EnterTeleport> {

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

        DataResult<TeleportDestination> destination = TeleportDestinationParser.parse(args);
        if (!destination.isSuccess()) throw new InvalidFlagFormat(destination.error() + " (append fx|none to toggle effects)");
        return new EnterTeleport(destination.result(), fx);
    }

    @Override
    protected String format(EnterTeleport value) {
        return value.destination().format() + (value.fx() ? "" : " none");
    }
}
