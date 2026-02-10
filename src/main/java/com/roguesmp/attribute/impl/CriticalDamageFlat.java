package com.roguesmp.attribute.impl;

import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.constant.Attributes;
import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageType;
import com.roguesmp.context.DamageContext;
import com.roguesmp.damage.DamageModifier;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

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
    public @NotNull List<Component> getDisplayText(double value, SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultFlatLoreProvider(value);
    }

    @Override
    public void onDamageEntity(DamageContext context, double value, SmpPlayer player) {
        if (context.isCritical()) {
            context.addDamageModifier(new DamageModifier(getId(), value, DamageType.PHYSICAL, DamageOperation.ADDITIVE));
        }
    }
}
