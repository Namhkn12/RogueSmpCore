package com.roguesmp.billboard;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record DisplaySettings(@Nullable Display.Billboard billboard,
                              @Nullable Float scale,
                              @Nullable Boolean shadow,
                              @Nullable Boolean seeThrough,
                              @Nullable Color background,
                              @Nullable TextDisplay.TextAlignment alignment,
                              @Nullable Integer lineWidth) {

    public static final DisplaySettings EMPTY = new DisplaySettings(null, null, null, null, null, null, null);
    public static final DisplaySettings DEFAULT = new DisplaySettings(Display.Billboard.CENTER, 1f, false, false, null, TextDisplay.TextAlignment.CENTER, 200);

    private static final Codec<Color> ARGB = Codec.STRING.comapFlatMap(
            hex -> {
                try {
                    String digits = hex.replace("#", "");
                    int value = (int) Long.parseLong(digits, 16);
                    return DataResult.success(digits.length() <= 6 ? Color.fromRGB(value) : Color.fromARGB(value));
                } catch (IllegalArgumentException e) {
                    return DataResult.error("Invalid ARGB color: " + hex);
                }
            },
            color -> String.format("#%08X", color.asARGB())
    );

    public static final Codec<DisplaySettings> CODEC = Codec.composite(
            Codec.enumOf(Display.Billboard.class).optionalFieldOf("billboard").forGetter(s -> Optional.ofNullable(s.billboard)),
            Codec.FLOAT.optionalFieldOf("scale").forGetter(s -> Optional.ofNullable(s.scale)),
            Codec.BOOLEAN.optionalFieldOf("shadow").forGetter(s -> Optional.ofNullable(s.shadow)),
            Codec.BOOLEAN.optionalFieldOf("see_through").forGetter(s -> Optional.ofNullable(s.seeThrough)),
            ARGB.optionalFieldOf("background").forGetter(s -> Optional.ofNullable(s.background)),
            Codec.enumOf(TextDisplay.TextAlignment.class).optionalFieldOf("alignment").forGetter(s -> Optional.ofNullable(s.alignment)),
            Codec.INT.optionalFieldOf("line_width").forGetter(s -> Optional.ofNullable(s.lineWidth)),
            (billboard, scale, shadow, seeThrough, background, alignment, lineWidth) -> new DisplaySettings(
                    billboard.orElse(null), scale.orElse(null), shadow.orElse(null), seeThrough.orElse(null),
                    background.orElse(null), alignment.orElse(null), lineWidth.orElse(null))
    );

    public DisplaySettings merge(DisplaySettings parent) {
        return new DisplaySettings(
                billboard != null ? billboard : parent.billboard,
                scale != null ? scale : parent.scale,
                shadow != null ? shadow : parent.shadow,
                seeThrough != null ? seeThrough : parent.seeThrough,
                background != null ? background : parent.background,
                alignment != null ? alignment : parent.alignment,
                lineWidth != null ? lineWidth : parent.lineWidth
        );
    }

    public float scaleOrDefault() {
        return scale != null ? scale : 1f;
    }

    public void apply(TextDisplay display) {
        if (billboard != null) display.setBillboard(billboard);
        if (shadow != null) display.setShadowed(shadow);
        if (seeThrough != null) display.setSeeThrough(seeThrough);
        if (background != null) display.setBackgroundColor(background);
        if (alignment != null) display.setAlignment(alignment);
        if (lineWidth != null) display.setLineWidth(lineWidth);
        if (scale != null) {
            Transformation current = display.getTransformation();
            display.setTransformation(new Transformation(current.getTranslation(), current.getLeftRotation(),
                    new Vector3f(scale, scale, scale), current.getRightRotation()));
        }
    }
}
