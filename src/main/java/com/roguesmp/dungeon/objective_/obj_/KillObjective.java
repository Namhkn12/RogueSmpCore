package com.roguesmp.dungeon.objective_.obj_;

import com.roguesmp.entity.EntityManager;
import com.roguesmp.entity.SmpEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.List;

public abstract class KillObjective extends CounterObjective{
    protected List<String> targetId;

    protected boolean isValidTarget(EntityDeathEvent e) {
        if (targetId == null || targetId.isEmpty()) return true;
        if (!(e.getEntity() instanceof LivingEntity living)) return false;
        SmpEntity smpEntity = EntityManager.getInstance().getSmpEntity(living);
        return smpEntity != null && targetId.contains(smpEntity.getId());
    }
}
