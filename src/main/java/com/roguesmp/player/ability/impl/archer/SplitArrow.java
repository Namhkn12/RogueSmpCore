package com.roguesmp.player.ability.impl.archer;

import com.destroystokyo.paper.ParticleBuilder;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.EntityUtils;
import com.roguesmp.utils.LocationUtils;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SplitArrow extends Ability {

    public static final AbilityInfo<SplitArrow> INFO = new AbilityInfo<>("split_arrow", SplitArrow.class, SplitArrow::new);

    private final Player player;

    private final double range;
    private final int bounce;
    private final double multiplier;

    public SplitArrow(SmpPlayer smpPlayer, int level) {
        super(smpPlayer, level);
        player = smpPlayer.getBukkitPlayer();
        range = getBaseAttributeValue("range");
        bounce = (int) getBaseAttributeValue("bounce");
        multiplier = getBaseAttributeValue("multiplier");
    }

    @Override
    public void onDamageEntity(DamageEvent event) {
        if (event.getDamageType() != DamageType.PROJECTILE || !(event.getDamager() instanceof Projectile) || EntityUtils.isFriendly(event.getDamager())) return;
        if (!(event.getVictim() instanceof LivingEntity living)) return;
        double damage = event.getFinalDamage() * multiplier;
        LivingEntity sourceEnemy = living;
        List<LivingEntity> chainedMobs = new ArrayList<>();
        for (int j = 0; j < bounce; j++) {
            chainedMobs.add(sourceEnemy);
            List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(sourceEnemy.getLocation(), range, range, range, living1 -> !chainedMobs.contains(living1));
            LivingEntity nearestMob = EntityUtils.getNearestMob(sourceEnemy.getLocation(), nearbyMobs);
            if (nearestMob == null) {
                break;
            }
            Location loc = sourceEnemy.getEyeLocation();
            Location eye = nearestMob.getEyeLocation();
            Vector dir = LocationUtils.getDirectionTo(eye, loc);
            World world = player.getWorld();
            ParticleBuilder line = new ParticleBuilder(Particle.CRIT).location(loc).count(2).offset(0.1, 0.1, 0.1).extra(0);
            for (int i = 0; i < 50; i++) {
                loc.add(dir.clone().multiply(0.1));
                line.location(loc).spawn();
                if (loc.distance(eye) < 0.4) {
                    break;
                }
            }

            new ParticleBuilder(Particle.CRIT).location(eye).count(30).offset(0, 0,0).extra(0.6).spawn();
            new ParticleBuilder(Particle.ENCHANTED_HIT).location(eye).count(20).offset(0, 0, 0).extra(0.6).spawn();
            world.playSound(Sound.sound(SoundEventKeys.ENTITY_ARROW_HIT, Sound.Source.PLAYER, 1, 1.2f), nearestMob);
            DamageEvent.Metadata metadata = new DamageEvent.Metadata(DamageType.UNSCALABLE_ABILITY);
            metadata.setIgnoreIframe(true);
            metadata.setDoKnockback(false);
            DamageUtils.damage(nearestMob, player, damage, metadata);

            sourceEnemy = nearestMob;
        }
    }

    @Override
    public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() {
        return INFO;
    }
}
