package com.roguesmp.gate.requirement.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.gate.requirement.Requirement;
import com.roguesmp.player.SmpPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class NotRequirement implements Requirement {

    public static final String TYPE_KEY = "not";
    public static final Codec<NotRequirement> CODEC = Requirement.CODEC.fieldOf("requirement")
            .xmap(NotRequirement::new, NotRequirement::getRequirement).codec();

    private final Requirement requirement;

    public NotRequirement(Requirement requirement) {
        this.requirement = requirement;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    public Requirement getRequirement() {
        return requirement;
    }

    @Override
    public boolean test(SmpPlayer player) {
        return !requirement.test(player);
    }

    @Override
    public Component getDisplay(SmpPlayer player) {
        return Component.text("Không: ", NamedTextColor.GRAY).append(requirement.getDisplay(player));
    }
}
