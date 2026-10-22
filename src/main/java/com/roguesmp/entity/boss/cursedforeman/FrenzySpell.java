package com.roguesmp.entity.boss.cursedforeman;

import com.roguesmp.constant.Keys;
import com.roguesmp.entity.spell.Spell;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class FrenzySpell extends Spell {

    private static final double SPEED_BONUS = 0.35;
    private static final double DAMAGE_BONUS = 0.5;
    private static final Particle.DustOptions AURA = new Particle.DustOptions(Color.fromRGB(180, 20, 20), 1.4f);

    private final LivingEntity owner;
    private boolean active;

    public FrenzySpell(LivingEntity owner) {
        this.owner = owner;
    }

    public void activate() {
        if (active) return;
        active = true;

        addModifier(Attribute.MOVEMENT_SPEED, SPEED_BONUS);
        addModifier(Attribute.ATTACK_DAMAGE, DAMAGE_BONUS);

        owner.getWorld().playSound(owner.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 2f, 0.7f);
        owner.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, owner.getEyeLocation(), 12, 0.6, 0.4, 0.6, 0);
    }

    private void addModifier(Attribute attribute, double amount) {
        AttributeInstance instance = owner.getAttribute(attribute);
        if (instance == null) return;
        instance.removeModifier(Keys.FRENZY_MODIFIER);
        instance.addModifier(new AttributeModifier(Keys.FRENZY_MODIFIER, amount, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
    }

    @Override
    public void run(int interval) {
        if (!active) return;
        owner.getWorld().spawnParticle(Particle.DUST, owner.getLocation().add(0, owner.getHeight() * 0.5, 0), 4, 0.35, 0.6, 0.35, 0, AURA);
    }

    @Override
    public boolean persistOnPhaseChange() {
        return true;
    }

    @Override
    public int cooldownTicks() {
        return 0;
    }
}
