package com.roguesmp.item.ability;

import com.roguesmp.attribute.Attributes;
import com.roguesmp.codec.Codec;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.event.DurabilityChangedEvent;
import com.roguesmp.item.SmpItem;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.Registries;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface ItemAbility {

    Codec<ItemAbility> CODEC = Codec.dispatch(
            ItemAbility::getTypeId,
            Registries.ITEM_ABILITY_CODEC::getOrThrow
    );

    String getTypeId();

    default @Nullable List<Component> getDisplay(SmpPlayer player, SmpItem smpItem) {
        return null;
    }

    default String getSimpleDescription() {
        return "No description yet";
    }

    default @NotNull Map<Attributes, Double> provideAttribute(SmpPlayer player, SmpItem smpItem) {
        return Collections.emptyMap();
    }

    /**
     * The smpItem's durability is about to change via hitting mobs, mining block, shooting stuff -
     * fired before the change is committed, so {@code event.setChangeAmount(...)} can reduce/
     * increase how much is actually lost, or zero it out to veto the change outright. Currently
     * not called for repairing.
     */
    default void onDurabilityChange(SmpPlayer player, SmpItem item, DurabilityChangedEvent event) {

    }

    /**
     * The player holding/wearing this item just dealt damage.
     */
    default void onDamageEntity(SmpPlayer player, SmpItem item, DamageEvent event) {

    }

    /**
     * The player holding/wearing this item just took damage.
     */
    default void onHurt(SmpPlayer player, SmpItem item, DamageEvent event) {

    }

    default void onTick(SmpPlayer player, SmpItem item, int interval) {

    }
}
