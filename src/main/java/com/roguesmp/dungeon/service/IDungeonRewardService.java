package com.roguesmp.dungeon.service;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public interface IDungeonRewardService {
    boolean onPlace(BlockPlaceEvent e);
    boolean onBreak(BlockBreakEvent e);
    boolean onOpen(PlayerInteractEvent e);
}
