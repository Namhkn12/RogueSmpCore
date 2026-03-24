package com.roguesmp.dungeon.actor.ui;

import com.roguesmp.dungeon.controller.BuildingController;
import com.roguesmp.dungeon.controller.DungeonController;
import com.roguesmp.dungeon.controller.PartyController;
import com.roguesmp.dungeon.data.Party;
import com.roguesmp.dungeon.dto.NextRoom;
import com.roguesmp.dungeon.exception.BaseException;
import com.roguesmp.dungeon.exception.GlobalException;
import com.roguesmp.dungeon.instance.DungeonInstance;
import com.roguesmp.gui.BaseGui;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Vault;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;

import java.util.List;
import java.util.UUID;

public class NextRoomGui extends BaseGui {

    private static final int TOTAL_SLOTS = 54;
    private static final int CENTER_COL = 4;

    private final DungeonInstance instance;
    private final DungeonController dungeonController;
    private final BuildingController buildingController;
    private final PartyController partyController;
    private final List<NextRoom> nextRooms;
    private final Block door;

    private final ItemStack border;
    private final ItemStack midline;
    private final ItemStack fill;
    private final ItemStack connector;
    private final ItemStack playerMark;

    public NextRoomGui(DungeonInstance instance, DungeonController dungeonController, BuildingController buildingController, PartyController partyController, Block door) {
        super(Component.text("Chọn phòng tiếp theo"), 6);
        this.instance = instance;
        this.dungeonController = dungeonController;
        this.buildingController = buildingController;
        this.partyController = partyController;
        this.nextRooms = dungeonController.getNextRooms(instance).getData();
        this.door = door;

        border = makeHidden(ItemStack.of(Material.MAGENTA_STAINED_GLASS_PANE));
        fill = makeHidden(ItemStack.of(Material.BLACK_STAINED_GLASS_PANE));
        connector = makeHidden(ItemStack.of(Material.BLUE_STAINED_GLASS_PANE));
        midline = makeHidden(ItemStack.of(Material.BLUE_STAINED_GLASS_PANE));
        playerMark = makeHidden(ItemStack.of(Material.PLAYER_HEAD));
    }

    private ItemStack makeHidden(ItemStack item) {
        item.setData(DataComponentTypes.TOOLTIP_DISPLAY,
                TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
        return item;
    }

    @Override
    public void onCloseInventory(InventoryCloseEvent event) {
        if (door.getState() instanceof Vault vault) {
            for (UUID uuid : vault.getRewardedPlayers()) {
                vault.removeRewardedPlayer(uuid);
            }
            vault.update();
        }
    }

    @Override
    public void setup() {
        // 1. Fill đen toàn bộ
        fillEmpty(fill);

        // 2. Cột divider tím (col 3 và col 8)
        for (int slot = 0; slot < TOTAL_SLOTS; slot++) {
            int col = slot % 9;
            if (col == 0 || col == 8) {
                addButton(slot, border, ClickHandler.noAction());
            }
        }

        // 2. Cột divider xanh (col 4)
        for (int slot = 0; slot < TOTAL_SLOTS; slot++) {
            int col = slot % 9;
            if (col == CENTER_COL) {
                addButton(slot, midline, ClickHandler.noAction());
            }
        }

        for (int col = 0; col < 9; col++) {
            addButton(5, col, border, ClickHandler.noAction());
        }

        addButton(49, playerMark, ClickHandler.noAction());

        // 5. Đặt room items (sau cùng để không bị đè)
        for (NextRoom nextRoom : nextRooms) {
            int slot = nextRoom.getIndex();
            if (slot < 0 || slot >= TOTAL_SLOTS) continue;

            ItemStack roomItem = buildRoomItem(nextRoom);
            addButton(slot, roomItem, event -> {
                event.setCancelled(true);
                event.getWhoClicked().closeInventory();
                Location doorLoc = door.getLocation();
                World world = doorLoc.getWorld();

                Location spawnLoc = doorLoc.clone().add(0, -1, 0);
                dungeonController.selectNextRoom(instance, nextRoom.getNode());
                try {
                    BoundingBox roomBox = buildingController.buildSchematicById(nextRoom.getNode().getSchemetas(), spawnLoc);
                    instance.getActiveRoom().setRoomBounds(roomBox);
                    // remove door
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            world.getBlockAt(doorLoc.clone().add(dx, dy, 0)).setType(Material.AIR);
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            });
        }
    }

    private ItemStack buildRoomItem(NextRoom nextRoom) {
        Material material = parseMaterial(nextRoom.getNode().getIcon());
        ItemStack item = ItemStack.of(material);
        item.setData(DataComponentTypes.ITEM_NAME,
                Component.text(nextRoom.getNode().getName(), NamedTextColor.GOLD)
                        .decoration(TextDecoration.ITALIC, false));
        return item;
    }

    private Material parseMaterial(String iconName) {
        if (iconName == null || iconName.isBlank()) return Material.CHEST;
        try {
            Material mat = Material.valueOf(iconName.toUpperCase());
            return mat.isItem() ? mat : Material.CHEST;
        } catch (IllegalArgumentException e) {
            return Material.CHEST;
        }
    }
}