package com.roguesmp.party;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;

public class PartyCommand {

    private final PartyManager partyManager;

    public PartyCommand(PartyManager partyManager) {
        this.partyManager = partyManager;
    }

    public void register() {
        PartyInviteManager inviteManager = partyManager.getInviteManager();

        new CommandAPICommand("party")
                .withSubcommand(new CommandAPICommand("create")
                        .executesPlayer((player, args) -> {
                            partyManager.createParty(player);
                        }))
                .withSubcommand(new CommandAPICommand("disband")
                        .executesPlayer((player, args) -> {
                            partyManager.disbandParty(player);
                        }))
                .withSubcommand(new CommandAPICommand("invite")
                        .withArguments(new EntitySelectorArgument.OnePlayer("target"))
                        .executesPlayer((player, args) -> {
                            inviteManager.sendInvite(player, (Player) args.get("target"));
                        }))
                .withSubcommand(new CommandAPICommand("accept")
                        .withArguments(new StringArgument("player"))
                        .executesPlayer((player, args) -> {
                            inviteManager.acceptInvite(player, (String) args.get("player"));
                        }))
                .withSubcommand(new CommandAPICommand("deny")
                        .withArguments(new StringArgument("player"))
                        .executesPlayer((player, args) -> {
                            inviteManager.denyInvite(player, (String) args.get("player"));
                        }))
                .withSubcommand(new CommandAPICommand("leave")
                        .executesPlayer((player, args) -> {
                            partyManager.leaveParty(player);
                        }))
                .withSubcommand(new CommandAPICommand("info")
                        .executesPlayer((player, args) -> {
                            player.sendMessage(partyManager.buildPartyInfo(player));
                        }))
                .register();
    }
}
