package com.roguesmp.block.impl.altar;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

final class AltarItemDisplay {

    static final double HOVER_HEIGHT = 1.35;
    static final float ITEM_SCALE = 0.55f;
    static final float SPIN_STEP = (float) (2 * Math.PI / 3);

    private static final float BOB_AMPLITUDE = 0.12f;
    private static final int IDLE_STEP_TICKS = 20;
    private static final int PARTICLE_INTERVAL = 6;

    private @Nullable ItemDisplay display;
    private int ticks;
    private int step;

    void refresh(AltarBlock altar) {
        ItemStack item = altar.getItem();
        if (item == null) {
            remove();
            return;
        }

        if (display == null || !display.isValid()) display = spawn(altar.getHoverLocation());
        ItemStack shown = item.clone();
        shown.setAmount(1);
        display.setItemStack(shown);
    }

    void tick(AltarBlock altar) {
        if (altar.getItem() == null) return;
        if (display == null || !display.isValid()) refresh(altar);

        ticks++;
        if (ticks % IDLE_STEP_TICKS == 0) idleStep();
        if (ticks % PARTICLE_INTERVAL == 0) spawnIdleParticles(altar.getHoverLocation());
    }

    void animate(Vector3f translation, float angle, float scale, int duration) {
        if (display == null || !display.isValid()) return;

        display.setInterpolationDelay(0);
        display.setInterpolationDuration(duration);
        display.setTransformation(transformation(translation, angle, scale));
    }

    void settle() {
        animate(new Vector3f(), 0f, ITEM_SCALE, 0);
        step = 0;
    }

    void remove() {
        if (display == null) return;

        display.remove();
        display = null;
    }

    private void idleStep() {
        step++;
        float bob = step % 2 == 0 ? -BOB_AMPLITUDE : BOB_AMPLITUDE;
        animate(new Vector3f(0, bob, 0), step * SPIN_STEP, ITEM_SCALE, IDLE_STEP_TICKS);
    }

    private void spawnIdleParticles(Location hover) {
        hover.getWorld().spawnParticle(Particle.END_ROD, hover, 1, 0.2, 0.2, 0.2, 0.01);
        hover.getWorld().spawnParticle(Particle.ENCHANT, hover, 3, 0.4, 0.3, 0.4, 0.3);
    }

    private ItemDisplay spawn(Location hover) {
        return hover.getWorld().spawn(hover, ItemDisplay.class, spawned -> {
            spawned.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            spawned.setTransformation(transformation(new Vector3f(), 0f, ITEM_SCALE));
            spawned.setPersistent(false);
            spawned.setInvulnerable(true);
        });
    }

    private static Transformation transformation(Vector3f translation, float angle, float scale) {
        return new Transformation(translation, new AxisAngle4f(angle, 0, 1, 0), new Vector3f(scale), new AxisAngle4f());
    }
}
