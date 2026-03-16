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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FlameWave extends Ability {

    public static final String ID = "flame_wave";
    public static final List<Integer> DAMAGES = List.of(8, 14, 20, 26);

    public static final AbilityInfo<FlameWave> INFO =
            new AbilityInfo.Builder<FlameWave>()
                    .id(ID)
                    .displayText(Component.text("Flame Wave", NamedTextColor.GOLD))
                    .displayIcon(Material.BLAZE_POWDER)
                    .trigger(AbilityTrigger.RIGHT_CLICK)
                    .descriptionProvider((player, lvl) ->
                            List.of(Component.text(
                                    "Bắn sóng lửa gây " + DAMAGES.get(lvl - 1) + " sát thương",
                                    NamedTextColor.GRAY)))
                    .factory(FlameWave::new)
                    .build();

    public FlameWave(SmpPlayer player, int level) {
        super(player, level);
    }

    @Override
    public void cast() {

        Player player = smpPlayer.getBukkitPlayer();
        Location loc = player.getEyeLocation();

        List<LivingEntity> mobs = new Hitbox.SphereHitbox(loc.add(loc.getDirection().multiply(4)), 4).getHitMobs();

        for (LivingEntity mob : mobs) {
            DamageUtils.damage(mob, player, DAMAGES.get(level - 1), new DamageEvent.Metadata(ID, DamageType.MAGIC));
        }

        player.getWorld().spawnParticle(Particle.FLAME, loc, 40, 1,1,1,0.02);

        setCooldownTick(60);
    }

    @Override
    public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() {
        return INFO;
    }
}
