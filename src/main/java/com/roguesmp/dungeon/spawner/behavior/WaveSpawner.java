package com.roguesmp.dungeon.spawner.behavior;

import com.roguesmp.utils.LocationUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
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

        for (int i = 0; i < extra; i++) {
            Location spawnLoc = LocationUtils.randomStandableNear(location);
            entity.copy(spawnLoc);
        }

        return true;
    }

    @Override
    public void reset() {
        super.reset();
        waveCount = 0;
    }
}
