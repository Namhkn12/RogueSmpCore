package com.roguesmp.worldboss.encounter;

import com.roguesmp.entity.SmpEntity;
import com.roguesmp.worldboss.WorldBossManager;
import com.roguesmp.worldboss.runtime.RemovalReason;
import com.roguesmp.worldboss.runtime.WorldBossInstance;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class WorldBossEncounter {

    public abstract @Nullable SmpEntity spawnBoss(WorldBossInstance fight);

    public void onOpen(WorldBossInstance fight) {
    }

    public void onCountdown(WorldBossInstance fight, int secondsLeft) {
    }

    public void onLock(WorldBossInstance fight) {
    }

    public void onRemove(WorldBossInstance fight, Player player, RemovalReason reason) {
    }

    public void onVictory(WorldBossInstance fight, List<Player> survivors) {
        giveRewards(fight);
    }

    protected void giveRewards(WorldBossInstance fight) {
        WorldBossManager.getInstance().grantRewards(fight);
    }

    public void onWipe(WorldBossInstance fight) {
    }

    public void onTimeout(WorldBossInstance fight) {
    }

    public void onCancel(WorldBossInstance fight) {
    }

    protected Announcer server(WorldBossInstance fight) {
        return new Announcer(fight, Bukkit.getOnlinePlayers());
    }

    protected Announcer world(WorldBossInstance fight) {
        return new Announcer(fight, fight.getCenter().getWorld().getPlayers());
    }

    protected Announcer arena(WorldBossInstance fight) {
        List<Player> list = new ArrayList<>();
        for (Player player : fight.getCenter().getWorld().getPlayers()) {
            if (fight.contains(player.getLocation())) {
                list.add(player);
            }
        }
        return new Announcer(fight, list);
    }

    protected Announcer participants(WorldBossInstance fight) {
        return new Announcer(fight, fight.getAlivePlayers());
    }

    protected Announcer player(WorldBossInstance fight, Player player) {
        return new Announcer(fight, List.of(player));
    }
}
