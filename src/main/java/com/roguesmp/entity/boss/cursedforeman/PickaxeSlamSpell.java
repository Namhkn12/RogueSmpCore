package com.roguesmp.entity.boss.cursedforeman;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.common.SpellTargets;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.EntityUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PickaxeSlamSpell extends Spell {

    public static final String ID = "pickaxe_slam";

    private static final double TRIGGER_RANGE = 4.5;
    private static final double RADIUS = 5.0;
    private static final double DAMAGE_MULTIPLIER = 1.6;
    private static final int WINDUP_TICKS = 16;
    private static final int COOLDOWN_TICKS = 120;

    private final LivingEntity owner;

    public PickaxeSlamSpell(LivingEntity owner) {
        this.owner = owner;
    }

    @Override
    public boolean canRun() {
        Player target = SpellTargets.playerTarget(owner);
        return target != null && target.getLocation().distanceSquared(owner.getLocation()) <= TRIGGER_RANGE * TRIGGER_RANGE;
    }

    @Override
    public void run(int interval) {
        owner.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, WINDUP_TICKS, 9, false, false));
        owner.getWorld().playSound(owner.getLocation(), Sound.ENTITY_WITHER_SKELETON_AMBIENT, 1.5f, 0.5f);

        BukkitRunnable windup = new BukkitRunnable() {
            int elapsed;

            @Override
            public void run() {
                if (!owner.isValid()) {
                    cancel();
                    activeRunnables.remove(this);
                    return;
                }
                if (elapsed < WINDUP_TICKS) {
                    showTelegraph(elapsed);
                    elapsed += 2;
                    return;
                }
                cancel();
                activeRunnables.remove(this);
                slam();
            }
        };
        activeRunnables.add(windup);
        windup.runTaskTimer(RogueSmpCore.getInstance(), 0, 2);
    }

    private void showTelegraph(int elapsed) {
        Location center = owner.getLocation();
        double radius = RADIUS * elapsed / WINDUP_TICKS;
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2 * i / 24;
            center.getWorld().spawnParticle(Particle.CRIT, center.clone().add(Math.cos(angle) * radius, 0.1, Math.sin(angle) * radius), 1, 0, 0, 0, 0);
        }
        if (owner instanceof Mob mob && mob.getTarget() != null) {
            mob.lookAt(mob.getTarget());
        }
    }

    private void slam() {
        Location center = owner.getLocation();
        owner.swingMainHand();

        BlockData ground = center.clone().subtract(0, 1, 0).getBlock().getBlockData();
        if (ground.getMaterial().isAir()) ground = Material.STONE.createBlockData();
        center.getWorld().spawnParticle(Particle.BLOCK, center, 120, RADIUS * 0.4, 0.2, RADIUS * 0.4, 0.2, ground);
        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 3, 1, 0.1, 1, 0);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.6f);
        center.getWorld().playSound(center, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.5f);

        double damage = EntityUtils.getAttributeOrDefault(owner, Attribute.ATTACK_DAMAGE, 0) * DAMAGE_MULTIPLIER;
        for (Player player : center.getNearbyPlayers(RADIUS)) {
            if (!player.isValid() || player.getLocation().getY() - center.getY() > 1.5) continue;

            DamageEvent.Metadata metadata = DamageEvent.Metadata.spell(DamageType.MELEE, ID);
            metadata.setIgnoreIframe(true);
            metadata.setDoKnockback(false);
            DamageUtils.damage(player, owner, damage, metadata);

            Vector push = SpellTargets.horizontalDirection(center, player.getLocation()).multiply(0.6).setY(0.7);
            player.setVelocity(player.getVelocity().add(push));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
        }
    }

    @Override
    public int castTicks() {
        return WINDUP_TICKS;
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }
}
