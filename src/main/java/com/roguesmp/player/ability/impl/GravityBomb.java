package com.roguesmp.player.ability.impl;

import com.roguesmp.constant.AbilityTrigger;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.Hitbox;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GravityBomb extends Ability {
    public static final String ID = "gravity_bomb";
    public static final List<Integer> DAMAGES = List.of(10, 20, 30, 40);
    public static final AbilityInfo<GravityBomb> INFO = new AbilityInfo.Builder<GravityBomb>()
            .id(ID)
            .descriptionProvider((smpPlayer1, integer) -> List.of(Component.text("Thả bom trọng lực, gây " + DAMAGES.get(integer - 1) + " sát thương", NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false)))
            .displayText(Component.text("Gravity Bomb", NamedTextColor.BLUE))
            .displayIcon(Material.TNT)
            .factory(GravityBomb::new)
            .trigger(AbilityTrigger.SWAP)
            .build();

    public GravityBomb(SmpPlayer player, int level) {
        super(player, level);
    }

    @Override
    public void cast() {
        RayTraceResult result = smpPlayer.getBukkitPlayer().rayTraceEntities(12);
        if (result != null) {
            doExplosion(result.getHitEntity().getLocation());
        }
        setCooldownTick(100);
    }

    @Override
    public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() {
        return INFO;
    }

    private void doExplosion(Location loc) {
        World world = loc.getWorld();
        world.spawnParticle(Particle.EXPLOSION, loc, 10);
        world.playSound(loc, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.5f, 2.0f);
        world.playSound(loc, Sound.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 1.5f, 2.0f);
        world.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 1.5f, 0.5f);
        world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.PLAYERS, 1.5f, 1.2f);
        world.playSound(loc, Sound.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.PLAYERS, 1.5f, 0.5f);
        world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 1.5f, 1.2f);

        List<LivingEntity> mobs = new Hitbox.SphereHitbox(loc, 12).getHitMobs();
        mobs.removeIf(mob -> mob instanceof Player);

        for (LivingEntity mob : mobs) {
            DamageUtils.damage(mob, getSmpPlayer().getBukkitPlayer(), 100, new DamageEvent.Metadata(ID, DamageType.PROJECTILE_ABILITY));
        }
    }
}
