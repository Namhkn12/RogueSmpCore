package com.roguesmp.gui.ability;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.registry.AbilityRegistry;
import dev.jorel.commandapi.CommandAPICommand;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AbilityCatalogue extends BaseGui {

    private final SmpPlayer smpPlayer;
    private final List<AbilityInfo<?>> abilityList = new ArrayList<>();
    private final int pageSize = 28;

    private final ItemStack border;
    private final ItemStack infoBook;
    private final ItemStack notUnlockedItem;
    private final ItemStack prevPage;
    private final ItemStack nextPage;

    private int currentPage = 0;

    public AbilityCatalogue(SmpPlayer smpPlayer) {
        super(Component.text("Sách kĩ năng"), 6);
        this.smpPlayer = smpPlayer;

        border = ItemStack.of(Material.PURPLE_STAINED_GLASS_PANE);
        border.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());

        infoBook = ItemStack.of(Material.WRITTEN_BOOK);
        infoBook.setData(DataComponentTypes.ITEM_NAME, Component.text("Danh sách kĩ năng", NamedTextColor.LIGHT_PURPLE));

        notUnlockedItem = ItemStack.of(Material.COAL_BLOCK);
        notUnlockedItem.setData(DataComponentTypes.ITEM_NAME, Component.text("Kĩ năng chưa mở khóa", NamedTextColor.GRAY));

        prevPage = ItemStack.of(Material.ARROW);
        prevPage.setData(DataComponentTypes.ITEM_NAME, Component.text("Trang trước"));

        nextPage = ItemStack.of(Material.ARROW);
        nextPage.setData(DataComponentTypes.ITEM_NAME, Component.text("Trang sau"));

        abilityList.addAll(AbilityRegistry.getAll());
    }

    @Override
    public void setup() {
        for (int col = 0; col < 9; col++) {
            this.addButton(0, col, border, ClickHandler.noAction());
            this.addButton(5, col, border, ClickHandler.noAction());
        }
        this.addButton(0, 4, infoBook, ClickHandler.noAction());
        for (int row = 1; row < 5; row++) {
            this.addButton(row, 0, border, ClickHandler.noAction());
            this.addButton(row, 8, border, ClickHandler.noAction());
        }
        drawAbilities();
        drawNavigation();
        fillEmpty();
    }

    private void drawAbilities() {
        int start = currentPage * pageSize;
        int end = Math.min(start + pageSize, abilityList.size());

        int index = 0;

        Map<String, Integer> unlocked = smpPlayer.getPlayerData().getUnlockedAbilities();
        for (int i = start; i < end; i++) {
            int row = 1 + (index / 7);
            int col = 1 + (index % 7);

            AbilityInfo<?> info = abilityList.get(i);
            boolean isUnlocked = unlocked.containsKey(info.id());
            ItemStack display;
            if (isUnlocked) {
                int level = unlocked.get(info.id());
                display = info.createInfoItem(smpPlayer, level);
            } else {
                display = notUnlockedItem.clone();
                display.setData(DataComponentTypes.ITEM_NAME, info.displayText());
                display.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(Component.text("Chưa mở khóa", NamedTextColor.GRAY))));
            }

            addButton(row, col, display, event -> {
                event.setCancelled(true);
                if (!isUnlocked) return;
                // TODO: open ability detail GUI
            });

            index++;
        }
    }

    private void drawNavigation() {
        int maxPage = (abilityList.size() - 1) / pageSize;

        if (currentPage > 0) {
            addButton(5, 3, prevPage, event -> {
                event.setCancelled(true);
                currentPage--;
                setup();
            });
        }

        if (currentPage < maxPage) {
            addButton(5, 5, nextPage, event -> {
                event.setCancelled(true);
                currentPage++;
                setup();
            });
        }
    }

    public static void register() {
        new CommandAPICommand("ability")
                .executesPlayer((player1, commandArguments) -> {
                    SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player1.getUniqueId());
                    if (smpPlayer == null) return;
                    new AbilityCatalogue(smpPlayer).showInventory(player1);
                })
                .withSubcommand(new CommandAPICommand("loadout")
                        .executesPlayer((player, commandArguments) -> {
                            SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player.getUniqueId());
                            if (smpPlayer == null) return;
                            new AbilityLoadoutGui(smpPlayer).showInventory(player);
                        }))
                .withSubcommand(new CommandAPICommand("catalogue")
                        .executesPlayer((player, commandArguments) -> {
                            SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player.getUniqueId());
                            if (smpPlayer == null) return;
                            new AbilityCatalogue(smpPlayer).showInventory(player);
                        }))
                .register(RogueSmpCore.getInstance());
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
