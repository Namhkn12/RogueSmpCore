package com.roguesmp.player.inbox;

import com.roguesmp.RogueSmpCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class InboxListener implements Listener {

    private final InboxManager inboxManager;

    public InboxListener(InboxManager inboxManager) {
        this.inboxManager = inboxManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerPreJoin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
        inboxManager.stagePendingJoin(event.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!inboxManager.cachePendingJoin(event.getPlayer().getUniqueId())) {
            RogueSmpCore.LOGGER.error("No inbox was loaded for {} before joining", event.getPlayer().getName());
            return;
        }
        inboxManager.notifyPending(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLeave(PlayerQuitEvent event) {
        inboxManager.saveAndUncache(event.getPlayer().getUniqueId());
    }
}
