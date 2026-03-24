package com.roguesmp.dungeon.presentation.sound;

import org.bukkit.Sound;

public class SoundPresets {
    public static DungeonSound DUNGEON_TELEPORT =
        DungeonSound.of(Sound.BLOCK_END_PORTAL_SPAWN, 1f, 1f);

    public static DungeonSound LEVEL_UP =
            DungeonSound.of(Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);

    public static DungeonSound ERROR =
            DungeonSound.of(Sound.ENTITY_VILLAGER_NO, 1f, 0.8f);
}
