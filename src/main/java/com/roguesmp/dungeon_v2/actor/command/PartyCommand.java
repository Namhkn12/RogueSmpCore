package com.roguesmp.dungeon_v2.actor.command;

import com.roguesmp.dungeon_v2.controller_.PartyController;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerProfileArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerProfile;

import java.util.Collection;

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
                                    partyController.handleCreateParty(player);
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("disband")
                                .executesPlayer((player, args) -> {
                                    partyController.handleDisbandParty(player);
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("invite")
                                .withArguments(new PlayerProfileArgument("target"))
                                .executesPlayer((player, args) -> {
                                    @SuppressWarnings("unchecked")
                                    Collection<PlayerProfile> profiles = (Collection<PlayerProfile>) args.get("target");

                                    if (profiles == null || profiles.isEmpty()) {
                                        player.sendMessage("§cKhông tìm thấy player!");
                                        return;
                                    }

                                    PlayerProfile profile = profiles.iterator().next();
                                    Player target = Bukkit.getPlayer(profile.getUniqueId());

                                    if (target == null) {
                                        player.sendMessage("§cPlayer không online!");
                                        return;
                                    }

                                    partyController.handleInvitePlayer(player, target);
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("accept")
                                .withArguments(new StringArgument("player"))
                                .executesPlayer((player, args) -> {
                                    String inviterName = (String) args.get("player");
                                    partyController.handleAcceptInvite(player, inviterName);
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("deny")
                                .withArguments(new StringArgument("player"))
                                .executesPlayer((player, args) -> {
                                    String inviterName = (String) args.get("player");
                                    partyController.handleDenyInvite(player, inviterName);
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("leave")
                                .executesPlayer((player, args) -> {
                                    partyController.handleLeaveParty(player);
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("info")
                                .executesPlayer((player, args) -> {
                                    partyController.handeShowPartyInfo(player);
                                })
                )

                .register();
    }
}
