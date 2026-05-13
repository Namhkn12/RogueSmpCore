package com.roguesmp.attribute.impl;

import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.constant.Attributes;
import com.roguesmp.player.PlayerProjectile;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ProjectileSpeedPercent implements SmpAttribute {

    @Override
    public @NotNull String getId() {
        return "projectile_speed_percent";
    }

    @Override
    public @NotNull Attributes getEnumConstant() {
        return Attributes.PROJECTILE_SPEED_PERCENT;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Vận tốc đạn";
    }

    @Override
    public @Nullable List<Component> getDisplayText(double value, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultPercentLoreProvider(value * 100);
    }

    @Override
    public void onProjectileLaunch(ProjectileLaunchEvent event, double value, @NotNull SmpPlayer player) {
        Entity entity = event.getEntity();
        PlayerProjectile playerProjectile = player.getProjectile(entity.getUniqueId());
        if (playerProjectile != null) {
            // If contains base speed stat, ignore, it is handled in Speed base attribute
            if (playerProjectile.getActiveAttributes().containsKey(Attributes.PROJECTILE_SPEED_BASE)) return;

            entity.setVelocity(entity.getVelocity().multiply(1 + value));
        }

    }
}
