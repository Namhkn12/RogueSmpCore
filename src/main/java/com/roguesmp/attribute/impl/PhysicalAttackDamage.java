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

public class PhysicalAttackDamage implements SmpAttribute {
    @Override
    public @NotNull String getId() {
        return "physical_damage";
    }

    @Override
    public @NotNull Attributes getEnumConstant() {
        return Attributes.PHYSICAL_ATTACK_DAMAGE;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Sát thương vật lý";
    }

    @Override
    public @NotNull List<Component> getDisplayText(double value, SmpPlayer player, PersistentDataContainerView pdc) {
        return Utils.fromStrings("<!i><aqua>" + value + " " +getSimpleName());
    }

    @Override
    public void onAttackEntity(DamageContext context, double value) {
        context.addDamageModifier(new DamageModifier("physical_damage", value, DamageType.PHYSICAL, DamageOperation.ADDITIVE));
    }
}
