package com.roguesmp.gui.island;

import com.roguesmp.gate.cost.Cost;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.island.IslandData;
import com.roguesmp.island.IslandLevel;
import com.roguesmp.island.IslandLevelService;
import com.roguesmp.island.IslandManager;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class IslandUpgradeConfirmGui extends BaseGui {

    private final Player player;
    private final IslandData island;
    private final int targetLevel;
    private final BaseGui parent;

    public IslandUpgradeConfirmGui(Player player, IslandData island, int targetLevel, BaseGui parent) {
        super(Component.text("Xác Nhận Nâng Cấp Đảo?", NamedTextColor.DARK_GREEN).decoration(TextDecoration.BOLD, true), 3);
        this.player = player;
        this.island = island;
        this.targetLevel = targetLevel;
        this.parent = parent;
    }

    @Override
    public void setup() {
        clearUi();
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text("Nâng đảo lên cấp " + targetLevel + ".", NamedTextColor.GRAY));
        List<Cost> costs = IslandLevelService.directCosts(IslandLevel.of(targetLevel));
        if (!costs.isEmpty()) {
            lore.add(Component.empty());
            lore.add(Utils.text("Bạn sẽ trả:", NamedTextColor.YELLOW));
            for (Cost cost : costs) lore.add(Component.text(" ").append(cost.getDisplay(smpPlayer)).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }

        ItemStack confirm = ItemStack.of(Material.LIME_CONCRETE);
        confirm.setData(DataComponentTypes.ITEM_NAME, Component.text("XÁC NHẬN", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true));
        confirm.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        addButton(1, 2, confirm, event -> {
            event.setCancelled(true);
            IslandLevelService.Result result = IslandManager.getInstance().getLevelService().upgrade(smpPlayer, island, targetLevel);
            if (result == IslandLevelService.Result.LEVEL_CHANGED) player.sendMessage(Utils.fromString("<red>Đảo đã được nâng cấp bởi thành viên khác."));
            else if (result != IslandLevelService.Result.SUCCESS) player.sendMessage(Utils.fromString("<red>Không thể nâng cấp đảo!"));
            returnToParent();
            if (result == IslandLevelService.Result.SUCCESS) IslandLevelGui.refreshViewers(island);
        });

        ItemStack cancel = ItemStack.of(Material.RED_CONCRETE);
        cancel.setData(DataComponentTypes.ITEM_NAME, Component.text("HỦY BỎ", NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
        cancel.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(Utils.text("Quay lại", NamedTextColor.GRAY))));
        addButton(1, 6, cancel, event -> {
            event.setCancelled(true);
            returnToParent();
        });

        fillEmpty(FILLER_BLACK);
    }

    public boolean isFor(IslandData other) {
        return island == other;
    }

    public int getTargetLevel() {
        return targetLevel;
    }

    public void returnToParent() {
        parent.showInventory(player);
    }

    @Override
    public void onClickTopInventory(InventoryClickEvent event) {
        event.setCancelled(true);
        super.onClickTopInventory(event);
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
