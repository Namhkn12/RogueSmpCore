package com.roguesmp.spawner.behavior;

import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.impl.VanillaPotionEffect;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Example json config
 *
 * {
 *   "spawnerId": "cursed_totem",
 *   "behaviors": [
 *     {
 *       "type": "cursed"
 *     }
 *   ]
 * }
 *
 * */
public class CursedSpawner extends BaseBehavior {

    public CursedSpawner(BehaviorData data) {
        super(data);
    }

    private enum CurseEffect {
        NAUSEA,
        BLINDNESS,
        POISON,
        COBWEB,
        FIRE
    }

    @Override
    public boolean onSpawn(
            LivingEntity entity,
            List<Player> players,
            Location location
    ) {

        if (players.isEmpty())
            return true;

        Location center = SpawnerFx.center(location);
        center.getWorld().spawnParticle(Particle.SCULK_SOUL, center, 30, 0.4, 0.4, 0.4, 0.08, null, true);
        SpawnerFx.sound(center, Sound.ENTITY_WITHER_AMBIENT, 2.5f, 0.6f);

        for (Player player : players) {

            List<CurseEffect> effects = new ArrayList<>();

            Collections.addAll(
                    effects,
                    CurseEffect.values()
            );

            Collections.shuffle(effects);

            player.getWorld().spawnParticle(Particle.WITCH, player.getLocation().add(0, 1, 0), 30, 0.4, 0.8, 0.4, 0, null, true);
            player.playSound(player, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f, 1.2f);
            applyCurse(player, effects.get(0));
            applyCurse(player, effects.get(1));
        }

        return true;
    }

    @Override
    protected void onTick(Location location, int ticks) {
        Location center = SpawnerFx.center(location);
        SpawnerFx.column(center, 5, 0.7, Particle.SOUL, null);
        if (ticks % 8 == 0) SpawnerFx.sound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 2.5f, 0.6f);
    }

    private void applyCurse(
            Player player,
            CurseEffect effect
    ) {

        switch(effect) {

            case NAUSEA:
                EffectManager.getInstance().addEffect(player, VanillaPotionEffect.sourceOf(PotionEffectType.NAUSEA),
                        new VanillaPotionEffect(300, PotionEffectType.NAUSEA, 0));
                player.sendMessage(Component.text("You are afflicted with nausea!", NamedTextColor.RED));
                break;

            case BLINDNESS:
                EffectManager.getInstance().addEffect(player, VanillaPotionEffect.sourceOf(PotionEffectType.BLINDNESS),
                        new VanillaPotionEffect(80, PotionEffectType.BLINDNESS, 0));
                player.sendMessage(Component.text("Darkness consumes your vision!", NamedTextColor.RED));
                break;

            case POISON:
                EffectManager.getInstance().addEffect(player, VanillaPotionEffect.sourceOf(PotionEffectType.POISON),
                        new VanillaPotionEffect(300, PotionEffectType.POISON, 0));
                player.sendMessage(Component.text("Poison courses through your veins!", NamedTextColor.RED));
                break;

            case COBWEB:

                Location below =
                        player.getLocation()
                                .clone();

                if (below.getBlock().getType()
                        == Material.AIR) {

                    below.getBlock()
                            .setType(Material.COBWEB);
                }

                player.sendMessage(Component.text("Webs entangle your feet!", NamedTextColor.RED));
                break;

            case FIRE:
                player.setFireTicks(80);

                player.sendMessage(Component.text("You are engulfed in flames!", NamedTextColor.RED));
                break;
        }
    }
}