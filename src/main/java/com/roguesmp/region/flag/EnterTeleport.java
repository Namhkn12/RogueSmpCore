package com.roguesmp.region.flag;

import com.roguesmp.teleport.TeleportDestination;
import com.roguesmp.teleport.TeleportOptions;
import com.roguesmp.teleport.effect.NoTeleportEffect;

public record EnterTeleport(TeleportDestination destination, boolean fx) {

    private static final TeleportOptions WITH_FX = TeleportOptions.DEFAULT.withWarmup(20);
    private static final TeleportOptions NEXT_TICK = new TeleportOptions(NoTeleportEffect.INSTANCE, 1, true);

    public TeleportOptions options() {
        return fx ? WITH_FX : NEXT_TICK;
    }
}
