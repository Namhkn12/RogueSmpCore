package com.roguesmp.dungeon.objective_.ievento;

import com.roguesmp.dungeon.objective_.IObjective;
import org.bukkit.event.entity.EntityPickupItemEvent;

public interface IItemCollectObjective extends IObjective {
    void onItemCollect(EntityPickupItemEvent e);
}
