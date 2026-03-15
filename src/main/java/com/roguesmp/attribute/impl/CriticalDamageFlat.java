package com.roguesmp.attribute.impl;

import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.constant.Attributes;
import com.roguesmp.constant.DamageOperation;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CriticalDamageFlat implements SmpAttribute {
    @Override
    public @NotNull String getId() {
        return "crit_damage_flat";
    }

    @Override
    public @NotNull Attributes getEnumConstant() {
        return Attributes.CRIT_DAMAGE_FLAT;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Sát thương chí mạng";
    }

    @Override
    public @NotNull List<Component> getDisplayText(double value, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultFlatLoreProvider(value);
    }

    @Override
    public void onDamageEntity(DamageEvent event, double value, @NotNull SmpPlayer player) {
        if (event.isCritical()) {
            event.addDamageModifier(value, DamageOperation.ADD_BASE);
        }
    }
}
