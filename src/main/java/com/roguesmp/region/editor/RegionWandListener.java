package com.roguesmp.region.editor;

import com.roguesmp.permission.Permissions;
import com.roguesmp.region.RegionSelection;
import com.roguesmp.utils.Utils;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

public class RegionWandListener implements Listener {

    private final RegionSelections selections;
    private final RegionVisualizer visualizer;

    public RegionWandListener(RegionSelections selections, RegionVisualizer visualizer) {
        this.selections = selections;
        this.visualizer = visualizer;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !RegionWand.isWand(event.getItem())) return;

        Player player = event.getPlayer();
        if (!player.hasPermission(Permissions.REGION_ADMIN.node())) return;

        Action action = event.getAction();
        if (action.isRightClick() && player.isSneaking()) {
            event.setCancelled(true);
            new RegionBrowserGui(player).showInventory(player);
            return;
        }

        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            if (player.isSneaking()) {
                selections.clear(player);
                player.sendActionBar(Utils.fromString("<yellow>Selection cleared."));
            } else {
                selections.undo(player);
                player.sendActionBar(Utils.fromString("<yellow>Removed the last corner."));
            }
            return;
        }

        Block block = event.getClickedBlock();
        if (action != Action.RIGHT_CLICK_BLOCK || block == null) return;

        event.setCancelled(true);
        selections.addPoint(player, BlockVector3.at(block.getX(), block.getY(), block.getZ()));
        RegionSelection selection = selections.current(player);
        String shape = selection == null || !selection.isValid() ? "need 1 more corner" : selection.isPolygon() ? "polygon" : "box";
        int count = selection == null ? 0 : selection.points().size();
        player.sendActionBar(Utils.fromString("<green>Corner " + count + " set: <white>" + block.getX() + ", " + block.getY() + ", " + block.getZ()
                + " <gray>(" + shape + ")"));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        selections.clear(event.getPlayer());
        visualizer.clearFocus(event.getPlayer());
    }
}
