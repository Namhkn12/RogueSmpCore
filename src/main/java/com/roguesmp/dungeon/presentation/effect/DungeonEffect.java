package com.roguesmp.dungeon.presentation.effect;

import org.bukkit.potion.PotionEffect;

public class DungeonEffect {

    private final PotionEffect effect;
    private final long delay;

    public DungeonEffect(PotionEffect effect, long delay) {
        this.effect = effect;
        this.delay = delay;
    }

    public PotionEffect getEffect() { return effect; }
    public long getDelay() { return delay; }

    public static DungeonEffect of(PotionEffect effect) {
        return new DungeonEffect(effect, 0);
    }

    public DungeonEffect delay(long delay) {
        return new DungeonEffect(effect, delay);
    }
}
