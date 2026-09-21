package com.roguesmp.player.ability.trigger;

import org.bukkit.entity.Player;

import java.util.function.Predicate;

/**
 * A named extra condition on an {@link AbilityTrigger} (e.g. "sneaking"), plus the text shown to
 * players in ability GUIs to describe that condition.
 */
public record TriggerOption(String displayText, Predicate<Player> condition) {

    public boolean test(Player player) {
        return condition.test(player);
    }
}
