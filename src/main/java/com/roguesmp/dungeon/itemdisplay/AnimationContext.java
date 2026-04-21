package com.roguesmp.dungeon.itemdisplay;

import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;

/**
 * Context được inject vào mọi callback.
 * Chứa đủ thông tin để callback tự quyết định làm gì.
 */
public class AnimationContext {

    private final Location location;
    private final int currentTick;
    private final int totalDuration;
    private final ItemDisplay display;
    private final Runnable cancelFn;

    public AnimationContext(
            Location location,
            int currentTick,
            int totalDuration,
            ItemDisplay display,
            Runnable cancelFn
    ) {
        this.location = location;
        this.currentTick = currentTick;
        this.totalDuration = totalDuration;
        this.display = display;
        this.cancelFn = cancelFn;
    }

    public Location getLocation()    { return location; }
    public int getTick()             { return currentTick; }
    public int getDuration()         { return totalDuration; }
    public ItemDisplay getDisplay()  { return display; }

    /** Gọi từ trong callback để dừng animation sớm → trigger onEnd */
    public void cancel()             { cancelFn.run(); }

    /** Tiện dụng: tính % tiến độ (0.0 → 1.0) */
    public float progress()          { return (float) currentTick / totalDuration; }
}
