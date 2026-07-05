package com.roguesmp.attribute.impl;

import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.constant.Attributes;
import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ProjectileDamagePercent implements SmpAttribute {
    @Override
    public @NotNull String getId() {
        return "projectile_damage_percent";
    }

    @Override
    public @NotNull Attributes getEnumConstant() {
        return Attributes.PROJECTILE_DAMAGE_PERCENT;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Sát thương tầm xa";
    }

    @Override
    public @Nullable List<Component> getDisplayText(double value, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultPercentLoreProvider(value * 100);
    }

    @Override
    public void onDamageEntity(DamageEvent event, double value, @NotNull SmpPlayer player) {
        if (DamageType.isProjectileDamage(event.getDamageType())) {
            event.addDamageModifier(value, DamageOperation.INCREASE_BASE);
        }

    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Tăng sát thương tầm xa thêm x %";
    }

    @Override
    public Material getIcon() {
        return Material.ARROW;
    }
}
