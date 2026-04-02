package com.roguesmp.attribute.impl;

import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.constant.Attributes;
import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MeleeDamageBase implements SmpAttribute {
    @Override
    public @NotNull String getId() {
        return "melee_damage_base";
    }

    @Override
    public @NotNull Attributes getEnumConstant() {
        return Attributes.MELEE_DAMAGE_BASE;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Sát thương cận chiến";
    }

    @Override
    public @NotNull List<Component> getDisplayText(double value, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        Component res = Component.text(" " + Utils.formatDecimal(value) + " " + getSimpleName(), NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false);
        return List.of(res);
    }

    @Override
    public void onDamageEntity(DamageEvent event, double value, @NotNull SmpPlayer player) {
        if (event.getDamageType() == DamageType.MELEE) {
            event.addDamageModifier(value * getMeleeCooldownMultiplier(player.getBukkitPlayer()), DamageOperation.BASE);
        }
    }

    private static double getMeleeCooldownMultiplier(Player player) {
        float p = player.getAttackCooldown();
        return 0.2 + 0.8 * p * p;
    }
}
