package com.roguesmp.entity.spell.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.entity.EntityManager;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.entity.spell.ActiveSpell;
import com.roguesmp.entity.spell.SpellContext;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.entity.spell.common.SpellTargets;
import com.roguesmp.utils.GlowUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SummonSpell extends ActiveSpell {

    public static final String TYPE_KEY = "summon_spell";

    private static final int DEFAULT_RECOVERY = 20;

    public record Params(List<String> entityIds, int count, double spawnRadius, double castRange, int maxAlive,
                         int spawnIntervalTicks, int windupTicks, int cooldownTicks) implements SpellParams {
        public static final Codec<Params> CODEC = Codec.composite(
                Codec.listOf(Codec.STRING).optionalFieldOf("entityIds", List.of()).forGetter(Params::entityIds),
                Codec.INT.optionalFieldOf("count", 3).forGetter(Params::count),
                Codec.DOUBLE.optionalFieldOf("spawnRadius", 2.5).forGetter(Params::spawnRadius),
                Codec.DOUBLE.optionalFieldOf("castRange", 16.0).forGetter(Params::castRange),
                Codec.INT.optionalFieldOf("maxAlive", 6).forGetter(Params::maxAlive),
                Codec.INT.optionalFieldOf("spawnIntervalTicks", 5).forGetter(Params::spawnIntervalTicks),
                Codec.INT.optionalFieldOf("windupTicks", 40).forGetter(Params::windupTicks),
                Codec.INT.optionalFieldOf("cooldownTicks", 400).forGetter(Params::cooldownTicks),
                Params::new
        );
    }

    private final LivingEntity owner;
    private final Params params;
    private final List<LivingEntity> summoned = new ArrayList<>();
    private @Nullable Location targetLocation;
    private int pending;
    private long nextSpawnTick;

    public SummonSpell(LivingEntity owner, Params params) {
        super(params.cooldownTicks(), params.windupTicks(), DEFAULT_RECOVERY);
        this.owner = owner;
        this.params = params;
    }

    @Override
    public boolean canCast(SpellContext context) {
        if (params.entityIds().isEmpty() || pending > 0) return false;
        Player target = context.target();
        if (target == null || target.getWorld() != owner.getWorld()) return false;
        if (target.getLocation().distanceSquared(owner.getLocation()) > params.castRange() * params.castRange()) return false;
        summoned.removeIf(entity -> !entity.isValid());
        return summoned.size() < params.maxAlive();
    }

    @Override
    public void onWindupStart(SpellContext context) {
        Player target = context.target();
        targetLocation = (target != null ? target : owner).getLocation();
        owner.getWorld().playSound(owner.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.5f, 0.9f);
        owner.swingMainHand();
        GlowUtils.glow(owner, ChatColor.DARK_PURPLE);
    }

    @Override
    public void onWindupTick(SpellContext context, int elapsed) {
        if (targetLocation == null) return;
        Location center = targetLocation.clone().add(0, 0.1, 0);
        double angle = elapsed * 0.4;
        double radius = params.spawnRadius();
        for (int i = 0; i < 3; i++) {
            double a = angle + i * (Math.PI * 2 / 3);
            center.getWorld().spawnParticle(Particle.WITCH, center.clone().add(Math.cos(a) * radius, 0, Math.sin(a) * radius), 2, 0.05, 0.05, 0.05, 0);
        }
        center.getWorld().spawnParticle(Particle.SOUL, center, 2, radius * 0.5, 0.05, radius * 0.5, 0.01);
        owner.getWorld().spawnParticle(Particle.ENCHANT, SpellTargets.centerOf(owner), 6, 0.4, 0.6, 0.4, 0.5);
        if (elapsed % 10 == 0) {
            center.getWorld().playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 0.6f + 0.6f * elapsed / Math.max(1, params.windupTicks()));
        }
    }

    @Override
    public void onCast(SpellContext context) {
        if (targetLocation == null) {
            finish();
            return;
        }
        pending = Math.min(params.count(), params.maxAlive() - summoned.size());
        nextSpawnTick = context.tick();
        if (pending <= 0) finish();
        else tick(context, 0);
    }

    @Override
    public void onInterrupted(SpellContext context) {
        finish();
    }

    @Override
    public void tick(SpellContext context, int interval) {
        if (pending <= 0 || targetLocation == null) return;
        if (!owner.isValid()) {
            finish();
            return;
        }
        while (pending > 0 && context.tick() >= nextSpawnTick) {
            spawnOne(context.target());
            pending--;
            nextSpawnTick += Math.max(1, params.spawnIntervalTicks());
        }
        if (pending <= 0) finish();
    }

    private void spawnOne(@Nullable Player target) {
        Location location = randomSpawnLocation();
        List<String> ids = params.entityIds();
        String id = ids.get(ThreadLocalRandom.current().nextInt(ids.size()));
        SmpEntity entity = EntityManager.getInstance().spawnEntity(id, location);
        if (entity == null) return;
        LivingEntity living = entity.getEntity();
        summoned.add(living);
        if (living instanceof Mob mob && target != null) mob.setTarget(target);

        Location center = SpellTargets.centerOf(living);
        location.getWorld().spawnParticle(Particle.LARGE_SMOKE, center, 20, 0.3, 0.5, 0.3, 0.03);
        location.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, location, 15, 0.3, 0.1, 0.3, 0.05);
        location.getWorld().playSound(location, Sound.ENTITY_EVOKER_CAST_SPELL, 1f, 1.2f);
        location.getWorld().playSound(location, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.4f, 1.6f);
    }

    private Location randomSpawnLocation() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble(Math.PI * 2);
        double distance = Math.sqrt(random.nextDouble()) * params.spawnRadius();
        Location location = targetLocation.clone().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
        return location.getBlock().isPassable() ? location : targetLocation.clone();
    }

    @Override
    public void onRemoved() {
        finish();
        summoned.clear();
    }

    private void finish() {
        pending = 0;
        targetLocation = null;
        GlowUtils.unsetGlow(owner);
    }
}
