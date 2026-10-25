package com.roguesmp.block.impl.energy;

import com.roguesmp.block.BlockProperties;
import com.roguesmp.block.EnergyStorage;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.Tickable;
import com.roguesmp.block.data.WirelessNodeData;
import com.roguesmp.block.event.SmpBlockBreakEvent;
import com.roguesmp.block.gui.WirelessNodeGui;
import com.roguesmp.block.impl.energy.network.EnergyNetwork;
import com.roguesmp.block.impl.energy.network.EnergyNetworkData;
import com.roguesmp.block.impl.energy.network.EnergyNetworkManager;
import com.roguesmp.block.persistence.StateSection;
import com.roguesmp.codec.Codec;
import com.roguesmp.island.IslandData;
import com.roguesmp.island.IslandManager;
import com.roguesmp.permission.Permissions;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WirelessNodeBlock extends SmpBlock implements Tickable, EnergyStorage {

    private static final int GUI_REFRESH_INTERVAL = 10;

    private final WirelessNodeData data;
    private String networkId = "";
    private @Nullable EnergyNetwork network;
    private @Nullable WirelessNodeGui gui;
    private int guiRefreshTicks;

    public WirelessNodeBlock(BlockProperties properties, WirelessNodeData data) {
        super(properties);
        this.data = data;
    }

    @Override
    public boolean canPlace(Player player, Block target) {
        if (IslandManager.getInstance().getIslandByWorld(target.getWorld()) != null) return true;

        player.sendActionBar(Utils.text("Chỉ có thể đặt nút năng lượng trên đảo", NamedTextColor.RED));
        return false;
    }

    @Override
    public void tick() {
        if (network == null && !networkId.isEmpty()) tryJoin();
        syncDisplayModel();
        tickGuiRefresh();
    }

    private void tryJoin() {
        IslandData island = island();
        if (island == null) return;

        EnergyNetworkData networkData = island.getEnergyNetwork(networkId);
        if (networkData == null) {
            disconnect();
            return;
        }
        network = EnergyNetworkManager.getInstance().join(this, island, networkData);
    }

    private void tickGuiRefresh() {
        if (gui == null) return;
        if (++guiRefreshTicks < GUI_REFRESH_INTERVAL) return;

        guiRefreshTicks = 0;
        gui.refresh();
    }

    public void tune(EnergyNetworkData networkData) {
        leaveNetwork();
        networkId = networkData.id();
        markDirty();
        tryJoin();
    }

    public void disconnect() {
        leaveNetwork();
        networkId = "";
        markDirty();
    }

    private void leaveNetwork() {
        if (network == null) return;
        EnergyNetworkManager.getInstance().leave(this, network);
        network = null;
    }

    public @Nullable IslandData island() {
        return IslandManager.getInstance().getIslandByWorld(getLocation().getWorld());
    }

    public @Nullable EnergyNetwork network() {
        return network;
    }

    public int throughput() {
        return data.throughput();
    }

    @Override
    public Key getDisplayModel() {
        return network != null ? data.modelActive() : super.getDisplayModel();
    }

    @Override
    public int getEnergy() {
        return network == null ? 0 : network.getEnergy();
    }

    @Override
    public int getMaxEnergy() {
        return network == null ? 0 : network.getMaxEnergy();
    }

    @Override
    public int addEnergy(int amount) {
        return network == null ? 0 : network.addEnergy(amount);
    }

    @Override
    public int removeEnergy(int amount) {
        return network == null ? 0 : network.removeEnergy(amount);
    }

    @Override
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        IslandData island = island();
        if ((island == null || !island.isMember(player.getUniqueId())) && !Permissions.ISLAND_ADMIN.has(player)) {
            player.sendActionBar(Utils.text("Chỉ thành viên đảo mới có thể chỉnh nút này", NamedTextColor.RED));
            return;
        }
        openGui(player);
    }

    public void openGui(HumanEntity viewer) {
        if (gui == null) gui = new WirelessNodeGui(this);
        gui.showInventory(viewer);
    }

    public void releaseGui(WirelessNodeGui closed) {
        if (gui == closed) gui = null;
    }

    @Override
    public void onBlockBreak(SmpBlockBreakEvent event) {
        if (gui != null) gui.closeAll();
    }

    @Override
    public void onUnload() {
        leaveNetwork();
    }

    @Override
    protected void collectSections(List<StateSection<?>> sections) {
        super.collectSections(sections);
        sections.add(StateSection.of(Codec.STRING.optionalFieldOf("network", ""), () -> networkId, value -> networkId = value));
    }
}
