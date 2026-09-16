package com.roguesmp.player.ability.impl.warrior;

import com.destroystokyo.paper.ParticleBuilder;
import com.roguesmp.constant.DamageType;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.impl.SpeedEffect;
import com.roguesmp.effect.impl.StunEffect;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.event.PlayerStartBlockAttackEvent;
import com.roguesmp.fx.FxEffect;
import com.roguesmp.fx.FxEngine;
import com.roguesmp.fx.FxPart;
import com.roguesmp.fx.render.ParticleRenderer;
import com.roguesmp.fx.shape.FxShape;
import com.roguesmp.fx.shape.PointShape;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.utils.*;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ShieldBash extends Ability {
    //No cast because it is based on event.
    public static final AbilityInfo<ShieldBash> INFO = new AbilityInfo<>("shield_bash", ShieldBash.class, ShieldBash::new);

    private final Player player;

    private final double damage;
    private final int cooldown;
    private final int stunDuration;
    private final double range;
    private final double radius;
    private final double knockbackStrength;

    public ShieldBash(SmpPlayer smpPlayer, int level) {
        super(smpPlayer, level);
        this.player = smpPlayer.getBukkitPlayer();

        damage = getBaseAttributeValue("damage");
        cooldown = (int) getBaseAttributeValue("cooldown");
        stunDuration = (int) getBaseAttributeValue("stun_duration");
        range = getBaseAttributeValue("range");
        radius = getBaseAttributeValue("radius");
        knockbackStrength = getBaseAttributeValue("knockback_strength");
    }

    @Override
    public void onStartBlocking(PlayerStartBlockAttackEvent event) {
        if (isOnCooldown()) return;

        LivingEntity mob = EntityUtils.getLivingEntityAtCursor(player, range, entity -> entity.getType() != EntityType.PLAYER, 0.425);
        if (mob == null) {
            return;
        }

        setCooldownTick(cooldown);
        Location mobLoc = mob.getEyeLocation();

        castEffect(player, mobLoc);
        bash(mob);
    }

    private void bash(LivingEntity mob) {

        Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mob), radius);
        for (LivingEntity le : hitbox.getHitMobs()) {
            DamageUtils.damage(le, player, damage, new DamageEvent.Metadata(DamageType.MELEE_ABILITY));
            if (knockbackStrength > 0) {
                MovementUtils.knockAway(player, le, (float) knockbackStrength);
            }
            if (EntityUtils.isBoss(le) || EntityUtils.isElite(le)) {
                EffectManager.getInstance().addEffect(le, "sb_slowness", new SpeedEffect(stunDuration, -0.99, "sb_slowness"));
            } else {
                EffectManager.getInstance().addEffect(le, StunEffect.ID, new StunEffect(stunDuration));
            }
        }

    }

    private void castEffect(Player player, Location mobLoc) {
        FxShape shape = new PointShape();
        ParticleBuilder crit = new ParticleBuilder(Particle.CRIT).count(50).offset(0.1, 0.2, 0.1).extra(0.3);
        ParticleBuilder critMagic = new ParticleBuilder(Particle.ENCHANTED_HIT).count(50).offset(0.1, 0.25, 0.1).extra(0.25);
        ParticleBuilder cloud = new ParticleBuilder(Particle.CLOUD).count(5).offset(0.15, 0.15, 0.15).extra(0);
        FxEngine.getInstance().play(FxEffect.builder(mobLoc).part(new FxPart(shape, new ParticleRenderer(crit, critMagic, cloud))).duration(1).build());

        World world = player.getWorld();
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_PLAYER_ATTACK_CRIT, Sound.Source.PLAYER, 1.3f, 0.1f), player);
        world.playSound(Sound.sound(SoundEventKeys.ITEM_SHIELD_BLOCK, Sound.Source.PLAYER, 1.3f, 0.1f), player);
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_PLAYER_ATTACK_STRONG, Sound.Source.PLAYER, 1.3f, 0.1f), player);
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_ELDER_GUARDIAN_HURT, Sound.Source.PLAYER, 0.7f, 0.1f), player);
        world.playSound(Sound.sound(SoundEventKeys.ITEM_SHIELD_BREAK, Sound.Source.PLAYER, 0.7f, 0.1f), player);
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, Sound.Source.PLAYER, 0.4f, 0.1f), player);
    }

    @Override
    public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() {
        return INFO;
    }
}
