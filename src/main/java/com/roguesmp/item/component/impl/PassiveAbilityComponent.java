package com.roguesmp.item.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.item.ability.ItemAbility;
import com.roguesmp.item.component.ItemComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.List;

/**
 * A list of {@link ItemAbility}s an item passively grants while equipped (any slot - weapon
 * on-hit, armor on-hurt/tick, etc.). Each JSON entry decodes straight into a ready-to-use
 * {@link ItemAbility} via {@link ItemAbility#CODEC} (see that interface's javadoc for why no
 * separate params/factory step is needed here). Fully immutable/stateless, so {@link #copy()}
 * safely returns {@code this} rather than rebuilding, same as other stateless components like
 * {@code DisplayNameComponent}.
 */
public class PassiveAbilityComponent implements ItemComponent {

    public static final Codec<PassiveAbilityComponent> CODEC = Codec.listOf(ItemAbility.CODEC)
            .xmap(PassiveAbilityComponent::new, PassiveAbilityComponent::getAbilities);

    private final List<ItemAbility> abilities;

    public PassiveAbilityComponent(List<ItemAbility> abilities) {
        this.abilities = List.copyOf(abilities);
    }

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    public @Unmodifiable List<ItemAbility> getAbilities() {
        return Collections.unmodifiableList(abilities);
    }
}
