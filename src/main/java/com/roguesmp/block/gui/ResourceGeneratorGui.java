package com.roguesmp.block.gui;

import com.roguesmp.block.StoredItem;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import com.roguesmp.block.data.GeneratorData;
import com.roguesmp.block.impl.generator.ResourceGeneratorBlock;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class ResourceGeneratorGui extends MachineGui {

    private static final int MAIN_INFO_SLOT = 4;
    private static final int STATUS_SLOT = 5;
    private static final int BUFF_SUMMARY_SLOT = 6;
    private static final int TAKE_ALL_SLOT = 34;
    private static final int SIDES_SLOT = 35;
    private static final int CATALYST_SLOT = 10;
    private static final int ACTIVE_CATALYST_SLOT = 19;
    private static final int[] LOOT_SLOTS = {12, 13, 14, 15, 16, 21, 22, 23, 24, 25};
    private static final int[] MODULE_SLOTS = {28, 29, 30, 31, 32};

    private final ResourceGeneratorBlock generator;
    private final UpgradePanel upgrades;

    public ResourceGeneratorGui(ResourceGeneratorBlock generator) {
        super(Component.text("Resource Generator"), 4, generator.itemStorage(), generator::openGui);
        this.generator = generator;
        this.upgrades = new UpgradePanel(this, generator.upgrades(), this::refresh);
    }

    @Override
    public void setup() {
        super.setup();
        renderTakeAllButton();
        addSidesButton(SIDES_SLOT);
    }

    private void renderMainInfo() {
        ItemStack item = ItemStack.of(Material.STONE);
        item.setData(DataComponentTypes.ITEM_MODEL, generator.getProperties().model());
        ItemStackUtils.setItemName(item, Utils.fromString(generator.getProperties().displayName()));
        GeneratorData data = generator.data();
        List<Component> infoLore = new ArrayList<>();
        infoLore.add(Utils.text("Năng lượng: ", NamedTextColor.YELLOW).append(Component.text(generator.getEnergy() + "/" + generator.getMaxEnergy(), NamedTextColor.GRAY)));
        infoLore.add(Utils.text("Sức chứa: ", NamedTextColor.YELLOW).append(Component.text(generator.loot().occupied() + "/" + generator.loot().capacity(), NamedTextColor.GRAY)));
        infoLore.add(statLine("Năng lượng tối đa", data.maxEnergy(), generator.getMaxEnergy(), "", true));
        infoLore.add(statLine("Sức chứa tối đa", data.lootCapacity(), generator.lootCapacity(), "", true));
        infoLore.add(statLine("Thời gian đặt", data.placeDelayTicks() / 20d, generator.placeDelayTicks() / 20d, "s", false));
        infoLore.add(statLine("Thời gian khai thác", data.breakDelayTicks() / 20d, generator.breakDelayTicks() / 20d, "s", false));
        infoLore.add(statLine("Năng lượng/tick", data.energyPerTick(), generator.energyPerTick(), "", false));
        infoLore.add(Utils.text("Module: ", NamedTextColor.YELLOW).append(Component.text(IntStream.range(0, generator.modules().size()).filter(i -> generator.modules().get(i) != null).count() + "/" + generator.modules().size(), NamedTextColor.GRAY)));
        ItemStackUtils.setLore(item, infoLore);
        addButton(MAIN_INFO_SLOT, item, ClickHandler.noAction());
    }

    private Component statLine(String label, double base, double result, String suffix, boolean higherIsBetter) {
        Component line = Utils.text(label + ": ", NamedTextColor.YELLOW);
        if (Math.abs(result - base) < 1e-9) return line.append(Component.text(Utils.formatDecimal(base) + suffix, NamedTextColor.GRAY));

        NamedTextColor color = result > base == higherIsBetter ? NamedTextColor.GREEN : NamedTextColor.RED;
        return line.append(Component.text(Utils.formatDecimal(base) + suffix, NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH))
                .append(Component.text(" → ", NamedTextColor.GRAY))
                .append(Component.text(Utils.formatDecimal(result) + suffix, color));
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
            int elapsed = generator.breakDelayTicks() - generator.getBreakTicksLeft();
            lore.add(Utils.text("Tiến độ: " + elapsed + " / " + generator.breakDelayTicks() + " tick", NamedTextColor.GRAY));
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

    private void renderLootSlots() {
        List<StoredItem> items = generator.loot().entries();
        for (int i = 0; i < LOOT_SLOTS.length; i++) {
            ItemStack filler = ItemStackUtils.hideTooltip(ItemStack.of(Material.WHITE_STAINED_GLASS_PANE));
            if (i < items.size()) addButton(LOOT_SLOTS[i], lootDisplayItem(items.get(i)), this::onLootClick);
            else addButton(LOOT_SLOTS[i], filler, ClickHandler.noAction());
        }
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

    private int indexOf(int[] slots, int slot) {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) return i;
        }
        return -1;
    }

    @Override
    protected void onAllViewersClosed() {
        generator.releaseGui(this);
    }

    @Override
    public void refresh() {
        renderMainInfo();
        renderStatus();
        upgrades.renderModules(generator.modules(), MODULE_SLOTS, generator::checkModule, generator::isValidModule);
        upgrades.renderCatalyst(generator.catalystFeed(), generator::isValidCatalyst, generator.catalyst(), generator.isCatalystPaused(), generator::clearCatalyst, CATALYST_SLOT, ACTIVE_CATALYST_SLOT);
        upgrades.renderSummary(BUFF_SUMMARY_SLOT);
        renderLootSlots();
    }
}
