package com.roguesmp.dungeon_v2.data.definition.spawner.behavior.impl;

import com.roguesmp.dungeon_v2.data.definition.spawner.behavior.BaseBehavior;
import com.roguesmp.dungeon_v2.data.definition.spawner.behavior.BehaviorData;
import com.roguesmp.dungeon_v2.utils.DungeonEcho;
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
                DungeonEcho.error(player,
                        "You are afflicted with nausea!");
                break;

            case BLINDNESS:
                player.addPotionEffect(
                        new PotionEffect(
                                PotionEffectType.BLINDNESS,
                                80,
                                0
                        )
                );
                DungeonEcho.error(player,
                        "Darkness consumes your vision!");
                break;

            case POISON:
                player.addPotionEffect(
                        new PotionEffect(
                                PotionEffectType.POISON,
                                300,
                                0
                        )
                );
                DungeonEcho.error(player,
                        "Poison courses through your veins!");
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

                DungeonEcho.error(player,
                        "Webs entangle your feet!");
                break;

            case FIRE:
                player.setFireTicks(80);

                DungeonEcho.error(player,
                        "You are engulfed in flames!");
                break;
        }
    }
}