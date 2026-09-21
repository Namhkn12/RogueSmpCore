package com.roguesmp.player.ability.impl.warrior;

import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.Hitbox;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BruteForce extends Ability {

    public static final AbilityInfo<BruteForce> INFO = new AbilityInfo<>("brute_force", BruteForce.class, BruteForce::new);

    private final Player player;

    private final double radius;
    private final double damage;

    public BruteForce(SmpPlayer smpPlayer, int level) {
        super(smpPlayer, level);
        player = smpPlayer.getBukkitPlayer();
        radius = getBaseAttributeValue("radius");
        damage = getBaseAttributeValue("damage");
    }

    @Override
    public void onDamageEntity(DamageEvent event) {
        if (event.getDamageType() == DamageType.MELEE) return;
        if (!event.isCritical()) return;
        World world = player.getWorld();
        Entity victim = event.getVictim();

        Particle.EXPLOSION.builder().location(victim.getLocation().add(0, 0.75, 0)).count(0).offset(1.5, 0, 0).spawn();
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_DRAGON_FIREBALL_EXPLODE, Sound.Source.PLAYER,0.2f, 0.6f), victim);
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_PLAYER_ATTACK_CRIT, Sound.Source.PLAYER, 1f, 0.5f), victim);
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_PLAYER_ATTACK_STRONG, Sound.Source.PLAYER, 1f, 0.5f), victim);
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_BLAZE_HURT, Sound.Source.PLAYER, 0.2f, 0.5f), victim);

        Hitbox.SphereHitbox hurtbox = new Hitbox.SphereHitbox(victim.getLocation().add(0, 0.75, 0), radius);
        for (LivingEntity entity : hurtbox.getHitMobs()) {
            if (entity == victim) continue;
            DamageEvent.Metadata metadata = new DamageEvent.Metadata(DamageType.MELEE_ABILITY);
            metadata.setIgnoreIframe(true);
            DamageUtils.damage(entity, player, damage, metadata);
        }
    }

    @Override
    public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() {
        return INFO;
    }
}
