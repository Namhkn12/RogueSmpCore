package com.roguesmp.entity.boss.veinstalker;

import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.impl.VanillaPotionEffect;
import com.roguesmp.entity.spell.ActiveSpell;
import com.roguesmp.entity.spell.SpellContext;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class DarknessSpell extends ActiveSpell {

    private static final double RADIUS = 10.0;
    private static final int DURATION_TICKS = 120;
    private static final int COOLDOWN_TICKS = 300;
    private static final int WINDUP_TICKS = 20;
    private static final int RECOVERY_TICKS = 20;

    private final LivingEntity owner;

    public DarknessSpell(LivingEntity owner) {
        super(COOLDOWN_TICKS, WINDUP_TICKS, RECOVERY_TICKS);
        this.owner = owner;
    }

    @Override
    public void onWindupStart(SpellContext context) {
        owner.getWorld().playSound(owner.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 1.5f, 0.8f);
    }

    @Override
    public void onWindupTick(SpellContext context, int elapsed) {
        owner.getWorld().spawnParticle(Particle.SCULK_SOUL, owner.getLocation().add(0, 0.5, 0), 4, 0.6, 0.3, 0.6, 0.02);
    }

    @Override
    public void onCast(SpellContext context) {
        Location center = owner.getLocation();
        center.getWorld().spawnParticle(Particle.SQUID_INK, center.clone().add(0, 0.5, 0), 60, RADIUS * 0.3, 0.5, RADIUS * 0.3, 0.05);
        center.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.5f, 0.7f);

        for (Player player : center.getNearbyPlayers(RADIUS)) {
            if (!player.isValid()) continue;
            EffectManager.getInstance().addEffect(player, VanillaPotionEffect.sourceOf(PotionEffectType.DARKNESS),
                    new VanillaPotionEffect(DURATION_TICKS, PotionEffectType.DARKNESS, 0));
        }
    }
}
