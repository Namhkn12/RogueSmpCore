package com.roguesmp.block.gui;

import com.roguesmp.block.data.CrusherData;
import com.roguesmp.block.impl.crusher.CrusherBlock;
import com.roguesmp.crafting.recipe.CrusherRecipe;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CrusherGui extends MachineGui {

    private static final int INFO_SLOT = 4;
    private static final int BUFF_SUMMARY_SLOT = 5;
    private static final int INPUT_SLOT = 10;
    private static final int PROGRESS_SLOT = 12;
    private static final int[] OUTPUT_SLOTS = {14, 15, 16, 23, 24, 25};
    private static final int CATALYST_SLOT = 19;
    private static final int ACTIVE_CATALYST_SLOT = 20;
    private static final int SIDES_SLOT = 26;
    private static final int[] MODULE_SLOTS = {28, 29, 30, 31, 32};

    private final CrusherBlock crusher;
    private final UpgradePanel upgrades;

    public CrusherGui(CrusherBlock crusher) {
        super(Utils.fromString(crusher.getProperties().displayName()), 4, crusher.itemStorage(), crusher::openGui);
        this.crusher = crusher;
        this.upgrades = new UpgradePanel(this, crusher.upgrades(), this::refresh);
    }

    @Override
    public void refresh() {
        bindSlot(INPUT_SLOT, crusher.input(), 0, placeholder(Material.ORANGE_STAINED_GLASS_PANE, "Nguyên liệu", NamedTextColor.GOLD, "Đặt nguyên liệu vào đây"));
        for (int i = 0; i < OUTPUT_SLOTS.length; i++) {
            bindSlot(OUTPUT_SLOTS[i], crusher.outputs(), i, placeholder(Material.LIME_STAINED_GLASS_PANE, "Thành phẩm", NamedTextColor.GREEN, "Thành phẩm sẽ xuất hiện ở đây"));
        }
        addButton(PROGRESS_SLOT, progressDisplay(), ClickHandler.noAction());
        addButton(INFO_SLOT, infoDisplay(), ClickHandler.noAction());
        upgrades.renderCatalyst(crusher.catalyst(), crusher.isRunning(), CATALYST_SLOT, ACTIVE_CATALYST_SLOT);
        upgrades.renderModules(crusher.modules(), MODULE_SLOTS, crusher::checkModule, crusher::isValidModule);
        upgrades.renderSummary(BUFF_SUMMARY_SLOT);
        addSidesButton(SIDES_SLOT);
    }

    @Override
    protected void onAllViewersClosed() {
        crusher.releaseGui(this);
    }

    private ItemStack infoDisplay() {
        ItemStack item = ItemStack.of(Material.STONE);
        item.setData(DataComponentTypes.ITEM_MODEL, crusher.getProperties().model());
        ItemStackUtils.setItemName(item, Utils.fromString(crusher.getProperties().displayName()));

        CrusherData data = crusher.data();
        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text("Cấp: ", NamedTextColor.YELLOW).append(Component.text(data.tier(), NamedTextColor.GRAY)));
        lore.add(Utils.text("Năng lượng: ", NamedTextColor.YELLOW).append(Component.text(crusher.getEnergy() + "/" + crusher.getMaxEnergy(), NamedTextColor.GRAY)));
        lore.add(statLine("Năng lượng/tick", data.energyPerTick(), crusher.energyPerTick(), "", false));
        lore.add(statLine("Tốc độ", data.speed(), crusher.speed(), "×", true));
        ItemStackUtils.setLore(item, lore);
        return item;
    }

    private ItemStack progressDisplay() {
        if (crusher.isStalled()) return placeholder(Material.RED_STAINED_GLASS_PANE, "Tạm dừng", NamedTextColor.RED, "Ô thành phẩm đã đầy");

        CrusherRecipe recipe = crusher.activeRecipe();
        if (recipe == null) return placeholder(Material.GRAY_STAINED_GLASS_PANE, "Đang chờ", NamedTextColor.GRAY, "Không có công thức phù hợp");

        int duration = crusher.duration();
        int percent = Math.min(100, crusher.progress() * 100 / duration);

        ItemStack item = ItemStack.of(Material.GRINDSTONE);
        item.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
        item.setData(DataComponentTypes.MAX_DAMAGE, 100);
        item.setData(DataComponentTypes.DAMAGE, 100 - percent);
        ItemStackUtils.setItemName(item, Utils.text("Tiến độ: " + percent + "%", NamedTextColor.YELLOW));

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text("Thời gian: ", NamedTextColor.YELLOW).append(Component.text(
                Utils.formatDecimal(crusher.progress() / 20d) + "s / " + Utils.formatDecimal(duration / 20d) + "s", NamedTextColor.GRAY)));
        if (crusher.getEnergy() <= 0) lore.add(Utils.text("Tạm dừng: hết năng lượng", NamedTextColor.RED));
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
