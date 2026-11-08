package com.roguesmp.gate.requirement.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.gate.requirement.Requirement;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;

public class HasIslandRequirement implements Requirement {

    public static final String TYPE_KEY = "has_island";
    public static final Codec<HasIslandRequirement> CODEC = Codec.unit(HasIslandRequirement::new);

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public boolean test(SmpPlayer player) {
        return player.getPlayerData().getIslandId() != null;
    }

    @Override
    public Component getDisplay(SmpPlayer player) {
        return Utils.fromString("<gray>Sở hữu đảo");
    }
}
