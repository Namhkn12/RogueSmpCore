package com.roguesmp.player.ability.trigger;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.ability.TriggerOptionRegistry;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class AbilityTrigger {
    public enum Key {
        LEFT_CLICK,
        RIGHT_CLICK,
        SWAP,
        SNEAK,
        JUMP
    }

    public static final Codec<AbilityTrigger> CODEC = Codec.composite(
            Codec.enumOf(Key.class).fieldOf("key").forGetter(AbilityTrigger::getKey),
            Codec.listOf(Codec.STRING).optionalFieldOf("options", new ArrayList<>()).forGetter(AbilityTrigger::getOptions),
            AbilityTrigger::new
    );

    private final Key key;
    private final List<String> options = new ArrayList<>();

    public AbilityTrigger(Key key, List<String> options) {
        this.key = key;
        this.options.addAll(options);
    }

    public AbilityTrigger addOption(String optionKey) {
        this.options.add(optionKey);
        return this;
    }

    /**
     * Logic: Check physical key, then look up the logic for each option key
     */
    public boolean matches(Player player, Key pressedKey) {
        if (this.key != pressedKey) return false;

        for (String optionKey : options) {
            Predicate<Player> condition = TriggerOptionRegistry.get(optionKey);
            if (condition != null && !condition.test(player)) return false;
        }
        return true;
    }

    public Key getKey() {
        return key;
    }

    public List<String> getOptions() {
        return options;
    }
}
