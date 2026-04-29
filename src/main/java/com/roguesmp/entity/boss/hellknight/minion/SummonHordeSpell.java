package com.roguesmp.entity.boss.hellknight.minion;

import com.roguesmp.entity.spell.Spell;
import com.roguesmp.registry.entity.EntityRegistry;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;

public class SummonHordeSpell extends Spell {

    private final LivingEntity owner;
    private final int amount;
    private final String hordeId;

    public SummonHordeSpell(LivingEntity owner, int amount, String hordeId) {
        this.owner = owner;
        this.amount = amount;
        this.hordeId = hordeId;
    }

    @Override
    public void run(int interval) {

    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    @Override
    public void onDeath(EntityDeathEvent event) {
        Location location = owner.getLocation();
        for (int i = 0; i < amount; i++) {
            EntityRegistry.getInstance().spawnEntity(hordeId, location);
        }
    }
}
