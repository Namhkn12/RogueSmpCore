package com.roguesmp.player.ability.impl.active;

import com.roguesmp.constant.DamageType;
import com.roguesmp.constant.Keys;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.trigger.AbilityResponse;
import com.roguesmp.utils.DamageUtils;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class AetherStance extends Ability {
    public static final String ID = "aether_stance";

    private boolean isActive = false;
    private int shotsRemaining = 0;
    private int ticksLeft = 0;

    private final double beamDamage;
    private final int maxShots;
    private final int durationTicks;
    private final double dashPower;
    private final int cooldown;

    /**
     * 1. activate: Starts the stance.
     * 2. fire: Handles shooting.
     * 3. dash: Handles movement.
     */
    public static final AbilityInfo<AetherStance> INFO = new AbilityInfo<>(
            ID, AetherStance.class, AetherStance::new
    ).registerAction("activate", AetherStance::handleActivate)
            .registerAction("fire", AetherStance::handleFire)
            .registerAction("dash", AetherStance::handleDash);

    public AetherStance(SmpPlayer player, int level) {
        super(player, level);
        this.beamDamage = getAbilityInfo().getAttributeForLevel("damage", level);
        this.maxShots = (int) getAbilityInfo().getAttributeForLevel("shots", level);
        this.durationTicks = (int) getAbilityInfo().getAttributeForLevel("duration", level);
        this.dashPower = getAbilityInfo().getAttributeForLevel("dash_power", level);
        this.cooldown = (int) getAbilityInfo().getAttributeForLevel("cooldown", level);
    }

    @Override
    public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() {
        return INFO;
    }

    // --- Action 1: Activation ---
    public AbilityResponse handleActivate() {
        if (isOnCooldown() || isActive) return AbilityResponse.deny();

        startStance();
        return AbilityResponse.capture(durationTicks);
    }

    // --- Action 2: Shooting (Triggered by click while captured) ---
    public AbilityResponse handleFire() {
        if (!isActive) return AbilityResponse.continueChain();

        executeBeam(smpPlayer.getBukkitPlayer());
        shotsRemaining--;

        if (shotsRemaining <= 0) {
            return endStance();
        }

        return AbilityResponse.capture(ticksLeft); // Renew capture with remaining time
    }

    // --- Action 3: Dashing (Triggered by shift while captured) ---
    public AbilityResponse handleDash() {
        if (!isActive) return AbilityResponse.continueChain();

        executeDash(smpPlayer.getBukkitPlayer());
        // Dashing doesn't consume shots, just maintains the state
        return AbilityResponse.capture(ticksLeft);
    }

    private void startStance() {
        this.isActive = true;
        this.shotsRemaining = maxShots;
        this.ticksLeft = durationTicks;

        Player p = smpPlayer.getBukkitPlayer();
        startLevitate(p);

        // --- Sound: Cinematic Power Up ---
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 1.5f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.0f);
    }

    private AbilityResponse endStance() {
        this.isActive = false;
        this.ticksLeft = 0;

        Player p = smpPlayer.getBukkitPlayer();
        removeLevitate(p);
        setCooldownTick(cooldown);

        // --- Sound: Power Down / Exit ---
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.8f);

        return AbilityResponse.release();
    }

    private void executeBeam(Player p) {
        // --- Sound: Sharp Energy Blast ---
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 0.8f, 2.0f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.2f);

        RayTraceResult result = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getLocation().getDirection(), 20, 0.5, e -> e != p);

        // Particles
        p.getWorld().spawnParticle(Particle.END_ROD, p.getEyeLocation(), 15, p.getLocation().getDirection().getX(), p.getLocation().getDirection().getY(), p.getLocation().getDirection().getZ(), 0.5);

        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            DamageUtils.damage(target, p, beamDamage, new DamageEvent.Metadata(ID, DamageType.MAGIC));
            // --- Sound: Hit Confirmation ---
            p.playSound(p.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.5f, 0.5f);
        }
    }

    private void executeDash(Player p) {
        p.setVelocity(p.getLocation().getDirection().multiply(dashPower));

        // --- Sound: Wind Whoosh ---
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.5f);
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_ELYTRA, 1.0f, 1.2f);

        p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 20, 0.2, 0.2, 0.2, 0.1);
    }

    @Override
    public void tick(int period) {
        if (!isActive) return;

        ticksLeft -= period;

        if (ticksLeft <= 0) {
            endStance();
        }
    }

    public void startLevitate(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 8, 2));
        AttributeInstance ai = player.getAttribute(Attribute.GRAVITY);
        if (ai != null) {
            ai.removeModifier(Keys.of("aether_stance"));
            ai.addTransientModifier(new AttributeModifier(Keys.of("aether_stance"), -0.08, AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    public void removeLevitate(Player player) {
        player.setFallDistance(0);
        AttributeInstance ai = player.getAttribute(Attribute.GRAVITY);
        if (ai != null) {
            ai.removeModifier(Keys.of("aether_stance"));
        }
    }
}
