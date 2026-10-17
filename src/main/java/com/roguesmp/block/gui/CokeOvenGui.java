package com.roguesmp.block.gui;

import com.roguesmp.block.StoredItem;
import com.roguesmp.block.impl.cokeoven.CokeOvenBlock;
import com.roguesmp.crafting.CraftingIngredient;
import com.roguesmp.crafting.recipe.CokeOvenRecipe;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CokeOvenGui extends MachineGui {

    private static final int[] INPUT_SLOTS = {10, 11};
    private static final int[] OUTPUT_SLOTS = {15, 16};
    private static final int PROGRESS_SLOT = 13;
    private static final int RESULT_PREVIEW_SLOT = 22;
    private static final int SIDES_SLOT = 26;

    private final CokeOvenBlock oven;

    public CokeOvenGui(CokeOvenBlock oven) {
        super(Component.text("Coke Oven"), 3, oven.itemStorage(), oven::openGui);
        this.oven = oven;
    }

    @Override
    public void refresh() {
        for (int i = 0; i < INPUT_SLOTS.length; i++) bindSlot(INPUT_SLOTS[i], oven.inputs(), i, inputPlaceholder());
        for (int i = 0; i < OUTPUT_SLOTS.length; i++) bindSlot(OUTPUT_SLOTS[i], oven.outputs(), i, outputPlaceholder());
        addButton(PROGRESS_SLOT, progressDisplay(), ClickHandler.noAction());
        addButton(RESULT_PREVIEW_SLOT, resultPreview(), this::onPreviewClick);
        addSidesButton(SIDES_SLOT);
    }

    @Override
    protected void onAllViewersClosed() {
        oven.releaseGui(this);
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
        CokeOvenRecipe active = oven.activeRecipe();
        CokeOvenRecipe remembered = oven.rememberedRecipe();
        CokeOvenRecipe recipe = active != null ? active : remembered;
        ItemStack preview = recipe == null || recipe.getResults().isEmpty() ? null : recipe.getResults().getFirst().toItemStack();
        if (preview == null) {
            ItemStack item = ItemStack.of(Material.GRAY_STAINED_GLASS_PANE);
            ItemStackUtils.setItemName(item, Utils.text("Thành phẩm dự kiến", NamedTextColor.GRAY));
            ItemStackUtils.setLore(item, List.of(Utils.text("Chưa có công thức", NamedTextColor.DARK_GRAY)));
            return item;
        }

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text(active != null ? "Đang chế tạo:" : "Công thức ghi nhớ:", NamedTextColor.GOLD));
        for (CraftingIngredient result : recipe.getResults()) {
            StoredItem stored = new StoredItem(result.key(), result.count());
            lore.add(Utils.text("- " + result.count() + "x ", NamedTextColor.GRAY).append(stored.displayName()));
        }
        if (remembered != null) {
            lore.add(Utils.text("-------"));
            lore.add(Utils.text("Kéo thông minh sẽ nạp nguyên liệu theo công thức này", NamedTextColor.DARK_GRAY));
            lore.add(Utils.fromString("<!i><gray><key:key.attack> để xóa công thức ghi nhớ"));
        }
        ItemStackUtils.setLore(preview, lore);
        return preview;
    }

    private void onPreviewClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (oven.rememberedRecipe() != null) oven.forgetRecipe();
    }
}
