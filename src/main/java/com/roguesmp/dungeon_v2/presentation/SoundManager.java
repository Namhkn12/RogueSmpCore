package com.roguesmp.dungeon_v2.presentation;

import com.roguesmp.dungeon_v2.presentation.sound.DungeonSound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.List;

public class SoundManager {

    private final Plugin plugin;
    public SoundManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void play(Player player, DungeonSound data) {
        if (data == null) return;
        if (data.getDelay() > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                playNow(player, data);
            }, data.getDelay());
        } else {
            playNow(player, data);
        }
    }

    private void playNow(Player player, DungeonSound data) {
        if (data == null) return;
        if (player == null || !player.isOnline()) return;
        player.playSound(
                player.getLocation(),
                data.getSound(),
                data.getVolume(),
                data.getPitch()
        );
    }

    public void playSequence(Player player, List<DungeonSound> list) {
        if (list == null || list.isEmpty()) return;
        long totalDelay = 0;

        for (DungeonSound s : list) {
            long finalDelay = totalDelay + s.getDelay();

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                playNow(player, s);
            }, finalDelay);

            totalDelay += s.getDelay();
        }
    }

    public void play(Collection<Player> players, DungeonSound data) {
        if (data == null) return;
        players.forEach(p -> play(p, data));
    }

    public void play(Player player, Location loc, DungeonSound data) {
        if (data == null) return;
        player.playSound(loc, data.getSound(), data.getVolume(), data.getPitch());
    }

}