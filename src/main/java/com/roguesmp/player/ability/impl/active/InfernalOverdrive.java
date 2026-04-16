package com.roguesmp.player.ability.impl.active;

import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.trigger.AbilityResponse;
import com.roguesmp.utils.DamageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class InfernalOverdrive extends Ability {
    public static final String ID = "infernal_overdrive";

    // Internal States
    private int chargeTicks = 0;
    private boolean isOverheated = false;
    private int overheatRemaining = 0;

    // Cached Scaling
    private final int maxCharge;
    private final double explosionDmg;
    private final double explosionRadius;
    private final int overheatDuration;
    private final int cooldown;

    public static final AbilityInfo<InfernalOverdrive> INFO = new AbilityInfo<>(
            ID,
            InfernalOverdrive.class,
            InfernalOverdrive::new
    )
            .registerAction("start_charge", InfernalOverdrive::beginCharging)
            .registerAction("release_burst", InfernalOverdrive::detonate);

    public InfernalOverdrive(SmpPlayer player, int level) {
        super(player, level);
        this.maxCharge = (int) getAbilityInfo().getAttributeForLevel("duration_max_charge", level);
        this.explosionDmg = getAbilityInfo().getAttributeForLevel("damage", level);
        this.explosionRadius = getAbilityInfo().getAttributeForLevel("radius", level);
        this.overheatDuration = (int) getAbilityInfo().getAttributeForLevel("duration_overheat", level);
        this.cooldown = (int) getAbilityInfo().getAttributeForLevel("cooldown", level);
    }

    @Override
    public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() {
        return INFO;
    }

    public AbilityResponse beginCharging() {
        if (isOnCooldown() || isOverheated) return AbilityResponse.continueChain();
        return AbilityResponse.consume();
    }

    public AbilityResponse detonate() {
        if (chargeTicks < 10) { // Too short to blast
            chargeTicks = 0;
            return AbilityResponse.deny();
        }

        // Logic for detonation strength based on charge level
        double powerMultiplier = (double) chargeTicks / maxCharge;
        executeExplosion(powerMultiplier);

        // Enter Overheat State
        this.isOverheated = true;
        this.overheatRemaining = overheatDuration;
        this.chargeTicks = 0;
        setCooldownTick(cooldown);

        return AbilityResponse.consume();
    }

    @Override
    public void tick(int period) {
        Player p = smpPlayer.getBukkitPlayer();

        // Stage 1: Charging (While holding shift)
        if (p.isSneaking() && !isOnCooldown() && !isOverheated) {
            if (chargeTicks < maxCharge) {
                chargeTicks += period;
                playChargeEffects(p);
            }
        }

        // Stage 2: Overheat Passive Debuff
        if (isOverheated) {
            overheatRemaining -= period;
            p.setFireTicks(20); // Player smokes/burns slightly while overheated
            if (overheatRemaining <= 0) {
                isOverheated = false;
                p.sendMessage(Component.text("Hệ thống đã hạ nhiệt!", NamedTextColor.GREEN));
            }
        }
    }

    private void executeExplosion(double mult) {
        Location loc = smpPlayer.getBukkitPlayer().getLocation();
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, (int)(10 * mult));
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);

        for (LivingEntity e : loc.getNearbyLivingEntities(explosionRadius * mult)) {
            if ((e instanceof Player)) continue;
            DamageUtils.damage(e, smpPlayer.getBukkitPlayer(), explosionDmg * mult,
                    new DamageEvent.Metadata(ID, DamageType.MAGIC));
        }
    }

    private void playChargeEffects(Player p) {
        p.sendActionBar(Component.text("Charging: " + (chargeTicks * 100 / maxCharge) + "%", NamedTextColor.RED));
        p.getWorld().spawnParticle(Particle.FLAME, p.getLocation(), 5, 0.2, 0.2, 0.2, 0.02);
    }
}
