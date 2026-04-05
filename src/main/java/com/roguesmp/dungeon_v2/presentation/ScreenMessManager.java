package com.roguesmp.dungeon_v2.presentation;

import com.roguesmp.dungeon_v2.presentation.message.ScreenMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import static me.clip.placeholderapi.util.Msg.color;

public class ScreenMessManager {

    private final Plugin plugin;

    public ScreenMessManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void send(Player player, ScreenMessage msg) {
        if (msg.getDelay() > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                sendNow(player, msg);
            }, msg.getDelay());
        } else {
            sendNow(player, msg);
        }
    }

    private void sendNow(Player player, ScreenMessage msg) {
        if (player == null || !player.isOnline()) return;

        switch (msg.getType()) {
            case TITLE -> sendTitle(player, msg);
            case ACTIONBAR -> sendActionBar(player, msg);
        }
    }

    private void sendTitle(Player p, ScreenMessage msg) {
        p.sendTitle(
                color(msg.getTitle()),
                color(msg.getSubtitle()),
                msg.getFadeIn(),
                msg.getStay(),
                msg.getFadeOut()
        );
    }

    private void sendActionBar(Player p, ScreenMessage msg) {
        p.sendActionBar(color(msg.getTitle()));
    }

}
