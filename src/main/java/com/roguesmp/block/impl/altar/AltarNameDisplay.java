package com.roguesmp.block.impl.altar;

import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

final class AltarNameDisplay {

    private static final double HEIGHT_ABOVE_ITEM = 0.6;
    private static final int VISIBLE_TICKS = 80;
    private static final int POP_TICKS = 10;

    private final TextDisplay display;
    private int ticks;

    AltarNameDisplay(Location hover, Component name) {
        display = hover.getWorld().spawn(hover.clone().add(0, HEIGHT_ABOVE_ITEM, 0), TextDisplay.class, spawned -> {
            spawned.text(name);
            spawned.setBillboard(Display.Billboard.CENTER);
            spawned.setDefaultBackground(false);
            spawned.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            spawned.setShadowed(true);
            spawned.setPersistent(false);
            spawned.setInvulnerable(true);
            spawned.setTransformation(transformation(0f));
        });
    }

    boolean tick() {
        if (!display.isValid()) return true;

        ticks++;
        if (ticks == 1) animate(1f);
        if (ticks == VISIBLE_TICKS - POP_TICKS) animate(0f);
        if (ticks < VISIBLE_TICKS) return false;

        remove();
        return true;
    }

    void remove() {
        display.remove();
    }

    private void animate(float scale) {
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(POP_TICKS);
        display.setTransformation(transformation(scale));
    }

    private static Transformation transformation(float scale) {
        return new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(scale), new AxisAngle4f());
    }
}
