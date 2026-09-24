package com.roguesmp.text;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;

import java.io.File;
import java.io.IOException;

public final class GlyphFontCommand {

    private static final String OUTPUT_FOLDER = "generated";

    private GlyphFontCommand() {
    }

    public static void register() {
        new CommandAPICommand("generateglyphs")
                .withPermission(CommandPermission.OP)
                .executes((sender, args) -> {
                    File output = new File(RogueSmpCore.getInstance().getDataFolder(), OUTPUT_FOLDER);

                    try {
                        GlyphFontGenerator.Result result = new GlyphFontGenerator(Registries.GLYPH).generate(output);

                        sender.sendMessage(Utils.fromString("<green>Generated " + result.files().size() + " font file(s) in " + output.getPath()));
                        result.warnings().forEach(warning -> sender.sendMessage(Utils.fromString("<yellow>" + warning)));
                    } catch (IllegalStateException | IOException e) {
                        sender.sendMessage(Utils.fromString("<red>Glyph font generation failed: " + e.getMessage()));
                    }
                })
                .register();
    }
}
