package com.roguesmp.constant;

import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;

public enum AbilityTrigger {
    LEFT_CLICK("<key:key.attack>"),
    SHIFT_LEFT_CLICK("<key:key.sneak> + <key:key.attack>"),
    RIGHT_CLICK("<key:key.use>"),
    SHIFT_RIGHT_CLICK("<key:key.sneak> + <key:key.use>"),
    SWAP("<key:key.swapOffhand>"),
    SHIFT_SWAP("<key:key.sneak> + <key:key.swapOffhand>"),
    SHIFT_PROJECTILE("<key:key.sneak> + Bắn tên, snowball,..."),
    // Triggers that is not above are by default Passive
    PASSIVE("Bị động")
    ;

    private final String keybind;

    AbilityTrigger(String keybind) {
        this.keybind = keybind;
    }

    public Component simpleName() {
        return Utils.fromString(keybind);
    }
}
