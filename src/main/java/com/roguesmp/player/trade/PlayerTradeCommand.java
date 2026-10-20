package com.roguesmp.player.trade;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlayerTradeCommand {

    private final PlayerTradeManager tradeManager;

    public PlayerTradeCommand(PlayerTradeManager tradeManager) {
        this.tradeManager = tradeManager;
    }

    public void register() {
        new CommandAPICommand("trade")
                .withSubcommand(new CommandAPICommand("request")
                        .withArguments(new EntitySelectorArgument.OnePlayer("target"))
                        .executesPlayer((player, args) -> {
                            tradeManager.sendRequest(player, (Player) args.get("target"));
                        }))
                .withSubcommand(new CommandAPICommand("accept")
                        .withArguments(new StringArgument("player"))
                        .executesPlayer((player, args) -> {
                            tradeManager.acceptRequest(player, Bukkit.getPlayerExact((String) args.get("player")));
                        }))
                .withSubcommand(new CommandAPICommand("deny")
                        .withArguments(new StringArgument("player"))
                        .executesPlayer((player, args) -> {
                            tradeManager.denyRequest(player, Bukkit.getPlayerExact((String) args.get("player")));
                        }))
                .register();
    }
}
