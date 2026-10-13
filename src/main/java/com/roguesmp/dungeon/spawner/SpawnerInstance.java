package com.roguesmp.dungeon.spawner;

import com.roguesmp.dungeon.spawner.behavior.BaseBehavior;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

public class SpawnerInstance {
    private final String iid;
    private final String templateId;
    private final Location location;
    private final List<BaseBehavior> behaviors;

    public SpawnerInstance(String iid, String templateId, Location location, List<BaseBehavior> behaviors) {
        this.iid = iid;
        this.templateId = templateId;
        this.location = location;
        this.behaviors = List.copyOf(behaviors);
    }

    public boolean runBreak(Player player, Location location) {
        boolean allow = true;
        for (BaseBehavior behavior : behaviors) {
            if (!behavior.onBreak(player, location)) allow = false;
        }
        return allow;
    }

    public boolean runSpawn(LivingEntity entity, List<Player> players, Location location) {
        boolean allow = true;
        for (BaseBehavior behavior : behaviors) {
            if (!behavior.onSpawn(entity, players, location)) allow = false;
        }
        return allow;
    }

    public String getId() {
        return iid;
    }

    public String getTemplateId() {
        return templateId;
    }

    public List<BaseBehavior> getBehaviors() {
        return behaviors;
    }

    public Location getLocation() {
        return location;
    }
}
