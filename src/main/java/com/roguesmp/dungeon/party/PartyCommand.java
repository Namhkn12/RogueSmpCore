package com.roguesmp.dungeon.party;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerProfileArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerProfile;

public class PartyCommand {

    private final PartyManager partyManager;
    private final PartyInviteManager inviteManager;

    public PartyCommand(PartyManager partyManager, PartyInviteManager inviteManager) {
        this.partyManager = partyManager;
        this.inviteManager = inviteManager;
    }

    public void register() {

        new CommandAPICommand("party")

                .withSubcommand(
                        new CommandAPICommand("create")
                                .executesPlayer((player, args) -> {
                                    partyManager.createParty(player);
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("disband")
                                .executesPlayer((player, args) -> {
                                    partyManager.disbandParty(player);
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

                                    inviteManager.sendInvite(player, target);
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("accept")
                                .withArguments(new StringArgument("player"))
                                .executesPlayer((player, args) -> {
                                    String inviterName = (String) args.get("player");
                                    inviteManager.acceptInvite(player, inviterName);
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("deny")
                                .withArguments(new StringArgument("player"))
                                .executesPlayer((player, args) -> {
                                    String inviterName = (String) args.get("player");
                                    inviteManager.denyInvite(player, inviterName);
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("leave")
                                .executesPlayer((player, args) -> {
                                    partyManager.leaveParty(player);
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("info")
                                .executesPlayer((player, args) -> {
                                    player.sendMessage(partyManager.getPartyInfo(player));
                                })
                )

                .register();
    }
}