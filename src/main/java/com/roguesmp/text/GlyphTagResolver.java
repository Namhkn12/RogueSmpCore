package com.roguesmp.text;

import com.roguesmp.registry.Registries;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

public final class GlyphTagResolver implements TagResolver {

    @Override
    public boolean has(@NotNull String name) {
        return name.equalsIgnoreCase("glyph");
    }

    @Override
    public Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull Context context) {
        if (!name.equalsIgnoreCase("glyph")) return null;
        if (!arguments.hasNext()) {
            return null;
        }

        String glyphName = arguments.pop().value().toLowerCase();
        Glyph glyph = Registries.GLYPH.get(glyphName);
        if (glyph == null) {
            return null;
        }

        if (!arguments.hasNext()) {
            return Tag.selfClosingInserting(glyph.create());
        }

        String operation = arguments.pop().value();

        return switch (operation) {
            case "repeat" -> {
                if (!arguments.hasNext()) {
                    yield null;
                }

                int count = arguments.pop().asInt().orElseThrow(() -> context.newException("Expected an integer"));

                yield Tag.selfClosingInserting(glyph.repeat(count));
            }

            case "offset" -> {
                if (!arguments.hasNext()) {
                    yield null;
                }

                int pixels = arguments.pop().asInt().orElseThrow(() -> context.newException("Expected an integer"));

                yield Tag.selfClosingInserting(createOffset(glyph, pixels));
            }

            default -> null;
        };
    }

    private static Component createOffset(Glyph glyph, int pixels) {
        return Component.empty()
                .append(Space.px(-pixels))
                .append(glyph.create())
                .append(Space.px(pixels));
    }
}
