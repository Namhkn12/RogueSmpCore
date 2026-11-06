package com.roguesmp.block.gui;

import com.roguesmp.block.StoredItem;
import com.roguesmp.block.storage.FixedSlots;
import com.roguesmp.block.upgrade.ActiveCatalyst;
import com.roguesmp.block.upgrade.UpgradeInstallResult;
import com.roguesmp.block.upgrade.UpgradeType;
import com.roguesmp.block.upgrade.UpgradeSet;
import com.roguesmp.gui.BaseGui.ClickHandler;
import com.roguesmp.item.component.impl.BlockUpgradeComponent;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public final class UpgradePanel {

    private final MachineGui gui;
    private final UpgradeSet upgrades;
    private final Runnable onChanged;

    public UpgradePanel(MachineGui gui, UpgradeSet upgrades, Runnable onChanged) {
        this.gui = gui;
        this.upgrades = upgrades;
        this.onChanged = onChanged;
    }

    public void renderModules(FixedSlots modules, int[] guiSlots, Function<String, UpgradeInstallResult> check, Predicate<String> valid) {
        String name = UpgradeType.MODULE.displayName();
        for (int i = 0; i < guiSlots.length; i++) {
            if (i >= modules.size()) {
                gui.addButton(guiSlots[i], ItemStackUtils.hideTooltip(ItemStack.of(Material.BLACK_STAINED_GLASS_PANE)), ClickHandler.noAction());
                continue;
            }

            int index = i;
            StoredItem stored = modules.get(i);
            ItemStack display = stored == null ? null : moduleDisplay(stored.item(), valid.test(stored.item()));
            gui.addButton(guiSlots[i], display != null ? display : placeholder(Material.PURPLE_STAINED_GLASS_PANE, "Ô " + name, "Đặt " + name.toLowerCase() + " vào đây"),
                    event -> onModuleClick(event, modules, index, check));
        }
    }

    public void renderCatalyst(FixedSlots feed, Predicate<String> valid, @Nullable ActiveCatalyst active, boolean paused, Runnable clear, int inputGuiSlot, int activeGuiSlot) {
        String name = UpgradeType.CATALYST.displayName();
        StoredItem stored = feed.get(0);
        ItemStack display = stored != null ? stored.toItemStack() : null;
        if (display != null && !valid.test(stored.item())) appendLore(display, Utils.text("Không còn là " + name.toLowerCase() + ", không có tác dụng", NamedTextColor.RED));
        gui.bindSlotDisplay(inputGuiSlot, feed, 0, display != null ? display : placeholder(Material.LIME_STAINED_GLASS_PANE, name, "Đặt " + name.toLowerCase() + " vào đây"));

        ItemStack activeDisplay = active == null ? null : activeDisplay(active, paused);
        gui.addButton(activeGuiSlot, activeDisplay != null ? activeDisplay : placeholder(Material.GRAY_STAINED_GLASS_PANE, "Hiệu ứng " + name.toLowerCase(), "Chưa có hiệu ứng nào"),
                event -> onActiveClick(event, active, clear));
    }

    public void renderSummary(int guiSlot) {
        ItemStack item = ItemStack.of(Material.BEACON);
        ItemStackUtils.setItemName(item, Utils.text("Hiệu ứng đang hoạt động", NamedTextColor.GOLD));
        List<Component> lore = new ArrayList<>(upgrades.describe());
        if (lore.isEmpty()) lore.add(Utils.text("Chưa có hiệu ứng nào", NamedTextColor.DARK_GRAY));
        ItemStackUtils.setLore(item, lore);
        gui.addButton(guiSlot, item, ClickHandler.noAction());
    }

    private @Nullable ItemStack moduleDisplay(String itemId, boolean valid) {
        ItemStack display = new StoredItem(itemId, 1).toItemStack();
        if (display == null) return null;

        String name = UpgradeType.MODULE.displayName().toLowerCase();
        if (!valid) appendLore(display, Utils.text("Không còn là " + name + ", không có tác dụng", NamedTextColor.RED));
        appendLore(display, Utils.text("-------"));
        appendLore(display, Utils.fromString("<!i><gray><key:key.attack> để gỡ " + name));
        return display;
    }

    private @Nullable ItemStack activeDisplay(ActiveCatalyst active, boolean paused) {
        BlockUpgradeComponent component = active.component();
        ItemStack display = new StoredItem(active.item(), 1).toItemStack();
        if (component == null || display == null) return null;

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text("Hiệu ứng " + UpgradeType.CATALYST.displayName().toLowerCase() + " đang chạy", NamedTextColor.GOLD));
        lore.addAll(component.effect().describe());
        lore.add(Utils.text("Còn lại: " + component.unit().format(active.remaining()), NamedTextColor.GRAY));
        if (paused) lore.add(Utils.text("Tạm dừng: máy không hoạt động", NamedTextColor.RED));
        lore.add(Utils.text("Shift + chuột phải để hủy hiệu ứng", NamedTextColor.YELLOW));
        ItemStackUtils.setLore(display, lore);
        return display;
    }

    private void onModuleClick(InventoryClickEvent event, FixedSlots modules, int index, Function<String, UpgradeInstallResult> check) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack cursor = event.getView().getCursor();
        StoredItem stored = modules.get(index);

        if (cursor.getType().isAir()) {
            if (stored != null) remove(player, modules, index);
        } else {
            install(event, player, modules, index, stored, cursor, check);
        }
        onChanged.run();
    }

    private void remove(Player player, FixedSlots modules, int index) {
        StoredItem removed = modules.extract(index, 1, false);
        ItemStack stack = removed == null ? null : removed.toItemStack();
        if (stack == null) return;

        PlayerUtils.giveItem(player, stack);
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 0.8f);
    }

    private void install(InventoryClickEvent event, Player player, FixedSlots modules, int index, @Nullable StoredItem stored, ItemStack cursor, Function<String, UpgradeInstallResult> check) {
        StoredItem held = StoredItem.of(cursor);
        UpgradeInstallResult result = stored != null ? UpgradeInstallResult.OCCUPIED
                : held == null ? UpgradeInstallResult.NOT_AN_UPGRADE
                : check.apply(held.item());
        if (result == UpgradeInstallResult.INSTALLED && modules.insert(index, held.withAmount(1), false) == 1) {
            ItemStack remaining = cursor.clone();
            remaining.setAmount(cursor.getAmount() - 1);
            event.getView().setCursor(remaining.getAmount() > 0 ? remaining : null);
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.5f);
            return;
        }

        player.sendMessage(Utils.text(failureMessage(result, UpgradeType.MODULE.displayName().toLowerCase()), NamedTextColor.RED));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }

    private void onActiveClick(InventoryClickEvent event, @Nullable ActiveCatalyst active, Runnable clear) {
        event.setCancelled(true);
        if (event.getClick() != ClickType.SHIFT_RIGHT || active == null) return;

        clear.run();
        Player player = (Player) event.getWhoClicked();
        player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1f, 1f);
        onChanged.run();
    }

    private static String failureMessage(UpgradeInstallResult result, String name) {
        return switch (result) {
            case NOT_AN_UPGRADE -> "Vật phẩm này không phải " + name + ".";
            case NOT_ELIGIBLE -> "Vật phẩm này không dùng được cho máy này.";
            case WRONG_TYPE -> "Vật phẩm này không lắp vào ô " + name + ".";
            case OCCUPIED -> "Ô " + name + " này đã có vật phẩm.";
            case INCOMPATIBLE -> "Vật phẩm này xung đột với " + name + " đã lắp.";
            case LIMIT_REACHED -> "Đã đạt số lượng tối đa của " + name + " này.";
            case INSTALLED -> "Không thể lắp " + name + " vào lúc này.";
        };
    }

    private static ItemStack placeholder(Material material, String title, String hint) {
        ItemStack item = ItemStack.of(material);
        ItemStackUtils.setItemName(item, Utils.text(title, NamedTextColor.GRAY));
        ItemStackUtils.setLore(item, List.of(Utils.text(hint, NamedTextColor.DARK_GRAY)));
        return item;
    }

    private static void appendLore(ItemStack item, Component line) {
        List<Component> lore = new ArrayList<>(item.lore() != null ? item.lore() : List.of());
        lore.add(line);
        ItemStackUtils.setLore(item, lore);
    }
}
