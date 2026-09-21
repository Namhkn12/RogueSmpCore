package com.roguesmp.player.ability.trigger;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class AbilityTrigger {
    public enum Key {
        LEFT_CLICK("<key:key.attack>"),
        RIGHT_CLICK("<key:key.use>"),
        SWAP("<key:key.swapOffhand>"),
        SNEAK("<key:key.sneak>"),
        JUMP("<key:key.jump>");

        private final String displayName;

        Key(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public static final Codec<AbilityTrigger> CODEC = Codec.composite(
            Codec.enumOf(Key.class).fieldOf("key").forGetter(AbilityTrigger::getKey),
            Codec.listOf(Codec.STRING).optionalFieldOf("options", new ArrayList<>()).forGetter(AbilityTrigger::getOptions),
            Codec.STRING.optionalFieldOf("displayName", "").forGetter(AbilityTrigger::getDisplayName),
            AbilityTrigger::new
    );

    private final Key key;
    private final List<String> options = new ArrayList<>();
    private final String displayName;

    public AbilityTrigger(Key key, List<String> options, String displayName) {
        this.key = key;
        this.displayName = displayName;
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
            TriggerOption option = Registries.TRIGGER_OPTION.get(optionKey);
            if (option != null && !option.test(player)) return false;
        }
        return true;
    }

    /**
     * The activation line shown in ability GUIs: the key, then this trigger's conditions
     * ({@link TriggerOption#displayText()}), with {@link #getDisplayName()} as an optional label
     * when an ability has several triggers. Unknown option ids are skipped, same as in {@link #matches}.
     */
    public Component getFormattedActivation() {
        StringBuilder text = new StringBuilder("<gold>Kích hoạt");
        if (!displayName.isEmpty()) text.append(" <gray>(").append(displayName).append("</gray>)");
        text.append(": <white>").append(key.getDisplayName()).append("</white>");

        List<String> conditions = new ArrayList<>();
        for (String optionKey : options) {
            TriggerOption option = Registries.TRIGGER_OPTION.get(optionKey);
            if (option != null && !option.displayText().isEmpty()) conditions.add(option.displayText());
        }
        if (!conditions.isEmpty()) text.append(" <gray>(").append(String.join(", ", conditions)).append(")</gray>");

        return Utils.fromString(text.toString())
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public Key getKey() {
        return key;
    }

    public List<String> getOptions() {
        return options;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Component getFormattedDisplayName() {
        return Utils.fromString(displayName);
    }
}
