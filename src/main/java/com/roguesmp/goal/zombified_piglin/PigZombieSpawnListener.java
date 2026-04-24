package com.roguesmp.goal.zombified_piglin;

import com.destroystokyo.paper.entity.ai.MobGoals;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import org.bukkit.entity.PigZombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class PigZombieSpawnListener implements Listener {

    private final Plugin plugin;

    public PigZombieSpawnListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    @SuppressWarnings("UnstableApiUsage")
    public void onPigZombieSpawn(EntityAddToWorldEvent event){
        if(!(event.getEntity() instanceof PigZombie pigZombie)) return;

        MobGoals goals = plugin.getServer().getMobGoals();
        goals.addGoal(pigZombie, 1, new ZombifiedPiglinGoal(pigZombie));
    }
}
