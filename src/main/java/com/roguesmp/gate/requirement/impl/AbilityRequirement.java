package com.roguesmp.gate.requirement.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.gate.requirement.Requirement;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.registry.Holder;
import com.roguesmp.registry.Registries;
import com.roguesmp.registry.Registry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class AbilityRequirement implements Requirement {

    public static final String TYPE_KEY = "ability";
    public static final Codec<AbilityRequirement> CODEC = Codec.composite(
            Registry.referenceCodec(() -> Registries.ABILITY).fieldOf("ability").forGetter(AbilityRequirement::getAbility),
            Codec.INT.optionalFieldOf("level", 1).forGetter(AbilityRequirement::getLevel),
            AbilityRequirement::new
    );

    private final Holder<AbilityInfo<? extends Ability>> ability;
    private final int level;

    public AbilityRequirement(Holder<AbilityInfo<? extends Ability>> ability, int level) {
        this.ability = ability;
        this.level = level;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    public Holder<AbilityInfo<? extends Ability>> getAbility() {
        return ability;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public boolean test(SmpPlayer player) {
        return player.getPlayerData().getAbilityLevel(ability.getId()) >= level;
    }

    @Override
    public Component getDisplay(SmpPlayer player) {
        Component name = ability.isBound() ? ability.value().getFormattedDisplayName() : Component.text(ability.getId(), NamedTextColor.RED);
        Component display = Component.text("Mở khóa ", NamedTextColor.GRAY).append(name);
        return level > 1 ? display.append(Component.text(" cấp " + level, NamedTextColor.WHITE)) : display;
    }
}
