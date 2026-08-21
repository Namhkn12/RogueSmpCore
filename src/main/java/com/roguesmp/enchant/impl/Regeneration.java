package com.roguesmp.enchant.impl;

import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.EntityUtils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class Regeneration implements SmpEnchant {

    @Override
    public @NotNull String getId() {
        return "regeneration";
    }

    @Override
    public @NotNull Enchants getEnumConstant() {
        return Enchants.REGENERATION;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Hồi phục";
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
    public @NotNull String getSimpleDescription() {
        return "Mỗi 3 giây hồi sqrt(cấp) máu, chia ra làm 4 khoảng";
    }

    @Override
    public Material getIcon() {
        return SmpEnchant.super.getIcon();
    }

    @Override
    public void tick(@NotNull SmpPlayer player, int periodIncrement, int level) {
        Player bukkitPlayer = player.getBukkitPlayer();

        Integer currentTick = (Integer) EntityUtils.getMetadataValue(bukkitPlayer, "regeneration_tracking");
        if (currentTick == null) {
            currentTick = 0;
        }

        currentTick += periodIncrement;
        int intervalTick = (3 * 20) / 4; // 15 ticks per burst

        if (currentTick >= intervalTick) {
            // Calculate how many 15-tick intervals have passed in case of large delta
            int bursts = currentTick / intervalTick;
            currentTick %= intervalTick; // Retain remaining delta remainder

            double healPerBurst = Math.sqrt(level) / 4.0;
            double totalHeal = healPerBurst * bursts;

            bukkitPlayer.heal(totalHeal);
        }

        EntityUtils.addMetadata(bukkitPlayer, "regeneration_tracking", currentTick);
    }
}
