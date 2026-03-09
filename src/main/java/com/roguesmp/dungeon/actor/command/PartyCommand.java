package com.roguesmp.dungeon.actor.command;

import com.roguesmp.dungeon.controller.PartyController;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerProfileArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerProfile;

public class PartyCommand {

    private final PartyController partyController;

    public PartyCommand(PartyController partyController) {
        this.partyController = partyController;
    }

    public void register() {

        new CommandAPICommand("party")

                .withSubcommand(
                        new CommandAPICommand("create")
                                .executesPlayer((player, args) -> {
                                    partyController.createParty(player);
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("disband")
                                .executesPlayer((player, args) -> {
                                    partyController.disbandParty(player);
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("invite")
                                .withArguments(new PlayerProfileArgument("target"))
                                .executesPlayer((player, args) -> {
                                    PlayerProfile profile = (PlayerProfile) args.get("target");
                                    Player target = Bukkit.getPlayer(profile.getUniqueId());

                                    if (target == null) {
                                        player.sendMessage("§cPlayer is not online!");
                                        return;
                                    }

                                    partyController.invitePlayer(player, target);
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("accept")
                                .withArguments(new StringArgument("player"))
                                .executesPlayer((player, args) -> {
                                    String inviterName = (String) args.get("player");
                                    partyController.acceptInvite(player, inviterName);
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("deny")
                                .withArguments(new StringArgument("player"))
                                .executesPlayer((player, args) -> {
                                    String inviterName = (String) args.get("player");
                                    partyController.denyInvite(player, inviterName);
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("leave")
                                .executesPlayer((player, args) -> {
                                    partyController.leaveParty(player);
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("info")
                                .executesPlayer((player, args) -> {
                                    partyController.showPartyInfo(player);
                                })
                )

                .register();
    }
}