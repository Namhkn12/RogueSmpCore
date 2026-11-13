package com.roguesmp.billboard.line;

import com.roguesmp.billboard.DisplaySettings;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.gate.requirement.Requirement;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public record AnimatedLine(List<BillboardLine> frames, int interval, DisplaySettings display,
                           @Nullable Requirement condition) implements BillboardLine {

    public static final String TYPE_KEY = "animated";

    public static final Codec<AnimatedLine> CODEC = Codec.composite(
            Codec.listOf(BillboardLine.CODEC).fieldOf("frames").forGetter(AnimatedLine::frames),
            Codec.INT.optionalFieldOf("interval", 20).forGetter(AnimatedLine::interval),
            DisplaySettings.CODEC.optionalFieldOf("display", DisplaySettings.EMPTY).forGetter(AnimatedLine::display),
            Requirement.CODEC.optionalFieldOf("condition").forGetter(line -> Optional.ofNullable(line.condition)),
            (frames, interval, display, condition) -> new AnimatedLine(frames, interval, display, condition.orElse(null))
    ).flatXmap(AnimatedLine::validate, DataResult::success);

    private static DataResult<AnimatedLine> validate(AnimatedLine line) {
        if (line.frames.isEmpty()) return DataResult.error("Animated line needs at least one frame");
        if (line.interval <= 0) return DataResult.error("Animated line interval must be positive");
        return DataResult.success(line);
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public Component render(Player viewer, long tick) {
        return frames.get((int) ((tick / interval) % frames.size())).render(viewer, tick);
    }
}
