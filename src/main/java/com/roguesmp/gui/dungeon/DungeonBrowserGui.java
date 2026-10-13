package com.roguesmp.gui.dungeon;

import com.roguesmp.dungeon_core.DungeonDefinition;
import com.roguesmp.dungeon_core.DungeonInstance;
import com.roguesmp.dungeon_core.DungeonManager;
import com.roguesmp.dungeon_core.feature.MembershipFeature;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DungeonBrowserGui extends BaseGui {

    private static final int PER_PAGE = 28;

    private final Player player;
    private int page;

    public DungeonBrowserGui(Player player) {
        super(Component.text("Phó bản"), 6);
        this.player = player;
    }

    @Override
    public void setup() {
        clearUi();
        for (int r = 0; r <= 5; r++) {
            for (int c = 0; c < 9; c++) {
                if (r == 0 || r == 5 || c == 0 || c == 8) addButton(r, c, FILLER_BLACK, ClickHandler.noAction());
            }
        }

        DungeonManager manager = DungeonManager.getInstance();
        List<Map.Entry<String, DungeonDefinition>> dungeons = Registries.DUNGEON.getAll().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();
        int pages = Math.max(1, (dungeons.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.min(page, pages - 1);
        int index = page * PER_PAGE;
        for (int r = 1; r <= 4; r++) {
            for (int c = 1; c <= 7; c++) {
                if (index >= dungeons.size()) break;
                String dungeonId = dungeons.get(index).getKey();
                DungeonDefinition definition = dungeons.get(index++).getValue();

                List<Component> status = new ArrayList<>();
                DungeonInstance run = manager.findFor(player.getUniqueId(), dungeonId);
                int invites = MembershipFeature.invitesFor(manager, player.getUniqueId(), dungeonId).size();
                if (run != null) {
                    status.add(Utils.fromString("<green>● Đang tham gia"));
                } else if (invites > 0) {
                    status.add(Utils.fromString("<aqua>✉ " + invites + " lời mời đang chờ"));
                }
                status.add(Utils.fromString("<yellow>▶ Click để xem"));

                addButton(r, c, definition.toIcon(status), event -> {
                    event.setCancelled(true);
                    new DungeonDetailGui(player, dungeonId).showInventory(player);
                });
            }
        }
        if (page > 0) addButton(5, 3, PREV_PAGE_BUTTON, event -> {
            event.setCancelled(true);
            page--;
            setup();
        });
        if (page < pages - 1) addButton(5, 5, NEXT_PAGE_BUTTON, event -> {
            event.setCancelled(true);
            page++;
            setup();
        });
        fillEmpty(FILLER);
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
