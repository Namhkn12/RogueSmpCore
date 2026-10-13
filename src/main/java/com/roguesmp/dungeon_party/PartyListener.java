package com.roguesmp.dungeon_party;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PartyListener implements Listener {

    private final PartyManager partyManager;

    public PartyListener(PartyManager partyManager) {
        this.partyManager = partyManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        partyManager.getInviteManager().handlePlayerQuit(event.getPlayer());
    }
}
