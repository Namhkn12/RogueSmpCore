package com.roguesmp.enchant.impl;

import com.roguesmp.constant.DamageType;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.Hitbox;
import io.papermc.paper.persistence.PersistentDataContainerView;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class Smash implements SmpEnchant {

    private static final double RADIUS = 2d;
    private static final double RATIO_PER_LEVEL = 0.1;

    @Override
    public @NotNull String getId() {
        return "smash";
    }

    @Override
    public @NotNull Enchants getEnumConstant() {
        return Enchants.SMASH;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Bộc phá";
    }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultLoreProvider(level);
    }

    @Override
    public @NotNull Set<EquipSlot> getActiveSlots() {
        return Set.of(EquipSlot.MAINHAND);
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Khi tung ra đòn cận chiến Chí Mạng, gây thêm 10% sát thương mỗi cấp trong bán kĩnh 2 khối quanh mục tiêu";
    }

    @Override
    public Material getIcon() {
        return Material.MACE;
    }

    @Override
    public void onDamageEntityFinal(DamageEvent event, int level, @NotNull SmpPlayer player) {
        if (event.getDamageType() != DamageType.MELEE) return;
        if (!event.isCritical()) return;
        Player bukkitPlayer = player.getBukkitPlayer();
        World world = bukkitPlayer.getWorld();
        Entity victim = event.getVictim();

        Particle.EXPLOSION.builder().location(victim.getLocation().add(0, 0.75, 0)).count(0).offset(1.5, 0, 0).spawn();
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_DRAGON_FIREBALL_EXPLODE, Sound.Source.PLAYER,0.2f, 0.6f), victim);
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_PLAYER_ATTACK_CRIT, Sound.Source.PLAYER, 1f, 0.5f));
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_PLAYER_ATTACK_STRONG, Sound.Source.PLAYER, 1f, 0.5f));
        world.playSound(Sound.sound(SoundEventKeys.ENTITY_BLAZE_HURT, Sound.Source.PLAYER, 0.2f, 0.5f));

        Hitbox.SphereHitbox hurtbox = new Hitbox.SphereHitbox(victim.getLocation().add(0, 0.75, 0), RADIUS);
        for (LivingEntity entity : hurtbox.getHitMobs()) {
            if (entity == victim) continue;
            DamageUtils.damage(entity, bukkitPlayer, event.getFinalDamage() * RATIO_PER_LEVEL * level, new DamageEvent.Metadata(DamageType.TRUE));
        }
    }
}
