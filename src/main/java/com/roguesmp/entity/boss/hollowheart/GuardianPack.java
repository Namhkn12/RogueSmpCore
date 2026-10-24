package com.roguesmp.entity.boss.hollowheart;

import com.roguesmp.entity.spell.common.SpellTargets;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class GuardianPack {

    public static final String GUARDIAN_ID = "crystal_guardian";

    private static final EntityType FALLBACK_TYPE = EntityType.ZOMBIE;
    private static final double MIN_DISTANCE = 4.0;
    private static final double MAX_DISTANCE = 8.0;
    private static final int PULSE_INTERVAL = 20;
    private static final double HEAL_PERCENT_PER_PULSE = 0.005;

    private final LivingEntity crystal;
    private final Arena arena;
    private final List<UUID> guardians = new ArrayList<>();

    public GuardianPack(LivingEntity crystal, Arena arena) {
        this.crystal = crystal;
        this.arena = arena;
    }

    public void summon(int count) {
        for (Location point : arena.ringPoints(count, MIN_DISTANCE, MAX_DISTANCE)) {
            LivingEntity guardian = Absorption.spawn(GUARDIAN_ID, point, FALLBACK_TYPE);
            if (guardian == null) continue;
            guardians.add(guardian.getUniqueId());
            point.getWorld().spawnParticle(Particle.REVERSE_PORTAL, point.clone().add(0, 1, 0), 30, 0.4, 0.8, 0.4, 0.05);
        }
    }

    public List<LivingEntity> alive() {
        List<LivingEntity> alive = Absorption.alive(crystal, guardians);
        guardians.retainAll(alive.stream().map(LivingEntity::getUniqueId).toList());
        return alive;
    }

    public void tick(long now, double healMultiplier) {
        if (now % PULSE_INTERVAL != 0) return;
        List<LivingEntity> alive = alive();
        if (alive.isEmpty()) return;

        List<Player> players = arena.players();
        for (LivingEntity guardian : alive) {
            Absorption.trail(SpellTargets.centerOf(guardian), SpellTargets.centerOf(crystal), Particle.HAPPY_VILLAGER);
            if (guardian instanceof Mob mob && mob.getTarget() == null) mob.setTarget(nearest(players, guardian));
        }
        Absorption.heal(crystal, HEAL_PERCENT_PER_PULSE * alive.size() * healMultiplier);
    }

    private static @Nullable Player nearest(List<Player> players, LivingEntity from) {
        return players.stream()
                .min(Comparator.comparingDouble(player -> player.getLocation().distanceSquared(from.getLocation())))
                .orElse(null);
    }

    public void removeAll() {
        alive().forEach(Absorption::remove);
        guardians.clear();
    }
}
