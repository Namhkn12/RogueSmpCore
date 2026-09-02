package com.roguesmp.player.ability.impl.assassin;

import com.destroystokyo.paper.ParticleBuilder;
import com.roguesmp.constant.DamageType;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.impl.EmpoweredStrikeEffect;
import com.roguesmp.effect.impl.SilenceEffect;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.fx.FxEffect;
import com.roguesmp.fx.FxEngine;
import com.roguesmp.fx.FxPart;
import com.roguesmp.fx.FxTransform;
import com.roguesmp.fx.render.ParticleRenderer;
import com.roguesmp.fx.shape.LineShape;
import com.roguesmp.fx.shape.PointShape;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.trigger.AbilityResponse;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.VectorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Throws a fan of {@value #DAGGER_COUNT} daggers, damaging and silencing ({@link SilenceEffect})
 * whatever each one first hits, and granting the caster {@link EmpoweredStrikeEffect} (bonus damage
 * on their next melee hit) per dagger landed. The thrown daggers can always be recalled - either by
 * casting again within the recast window, or automatically once it elapses - for a second,
 * reduced-damage hit on the way back.
 */
public class DaggerThrow extends Ability {
    public static final String ID = "dagger_throw";

    private static final int DAGGER_COUNT = 3;
    private static final double SPREAD_DEGREES = 25;
    private static final int RECAST_INPUT_DELAY = 2; // Ticks - ignore an accidental instant re-press

    // Fx
    private static final Particle.DustOptions TRAIL_COLOR = new Particle.DustOptions(Color.SILVER, 1.0f);
    private static final double TRAIL_SPACING = 0.35;

    public static final AbilityInfo<DaggerThrow> INFO = new AbilityInfo<>(
            ID,
            DaggerThrow.class,
            DaggerThrow::new
    ).registerAction("cast", DaggerThrow::cast);

    private final double damage;
    private final double range;
    private final int cooldown;
    private final double damageBoost;
    private final int damageBoostDuration;
    private final int silenceDuration;
    private final int recastDuration;
    private final double recastMultiplier;

    private boolean canRecast = false;
    private int castTick = 0;
    private final List<Location> daggerEndPoints = new ArrayList<>();

    public DaggerThrow(SmpPlayer smpPlayer, int level) {
        super(smpPlayer, level);
        damage = getAbilityInfo().getAttributeForLevel("damage", level);
        range = getAbilityInfo().getAttributeForLevel("range", level);
        cooldown = (int) getAbilityInfo().getAttributeForLevel("cooldown", level);
        damageBoost = getAbilityInfo().getAttributeForLevel("damage_boost", level);
        damageBoostDuration = (int) getAbilityInfo().getAttributeForLevel("damage_boost_duration", level);
        silenceDuration = (int) getAbilityInfo().getAttributeForLevel("silence_duration", level);
        recastDuration = (int) getAbilityInfo().getAttributeForLevel("recast_duration", level);
        recastMultiplier = getAbilityInfo().getAttributeForLevel("recast_multiplier", level);
    }

    public AbilityResponse cast() {
        Player p = smpPlayer.getBukkitPlayer();
        if (p == null) return AbilityResponse.continueChain();
        World world = p.getWorld();

        if (isOnCooldown()) {
            if (canRecast && Bukkit.getCurrentTick() > castTick + RECAST_INPUT_DELAY) {
                recallDaggers(world);
                return AbilityResponse.consume();
            }
            return AbilityResponse.continueChain();
        }

        setCooldownTick(cooldown);
        castTick = Bukkit.getCurrentTick();

        Location startLoc = p.getEyeLocation();
        Vector dir = startLoc.getDirection();
        playThrowSound(world, startLoc);

        for (int a = -(DAGGER_COUNT / 2); a <= DAGGER_COUNT / 2; a++) {
            Vector daggerDir = VectorUtils.rotateYAxis(dir.clone(), a * SPREAD_DEGREES).normalize();
            Location endLoc = throwDagger(world, startLoc, daggerDir, range, p, 1.0);
            daggerEndPoints.add(endLoc);
        }

        canRecast = true;
        Utils.runLater(() -> {
            if (canRecast) recallDaggers(world);
        }, recastDuration);

        return AbilityResponse.consume();
    }

    /**
     * Raytraces a single dagger from {@code start} along {@code dir} for up to {@code maxDistance},
     * damaging (and debuffing) whatever it first hits. Returns where the dagger ended up, for a
     * possible recall later.
     */
    private Location throwDagger(World world, Location start, Vector dir, double maxDistance, Player attacker, double damageMultiplier) {
        RayTraceResult result = world.rayTrace(start, dir, maxDistance, FluidCollisionMode.NEVER, true, 0.5,
                e -> e instanceof LivingEntity && !(e instanceof Player));

        Location endLoc = (result != null && result.getHitPosition() != null)
                ? result.getHitPosition().toLocation(world)
                : start.clone().add(dir.clone().multiply(maxDistance));

        spawnTracer(start, endLoc);

        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            DamageUtils.damage(target, attacker, damage * damageMultiplier, new DamageEvent.Metadata(ID, DamageType.PROJECTILE_ABILITY));
            EffectManager.getInstance().addEffect(attacker, EmpoweredStrikeEffect.ID, new EmpoweredStrikeEffect(damageBoostDuration, damageBoost));
            EffectManager.getInstance().addEffect(target, SilenceEffect.ID, new SilenceEffect(silenceDuration));
        }

        spawnImpact(endLoc);
        return endLoc;
    }

    /** A one-shot particle streak from {@code start} to {@code end}, oriented along their actual 3D direction. */
    private void spawnTracer(Location start, Location end) {
        if (start.getWorld() != end.getWorld()) return;

        Vector direction = end.toVector().subtract(start.toVector());
        double distance = direction.length();
        if (distance < 0.01) return;
        direction.normalize();

        // FxTransform.at(origin) only bakes in yaw, not pitch - so the line's real 3D direction
        // (which does vary in pitch, matching wherever the player was looking) is carried entirely
        // by this part's own local rotation instead, aligned onto a rotation-neutral (yaw/pitch 0) origin.
        Quaternionf rotation = new Quaternionf().rotationTo(
                new Vector3f(0, 0, 1),
                new Vector3f((float) direction.getX(), (float) direction.getY(), (float) direction.getZ())
        );

        FxPart trail = new FxPart(new LineShape(distance, TRAIL_SPACING),
                new ParticleRenderer(new ParticleBuilder(Particle.DUST).data(TRAIL_COLOR).count(0)))
                .transform(new FxTransform(new Vector3f(), rotation, new Vector3f(1, 1, 1)))
                .once();

        Location origin = new Location(start.getWorld(), start.getX(), start.getY(), start.getZ());
        FxEngine.getInstance().play(FxEffect.builder(origin).duration(1).part(trail).build());
    }

    /** A small impact burst wherever a dagger ends up, hit or not. */
    private void spawnImpact(Location loc) {
        FxPart burst = new FxPart(new PointShape(),
                new ParticleRenderer(new ParticleBuilder(Particle.SWEEP_ATTACK).count(3).offset(0.3, 0.3, 0.3).extra(0.1)))
                .once();

        FxEngine.getInstance().play(FxEffect.builder(loc).duration(1).part(burst).build());
        if (loc.getWorld() != null) {
            loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 0.4f, 2.5f);
        }
    }

    private void playThrowSound(World world, Location loc) {
        world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.9f, 1.5f);
        world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.9f, 1.25f);
        world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.9f, 1.0f);
    }

    private void recallDaggers(World world) {
        canRecast = false;

        Player p = smpPlayer.getBukkitPlayer();
        if (p == null || !p.getWorld().equals(world)) {
            daggerEndPoints.clear();
            return;
        }

        Location playerLoc = p.getEyeLocation();
        playThrowSound(world, playerLoc);
        for (Location endPoint : daggerEndPoints) {
            Vector backVector = playerLoc.toVector().subtract(endPoint.toVector());
            double dist = backVector.length();
            if (dist < 0.01) continue;

            throwDagger(world, endPoint, backVector.normalize(), dist, p, recastMultiplier);
        }

        daggerEndPoints.clear();
    }

    @Override public @NotNull AbilityInfo<?> getAbilityInfo() { return INFO; }
}
