package com.roguesmp.gate.cost;

import com.roguesmp.player.SmpPlayer;
import org.bukkit.entity.Player;

import java.util.Collection;

public final class CostPayment {

    private CostPayment() {
    }

    public static boolean canAfford(SmpPlayer player, Collection<? extends Cost> costs) {
        for (Cost cost : costs) {
            if (!cost.canAfford(player)) {
                return false;
            }
        }
        return true;
    }

    public static boolean pay(SmpPlayer player, Collection<? extends Cost> costs) {
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null || !canAfford(player, costs)) return false;

        costs.forEach(cost -> cost.takeFromInventory(bukkitPlayer.getInventory()));
        costs.forEach(cost -> cost.takeFromPlayer(player));
        return true;
    }

    public static void refund(SmpPlayer player, Collection<? extends Cost> costs) {
        costs.forEach(cost -> cost.refund(player));
    }
}
