package com.roguesmp.command;

import com.roguesmp.fishing.FishingHotspotManager;
import com.roguesmp.permission.Permissions;
import com.roguesmp.region.ServerRegionManager;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;

public final class FishingHotspotCommand {

    private FishingHotspotCommand() {
    }

    public static void register() {
        new CommandAPICommand("fishhotspot")
                .withPermission(Permissions.FISHING_ADMIN.node())
                .withSubcommand(new CommandAPICommand("spawn")
                        .withArguments(
                                new StringArgument("hotspot")
                                        .replaceSuggestions(ArgumentSuggestions.strings(info -> Registries.FISHING_HOTSPOT.getAll().keySet().toArray(String[]::new))),
                                new StringArgument("region")
                                        .replaceSuggestions(ArgumentSuggestions.strings(info -> info.sender() instanceof Player player
                                                ? ServerRegionManager.getInstance().regions(player.getWorld()).keySet().toArray(String[]::new)
                                                : new String[0])))
                        .executesPlayer((player, args) -> {
                            String hotspotId = (String) args.get("hotspot");
                            String regionId = (String) args.get("region");
                            boolean activated = FishingHotspotManager.getInstance().activate(hotspotId, player.getWorld(), regionId);
                            if (!activated) {
                                player.sendMessage(Utils.fromString("<red>Could not activate: unknown hotspot or region, or the region already has a hotspot."));
                            }
                        }))
                .withSubcommand(new CommandAPICommand("clear")
                        .executes((sender, args) -> {
                            FishingHotspotManager.getInstance().clear();
                            sender.sendMessage(Utils.fromString("<green>All active hotspots cleared."));
                        }))
                .register();
    }
}
