package com.roguesmp.block.gui;

import com.roguesmp.block.data.SieveData;
import com.roguesmp.block.impl.sieve.SieveBlock;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SieveGui extends MachineGui {

    private static final int INFO_SLOT = 4;
    private static final int BUFF_SUMMARY_SLOT = 5;
    private static final int INPUT_SLOT = 10;
    private static final int PROGRESS_SLOT = 12;
    private static final int[] OUTPUT_SLOTS = {14, 15, 16, 23, 24, 25};
    private static final int CATALYST_SLOT = 19;
    private static final int ACTIVE_CATALYST_SLOT = 20;
    private static final int SIDES_SLOT = 26;
    private static final int[] MODULE_SLOTS = {28, 29, 30, 31, 32};

    private final SieveBlock machine;
    private final UpgradePanel upgrades;

    public SieveGui(SieveBlock machine) {
        super(Utils.fromString(machine.getProperties().displayName()), 4, machine.itemStorage(), machine::openGui);
        this.machine = machine;
        this.upgrades = new UpgradePanel(this, machine.upgrades(), this::refresh);
    }

    @Override
    public void refresh() {
        bindSlot(INPUT_SLOT, machine.input(), 0, placeholder(Material.ORANGE_STAINED_GLASS_PANE, "Nguyên liệu", NamedTextColor.GOLD, "Đặt nguyên liệu vào đây"));
        for (int i = 0; i < OUTPUT_SLOTS.length; i++) {
            bindSlot(OUTPUT_SLOTS[i], machine.outputs(), i, placeholder(Material.LIME_STAINED_GLASS_PANE, "Thành phẩm", NamedTextColor.GREEN, "Thành phẩm sẽ xuất hiện ở đây"));
        }
        addButton(PROGRESS_SLOT, progressDisplay(), ClickHandler.noAction());
        addButton(INFO_SLOT, infoDisplay(), ClickHandler.noAction());
        upgrades.renderCatalyst(machine.catalyst(), machine.isRunning(), CATALYST_SLOT, ACTIVE_CATALYST_SLOT);
        upgrades.renderModules(machine.modules(), MODULE_SLOTS, machine::checkModule, machine::isValidModule);
        upgrades.renderSummary(BUFF_SUMMARY_SLOT);
        addSidesButton(SIDES_SLOT);
    }

    @Override
    protected void onAllViewersClosed() {
        machine.releaseGui(this);
    }

    private ItemStack infoDisplay() {
        ItemStack item = ItemStack.of(Material.STONE);
        item.setData(DataComponentTypes.ITEM_MODEL, machine.getProperties().model());
        ItemStackUtils.setItemName(item, Utils.fromString(machine.getProperties().displayName()));

        SieveData data = machine.data();
        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text("Cấp: ", NamedTextColor.YELLOW).append(Component.text(data.tier(), NamedTextColor.GRAY)));
        lore.add(Utils.text("Năng lượng: ", NamedTextColor.YELLOW).append(Component.text(machine.getEnergy() + "/" + machine.getMaxEnergy(), NamedTextColor.GRAY)));
        lore.add(statLine("Năng lượng/tick", data.energyPerTick(), machine.energyPerTick(), "", false));
        lore.add(statLine("Tốc độ", data.speed(), machine.speed(), "×", true));
        ItemStackUtils.setLore(item, lore);
        return item;
    }

    private ItemStack progressDisplay() {
        if (machine.isStalled()) return placeholder(Material.RED_STAINED_GLASS_PANE, "Tạm dừng", NamedTextColor.RED, "Ô thành phẩm đã đầy");

        if (machine.activeRecipe() == null) return placeholder(Material.GRAY_STAINED_GLASS_PANE, "Đang chờ", NamedTextColor.GRAY, "Không có công thức phù hợp");

        int duration = machine.duration();
        int percent = Math.min(100, machine.progress() * 100 / duration);

        ItemStack item = ItemStack.of(Material.SCAFFOLDING);
        item.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
        item.setData(DataComponentTypes.MAX_DAMAGE, 100);
        item.setData(DataComponentTypes.DAMAGE, 100 - percent);
        ItemStackUtils.setItemName(item, Utils.text("Tiến độ: " + percent + "%", NamedTextColor.YELLOW));

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text("Thời gian: ", NamedTextColor.YELLOW).append(Component.text(
                Utils.formatDecimal(machine.progress() / 20d) + "s / " + Utils.formatDecimal(duration / 20d) + "s", NamedTextColor.GRAY)));
        if (machine.getEnergy() <= 0) lore.add(Utils.text("Tạm dừng: hết năng lượng", NamedTextColor.RED));
        ItemStackUtils.setLore(item, lore);
        return item;
    }

    private static ItemStack placeholder(Material material, String title, NamedTextColor color, String hint) {
        ItemStack item = ItemStack.of(material);
        ItemStackUtils.setItemName(item, Utils.text(title, color));
        ItemStackUtils.setLore(item, List.of(Utils.text(hint, NamedTextColor.DARK_GRAY)));
        return item;
    }
}
