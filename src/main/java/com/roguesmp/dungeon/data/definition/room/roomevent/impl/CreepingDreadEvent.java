package com.roguesmp.dungeon.data.definition.room.roomevent.impl;

import com.roguesmp.dungeon.data.definition.room.roomevent.BaseRoomEvent;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
/*
* Example json config
* {
  "type": "creeping_dread",
  "params": {
    "maxTriggers": 4,
    "spawnChance": 0.25,
    "minInterval": 40,
    "maxInterval": 100
  }
}
* */
public class CreepingDreadEvent extends BaseRoomEvent {

    public static final String TYPE = "creeping_dread";

    private int maxTriggers = 4;
    private double spawnChance = 0.25;
    private int minInterval = 40;
    private int maxInterval = 100;

    public void setMaxInterval(int maxInterval) {
        this.maxInterval = maxInterval;
    }

    public void setMaxTriggers(int maxTriggers) {
        this.maxTriggers = maxTriggers;
    }

    public void setSpawnChance(double spawnChance) {
        this.spawnChance = spawnChance;
    }

    public void setMinInterval(int minInterval) {
        this.minInterval = minInterval;
    }

    private final Map<UUID, Integer> triggerCount = new HashMap<>();
    private final Map<UUID, Long> nextTriggerTick = new HashMap<>();

    @Override
    protected String getType() {
        return TYPE;
    }

    @Override
    protected void onPlayInternal() {
        if (context == null || context.getParty() == null || context.getPartyService() == null) return;

        context.getPartyService().getOnlineMembers(context.getParty()).forEach(player -> {
            UUID id = player.getUniqueId();

            int count = triggerCount.getOrDefault(id, 0);
            if (count >= maxTriggers) return;

            long nextTick = nextTriggerTick.getOrDefault(id, 0L);
            if (tickCount < nextTick) return;

            playCreeperSound(player);

            if (Math.random() < spawnChance) {
                spawnCreeper(player);
            }

            triggerCount.put(id, count + 1);
            nextTriggerTick.put(id, tickCount + randomInterval());
        });
    }

    private void playCreeperSound(Player player) {
        Location loc = player.getLocation().clone().add(
                (Math.random() - 0.5) * 4,
                0,
                (Math.random() - 0.5) * 4
        );

        player.playSound(loc, Sound.ENTITY_CREEPER_PRIMED, 1.0f, 1.0f);
    }

    private void spawnCreeper(Player player) {
        Vector dir = player.getLocation().getDirection().normalize().multiply(-3);
        Location behind = player.getLocation().add(dir);

        player.getWorld().spawn(behind, Creeper.class);
    }

    private int randomInterval() {
        return minInterval + (int)(Math.random() * (maxInterval - minInterval));
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>(super.serialize());
        data.put("maxTriggers", maxTriggers);
        data.put("spawnChance", spawnChance);
        data.put("minInterval", minInterval);
        data.put("maxInterval", maxInterval);
        return data;
    }

    @Override
    public void deserialize(Map<String, Object> data) {
        super.deserialize(data);
        this.maxTriggers = data.get("maxTriggers") instanceof Number n ? n.intValue() : 4;
        this.spawnChance = data.get("spawnChance") instanceof Number n ? n.doubleValue() : 0.25;
        this.minInterval = data.get("minInterval") instanceof Number n ? n.intValue() : 40;
        this.maxInterval = data.get("maxInterval") instanceof Number n ? n.intValue() : 100;
    }
}
