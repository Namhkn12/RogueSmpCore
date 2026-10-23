package com.roguesmp.entity.boss.blackveinhorror;

import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellContext;
import com.roguesmp.entity.spell.common.PotionEffectSpec;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CorruptedGroundSpell extends Spell {

    public static final String ID = "corrupted_ground";

    private static final double STEP_DISTANCE = 1.0;
    private static final double HIT_RADIUS = 1.2;
    private static final int LIFETIME_TICKS = 80;
    private static final int MAX_PATCHES = 24;
    private static final int PULSE_INTERVAL = 20;
    private static final double DAMAGE = 3.0;
    private static final List<PotionEffectSpec> EFFECTS = List.of(
            new PotionEffectSpec(PotionEffectType.SLOWNESS, 40, 0),
            new PotionEffectSpec(PotionEffectType.WEAKNESS, 60, 0)
    );

    private record Patch(Location location, long expiresAt) {
    }

    private final LivingEntity owner;
    private final Deque<Patch> patches = new ArrayDeque<>();
    private Location lastStep;

    public CorruptedGroundSpell(LivingEntity owner) {
        this.owner = owner;
    }

    @Override
    public void tick(SpellContext context, int interval) {
        long now = context.tick();
        trackSteps(now);
        while (!patches.isEmpty() && patches.peekFirst().expiresAt() <= now) patches.pollFirst();

        patches.forEach(patch -> patch.location().getWorld().spawnParticle(Particle.LARGE_SMOKE, patch.location(), 1, 0.4, 0.05, 0.4, 0.005));
        if (now % PULSE_INTERVAL == 0) pulse();
    }

    private void trackSteps(long now) {
        Location location = owner.getLocation();
        if (!owner.isOnGround()) return;
        if (lastStep != null && lastStep.getWorld() == location.getWorld() && lastStep.distanceSquared(location) < STEP_DISTANCE * STEP_DISTANCE) return;

        lastStep = location;
        patches.addLast(new Patch(location.clone().add(0, 0.1, 0), now + LIFETIME_TICKS));
        if (patches.size() > MAX_PATCHES) patches.pollFirst();
    }

    private void pulse() {
        Set<Player> hit = new HashSet<>();
        for (Patch patch : patches) {
            for (Player player : patch.location().getNearbyPlayers(HIT_RADIUS, 1.0)) {
                if (!player.isValid() || !hit.add(player)) continue;

                DamageEvent.Metadata metadata = DamageEvent.Metadata.spell(DamageType.MAGIC, ID);
                metadata.setDoKnockback(false);
                DamageUtils.damage(player, owner, DAMAGE, metadata);
                PotionEffectSpec.applyAll(player, EFFECTS);
            }
        }
    }

    @Override
    public void onRemoved() {
        patches.clear();
    }
}
