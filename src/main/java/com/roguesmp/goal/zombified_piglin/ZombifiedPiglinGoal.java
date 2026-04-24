package com.roguesmp.goal.zombified_piglin;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import com.roguesmp.dungeon.utils.NameSpaceKeys;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;

import java.util.EnumSet;

public class ZombifiedPiglinGoal implements Goal<PigZombie> {

    private final PigZombie pigZombie;
    private Player player;
    private int lostSightTicks = 0;
    private static final int MAX_LOST_SIGHT_TICKS = 60; // 3s

    public ZombifiedPiglinGoal(PigZombie pigZombie) {
        this.pigZombie = pigZombie;
    }

    @Override
    public boolean shouldActivate() {
        player = pigZombie.getNearbyEntities(16, 8, 16).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .filter(pigZombie::hasLineOfSight)
                .findFirst()
                .orElse(null);
        return player != null;
    }

    @Override
    public boolean shouldStayActive() {
        if (player == null || player.isDead()) return false;
        if (pigZombie.getLocation().distanceSquared(player.getLocation()) > 256) return false;

        if (pigZombie.hasLineOfSight(player)) {
            lostSightTicks = 0;
        } else {
            lostSightTicks++;
        }

        return lostSightTicks <= MAX_LOST_SIGHT_TICKS;
    }

    @Override
    public void start() {
        pigZombie.setTarget(player);
    }

    @Override
    public void stop() {
        player = null;
        lostSightTicks = 0;
        pigZombie.setTarget(null);
    }

    @Override
    public void tick() {}

    @Override
    public GoalKey<PigZombie> getKey() {
        return GoalKey.of(PigZombie.class, NameSpaceKeys.PIGZOMBIE_KEY);
    }

    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.TARGET, GoalType.MOVE);
    }
}
