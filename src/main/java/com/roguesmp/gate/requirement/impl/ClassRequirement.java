package com.roguesmp.gate.requirement.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.gate.requirement.Requirement;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.classes.PlayerClass;
import com.roguesmp.registry.Holder;
import com.roguesmp.registry.Registries;
import com.roguesmp.registry.Registry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ClassRequirement implements Requirement {

    public static final String TYPE_KEY = "class";
    public static final Codec<ClassRequirement> CODEC = Registry.referenceCodec(() -> Registries.PLAYER_CLASS).fieldOf("class")
            .xmap(ClassRequirement::new, ClassRequirement::getPlayerClass).codec();

    private final Holder<PlayerClass> playerClass;

    public ClassRequirement(Holder<PlayerClass> playerClass) {
        this.playerClass = playerClass;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    public Holder<PlayerClass> getPlayerClass() {
        return playerClass;
    }

    @Override
    public boolean test(SmpPlayer player) {
        return playerClass.getId().equals(player.getPlayerData().getClassId());
    }

    @Override
    public Component getDisplay(SmpPlayer player) {
        Component name = playerClass.isBound() ? playerClass.value().getFormattedDisplayName() : Component.text(playerClass.getId(), NamedTextColor.RED);
        return Component.text("Lớp nhân vật ", NamedTextColor.GRAY).append(name);
    }
}
