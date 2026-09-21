package com.roguesmp.effect.impl;

import com.destroystokyo.paper.ParticleBuilder;
import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageType;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.fx.FxEffect;
import com.roguesmp.fx.FxEngine;
import com.roguesmp.fx.FxHandle;
import com.roguesmp.fx.FxPart;
import com.roguesmp.fx.render.ParticleRenderer;
import com.roguesmp.fx.shape.FxShape;
import com.roguesmp.fx.shape.PointShape;
import com.roguesmp.utils.LocationUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

//Transient
public class ArmorBreakerEffect extends SmpEffect {

    public static final String ID = "armor_breaker";

    private final int fullDuration;
    private final double bonusPerStack;
    private final int maxStacks;
    private final Entity applier;
    private int stacks;
    private FxHandle handle;

    public ArmorBreakerEffect(int duration, double bonusPerStack, int maxStack, Entity applier) {
        super(ID, duration);
        this.fullDuration = duration;
        this.bonusPerStack = bonusPerStack;
        this.maxStacks = maxStack;
        this.applier = applier;
    }

    public void addStack() {
        stacks = Math.min(stacks + 1, maxStacks);
        setDuration(fullDuration);
    }

    @Override
    public double getMagnitude() {
        return bonusPerStack;
    }

    @Override
    public void onGainEffect(Entity entity) {
        FxShape shape = new PointShape();
        FxPart part = new FxPart(shape, new ParticleRenderer(new ParticleBuilder(Particle.ENCHANTED_HIT).count(8).extra(0.6))).activeTicks(value -> value % 10 == 0);
        FxEffect.Builder effectBuilder = FxEffect.builder(LocationUtils.getHalfHeightLocation(entity)).part(part).duration(-1);
        if (applier instanceof Player player) {
            effectBuilder.viewer(player);
        }
        handle = FxEngine.getInstance().play(effectBuilder.build());
    }

    @Override
    public void onLoseEffect(Entity entity) {
        if (handle != null) handle.stop();
    }

    @Override
    public void onHurt(DamageEvent event) {
        if (!DamageType.isMeleeDamage(event.getDamageType())) return;
        if (event.getDamager() != null && event.getDamager().getUniqueId().equals(applier.getUniqueId())) {
            event.addDamageModifier(bonusPerStack * stacks, DamageOperation.INCREASE_BASE);
        }
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public @Nullable Component getDisplayComponent() {
        return Component.text("Phá giáp (" + stacks + " stack)", NamedTextColor.RED);
    }
}
