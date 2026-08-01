package com.roguesmp.utils;

import com.roguesmp.constant.DamageType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class DamageDisplayUtils {

    private static final float BASE_SCALE = 1f;
    private static final float POP_START_SCALE = BASE_SCALE * 0.4f;
    private static final float PEAK_SCALE_MULTIPLIER = 1.35f;
    private static final float END_SCALE = 0.01f;
    private static final float CRIT_SCALE_MULTIPLIER = 1.3f;
    private static final float RISE_MID_HEIGHT = 0.4f;
    private static final float RISE_HEIGHT = 0.9f;
    private static final float DRIFT_RANGE = 0.25f;

    private static final int POP_TICKS = 3;
    private static final int GROW_TICKS = 6;
    private static final int SHRINK_TICKS = 8;
    private static final int REMOVE_TICKS = POP_TICKS + GROW_TICKS + SHRINK_TICKS + 1;

    public static void spawnDamageDisplay(Location hitLocation, double damage, DamageType type, boolean isCritical) {
        Component text = formatDamage(damage, type, isCritical);
        float targetScale = isCritical ? BASE_SCALE * CRIT_SCALE_MULTIPLIER : BASE_SCALE;
        float peakScale = targetScale * PEAK_SCALE_MULTIPLIER;

        TextDisplay display = hitLocation.getWorld().spawn(hitLocation, TextDisplay.class, entity -> {
            entity.setPersistent(false);
            entity.text(text);
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setBackgroundColor(Color.fromARGB(50, 0, 0, 0));
            entity.setShadowed(true);
            entity.setTransformation(scaled(new Vector3f(), POP_START_SCALE));
        });

        // Pop in from a small scale to full size.
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(POP_TICKS);
        display.setTransformation(scaled(new Vector3f(), targetScale));

        float driftX = (float) (Utils.RANDOM.nextDouble() - 0.5) * DRIFT_RANGE;
        float driftZ = (float) (Utils.RANDOM.nextDouble() - 0.5) * DRIFT_RANGE;
        Vector3f midOffset = new Vector3f(driftX, RISE_MID_HEIGHT, driftZ);
        Vector3f endOffset = new Vector3f(driftX, RISE_HEIGHT, driftZ);

        // Grow past full size while rising to the midpoint of the float.
        Utils.runLater(() -> {
            if (!display.isValid()) return;
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(GROW_TICKS);
            display.setTransformation(scaled(midOffset, peakScale));
        }, POP_TICKS);

        // Shrink back down while finishing the rise, right up until despawn.
        Utils.runLater(() -> {
            if (!display.isValid()) return;
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(SHRINK_TICKS);
            display.setTransformation(scaled(endOffset, END_SCALE));
        }, POP_TICKS + GROW_TICKS);

        Utils.runLater(display::remove, REMOVE_TICKS);
    }

    private static Transformation scaled(Vector3f translation, float scale) {
        return new Transformation(translation, new Quaternionf(), new Vector3f(scale), new Quaternionf());
    }

    private static Component formatDamage(double damage, DamageType type, boolean isCritical) {
        String val = Utils.formatDecimal(damage);

        TextColor color = switch (type) {
            case MELEE, MELEE_ABILITY -> NamedTextColor.WHITE;
            case PROJECTILE, PROJECTILE_ABILITY -> NamedTextColor.YELLOW;
            case MAGIC -> NamedTextColor.LIGHT_PURPLE;
            case FIRE -> NamedTextColor.GOLD;
            case BLAST -> NamedTextColor.RED;
            case AILMENT -> NamedTextColor.GREEN;
            case TRUE -> NamedTextColor.AQUA;
            case FALL, THORNS -> NamedTextColor.GRAY;
            default -> NamedTextColor.DARK_GRAY;
        };

        String prefix = switch (type) {
            case MELEE -> "⚔ ";
            case PROJECTILE -> "🏹 ";
            case MELEE_ABILITY, PROJECTILE_ABILITY -> "★ ";
            case MAGIC -> "✦ ";
            case AILMENT -> "☣ ";
            case FIRE -> "🔥 ";
            case TRUE -> "⚡ ";
            default -> "";
        };

        // Crit is indicated by scale (see targetScale above) plus this marker, applied on top of
        // whatever type icon/color is already showing — kept separate so it doesn't crowd out the type.
        String suffix = isCritical ? " ‼" : "";

        return Component.text(prefix + val + suffix, color, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, type.name().contains("ABILITY"));
    }
}
