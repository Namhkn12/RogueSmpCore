package com.roguesmp.billboard.line;

import com.roguesmp.billboard.DisplaySettings;
import com.roguesmp.codec.Codec;
import com.roguesmp.gate.requirement.Requirement;
import com.roguesmp.utils.Utils;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record TextLine(String text, DisplaySettings display, @Nullable Requirement condition,
                       @Nullable Component staticComponent) implements BillboardLine {

    public static final String TYPE_KEY = "text";

    public static final Codec<TextLine> CODEC = Codec.composite(
            Codec.STRING.fieldOf("text").forGetter(TextLine::text),
            DisplaySettings.CODEC.optionalFieldOf("display", DisplaySettings.EMPTY).forGetter(TextLine::display),
            Requirement.CODEC.optionalFieldOf("condition").forGetter(line -> Optional.ofNullable(line.condition)),
            (text, display, condition) -> new TextLine(text, display, condition.orElse(null))
    );

    public TextLine(String text, DisplaySettings display, @Nullable Requirement condition) {
        this(text, display, condition, text.indexOf('%') < 0 ? Utils.fromString(text) : null);
    }

    public static TextLine of(String text) {
        return new TextLine(text, DisplaySettings.EMPTY, null);
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public Component render(Player viewer, long tick) {
        return staticComponent != null ? staticComponent : Utils.fromString(PlaceholderAPI.setPlaceholders(viewer, text));
    }
}
