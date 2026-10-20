package com.roguesmp.block.gui;

import com.roguesmp.block.impl.energy.WirelessNodeBlock;
import com.roguesmp.block.impl.energy.network.EnergyNetwork;
import com.roguesmp.block.impl.energy.network.EnergyNetworkData;
import com.roguesmp.block.impl.energy.network.EnergyNetworkManager;
import com.roguesmp.block.manager.BlockManager;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.island.IslandData;
import com.roguesmp.island.IslandLevel;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class WirelessNodeGui extends BaseGui {

    private static final int STATUS_SLOT = 3;
    private static final int CREATE_SLOT = 5;
    private static final int FIRST_NETWORK_SLOT = 9;
    private static final int NETWORK_SLOTS = 18;

    private final WirelessNodeBlock node;

    public WirelessNodeGui(WirelessNodeBlock node) {
        super(Component.text("Nút truyền năng lượng"), 3);
        this.node = node;
    }

    @Override
    public void setup() {
        refresh();
    }

    public void refresh() {
        clearUi();
        fillEmpty(FILLER_BLACK);
        IslandData island = node.island();
        if (island == null) return;

        addButton(STATUS_SLOT, statusDisplay(), event -> {
            event.setCancelled(true);
            node.disconnect();
            refresh();
        });
        addButton(CREATE_SLOT, createDisplay(island), event -> {
            event.setCancelled(true);
            EnergyNetworkData created = EnergyNetworkManager.getInstance().create(island);
            if (created != null) node.tune(created);
            refresh();
        });

        int slot = FIRST_NETWORK_SLOT;
        for (EnergyNetworkData networkData : island.getEnergyNetworks()) {
            if (slot >= FIRST_NETWORK_SLOT + NETWORK_SLOTS) break;
            addButton(slot++, networkDisplay(networkData), event -> onNetworkClick(event, island, networkData));
        }
    }

    private void onNetworkClick(InventoryClickEvent event, IslandData island, EnergyNetworkData networkData) {
        event.setCancelled(true);
        if (event.isShiftClick() && event.isRightClick() && event.getWhoClicked() instanceof Player player) {
            confirmDelete(player, island, networkData);
            return;
        }
        node.tune(networkData);
        refresh();
    }

    private void confirmDelete(Player player, IslandData island, EnergyNetworkData networkData) {
        Dialog dialog = DialogBuilder.create(Component.text("Xoá mạng năng lượng?", NamedTextColor.RED))
                .addTextBody(Component.text("Xoá \"" + networkData.name() + "\" sẽ mất toàn bộ " + Utils.formatMoney(networkData.energy()) + " năng lượng đang lưu và ngắt kết nối mọi nút."))
                .confirmation()
                .yesButton(Component.text("Xoá", NamedTextColor.RED), null, (response, audience) -> Utils.runLater(() -> {
                    if (island.getEnergyNetwork(networkData.id()) == networkData) EnergyNetworkManager.getInstance().delete(island, networkData);
                    reopen(player);
                }))
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> reopen(player)))
                .build();
        player.showDialog(dialog);
    }

    private void reopen(Player player) {
        if (BlockManager.getInstance().get(node.getPos()) == node) node.openGui(player);
    }

    private ItemStack statusDisplay() {
        EnergyNetwork network = node.network();
        if (network == null) {
            ItemStack item = ItemStack.of(Material.GRAY_STAINED_GLASS_PANE);
            ItemStackUtils.setItemName(item, Utils.text("Chưa kết nối mạng", NamedTextColor.GRAY));
            ItemStackUtils.setLore(item, List.of(
                    stat("Truyền của nút: ", node.throughput() + "/tick"),
                    Utils.text("Chọn một mạng bên dưới", NamedTextColor.DARK_GRAY)
            ));
            return item;
        }

        ItemStack item = ItemStack.of(Material.LIGHTNING_ROD);
        ItemStackUtils.setItemName(item, Utils.text("Đang kết nối: " + network.data().name(), NamedTextColor.AQUA));
        ItemStackUtils.setLore(item, List.of(
                stat("Truyền của nút: ", node.throughput() + "/tick"),
                Utils.text("Bấm để ngắt kết nối", NamedTextColor.RED)
        ));
        return item;
    }

    private ItemStack createDisplay(IslandData island) {
        int max = IslandLevel.of(island.getLevel()).maxEnergyNetworks();
        int count = island.getEnergyNetworks().size();
        boolean allowed = EnergyNetworkManager.canCreate(island);

        ItemStack item = ItemStack.of(allowed ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemStackUtils.setItemName(item, Utils.text("Tạo mạng mới", allowed ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        ItemStackUtils.setLore(item, List.of(
                stat("Số mạng: ", count + "/" + max),
                Utils.text(allowed ? "Bấm để tạo và kết nối" : "Nâng cấp đảo để tạo thêm mạng", allowed ? NamedTextColor.DARK_GRAY : NamedTextColor.RED)
        ));
        return item;
    }

    private ItemStack networkDisplay(EnergyNetworkData networkData) {
        EnergyNetwork active = EnergyNetworkManager.getInstance().getActive(networkData);
        boolean connected = node.network() != null && node.network().data() == networkData;

        ItemStack item = ItemStack.of(connected ? Material.ENDER_EYE : Material.ENDER_PEARL);
        ItemStackUtils.setItemName(item, Utils.text(networkData.name(), connected ? NamedTextColor.GREEN : NamedTextColor.YELLOW));

        List<Component> lore = new ArrayList<>();
        String capacity = active == null ? "" : "/" + Utils.formatMoney(active.getMaxEnergy());
        lore.add(stat("Lưu trữ: ", Utils.formatMoney(networkData.energy()) + capacity));
        if (active != null) {
            lore.add(stat("Nút đang tải: ", String.valueOf(active.nodes().size())));
            lore.add(stat("Truyền tối đa: ", active.throughput() + "/tick"));
            lore.add(stat("Đang truyền: ", active.lastFlow() + "/tick"));
        }
        lore.add(Utils.text(connected ? "Đang kết nối" : "Bấm để kết nối", connected ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY));
        lore.add(Utils.text("Shift + chuột phải: xoá mạng (mất năng lượng)", NamedTextColor.RED));
        ItemStackUtils.setLore(item, lore);
        return item;
    }

    private static Component stat(String label, String value) {
        return Utils.text(label, NamedTextColor.YELLOW).append(Component.text(value, NamedTextColor.GRAY));
    }

    @Override
    public void onCloseInventory(InventoryCloseEvent event) {
        Utils.runLater(() -> {
            if (getInventory().getViewers().isEmpty()) node.releaseGui(this);
        });
    }

    public void closeAll() {
        for (HumanEntity viewer : List.copyOf(getInventory().getViewers())) viewer.closeInventory();
    }
}
