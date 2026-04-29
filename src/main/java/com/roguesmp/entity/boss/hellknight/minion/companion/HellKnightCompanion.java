package com.roguesmp.entity.boss.hellknight.minion.companion;

import com.roguesmp.constant.DamageOperation;
import com.roguesmp.entity.BaseEntity;
import com.roguesmp.entity.boss.hellknight.minion.HellKnightMinion;
import com.roguesmp.entity.spell.SpellManager;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import org.bukkit.entity.LivingEntity;

import java.util.Collections;
import java.util.List;

public class HellKnightCompanion extends HellKnightMinion {

    public static final String ID = "hell_knight_companion";

    public HellKnightCompanion(BaseEntity base, LivingEntity entity) {
        super(base, entity);
    }

    @Override
    public void initialize() {
        if (initialized) return; // Safety check

        // This triggers the logic inside BaseEntity to call startSpell()
        base.processEntity(this.entity);

        this.startSpell(new SpellManager(List.of(new InfernalTremor(entity, 14, 3, 30,40, 1.2, 30))), Collections.emptyList(), 50, null);

        this.initialized = true;
    }

    public void setAi(boolean state) {
        entity.setAI(state);
        entity.setInvulnerable(!state);
    }

    // Transfer damage to main body
    @Override
    public void onHurt(DamageEvent event) {
        super.onHurt(event);
        if (getMainBoss() == null) return;

        if (getMainBoss().getEntity().isInvulnerable()) {
            event.setCancelled(true);
            return;
        }

        // Redirect 100% of the damage to the boss
        DamageUtils.damage(
                getMainBoss().getEntity(),
                event.getDamager(),
                event.getFinalDamage(),
                new DamageEvent.Metadata("redirected_damage", null, event.getDamageType(), event.isCritical())
        );

        event.addDamageModifier(0, DamageOperation.MORE_FINAL);

    }
}
