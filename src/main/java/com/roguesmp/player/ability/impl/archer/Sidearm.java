package com.roguesmp.player.ability.impl.archer;

import com.destroystokyo.paper.ParticleBuilder;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.fx.FxEffect;
import com.roguesmp.fx.FxEngine;
import com.roguesmp.fx.FxPart;
import com.roguesmp.fx.render.ParticleRenderer;
import com.roguesmp.fx.shape.FxShape;
import com.roguesmp.fx.shape.LineShape;
import com.roguesmp.fx.shape.PointShape;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.AbilityWithCharge;
import com.roguesmp.player.ability.trigger.AbilityResponse;
import com.roguesmp.utils.DamageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class Sidearm extends AbilityWithCharge {
    public static final String ID = "sidearm";

    private final double damage;
    private final int cooldown;

    public static final AbilityInfo<Sidearm> INFO = new AbilityInfo<>(
            ID,
            Sidearm.class,
            Sidearm::new
    ).registerAction("execute", Sidearm::handleCast);

    public Sidearm(SmpPlayer player, int level) {
        super(player, level);
        this.damage = getAbilityInfo().getAttributeForLevel("damage", level);
        this.cooldown = (int) getAbilityInfo().getAttributeForLevel("cooldown", level);
        setMaxCharges((int) getAbilityInfo().getAttributeForLevel("max_stacks", level), cooldown);
    }

    public AbilityResponse handleCast() {
        if (isOnCooldown()) {
            return AbilityResponse.continueChain();
        }

        Player p = smpPlayer.getBukkitPlayer();
        Location eye = p.getEyeLocation();
        Vector direction = eye.getDirection();
        double range = 25.0; // Consistent range

        // 1. Raytrace
        RayTraceResult result = p.getWorld().rayTrace(eye, direction, range,
                FluidCollisionMode.NEVER, true, 0.75, (e) -> !(e instanceof Player) && e instanceof LivingEntity);

        Location endPoint = result != null
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

        setCooldownTick(cooldown);
        updateActionBar(p);

        return AbilityResponse.consume();
    }

    private void renderTracer(Location start, Location end) {
        double dist = start.distance(end);
        Vector dir = end.toVector().subtract(start.toVector()).normalize();

        // Z-aligned LineShape, pointed via the location clone's direction (matches the standard Matrix)
        FxShape lineShape = new LineShape(dist, 0.3);
        ParticleBuilder steel = new ParticleBuilder(Particle.CRIT).count(0).offset(0.05, 0.05, 0.05);
        FxPart tracer = new FxPart(lineShape, new ParticleRenderer(steel));

        FxEngine.getInstance().play(FxEffect.builder(start.clone().setDirection(dir)).part(tracer).build());
    }

    private void playMuzzleEffects(Location eye) {
        World world = eye.getWorld();
        world.playSound(eye, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1f, charges == 1 ? 0.8f : 0.6f);
        world.playSound(eye, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 1f, 2f);
        world.playSound(eye, Sound.BLOCK_NOTE_BLOCK_SNARE, SoundCategory.PLAYERS, 1f, charges == 1 ? 1.5f : 0.5f);
        world.playSound(eye, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, SoundCategory.PLAYERS, 1f, charges == 1 ? 1.5f : 0.5f);
        world.playSound(eye, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.PLAYERS, 1f, 2f);

        // Muzzle smoke
        Location muzzle = eye.clone().add(eye.getDirection().multiply(0.5));
        FxPart smoke = new FxPart(new PointShape(), new ParticleRenderer(new ParticleBuilder(Particle.SMOKE).count(3).offset(0.05, 0.05, 0.05).extra(0.02)));
        FxEngine.getInstance().play(FxEffect.builder(muzzle).part(smoke).build());
    }

    private void playImpactEffects(Location loc) {
        World world = loc.getWorld();
        world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS,1f, 0f);
        world.playSound(loc, Sound.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.3f, 1.1f);

        // "Steel" impact sparks
        FxPart firework = new FxPart(new PointShape(), new ParticleRenderer(new ParticleBuilder(Particle.FIREWORK).count(3).offset(0.1, 0.1, 0.1).extra(0.05)));
        FxPart crit = new FxPart(new PointShape(), new ParticleRenderer(new ParticleBuilder(Particle.CRIT).count(5).offset(0.1, 0.1, 0.1).extra(0.1)));
        FxEngine.getInstance().play(FxEffect.builder(loc).part(firework, crit).build());
    }

    private void updateActionBar(Player p) {
        p.sendActionBar(Component.text("Sidearm: ", NamedTextColor.GRAY)
                .append(Component.text("▮".repeat(charges), NamedTextColor.YELLOW))
                .append(Component.text("▯".repeat(maxCharges - charges), NamedTextColor.DARK_GRAY)));
    }

    @Override public @NotNull AbilityInfo<?> getAbilityInfo() { return INFO; }
}
