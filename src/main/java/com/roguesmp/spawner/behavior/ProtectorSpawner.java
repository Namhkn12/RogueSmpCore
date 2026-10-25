package com.roguesmp.spawner.behavior;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Example json config
 *
 * {
 *   "spawnerId": "boss_guardian",
 *   "behaviors": [
 *     {
 *       "type": "protector",
 *       "requireBreak": 5
 *     }
 *   ]
 * }
 *
 * */
public class ProtectorSpawner extends BaseBehavior {

    private static final Particle.DustOptions SHIELD = new Particle.DustOptions(Color.fromRGB(80, 200, 255), 1.2f);

    private final int requireBreak;
    private int brokenCount = 0;

    public ProtectorSpawner(BehaviorData data) {
        super(data);
        this.requireBreak = data.getInt("requireBreak", 3);
    }

    @Override
    public boolean onBreak(Player player, Location location) {
        if (completed) return true;

        brokenCount++;
        int remaining = requireBreak - brokenCount;

        if (remaining > 0) {
            sendMessage(player, "The spawner shield active —  " + remaining + " break remaining.");
            Location center = SpawnerFx.center(location);
            center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, center, 30, 0.6, 0.6, 0.6, 0.3, null, true);
            SpawnerFx.sound(center, Sound.ITEM_SHIELD_BLOCK, 2f, 0.8f);
            SpawnerFx.sound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 2f, 0.5f + 1.5f * brokenCount / requireBreak);
            return false;
        }

        completed = true;
        sendMessage(player, "Spawner defenses destroyed!");
        Location center = SpawnerFx.center(location);
        center.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, center, 60, 0.5, 0.8, 0.5, 0.4, null, true);
        SpawnerFx.sound(center, Sound.BLOCK_GLASS_BREAK, 3f, 0.6f);
        SpawnerFx.sound(center, Sound.BLOCK_BEACON_DEACTIVATE, 3f, 1f);
        return true;
    }

    @Override
    protected void onTick(Location location, int ticks) {
        if (completed) return;
        Location center = SpawnerFx.center(location);
        SpawnerFx.ring(center, 1.2, 16, ticks * 0.2, Particle.DUST, SHIELD);
        if (ticks % 16 == 0) SpawnerFx.sound(center, Sound.BLOCK_BEACON_AMBIENT, 2.5f, 1.2f);
    }

    @Override
    public void reset() {
        super.reset();
        brokenCount = 0;
    }
}