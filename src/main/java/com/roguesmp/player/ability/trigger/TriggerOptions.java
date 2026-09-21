package com.roguesmp.player.ability.trigger;

import com.roguesmp.registry.Registries;
import org.bukkit.entity.Player;

import java.util.function.Predicate;

/**
 * Configurable options for use in AbilityTrigger. Each constant registers itself into
 * {@link Registries#TRIGGER_OPTION} as it's initialized - call {@link #loadClass()} to force that
 * to happen.
 */
public class TriggerOptions {

    public static final TriggerOption SNEAKING = register("sneaking", "Đang ngồi", Player::isSneaking);
    public static final TriggerOption NOT_SNEAKING = register("not_sneaking", "Không ngồi", p -> !p.isSneaking());
    public static final TriggerOption SPRINTING = register("sprinting", "Đang chạy", Player::isSprinting);
    public static final TriggerOption NOT_SPRINTING = register("not_sprinting", "Không chạy", p -> !p.isSprinting());

    public static void loadClass() {

    }

    private static TriggerOption register(String id, String displayText, Predicate<Player> predicate) {
        TriggerOption option = new TriggerOption(displayText, predicate);
        Registries.TRIGGER_OPTION.register(id, option);
        return option;
    }
}
