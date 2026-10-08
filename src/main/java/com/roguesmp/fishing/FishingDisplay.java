package com.roguesmp.fishing;

import com.roguesmp.RogueSmpCore;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public final class FishingDisplay {

    private static final float SCALE = 0.9f;
    private static final float LAYER_DEPTH = 0.01f;
    private static final double MINIGAME_HEIGHT = 1.1;
    private static final double PROGRESS_HEIGHT = 0.85;

    private final FishHook hook;
    private final TextDisplay track;
    private final TextDisplay bar;
    private final TextDisplay indicator;
    private final TextDisplay progress;

    private Component barText = Component.empty();
    private Component indicatorText = Component.empty();
    private Component progressText = Component.empty();

    public FishingDisplay(Player viewer, FishHook hook) {
        this.hook = hook;
        this.track = spawn(viewer, hook.getLocation().add(0, MINIGAME_HEIGHT, 0), 0);
        this.bar = spawn(viewer, hook.getLocation().add(0, MINIGAME_HEIGHT, 0), LAYER_DEPTH);
        this.indicator = spawn(viewer, hook.getLocation().add(0, MINIGAME_HEIGHT, 0), LAYER_DEPTH * 2);
        this.progress = spawn(viewer, hook.getLocation().add(0, PROGRESS_HEIGHT, 0), 0);
        this.track.text(FishingMinigameRenderer.trackLayer());
    }

    public void update(FishingMinigame game) {
        follow(track, MINIGAME_HEIGHT);
        follow(bar, MINIGAME_HEIGHT);
        follow(indicator, MINIGAME_HEIGHT);
        follow(progress, PROGRESS_HEIGHT);

        Component nextBar = FishingMinigameRenderer.barLayer(game);
        if (!nextBar.equals(barText)) {
            barText = nextBar;
            bar.text(nextBar);
        }

        Component nextIndicator = FishingMinigameRenderer.indicatorLayer(game);
        if (!nextIndicator.equals(indicatorText)) {
            indicatorText = nextIndicator;
            indicator.text(nextIndicator);
        }

        Component nextProgress = FishingMinigameRenderer.progressBar(game);
        if (!nextProgress.equals(progressText)) {
            progressText = nextProgress;
            progress.text(nextProgress);
        }
    }

    public void remove() {
        removeLater(track);
        removeLater(bar);
        removeLater(indicator);
        removeLater(progress);
    }

    private void follow(TextDisplay display, double height) {
        Location target = hook.getLocation().add(0, height, 0);
        if (display.getLocation().distanceSquared(target) > 0.0001) {
            display.setTeleportDuration(1);
            display.teleport(target);
        }
    }

    private static TextDisplay spawn(Player viewer, Location location, float depth) {
        TextDisplay display = location.getWorld().spawn(location, TextDisplay.class, spawned -> {
            spawned.setVisibleByDefault(false);
            spawned.setBillboard(Display.Billboard.CENTER);
            spawned.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            spawned.setShadowed(false);
            spawned.setLineWidth(Integer.MAX_VALUE);
            spawned.setPersistent(false);
            spawned.setTransformation(new Transformation(
                    new Vector3f(0, 0, depth), new AxisAngle4f(), new Vector3f(SCALE), new AxisAngle4f()));
        });
        viewer.showEntity(RogueSmpCore.getInstance(), display);
        return display;
    }

    private static void removeLater(TextDisplay display) {
        display.getScheduler().run(RogueSmpCore.getInstance(), task -> display.remove(), null);
    }
}
