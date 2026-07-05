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
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ThrowRateBase implements SmpAttribute {
    @Override
    public @NotNull String getId() {
        return "throw_rate_base";
    }

    @Override
    public @NotNull Attributes getEnumConstant() {
        return Attributes.THROW_RATE_BASE;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Tốc ném";
    }

    @Override
    public @Nullable List<Component> getDisplayText(double value, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return List.of(Component.text(" "+ Utils.formatDecimal(value) + " " + getSimpleName(), NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
    }

    @Override
    public void onProjectileLaunch(PlayerLaunchProjectileEvent event, double value, @NotNull SmpPlayer player) {
        Projectile projectile = event.getProjectile();
        Player bukkitPlayer = player.getBukkitPlayer();
        event.setShouldConsume(false);
        PlayerProjectile playerProjectile = player.getProjectile(projectile.getUniqueId());
        if (playerProjectile != null) {
            double bonus = playerProjectile.getActiveAttributes().getOrDefault(Attributes.THROW_RATE_PERCENT, 0d);
            value = value * (1 + bonus);
        }

        int cooldown = (int) (20 / value);
        bukkitPlayer.setCooldown(event.getItemStack().getType(), cooldown);

    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Tốc độ ném vũ khí gốc (đinh ba, trứng, v.v...), càng cao thì thời gian chờ càng nhỏ";
    }

    @Override
    public Material getIcon() {
        return Material.SNOWBALL;
    }
}
