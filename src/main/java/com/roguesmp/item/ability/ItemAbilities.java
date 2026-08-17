package com.roguesmp.item.ability;

import com.roguesmp.codec.Codec;
import com.roguesmp.item.ability.impl.*;
import com.roguesmp.registry.Registries;

/**
 * Item ability codecs, keyed by type id - same shape as
 * {@link com.roguesmp.item.component.ItemComponentKeys}/{@code EntityComponentKeys} (a plain
 * {@code Codec<? extends ItemAbility>} per id, no separate params/factory wrapper needed since
 * {@link ItemAbility} decodes straight into a finished, usable instance - see its own javadoc for
 * why that's safe here but not for {@code Spell}). Call {@link #loadClass()} to force the static
 * initializer (and thus every registration below) to run.
 * <p>
 * Add a new passive ability by writing an {@link ItemAbility} implementation with its own
 * {@code CODEC} (see {@link BurnOnHitAbility}/{@link PeriodicSelfHealAbility}) and registering it here.
 */
public class ItemAbilities {

    public static final Codec<BurnOnHitAbility> BURN_ON_HIT =
            register(BurnOnHitAbility.TYPE_KEY, BurnOnHitAbility.CODEC);
    public static final Codec<PeriodicSelfHealAbility> PERIODIC_SELF_HEAL =
            register(PeriodicSelfHealAbility.TYPE_KEY, PeriodicSelfHealAbility.CODEC);

    public static void loadClass() {

    }

    private static <T extends ItemAbility> Codec<T> register(String id, Codec<T> codec) {
        Registries.ITEM_ABILITY_CODEC.register(id, codec);
        return codec;
    }
}
