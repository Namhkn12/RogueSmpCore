package com.roguesmp.command;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.fishing.FishingHotspotManager;
import com.roguesmp.fishing.FishingHotspotSpot;
import com.roguesmp.permission.Permissions;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.WorldPos;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.ListArgumentBuilder;
import dev.jorel.commandapi.arguments.StringArgument;

import java.util.List;

public final class FishingHotspotCommand {

    private FishingHotspotCommand() {
    }

    public static void register() {
        new CommandAPICommand("fishhotspot")
                .withPermission(Permissions.FISHING_ADMIN.node())
                .withSubcommand(new CommandAPICommand("addspot")
                        .withArguments(new StringArgument("id"))
                        .withOptionalArguments(new ListArgumentBuilder<String>("hotspots")
                                .withList(() -> Registries.FISHING_HOTSPOT.getAll().keySet())
                                .withMapper(hotspotId -> hotspotId)
                                .buildGreedy())
                        .executesPlayer((player, args) -> {
                            String id = (String) args.get("id");
                            List<String> hotspots = (List<String>) args.getOrDefault("hotspots", List.of());
                            FishingHotspotSpot spot = new FishingHotspotSpot(WorldPos.of(player.getLocation()), List.copyOf(hotspots));
                            Registries.FISHING_HOTSPOT_SPOT.registerAndSave(RogueSmpCore.getInstance(), id, spot);
                            player.sendMessage(Utils.fromString("<green>Hotspot spot '" + id + "' saved at your position ("
                                    + (hotspots.isEmpty() ? "any hotspot type" : String.join(", ", hotspots)) + ")."));
                        }))
                .withSubcommand(new CommandAPICommand("spawn")
                        .withArguments(new StringArgument("hotspot")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> Registries.FISHING_HOTSPOT.getAll().keySet().toArray(String[]::new))))
                        .executesPlayer((player, args) -> {
                            String hotspotId = (String) args.get("hotspot");
                            boolean activated = FishingHotspotManager.getInstance().activate("manual_" + player.getUniqueId(), hotspotId, player.getLocation());
                            if (!activated) player.sendMessage(Utils.fromString("<red>No hotspot named '" + hotspotId + "'."));
                        }))
                .withSubcommand(new CommandAPICommand("clear")
                        .executes((sender, args) -> {
                            FishingHotspotManager.getInstance().clear();
                            sender.sendMessage(Utils.fromString("<green>All active hotspots cleared."));
                        }))
                .register();
    }
}
