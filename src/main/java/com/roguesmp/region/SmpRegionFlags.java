package com.roguesmp.region;

import com.roguesmp.RogueSmpCore;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;

public final class SmpRegionFlags {

    public static final StateFlag ABILITIES = new StateFlag("smp-abilities", true);
    public static final StateFlag SPAWNERS = new StateFlag("smp-spawners", false);
    public static final StateFlag GROUPED = new StateFlag("smp-grouped", false);
    public static final StringFlag DISPLAY_NAME = new StringFlag("smp-display-name", "<gray>Unknown");

    private SmpRegionFlags() {
    }

    public static void register() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            registry.register(ABILITIES);
            registry.register(GROUPED);
            registry.register(SPAWNERS);
            registry.register(DISPLAY_NAME);
        } catch (FlagConflictException e) {
            RogueSmpCore.LOGGER.error("WorldGuard flag {} is already registered by another plugin", ABILITIES.getName(), e);
        }
    }
}
