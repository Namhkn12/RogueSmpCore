package com.roguesmp.gui.enchant;

import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.constant.Enchants;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.impl.EquipAttributeComponent;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.SmpItemUtils;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class EnchantingGui extends BaseGui {

    private enum GuiState { INPUT, ENCHANT_SELECT, LEVEL_SELECT }

    private final SmpPlayer smpPlayer;
    private GuiState currentState = GuiState.INPUT;

    private ItemStack targetItem = null;
    private Enchants selectedEnchant = null;

    private int page = 0;

    private static final int ITEMS_PER_PAGE = 21;
    private final int INPUT_SLOT = 22;
    private static final ItemStack BORDER = ItemStackUtils.hideTooltip(ItemStack.of(Material.BLACK_STAINED_GLASS_PANE));

    public EnchantingGui(SmpPlayer smpPlayer) {
        super(Component.text("Enchanting", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD), 6);
        this.smpPlayer = smpPlayer;
    }

    @Override
    public void setup() {
        getInventory().clear();
        drawBackground();

        switch (currentState) {
            case INPUT -> renderInput();
            case ENCHANT_SELECT -> renderEnchantSelection();
            case LEVEL_SELECT -> renderLevelSelection();
        }
    }

    private void drawBackground() {
        // Draw a frame around the 6-row inventory
        for (int i = 0; i < 54; i++) {
            // Frame logic: first/last row or first/last column
            addButton(i, BORDER, ClickHandler.noAction());
        }
    }

    private void renderInput() {

        ItemStack glow = new ItemStack(Material.MAGENTA_STAINED_GLASS_PANE);
        glow.setData(DataComponentTypes.ITEM_NAME, Component.text("Place Item Here", NamedTextColor.LIGHT_PURPLE));

        addButton(INPUT_SLOT, null, event -> {
            if (currentState == GuiState.INPUT && event.getSlot() == INPUT_SLOT) {
                Utils.runLater(() -> {
                    ItemStack item = getInventory().getItem(INPUT_SLOT);
                    if (ItemStackUtils.isValidItem(item)) {
                        this.page = 0;
                        this.targetItem = item;
                        this.currentState = GuiState.ENCHANT_SELECT;
                        setup();
                    }
                });
            }
        });

        ItemStack info = new ItemStack(Material.KNOWLEDGE_BOOK);
        info.setData(DataComponentTypes.ITEM_NAME, Component.text("How to Enchant", NamedTextColor.GOLD));
        info.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Component.text("1. Drop an item in the slot above", NamedTextColor.GRAY),
                Component.text("2. Choose your ancient power", NamedTextColor.GRAY),
                Component.text("3. Pay with your soul (XP)", NamedTextColor.GRAY)
        )));
        addItem(49, info);
    }

    private void renderEnchantSelection() {
        addItem(4, targetItem);

        // Header separator
        for (int i = 9; i < 18; i++) {
            addItem(i, ItemStack.of(Material.PURPLE_STAINED_GLASS_PANE));
        }

        // Back to Input button (Lower left)
        ItemStack back = new ItemStack(Material.BARRIER);
        back.setData(DataComponentTypes.ITEM_NAME, Component.text("Quay lại", NamedTextColor.RED));
        addButton(45, back, event -> {
            event.setCancelled(true);
            this.page = 0; // Reset page
            this.currentState = GuiState.INPUT;
            this.targetItem = null;
            Map<Integer, ItemStack> leftOver = event.getWhoClicked().getInventory().addItem(targetItem);
            leftOver.values().forEach(item -> event.getWhoClicked().getWorld().dropItemNaturally(event.getWhoClicked().getLocation(), item));
            setup();
        });

        List<Enchants> allEnchants = new ArrayList<>();
        BaseItem baseItem = SmpItemUtils.getBaseItem(targetItem);
        if (baseItem == null) return;
        EquipAttributeComponent equipAttributeComponent = baseItem.getComponent(ComponentKeys.ATTRIBUTE);
        if (equipAttributeComponent == null) return;
        EquipSlot equipSlot = equipAttributeComponent.getSlot(); //Only enchants that work on this slot will show up
        for (Enchants enchants : Enchants.values()) {
            if (enchants.getEnchant().getActiveSlots().contains(equipSlot)) {
                allEnchants.add(enchants);
            }
        }

//        List<Enchants> allEnchants = Arrays.asList(Enchants.values());
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allEnchants.size());

        // We use 'i' for the list index and 'relativeIndex' for the grid position
        for (int i = startIndex; i < endIndex; i++) {
            int relativeIndex = i - startIndex; // 0 to 20

            /* * Logic to map 0-20 to the inner 7x3 grid:
             * Row starts at 2. Column starts at 1.
             */
            int row = 2 + (relativeIndex / 7);
            int col = 1 + (relativeIndex % 7);
            int targetSlot = getSlot(row, col);

            Enchants enchantEnum = allEnchants.get(i);
            ItemStack icon = new ItemStack(Material.ENCHANTED_BOOK);
            icon.setData(DataComponentTypes.ITEM_NAME, Component.text(enchantEnum.getEnchant().getSimpleName(), NamedTextColor.AQUA, TextDecoration.BOLD));

            icon.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                    Component.empty(),
                    Utils.text("Click để chọn cấp độ", NamedTextColor.YELLOW)
            )));

            addButton(targetSlot, icon, event -> {
                event.setCancelled(true);
                this.selectedEnchant = enchantEnum;
                this.currentState = GuiState.LEVEL_SELECT;
                setup();
            });
        }

        // --- Pagination Controls ---

        // Previous Page (Slot 48)
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            prev.setData(DataComponentTypes.ITEM_NAME, Component.text("← Trang trước", NamedTextColor.YELLOW));
            addButton(48, prev, event -> {
                event.setCancelled(true);
                page--;
                setup();
            });
        }

        // Current Page Info (Slot 49)
        ItemStack pageInfo = new ItemStack(Material.PAPER);
        pageInfo.setData(DataComponentTypes.ITEM_NAME, Component.text("Trang " + (page + 1), NamedTextColor.WHITE));
        addItem(49, pageInfo);

        // Next Page (Slot 50)
        if (endIndex < allEnchants.size()) {
            ItemStack next = new ItemStack(Material.ARROW);
            next.setData(DataComponentTypes.ITEM_NAME, Component.text("Trang sau →", NamedTextColor.YELLOW));
            addButton(50, next, event -> {
                event.setCancelled(true);
                page++;
                setup();
            });
        }
    }

    private void renderLevelSelection() {
        addItem(4, targetItem);

        // Title of the enchantment being selected
        ItemStack title = new ItemStack(Material.BOOK);
        title.setData(DataComponentTypes.ITEM_NAME, Component.text("Enchanting: " + selectedEnchant.getEnchant().getSimpleName(), NamedTextColor.AQUA));
        addItem(13, title);

        for (int i = 1; i <= 5; i++) {
            int level = i;
            int cost = level * 5;

            ItemStack levelIcon = new ItemStack(Material.EXPERIENCE_BOTTLE);
            levelIcon.setAmount(level);
            levelIcon.setData(DataComponentTypes.ITEM_NAME, Component.text("Level " + Utils.toRoman(level), NamedTextColor.GREEN));
            levelIcon.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                    Component.empty(),
                    Component.text("Cost: ", NamedTextColor.GRAY).append(Component.text(cost + " XP Levels", NamedTextColor.YELLOW)),
                    Component.empty(),
                    Component.text("Click to enchant!", NamedTextColor.LIGHT_PURPLE)
            )));

            // Centered layout for levels 1-5 (Slots 29, 30, 31, 32, 33)
            addButton(28 + i, levelIcon, event -> {
                event.setCancelled(true);
                handleFinalPurchase(level, cost);
            });
        }

        // Return to selection
        ItemStack back = new ItemStack(Material.ARROW);
        back.setData(DataComponentTypes.ITEM_NAME, Component.text("Back to Enchants", NamedTextColor.GRAY));
        addButton(49, back, event -> {
            event.setCancelled(true);
            this.currentState = GuiState.ENCHANT_SELECT;
            setup();
        });
    }

    private void handleFinalPurchase(int level, int cost) {
        Player p = smpPlayer.getBukkitPlayer();
        if (p == null) return;

        if (p.getLevel() < cost) {
            p.sendMessage(Component.text("✖ You lack the experience required!", NamedTextColor.RED));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        p.setLevel(p.getLevel() - cost);
        Map<Enchants, Integer> data = new HashMap<>();
        data.put(selectedEnchant, level);

        this.targetItem = SmpItemUtils.addEnchant(targetItem, smpPlayer, data);

        this.currentState = GuiState.ENCHANT_SELECT;
        setup();
        p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 0.8f);
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
    }

    @Override
    public void onCloseInventory(InventoryCloseEvent event) {
        if (targetItem != null) {
            // Check if player's inventory is full
            Map<Integer, ItemStack> leftOver = event.getPlayer().getInventory().addItem(targetItem);
            leftOver.values().forEach(item -> event.getPlayer().getWorld().dropItemNaturally(event.getPlayer().getLocation(), item));
            this.targetItem = null;
        }
    }

    public static void registerCommand() {
        new CommandAPICommand("smpenchant")
                .executesPlayer((sender, args) -> {
                    new EnchantingGui(PlayerManager.getInstance().getSmpPlayer(sender.getUniqueId())).showInventory(sender);
                })
                .register();
    }
}
