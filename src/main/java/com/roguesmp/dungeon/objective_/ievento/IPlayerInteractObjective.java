package com.roguesmp.dungeon.objective_.ievento;

import com.roguesmp.dungeon.objective_.IObjective;
import org.bukkit.event.player.PlayerInteractEvent;

public interface IPlayerInteractObjective extends IObjective {
    void onPlayerInteract(PlayerInteractEvent e);
}
