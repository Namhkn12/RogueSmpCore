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
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
    public void onProjectileDamageEntity(DamageContext context, double value, SmpPlayer player) {
        context.addDamageModifier(new DamageModifier(getId(), value, DamageType.PHYSICAL, DamageOperation.BASE));
    }
}
