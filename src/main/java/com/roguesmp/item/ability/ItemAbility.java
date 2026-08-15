package com.roguesmp.item.ability;

import com.roguesmp.codec.Codec;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.item.SmpItem;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.Registries;

public interface ItemAbility {

    Codec<ItemAbility> CODEC = Codec.dispatch(
            ItemAbility::getTypeId,
            id -> Registries.ITEM_ABILITY.getOrThrow(id)
    );

    String getTypeId();

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
