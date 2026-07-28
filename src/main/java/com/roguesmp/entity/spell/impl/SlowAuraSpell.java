package com.roguesmp.entity.spell.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.impl.SpeedEffect;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellParams;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collection;

public class SlowAuraSpell extends Spell {

    public static final String TYPE_KEY = "slow_aura_spell";

    public record Params() implements SpellParams {
        public static final Codec<Params> CODEC = MapCodec.unit(Params::new).codec();

        @Override
        public String getTypeId() {
            return TYPE_KEY;
        }
    }

    private final LivingEntity owner;

    public SlowAuraSpell(LivingEntity owner) {
        this.owner = owner;
    }

    @Override
    public void run(int interval) {
        Collection<Player> players = owner.getLocation().getNearbyPlayers(5);
        players.forEach(player -> {
            EffectManager.getInstance().addEffect(player, "slow_aura_spell", new SpeedEffect(100, -0.3, "slow_aura_spell"));
        });

    }

    @Override
    public int cooldownTicks() {
        return 5;
    }

    public static SlowAuraSpell create(Params params, LivingEntity owner) {
        return new SlowAuraSpell(owner);
    }
}
