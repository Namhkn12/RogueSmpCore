package com.roguesmp.player.ability.impl.warrior;

import com.destroystokyo.paper.ParticleBuilder;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageType;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.effect.impl.KnockbackResistIncreaseEffect;
import com.roguesmp.effect.impl.ResistanceEffect;
import com.roguesmp.effect.impl.StunEffect;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.fx.FxEffect;
import com.roguesmp.fx.FxEngine;
import com.roguesmp.fx.FxPart;
import com.roguesmp.fx.render.ParticleRenderer;
import com.roguesmp.fx.shape.FxShape;
import com.roguesmp.fx.shape.PointShape;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.trigger.AbilityResponse;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.Hitbox;
import com.roguesmp.utils.LocationUtils;
import com.roguesmp.utils.MovementUtils;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GloriousBattle extends Ability {

    private static final String GLORIOUS_KB_RESIST_EFFECT_ID = "glorious_kb_resist";
    private static final String GLORIOUS_DMG_RESIST_EFFECT_ID = "glorious_dmg_resist";

    private static final int SHOCKWAVE_GROWTH_TICKS = 12;


    public static final AbilityInfo<GloriousBattle> INFO = new AbilityInfo<>("glorious_battle", GloriousBattle.class, GloriousBattle::new)
            .registerAction("cast", GloriousBattle::cast);

    private final Player player;

    private final double velocity;
    private final int cooldown;
    private final double collisionRadius;
    private final double collisionDamage;
    private final double collisionKnockback;
    private final double landingDamage;
    private final double landingKnockback;
    private final double landingRadius;
    private final double resistanceAmplifier;
    private final int resistanceDuration;
    private final int stunDuration;

    private final List<LivingEntity> charged = new ArrayList<>();
    private BukkitRunnable runnable;

    public GloriousBattle(SmpPlayer smpPlayer, int level) {
        super(smpPlayer, level);
        player = smpPlayer.getBukkitPlayer();
        cooldown = (int) getBaseAttributeValue("cooldown");
        velocity = getBaseAttributeValue("velocity");
        collisionRadius = getBaseAttributeValue("collision_radius");
        collisionDamage = getBaseAttributeValue("collision_damage");
        collisionKnockback = getBaseAttributeValue("collision_knockback");
        landingDamage = getBaseAttributeValue("landing_damage");
        landingKnockback = getBaseAttributeValue("landing_knockback");
        landingRadius = getBaseAttributeValue("landing_radius");
        resistanceAmplifier = getBaseAttributeValue("resistance_amplifier");
        resistanceDuration = (int) getBaseAttributeValue("resistance_duration");
        stunDuration = (int) getBaseAttributeValue("stun_duration");
    }

    public AbilityResponse cast() {
        if (isOnCooldown() || player.getVehicle() != null) return AbilityResponse.continueChain();
        setCooldownTick(cooldown);
        Vector dir = getLungeVector();
        player.setVelocity(dir);
        EffectManager.getInstance().addEffect(player, GLORIOUS_KB_RESIST_EFFECT_ID, new KnockbackResistIncreaseEffect(200, 1));
        EffectManager.getInstance().addEffect(player, GLORIOUS_DMG_RESIST_EFFECT_ID, new ResistanceEffect(resistanceDuration, resistanceAmplifier, SmpEffect.DeathBehavior.REMOVE_ON_DEATH));

        Location location = player.getLocation();
        World world = player.getWorld();
        int spellDelay = 10;
        gloryStartEffect(world, player, location);

        if (runnable != null) {
            runnable.cancel();
        }

        runnable = new BukkitRunnable() {
            int t = 0;
            boolean landed = false;

            @Override
            public void run() {
                if (!player.isValid()) {
                    this.cancel();
                    return;
                }
                t++;
                if (!landed) {
                    //noinspection deprecation
                    if (player.isOnGround() && t >= spellDelay) {
                        EffectManager.getInstance().clearEffects(player, GLORIOUS_KB_RESIST_EFFECT_ID);
                        landed = true;
                        t = 0;
                    }

                    //pierce
                    List<LivingEntity> mobs = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(player), collisionRadius).getHitMobs();
                    for (LivingEntity mob : mobs) {
                        if (charged.contains(mob)) continue;
                        charged.add(mob);
                        DamageEvent.Metadata metadata = new DamageEvent.Metadata(DamageType.MELEE_ABILITY);
                        metadata.setDoKnockback(false);
                        DamageUtils.damage(mob, player, collisionDamage, metadata);
                        EffectManager.getInstance().addEffect(mob, "glorious_stun_effect", new StunEffect(stunDuration));
                        if (collisionKnockback > 0) {
                            Vector knockbackDirection = getLungeVector().clone();
                            knockbackDirection.setY(0).normalize().multiply(collisionKnockback).setY(0.2);
                            MovementUtils.knockAwayDirection(knockbackDirection, mob, 0);
                        }
                        gloryCollisionEffect(world, player, mob);

                    }
                } else {
                    if (t == 1) {
                        gloryOnLandEffect(world, player);
                        charged.clear();
                        this.cancel();

                        List<LivingEntity> mobs = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(player), landingRadius).getHitMobs();
                        for (LivingEntity mob : mobs) {
                            DamageEvent.Metadata metadata = new DamageEvent.Metadata(DamageType.MELEE_ABILITY);
                            metadata.setDoKnockback(false);
                            metadata.setIgnoreIframe(true);
                            DamageUtils.damage(mob, player, landingDamage, metadata);
                            EffectManager.getInstance().addEffect(mob, "glorious_stun_effect", new StunEffect(stunDuration));
                            if (landingKnockback > 0) {
                                Vector knockbackDirection = getLungeVector().clone();
                                knockbackDirection.setY(0).normalize().multiply(landingKnockback).setY(0.2);
                                MovementUtils.knockAwayDirection(knockbackDirection, mob, 0);
                            }
                            gloryCollisionEffect(world, player, mob);

                        }

                        return;
                    }

                    if (t > 200) {
                        charged.clear();
                        this.cancel();
                    }
                }
            }
        };
        runnable.runTaskTimer(RogueSmpCore.getInstance(), 0, 1);

        return AbilityResponse.consume();
    }

    private void gloryStartEffect(World world, Player player, Location location) {
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_PLAYER_ATTACK_STRONG, Sound.Source.PLAYER, 2.0f, 0.1f), player);
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_PLAYER_ATTACK_STRONG, Sound.Source.PLAYER, 2.0f, 0.1f), player);
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_PLAYER_ATTACK_STRONG, Sound.Source.PLAYER, 2.0f, 0.1f), player);

        FxShape sporeShape = new PointShape();
        FxShape critShape = new PointShape();
        FxPart spore = new FxPart(sporeShape, new ParticleRenderer(new ParticleBuilder(Particle.CRIMSON_SPORE).offset(1, 0, 1).count(25)));
        FxPart crit = new FxPart(critShape, new ParticleRenderer(new ParticleBuilder(Particle.CRIT).count(15).offset(1, 0, 1)));
        FxEngine.getInstance().play(FxEffect.builder(location).part(spore, crit).duration(1).build());
    }

    private void gloryCollisionEffect(World world, Player player, LivingEntity target) {
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_PLAYER_ATTACK_CRIT, Sound.Source.PLAYER, 1f, 0.5f), player);
        FxShape sweepShape = new PointShape();
        FxPart sweep = new FxPart(sweepShape, new ParticleRenderer(new ParticleBuilder(Particle.SWEEP_ATTACK).extra(2)));
        FxEngine.getInstance().play(FxEffect.builder(target.getLocation()).part(sweep).build());
    }

    private void gloryOnLandEffect(World world, Player player) {
        world.playSound(Sound.sound(SoundEventKeys.ITEM_SHIELD_BREAK, Sound.Source.PLAYER, 0.8f, 0.1f), player);
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, Sound.Source.PLAYER, 0.5f, 0.8f), player);
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_PLAYER_ATTACK_CRIT, Sound.Source.PLAYER, 2.0f, 0.5f), player);
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_PLAYER_DEATH, Sound.Source.PLAYER, 0.5f, 0.8f), player);
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_ELDER_GUARDIAN_HURT, Sound.Source.PLAYER, 0.5f, 1.5f), player);
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_ENDER_DRAGON_HURT, Sound.Source.PLAYER, 0.3f, 2f), player);
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_BLAZE_HURT, Sound.Source.PLAYER, 0.3f, 2f), player);
        FxShape sweepShape = new PointShape();
        FxPart sweep = new FxPart(sweepShape, new ParticleRenderer(new ParticleBuilder(Particle.SWEEP_ATTACK).extra(2).count(20).offset(1,0,1)));
        FxEngine.getInstance().play(FxEffect.builder(player.getLocation()).part(sweep).build());
    }

    private Vector getLungeVector() {
        Vector dir = player.getLocation().getDirection();
        dir.multiply(velocity);

        if (Math.signum(dir.getY()) > 0) {
            // +0.22 to allow for horizontal movement
            dir.setY(dir.getY() * 0.4 + 0.22);
        } else {
            dir.setY(dir.getY() * 0.4);
        }

        return dir;
    }

    @Override
    public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() {
        return INFO;
    }
}
