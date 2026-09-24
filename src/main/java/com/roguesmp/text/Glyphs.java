package com.roguesmp.text;

import com.roguesmp.constant.Keys;
import com.roguesmp.registry.Registries;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

public final class Glyphs {

    private static final Key FONT = Keys.of("glyphs");

    private Glyphs() {
    }

    public static final Glyph SERVER_ICON = glyph("icon/server", '\uE000', 8, 7, 8);
    public static final Glyph COIN_ICON = glyph("icon/coin", '\uE001', 8, 7, 8);

    private static Glyph glyph(String name, int symbol, int height, int ascent, int width) {
        return glyph(name, symbol, Keys.GLOBAL_NAMESPACE + ":" + name + ".png", height, ascent, width);
    }

    private static Glyph glyph(String name, int symbol, String texture, int height, int ascent, int width) {
        Glyph glyph = new Glyph(FONT, symbol, texture, ascent, height, width);
        Registries.GLYPH.register(name, glyph);
        return glyph;
    }

    public static void bootstrap() {

    }

    public static void registerTestCommand() {
        new CommandAPICommand("glyph")
                .withArguments(new StringArgument("glyph_id"))
                .executesPlayer((sender, args) -> {
                    String glyphId = (String) args.get("glyph_id");
                    Glyph glyph = Registries.GLYPH.get(glyphId);
                    if (glyph == null) {
                        sender.sendMessage("No glyph found for " + glyphId);
                        return;
                    }
                    sender.sendMessage(Component.text("Prepend text").append(glyph.create()).append(Component.text("and additional text")));
                })
                .register();
    }
}
