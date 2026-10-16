package com.roguesmp.block.gui;

import com.roguesmp.block.StoredItem;
import com.roguesmp.block.impl.cokeoven.CokeOvenBlock;
import com.roguesmp.crafting.CraftingIngredient;
import com.roguesmp.crafting.recipe.CokeOvenRecipe;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.world.level.block.entity.Hopper;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CokeOvenGui extends BaseGui {

    private static final int[] INPUT_SLOTS = {10, 11};
    private static final int[] OUTPUT_SLOTS = {15, 16};
    private static final int PROGRESS_SLOT = 13;
    private static final int RESULT_PREVIEW_SLOT = 22;

    private final CokeOvenBlock oven;

    public CokeOvenGui(CokeOvenBlock oven) {
        super(Component.text("Coke Oven"), 3);
        this.oven = oven;
    }

    @Override
    public void setup() {
        fillEmpty(FILLER_BLACK);
        refresh();
    }

    public void refresh() {
        for (int i = 0; i < INPUT_SLOTS.length; i++) {
            int index = i;
            addButton(INPUT_SLOTS[i], slotDisplay(oven.input(i), inputPlaceholder()), event -> onInputClick(event, index));
        }
        for (int i = 0; i < OUTPUT_SLOTS.length; i++) {
            int index = i;
            addButton(OUTPUT_SLOTS[i], slotDisplay(oven.output(i), outputPlaceholder()), event -> onOutputClick(event, index));
        }
        addButton(PROGRESS_SLOT, progressDisplay(), ClickHandler.noAction());
        addButton(RESULT_PREVIEW_SLOT, resultPreview(), ClickHandler.noAction());
    }

    private ItemStack slotDisplay(StoredItem stored, ItemStack placeholder) {
        ItemStack stack = stored == null ? null : stored.toItemStack();
        return stack != null ? stack : placeholder;
    }

    private ItemStack inputPlaceholder() {
        ItemStack item = ItemStack.of(Material.ORANGE_STAINED_GLASS_PANE);
        ItemStackUtils.setItemName(item, Utils.text("Nguyên liệu", NamedTextColor.GOLD));
        ItemStackUtils.setLore(item, List.of(Utils.text("Đặt nguyên liệu vào đây", NamedTextColor.DARK_GRAY)));
        return item;
    }

    private ItemStack outputPlaceholder() {
        ItemStack item = ItemStack.of(Material.LIME_STAINED_GLASS_PANE);
        ItemStackUtils.setItemName(item, Utils.text("Thành phẩm", NamedTextColor.GREEN));
        ItemStackUtils.setLore(item, List.of(Utils.text("Thành phẩm sẽ xuất hiện ở đây", NamedTextColor.DARK_GRAY)));
        return item;
    }

    private ItemStack progressDisplay() {
        CokeOvenRecipe recipe = oven.activeRecipe();
        if (recipe == null) {
            ItemStack item = ItemStack.of(Material.GRAY_STAINED_GLASS_PANE);
            ItemStackUtils.setItemName(item, Utils.text("Đang chờ", NamedTextColor.GRAY));
            ItemStackUtils.setLore(item, List.of(Utils.text("Không có công thức phù hợp", NamedTextColor.DARK_GRAY)));
            return item;
        }

        int duration = recipe.getDuration();
        int percent = Math.min(100, oven.progress() * 100 / duration);

        ItemStack item = ItemStack.of(Material.BLAZE_POWDER);
        item.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
        item.setData(DataComponentTypes.MAX_DAMAGE, 100);
        item.setData(DataComponentTypes.DAMAGE, 100 - percent);
        ItemStackUtils.setItemName(item, Utils.text("Tiến độ: " + percent + "%", NamedTextColor.YELLOW));

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text("Thời gian: ", NamedTextColor.YELLOW).append(Component.text(
                Utils.formatDecimal(oven.progress() / 20d) + "s / " + Utils.formatDecimal(duration / 20d) + "s", NamedTextColor.GRAY)));
        if (oven.isStalled()) lore.add(Utils.text("Tạm dừng: ô thành phẩm đã đầy", NamedTextColor.RED));
        ItemStackUtils.setLore(item, lore);
        return item;
    }

    private ItemStack resultPreview() {
        CokeOvenRecipe recipe = oven.activeRecipe();
        ItemStack preview = recipe == null || recipe.getResults().isEmpty() ? null : recipe.getResults().getFirst().toItemStack();
        if (preview == null) {
            ItemStack item = ItemStack.of(Material.GRAY_STAINED_GLASS_PANE);
            ItemStackUtils.setItemName(item, Utils.text("Thành phẩm dự kiến", NamedTextColor.GRAY));
            ItemStackUtils.setLore(item, List.of(Utils.text("Chưa có công thức", NamedTextColor.DARK_GRAY)));
            return item;
        }

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text("Đang chế tạo:", NamedTextColor.GOLD));
        for (CraftingIngredient result : recipe.getResults()) {
            StoredItem stored = new StoredItem(result.key(), result.count());
            lore.add(Utils.text("- " + result.count() + "x ", NamedTextColor.GRAY).append(stored.displayName()));
        }
        ItemStackUtils.setLore(preview, lore);
        return preview;
    }

    private void onInputClick(InventoryClickEvent event, int index) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        StoredItem stored = oven.input(index);

        if (event.isShiftClick()) {
            if (stored != null) giveTaken(player, oven.takeInput(index, stored.amount()));
            return;
        }

        ItemStack cursor = event.getView().getCursor();
        if (cursor.getType().isAir()) {
            if (stored == null) return;
            setCursor(event, oven.takeInput(index, event.isRightClick() ? (stored.amount() + 1) / 2 : stored.amount()));
            return;
        }

        StoredItem incoming = StoredItem.of(cursor);
        if (stored != null && incoming != null && !stored.item().equals(incoming.item())) {
            StoredItem previous = oven.takeInput(index, stored.amount());
            oven.insertInput(index, cursor);
            setCursor(event, previous);
            return;
        }

        ItemStack offered = cursor.clone();
        if (event.isRightClick()) offered.setAmount(1);
        int accepted = oven.insertInput(index, offered);
        shrinkCursor(event, cursor, accepted);
    }

    private void onOutputClick(InventoryClickEvent event, int index) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        StoredItem stored = oven.output(index);
        if (stored == null) return;

        if (event.isShiftClick()) {
            giveTaken(player, oven.takeOutput(index, stored.amount()));
            return;
        }

        ItemStack cursor = event.getView().getCursor();
        if (cursor.getType().isAir()) {
            setCursor(event, oven.takeOutput(index, event.isRightClick() ? (stored.amount() + 1) / 2 : stored.amount()));
            return;
        }

        StoredItem held = StoredItem.of(cursor);
        if (held == null || !held.item().equals(stored.item())) return;

        int room = held.maxStackSize() - held.amount();
        StoredItem taken = oven.takeOutput(index, Math.min(room, stored.amount()));
        if (taken == null) return;

        ItemStack grown = cursor.clone();
        grown.setAmount(cursor.getAmount() + taken.amount());
        event.getView().setCursor(grown);
    }

    private void giveTaken(Player player, StoredItem taken) {
        ItemStack stack = taken == null ? null : taken.toItemStack();
        if (stack != null) PlayerUtils.giveItem(player, stack);
    }

    private void setCursor(InventoryClickEvent event, StoredItem taken) {
        event.getView().setCursor(taken == null ? null : taken.toItemStack());
    }

    private void shrinkCursor(InventoryClickEvent event, ItemStack cursor, int accepted) {
        if (accepted <= 0) return;

        ItemStack remaining = cursor.clone();
        remaining.setAmount(cursor.getAmount() - accepted);
        event.getView().setCursor(remaining.getAmount() > 0 ? remaining : null);
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }
        if (!event.isShiftClick()) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        int accepted = oven.insertAnyInput(clicked);
        if (accepted <= 0) return;

        ItemStack remaining = clicked.clone();
        remaining.setAmount(clicked.getAmount() - accepted);
        event.setCurrentItem(remaining.getAmount() > 0 ? remaining : null);
    }

    @Override
    public void onDragInventory(InventoryDragEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onCloseInventory(InventoryCloseEvent event) {
        Utils.runLater(() -> {
            if (getInventory().getViewers().isEmpty()) oven.releaseGui(this);
        });
    }

    public void closeAll() {
        for (HumanEntity viewer : List.copyOf(getInventory().getViewers())) viewer.closeInventory();
    }
}
