package com.roguesmp.entity.spell.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.event.DamageEvent;
import org.bukkit.entity.LivingEntity;

public class FireAspectSpell extends Spell {

    public static final String TYPE_KEY = "fire_aspect_spell";

    public record Params() implements SpellParams {
        public static final Codec<Params> CODEC = MapCodec.unit(Params::new).codec();

        @Override
        public String getTypeId() {
            return TYPE_KEY;
        }
    }

    public FireAspectSpell() {
    }

    @Override
    public void run(int interval) {
    }

    @Override
    public void onDamage(DamageEvent event) {
        event.getVictim().setFireTicks(60);
    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    public static FireAspectSpell create(Params params, LivingEntity owner) {
        return new FireAspectSpell();
    }

}
