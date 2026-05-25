package com.roguesmp.gui.island;

import com.roguesmp.gui.BaseGui;
import com.roguesmp.island.IslandManager;
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

import java.util.List;
import java.util.UUID;

public class IslandLeaveConfirmGui extends BaseGui {

    private final UUID islandId;
    private final Player player;

    public IslandLeaveConfirmGui(UUID islandId, Player player) {
        super(Component.text("Xác Nhận Rời Khỏi Đảo?", NamedTextColor.RED).decoration(TextDecoration.BOLD, true), 3);
        this.islandId = islandId;
        this.player = player;
    }

    @Override
    public void setup() {
        clearUi();

        // --- RED BUTTON: PROCEED WITH DELETION ---
        ItemStack confirmButton = ItemStack.of(Material.RED_CONCRETE);
        confirmButton.setData(DataComponentTypes.ITEM_NAME, Component.text("XÁC NHẬN XÓA ĐẢO", NamedTextColor.DARK_RED).decoration(TextDecoration.BOLD, true));
        confirmButton.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.text("Đồng ý rời khỏi hòn đảo.", NamedTextColor.GRAY),
                Utils.text("Hành động này hoàn toàn không thể khôi phục!", NamedTextColor.RED)
        )));

        addButton(1, 2, confirmButton, event -> {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player executioner) {
                executioner.closeInventory();
                // Execute master teardown sequence via singleton
                IslandManager.getInstance().removeMember(islandId, player.getUniqueId());
            }
        });

        // --- GREEN BUTTON: ABORT AND RETURN ---
        ItemStack abortButton = ItemStack.of(Material.GREEN_CONCRETE);
        abortButton.setData(DataComponentTypes.ITEM_NAME, Component.text("HỦY BỎ", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true));
        abortButton.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(Utils.text("Quay lại menu chính", NamedTextColor.GRAY))));

        addButton(1, 6, abortButton, ClickHandler.openGui(new IslandMainGui(player)));

        // Fill background slots for a clean UI aesthetic
        fillEmpty(FILLER_BLACK);
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
