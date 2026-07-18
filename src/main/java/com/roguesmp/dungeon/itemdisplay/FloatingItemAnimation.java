package com.roguesmp.dungeon.itemdisplay;

import com.roguesmp.dungeon.task.TaskScheduler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class FloatingItemAnimation {

    private final Plugin plugin;
    private final TaskScheduler taskScheduler;
    private final Location location;
    private final int duration;
    private final float spinSpeed;
    private final List<ItemStack> items;
    private final int iconCycleInterval;
    private final float displayScale;
    private final Player viewer;

    private final Consumer<AnimationContext> onStart;
    private final Consumer<AnimationContext> onTick;
    private final Consumer<AnimationContext> onEnd;

    private FloatingItemAnimation(Builder builder) {
        this.plugin            = builder.plugin;
        this.taskScheduler     = builder.taskScheduler;
        this.location          = builder.location;
        this.duration          = builder.duration;
        this.spinSpeed         = builder.spinSpeed;
        this.items             = builder.items;
        this.iconCycleInterval = builder.iconCycleInterval;
        this.displayScale      = builder.displayScale;
        this.viewer            = builder.viewer;
        this.onStart           = builder.onStart;
        this.onTick            = builder.onTick;
        this.onEnd             = builder.onEnd;
    }

    /**
     * Bắt đầu animation.
     * @return AnimationHandle – dùng để cancel từ bên ngoài
     */
    public AnimationHandle play() {
        Location spawnLoc = location.clone().add(0, 1.5, 0);

        ItemStack firstItem = items.isEmpty()
                ? new ItemStack(Material.CHEST)
                : items.get(0);

        // Spawn ItemDisplay
        ItemDisplay display = location.getWorld().spawn(spawnLoc, ItemDisplay.class, d -> {
            d.setItemStack(firstItem);
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
            d.setBillboard(Display.Billboard.FIXED);
            d.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new Quaternionf(),
                    new Vector3f(displayScale, displayScale, displayScale),
                    new Quaternionf()
            ));
            d.setGlowing(true);
            if (viewer != null) d.setVisibleByDefault(false);
        });

        if (viewer != null) viewer.showEntity(plugin, display);

        AtomicBoolean cancelFlag = new AtomicBoolean(false);
        AtomicInteger tickRef = new AtomicInteger(0);
        AtomicReference<Float> angleRef = new AtomicReference<>(0f);
        AnimationHandle handle = new AnimationHandle(() -> cancelFlag.set(true));

        BukkitTask task = taskScheduler.runTimerCancellable(0L, 1L, () -> {
            if (cancelFlag.get()) {
                cleanup(display);
                return;
            }

            int tick = tickRef.get();
            AnimationContext ctx = new AnimationContext(
                    location, tick, duration, display, () -> cancelFlag.set(true)
            );

            if (tick == 0 && onStart != null) onStart.accept(ctx);

            // Cycle icon
            if (!items.isEmpty() && iconCycleInterval > 0 && tick % iconCycleInterval == 0) {
                int idx = (tick / iconCycleInterval) % items.size();
                display.setItemStack(items.get(idx));
            }

            // Spin
            float angle = (angleRef.get() + spinSpeed) % 360f;
            angleRef.set(angle);
            Quaternionf rot = new Quaternionf().rotateY((float) Math.toRadians(angle));
            Transformation t = display.getTransformation();
            display.setTransformation(new Transformation(
                    t.getTranslation(), rot, t.getScale(), t.getRightRotation()
            ));

            // onTick
            if (onTick != null) {
                onTick.accept(ctx);
                if (cancelFlag.get()) {
                    cleanup(display);
                    return;
                }
            }

            tickRef.incrementAndGet();

            // Natural end
            if (tickRef.get() >= duration) {
                AnimationContext endCtx = new AnimationContext(
                        location, tickRef.get(), duration, display, () -> {}
                );
                cleanup(display);
                if (onEnd != null) onEnd.accept(endCtx);
                cancelFlag.set(true);
            }
        });

        handle.attachTask(task);
        return handle;
    }

    private void cleanup(ItemDisplay display) {
        if (!display.isDead()) display.remove();
    }

    // ── Builder ───────────────────────────────────────────────────

    public static Builder builder(Plugin plugin, Location location, TaskScheduler taskScheduler) {
        return new Builder(plugin, taskScheduler, location);
    }

    public static class Builder {
        private final Plugin plugin;
        private final TaskScheduler taskScheduler;
        private final Location location;

        private int duration            = 60;
        private float spinSpeed         = 8f;
        private List<ItemStack> items   = new ArrayList<>();
        private int iconCycleInterval   = 5;
        private float displayScale      = 0.7f;
        private Player viewer;

        private Consumer<AnimationContext> onStart;
        private Consumer<AnimationContext> onTick;
        private Consumer<AnimationContext> onEnd;

        private Builder(Plugin plugin, TaskScheduler taskScheduler, Location location) {
            this.plugin         = plugin;
            this.taskScheduler  = taskScheduler;
            this.location       = location;
        }

        public Builder duration(int ticks)                     { this.duration = ticks;            return this; }
        public Builder spinSpeed(float degPerTick)             { this.spinSpeed = degPerTick;       return this; }
        public Builder items(List<ItemStack> items)            { this.items = items;                return this; }
        public Builder iconCycleInterval(int ticks)           { this.iconCycleInterval = ticks;    return this; }
        public Builder displayScale(float scale)               { this.displayScale = scale;         return this; }
        public Builder viewer(Player viewer)                   { this.viewer = viewer;              return this; }
        public Builder onStart(Consumer<AnimationContext> cb)  { this.onStart = cb;                 return this; }
        public Builder onTick(Consumer<AnimationContext> cb)   { this.onTick = cb;                  return this; }
        public Builder onEnd(Consumer<AnimationContext> cb)    { this.onEnd = cb;                   return this; }

        public FloatingItemAnimation build()                   { return new FloatingItemAnimation(this); }
    }
}
