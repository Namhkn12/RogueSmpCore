package com.roguesmp.dungeon.actor.listener;

import com.roguesmp.dungeon.manager.PartyManager;
import io.papermc.paper.event.block.VaultChangeStateEvent;
import io.papermc.paper.event.player.PlayerPickItemEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class DungeonListener implements Listener {

    private final PartyManager partyManager;

    public DungeonListener(PartyManager partyManager) {
        this.partyManager = partyManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        //handle player join
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        //handle player quit
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event){
        //handle player death in dungeon
    }

    @EventHandler
    public void onPlayerInteractVault(VaultChangeStateEvent event){
        //handle player interact with vault ( door )
    }

    @EventHandler
    public void onPlayerBreakBlock(BlockBreakEvent event){
        //handle player break block in dungeon
    }

    @EventHandler
    public void onPlayerKillMob(EntityDeathEvent event){
        //handle player kill entity in dungeon
    }

    @EventHandler
    public void onPlayerPickUpItem(PlayerPickItemEvent event){
        //handle player pickup item in dungeon
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event){
        //handle player use bed in dungeon
    }

    @EventHandler
    public void onPlayerNetherEnter(PlayerPortalEvent event){
        //handle player go through the portal ( nether or end )
    }
}
