package com.roguesmp.entity.boss.hellknight.minion;

import com.roguesmp.entity.BaseEntity;
import com.roguesmp.entity.EntityManager;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.entity.boss.hellknight.HellKnight;
import com.roguesmp.event.DamageEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class HellKnightMinion extends SmpEntity {

    private HellKnight mainBoss;

    public HellKnightMinion(BaseEntity base, LivingEntity entity) {
        super(base, entity);
        entity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 1));
    }

    public @Nullable HellKnight getMainBoss() {
        return mainBoss;
    }

    public void setMainBoss(HellKnight mainBoss) {
        this.mainBoss = mainBoss;
    }

    // To make sure minions can't hurt each other
    @Override
    public void onHurt(DamageEvent event) {
        super.onHurt(event);
        if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof LivingEntity living) {
                if (EntityManager.getInstance().getSmpEntity(living) instanceof HellKnightMinion) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        if (event.getDamager() instanceof LivingEntity living) {
            if (EntityManager.getInstance().getSmpEntity(living) instanceof HellKnightMinion) {
                event.setCancelled(true);
            }
        }

    }

    @Override
    public void onDamage(DamageEvent event) {
        super.onDamage(event);
        if (event.getVictim() instanceof LivingEntity living) {
            if (EntityManager.getInstance().getSmpEntity(living) instanceof HellKnight) {
                event.setCancelled(true);
            }
        }
    }

    @Override
    public void onTargetEntity(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() != null && !(event.getTarget() instanceof Player)) {
            event.setCancelled(true);
        }
    }
}
