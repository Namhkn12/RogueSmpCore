package com.roguesmp.dungeon_v2.actor.ui;

import com.roguesmp.dungeon_v2.controller_.DungeonFlowController;
import com.roguesmp.dungeon_v2.data.definition.room.Room;
import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import com.roguesmp.gui.BaseGui;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;

public class OpenDoorGui extends BaseGui {

    private static final int TOTAL_SLOT = 54;
    private static final int PLAYER_SLOT = 49;

    // Vị trí 3 room slots (cố định)
    private static final int[] ROOM_SLOTS = {10, 13, 16};

    /*
     * Layout minh họa (6x9):
     *  B  B  B  B  B  B  B  B  B   <- row 0: border trên
     *  B [R0] .  . [R1] .  . [R2] B  <- row 1: room slots 10,13,16
     *  B  .  .  .  .  .  .  .  B   <- row 2
     *  B  .  .  .  .  .  .  .  B   <- row 3
     *  B  .  .  .  .  .  .  .  B   <- row 4
     *  B  B  B  B [P] B  B  B  B   <- row 5: border bao, player ở 49
     *
     * Connector paths (từ player 49 lên từng room):
     *  slot 10: 49->40->31->22->13->... nhưng rẽ trái tại row 2 (slot 19)
     *  slot 13: thẳng đứng 49->40->31->22->13
     *  slot 16: tương tự rẽ phải
     */

    private final Block door;
    private final List<Room> rooms;
    private final DungeonFlowController dungeonFlowController;

    private final ItemStack border;
    private final ItemStack bounder;
    private final ItemStack filler;
    private final ItemStack connector;
    private final ItemStack playerIcon;

    public OpenDoorGui( Block door, List<Room> rooms, DungeonFlowController dungeonFlowController) {
        super(Component.text("Dungeon Door"), 5);
        this.door = door;
        this.rooms = rooms;
        this.dungeonFlowController = dungeonFlowController;

        border    = makeHidden(ItemStack.of(Material.MAGENTA_STAINED_GLASS_PANE));
        filler    = makeHidden(ItemStack.of(Material.BLACK_STAINED_GLASS_PANE));
        connector = makeHidden(ItemStack.of(Material.BLUE_STAINED_GLASS_PANE));
        bounder   = makeHidden(ItemStack.of(Material.BLUE_STAINED_GLASS_PANE));
        playerIcon = makeHidden(ItemStack.of(Material.PLAYER_HEAD));
    }

    @Override
    public void setup() {
        // 1. Nền đen toàn bộ
        fillEmpty(filler);

        // 2. Border viền ngoài (row 0, row 5, col 0, col 8)
        placeBorder();

        // 3. Player icon ở slot 49
        addButton(PLAYER_SLOT, playerIcon, ClickHandler.noAction());

        // 4. Vẽ connector + room items tuỳ số lượng rooms
        placeConnectorsAndRooms();
    }

    // -----------------------------------------------------------------------
    // Border
    // -----------------------------------------------------------------------

    private void placeBorder() {
        for (int slot = 0; slot < TOTAL_SLOT; slot++) {
            int row = slot / 9;
            int col = slot % 9;
            boolean isEdge = (row == 0 || row == 5 || col == 0 || col == 8);
            if (isEdge) {
                addButton(slot, border, ClickHandler.noAction());
            }
        }
    }

    // -----------------------------------------------------------------------
    // Connectors & Rooms
    // -----------------------------------------------------------------------

    /**
     * Tuỳ số lượng rooms (0-3) mà vẽ đường connector và đặt room item.
     *
     * Chiến lược connector:
     *  - Trục dọc chính: từ player (row5,col4=49) đi thẳng lên đến row2 (slot 22)
     *    qua các slot: 40 -> 31 -> 22
     *  - Từ row2 rẽ ngang đến đúng cột của room rồi đi lên row1
     *
     *  Slot tham chiếu theo cột:
     *    col1=slot10, col4=slot13, col7=slot16
     *    row2: col1=19, col4=22, col7=25
     *    row3: col1=28, col4=31, col7=34
     *    row4: col1=37, col4=40, col7=43
     */
    private void placeConnectorsAndRooms() {
        int count = rooms.size();
        if (count == 0) return;

        // Xác định các room slot cần dùng (lấy từ giữa ra)
        int[] activeRoomSlots = switch (count) {
            case 1 -> new int[]{13};
            case 2 -> new int[]{10, 16};
            default -> new int[]{10, 13, 16};
        };

        // Vẽ trục dọc chung từ player lên row3 (slot 49->40->31)
        // row4 col4=40, row3 col4=31
        placeConnector(40);
        placeConnector(31);

        // Với mỗi room được chọn, vẽ nhánh từ row3 lên room
        for (int i = 0; i < activeRoomSlots.length; i++) {
            int roomSlot = activeRoomSlots[i];
            int col = roomSlot % 9; // col của room (1, 4, hoặc 7)

            // row3 tại cột đích
            int row3Target = 27 + col; // row3 = offset 27
            // row2 tại cột đích
            int row2Target = 18 + col; // row2 = offset 18

            // Nếu cần rẽ ngang tại row3 (chỉ khi col != 4)
            if (col != 4) {
                // Điền ngang từ col4 đến col đích tại row3
                int startCol = 4;
                int endCol   = col;
                int step     = (endCol > startCol) ? 1 : -1;
                for (int c = startCol + step; c != endCol + step; c += step) {
                    placeConnector(27 + c);
                }
            }

            // Đi thẳng từ row3 lên row1 tại cột đích (row2 -> room)
            placeConnector(row2Target);
            // slot roomSlot = row1 col đích — không đặt connector, đặt room item

            // Đặt room item
            Room room = rooms.get(i);
            ItemStack roomItem = buildRoomItem(room);
            addButton(roomSlot, roomItem, event -> {
                Player player = (Player) event.getWhoClicked();
                event.setCancelled(true);
                player.closeInventory();
                dungeonFlowController.handleSelectNextRoom(player, door, room);
            });
        }
    }

    private void placeConnector(int slot) {
        addButton(slot, connector, ClickHandler.noAction());
    }

    // -----------------------------------------------------------------------
    // Room item builder
    // -----------------------------------------------------------------------

    private ItemStack buildRoomItem(Room room) {
        Material mat = parseMaterial(room.getType().getIconMaterial());
        ItemStack item = ItemStack.of(mat);
        item.setData(DataComponentTypes.ITEM_NAME,
                net.kyori.adventure.text.Component.text(room.getId(),
                                net.kyori.adventure.text.format.NamedTextColor.GOLD)
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
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

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private ItemStack makeHidden(ItemStack item) {
        item.setData(DataComponentTypes.TOOLTIP_DISPLAY,
                TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
        return item;
    }
}