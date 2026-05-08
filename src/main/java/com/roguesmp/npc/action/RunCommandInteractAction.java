package com.roguesmp.npc.action;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class RunCommandInteractAction implements NpcAction {

    private final String command;

    public RunCommandInteractAction(String command) {
        this.command = command;
    }

    @Override
    public void onRightClick(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        String processedCommand = processString(player);
        if (processedCommand.isBlank()) return;
        player.performCommand(processedCommand);
    }

    @Override
    public void onLeftClick(PrePlayerAttackEntityEvent event) {
        Player player = event.getPlayer();
        String processedCommand = processString(player);
        if (processedCommand.isBlank()) return;
        player.performCommand(processedCommand);
    }

    private String processString(Player player) {
        if (command == null) return "";

        return command
                .replace("@p", player.getName())
                .replace("@p_id", player.getUniqueId().toString());
    }
}
