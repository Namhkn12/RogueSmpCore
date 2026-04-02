package com.roguesmp.attribute;

import com.roguesmp.constant.Attributes;
import com.roguesmp.event.ArrowConsumeEvent;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.event.entity.EntityKnockbackEvent;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface SmpAttribute {
    @NotNull String getId();

    @NotNull Attributes getEnumConstant();

    @NotNull String getSimpleName();

    //return null for no display
    @Nullable List<Component> getDisplayText(double value, @Nullable SmpPlayer player, PersistentDataContainerView pdc);

    /**
     * Add attribute on equip, remove existing modifier before adding new one!
     */
    default void addVanillaAttribute(Player player, double value) {
//        Example:
//        player.getAttribute(Attribute.ATTACK_DAMAGE).addTransientModifier(new AttributeModifier(MODIFIER_ID, 10d, AttributeModifier.Operation.ADD_NUMBER));
    }

    /**
     * Remove attribute on unequip, must implement both add and remove method
     */
    default void removeVanillaAttribute(Player player) {
//        Example
//        player.getAttribute(Attribute.ATTACK_DAMAGE).removeModifier(MODIFIER_ID);
    }

    default void tick(@NotNull SmpPlayer player, int periodIncrement, double value) {

    }

    default void onDamageEntity(DamageEvent event, double value, @NotNull SmpPlayer player) {

    }

    default void onKillEntity(EntityDeathEvent event, double value, @NotNull SmpPlayer player) {

    }

    default void onHurt(DamageEvent event, double value, @NotNull SmpPlayer player) {

    }

    default void onHurtFatal(DamageEvent event, double value, @NotNull SmpPlayer player) {

    }

    default void onConsume(PlayerItemConsumeEvent event, double value, @NotNull SmpPlayer player) {

    }

    default void onExpChange(PlayerExpChangeEvent event, double value, @NotNull SmpPlayer player) {

    }

    default void onBlockBreak(BlockBreakEvent event, double value, @NotNull SmpPlayer player) {

    }

    /**
     * Called when player is put on fire
     */
    default void onCombust(EntityCombustEvent event, double value, @NotNull SmpPlayer player) {

    }

    /**
     * Called when player put other entities (not player) on fire (including projectiles...)
     */
    default void onCombustEntity(EntityCombustByEntityEvent event, double value, @NotNull SmpPlayer player) {

    }

    default void onProjectileHit(ProjectileHitEvent event, double value, @NotNull SmpPlayer player) {

    }

    default void onProjectileLaunch(ProjectileLaunchEvent event, double value, @NotNull SmpPlayer player) {

    }

    default void onConsumeArrow(ArrowConsumeEvent event, double value, @NotNull SmpPlayer player) {

    }

    default List<Component> defaultFlatLoreProvider(double value) {
        Component fullDisplay;
        TextColor color;

        String numberPrefix = value > 0 ? "+" : "";
        color = value > 0 ? NamedTextColor.BLUE : NamedTextColor.RED;
        fullDisplay = Component.text(numberPrefix + Utils.formatDecimal(value) + " " + getSimpleName(), color).decoration(TextDecoration.ITALIC, false);

        return List.of(fullDisplay);
    }

    default List<Component> defaultPercentLoreProvider(double value) {
        Component fullDisplay;
        TextColor color;

        String numberPrefix = value > 0 ? "+" : "";
        color = value > 0 ? NamedTextColor.BLUE : NamedTextColor.RED;
        fullDisplay = Component.text(numberPrefix + Utils.formatDecimal(value) + "% " + getSimpleName(), color).decoration(TextDecoration.ITALIC, false);

        return List.of(fullDisplay);
    }

    default List<Component> defaultMultLoreProvider(double value) {
        Component fullDisplay;
        TextColor color;

        String numberPrefix = value > 0 ? "×" : "× ";
        color = value > 0 ? NamedTextColor.BLUE : NamedTextColor.RED;
        fullDisplay = Component.text(numberPrefix + Utils.formatDecimal(value) + " " + getSimpleName(), color).decoration(TextDecoration.ITALIC, false);

        return List.of(fullDisplay);
    }
}
