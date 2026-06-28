package com.roguesmp.attribute.impl;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.constant.Attributes;
import com.roguesmp.player.PlayerProjectile;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ProjectileSpeedBase implements SmpAttribute {

    @Override
    public @NotNull String getId() {
        return "projectile_speed_base";
    }

    @Override
    public @NotNull Attributes getEnumConstant() {
        return Attributes.PROJECTILE_SPEED_BASE;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Vận tốc đạn";
    }

    @Override
    public @Nullable List<Component> getDisplayText(double value, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        Component res = Component.text(" " + Utils.formatDecimal(value) + " " + getSimpleName(), NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false);
        return List.of(res);
    }

    @Override
    public void onProjectileLaunch(PlayerLaunchProjectileEvent event, double value, @NotNull SmpPlayer player) {
        Entity entity = event.getProjectile();
        handleVelocity(value, player, entity);

    }

    @Override
    public void onShootArrow(EntityShootBowEvent event, double value, @NotNull SmpPlayer player) {
        Entity entity = event.getProjectile();
        handleVelocity(value, player, entity);
    }

    private static void handleVelocity(double value, @NotNull SmpPlayer player, Entity entity) {
        PlayerProjectile playerProjectile = player.getProjectile(entity.getUniqueId());
        if (playerProjectile != null) {
            double mult = playerProjectile.getActiveAttributes().getOrDefault(Attributes.PROJECTILE_SPEED_PERCENT, 0d);
            entity.setVelocity(entity.getVelocity().multiply(value * (1 + mult)));
        }
    }
}
