package com.roguesmp.player.ability.impl.assassin;

import com.destroystokyo.paper.ParticleBuilder;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.impl.DamageIncreaseEffect;
import com.roguesmp.fx.FxEffect;
import com.roguesmp.fx.FxEngine;
import com.roguesmp.fx.FxPart;
import com.roguesmp.fx.render.ParticleRenderer;
import com.roguesmp.fx.shape.PointShape;
import com.roguesmp.fx.shape.SphereShape;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.trigger.AbilityResponse;
import com.roguesmp.utils.EntityUtils;
import com.roguesmp.utils.LocationUtils;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class AdvancingShadow extends Ability {

    public static final AbilityInfo<AdvancingShadow> INFO = new AbilityInfo<>("advancing_shadow", AdvancingShadow.class, AdvancingShadow::new)
            .registerAction("cast", AdvancingShadow::cast);

    private static final double ADVANCING_SHADOWS_OFFSET = 2.7;
    private static final String PERCENT_DAMAGE_DEALT_EFFECT_NAME = "ASPercentDamageDealtEffect";

    private final Player player;

    private final double range;
    private final int cooldown;
    private final int duration;
    private final double dmgBonus;

    public AdvancingShadow(SmpPlayer smpPlayer, int level) {
        super(smpPlayer, level);
        player = smpPlayer.getBukkitPlayer();
        range = getBaseAttributeValue("range");
        cooldown = (int) getBaseAttributeValue("cooldown");
        duration = (int) getBaseAttributeValue("duration");
        dmgBonus = getBaseAttributeValue("dmgBonus");
    }

    public AbilityResponse cast() {
        if (isOnCooldown()) return AbilityResponse.continueChain();

        LivingEntity entity = EntityUtils.getLivingEntityAtCursor(player, range, entity1 -> entity1.getType() != player.getType(), 0.425);
        if (entity == null) return AbilityResponse.continueChain();

        double originDistance = player.getLocation().distance(entity.getLocation());
        if (originDistance > range) return AbilityResponse.continueChain();

        Vector dir = LocationUtils.getDirectionTo(entity.getLocation(), player.getLocation());
        World world = player.getWorld();
        Location loc = player.getLocation();

        while (loc.distance(entity.getLocation()) > ADVANCING_SHADOWS_OFFSET) {
            loc.add(dir.clone().multiply(0.3333));
            tpTrail(loc);
            if (loc.distance(entity.getLocation()) < ADVANCING_SHADOWS_OFFSET) {
                double multiplier = ADVANCING_SHADOWS_OFFSET - loc.distance(entity.getLocation());
                loc.subtract(dir.clone().multiply(multiplier));
                break;
            }
        }
        loc.add(0, 1, 0);

        // Just in case the player's teleportation loc is in a block.
        int count = 0;
        while (count < 5 && (!loc.isChunkLoaded() || loc.getBlock().getType().isSolid())) {
            count++;
            loc.subtract(dir.clone().multiply(1.15));
        }

        // If still solid, something is wrong.
        if (!loc.isChunkLoaded() || loc.getBlock().getType().isSolid()) {
            tpSoundFail(world);
            return AbilityResponse.continueChain();
        }

        // Prevent the player from teleporting over void
        if (loc.getY() < 8) {
            boolean safe = false;
            for (int y = 0; y < loc.getY() - 1; y++) {
                Location tempLoc = loc.clone();
                tempLoc.setY(y);
                if (!tempLoc.isChunkLoaded()) {
                    continue;
                }
                if (!tempLoc.getBlock().isPassable()) {
                    safe = true;
                    break;
                }
            }

            // Maybe void - not worth it
            if (!safe) {
                tpSoundFail(world);
                return AbilityResponse.continueChain();
            }

            // Don't teleport players below y = 1.1 to avoid clipping into oblivion
            loc.setY(Math.max(1.1, loc.getY()));
        }

        // Extra safeguard to prevent bizarro teleports
        if (player.getLocation().distance(loc) > range) {
            tpSoundFail(world);
            return AbilityResponse.continueChain();
        }

        if (loc.distanceSquared(entity.getLocation()) <= originDistance * originDistance) {
            player.teleport(loc);
        }

        EffectManager.getInstance().addEffect(player, PERCENT_DAMAGE_DEALT_EFFECT_NAME, new DamageIncreaseEffect(duration, dmgBonus));
        tpParticle();
        tpSound(world);
        setCooldownTick(cooldown);

        return AbilityResponse.consume();
    }

    private void tpParticle() {
        Location loc = player.getLocation().clone().add(0, 1, 0);

        FxPart smoke = new FxPart(new SphereShape(1.5, 12),
                new ParticleRenderer(new ParticleBuilder(Particle.LARGE_SMOKE).count(1).extra(0.35)));
        FxPart witch = new FxPart(new SphereShape(1.5, 35),
                new ParticleRenderer(new ParticleBuilder(Particle.WITCH).count(1).extra(0.1)));

        FxEngine.getInstance().play(FxEffect.builder(loc).duration(1).part(smoke, witch).build());
    }

    private void tpTrail(Location loc) {
        Location point = loc.clone().add(0, 1, 0);

        FxPart witch = new FxPart(new PointShape(), new ParticleRenderer(new ParticleBuilder(Particle.WITCH).count(4).offset(0.3, 0.5, 0.3).extra(1.0)));
        FxPart smoke = new FxPart(new PointShape(), new ParticleRenderer(new ParticleBuilder(Particle.SMOKE).count(10).offset(0.3, 0.5, 0.3).extra(0.025)));

        FxEngine.getInstance().play(FxEffect.builder(point).duration(1).part(witch, smoke).build());
    }

    private void tpSound(World world) {
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_ILLUSIONER_MIRROR_MOVE, Sound.Source.PLAYER, 1f, 1.1f), player);
    }

    private void tpSoundFail(World world) {
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_ILLUSIONER_MIRROR_MOVE, Sound.Source.PLAYER, 1f, 1.8f), player);
    }

    @Override
    public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() {
        return INFO;
    }
}
