package com.roguesmp.entity.spell.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellParams;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class SelfHealSpell extends Spell {

    public static final String TYPE_KEY = "self_heal_spell";

    public record Params() implements SpellParams {
        public static final Codec<Params> CODEC = MapCodec.unit(Params::new).codec();

        @Override
        public String getTypeId() {
            return TYPE_KEY;
        }
    }

    private final LivingEntity owner;

    public SelfHealSpell(LivingEntity owner) {
        this.owner = owner;
    }

    @Override
    public void run(int interval) {
        double maxHealth = owner.getAttribute(Attribute.MAX_HEALTH).getValue();
        double currentHealth = owner.getHealth();
        double missingHealth = maxHealth - currentHealth;

        owner.setHealth(Math.min(currentHealth + missingHealth * 0.2, maxHealth));
    }

    @Override
    public int cooldownTicks() {
        return 60;
    }

    public static SelfHealSpell create(Params params, LivingEntity owner) {
        return new SelfHealSpell(owner);
    }
}
