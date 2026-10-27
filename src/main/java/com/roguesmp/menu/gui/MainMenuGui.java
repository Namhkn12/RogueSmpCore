package com.roguesmp.menu.gui;

import com.roguesmp.gui.BaseGui;
import com.roguesmp.menu.MenuEntries;
import com.roguesmp.menu.MenuEntry;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class MainMenuGui extends BaseGui {

    private final Player player;

    public MainMenuGui(Player player) {
        super(Component.text("Menu", NamedTextColor.DARK_AQUA).decoration(TextDecoration.BOLD, true), 3);
        this.player = player;
    }

    @Override
    public void setup() {
        clearUi();
        for (MenuEntry entry : MenuEntries.ENTRIES) {
            addButton(entry.slot(), entry.icon().get(), event -> {
                event.setCancelled(true);
                BaseGui target = entry.factory().apply(player)
                        .withBackAction(getName(), viewer -> new MainMenuGui(player).showInventory(viewer));
                Utils.runLater(() -> target.showInventory(player));
            });
        }
        fillEmpty(FILLER_BLACK);
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
