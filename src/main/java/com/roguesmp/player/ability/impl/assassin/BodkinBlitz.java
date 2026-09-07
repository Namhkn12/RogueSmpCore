package com.roguesmp.player.ability.impl.assassin;

import com.destroystokyo.paper.ParticleBuilder;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageType;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.effect.impl.StealthEffect;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.fx.FxEffect;
import com.roguesmp.fx.FxEngine;
import com.roguesmp.fx.FxPart;
import com.roguesmp.fx.render.ParticleRenderer;
import com.roguesmp.fx.shape.PillarShape;
import com.roguesmp.fx.shape.PointShape;
import com.roguesmp.fx.shape.SphereShape;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.AbilityWithCharge;
import com.roguesmp.player.ability.trigger.AbilityResponse;
import com.roguesmp.utils.BoundingBoxUtils;
import com.roguesmp.utils.DamageUtils;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.sound.Sound;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class BodkinBlitz extends AbilityWithCharge {

    public static AbilityInfo<BodkinBlitz> INFO = new AbilityInfo<>(
            "bodkin_blitz",
            BodkinBlitz.class,
            BodkinBlitz::new
    ).registerAction("cast", BodkinBlitz::cast);

    private static final int TELEPORT_TICK = 4;

    private final Player player;

    private final int cooldown;
    private final int stealthDuration;
    private final double bonusDamage;
    private final int bonusDamageDuration;
    private final double distance;

    private boolean teleporting;
    private boolean interruptedTeleport;
    private boolean hasBonusDmgBuff;
    private int bonusDmgTicksLeft;

    public BodkinBlitz(SmpPlayer smpPlayer, int level) {
        super(smpPlayer, level);
        this.player = smpPlayer.getBukkitPlayer();

        this.cooldown = (int) getBaseAttributeValue("cooldown");
        stealthDuration = (int) getBaseAttributeValue("stealth_duration");
        bonusDamage = getBaseAttributeValue("bonus_damage");
        bonusDamageDuration = (int) getBaseAttributeValue("bonus_damage_duration");
        distance = getBaseAttributeValue("distance");

        setMaxCharges((int) getBaseAttributeValue("charges"), cooldown);
    }

    public AbilityResponse cast() {

        if (isOnCooldown() || teleporting) return AbilityResponse.continueChain();

        setCooldownTick(cooldown);

        teleporting = true;
        interruptedTeleport = false;

        World world = player.getWorld();
        blitzStartSound(world);

        new BukkitRunnable() {

            final BoundingBox playerBox = player.getBoundingBox();
            final Vector direction = player.getLocation().getDirection().normalize();
            final double distancePerTick = distance / TELEPORT_TICK;
            int tick = 0;
            Location lastTpLoc = player.getLocation();

            @Override
            public void run() {
                if (!player.isValid()) {
                    cancel();
                    return;
                }
                BoundingBox travelBox = playerBox.clone();

                boolean isBlocked = BoundingBoxUtils.travelTillObstructed(world, travelBox, distance, direction.clone(), distancePerTick, true,
                        location -> blitzTrailEffect(location));
                Location tpLoc = travelBox.getCenter().toLocation(world).add(0, -travelBox.getHeight() / 2, 0);
                if (isBlocked) {
                    tick = TELEPORT_TICK;
                    if (travelBox.equals(playerBox)) { // started when already obstructed - use previous iteration's end location
                        tpLoc = lastTpLoc;
                    }
                } else {
                    // Shift player box by travel distance for next tick's check
                    // Does not use travelBox as that may have been shifted to evade obstacles
                    playerBox.shift(direction.clone().multiply(distancePerTick));
                    lastTpLoc = tpLoc;
                }

                // Don't allow teleporting outside the world border
                if (!tpLoc.getWorld().getWorldBorder().isInside(tpLoc)) {
                    cancel();
                    return;
                }

                // Attempt to teleport player
                tick++;
                if (tick >= TELEPORT_TICK) {
                    tpLoc.setDirection(player.getLocation().getDirection());
                    if (player.getWorld() == tpLoc.getWorld() && !interruptedTeleport) {
                        player.teleport(tpLoc, PlayerTeleportEvent.TeleportCause.PLUGIN);
                    }

                    teleporting = false;

                    blitzEndEffect(tpLoc);

                    EffectManager.getInstance().addEffect(player, "bodkin_blitz_stealth", new StealthEffect(new SmpEffect.BaseProperties(stealthDuration, SmpEffect.DeathBehavior.REMOVE_ON_DEATH, SmpEffect.DisplayMode.WITH_TIME)));

                    tick = 100;
                    cancel();
                }
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0, 1);

        // A charge lets this cast again before the previous buff was consumed/expired - refresh
        // it back to full duration instead of letting the older cast's countdown race it down.
        hasBonusDmgBuff = true;
        bonusDmgTicksLeft = bonusDamageDuration;

        return AbilityResponse.continueChain();
    }

    @Override
    public void tick(int periodIncrement) {
        if (!hasBonusDmgBuff) return;

        bonusDmgTicksLeft -= periodIncrement;
        if (bonusDmgTicksLeft <= 0) {
            hasBonusDmgBuff = false;
        }
    }

    private void blitzStartSound(World world) {
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_PLAYER_BREATH, Sound.Source.PLAYER, 1f, 2f), player);
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_PLAYER_ATTACK_SWEEP, Sound.Source.PLAYER, 1f, 2f), player);
    }

    private void blitzTrailEffect(Location loc) {
        FxPart part = new FxPart(new SphereShape(1.5, 8), new ParticleRenderer(new ParticleBuilder(Particle.FALLING_DUST).data(Bukkit.createBlockData(Material.GRAY_CONCRETE)).extra(0.1).count(1)));
        FxPart crit = new FxPart(new SphereShape(1.5, 5), new ParticleRenderer(new ParticleBuilder(Particle.CRIT).count(1)));
        FxPart smokeNormal = new FxPart(new SphereShape(1.5, 8), new ParticleRenderer(new ParticleBuilder(Particle.SMOKE).count(1).extra(0.01)));
        FxEffect effect = FxEffect.builder(loc).duration(1).part(part, crit, smokeNormal).build();
        FxEngine.getInstance().play(effect);
    }

    private void blitzEndEffect(Location loc) {
        World world = loc.getWorld();

        world.playSound(Sound.sound(SoundEventKeys.BLOCK_ENDER_CHEST_OPEN, Sound.Source.PLAYER, 1f, 2f));
        world.playSound(Sound.sound(SoundEventKeys.ITEM_TRIDENT_RETURN, Sound.Source.PLAYER, 1f, 0.8f));
        world.playSound(Sound.sound(SoundEventKeys.ITEM_TRIDENT_THROW, Sound.Source.PLAYER, 1f, 0.5f));
        world.playSound(Sound.sound(SoundEventKeys.ITEM_TRIDENT_HIT, Sound.Source.PLAYER, 1f, 1f));
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_PHANTOM_HURT, Sound.Source.PLAYER, 1f, 0.75f));
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_BLAZE_SHOOT, Sound.Source.PLAYER, 1f, 1f));

        Location cloned = loc.clone().add(0, 1, 0);
        FxPart smokeNormal = new FxPart(new SphereShape(1.5, 15), new ParticleRenderer(new ParticleBuilder(Particle.SMOKE).count(1).extra(0.1)));
        FxPart spellWitch = new FxPart(new SphereShape(1.5, 15), new ParticleRenderer(new ParticleBuilder(Particle.WITCH).count(1).extra(0)));
        FxPart smokeMore = new FxPart(new SphereShape(2, 50), new ParticleRenderer(new ParticleBuilder(Particle.SMOKE).count(1).extra(0.05)));
        FxPart crit = new FxPart(new SphereShape(2.5, 25), new ParticleRenderer(new ParticleBuilder(Particle.SMOKE).count(1).extra(0.3)));
        FxEffect effect = FxEffect.builder(cloned).part(smokeMore, spellWitch, smokeNormal, crit).duration(1).build();
        FxEngine.getInstance().play(effect);

    }

    @Override
    public void onDamageEntity(DamageEvent event) {
        if (event.isCancelled() || event.getDamageType() != DamageType.MELEE) return;
        if (!hasBonusDmgBuff) return;

        Entity victim = event.getVictim();
        if (victim instanceof LivingEntity living) {
            DamageEvent.Metadata metadata = new DamageEvent.Metadata(DamageType.MELEE_ABILITY);
            metadata.setDoKnockback(false);
            metadata.setIgnoreIframe(true);
            DamageUtils.damage(living, event.getDamager(), bonusDamage, metadata);
            hasBonusDmgBuff = false;
            hitEffect(living.getLocation());
        }
    }

    private void hitEffect(Location loc) {
        World world = loc.getWorld();

        world.playSound(Sound.sound(SoundEventKeys.ITEM_TRIDENT_THROW, Sound.Source.PLAYER, 0.2f, 0.7f), loc.getX(), loc.getY(), loc.getZ());
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_PLAYER_DEATH, Sound.Source.PLAYER, 1.2f, 1.0f), loc.getX(), loc.getY(), loc.getZ());
        world.playSound(Sound.sound(SoundEventKeys.ITEM_SHIELD_BREAK, Sound.Source.PLAYER, 1.0f, 1.5f), loc.getX(), loc.getY(), loc.getZ());
        world.playSound(Sound.sound(SoundEventKeys.ITEM_SHIELD_BREAK, Sound.Source.PLAYER, 2.0f, 0.1f), loc.getX(), loc.getY(), loc.getZ());
        world.playSound(Sound.sound(SoundEventKeys.ITEM_CROSSBOW_SHOOT, Sound.Source.PLAYER, 1.2f, 1.5f), loc.getX(), loc.getY(), loc.getZ());
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, Sound.Source.PLAYER, 0.9f, 2.0f), loc.getX(), loc.getY(), loc.getZ());
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_PLAYER_ATTACK_SWEEP, Sound.Source.PLAYER, 2.0f, 0.3f), loc.getX(), loc.getY(), loc.getZ());
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_PLAYER_ATTACK_CRIT, Sound.Source.PLAYER, 2.0f, 1.0f), loc.getX(), loc.getY(), loc.getZ());
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_WITHER_SKELETON_HURT, Sound.Source.PLAYER, 0.9f, 0.6f), loc.getX(), loc.getY(), loc.getZ());

        FxPart spellMob = new FxPart(new PointShape(),
                new ParticleRenderer(new ParticleBuilder(Particle.ENTITY_EFFECT).data(Color.RED).count(level * 15).offset(0.25, 0.5, 0.5).extra(0.001)))
                .once();
        FxPart crit = new FxPart(new PointShape(),
                new ParticleRenderer(new ParticleBuilder(Particle.CRIT).count(30).offset(0.25, 0.5, 0.5).extra(0.001)))
                .once();
        FxPart critMagicPillar = new FxPart(new PillarShape(3, 15),
                new ParticleRenderer(new ParticleBuilder(Particle.ENCHANTED_HIT).count(1).extra(0.1)))
                .once();

        FxEngine.getInstance().play(FxEffect.builder(loc).duration(1).part(spellMob, crit, critMagicPillar).build());
    }

    @Override
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.PLUGIN && teleporting) {
            interruptedTeleport = true;
        }
    }

    @Override
    public void onDeath(PlayerDeathEvent event) {
        if (!event.isCancelled() && teleporting) {
            interruptedTeleport = true;
        }
    }

    @Override
    public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() {
        return INFO;
    }
}
