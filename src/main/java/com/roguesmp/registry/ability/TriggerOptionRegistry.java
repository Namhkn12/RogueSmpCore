package com.roguesmp.registry.ability;

import com.roguesmp.utils.ItemStackUtils;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Hold configurable options for use in AbilityTrigger
 */
public class TriggerOptionRegistry {
    private static final Map<String, Predicate<Player>> registry = new HashMap<>();

    static {
        registry.put("sneaking", Player::isSneaking);
        registry.put("not_sneaking", p -> !p.isSneaking());
        registry.put("sprinting", Player::isSprinting);
        registry.put("not_sprinting", p -> !p.isSprinting());
        registry.put("not_holding_pickaxe", p -> !ItemStackUtils.isPickaxe(p.getInventory().getItemInMainHand()));
        registry.put("holding_projectile", p -> ItemStackUtils.isShootableItem(p.getInventory().getItemInMainHand()));
        registry.put("not_holding_projectile", p -> !ItemStackUtils.isShootableItem(p.getInventory().getItemInMainHand()));
        registry.put("not_holding_consumable", p -> !ItemStackUtils.isConsumable(p.getInventory().getItemInMainHand()));
    }

    public static Predicate<Player> get(String key) {
        return registry.get(key);
    }
}
