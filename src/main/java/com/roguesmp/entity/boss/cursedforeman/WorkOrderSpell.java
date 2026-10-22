package com.roguesmp.entity.boss.cursedforeman;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.entity.EntityManager;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.entity.spell.ActiveSpell;
import com.roguesmp.entity.spell.SpellContext;
import com.roguesmp.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WorkOrderSpell extends ActiveSpell {

    private record Worker(String id, EntityType fallback) {
    }

    private static final List<Worker> WORKERS = List.of(
            new Worker("cursed_miner", EntityType.ZOMBIE),
            new Worker("cursed_digger", EntityType.HUSK),
            new Worker("cursed_blaster", EntityType.CREEPER),
            new Worker("cursed_hauler", EntityType.SKELETON)
    );

    private static final int MIN_SUMMONS = 2;
    private static final int MAX_SUMMONS = 3;
    private static final int MAX_ALIVE = 5;
    private static final double SPAWN_RADIUS = 3.0;
    private static final int COOLDOWN_TICKS = 300;
    private static final int WINDUP_TICKS = 10;
    private static final int RECOVERY_TICKS = 15;
    private static final int CLEANUP_INTERVAL = 20;

    private final LivingEntity owner;
    private final List<UUID> summoned = new ArrayList<>();

    public WorkOrderSpell(LivingEntity owner) {
        super(COOLDOWN_TICKS, WINDUP_TICKS, RECOVERY_TICKS);
        this.owner = owner;
    }

    @Override
    public void tick(SpellContext context, int interval) {
        if (context.tick() % CLEANUP_INTERVAL != 0) return;
        summoned.removeIf(uuid -> {
            Entity entity = owner.getServer().getEntity(uuid);
            return entity == null || !entity.isValid();
        });
    }

    @Override
    public boolean canCast(SpellContext context) {
        return summoned.size() < MAX_ALIVE;
    }

    @Override
    public void onWindupStart(SpellContext context) {
        owner.getWorld().playSound(owner.getLocation(), Sound.ENTITY_WITHER_SKELETON_AMBIENT, 1.5f, 0.7f);
    }

    @Override
    public void onCast(SpellContext context) {
        Player target = context.target();
        int count = Math.min(MAX_ALIVE - summoned.size(), MIN_SUMMONS + Utils.RANDOM.nextInt(MAX_SUMMONS - MIN_SUMMONS + 1));

        owner.swingMainHand();
        owner.getWorld().playSound(owner.getLocation(), Sound.BLOCK_BELL_USE, 2f, 0.5f);

        for (int i = 0; i < count; i++) {
            Location location = randomSpawnLocation();
            LivingEntity worker = spawnWorker(WORKERS.get(Utils.RANDOM.nextInt(WORKERS.size())), location);
            if (worker == null) continue;

            summoned.add(worker.getUniqueId());
            if (target != null && worker instanceof Mob mob) mob.setTarget(target);
            location.getWorld().spawnParticle(Particle.SOUL, location.clone().add(0, 0.5, 0), 15, 0.3, 0.5, 0.3, 0.02);
            location.getWorld().spawnParticle(Particle.LARGE_SMOKE, location, 10, 0.3, 0.1, 0.3, 0.01);
        }
    }

    private Location randomSpawnLocation() {
        double angle = Utils.RANDOM.nextDouble() * Math.PI * 2;
        Location location = owner.getLocation().add(Math.cos(angle) * SPAWN_RADIUS, 0, Math.sin(angle) * SPAWN_RADIUS);
        return location.getBlock().isPassable() ? location : owner.getLocation();
    }

    private @Nullable LivingEntity spawnWorker(Worker worker, Location location) {
        SmpEntity smpEntity = EntityManager.getInstance().spawnEntity(worker.id(), location);
        if (smpEntity != null) return smpEntity.getEntity();
        return location.getWorld().spawnEntity(location, worker.fallback()) instanceof LivingEntity living ? living : null;
    }

    @Override
    public void onDeath(EntityDeathEvent event) {
        onRemoved();
    }

    @Override
    public void onRemoved() {
        summoned.forEach(uuid -> {
            Entity entity = owner.getServer().getEntity(uuid);
            if (entity != null && entity.isValid()) entity.getScheduler().run(RogueSmpCore.getInstance(), task -> entity.remove(), null);
        });
        summoned.clear();
    }
}
