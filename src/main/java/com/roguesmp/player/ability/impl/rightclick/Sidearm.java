package com.roguesmp.player.ability.impl.rightclick;

import com.destroystokyo.paper.ParticleBuilder;
import com.roguesmp.constant.AbilityTrigger;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.particle.LineShape;
import com.roguesmp.particle.ParticleShape;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
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

import java.util.ArrayList;
import java.util.List;

public class Sidearm extends Ability {
    public static final String ID = "sidearm";

    // Level Scaling
    public static final List<Double> DAMAGES = List.of(6.0, 9.0, 12.0, 15.0, 18.0);
    public static final List<Integer> MAX_STACKS = List.of(3, 3, 4, 4, 5);
    public static final List<Integer> RECHARGE_TICKS = List.of(120, 100, 100, 80, 60); // 6s down to 3s

    private int currentStacks;
    private int rechargeTimer = 0;

    public static final AbilityInfo<Sidearm> INFO = new AbilityInfo.Builder<Sidearm>()
            .id(ID)
            .displayText(Component.text("Sidearm", NamedTextColor.YELLOW, TextDecoration.BOLD))
            .descriptionProvider((p, l) -> List.of(
                    Utils.text("Bắn một tia thép nén từ tầm mắt.", NamedTextColor.GRAY),
                    Utils.text("Sát thương: ", NamedTextColor.GRAY)
                            .append(Utils.text(Utils.formatDecimal(DAMAGES.get(l - 1)), NamedTextColor.YELLOW)),
                    Utils.text("Tối đa: ", NamedTextColor.GRAY)
                            .append(Utils.text(MAX_STACKS.get(l - 1) + " cộng dồn", NamedTextColor.GOLD))
            ))
            .displayIcon(Material.IRON_HORSE_ARMOR)
            .factory(Sidearm::new)
            .trigger(AbilityTrigger.RIGHT_CLICK)
            .build();

    public Sidearm(SmpPlayer player, int level) {
        super(player, level);
        this.currentStacks = MAX_STACKS.get(level - 1);
    }

    @Override
    public void tick(int periodIncrement) {
        int max = MAX_STACKS.get(level - 1);
        if (currentStacks < max) {
            rechargeTimer += periodIncrement;
            if (rechargeTimer >= RECHARGE_TICKS.get(level - 1)) {
                currentStacks++;
                rechargeTimer = 0;
            }
        }
    }

    @Override
    public void cast() {
        Player p = smpPlayer.getBukkitPlayer();
        Location eye = p.getEyeLocation();
        Vector direction = eye.getDirection();
        double range = 20.0; // Slightly buffed range

        RayTraceResult result = p.getWorld().rayTrace(eye, direction, range,
                FluidCollisionMode.NEVER, true, 0.5, (e) -> !e.equals(p) && e instanceof LivingEntity);

        Location endPoint = (result != null) ? result.getHitPosition().toLocation(p.getWorld())
                : eye.clone().add(direction.clone().multiply(range));

        // Damage Logic
        if (result != null && result.getHitEntity() instanceof LivingEntity victim) {
            DamageUtils.damage(victim, p, DAMAGES.get(level - 1), new DamageEvent.Metadata(ID, DamageType.PROJECTILE_ABILITY));
            playImpactEffects(endPoint);
        }

        // Visuals
        playMuzzleEffects(eye);
        renderTracer(eye, endPoint);

        currentStacks--;
        updateActionBar(p);
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
        int max = MAX_STACKS.get(level - 1);
        p.sendActionBar(Component.text("Sidearm: ", NamedTextColor.GRAY)
                .append(Component.text("▮".repeat(currentStacks), NamedTextColor.YELLOW))
                .append(Component.text("▯".repeat(max - currentStacks), NamedTextColor.DARK_GRAY)));
    }

    @Override public boolean isOnCooldown() { return currentStacks <= 0; }
    @Override public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() { return INFO; }
}
