package com.roguesmp.entity.boss.hellknight;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.registry.entity.EntityRegistry;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

public class SummonMinionSpell extends Spell {
    private final LivingEntity owner;
    private final HellKnight boss;
    private final Location altarLocation;
    private final List<String> minionIds;
    private final int minAmount;
    private final int maxAmount;
    private final double spawnRadius;
    private final Random random = new Random();

    private int currentCooldown = 0;

    public SummonMinionSpell(LivingEntity owner, HellKnight boss, Location altarLocation, List<String> minionIds, int minAmount, int maxAmount, double spawnRadius) {
        this.owner = owner;
        this.boss = boss;
        this.altarLocation = altarLocation;
        this.minionIds = minionIds;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.spawnRadius = spawnRadius;
    }

    @Override
    public void run(int interval) {
        currentCooldown = currentCooldown + interval;
        if (currentCooldown < cooldownTicks()) return;
        currentCooldown = 0;

        if (owner == null || !owner.isValid() || minionIds.isEmpty()) return;

        // Tính toán số lượng spawn
        int amountToSpawn = random.nextInt((maxAmount - minAmount) + 1) + minAmount;

        // Hiệu ứng bắt đầu triệu hồi
        owner.getWorld().playSound(owner.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.5f, 0.5f);
        owner.getWorld().spawnParticle(Particle.SOUL, owner.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);

        for (int i = 0; i < amountToSpawn; i++) {
            Location spawnLoc = getRandomLocationAroundAltar();
            String selectedId = minionIds.get(random.nextInt(minionIds.size()));

            spawnLoc.getWorld().spawnParticle(Particle.FLAME, spawnLoc, 15, 0.1, 0.1, 0.1, 0.05);

            BukkitRunnable task = new BukkitRunnable() {
                @Override
                public void run() {
                    if (owner.isDead() || !owner.isValid()) return;

                    spawnLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, spawnLoc, 10, 0.2, 0.5, 0.2, 0.05);
                    spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_WITHER_SHOOT, 1f, 1.5f);

                    EntityRegistry.getInstance().spawnEntity(selectedId, spawnLoc);
                }
            };
            task.runTaskLater(RogueSmpCore.getInstance(), 15L);
            activeRunnables.add(task);
        }
    }

    private Location getRandomLocationAroundAltar() {
        for (int i = 0; i < 15; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double r = random.nextDouble() * spawnRadius;
            double x = altarLocation.getX() + r * Math.cos(angle);
            double z = altarLocation.getZ() + r * Math.sin(angle);

            Location loc = new Location(altarLocation.getWorld(), x, altarLocation.getY(), z);
            loc.add(0.5, 1, 0.5);

            if (loc.getBlock().getType().isAir()) {
                return loc;
            }
        }
        return altarLocation.clone().add(1, 1, 0);
    }

    @Override
    public int cooldownTicks() {
        return 260;
    }
}
