package com.roguesmp.entity.spell.impl;

import com.roguesmp.entity.spell.Spell;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

import java.util.Map;

public class SelfHealSpell extends Spell {

    private final LivingEntity owner;

    public SelfHealSpell(LivingEntity owner) {
        this.owner = owner;
    }

    @Override
    public void run() {
        double maxHealth = owner.getAttribute(Attribute.MAX_HEALTH).getValue();
        double currentHealth = owner.getHealth();
        double missingHealth = maxHealth - currentHealth;

        owner.setHealth(Math.min(currentHealth + missingHealth * 0.2, maxHealth));
    }

    @Override
    public int cooldownTicks() {
        return 60;
    }

    public static SelfHealSpell readParam(Map<String, Object> param, LivingEntity owner) {
        return new SelfHealSpell(owner);
    }
}
