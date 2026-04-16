package com.roguesmp.player.ability.impl.active;

import com.destroystokyo.paper.ParticleBuilder;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.particle.LineShape;
import com.roguesmp.particle.ParticleShape;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.trigger.AbilityResponse;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.ParticleUtils;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Sidearm extends Ability {
    public static final String ID = "sidearm";

    // Cached Attributes
    private final double damage;
    private final int maxStacks;
    private final int rechargeTicks;

    // State variables
    private int currentStacks;
    private int rechargeTimer = 0;

    /**
     * Action-based INFO shell.
     * Descriptions and Scaling are now handled by JSON.
     */
    public static final AbilityInfo<Sidearm> INFO = new AbilityInfo<>(
            ID,
            Sidearm.class,
            Sidearm::new
    ).registerAction("execute", Sidearm::handleCast);

    public Sidearm(SmpPlayer player, int level) {
        super(player, level);
        // Cache attributes from JSON
        this.damage = getAbilityInfo().getAttributeForLevel("damage", level);
        this.maxStacks = (int) getAbilityInfo().getAttributeForLevel("max_stacks", level);
        this.rechargeTicks = (int) getAbilityInfo().getAttributeForLevel("cooldown", level);

        // Initialize state
        this.currentStacks = maxStacks;
    }

    @Override
    public void tick(int periodIncrement) {
        // Handle stack regeneration
        if (currentStacks < maxStacks) {
            rechargeTimer += periodIncrement;
            if (rechargeTimer >= rechargeTicks) {
                currentStacks++;
                rechargeTimer = 0;
                // Optional: Sound effect when a stack is ready
                smpPlayer.getBukkitPlayer().playSound(smpPlayer.getBukkitPlayer().getLocation(),
                        Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 2.0f);
            }
        }
    }

    public AbilityResponse handleCast() {
        if (currentStacks <= 0) {
            smpPlayer.getBukkitPlayer().playSound(smpPlayer.getBukkitPlayer().getLocation(),
                    Sound.BLOCK_DISPENSER_FAIL, 1f, 2f);
            return AbilityResponse.continueChain();
        }

        Player p = smpPlayer.getBukkitPlayer();
        Location eye = p.getEyeLocation();
        Vector direction = eye.getDirection();
        double range = 25.0; // Consistent range

        // 1. Raytrace
        RayTraceResult result = p.getWorld().rayTrace(eye, direction, range,
                FluidCollisionMode.NEVER, true, 0.5, (e) -> !(e instanceof Player) && e instanceof LivingEntity);

        Location endPoint = (result != null && result.getHitPosition() != null)
                ? result.getHitPosition().toLocation(p.getWorld())
                : eye.clone().add(direction.clone().multiply(range));

        // 2. Damage Logic using cached damage
        if (result != null && result.getHitEntity() instanceof LivingEntity victim) {
            DamageUtils.damage(victim, p, damage, new DamageEvent.Metadata(ID, DamageType.PROJECTILE_ABILITY));
            playImpactEffects(endPoint);
        }

        // 3. Visuals & Consume Stack
        playMuzzleEffects(eye);
        renderTracer(eye, endPoint);

        currentStacks--;
        updateActionBar(p);

        return AbilityResponse.consume();
    }

    private void renderTracer(Location start, Location end) {
        double dist = start.distance(end);
        Vector dir = end.toVector().subtract(start.toVector()).normalize();

        // Use Z-aligned LineShape (matches the standard Matrix)
        ParticleShape lineShape = new LineShape(dist, 0.3);

        ParticleBuilder steel = new ParticleBuilder(Particle.CRIT)
                .count(0)
                .offset(0.05, 0.05, 0.05);

        // Pointing the location clone handles the rotation matrix perfectly
        ParticleUtils.spawnShape(start.clone().setDirection(dir), lineShape, List.of(steel), 0);
    }

    private void playMuzzleEffects(Location eye) {
        World world = eye.getWorld();
        world.playSound(eye, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1f, currentStacks == 1 ? 0.8f : 0.6f);
        world.playSound(eye, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 1f, 2f);
        world.playSound(eye, Sound.BLOCK_NOTE_BLOCK_SNARE, SoundCategory.PLAYERS, 1f, currentStacks == 1 ? 1.5f : 0.5f);
        world.playSound(eye, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, SoundCategory.PLAYERS, 1f, currentStacks == 1 ? 1.5f : 0.5f);
        world.playSound(eye, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.PLAYERS, 1f, 2f);

        // Muzzle smoke
        world.spawnParticle(Particle.SMOKE, eye.clone().add(eye.getDirection().multiply(0.5)), 3, 0.05, 0.05, 0.05, 0.02);
    }

    private void playImpactEffects(Location loc) {
        World world = loc.getWorld();
        world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS,1f, 0f);
        world.playSound(loc, Sound.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.3f, 1.1f);

        // "Steel" impact sparks
        world.spawnParticle(Particle.FIREWORK, loc, 3, 0.1, 0.1, 0.1, 0.05);
        world.spawnParticle(Particle.CRIT, loc, 5, 0.1, 0.1, 0.1, 0.1);
    }

    private void updateActionBar(Player p) {
        p.sendActionBar(Component.text("Sidearm: ", NamedTextColor.GRAY)
                .append(Component.text("▮".repeat(currentStacks), NamedTextColor.YELLOW))
                .append(Component.text("▯".repeat(maxStacks - currentStacks), NamedTextColor.DARK_GRAY)));
    }

    @Override public boolean isOnCooldown() { return currentStacks <= 0; }
    @Override public @NotNull AbilityInfo<?> getAbilityInfo() { return INFO; }
}
