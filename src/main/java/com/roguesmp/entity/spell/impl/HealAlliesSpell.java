package com.roguesmp.entity.spell.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.entity.EntityManager;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.entity.spell.common.SpellTargets;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Throws a (cosmetic) splash potion at the most wounded nearby custom mob; on impact it heals every
 * custom mob within {@code splashRadius} directly - vanilla healing potions would hurt undead allies.
 */
public class HealAlliesSpell extends Spell {

    public static final String TYPE_KEY = "heal_allies_spell";

    private static final double POTION_GRAVITY = 0.05;
    private static final double POTION_DRAG = 0.99;
    private static final int POTION_FLIGHT_TICKS = 15;

    public record Params(double searchRadius, double healthThreshold, double healAmount,
                         double splashRadius, int cooldownTicks) implements SpellParams {
        public static final Codec<Params> CODEC = Codec.composite(
                Codec.DOUBLE.optionalFieldOf("searchRadius", 10.0).forGetter(Params::searchRadius),
                Codec.DOUBLE.optionalFieldOf("healthThreshold", 0.6).forGetter(Params::healthThreshold),
                Codec.DOUBLE.optionalFieldOf("healAmount", 8.0).forGetter(Params::healAmount),
                Codec.DOUBLE.optionalFieldOf("splashRadius", 3.0).forGetter(Params::splashRadius),
                Codec.INT.optionalFieldOf("cooldownTicks", 100).forGetter(Params::cooldownTicks),
                Params::new
        );

        @Override
        public String getTypeId() {
            return TYPE_KEY;
        }
    }

    private final LivingEntity owner;
    private final Params params;
    private final Set<UUID> thrownPotions = new HashSet<>();

    public HealAlliesSpell(LivingEntity owner, Params params) {
        this.owner = owner;
        this.params = params;
    }

    @Override
    public boolean canRun() {
        return findWoundedAlly() != null;
    }

    @Override
    public void run(int interval) {
        LivingEntity ally = findWoundedAlly();
        if (ally == null) return;

        ItemStack potionItem = new ItemStack(Material.SPLASH_POTION);
        potionItem.editMeta(PotionMeta.class, meta -> meta.setColor(Color.fromRGB(0xF82423)));

        Location from = owner.getEyeLocation();
        ThrownPotion potion = owner.launchProjectile(ThrownPotion.class,
                SpellTargets.ballisticVelocity(from, ally.getLocation(), POTION_FLIGHT_TICKS, POTION_GRAVITY, POTION_DRAG));
        potion.setItem(potionItem);
        thrownPotions.add(potion.getUniqueId());
    }

    @Override
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!thrownPotions.remove(event.getEntity().getUniqueId())) return;

        Location impact = event.getEntity().getLocation();
        impact.getNearbyLivingEntities(params.splashRadius()).stream()
                .filter(this::isAlly)
                .forEach(this::heal);
    }

    private @Nullable LivingEntity findWoundedAlly() {
        return owner.getLocation().getNearbyLivingEntities(params.searchRadius()).stream()
                .filter(entity -> !entity.equals(owner) && isAlly(entity) && owner.hasLineOfSight(entity))
                .filter(entity -> healthRatio(entity) < params.healthThreshold())
                .min(Comparator.comparingDouble(HealAlliesSpell::healthRatio))
                .orElse(null);
    }

    private boolean isAlly(LivingEntity entity) {
        return !(entity instanceof Player) && entity.isValid() && EntityManager.getInstance().getSmpEntity(entity) != null;
    }

    private void heal(LivingEntity entity) {
        AttributeInstance maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) return;
        entity.setHealth(Math.min(maxHealth.getValue(), entity.getHealth() + params.healAmount()));
        entity.getWorld().spawnParticle(Particle.HEART, SpellTargets.centerOf(entity).add(0, entity.getHeight() * 0.6, 0), 4, 0.4, 0.3, 0.4);
    }

    private static double healthRatio(LivingEntity entity) {
        AttributeInstance maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
        return maxHealth == null ? 1 : entity.getHealth() / maxHealth.getValue();
    }

    @Override
    public int cooldownTicks() {
        return params.cooldownTicks();
    }

    public static HealAlliesSpell create(Params params, LivingEntity owner) {
        return new HealAlliesSpell(owner, params);
    }
}
