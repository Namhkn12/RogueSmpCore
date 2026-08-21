package com.roguesmp.effect.impl;

import com.destroystokyo.paper.ParticleBuilder;
import com.roguesmp.codec.Codec;
import com.roguesmp.constant.DamageType;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.fx.FxEffect;
import com.roguesmp.fx.FxEngine;
import com.roguesmp.fx.FxPart;
import com.roguesmp.fx.motion.GravityMotion;
import com.roguesmp.fx.render.ParticleRenderer;
import com.roguesmp.fx.shape.PointShape;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A damage-over-time debuff - ticks {@link #damagePerSecond} once a second (via {@code oneHz}) as
 * {@link DamageType#AILMENT} damage, and spawns a little burst of falling red/
 * dark-red particles at the entity's location every {@code onTick} call for the "bleeding" visual,
 * played through {@link FxEngine} (fx is presentation-only - the damage and the visual are two
 * separate calls, not one).
 */
public class BleedingEffect extends SmpEffect {

    public static final String ID = "bleeding";

    public static final Codec<BleedingEffect> CODEC = Codec.composite(
            SmpEffect.BASE_CODEC.forGetter(SmpEffect::getBaseProperties),
            Codec.DOUBLE.fieldOf("damage_per_second").forGetter(BleedingEffect::getDamagePerSecond),
            BleedingEffect::new
    );

    private static final int BURST_DURATION_TICKS = 12;
    private static final int DROPLETS_PER_BURST = 3;
    private static final Particle.DustOptions RED = new Particle.DustOptions(Color.RED, 0.9f);
    private static final Particle.DustOptions DARK_RED = new Particle.DustOptions(Color.fromRGB(139, 0, 0), 0.9f);

    private final double damagePerSecond;
    private final @Nullable Entity applier;

    public BleedingEffect(int duration, double damagePerSecond, @Nullable Entity applier) {
        super(ID, duration);
        this.damagePerSecond = damagePerSecond;
        this.applier = applier;
    }

    public BleedingEffect(BaseProperties base, double damagePerSecond) {
        super(ID, base);
        this.damagePerSecond = damagePerSecond;
        this.applier = null;
    }

    public BleedingEffect(BaseProperties base, double damagePerSecond, @Nullable Entity applier) {
        super(ID, base);
        this.damagePerSecond = damagePerSecond;
        this.applier = applier;
    }

    @Override
    public double getMagnitude() {
        return damagePerSecond;
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public @Nullable Component getDisplayComponent() {
        return Component.text("Chảy máu (-" + Utils.formatDecimal(damagePerSecond) + "/s)", NamedTextColor.DARK_RED);
    }

    @Override
    public void onTick(Entity entity, boolean oneHz, boolean twoHz) {
        if (!(entity instanceof LivingEntity livingEntity) || !livingEntity.isValid()) return;

        spawnBloodBurst(livingEntity.getLocation().add(0, livingEntity.getHeight() * 0.6, 0));

        if (oneHz) {
            DamageEvent.Metadata metadata = new DamageEvent.Metadata(DamageType.AILMENT);
            metadata.setIgnoreIframe(true);
            metadata.setDoKnockback(false);
            DamageUtils.damage(livingEntity, applier, damagePerSecond, metadata);
        }
    }

    public double getDamagePerSecond() {
        return damagePerSecond;
    }

    private static void spawnBloodBurst(Location origin) {
        FxEffect.Builder builder = FxEffect.builder(origin).duration(BURST_DURATION_TICKS);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < DROPLETS_PER_BURST; i++) {
            builder.part(droplet(random));
        }
        FxEngine.getInstance().play(builder.build());
    }

    private static FxPart droplet(ThreadLocalRandom random) {
        Particle.DustOptions color = random.nextBoolean() ? RED : DARK_RED;
        ParticleRenderer renderer = new ParticleRenderer(new ParticleBuilder(Particle.DUST).data(color).count(1));

        Vector3f velocity = new Vector3f(
                (float) random.nextDouble(-0.05, 0.05),
                (float) random.nextDouble(0.05, 0.15),
                (float) random.nextDouble(-0.05, 0.05)
        );
        return new FxPart(new PointShape(), renderer).motion(new GravityMotion(velocity, 0.02f));
    }

    public static CommandAPICommand registerCommand() {
        return new CommandAPICommand("bleeding")
                .withArguments(new IntegerArgument("duration"), new DoubleArgument("damagePerSecond"), new StringArgument("source"))
                .executesPlayer((player, args) -> {
                    int duration = (Integer) args.get("duration");
                    double damagePerSecond = (Double) args.get("damagePerSecond");
                    String source = (String) args.get("source");

                    EffectManager.getInstance().addEffect(player, source, new BleedingEffect(duration, damagePerSecond, player));
                });
    }
}
