package com.roguesmp.block.gui;

import com.roguesmp.block.impl.energy.EnergyGeneratorBlock;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class EnergyGeneratorGui extends MachineGui {

    private static final int FUEL_SLOT = 11;
    private static final int BURN_SLOT = 13;
    private static final int ENERGY_SLOT = 15;
    private static final int SIDES_SLOT = 26;

    private final EnergyGeneratorBlock generator;

    public EnergyGeneratorGui(EnergyGeneratorBlock generator) {
        super(Component.text("Máy phát năng lượng"), 3, generator.itemStorage(), generator::openGui);
        this.generator = generator;
    }

    @Override
    public void refresh() {
        bindSlot(FUEL_SLOT, generator.fuelSlot(), 0, fuelPlaceholder());
        addButton(BURN_SLOT, burnDisplay(), ClickHandler.noAction());
        addButton(ENERGY_SLOT, energyDisplay(), ClickHandler.noAction());
        addSidesButton(SIDES_SLOT);
    }

    @Override
    protected void onAllViewersClosed() {
        generator.releaseGui(this);
    }

    private ItemStack fuelPlaceholder() {
        ItemStack item = ItemStack.of(Material.ORANGE_STAINED_GLASS_PANE);
        ItemStackUtils.setItemName(item, Utils.text("Nhiên liệu", NamedTextColor.GOLD));
        ItemStackUtils.setLore(item, List.of(Utils.text("Đặt nhiên liệu vào đây", NamedTextColor.DARK_GRAY)));
        return item;
    }

    private ItemStack burnDisplay() {
        if (!generator.isBurning()) {
            ItemStack item = ItemStack.of(Material.GRAY_STAINED_GLASS_PANE);
            ItemStackUtils.setItemName(item, Utils.text("Không cháy", NamedTextColor.GRAY));
            ItemStackUtils.setLore(item, List.of(Utils.text("Chưa có nhiên liệu", NamedTextColor.DARK_GRAY)));
            return item;
        }

        int percent = generator.burnTicksLeft() * 100 / Math.max(1, generator.burnTicksTotal());
        ItemStack item = gauge(Material.BLAZE_POWDER, percent);
        ItemStackUtils.setItemName(item, Utils.text("Đang cháy: " + percent + "%", NamedTextColor.GOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text("Còn lại: ", NamedTextColor.YELLOW).append(Component.text(Utils.formatDecimal(generator.burnTicksLeft() / 20d) + "s", NamedTextColor.GRAY)));
        lore.add(Utils.text("Sản lượng: ", NamedTextColor.YELLOW).append(Component.text(generator.burnEnergyPerTick() + "/tick", NamedTextColor.GRAY)));
        if (generator.getEnergy() >= generator.getMaxEnergy()) lore.add(Utils.text("Tạm dừng: bộ chứa đã đầy", NamedTextColor.RED));
        ItemStackUtils.setLore(item, lore);
        return item;
    }

    private ItemStack energyDisplay() {
        int percent = generator.getEnergy() * 100 / Math.max(1, generator.getMaxEnergy());
        ItemStack item = gauge(Material.GLOWSTONE_DUST, percent);
        ItemStackUtils.setItemName(item, Utils.text("Năng lượng: " + percent + "%", NamedTextColor.YELLOW));
        ItemStackUtils.setLore(item, List.of(
                Utils.text("Lưu trữ: ", NamedTextColor.YELLOW).append(Component.text(Utils.formatMoney(generator.getEnergy()) + "/" + Utils.formatMoney(generator.getMaxEnergy()), NamedTextColor.GRAY)),
                Utils.text("Truyền tối đa: ", NamedTextColor.YELLOW).append(Component.text(generator.transferRate() + "/tick", NamedTextColor.GRAY)),
                Utils.text("Tự động truyền sang máy kề bên", NamedTextColor.DARK_GRAY)
        ));
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
