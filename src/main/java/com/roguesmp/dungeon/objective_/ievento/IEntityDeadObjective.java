package com.roguesmp.dungeon.objective_.ievento;

import com.roguesmp.dungeon.objective_.IObjective;
import org.bukkit.event.entity.EntityDeathEvent;

public interface IEntityDeadObjective extends IObjective {
    void onEntityDead(EntityDeathEvent e);
}
