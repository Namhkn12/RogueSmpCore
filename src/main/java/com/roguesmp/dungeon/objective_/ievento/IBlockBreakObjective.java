package com.roguesmp.dungeon.objective_.ievento;

import com.roguesmp.dungeon.objective_.IObjective;
import org.bukkit.event.block.BlockBreakEvent;

public interface IBlockBreakObjective extends IObjective {
    void onBlockBreak(BlockBreakEvent e);
}
