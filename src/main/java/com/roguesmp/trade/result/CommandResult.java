package com.roguesmp.trade.result;

import com.roguesmp.codec.Codec;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CommandResult implements TradeResult {

    public static final String TYPE_KEY = "command";
    public static final Codec<CommandResult> CODEC = Codec.composite(
            Codec.STRING.fieldOf("command").forGetter(CommandResult::getCommand),
            Codec.STRING.optionalFieldOf("display", "").forGetter(CommandResult::getDisplayText),
            CommandResult::new
    );

    private final String command;
    private final String displayText;

    public CommandResult(String command, String displayText) {
        this.command = command;
        this.displayText = displayText;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    public String getCommand() {
        return command;
    }

    public String getDisplayText() {
        return displayText;
    }

    @Override
    public void give(SmpPlayer player) {
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null) return;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command
                .replace("%player%", bukkitPlayer.getName())
                .replace("%uuid%", bukkitPlayer.getUniqueId().toString()));
    }

    @Override
    public Component getDisplay(SmpPlayer player) {
        return Utils.fromString(displayText);
    }
}
