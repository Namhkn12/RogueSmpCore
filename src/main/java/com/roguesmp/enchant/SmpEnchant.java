package com.roguesmp.enchant;

import com.roguesmp.constant.Enchants;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public interface SmpEnchant {

    @NotNull String getId();

    @NotNull Enchants getEnumConstant();

    @NotNull String getSimpleName();

    // return null for no display
    @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc);

    @NotNull Set<EquipSlot> getActiveSlots();

    /**
     * Called the first time this enchant is added to an item, for putting default/starter data for use in stacking enchants, etc...
     */
    default void attachDefaultData(PersistentDataContainer pdc) {

    }

    default void tick(@NotNull SmpPlayer player, int level, boolean twoHz, boolean oneHz) {

    }

    default void onDamageEntity(DamageEvent event, int level, @NotNull SmpPlayer player) {

    }

    default void onKillEntity(EntityDeathEvent event, int level, @NotNull SmpPlayer player) {

    }

    default void onHurt(DamageEvent event, int level, @NotNull SmpPlayer player) {

    }

    default void onHurtFatal(DamageEvent event, int level, @NotNull SmpPlayer player) {

    }

    default void onConsume(PlayerItemConsumeEvent event, int level, @NotNull SmpPlayer player) {

    }

    default void onExpChange(PlayerExpChangeEvent event, int level, @NotNull SmpPlayer player) {

    }

    default void onBlockBreak(BlockBreakEvent event, int level, @NotNull SmpPlayer player) {

    }

    default void onProjectileHit(ProjectileHitEvent event, int level, @NotNull SmpPlayer player) {

    }

    default void onProjectileLaunch(ProjectileLaunchEvent event, int level, @NotNull SmpPlayer player) {

    }

    default List<Component> defaultLoreProvider(int level) {
        return List.of(Component.text(getSimpleName() + " " + Utils.toRoman(level), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
    }
}
