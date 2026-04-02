package com.roguesmp.player.ability.impl.lifeline;

import com.roguesmp.constant.AbilityTrigger;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.EntityUtils;
import com.roguesmp.utils.MovementUtils;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SelfDestruct extends Ability {
    public static final String ID = "self_destruct";

    public static double RADIUS = 6;
    public static List<Double> healthPerLevel = List.of(50d, 55d, 60d, 65d, 70d);
    public static List<Double> dmgPerLevel = List.of(30d, 40d, 45d, 50d, 55d);

    public static final AbilityInfo<SelfDestruct> INFO = new AbilityInfo.Builder<SelfDestruct>()
            .id(ID)
            .displayText(Component.text("Tự bạo", NamedTextColor.GOLD))
            .descriptionProvider((player, integer) -> List.of(Utils.text("Khi nhận sát thương chí tử, lập tức bỏ qua sát thương đó, hồi " + Utils.formatDecimal(healthPerLevel.get(integer - 1)) +"% máu và phát nổ gây " + Utils.formatDecimal(dmgPerLevel.get(integer - 1)) + " sát thương cho kẻ địch xung quanh", NamedTextColor.RED)))
            .displayIcon(Material.LANTERN)
            .factory(SelfDestruct::new)
            .trigger(AbilityTrigger.LIFELINE)
            .build();


    public SelfDestruct(SmpPlayer smpPlayer, int level) {
        super(smpPlayer, level);
    }



    @Override
    public void cast() {
        Player player = smpPlayer.getBukkitPlayer();
        World world = player.getWorld();
        Location explosionLoc = player.getLocation();
        explosionLoc.add(0, 0.5, 0);
        world.spawnParticle(Particle.EXPLOSION, explosionLoc, 0, -2, 0, 0);

        world.playSound(explosionLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1f, 1.2f);

        List<LivingEntity> entities = EntityUtils.getNearbyMobs(explosionLoc, RADIUS, RADIUS, RADIUS, living -> true);
        for (LivingEntity entity : entities) {
            DamageUtils.damage(entity, player, dmgPerLevel.get(level), new DamageEvent.Metadata(ID, DamageType.MAGIC));
            MovementUtils.knockAway(player, entity, 0.4f);
        }
        setCooldownTick(120 * 20);
    }

    @Override
    public void onHurtFatal(DamageEvent event) {
        if (isOnCooldown()) return;
        event.setCancelled(true);
        if (event.getVictim() instanceof Player player) {
            EntityUtils.healPercent(player, 50);
            cast();
        }
    }

    @Override
    public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() {
        return INFO;
    }
}
