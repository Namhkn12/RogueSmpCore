package com.roguesmp.item.component.impl;

import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.impl.blocks.EnergyNode;
import com.roguesmp.block.manager.BlockManager;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.item.component.InteractableComponent;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Right/left-click behavior for the wrench tool against custom {@link SmpBlock}s: shift+right to
 * quick-break, shift+left to clear an {@link EnergyNode}'s connections (with a 5s re-click
 * confirmation), plain right-click to link two energy nodes. Formerly a standalone
 * {@code ItemInteraction} keyed by item id in the now-removed {@code ItemInteractionRegistry};
 * moved here so it goes through the same instanceof-mixin dispatch every other
 * {@link InteractableComponent} uses.
 * <p>
 * {@code linkingState}/{@code pendingClearConfirmations} are per-player, not per-item, so they
 * stay {@code static} rather than instance fields - a non-unique item like the wrench gets a fresh
 * {@link #copy()} on every interact (no stable identity to key runtime state off of), and the
 * behavior should follow the player across wrench swaps anyway.
 */
public class WrenchComponent implements InteractableComponent {

    public static final Codec<WrenchComponent> CODEC = MapCodec.unit(WrenchComponent::new).codec();

    private static final Map<UUID, Location> linkingState = new HashMap<>();
    private static final Map<UUID, Long> pendingClearConfirmations = new HashMap<>();

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    @Override
    public void onItemInteract(SmpPlayer smpPlayer, PlayerInteractEvent event) {
        Player player = event.getPlayer();

        Block clickedBlockVanilla = event.getClickedBlock();
        if (clickedBlockVanilla == null) return;

        Location clickedLoc = clickedBlockVanilla.getLocation();

        BlockManager manager = BlockManager.getInstance();
        if (!manager.isSmpBlock(clickedLoc)) return;

        SmpBlock clickedBlock = manager.getBlock(clickedLoc);
        Action action = event.getAction();
        boolean isSneaking = player.isSneaking();

        // HÀNH ĐỘNG 1: SHIFT + CHUỘT PHẢI -> PHÁ MÁY NHANH
        if (action == Action.RIGHT_CLICK_BLOCK && isSneaking) {
            event.setCancelled(true);

            linkingState.remove(player.getUniqueId());

            BlockBreakEvent mockEvent = new BlockBreakEvent(clickedBlockVanilla, player);
            clickedBlock.onBlockBreak(mockEvent);

            clickedBlockVanilla.setType(Material.AIR);

            return;
        }

        // HÀNH ĐỘNG 2: SHIFT + CHUỘT TRÁI -> GỠ KẾT NỐI (CLEAR LINKS)
        if (action == Action.LEFT_CLICK_BLOCK && isSneaking) {
            event.setCancelled(true);

            if (clickedBlock instanceof EnergyNode node) {
                int connectionCount = node.getConnections().size();

                if (connectionCount == 0) {
                    player.sendMessage("§cNút điện này chưa được kết nối với đâu cả!");
                    return;
                }

                UUID uuid = player.getUniqueId();
                long currentTime = System.currentTimeMillis();

                if (pendingClearConfirmations.containsKey(uuid) && currentTime <= pendingClearConfirmations.get(uuid)) {
                    pendingClearConfirmations.remove(uuid);

                    Set<Location> connectionsCopy = new HashSet<>(node.getConnections());

                    for (Location connectedLoc : connectionsCopy) {
                        SmpBlock connectedSmp = manager.getBlock(connectedLoc);
                        if (connectedSmp instanceof EnergyNode connectedNode) {
                            connectedNode.removeConnection(clickedLoc);
                        }
                    }

                    node.clearConnections();

                    player.sendMessage("§aĐã cắt đứt thành công " + connectionCount + " đường dây nối với nút điện này!");
                } else {
                    long expirationTime = currentTime + 5000L;
                    pendingClearConfirmations.put(uuid, expirationTime);

                    player.sendMessage("§eBạn đang chuẩn bị cắt đứt §c" + connectionCount + " §eđường dây.");
                    player.sendMessage("§eHãy §c[Shift + Chuột trái] §emột lần nữa trong §a5 giây §eđể xác nhận!");

                    Utils.runLater(() -> {
                        if (pendingClearConfirmations.containsKey(uuid) && pendingClearConfirmations.get(uuid) == expirationTime) {
                            pendingClearConfirmations.remove(uuid);

                            if (player.isOnline()) {
                                player.sendMessage("§cĐã hết 5 giây! Thao tác tháo dây điện bị hủy.");
                            }
                        }
                    }, 100);
                }
            }
            return;
        }

        // HÀNH ĐỘNG 3: CHUỘT PHẢI BÌNH THƯỜNG -> NỐI DÂY ĐIỆN
        if (action == Action.RIGHT_CLICK_BLOCK && !isSneaking) {
            if (clickedBlock instanceof EnergyNode currentNode) {
                event.setCancelled(true);
                UUID uuid = player.getUniqueId();

                if (!linkingState.containsKey(uuid)) {
                    linkingState.put(uuid, clickedLoc);
                    player.sendMessage("§aĐã chọn nút điện thứ nhất. Hãy click vào nút tiếp theo để nối dây!");
                } else {
                    Location firstLoc = linkingState.get(uuid);
                    linkingState.remove(uuid);

                    if (firstLoc.equals(clickedLoc)) {
                        player.sendMessage("§cBạn không thể nối một nút điện với chính nó!");
                        return;
                    }

                    if (firstLoc.getWorld() != clickedLoc.getWorld() || firstLoc.distance(clickedLoc) > 15.0) {
                        player.sendMessage("§cKhoảng cách quá xa! Khoảng cách tối đa giữa 2 nút là 15 block.");
                        return;
                    }

                    EnergyNode firstNode = (EnergyNode) manager.getBlock(firstLoc);
                    if (firstNode != null) {
                        if (firstNode.getConnections().contains(clickedLoc) || currentNode.getConnections().contains(firstLoc)) {
                            player.sendMessage("§eHai nút điện này đã được kết nối với nhau từ trước rồi!");
                            return;
                        }

                        firstNode.addConnection(clickedLoc);
                        currentNode.addConnection(firstLoc);
                        player.sendMessage("§aNối 2 nút thành công!");
                    }
                }
            }
        }
    }
}
