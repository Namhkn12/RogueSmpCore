package com.roguesmp.entity.boss.hellknight.minion;

import com.destroystokyo.paper.entity.ai.VanillaGoal;
import com.roguesmp.entity.BaseEntity;
import com.roguesmp.entity.spell.SpellManager;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.List;

public class HellKnightGolem extends HellKnightMinion {

    public HellKnightGolem(BaseEntity base, LivingEntity entity) {
        super(base, entity);
        entity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 1));
    }

    @Override
    public void initialize() {
        if (initialized) return; // Safety check

        // This triggers the logic inside BaseEntity to call startSpell()
        base.processEntity(this.entity);

        if (entity instanceof IronGolem golem) {
            Bukkit.getMobGoals().removeGoal(golem, VanillaGoal.IRON_GOLEM_DEFEND_VILLAGE);
            golem.setAggressive(true);
            golem.setPlayerCreated(false);
            PlayerUtils.playersInRange(golem.getLocation(), 50, true).stream().findAny().ifPresent(golem::setTarget);
        }

        startSpell(new SpellManager(List.of(new LineChargeSpell(this, 15, 1.2, 15, 3, 25, 200))), Collections.emptyList(), 40, null);

        this.initialized = true;
    }

    @Override
    public void onHurt(DamageEvent event) {
        super.onHurt(event);
        if (event.getDamager() instanceof Projectile) {
            event.setCancelled(true);
        }
    }
}
