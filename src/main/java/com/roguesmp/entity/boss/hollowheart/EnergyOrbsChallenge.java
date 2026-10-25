package com.roguesmp.entity.boss.hollowheart;

import com.roguesmp.entity.EntityManager;
import com.roguesmp.entity.spell.common.SpellTargets;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EnergyOrbsChallenge implements CrystalChallenge {

    private static final double REACH_DISTANCE = 1.6;
    private static final double HOVER_HEIGHT = 1.2;
    private static final double MIN_START_DISTANCE = 6.0;
    private static final double START_RADIUS_FRACTION = 0.8;

    private final CrystalContext context;
    private final List<UUID> orbs = new ArrayList<>();
    private int spawned;
    private int absorbed;

    public EnergyOrbsChallenge(CrystalContext context) {
        this.context = context;
    }

    @Override
    public void start(long now) {
        double maxDistance = Math.max(MIN_START_DISTANCE + 1, context.arena().radius() * START_RADIUS_FRACTION);
        double minDistance = Math.max(MIN_START_DISTANCE, maxDistance - 3);
        for (Location point : context.arena().ringPoints(context.phase().orbCount(), minDistance, maxDistance)) {
            LivingEntity orb = Absorption.spawn(CrystalMinion.ORB_ID, point.add(0, HOVER_HEIGHT, 0), null);
            if (orb == null) return;
            orbs.add(orb.getUniqueId());
            point.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 15, 0.3, 0.3, 0.3, 0.05);
        }
        spawned = orbs.size();
        context.crystal().getWorld().playSound(context.crystal().getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 2f, 1.5f);
    }

    @Override
    public Outcome tick(long now, int interval) {
        Location target = SpellTargets.centerOf(context.crystal());
        double step = context.phase().orbSpeed() * interval / 20.0;

        for (LivingEntity orb : Absorption.alive(context.crystal(), orbs)) {
            Location location = orb.getLocation();
            Vector toCrystal = target.toVector().subtract(location.toVector());
            double distance = toCrystal.length();
            if (distance <= REACH_DISTANCE) {
                absorb(orb);
                continue;
            }
            Location next = location.add(toCrystal.normalize().multiply(Math.min(step, distance)));
            orb.teleport(next);
            if (EntityManager.getInstance().getSmpEntity(orb) instanceof CrystalMinion minion) minion.body().follow();
            if (now % 4 == 0) orb.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, next, 2, 0.1, 0.1, 0.1, 0);
        }

        if (!Absorption.alive(context.crystal(), orbs).isEmpty()) return Outcome.RUNNING;
        return absorbed > 0 ? Outcome.FAILURE : Outcome.SUCCESS;
    }

    private void absorb(LivingEntity orb) {
        absorbed++;
        orbs.remove(orb.getUniqueId());
        Absorption.trail(SpellTargets.centerOf(orb), SpellTargets.centerOf(context.crystal()), Particle.ELECTRIC_SPARK);
        context.crystal().getWorld().playSound(context.crystal().getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 2f, 0.7f);
        Absorption.remove(orb);
    }

    @Override
    public BarState status(long now) {
        int remaining = Absorption.alive(context.crystal(), orbs).size();
        Component title = Component.text(remaining + "/" + spawned, NamedTextColor.GRAY);
        if (absorbed > 0) title = title.append(Component.text("  ⚡" + absorbed, NamedTextColor.GOLD));
        return BarState.fill(title, remaining, Math.max(1, spawned), BossBar.Color.BLUE);
    }

    @Override
    public int empowerOnFailure() {
        return absorbed;
    }

    @Override
    public void cleanup() {
        Absorption.alive(context.crystal(), orbs).forEach(Absorption::remove);
        orbs.clear();
    }
}
