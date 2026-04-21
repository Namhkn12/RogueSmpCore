package com.roguesmp.dungeon.data.definition.spawner.behavior;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Quản lý nhiều IBehavior chạy song song trên một spawner.
 *
 * Quy tắc aggregation:
 *   onBreak  → false (hủy break) nếu BẤT KỲ behavior nào trả false
 *   onSpawn  → false (hủy spawn) nếu BẤT KỲ behavior nào trả false
 *   onTick / onPlayerEnter → chạy tất cả, không aggregate
 */
public class BehaviorPipeline {

    private final List<IBehavior> behaviors;

    public BehaviorPipeline(List<IBehavior> behaviors) {
        this.behaviors = Collections.unmodifiableList(behaviors);
    }

    /** @return true nếu tất cả behaviors đều cho phép break */
    public boolean runBreak(Player player, Location location) {
        boolean allow = true;
        for (IBehavior b : behaviors) {
            if (!b.onBreak(player, location)) allow = false;
        }
        return allow;
    }

    /** @return true nếu tất cả behaviors đều cho phép spawn */
    public boolean runSpawn(LivingEntity entity, List<Player> players,Location location) {
        boolean allow = true;
        for (IBehavior b : behaviors) {
            if (!b.onSpawn(entity, players,location)) allow = false;
        }
        return allow;
    }

    public void runTick() {
        behaviors.forEach(IBehavior::onTick);
    }

    public void runPlayerEnter(Player player) {
        behaviors.forEach(b -> b.onPlayerEnter(player));
    }

    /** true nếu TẤT CẢ behaviors đã hoàn thành */
    public boolean allCompleted() {
        return behaviors.stream().allMatch(IBehavior::isCompleted);
    }

    public void resetAll() {
        behaviors.forEach(IBehavior::reset);
    }

    public List<IBehavior> getBehaviors() {
        return behaviors;
    }
}