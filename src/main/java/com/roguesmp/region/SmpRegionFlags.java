package com.roguesmp.region;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.region.flag.AmbientParticlesFlag;
import com.roguesmp.region.flag.EnterTeleportFlag;
import com.roguesmp.region.flag.SpawnPointFlag;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;

import java.util.List;

public final class SmpRegionFlags {

    public static final StateFlag ABILITIES = new StateFlag("smp-abilities", true);
    public static final StateFlag SPAWNERS = new StateFlag("smp-spawners", false);
    public static final StateFlag GROUPED = new StateFlag("smp-grouped", false);
    public static final StateFlag DEATH_CURSE = new StateFlag("smp-death-curse", false);
    public static final StringFlag DISPLAY_NAME = new StringFlag("smp-display-name", "<gray>Unknown");
    public static final AmbientParticlesFlag AMBIENT_PARTICLES = new AmbientParticlesFlag("smp-ambient-particles");
    public static final SpawnPointFlag SPAWN = new SpawnPointFlag("smp-spawn");
    public static final EnterTeleportFlag ENTER_TELEPORT = new EnterTeleportFlag("smp-enter-teleport");

    private SmpRegionFlags() {
    }

    public static void register() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        for (Flag<?> flag : List.of(ABILITIES, GROUPED, SPAWNERS, DEATH_CURSE, DISPLAY_NAME, AMBIENT_PARTICLES, SPAWN, ENTER_TELEPORT)) {
            try {
                registry.register(flag);
            } catch (FlagConflictException e) {
                RogueSmpCore.LOGGER.error("WorldGuard flag {} is already registered by another plugin", flag.getName(), e);
            }
        }
    }
}
