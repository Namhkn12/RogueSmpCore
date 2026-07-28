package com.roguesmp.entity.spell.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellParams;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class FireRestanceSpell extends Spell {

    public static final String TYPE_KEY = "fire_resistance_spell";

    public record Params() implements SpellParams {
        public static final Codec<Params> CODEC = MapCodec.unit(Params::new).codec();

        @Override
        public String getTypeId() {
            return TYPE_KEY;
        }
    }

    private final LivingEntity owner;

    public FireRestanceSpell(LivingEntity owner) {
        this.owner = owner;
    }

    @Override
    public void run(int interval) {
        owner.addPotionEffects(List.of(
                new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 500, 1)
        ));
    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    public static FireRestanceSpell create(Params params, LivingEntity owner) {
        return new FireRestanceSpell(owner);
    }
}
