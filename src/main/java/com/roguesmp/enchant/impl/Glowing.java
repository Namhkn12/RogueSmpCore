package com.roguesmp.enchant.impl;

import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * While active, keeps the player under a permanent vanilla {@link PotionEffectType#GLOWING} -
 * applied/removed via {@link #onEquipmentChange}, not {@code onDamageEntity}/{@code tick}, so it
 * only touches the potion effect when the enchant's active level actually changes rather than
 * reapplying it constantly.
 */
public class Glowing implements SmpEnchant {

    @Override
    public @NotNull String getId() {
        return "glowing";
    }

    @Override
    public @NotNull Enchants getEnumConstant() {
        return Enchants.GLOWING;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Phát sáng";
    }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return List.of(Component.text(getSimpleName(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
    }

    @Override
    public @NotNull Set<EquipSlot> getActiveSlots() {
        return EnumSet.allOf(EquipSlot.class);
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Người mang sẽ phát sáng khi trang bị";
    }

    @Override
    public Material getIcon() {
        return Material.GLOW_INK_SAC;
    }

    @Override
    public void onEquipmentChange(@NotNull SmpPlayer player, int level) {
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null) return;

        if (level <= 0) {
            bukkitPlayer.removePotionEffect(PotionEffectType.GLOWING);
        } else {
            bukkitPlayer.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, PotionEffect.INFINITE_DURATION, level - 1, true, false, true));
        }
    }
}
