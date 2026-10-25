package com.roguesmp.entity.spell.impl;

import com.roguesmp.entity.spell.ActiveSpell;
import com.roguesmp.entity.spell.SpellContext;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class SelfHealSpell extends ActiveSpell {

    private final LivingEntity owner;

    public SelfHealSpell(LivingEntity owner) {
        super(60);
        this.owner = owner;
    }

    @Override
    public void onCast(SpellContext context) {
        double maxHealth = owner.getAttribute(Attribute.MAX_HEALTH).getValue();
        double currentHealth = owner.getHealth();
        double missingHealth = maxHealth - currentHealth;

        owner.setHealth(Math.min(currentHealth + missingHealth * 0.2, maxHealth));
    }
}
