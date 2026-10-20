package com.roguesmp.entity.spell.impl;

import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.impl.VanillaPotionEffect;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.event.DamageEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;


public class BlindSpell extends Spell {

    public static final String TYPE_KEY = "blind_spell";

    public record Params() implements SpellParams {
        public static final Codec<Params> CODEC = MapCodec.unit(Params::new).codec();

        @Override
        public String getTypeId() {
            return TYPE_KEY;
        }
    }

    private final LivingEntity owner;

    public BlindSpell(LivingEntity owner) {
        this.owner = owner;
    }


    @Override
    public void run(int interval) {
    }

    @Override
    public void onDamage(DamageEvent event) {
        Player player = (Player) event.getVictim();
        if (player != null) EffectManager.getInstance().addEffect(player, VanillaPotionEffect.sourceOf(PotionEffectType.BLINDNESS),
                new VanillaPotionEffect(60, PotionEffectType.BLINDNESS, 4));
    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    public static BlindSpell create(Params params, LivingEntity owner) {
        return new BlindSpell(owner);
    }
}
