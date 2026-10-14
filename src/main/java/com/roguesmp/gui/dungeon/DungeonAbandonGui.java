package com.roguesmp.gui.dungeon;

import com.roguesmp.dungeon.DungeonInstance;
import com.roguesmp.dungeon.DungeonManager;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class DungeonAbandonGui extends BaseGui {

    private final Player player;
    private final DungeonInstance run;

    public DungeonAbandonGui(Player player, DungeonInstance run) {
        super(Component.text("Rời bỏ phó bản?", NamedTextColor.RED), 3);
        this.player = player;
        this.run = run;
    }

    @Override
    public void setup() {
        clearUi();
        boolean last = run.members().size() <= 1;
        String warning = last ? "<red>Bạn là thành viên cuối cùng: phó bản sẽ bị huỷ!" : "<gray>Bạn sẽ mất quyền vào phó bản này.";

        addButton(1, 2, DungeonDetailGui.button(Material.RED_CONCRETE, "<dark_red><b>XÁC NHẬN RỜI BỎ",
                List.of(Utils.fromString(warning), Utils.fromString("<red>Không thể hoàn tác!"))), event -> {
            event.setCancelled(true);
            player.closeInventory();
            DungeonManager.getInstance().abandon(player, run);
        });
        addButton(1, 6, DungeonDetailGui.button(Material.GREEN_CONCRETE, "<green><b>HUỶ", List.of(Utils.fromString("<gray>Quay lại"))), event -> {
            event.setCancelled(true);
            new DungeonDetailGui(player, run.dungeonId()).showInventory(player);
        });
        fillEmpty(FILLER_BLACK);
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
