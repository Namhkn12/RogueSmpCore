package com.roguesmp.gui.dungeon;

import com.roguesmp.dungeon_core.DungeonInstance;
import com.roguesmp.dungeon_core.DungeonManager;
import com.roguesmp.dungeon_core.DungeonPlayerActions;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.List;

public class DungeonInviteGui extends BaseGui {

    private static final int PER_PAGE = 45;

    private final Player player;
    private final DungeonInstance run;
    private int page;

    public DungeonInviteGui(Player player, DungeonInstance run) {
        super(Component.text("Mời người chơi"), 6);
        this.player = player;
        this.run = run;
    }

    @Override
    public void setup() {
        clearUi();
        DungeonManager manager = DungeonManager.getInstance();
        List<Player> candidates = Bukkit.getOnlinePlayers().stream()
                .filter(online -> !run.isMember(online.getUniqueId()))
                .filter(online -> manager.findFor(online.getUniqueId(), run.dungeonId()) == null)
                .sorted(Comparator.comparing(Player::getName))
                .map(online -> (Player) online)
                .toList();
        int pages = Math.max(1, (candidates.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.min(page, pages - 1);

        for (int slot = 0; slot < PER_PAGE && page * PER_PAGE + slot < candidates.size(); slot++) {
            Player target = candidates.get(page * PER_PAGE + slot);
            ItemStack head = DungeonDetailGui.button(Material.PLAYER_HEAD, "<white>" + target.getName(), List.of(Utils.fromString("<yellow>▶ Click để mời")));
            head.setData(DataComponentTypes.PROFILE, ResolvableProfile.resolvableProfile(target.getPlayerProfile()));
            addButton(slot, head, event -> {
                event.setCancelled(true);
                if (!target.isOnline()) return;
                DungeonPlayerActions.invite(player, run, target);
                setup();
            });
        }
        if (candidates.isEmpty()) {
            addButton(2, 4, DungeonDetailGui.button(Material.GRAY_DYE, "<gray>Không có ai để mời", List.of()), ClickHandler.noAction());
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
        addButton(5, 0, DungeonDetailGui.button(Material.ARROW, "<yellow>Quay lại", List.of()), event -> {
            event.setCancelled(true);
            new DungeonDetailGui(player, run.dungeonId()).showInventory(player);
        });
        fillEmpty(FILLER);
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
