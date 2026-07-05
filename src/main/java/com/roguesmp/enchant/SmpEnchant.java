package com.roguesmp.enchant;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.roguesmp.constant.Attributes;
import com.roguesmp.constant.Enchants;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.event.ArrowConsumeEvent;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SmpEnchant {

    @NotNull String getId();

    @NotNull Enchants getEnumConstant();

    @NotNull String getSimpleName();

    // return null for no display
    @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc);

    @NotNull Set<EquipSlot> getActiveSlots();

    default @NotNull String getSimpleDescription() {
        return "Phù phép này chưa có mô tả, xin hãy thông báo tới admin để được thêm.";
    }

    default Material getIcon() {
        return Material.PAPER;
    }

    /**
     * Called when this enchant is added to an itemStack, for putting default/starter data for use in stacking enchants, etc...
     */
    default void attachData(PersistentDataContainer pdc) {

    }

    default void attachVanillaEnchant(Map<Enchantment, Integer> currentVanillaEnchants, int level) {

    }

    default @NotNull Map<Attributes, Double> provideAttributes(int level) {
        return Collections.emptyMap();
    }

    default void tick(@NotNull SmpPlayer player, int periodIncrement, int level) {

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

    /**
     * Called when player is put on fire
     */
    default void onCombust(EntityCombustEvent event, int level, @NotNull SmpPlayer player) {

    }

    /**
     * Called when player put other entities (not player) on fire (including projectiles)
     */
    default void onCombustEntity(EntityCombustByEntityEvent event, int level, @NotNull SmpPlayer player) {

    }

    default void onProjectileHit(ProjectileHitEvent event, int level, @NotNull SmpPlayer player) {

    }

    default void onProjectileLaunch(PlayerLaunchProjectileEvent event, int level, @NotNull SmpPlayer player) {

    }

    default void onShootArrow(EntityShootBowEvent event, int level, @NotNull SmpPlayer player) {

    }

    default void onConsumeArrow(ArrowConsumeEvent event, int level, @NotNull SmpPlayer player) {

    }

    default List<Component> defaultLoreProvider(int level) {
        return List.of(Component.text(getSimpleName() + " " + Utils.toRoman(level), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
    }

    default List<Component> defaultNegativeLoreProvider(int level) {
        return List.of(Component.text(getSimpleName() + " " + Utils.toRoman(level), NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
    }
}
