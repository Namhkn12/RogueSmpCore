package com.roguesmp.dungeon_v2.data.definition.spawner.behavior.impl;

import com.roguesmp.dungeon_v2.data.definition.spawner.behavior.BaseBehavior;
import com.roguesmp.dungeon_v2.data.definition.spawner.behavior.BehaviorData;
import org.bukkit.entity.Player;

public class AlertSpawner extends BaseBehavior {

    private final String alertMessage;

    public AlertSpawner(BehaviorData data) {
        super(data);
        this.alertMessage = data.getString("message", "Kẻ xâm nhập đã bị phát hiện!");
    }

    @Override
    public void onPlayerEnter(Player player) {
        sendMessage(player, alertMessage);
        // Có thể thêm: kích hoạt alarm, spawn thêm mob, v.v.
    }
}