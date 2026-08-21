package com.roguesmp.entity.component.impl;

import com.roguesmp.entity.SmpEntity;
import com.roguesmp.entity.component.EntityComponent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;

public class LootableComponent implements EntityComponent {

    @Override
    public @NotNull EntityComponent copy() {
        return null;
    }

    @Override
    public void onDeath(EntityDeathEvent event, SmpEntity entity) {
        EntityComponent.super.onDeath(event, entity);
    }
}
