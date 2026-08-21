package com.roguesmp.enchant.impl;

import com.roguesmp.constant.DamageType;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class Lifesteal implements SmpEnchant {
    @Override
    public @NotNull String getId() {
        return "lifesteal";
    }

    @Override
    public @NotNull Enchants getEnumConstant() {
        return Enchants.LIFESTEAL;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Hút máu";
    }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultLoreProvider(level);
    }

    @Override
    public @NotNull Set<EquipSlot> getActiveSlots() {
        return Set.of(EquipSlot.MAINHAND);
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Đòn đánh cận chiến full lực sẽ hồi sqrt(cấp) máu";
    }

    @Override
    public Material getIcon() {
        return Material.REDSTONE_ORE;
    }

    @Override
    public void onDamageEntity(DamageEvent event, int level, @NotNull SmpPlayer player) {
        if (event.getDamageType() != DamageType.MELEE) return;
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer.getAttackCooldown() <= 0.96) return; //0.96 to be a bit more lenient
        player.getBukkitPlayer().heal(Math.sqrt(level));
    }
}
