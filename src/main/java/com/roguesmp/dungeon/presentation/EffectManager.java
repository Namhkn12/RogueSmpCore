package com.roguesmp.dungeon.presentation;

import com.roguesmp.dungeon.presentation.effect.DungeonEffect;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;

public class EffectManager {

    private final Plugin plugin;

    public EffectManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void apply(Player player, DungeonEffect data) {
        if (data.getDelay() > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                applyNow(player, data);
            }, data.getDelay());
        } else {
            applyNow(player, data);
        }
    }

    private void applyNow(Player player, DungeonEffect data) {
        if (player == null || !player.isOnline()) return;

        player.addPotionEffect(data.getEffect(), true);
    }

    public void apply(Collection<Player> players, DungeonEffect data) {
        players.forEach(p -> apply(p, data));
    }

    public void clear(Player player, PotionEffectType type) {
        player.removePotionEffect(type);
    }
}
