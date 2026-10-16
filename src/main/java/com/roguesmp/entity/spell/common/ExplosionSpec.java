package com.roguesmp.entity.spell.common;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.EntityUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * A spell-owned explosion whose damage scales off the caster's {@code attack_damage}
 * ({@code attack_damage * damageMultiplier}) instead of vanilla's power-based formula, dealt as
 * {@link DamageType#BLAST} so blast resistances still apply. Never breaks blocks.
 * <p>
 * With {@code falloff}, damage and knockback drop linearly from 100% at the center to 50% at the edge.
 */
public record ExplosionSpec(double radius, double damageMultiplier, boolean falloff, double knockback, boolean playersOnly) {

    private static final double EDGE_FALLOFF = 0.5;

    public static final MapCodec<ExplosionSpec> MAP_CODEC = Codec.composite(
            Codec.DOUBLE.optionalFieldOf("radius", 3.0).forGetter(ExplosionSpec::radius),
            Codec.DOUBLE.optionalFieldOf("damageMultiplier", 1.0).forGetter(ExplosionSpec::damageMultiplier),
            Codec.BOOLEAN.optionalFieldOf("falloff", true).forGetter(ExplosionSpec::falloff),
            Codec.DOUBLE.optionalFieldOf("knockback", 0.8).forGetter(ExplosionSpec::knockback),
            Codec.BOOLEAN.optionalFieldOf("playersOnly", true).forGetter(ExplosionSpec::playersOnly),
            ExplosionSpec::new
    );

    public void detonate(LivingEntity source, Location center, String spellId) {
        playVisuals(center);

        double baseDamage = EntityUtils.getAttributeOrDefault(source, Attribute.ATTACK_DAMAGE, 0) * damageMultiplier;
        center.getNearbyLivingEntities(radius).stream()
                .filter(entity -> !entity.equals(source) && entity.isValid())
                .filter(entity -> !playersOnly || entity instanceof Player)
                .forEach(entity -> hit(source, entity, center, baseDamage, spellId));
    }

    private void hit(LivingEntity source, LivingEntity victim, Location center, double baseDamage, String spellId) {
        Location victimCenter = SpellTargets.centerOf(victim);
        double distance = victimCenter.distance(center);
        if (distance > radius) return;
        double scale = falloff ? 1 - (1 - EDGE_FALLOFF) * (distance / radius) : 1;

        if (baseDamage > 0) {
            DamageEvent.Metadata metadata = DamageEvent.Metadata.spell(DamageType.BLAST, spellId);
            metadata.setIgnoreIframe(true);
            metadata.setDoKnockback(false);
            DamageUtils.damage(victim, source, baseDamage * scale, metadata);
        }

        if (knockback > 0) {
            Vector push = victimCenter.toVector().subtract(center.toVector());
            push = push.lengthSquared() < 1.0E-4 ? new Vector(0, 1, 0) : push.normalize();
            push.setY(Math.max(0.3, push.getY()));
            victim.setVelocity(victim.getVelocity().add(push.multiply(knockback * scale)));
        }
    }

    private void playVisuals(Location center) {
        Particle particle = radius >= 4 ? Particle.EXPLOSION_EMITTER : Particle.EXPLOSION;
        center.getWorld().spawnParticle(particle, center, 1);
        center.getWorld().spawnParticle(Particle.SMOKE, center, 20, radius * 0.3, 0.3, radius * 0.3, 0.05);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1f);
    }
}
