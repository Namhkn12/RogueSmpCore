package com.roguesmp.block.gui;

import com.roguesmp.block.impl.brewing.BrewingBlock;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BrewingGui extends MachineGui {

    private static final int[] INGREDIENT_SLOTS = {10, 11, 12, 19, 20, 21};
    private static final int[] BOTTLE_SLOTS = {37, 38, 39};
    private static final int START_SLOT = 14;
    private static final int PROGRESS_SLOT = 16;
    private static final int ENERGY_SLOT = 25;
    private static final int SIDES_SLOT = 44;

    private final BrewingBlock brewer;

    public BrewingGui(BrewingBlock brewer) {
        super(Component.text("Bàn pha chế"), 5, brewer.itemStorage(), brewer::openGui);
        this.brewer = brewer;
    }

    @Override
    public void refresh() {
        for (int i = 0; i < INGREDIENT_SLOTS.length; i++) {
            bindSlot(INGREDIENT_SLOTS[i], brewer.ingredients(), i, placeholder(Material.ORANGE_STAINED_GLASS_PANE, "Nguyên liệu", NamedTextColor.GOLD, "Đặt nguyên liệu pha chế vào đây"));
        }
        for (int i = 0; i < BOTTLE_SLOTS.length; i++) {
            int index = i;
            addButton(BOTTLE_SLOTS[i], bottleDisplay(i), event -> onBottleClick(event, index));
        }
        addButton(PROGRESS_SLOT, progressDisplay(), ClickHandler.noAction());
        addButton(START_SLOT, startButton(), this::onStartClick);
        addButton(ENERGY_SLOT, energyDisplay(), ClickHandler.noAction());
        addSidesButton(SIDES_SLOT);
    }

    @Override
    protected void onAllViewersClosed() {
        brewer.releaseGui(this);
    }

    private ItemStack bottleDisplay(int index) {
        ItemStack stack = brewer.bottleStack(index);
        return stack != null ? stack : placeholder(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "Bình nước", NamedTextColor.AQUA, "Đặt bình nước vào đây, thuốc sẽ thay thế nó");
    }

    private void onBottleClick(InventoryClickEvent event, int index) {
        event.setCancelled(true);
        ItemStack cursor = event.getView().getCursor();

        if (brewer.bottleStack(index) != null) {
            if (event.isShiftClick()) {
                ItemStack taken = brewer.takeBottle(index);
                if (taken != null) PlayerUtils.giveItem((Player) event.getWhoClicked(), taken);
            } else if (cursor.getType().isAir()) {
                event.getView().setCursor(brewer.takeBottle(index));
            }
            return;
        }

        if (BrewingBlock.isWaterBottle(cursor) && brewer.insertWater(index)) {
            event.getView().setCursor(cursor.getAmount() > 1 ? cursor.asQuantity(cursor.getAmount() - 1) : null);
        }
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (!event.isShiftClick() || !BrewingBlock.isWaterBottle(clicked)) {
            super.onClickBottomInventory(event);
            return;
        }

        event.setCancelled(true);
        int remaining = clicked.getAmount();
        for (int i = 0; i < BrewingBlock.BOTTLE_COUNT && remaining > 0; i++) {
            if (brewer.insertWater(i)) remaining--;
        }
        event.setCurrentItem(remaining > 0 ? clicked.asQuantity(remaining) : null);
    }

    private ItemStack startButton() {
        if (brewer.isBrewing()) {
            return placeholder(Material.RED_CONCRETE, "Hủy pha chế", NamedTextColor.RED, "Nguyên liệu sẽ không bị tiêu hao");
        }
        BrewingBlock.BrewStatus status = brewer.status();
        if (status == BrewingBlock.BrewStatus.READY) {
            return placeholder(Material.LIME_CONCRETE, "Bắt đầu pha chế", NamedTextColor.GREEN, "Nhấn để bắt đầu pha chế");
        }
        ItemStack item = ItemStack.of(Material.GRAY_CONCRETE);
        ItemStackUtils.setItemName(item, Utils.text("Bắt đầu pha chế", NamedTextColor.GRAY));
        ItemStackUtils.setLore(item, List.of(Utils.text(statusText(status), NamedTextColor.RED)));
        return item;
    }

    private void onStartClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (brewer.isBrewing()) brewer.cancelBrewing();
        else brewer.startBrewing();
    }

    private ItemStack progressDisplay() {
        BrewingBlock.BrewStatus status = brewer.status();
        if (!brewer.isBrewing()) {
            ItemStack item = ItemStack.of(Material.GRAY_STAINED_GLASS_PANE);
            ItemStackUtils.setItemName(item, Utils.text("Đang chờ", NamedTextColor.GRAY));
            ItemStackUtils.setLore(item, List.of(Utils.text(statusText(status), status == BrewingBlock.BrewStatus.READY ? NamedTextColor.GREEN : NamedTextColor.RED)));
            return item;
        }

        int duration = brewer.data().brewTicks();
        int percent = Math.min(100, brewer.progress() * 100 / Math.max(1, duration));
        ItemStack item = gauge(Material.BLAZE_POWDER, percent);
        ItemStackUtils.setItemName(item, Utils.text("Đang pha chế: " + percent + "%", NamedTextColor.YELLOW));
        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text("Thời gian: ", NamedTextColor.YELLOW).append(Component.text(
                Utils.formatDecimal(brewer.progress() / 20d) + "s / " + Utils.formatDecimal(duration / 20d) + "s", NamedTextColor.GRAY)));
        if (status == BrewingBlock.BrewStatus.NO_ENERGY) lore.add(Utils.text("Tạm dừng: " + statusText(status), NamedTextColor.RED));
        ItemStackUtils.setLore(item, lore);
        return item;
    }

    private static String statusText(BrewingBlock.BrewStatus status) {
        return switch (status) {
            case INVALID_POTION -> "Vật phẩm thuốc chưa được cấu hình";
            case NO_BOTTLE -> "Thiếu bình nước";
            case NO_INGREDIENTS -> "Chưa có nguyên liệu";
            case UNSTABLE -> "Hỗn hợp không ổn định: thời gian hiệu lực quá ngắn";
            case NO_ENERGY -> "Không đủ năng lượng";
            case OUTPUT_FULL -> "Hãy lấy thành phẩm ra";
            case READY -> "Sẵn sàng pha chế";
            case BREWING -> "";
        };
    }

    private ItemStack energyDisplay() {
        int percent = brewer.getEnergy() * 100 / Math.max(1, brewer.getMaxEnergy());
        ItemStack item = gauge(Material.GLOWSTONE_DUST, percent);
        ItemStackUtils.setItemName(item, Utils.text("Năng lượng: " + percent + "%", NamedTextColor.YELLOW));
        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text("Lưu trữ: ", NamedTextColor.YELLOW).append(Component.text(Utils.formatMoney(brewer.getEnergy()) + "/" + Utils.formatMoney(brewer.getMaxEnergy()), NamedTextColor.GRAY)));
        lore.add(Utils.text("Tiêu thụ: ", NamedTextColor.YELLOW).append(Component.text(brewer.data().energyPerTick() + "/tick", NamedTextColor.GRAY)));
        ItemStackUtils.setLore(item, lore);
        return item;
    }

    private static ItemStack placeholder(Material material, String name, NamedTextColor color, String hint) {
        ItemStack item = ItemStack.of(material);
        ItemStackUtils.setItemName(item, Utils.text(name, color));
        ItemStackUtils.setLore(item, List.of(Utils.text(hint, NamedTextColor.DARK_GRAY)));
        return item;
    }

    private static ItemStack gauge(Material material, int percent) {
        ItemStack item = ItemStack.of(material);
        item.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
        item.setData(DataComponentTypes.MAX_DAMAGE, 100);
        item.setData(DataComponentTypes.DAMAGE, 100 - Math.clamp(percent, 0, 100));
        return item;
    }
}
