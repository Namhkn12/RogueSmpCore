package com.roguesmp.block.gui;

import com.roguesmp.block.StoredItem;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.item.component.impl.GeneratorFuelComponent;
import com.roguesmp.item.component.impl.GeneratorModuleComponent;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import com.roguesmp.block.impl.generator.part.ActiveBurn;
import com.roguesmp.block.impl.generator.part.BurnUnit;
import com.roguesmp.block.impl.generator.behavior.GeneratorBehavior;
import com.roguesmp.block.impl.generator.stat.StatModifier;
import com.roguesmp.block.impl.generator.part.ModuleInstallResult;
import com.roguesmp.block.impl.generator.ResourceGeneratorBlock;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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

public class ResourceGeneratorGui extends BaseGui {

    private static final int MAIN_INFO_SLOT = 4;
    private static final int STATUS_SLOT = 5;
    private static final int BUFF_SUMMARY_SLOT = 6;
    private static final int TAKE_ALL_SLOT = 34;
    private static final int FUEL_SLOT = 10;
    private static final int BURN_SLOT = 19;
    private static final int[] LOOT_SLOTS = {12, 13, 14, 15, 16, 21, 22, 23, 24, 25};
    private static final int[] MODULE_SLOTS = {28, 29, 30, 31, 32};

    private final ResourceGeneratorBlock generator;

    public ResourceGeneratorGui(ResourceGeneratorBlock generator) {
        super(Component.text("Resource Generator"), 4);
        this.generator = generator;
    }

    @Override
    public void setup() {
        fillEmpty(FILLER_BLACK);
        renderMainInfo();
        renderStatus();
        renderTakeAllButton();
        renderFuelSlot();
        renderBurnSlot();
        renderBuffSummary();
        renderLootSlots();
        renderModuleSlots();
    }

    private void renderMainInfo() {
        ItemStack item = ItemStack.of(Material.STONE);
        item.setData(DataComponentTypes.ITEM_MODEL, generator.getProperties().model());
        ItemStackUtils.setItemName(item, Utils.fromString(generator.getProperties().displayName()));
        List<Component> infoLore = new ArrayList<>();
        infoLore.add(Utils.text("Năng lượng: ", NamedTextColor.YELLOW).append(Component.text(generator.getEnergy() + "/" + generator.getMaxEnergy(), NamedTextColor.GRAY)));
        infoLore.add(Utils.text("Sức chứa: ", NamedTextColor.YELLOW).append(Component.text(generator.loot().occupied() + "/" + generator.stats().lootCapacity(), NamedTextColor.GRAY)));
        infoLore.add(Utils.text("Thời gian đặt: ", NamedTextColor.YELLOW).append(Component.text(Utils.formatDecimal(generator.stats().placeDelayTicks() / 20d) + "s", NamedTextColor.GRAY)));
        infoLore.add(Utils.text("Thời gian khai thác: ", NamedTextColor.YELLOW).append(Component.text(Utils.formatDecimal(generator.stats().breakDelayTicks() / 20d) + "s", NamedTextColor.GRAY)));
        infoLore.add(Utils.text("Năng lượng/tick: ", NamedTextColor.YELLOW).append(Component.text(Utils.formatDecimal(generator.stats().energyPerTick()), NamedTextColor.GRAY)));
        infoLore.add(Utils.text("Module: ", NamedTextColor.YELLOW).append(Component.text(generator.modules().items().size() + "/" + generator.modules().capacity(), NamedTextColor.GRAY)));
        ItemStackUtils.setLore(item, infoLore);
        addButton(MAIN_INFO_SLOT, item, ClickHandler.noAction());
    }

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
            int elapsed = generator.stats().breakDelayTicks() - generator.getBreakTicksLeft();
            lore.add(Utils.text("Tiến độ: " + elapsed + " / " + generator.stats().breakDelayTicks() + " tick", NamedTextColor.GRAY));
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
        StoredItem fuel = generator.fuel().slot();
        ItemStack display = fuel != null ? fuel.toItemStack() : null;
        if (display != null && GeneratorFuelComponent.of(fuel.item()) == null) withWarning(display, "Không còn là nhiên liệu, không có tác dụng");
        addButton(FUEL_SLOT, display != null ? display : fuelPlaceholder(), this::onFuelClick);
    }

    private void withWarning(ItemStack item, String warning) {
        List<Component> lore = new ArrayList<>(item.lore() != null ? item.lore() : List.of());
        lore.add(Utils.text(warning, NamedTextColor.RED));
        ItemStackUtils.setLore(item, lore);
    }

    private void renderLootSlots() {
        List<StoredItem> items = generator.loot().entries();
        for (int i = 0; i < LOOT_SLOTS.length; i++) {
            ItemStack filler = ItemStackUtils.hideTooltip(ItemStack.of(Material.WHITE_STAINED_GLASS_PANE));
            if (i < items.size()) addButton(LOOT_SLOTS[i], lootDisplayItem(items.get(i)), this::onLootClick);
            else addButton(LOOT_SLOTS[i], filler, ClickHandler.noAction());
        }
    }

    private void renderModuleSlots() {
        List<String> modules = generator.modules().items();
        int slots = Math.min(generator.modules().capacity(), MODULE_SLOTS.length);
        for (int i = 0; i < MODULE_SLOTS.length; i++) {
            if (i >= slots) {
                addButton(MODULE_SLOTS[i], FILLER_BLACK, ClickHandler.noAction());
                continue;
            }

            ItemStack display = i < modules.size() ? moduleDisplayItem(modules.get(i)) : null;
            addButton(MODULE_SLOTS[i], display != null ? display : modulePlaceholder(), this::onModuleClick);
        }
    }

    private ItemStack moduleDisplayItem(String moduleId) {
        ItemStack display = new StoredItem(moduleId, 1).toItemStack();
        if (display == null) return modulePlaceholder();
        if (GeneratorModuleComponent.of(moduleId) == null) withWarning(display, "Không còn là module, không có tác dụng");

        List<Component> lore = new ArrayList<>(display.lore() != null ? display.lore() : List.of());
        lore.add(Utils.text("-------"));
        lore.add(Utils.fromString("<!i><gray><key:key.attack> để gỡ module"));
        ItemStackUtils.setLore(display, lore);
        return display;
    }

    private ItemStack modulePlaceholder() {
        ItemStack item = ItemStack.of(Material.PURPLE_STAINED_GLASS_PANE);
        ItemStackUtils.setItemName(item, Utils.text("Ô module", NamedTextColor.LIGHT_PURPLE));
        ItemStackUtils.setLore(item, List.of(Utils.text("Đặt module vào đây", NamedTextColor.DARK_GRAY)));
        return item;
    }

    private ItemStack lootDisplayItem(StoredItem stored) {
        ItemStack display = stored.withAmount(Math.min(stored.amount(), stored.maxStackSize())).toItemStack();
        if (display == null) return FILLER;

        Component name = display.getData(DataComponentTypes.CUSTOM_NAME);
        if (name != null) {
            Component newName = name.append(Component.text(" (x" + Utils.formatMoney(stored.amount()) + ")", NamedTextColor.GRAY));
            display.setData(DataComponentTypes.CUSTOM_NAME, newName.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        } else {
            Component oldName = display.getData(DataComponentTypes.ITEM_NAME);
            if (oldName != null) {
                Component newName = oldName.append(Component.text(" (x" + Utils.formatMoney(stored.amount()) + ")", NamedTextColor.GRAY));
                display.setData(DataComponentTypes.CUSTOM_NAME, newName.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            }
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

    private boolean isFuel(ItemStack stack) {
        StoredItem stored = StoredItem.of(stack);
        return stored != null && GeneratorFuelComponent.of(stored.item()) != null;
    }

    private void renderBuffSummary() {
        ItemStack item = ItemStack.of(Material.BEACON);
        ItemStackUtils.setItemName(item, Utils.text("Hiệu ứng đang hoạt động", NamedTextColor.GOLD));

        List<Component> lore = new ArrayList<>();
        for (StatModifier modifier : generator.stats().summary()) lore.add(modifier.describe());
        for (GeneratorBehavior behavior : generator.stats().behaviors()) lore.addAll(behavior.getDisplay());
        if (lore.isEmpty()) lore.add(Utils.text("Chưa có hiệu ứng nào", NamedTextColor.DARK_GRAY));
        ItemStackUtils.setLore(item, lore);
        addButton(BUFF_SUMMARY_SLOT, item, ClickHandler.noAction());
    }

    private void renderBurnSlot() {
        ActiveBurn burn = generator.fuel().burn();
        GeneratorFuelComponent fuel = generator.fuel().burningComponent();
        ItemStack display = burn != null && fuel != null ? burnDisplayItem(burn, fuel) : null;
        addButton(BURN_SLOT, display != null ? display : burnPlaceholder(), ClickHandler.noAction());
    }

    private ItemStack burnDisplayItem(ActiveBurn burn, GeneratorFuelComponent fuel) {
        ItemStack display = new StoredItem(burn.item(), 1).toItemStack();
        if (display == null) return null;

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text("Hiệu ứng nhiên liệu đang chạy", NamedTextColor.GOLD));
        lore.addAll(fuel.effect().describe());
        lore.add(Utils.text("Còn lại: " + fuel.unit().format(burn.remaining()), NamedTextColor.GRAY));
        if (generator.getEnergy() <= 0 && fuel.unit() == BurnUnit.TICKS) {
            lore.add(Utils.text("Tạm dừng: hết năng lượng", NamedTextColor.RED));
        }
        ItemStackUtils.setLore(display, lore);
        return display;
    }

    private ItemStack burnPlaceholder() {
        ItemStack item = ItemStack.of(Material.GRAY_STAINED_GLASS_PANE);
        ItemStackUtils.setItemName(item, Utils.text("Hiệu ứng nhiên liệu", NamedTextColor.GRAY));
        ItemStackUtils.setLore(item, List.of(Utils.text("Chưa có hiệu ứng nào", NamedTextColor.DARK_GRAY)));
        return item;
    }

    private void onFuelClick(InventoryClickEvent event) {
        event.setCancelled(true);

        ItemStack cursor = event.getView().getCursor();
        boolean cursorEmpty = cursor.getType().isAir();
        StoredItem currentFuel = generator.fuel().slot();
        if (currentFuel == null && cursorEmpty) return;

        Player player = (Player) event.getWhoClicked();
        if (!cursorEmpty && !isFuel(cursor)) {
            player.sendMessage(Utils.text("Vật phẩm này không phải nhiên liệu.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        if (cursorEmpty) {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
        } else {
            player.playSound(player.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1f, 1.2f);
        }

        generator.fuel().setSlot(cursorEmpty ? null : StoredItem.of(cursor));
        event.getView().setCursor(currentFuel != null ? currentFuel.toItemStack() : null);
        renderFuelSlot();
        renderBurnSlot();
        renderBuffSummary();
    }

    private void onLootClick(InventoryClickEvent event) {
        event.setCancelled(true);

        int index = indexOf(LOOT_SLOTS, event.getSlot());
        if (index < 0) return;

        List<StoredItem> items = generator.loot().entries();
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

        for (StoredItem stored : generator.loot().entries()) {
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

    private void onModuleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        int index = indexOf(MODULE_SLOTS, event.getSlot());
        if (index < 0) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack cursor = event.getView().getCursor();
        if (cursor.getType().isAir()) {
            removeModule(player, index);
        } else if (index >= generator.modules().items().size()) {
            installModule(event, player, cursor);
        }
        renderModuleSlots();
        renderMainInfo();
        renderBuffSummary();
    }

    private void removeModule(Player player, int index) {
        String removedId = generator.removeModule(index);
        if (removedId == null) return;

        ItemStack removed = new StoredItem(removedId, 1).toItemStack();
        if (removed != null) PlayerUtils.giveItem(player, removed);
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 0.8f);
    }

    private void installModule(InventoryClickEvent event, Player player, ItemStack cursor) {
        StoredItem stored = StoredItem.of(cursor);
        ModuleInstallResult result = stored == null ? ModuleInstallResult.NOT_A_MODULE : generator.installModule(stored.item());
        if (result == ModuleInstallResult.INSTALLED) {
            ItemStack remaining = cursor.clone();
            remaining.setAmount(cursor.getAmount() - 1);
            event.getView().setCursor(remaining.getAmount() > 0 ? remaining : null);
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.5f);
            return;
        }

        player.sendMessage(Utils.text(installFailureMessage(result), NamedTextColor.RED));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }

    private String installFailureMessage(ModuleInstallResult result) {
        return switch (result) {
            case NOT_A_MODULE -> "Vật phẩm này không phải module.";
            case NO_FREE_SLOT -> "Máy đã hết ô module.";
            case INCOMPATIBLE -> "Module này xung đột với module đã lắp.";
            case LIMIT_REACHED -> "Đã đạt số lượng tối đa của module này.";
            case INSTALLED -> "";
        };
    }

    private int indexOf(int[] slots, int slot) {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) return i;
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
        renderBurnSlot();
        renderBuffSummary();
        renderLootSlots();
    }
}
