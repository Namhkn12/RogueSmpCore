package com.roguesmp.enchant.impl;

import com.roguesmp.constant.Enchants;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.event.ArrowConsumeEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class Retrieval implements SmpEnchant {
    @Override
    public @NotNull String getId() {
        return "retrieval";
    }

    @Override
    public @NotNull Enchants getEnumConstant() {
        return Enchants.RETRIEVAL;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Thu hồi tên";
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Mũi tên bắn ra có tỉ lệ 10% không bị tiêu hao mỗi cấp";
    }

    @Override
    public Material getIcon() {
        return Material.BUNDLE;
    }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultLoreProvider(level);
    }

    @Override
    public @NotNull Set<EquipSlot> getActiveSlots() {
        return EnumSet.allOf(EquipSlot.class);
    }

    @Override
    public void onConsumeArrow(ArrowConsumeEvent event, int level, @NotNull SmpPlayer player) {
        double chance = 0.1 * level;
        if (Utils.RANDOM.nextDouble() < chance) {
            Player player1 = player.getBukkitPlayer();
            player1.playSound(player1.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 0.3f, 1.0f);
            event.setCancelled(true);
        }
    }
}
