package com.roguesmp.gui.dungeon;

import com.roguesmp.dungeon.room.RoomDefinition;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Vault;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

public class RoomChoiceGui extends BaseGui {

    private static final int[][] LAYOUTS = {{13}, {11, 15}, {10, 13, 16}, {10, 12, 14, 16}};

    private final Block door;
    private final List<String> rooms;
    private final BiConsumer<Player, String> onSelect;

    public RoomChoiceGui(Block door, List<String> rooms, BiConsumer<Player, String> onSelect) {
        super(Component.text("Chọn phòng tiếp theo"), 3);
        this.door = door;
        this.rooms = rooms;
        this.onSelect = onSelect;
    }

    @Override
    public void setup() {
        clearUi();
        int[] slots = LAYOUTS[Math.min(rooms.size(), LAYOUTS.length) - 1];
        for (int i = 0; i < slots.length; i++) {
            String roomId = rooms.get(i);
            RoomDefinition room = Registries.DUNGEON_ROOM.get(roomId);
            Material icon = room != null ? room.type().icon() : Material.BARRIER;
            String name = room != null && !room.name().isEmpty() ? room.name() : roomId;
            String type = room != null ? room.type().name() : "?";
            addButton(slots[i], DungeonDetailGui.button(icon, "<gold>" + name,
                    List.of(Utils.fromString("<gray>Loại: <white>" + type), Utils.fromString("<yellow>▶ Click để mở cửa"))), event -> {
                event.setCancelled(true);
                Player player = (Player) event.getWhoClicked();
                player.closeInventory();
                onSelect.accept(player, roomId);
            });
        }
        fillEmpty(FILLER_BLACK);
    }

    @Override
    public void onOpenInventory(InventoryOpenEvent event) {
        if (!(door.getState() instanceof Vault vault)) return;
        if (!vault.getRewardedPlayers().isEmpty()) {
            event.setCancelled(true);
            return;
        }
        vault.addRewardedPlayer(event.getPlayer().getUniqueId());
        vault.update();
    }

    @Override
    public void onCloseInventory(InventoryCloseEvent event) {
        if (!(door.getState() instanceof Vault vault)) return;
        for (UUID uuid : List.copyOf(vault.getRewardedPlayers())) vault.removeRewardedPlayer(uuid);
        vault.update();
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
