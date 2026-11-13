package com.roguesmp.billboard;

import com.roguesmp.action.ActionEntry;
import com.roguesmp.billboard.line.BillboardLine;
import com.roguesmp.codec.Codec;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public record Billboard(double viewRange, int refresh, double lineSpacing,
                        DisplaySettings display, List<BillboardLine> lines,
                        @Nullable Hitbox hitbox, List<ActionEntry> actions) {

    public static final Codec<Billboard> CODEC = Codec.composite(
            Codec.DOUBLE.optionalFieldOf("view_range", 48.0).forGetter(Billboard::viewRange),
            Codec.INT.optionalFieldOf("refresh", 10).forGetter(Billboard::refresh),
            Codec.DOUBLE.optionalFieldOf("line_spacing", 0.05).forGetter(Billboard::lineSpacing),
            DisplaySettings.CODEC.optionalFieldOf("display", DisplaySettings.EMPTY).forGetter(Billboard::display),
            Codec.listOf(BillboardLine.CODEC).fieldOf("lines").forGetter(Billboard::lines),
            Hitbox.CODEC.optionalFieldOf("hitbox").forGetter(billboard -> Optional.ofNullable(billboard.hitbox)),
            Codec.listOf(ActionEntry.CODEC).optionalFieldOf("actions", List.of()).forGetter(Billboard::actions),
            (viewRange, refresh, lineSpacing, display, lines, hitbox, actions) ->
                    new Billboard(viewRange, refresh, lineSpacing, display, lines, hitbox.orElse(null), actions)
    );

    public record Hitbox(float width, float height) {

        public static final Codec<Hitbox> CODEC = Codec.composite(
                Codec.FLOAT.fieldOf("width").forGetter(Hitbox::width),
                Codec.FLOAT.fieldOf("height").forGetter(Hitbox::height),
                Hitbox::new
        );
    }

    public static Billboard of(List<BillboardLine> lines) {
        return new Billboard(48.0, 10, 0.05, DisplaySettings.EMPTY, lines, null, List.of());
    }

    public boolean isClickable() {
        return hitbox != null || !actions.isEmpty();
    }

    public Billboard withViewRange(double viewRange) {
        return new Billboard(viewRange, refresh, lineSpacing, display, lines, hitbox, actions);
    }

    public Billboard withRefresh(int refresh) {
        return new Billboard(viewRange, refresh, lineSpacing, display, lines, hitbox, actions);
    }

    public Billboard withLineSpacing(double lineSpacing) {
        return new Billboard(viewRange, refresh, lineSpacing, display, lines, hitbox, actions);
    }

    public Billboard withDisplay(DisplaySettings display) {
        return new Billboard(viewRange, refresh, lineSpacing, display, lines, hitbox, actions);
    }

    public Billboard withLines(List<BillboardLine> lines) {
        return new Billboard(viewRange, refresh, lineSpacing, display, List.copyOf(lines), hitbox, actions);
    }

    public Billboard withHitbox(@Nullable Hitbox hitbox) {
        return new Billboard(viewRange, refresh, lineSpacing, display, lines, hitbox, actions);
    }

    public Billboard withActions(List<ActionEntry> actions) {
        return new Billboard(viewRange, refresh, lineSpacing, display, lines, hitbox, List.copyOf(actions));
    }
}
