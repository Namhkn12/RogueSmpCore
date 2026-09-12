package com.roguesmp.player.ability.impl.assassin;

import com.destroystokyo.paper.ParticleBuilder;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageType;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.effect.impl.SpeedEffect;
import com.roguesmp.effect.impl.StealthEffect;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.fx.FxEffect;
import com.roguesmp.fx.FxEngine;
import com.roguesmp.fx.FxPart;
import com.roguesmp.fx.render.ParticleRenderer;
import com.roguesmp.fx.shape.FxShape;
import com.roguesmp.fx.shape.PointShape;
import com.roguesmp.fx.shape.SphereShape;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.trigger.AbilityResponse;
import com.roguesmp.utils.AbilityUtils;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.Hitbox;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class CloakOfShadows extends Ability {

    public static final AbilityInfo<CloakOfShadows> INFO = new AbilityInfo<>("cloak_of_shadows", CloakOfShadows.class, CloakOfShadows::new)
            .registerAction("cast", CloakOfShadows::cast);

    private static final double VELOCITY = 0.7;
    private static final int EXPIRE_DURATION = 100;

    private final Player player;

    private final int cooldown;
    private final double radius;
    private final double slownessAmplifier;
    private final int stealthDuration;
    private final int slownessDuration;
    private final double damage;

    public CloakOfShadows(SmpPlayer smpPlayer, int level) {
        super(smpPlayer, level);
        player = smpPlayer.getBukkitPlayer();

        cooldown = (int) getBaseAttributeValue("cooldown");
        radius = getBaseAttributeValue("radius");
        slownessAmplifier = getBaseAttributeValue("slowness_amplifier");
        stealthDuration = (int) getBaseAttributeValue("stealth_duration");
        slownessDuration = (int) getBaseAttributeValue("slowness_duration");
        damage = getBaseAttributeValue("damage");
    }

    public AbilityResponse cast() {
        if (isOnCooldown()) return AbilityResponse.continueChain();

        World world = player.getWorld();
        Location loc = player.getEyeLocation();
        Item bomb = AbilityUtils.spawnAbilityItem(world, loc, Material.BLACK_CONCRETE, "Shadow Bomb", false, VELOCITY, true, true);
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_SNOWBALL_THROW, Sound.Source.PLAYER, 1, 0.15f), player);

        setCooldownTick(cooldown);
        EffectManager.getInstance().addEffect(player, "cos_stealth", new StealthEffect(stealthDuration));

        new BukkitRunnable() {

            int expiry = EXPIRE_DURATION;

            @Override
            public void run() {
                expiry--;
                if (!bomb.isValid() || expiry <= 0) {
                    for (LivingEntity entity : new Hitbox.SphereHitbox(bomb.getLocation(), radius).getHitMobs()) {
                        DamageEvent.Metadata metadata = new DamageEvent.Metadata(DamageType.MELEE_ABILITY);
                        metadata.setDoKnockback(false);
                        DamageUtils.damage(entity, player, damage, metadata);
                    }
                    explodeEffect(radius / 5, bomb);
                    cancel();
                    return;
                }

                if (bomb.isOnGround()) {
                    for (LivingEntity entity : new Hitbox.SphereHitbox(bomb.getLocation(), radius).getHitMobs()) {
                        DamageEvent.Metadata metadata = new DamageEvent.Metadata(DamageType.MELEE_ABILITY);
                        metadata.setDoKnockback(false);
                        DamageUtils.damage(entity, player, damage, metadata);
                        EffectManager.getInstance().addEffect(entity, "cos_slowness", new SpeedEffect(slownessDuration, - slownessAmplifier, "cos_slowness"));
                    }
                    explodeEffect(radius / 5, bomb);
                    bomb.remove();
                    cancel();
                }
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 1, 1);

        return AbilityResponse.consume();
    }

    private void explodeEffect(double mult, Entity item) {
        Location loc = item.getLocation();
        loc.getWorld().playSound(Sound.sound(SoundEventKeys.ENTITY_GENERIC_EXPLODE, Sound.Source.PLAYER, 1, 0.15f), item);

        FxShape campfireShape = new PointShape();
        FxShape explosionShape = new PointShape();
        FxPart campfire = new FxPart(campfireShape, new ParticleRenderer(new ParticleBuilder(Particle.CAMPFIRE_COSY_SMOKE).count((int) (30 * mult)).offset(3 * mult, 0, 3 * mult)));
        FxPart explosion = new FxPart(explosionShape, new ParticleRenderer(new ParticleBuilder(Particle.EXPLOSION).count((int) (3 * mult)).offset( 2 * mult, 0, 2 * mult)));
        FxEffect effect = FxEffect.builder(loc).duration(1).part(campfire, explosion).build();
        FxEngine.getInstance().play(effect);
    }

    @Override
    public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() {
        return INFO;
    }
}
