package com.roguesmp.entity.spell.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.event.DamageEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

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
        if(player != null) player.addPotionEffects(List.of(
                new PotionEffect(PotionEffectType.BLINDNESS, 60, 4)
        ));
    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    public static BlindSpell create(Params params, LivingEntity owner) {
        return new BlindSpell(owner);
    }
}
