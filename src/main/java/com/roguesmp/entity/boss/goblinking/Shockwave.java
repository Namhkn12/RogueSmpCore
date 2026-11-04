package com.roguesmp.entity.boss.goblinking;

import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.spell.common.SpellTargets;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class Shockwave implements Hazard {

    private static final double THICKNESS = 1.0;
    private static final double SPEED = 0.45;

    private final KingContext ctx;
    private final Location center;
    private final double maxRadius;
    private final double multiplier;
    private final Particle.DustOptions dust;
    private final String id;
    private final Set<UUID> hit = new HashSet<>();
    private double radius = 0.5;
    private int delay;

    public Shockwave(KingContext ctx, Location center, double maxRadius, double multiplier, Particle.DustOptions dust, String id) {
        this(ctx, center, maxRadius, multiplier, dust, id, 0);
    }

    public Shockwave(KingContext ctx, Location center, double maxRadius, double multiplier, Particle.DustOptions dust, String id, int delay) {
        this.ctx = ctx;
        this.center = center.clone();
        this.maxRadius = maxRadius;
        this.multiplier = multiplier;
        this.dust = dust;
        this.id = id;
        this.delay = delay;
    }

    @Override
    public boolean tick(int interval) {
        if (delay > 0) {
            delay -= interval;
            return false;
        }
        radius += SPEED * interval;
        Fx.ring(center, radius, dust);
        Fx.ring(center, radius, Particle.CRIT);

        for (Player player : ctx.players()) {
            if (hit.contains(player.getUniqueId())) continue;
            double distance = SpellTargets.horizontalDistance(player.getLocation(), center);
            if (distance > radius || distance < radius - THICKNESS * interval) continue;
            if (Math.abs(player.getY() - center.getY()) > 2 || !ctx.grounded(player)) continue;

            hit.add(player.getUniqueId());
            ctx.damage(player, multiplier, DamageType.BLAST, id);
            ctx.push(player, center, 0.7, 0.45);
        }
        return radius >= maxRadius;
    }
}
