package com.roguesmp.gate.requirement.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.gate.requirement.Requirement;
import com.roguesmp.player.SmpPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.function.Function;

public class CompoundRequirement implements Requirement {

    public static final String ANY_OF_KEY = "any_of";
    public static final String ALL_OF_KEY = "all_of";
    public static final Codec<CompoundRequirement> ANY_OF_CODEC = codec(requirements -> new CompoundRequirement(requirements, true));
    public static final Codec<CompoundRequirement> ALL_OF_CODEC = codec(requirements -> new CompoundRequirement(requirements, false));

    private final List<Requirement> requirements;
    private final boolean any;

    public CompoundRequirement(List<Requirement> requirements, boolean any) {
        this.requirements = List.copyOf(requirements);
        this.any = any;
    }

    private static Codec<CompoundRequirement> codec(Function<List<Requirement>, CompoundRequirement> factory) {
        return Codec.listOf(Requirement.CODEC).fieldOf("requirements")
                .xmap(factory, CompoundRequirement::getRequirements).codec();
    }

    @Override
    public String getTypeId() {
        return any ? ANY_OF_KEY : ALL_OF_KEY;
    }

    public List<Requirement> getRequirements() {
        return requirements;
    }

    @Override
    public boolean test(SmpPlayer player) {
        return any
                ? requirements.stream().anyMatch(requirement -> requirement.test(player))
                : requirements.stream().allMatch(requirement -> requirement.test(player));
    }

    @Override
    public Component getDisplay(SmpPlayer player) {
        Component separator = Component.text(any ? " hoặc " : " và ", NamedTextColor.DARK_GRAY);
        return Component.join(
                JoinConfiguration.separator(separator),
                requirements.stream().map(requirement -> requirement.getDisplay(player)).toList()
        );
    }
}
