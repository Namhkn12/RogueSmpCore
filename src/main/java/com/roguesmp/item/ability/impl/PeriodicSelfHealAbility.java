package com.roguesmp.item.ability.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.ability.ItemAbility;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.EntityUtils;
import org.bukkit.entity.Player;

/**
 * Every {@code cooldownTicks} while equipped, heals the wearer for {@code healPercent} of their
 * missing health - demonstrates the cooldown-gated shape (vs. {@link BurnOnHitAbility}'s stateless
 * one). Cooldown is tracked on {@link SmpPlayer}, not this ability instance, since non-unique
 * {@code SmpItem}s (and thus their components/abilities) get rebuilt fresh on every wrap.
 */
public class PeriodicSelfHealAbility implements ItemAbility {

    public static final String TYPE_KEY = "periodic_self_heal";

    public static final Codec<PeriodicSelfHealAbility> CODEC = Codec.composite(
            Codec.DOUBLE.optionalFieldOf("healPercent", 0.1).forGetter(PeriodicSelfHealAbility::healPercent),
            Codec.INT.optionalFieldOf("cooldownTicks", 200).forGetter(PeriodicSelfHealAbility::cooldownTicks),
            PeriodicSelfHealAbility::new
    );

    private final double healPercent;
    private final int cooldownTicks;

    public PeriodicSelfHealAbility(double healPercent, int cooldownTicks) {
        this.healPercent = healPercent;
        this.cooldownTicks = cooldownTicks;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public void onTick(SmpPlayer player, SmpItem item, int interval) {
        String cooldownKey = TYPE_KEY + ":" + item.getId();
        if (player.isAbilityOnCooldown(cooldownKey)) return;

        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null) return;

        double maxHealth = EntityUtils.getMaxHealth(bukkitPlayer);
        double missing = maxHealth - bukkitPlayer.getHealth();
        if (missing <= 0) return;

        bukkitPlayer.setHealth(Math.min(maxHealth, bukkitPlayer.getHealth() + missing * healPercent));
        player.setAbilityCooldown(cooldownKey, cooldownTicks * 50L);
    }

    public double healPercent() {
        return healPercent;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }
}
