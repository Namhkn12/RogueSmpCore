package com.roguesmp.player.ability;

import com.roguesmp.registry.Registries;
import com.roguesmp.utils.ItemStackUtils;
import org.bukkit.entity.Player;

import java.util.function.Predicate;

/**
 * Configurable options for use in AbilityTrigger. Each constant registers itself into
 * {@link Registries#TRIGGER_OPTION} as it's initialized - call {@link #loadClass()} to force that
 * to happen.
 */
public class TriggerOptions {

    public static final Predicate<Player> SNEAKING = register("sneaking", Player::isSneaking);
    public static final Predicate<Player> NOT_SNEAKING = register("not_sneaking", p -> !p.isSneaking());
    public static final Predicate<Player> SPRINTING = register("sprinting", Player::isSprinting);
    public static final Predicate<Player> NOT_SPRINTING = register("not_sprinting", p -> !p.isSprinting());
    public static final Predicate<Player> NOT_HOLDING_PICKAXE = register("not_holding_pickaxe",
            p -> !ItemStackUtils.isPickaxe(p.getInventory().getItemInMainHand()));
    public static final Predicate<Player> HOLDING_PROJECTILE = register("holding_projectile",
            p -> ItemStackUtils.isShootableItem(p.getInventory().getItemInMainHand()));
    public static final Predicate<Player> NOT_HOLDING_PROJECTILE = register("not_holding_projectile",
            p -> !ItemStackUtils.isShootableItem(p.getInventory().getItemInMainHand()));
    public static final Predicate<Player> NOT_HOLDING_CONSUMABLE = register("not_holding_consumable",
            p -> !ItemStackUtils.isConsumable(p.getInventory().getItemInMainHand()));

    public static void loadClass() {

    }

    private static Predicate<Player> register(String id, Predicate<Player> predicate) {
        Registries.TRIGGER_OPTION.register(id, predicate);
        return predicate;
    }
}
