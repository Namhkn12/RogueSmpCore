package com.roguesmp.entity.spell.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.entity.spell.common.HitCondition;
import com.roguesmp.entity.spell.common.OnHitSpell;
import com.roguesmp.entity.spell.common.SpellTargets;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.Utils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

/**
 * Overrides the victim's velocity a tick after the hit (after vanilla knockback). A negative
 * {@code horizontal} pulls the victim towards the owner instead of pushing it away.
 */
public class KnockbackOnHitSpell extends OnHitSpell {

    public static final String TYPE_KEY = "knockback_on_hit_spell";

    public record Params(HitCondition condition, double horizontal, double vertical) implements SpellParams {
        public static final Codec<Params> CODEC = Codec.composite(
                HitCondition.MAP_CODEC.forGetter(Params::condition),
                Codec.DOUBLE.optionalFieldOf("horizontal", 1.0).forGetter(Params::horizontal),
                Codec.DOUBLE.optionalFieldOf("vertical", 0.4).forGetter(Params::vertical),
                Params::new
        );

        @Override
        public String getTypeId() {
            return TYPE_KEY;
        }
    }

    private final double horizontal;
    private final double vertical;

    public KnockbackOnHitSpell(LivingEntity owner, Params params) {
        super(owner, params.condition());
        this.horizontal = params.horizontal();
        this.vertical = params.vertical();
    }

    @Override
    protected void onHit(LivingEntity victim, DamageEvent event) {
        Utils.runLater(() -> {
            if (!victim.isValid() || !owner.isValid()) return;
            Vector direction = SpellTargets.horizontalDirection(owner.getLocation(), victim.getLocation());
            victim.setVelocity(direction.multiply(horizontal).setY(vertical));
        });
    }

    public static KnockbackOnHitSpell create(Params params, LivingEntity owner) {
        return new KnockbackOnHitSpell(owner, params);
    }
}
