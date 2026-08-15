package com.roguesmp.item.ability.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.ability.ItemAbility;
import com.roguesmp.player.SmpPlayer;

/**
 * Sets the victim on fire whenever the wielder deals damage - stateless, no cooldown, tunable
 * only by how long the burn lasts. Matches items like "Torch" ("Đánh kèm cháy 2s").
 */
public class BurnOnHitAbility implements ItemAbility {

    public static final String TYPE_KEY = "burn_on_hit";

    public static final Codec<BurnOnHitAbility> CODEC = Codec.INT.optionalFieldOf("fireTicks", 40)
            .xmap(BurnOnHitAbility::new, BurnOnHitAbility::fireTicks).codec();

    private final int fireTicks;

    public BurnOnHitAbility(int fireTicks) {
        this.fireTicks = fireTicks;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public void onDamageEntity(SmpPlayer player, SmpItem item, DamageEvent event) {
        event.getVictim().setFireTicks(Math.max(event.getVictim().getFireTicks(), fireTicks));
    }

    public int fireTicks() {
        return fireTicks;
    }
}
