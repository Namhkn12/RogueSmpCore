package com.roguesmp.player.ability.impl.lifeline;

import com.roguesmp.constant.AbilityTrigger;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.effect.impl.ResistanceEffect;
import com.roguesmp.effect.impl.SpeedEffect;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.upgrade.ExpRequirement;
import com.roguesmp.player.ability.upgrade.ItemRequirement;
import com.roguesmp.player.ability.upgrade.UpgradeRequirement;
import com.roguesmp.registry.ItemRegistry;
import com.roguesmp.utils.EntityUtils;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LastBreath extends Ability {
    public static final String ID = "last_breath";

    // Trigger Threshold
    private static final double TRIGGER_HEALTH_PERCENT = 0.40; // 40% Health

    // Level Scaling
    private static final List<Double> CDR_PERCENT = List.of(0.4, 0.6, 0.7, 0.8, 1.0);
    private static final List<Integer> RESIST_TICKS = List.of(30, 40, 45, 50, 70);
    private static final List<Double> SPEED_BOOST = List.of(0.1, 0.15, 0.2, 0.25, 0.35);
    private static final List<Integer> COOLDOWN_LEVELS = List.of(1200, 1100, 1000, 900, 800); // 60s to 40s

    private static final double KNOCKBACK_RADIUS = 5.0;
    private static final double KNOCKBACK_STRENGTH = 2.0;

    public static final AbilityInfo<LastBreath> INFO = new AbilityInfo.Builder<LastBreath>()
            .id(ID)
            .displayText(Component.text("Last Breath", NamedTextColor.AQUA, TextDecoration.BOLD))
            .descriptionProvider((p, l) -> List.of(
                    Utils.text("Kích hoạt khi máu dưới 40%.", NamedTextColor.GRAY),
                    Utils.text("Giảm hồi chiêu kỹ năng khác: ", NamedTextColor.GRAY)
                            .append(Utils.text((int)(CDR_PERCENT.get(l-1) * 100) + "%", NamedTextColor.GREEN)),
                    Utils.text("Nhận 100% miễn thương và tốc độ trong thời gian ngắn.", NamedTextColor.GRAY),
                    Utils.text("Đẩy lùi mọi kẻ địch xung quanh.", NamedTextColor.DARK_AQUA)
            ))
            .displayIcon(Material.DRAGON_BREATH)
            .factory(LastBreath::new)
            .trigger(AbilityTrigger.LIFELINE)
            .build();

    public LastBreath(SmpPlayer player, int level) {
        super(player, level);
    }

    @Override
    public void onHurt(DamageEvent event) {
        Player p = smpPlayer.getBukkitPlayer();
        double currentHealth = p.getHealth();
        double maxHealth = p.getAttribute(Attribute.MAX_HEALTH).getValue();

        // Check if health after damage is below 40% and ability is off cooldown
        if ((currentHealth - event.getFinalDamage()) > (maxHealth * TRIGGER_HEALTH_PERCENT) || isOnCooldown()) {
            return;
        }

        activate();
        event.setCancelled(true);
    }

    private void activate() {
        setCooldownTick(COOLDOWN_LEVELS.get(level - 1));
        Player p = smpPlayer.getBukkitPlayer();
        World world = p.getWorld();
        Location loc = p.getLocation();

        double cdr = CDR_PERCENT.get(level - 1);
        int resistTicks = RESIST_TICKS.get(level - 1);
        double speed = SPEED_BOOST.get(level - 1);

        // 1. Cooldown Reset Logic
        for (Ability abil : smpPlayer.getAbilityLoadout().getAbilities()) {
            if (abil.equals(this)) continue;

            int currentCD = abil.getCooldownTick();
            if (currentCD > 0) {
                int newCD = (int) (currentCD * (1.0 - cdr));
                abil.setCooldownTick(newCD);
            }
        }

        // 2. Defensive Buffs
        EffectManager.getInstance().addEffect(p, "last_breath_resistance", new ResistanceEffect(resistTicks, 1, SmpEffect.DeathBehavior.REMOVE_ON_DEATH));
        EffectManager.getInstance().addEffect(p, "last_breath_speed", new SpeedEffect(120, speed, "last_breath_speed"));

        // 3. Shockwave Knockback
        for (LivingEntity e : EntityUtils.getNearbyMobs(loc, KNOCKBACK_RADIUS, 3, KNOCKBACK_RADIUS, living -> true)) {
            if (e instanceof LivingEntity victim) {
                Vector dir = victim.getLocation().toVector().subtract(loc.toVector()).normalize();
                if (Double.isNaN(dir.getX())) dir = new Vector(0, 1, 0);

                victim.setVelocity(dir.multiply(KNOCKBACK_STRENGTH).setY(0.5));
                world.spawnParticle(Particle.EXPLOSION, victim.getLocation(), 5, 0.1, 0.1, 0.1, 0.2);
            }
        }

        // 4. Visuals & Sound
        Location eye = p.getEyeLocation();
        world.spawnParticle(Particle.END_ROD, eye, 50, 0.5, 0.5, 0.5, 0.2);
        world.spawnParticle(Particle.CLOUD, eye, 75, 0.5, 0.5, 0.5, 0.3);
        world.spawnParticle(Particle.HAPPY_VILLAGER, eye, 30, 1, 1, 1, 0);

        world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 2f, 1.2f);
        world.playSound(loc, Sound.ITEM_TOTEM_USE, 1f, 1.5f);
        world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, 2f, 0.5f);

        p.sendActionBar(Component.text("LAST BREATH ACTIVATED!", NamedTextColor.AQUA, TextDecoration.BOLD));
    }

    @Override public void cast() { /* Passive */ }

    @Override public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() { return INFO; }
}
