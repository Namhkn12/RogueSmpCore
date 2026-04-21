package com.roguesmp.dungeon.actor.command;

import com.roguesmp.dungeon.controller.PartyController;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerProfileArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerProfile;

import java.util.Collection;
import java.util.function.Predicate;

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
                                }).withRequirement(inDungeonWorld())
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
                                }).withRequirement(inDungeonWorld())
                )
                .withSubcommand(
                        new CommandAPICommand("accept")
                                .withArguments(new StringArgument("player"))
                                .executesPlayer((player, args) -> {
                                    String inviterName = (String) args.get("player");
                                    partyController.handleAcceptInvite(player, inviterName);
                                }).withRequirement(inDungeonWorld())
                )
                .withSubcommand(
                        new CommandAPICommand("deny")
                                .withArguments(new StringArgument("player"))
                                .executesPlayer((player, args) -> {
                                    String inviterName = (String) args.get("player");
                                    partyController.handleDenyInvite(player, inviterName);
                                }).withRequirement(inDungeonWorld())
                )
                .withSubcommand(
                        new CommandAPICommand("leave")
                                .executesPlayer((player, args) -> {
                                    partyController.handleLeaveParty(player);
                                }).withRequirement(inDungeonWorld())
                )
                .withSubcommand(
                        new CommandAPICommand("info")
                                .executesPlayer((player, args) -> {
                                    partyController.handeShowPartyInfo(player);
                                })
                )

                .register();
    }

    private Predicate<CommandSender> inDungeonWorld() {
        return sender -> {
            if (!(sender instanceof Player player)) return false;
            return !player.getWorld().getName().startsWith("dungeon_");
        };
    }

}
