package com.roguesmp.dungeon_spawner.behavior.impl;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.Component;
import com.roguesmp.dungeon_spawner.behavior.BaseBehavior;
import com.roguesmp.dungeon_spawner.behavior.BehaviorData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
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

        for (Player player : players) {

            List<CurseEffect> effects = new ArrayList<>();

            Collections.addAll(
                    effects,
                    CurseEffect.values()
            );

            Collections.shuffle(effects);

            applyCurse(player, effects.get(0));
            applyCurse(player, effects.get(1));
        }

        return true;
    }

    private void applyCurse(
            Player player,
            CurseEffect effect
    ) {

        switch(effect) {

            case NAUSEA:
                player.addPotionEffect(
                        new PotionEffect(
                                PotionEffectType.NAUSEA,
                                300,
                                0
                        )
                );
                player.sendMessage(Component.text("You are afflicted with nausea!", NamedTextColor.RED));
                break;

            case BLINDNESS:
                player.addPotionEffect(
                        new PotionEffect(
                                PotionEffectType.BLINDNESS,
                                80,
                                0
                        )
                );
                player.sendMessage(Component.text("Darkness consumes your vision!", NamedTextColor.RED));
                break;

            case POISON:
                player.addPotionEffect(
                        new PotionEffect(
                                PotionEffectType.POISON,
                                300,
                                0
                        )
                );
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