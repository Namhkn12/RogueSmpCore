package com.roguesmp.attribute.impl;

import com.roguesmp.attribute.Attributes;
import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.DamageUtils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BlockReflect implements SmpAttribute {

    @Override public @NotNull String getId() { return "block_reflect"; }
    @Override public @NotNull Attributes getEnumConstant() { return Attributes.BLOCK_REFLECT; }
    @Override public @NotNull String getSimpleName() { return "Phản sát thương khi đỡ"; }

    @Override
    public @NotNull List<Component> getDisplayText(double value, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultPercentLoreProvider(value);
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Phản lại x% sát thương đã đỡ được về phía kẻ tấn công.";
    }

    @Override
    public Material getIcon() {
        return Material.SHIELD;
    }

    @Override
    public void onHurt(DamageEvent event, double value, @NotNull SmpPlayer player) {
        if (!event.isShieldBlocked() || !(event.getDamager() instanceof LivingEntity attacker)) return;
        if (event.getDamageType() == DamageType.THORNS) return;
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null || attacker.equals(bukkitPlayer)) return;

        double reflected = event.getInitialDamage() * event.getShieldBlockRatio() * value / 100;
        if (reflected <= 0) return;

        DamageUtils.damage(attacker, bukkitPlayer, reflected, DamageEvent.Metadata.attack(DamageType.THORNS).setScalable(false));
    }
}
