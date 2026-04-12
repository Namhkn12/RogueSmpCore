package com.roguesmp.gui.enchant;

import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.constant.Enchants;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.impl.EnchantComponent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.SmpItemUtils;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GrindstoneGui extends BaseGui {

    private final SmpPlayer smpPlayer;
    private final Player player;
    private final int INPUT_SLOT = 4;
    private ItemStack currentInput = null;

    private int currentPage = 0;
    private static final int PAGE_SIZE = 14; // 2 rows of 9
    private static final int[] ENCHANT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
    };

    // Control Buttons (Bottom Row)
    private static final int PREV_PAGE = 27;
    private static final int NEXT_PAGE = 35;
    private static final int FINISH_BUTTON = 31;

    public GrindstoneGui(SmpPlayer player) {
        super(Component.text("Gỡ enchant"), 4);
        this.smpPlayer = player;
        this.player = smpPlayer.getBukkitPlayer();
    }

    @Override
    public void setup() {
        clearUi();
        fillEmpty(FILLER_BLACK);

        if (currentInput == null) {
            setupEmptyState();
            return;
        }

        renderInterface();
    }

    private void setupEmptyState() {
        ItemStack info = new ItemStack(Material.HOPPER);
        info.setData(DataComponentTypes.ITEM_NAME, Component.text("Click một vật phẩm trong túi", NamedTextColor.GRAY));
        addButton(INPUT_SLOT, info, ClickHandler.noAction());
    }

    private void renderInterface() {
        // 1. Current Item Display
        addButton(INPUT_SLOT, currentInput, event -> {
            event.setCancelled(true);
            returnItemToPlayer();
        });

        SmpItem smpItem = new SmpItem(currentInput);
        EnchantComponent enchantComp = smpItem.getComponent(ComponentKeys.ENCHANT);

        if (enchantComp == null || enchantComp.getPersistentEnchants().isEmpty()) {
            setupNoEnchantsState();
            return;
        }

        // 2. Pagination Logic
        List<Map.Entry<Enchants, Integer>> entries = new ArrayList<>(enchantComp.getPersistentEnchants().entrySet());
        int maxPages = (int) Math.ceil((double) entries.size() / PAGE_SIZE);

        // Clamp page index just in case
        if (currentPage >= maxPages) currentPage = Math.max(0, maxPages - 1);

        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, entries.size());

        for (int i = start; i < end; i++) {
            int slotIndex = i - start;
            Map.Entry<Enchants, Integer> entry = entries.get(i);
            addEnchantButton(ENCHANT_SLOTS[slotIndex], entry.getKey(), entry.getValue());
        }

        // 3. Navigation Buttons
        if (currentPage > 0) {
            addButton(PREV_PAGE, PREV_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                currentPage--;
                setup();
            });
        }

        if (end < entries.size()) {
            addButton(NEXT_PAGE, NEXT_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                currentPage++;
                setup();
            });
        }

        // 4. Finish Button
        ItemStack finish = new ItemStack(Material.CHEST);
        finish.setData(DataComponentTypes.ITEM_NAME, Component.text("Thu hồi vật phẩm", NamedTextColor.GREEN));
        addButton(FINISH_BUTTON, finish, event -> {
            event.setCancelled(true);
            returnItemToPlayer();
        });
    }

    private void addEnchantButton(int slot, Enchants enchant, int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        book.setData(DataComponentTypes.ITEM_NAME, Component.text(enchant.getEnchant().getSimpleName() + " " + Utils.toRoman(level), NamedTextColor.AQUA));
        book.setData(DataComponentTypes.LORE, ItemLore.lore(Collections.singletonList(Utils.text("Click để gỡ", NamedTextColor.RED))));

        addButton(slot, book, event -> {
            event.setCancelled(true);
            Map<Enchants, Integer> removalMap = Map.of(enchant, -level);
            var result = SmpItemUtils.addEnchant(currentInput, smpPlayer, removalMap);
            if (result.success()) {
                this.currentInput = result.itemStack();
                player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1f, 1f);
                setup();
            }
        });
    }

    private void returnItemToPlayer() {
        if (currentInput != null) {
            PlayerUtils.giveItem(player, currentInput);
            currentInput = null;
            currentPage = 0;
            setup();
        }
    }

    private void setupNoEnchantsState() {
        ItemStack paper = new ItemStack(Material.PAPER);
        paper.setData(DataComponentTypes.ITEM_NAME, Component.text("Vật phẩm hiện không có enchant nào...", NamedTextColor.GRAY));
        addItem(22, paper);
        addButton(FINISH_BUTTON, new ItemStack(Material.CHEST), event -> {
            event.setCancelled(true);
            returnItemToPlayer();
        });
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (!ItemStackUtils.isValidItem(clicked)) return;

        if (currentInput != null) PlayerUtils.giveItem(player, currentInput);

        currentInput = clicked.clone();
        clicked.setAmount(0);
        currentPage = 0;
        setup();
    }

    @Override
    public void onCloseInventory(InventoryCloseEvent event) {
        if (currentInput != null) {
            PlayerUtils.giveItem(player, currentInput);
            currentInput = null;
        }
    }
}
