package com.roguesmp.dungeon_v2.data.definition.spawner.behavior;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public interface IBehavior {

    /**
     * @return true = cho phép block break, false = hủy
     */
    boolean onBreak(Player player);

    /**
     * @return true = cho phép spawn, false = hủy
     */
    boolean onSpawn(LivingEntity entity);

    void onTick();

    void onPlayerEnter(Player player);

    boolean isCompleted();

    void reset();

    BehaviorData getData();
}