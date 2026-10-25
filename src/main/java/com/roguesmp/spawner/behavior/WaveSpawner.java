package com.roguesmp.spawner.behavior;

import com.roguesmp.utils.LocationUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Example json config
 *
 * {
 *   "spawnerId": "undead_wave_nest",
 *   "behaviors": [
 *     {
 *       "type": "wave",
 *       "waves": 5
 *     }
 *   ]
 * }
 *
 * */
public class WaveSpawner extends BaseBehavior {

    private static final Particle.DustOptions BLOOD = new Particle.DustOptions(Color.fromRGB(200, 20, 20), 1.4f);

    private final int waves;
    private int waveCount = 0;

    public WaveSpawner(BehaviorData data) {
        super(data);
        this.waves = data.getInt("waves", 3);
    }

    @Override
    public boolean onBreak(Player player, Location location) {
        if(completed) return true;

        if(waveCount < waves){
            player.sendMessage(Component.text("You must survive " + (waves - waveCount) + " more waves.", NamedTextColor.GRAY));
            SpawnerFx.sound(SpawnerFx.center(location), Sound.BLOCK_CHAIN_HIT, 2f, 0.5f);
            return false;
        }

        player.sendMessage(Component.text("All waves cleared. The spawner is now vulnerable.", NamedTextColor.GREEN));
        return true;
    }

    @Override
    public boolean onSpawn(LivingEntity entity, List<Player> players, Location location) {
        if (completed) return true;

        waveCount++;
        if (waveCount >= waves) completed = true;

        int extra = Math.max(0, players.size() + 2);
        World world = location.getWorld();

        if (world == null) return true;

        Location center = SpawnerFx.center(location);
        world.spawnParticle(Particle.FLAME, center, 60, 0.4, 0.4, 0.4, 0.25, null, true);
        world.spawnParticle(Particle.EXPLOSION, center, 1, 0, 0, 0, 0, null, true);
        SpawnerFx.sound(center, Sound.EVENT_RAID_HORN, 4f, 1f);
        if (completed) SpawnerFx.sound(center, Sound.UI_TOAST_CHALLENGE_COMPLETE, 3f, 1f);

        for (int i = 0; i < extra; i++) {
            Location spawnLoc = LocationUtils.randomStandableNear(location);
            entity.copy(spawnLoc);
        }

        return true;
    }

    @Override
    protected void onTick(Location location, int ticks) {
        if (completed) return;
        Location center = SpawnerFx.center(location);
        SpawnerFx.helix(center, 3, 0.7, ticks * 0.35, Particle.DUST, BLOOD);
        if (ticks % 12 == 0) SpawnerFx.sound(center, Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT, 2.5f, 0.6f);
    }

    @Override
    public void reset() {
        super.reset();
        waveCount = 0;
    }
}
