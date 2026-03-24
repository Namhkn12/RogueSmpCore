package com.roguesmp.dungeon.presentation.sound;

import org.bukkit.Sound;

public class DungeonSound {
    private final Sound sound;
    private final float volume;
    private final float pitch;
    private final long delay; // tick

    public DungeonSound(Sound sound, float volume, float pitch, long delay) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
        this.delay = delay;
    }

    public Sound getSound() { return sound; }
    public float getVolume() { return volume; }
    public float getPitch() { return pitch; }
    public long getDelay() { return delay; }

    // factory cho tiện dùng
    public static DungeonSound of(Sound sound) {
        return new DungeonSound(sound, 1f, 1f, 0);
    }

    public static DungeonSound of(Sound sound, float volume, float pitch) {
        return new DungeonSound(sound, volume, pitch, 0);
    }

    public DungeonSound delay(long delay) {
        return new DungeonSound(sound, volume, pitch, delay);
    }
}
