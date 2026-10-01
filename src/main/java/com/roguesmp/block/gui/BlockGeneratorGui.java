package com.roguesmp.block.gui;

import com.roguesmp.block.impl.BlockGenerator;
import com.roguesmp.block.StoredItem;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BlockGeneratorGui extends BaseGui {

    private static final int MAIN_INFO_SLOT = 4;
    private static final int STATUS_SLOT = 5;
    private static final int TAKE_ALL_SLOT = 34;
    private static final int FUEL_SLOT = 10;
    private static final int[] LOOT_SLOTS = {12, 13, 14, 15, 16, 21, 22, 23, 24, 25};

    private final BlockGenerator generator;

    public BlockGeneratorGui(BlockGenerator generator) {
        super(Utils.text("Generator", NamedTextColor.DARK_GRAY), 4);
        this.generator = generator;
    }

    @Override
    public void setup() {
        fillEmpty(FILLER_BLACK);
        renderMainInfo();
        renderStatus();
        renderTakeAllButton();
        renderFuelSlot();
        renderLootSlots();
    }

    private void renderMainInfo() {
        ItemStack item = ItemStack.of(Material.STONE);
        item.setData(DataComponentTypes.ITEM_MODEL, generator.getType().model());
        ItemStackUtils.setItemName(item, Utils.fromString(generator.getType().displayName()));
        List<Component> infoLore = new ArrayList<>();
        infoLore.add(Utils.text("Năng lượng: ", NamedTextColor.YELLOW).append(Component.text(generator.getEnergy() + " / " + generator.getMaxEnergy(), NamedTextColor.GRAY)));
        infoLore.add(Utils.text("Sức chứa: ", NamedTextColor.YELLOW).append(Component.text(generator.getOccupiedCapacity() + " / " + generator.getLootCapacity(), NamedTextColor.GRAY)));
        infoLore.add(Utils.text("Thời gian đặt: ", NamedTextColor.YELLOW).append(Component.text(Utils.formatDecimal(generator.getPlaceDelayTicks() / 20d) + "s", NamedTextColor.GRAY)));
        infoLore.add(Utils.text("Thời gian khai thác: ", NamedTextColor.YELLOW).append(Component.text(Utils.formatDecimal(generator.getBreakDelayTicks() / 20d) + "s", NamedTextColor.GRAY)));
        infoLore.add(Utils.text("Năng lượng/tick: ", NamedTextColor.YELLOW).append(Component.text(generator.getEnergyPerTick(), NamedTextColor.GRAY)));
        ItemStackUtils.setLore(item, infoLore);
        addButton(MAIN_INFO_SLOT, item, ClickHandler.noAction());
    }

    /** Dynamic, separate from {@link #renderMainInfo}'s static properties - this is the machine's current activity, not its configuration. */
    private void renderStatus() {
        Material wool;
        Component name;
        List<Component> lore = new ArrayList<>();

        if (generator.getEnergy() <= 0) {
            wool = Material.RED_WOOL;
            name = Utils.text("Hết năng lượng", NamedTextColor.RED);
        } else if (generator.isMining()) {
            wool = Material.LIME_WOOL;
            name = Utils.text("Đang khai thác", NamedTextColor.GREEN);
            int elapsed = generator.getBreakDelayTicks() - generator.getBreakTicksLeft();
            lore.add(Utils.text("Tiến độ: " + elapsed + " / " + generator.getBreakDelayTicks() + " tick", NamedTextColor.GRAY));
        } else {
            wool = Material.YELLOW_WOOL;
            name = Utils.text("Đang chờ", NamedTextColor.YELLOW);
        }

        ItemStack item = ItemStack.of(wool);
        ItemStackUtils.setItemName(item, name);
        ItemStackUtils.setLore(item, lore);
        addButton(STATUS_SLOT, item, ClickHandler.noAction());
    }

    private void renderTakeAllButton() {
        ItemStack item = ItemStack.of(Material.CHEST);
        ItemStackUtils.setItemName(item, Utils.text("Lấy hết", NamedTextColor.GOLD));
        ItemStackUtils.setLore(item, List.of(Utils.text("Lấy đến khi đầy túi thì thôi", NamedTextColor.GRAY)));
        addButton(TAKE_ALL_SLOT, item, this::onTakeAllClick);
    }

    private void renderFuelSlot() {
        StoredItem fuel = generator.getFuel();
        ItemStack display = fuel != null ? fuel.toItemStack() : null;
        addButton(FUEL_SLOT, display != null ? display : fuelPlaceholder(), this::onFuelClick);
    }

    private void renderLootSlots() {
        List<StoredItem> items = generator.getLoot();
        for (int i = 0; i < LOOT_SLOTS.length; i++) {
            if (i < items.size()) addButton(LOOT_SLOTS[i], lootDisplayItem(items.get(i)), this::onLootClick);
            else addButton(LOOT_SLOTS[i], FILLER, ClickHandler.noAction());
        }
    }

    private ItemStack lootDisplayItem(StoredItem stored) {
        ItemStack display = stored.withAmount(Math.min(stored.amount(), stored.maxStackSize())).toItemStack();
        if (display == null) return FILLER;

        Component name = display.getData(DataComponentTypes.CUSTOM_NAME);
        if (name != null) {
            Component newName = name.append(Component.text(" (x" + Utils.formatMoney(stored.amount()) + ")", NamedTextColor.GRAY));
            display.setData(DataComponentTypes.ITEM_NAME, newName);
        }
        List<Component> displayLore = new ArrayList<>();
        displayLore.add(Utils.text("-------"));
        displayLore.add(Utils.fromString("<!i><gray><key:key.attack> để lấy full stack"));
        displayLore.add(Utils.fromString("<!i><gray><key:key.use> để lấy 1"));
        ItemStackUtils.setLore(display, displayLore);
        return display;
    }

    private ItemStack fuelPlaceholder() {
        ItemStack item = ItemStack.of(Material.LIME_STAINED_GLASS_PANE);
        ItemStackUtils.setItemName(item, Utils.text("Nguyên liệu", NamedTextColor.GRAY));
        ItemStackUtils.setLore(item, List.of(Utils.text("Đặt nguyên liệu vào đây", NamedTextColor.DARK_GRAY)));
        return item;
    }

    private void onFuelClick(InventoryClickEvent event) {
        event.setCancelled(true);

        ItemStack cursor = event.getView().getCursor();
        boolean cursorEmpty = cursor.getType().isAir();
        StoredItem currentFuel = generator.getFuel();
        if (currentFuel == null && cursorEmpty) return;

        Player player = (Player) event.getWhoClicked();
        if (cursorEmpty) {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
        } else {
            player.playSound(player.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1f, 1.2f);
        }

        generator.setFuel(cursorEmpty ? null : StoredItem.of(cursor));
        event.getView().setCursor(currentFuel != null ? currentFuel.toItemStack() : null);
        renderFuelSlot();
    }

    private void onLootClick(InventoryClickEvent event) {
        event.setCancelled(true);

        int index = lootIndex(event.getSlot());
        if (index < 0) return;

        List<StoredItem> items = generator.getLoot();
        if (index >= items.size()) return;

        StoredItem stored = items.get(index);
        int takeAmount;
        if (event.isRightClick()) {
            takeAmount = 1;
        } else takeAmount = Math.min(stored.amount(), stored.maxStackSize());

        StoredItem taken = generator.takeLoot(stored.item(), takeAmount);
        ItemStack takenStack = taken != null ? taken.toItemStack() : null;
        if (takenStack == null) return;

        Player player = (Player) event.getWhoClicked();
        PlayerUtils.giveItem(player, takenStack);
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
        renderLootSlots();
    }

    /**
     * Takes as much of every stored type as the clicker's inventory can actually hold.
     */
    private void onTakeAllClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        boolean tookAnything = false;

        for (StoredItem stored : generator.getLoot()) {
            int maxStack = stored.maxStackSize();
            int remaining = stored.amount();
            int taken = 0;

            while (remaining > 0) {
                int chunkAmount = Math.min(maxStack, remaining);
                ItemStack chunk = stored.withAmount(chunkAmount).toItemStack();
                if (chunk == null) break;

                Map<Integer, ItemStack> leftover = player.getInventory().addItem(chunk);
                int notTaken = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
                taken += chunkAmount - notTaken;
                remaining -= chunkAmount;

                if (notTaken > 0) break;
            }

            if (taken > 0) {
                generator.takeLoot(stored.item(), taken);
                tookAnything = true;
            }
        }

        if (tookAnything) player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 0.8f);
        renderLootSlots();
    }

    private int lootIndex(int slot) {
        for (int i = 0; i < LOOT_SLOTS.length; i++) {
            if (LOOT_SLOTS[i] == slot) return i;
        }
        return -1;
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        if (event.isShiftClick()) event.setCancelled(true);
    }

    @Override
    public void onDragInventory(InventoryDragEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onCloseInventory(InventoryCloseEvent event) {
        Utils.runLater(() -> {
            if (getInventory().getViewers().isEmpty()) generator.releaseGui(this);
        });
    }

    public void refresh() {
        renderMainInfo();
        renderStatus();
        renderFuelSlot();
        renderLootSlots();
    }
}
