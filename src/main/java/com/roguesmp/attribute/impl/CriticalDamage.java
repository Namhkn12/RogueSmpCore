package com.roguesmp.attribute.impl;

import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.constant.Attributes;
import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageType;
import com.roguesmp.context.DamageContext;
import com.roguesmp.damage.DamageModifier;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CriticalDamage implements SmpAttribute {
    @Override
    public @NotNull String getId() {
        return "critical";
    }

    @Override
    public @NotNull Attributes getEnumConstant() {
        return Attributes.CRITICAL_DAMAGE;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Chí mạng";
    }

    @Override
    public @NotNull List<Component> getDisplayText(double value, SmpPlayer player, PersistentDataContainerView pdc) {
        return List.of(Utils.fromString("<blue><!i>" + Utils.formatValue(value) + " Sát thương chí mạng"));
    }

    @Override
    public void onAttackEntity(DamageContext context, double value) {
        context.addDamageModifier(new DamageModifier("critical", value, DamageType.PHYSICAL, DamageOperation.ADDITIVE));
    }
}
