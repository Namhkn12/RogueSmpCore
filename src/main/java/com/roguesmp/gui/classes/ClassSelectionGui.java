package com.roguesmp.gui.classes;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.classes.PlayerClass;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Lets a player pick (or switch to) a {@link PlayerClass}. Picking a class the player has never
 * had grants its default abilities permanently; switching away from a class only unequips its
 * abilities, it never un-levels them (see {@link SmpPlayer#setPlayerClass(PlayerClass)}).
 */
public class ClassSelectionGui extends BaseGui {

    private final SmpPlayer smpPlayer;
    private final List<PlayerClass> classes;

    public ClassSelectionGui(SmpPlayer smpPlayer) {
        super(Component.text("Chọn Lớp Nhân Vật"), 4);
        this.smpPlayer = smpPlayer;
        this.classes = new ArrayList<>(Registries.PLAYER_CLASS.getAll().values());
    }

    @Override
    public void setup() {
        clearUi();
        fillEmpty(FILLER_BLACK);

        String currentClassId = smpPlayer.getPlayerData().getClassId();

        for (int i = 0; i < classes.size() && i < 14; i++) {
            PlayerClass playerClass = classes.get(i);
            int row = 1 + (i / 7);
            int col = 1 + (i % 7);
            boolean isCurrent = playerClass.getId().equals(currentClassId);

            addButton(row, col, createClassIcon(playerClass, isCurrent), event -> {
                event.setCancelled(true);
                if (isCurrent) return;

                smpPlayer.setPlayerClass(playerClass);
                smpPlayer.sendMessage(Component.text("Bạn đã chọn lớp nhân vật: ", NamedTextColor.GREEN)
                        .append(playerClass.getFormattedDisplayName()));
                Utils.runLater(() -> new ClassSelectionGui(smpPlayer).showInventory(event.getWhoClicked()));
            });
        }
    }

    private ItemStack createClassIcon(PlayerClass playerClass, boolean isCurrent) {
        ItemStack item = ItemStack.of(playerClass.getIcon());
        item.setData(DataComponentTypes.ITEM_NAME, playerClass.getFormattedDisplayName()
                .decoration(TextDecoration.BOLD, isCurrent));

        List<Component> lore = new ArrayList<>();
        if (isCurrent) {
            lore.add(Utils.text("✔ Đang sử dụng", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
        }
        for (String line : playerClass.getDescription()) {
            lore.add(Utils.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        if (!isCurrent) {
            lore.add(Component.empty());
            lore.add(Utils.text("Click để chọn lớp này", NamedTextColor.YELLOW));
        }

        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return item;
    }

    public static void register() {
        new CommandAPICommand("class")
                .executesPlayer((player, args) -> {
                    SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player.getUniqueId());
                    if (smpPlayer == null) return;
                    new ClassSelectionGui(smpPlayer).showInventory(player);
                })
                .register(RogueSmpCore.getInstance());
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
