package com.roguesmp.player.ability.impl.lifeline;

import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.effect.impl.ResistanceEffect;
import com.roguesmp.effect.impl.SpeedEffect;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.AbilityType;
import com.roguesmp.player.ability.trigger.AbilityResponse;
import com.roguesmp.utils.EntityUtils;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LastBreath extends Ability {
    public static final String ID = "last_breath";

    // Trigger Threshold
    private static final double TRIGGER_HEALTH_PERCENT = 0.40;
    private static final double KNOCKBACK_RADIUS = 5.0;
    private static final double KNOCKBACK_STRENGTH = 2.0;

    // Cached Attributes
    private final double cdrPercent;
    private final int resistTicks;
    private final double speedBoost;
    private final int cooldownTicks;

    /**
     * Action-based INFO registration.
     */
    public static final AbilityInfo<LastBreath> INFO = new AbilityInfo<>(
            ID,
            LastBreath.class,
            LastBreath::new
    );

    public LastBreath(SmpPlayer player, int level) {
        super(player, level);
        // Cache attributes from JSON scaling
        this.cdrPercent = getAbilityInfo().getAttributeForLevel("cdr_percent", level);
        this.resistTicks = (int) getAbilityInfo().getAttributeForLevel("duration_resist", level);
        this.speedBoost = getAbilityInfo().getAttributeForLevel("speed_boost", level);
        this.cooldownTicks = (int) getAbilityInfo().getAttributeForLevel("cooldown", level);
    }

    @Override
    public void onHurt(DamageEvent event) {
        if (isOnCooldown()) return;

        Player p = smpPlayer.getBukkitPlayer();
        double currentHealth = p.getHealth();
        double maxHealth = EntityUtils.getMaxHealth(p);

        // Check if health after damage is below 40%
        double healthAfterDamage = currentHealth - event.getFinalDamage();
        if (healthAfterDamage > (maxHealth * TRIGGER_HEALTH_PERCENT)) {
            return;
        }

        // Activate Lifeline
        activate();
//        EntityUtils.setHealthPercent(p, 0.4);
        event.setCancelled(true);
    }

    public void activate() {
        setCooldownTick(cooldownTicks);
        Player p = smpPlayer.getBukkitPlayer();
        World world = p.getWorld();
        Location loc = p.getLocation();

        // 1. Cooldown Reduction Logic for other abilities
        for (Ability abil : smpPlayer.getAbilityLoadout().getAbilities(AbilityType.ACTIVE)) {
            if (abil == null || abil.equals(this)) return;

            int currentCD = abil.getCooldownTick();
            if (currentCD > 0) {
                int newCD = (int) (currentCD * (1.0 - cdrPercent));
                abil.setCooldownTick(newCD);
            }
        }

        for (Ability abil : smpPlayer.getAbilityLoadout().getAbilities(AbilityType.PASSIVE)) {
            if (abil == null || abil.equals(this)) return;

            int currentCD = abil.getCooldownTick();
            if (currentCD > 0) {
                int newCD = (int) (currentCD * (1.0 - cdrPercent));
                abil.setCooldownTick(newCD);
            }
        }

        // 2. Defensive Buffs using cached values
        EffectManager.getInstance().addEffect(p, "last_breath_resistance",
                new ResistanceEffect(resistTicks, 1, SmpEffect.DeathBehavior.REMOVE_ON_DEATH));

        EffectManager.getInstance().addEffect(p, "last_breath_speed",
                new SpeedEffect(120, speedBoost, "last_breath_speed"));

        // 3. Shockwave Knockback
        for (LivingEntity victim : EntityUtils.getNearbyMobs(loc, KNOCKBACK_RADIUS, 3, KNOCKBACK_RADIUS, living -> true)) {
            Vector dir = victim.getLocation().toVector().subtract(loc.toVector()).normalize();
            if (Double.isNaN(dir.getX())) dir = new Vector(0, 1, 0);

            victim.setVelocity(dir.multiply(KNOCKBACK_STRENGTH).setY(0.5));
            world.spawnParticle(Particle.EXPLOSION, victim.getLocation(), 5, 0.1, 0.1, 0.1, 0.2);
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

    @Override public @NotNull AbilityInfo<?> getAbilityInfo() { return INFO; }
}
