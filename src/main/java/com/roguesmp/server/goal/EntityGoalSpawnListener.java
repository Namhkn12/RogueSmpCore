package com.roguesmp.server.goal;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.roguesmp.utils.EntityUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Spider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class EntityGoalSpawnListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityAddToWorld(EntityAddToWorldEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof Spider spider) {
            GoalModifiers.modifySpiderGoal(spider);
        }


    }
}
