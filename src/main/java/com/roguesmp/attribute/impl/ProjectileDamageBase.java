package com.roguesmp.attribute.impl;

import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.constant.Attributes;
import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.PlayerProjectile;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ProjectileDamageBase implements SmpAttribute {
    @Override
    public @NotNull String getId() {
        return "projectile_damage_base";
    }

    @Override
    public @NotNull Attributes getEnumConstant() {
        return Attributes.PROJECTILE_DAMAGE_BASE;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Sát thương tầm xa";
    }

    @Override
    public @Nullable List<Component> getDisplayText(double value, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        Component res = Component.text(" " + Utils.formatDecimal(value) + " " + getSimpleName(), NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false);
        return List.of(res);
    }

    @Override
    public void onDamageEntity(DamageEvent event, double value, @NotNull SmpPlayer player) {
        if (event.getDamageType() == DamageType.PROJECTILE && event.getDamager() instanceof Projectile projectile) {
            if (projectile instanceof AbstractArrow arrow && !(arrow instanceof Trident) && !arrow.isCritical()) {
                PlayerProjectile playerProjectile = player.getProjectile(projectile.getUniqueId());
                double speedMult = 1;
                if (playerProjectile != null) {
                    speedMult = playerProjectile.getActiveAttributes().getOrDefault(Attributes.PROJECTILE_SPEED_BASE, 1d);
                }

                value *= Math.max(0, Math.min(1, arrow.getVelocity().length() / 3.0 / speedMult));
            }
            event.addDamageModifier(value, DamageOperation.BASE);
        }
    }
}
