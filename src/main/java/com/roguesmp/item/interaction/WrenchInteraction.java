package com.roguesmp.item.interaction;

import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.impl.blocks.EnergyNode;
import com.roguesmp.block.manager.BlockManager;
import com.roguesmp.registry.ItemRegistry;
import com.roguesmp.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class WrenchInteraction implements ItemInteraction {

    private final BlockManager manager;
    private final Map<UUID, Location> linkingState = new HashMap<>();
    private final Map<UUID, Long> pendingClearConfirmations = new HashMap<>();

    public WrenchInteraction(){
        manager = BlockManager.getInstance();
    }

    @Override
    public void onInteract(PlayerInteractEvent event){
        Player player = event.getPlayer();

        Block clickedBlockVanilla = event.getClickedBlock();
        if(clickedBlockVanilla == null) return;

        Location clickedLoc = clickedBlockVanilla.getLocation();

        // 2. Chỉ tương tác nếu khối được click là Custom Block (SmpBlock)
        if(!manager.isSmpBlock(clickedLoc)) return;

        SmpBlock clickedBlock = manager.getBlock(clickedLoc);
        Action action = event.getAction();
        boolean isSneaking = player.isSneaking();

        // =================================================================
        // HÀNH ĐỘNG 1: SHIFT + CHUỘT PHẢI -> PHÁ MÁY NHANH
        // =================================================================
        if(action == Action.RIGHT_CLICK_BLOCK && isSneaking){
            event.setCancelled(true);

            linkingState.remove(player.getUniqueId());

            BlockBreakEvent mockEvent = new BlockBreakEvent(clickedBlockVanilla, player);
            clickedBlock.onBlockBreak(mockEvent);

            clickedBlockVanilla.setType(Material.AIR);

            return;
        }

        // =================================================================
        // HÀNH ĐỘNG 2: SHIFT + CHUỘT TRÁI -> GỠ KẾT NỐI (CLEAR LINKS)
        // =================================================================
        if(action == Action.LEFT_CLICK_BLOCK && isSneaking){
            event.setCancelled(true);

            if(clickedBlock instanceof EnergyNode node){
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

                    // 4. Xóa kết nối ở nút hiện tại
                    node.clearConnections();

                    player.sendMessage("§aĐã cắt đứt thành công " + connectionCount + " đường dây nối với nút điện này!");

                } else {
                    // LẦN CLICK ĐẦU TIÊN (HOẶC CLICK KHI ĐÃ QUÁ HẠN CŨ)

                    // Tạo một mốc thời gian hết hạn mới: Hiện tại + 5000ms
                    long expirationTime = currentTime + 5000L;
                    pendingClearConfirmations.put(uuid, expirationTime);

                    player.sendMessage("§eBạn đang chuẩn bị cắt đứt §c" + connectionCount + " §eđường dây.");
                    player.sendMessage("§eHãy §c[Shift + Chuột trái] §emột lần nữa trong §a5 giây §eđể xác nhận!");

                    Utils.runLater(() -> {
                        if (pendingClearConfirmations.containsKey(uuid) && pendingClearConfirmations.get(uuid) == expirationTime) {

                            // Xóa dữ liệu vì đã hết hạn
                            pendingClearConfirmations.remove(uuid);

                            // Check xem người chơi còn online không để gửi tin nhắn
                            if (player.isOnline()) {
                                player.sendMessage("§cĐã hết 5 giây! Thao tác tháo dây điện bị hủy.");
                            }
                        }
                    }, 100);
                }
            }
            return;
        }

        // =================================================================
        // HÀNH ĐỘNG 3: CHUỘT PHẢI BÌNH THƯỜNG -> NỐI DÂY ĐIỆN
        // =================================================================
        if(action == Action.RIGHT_CLICK_BLOCK && !isSneaking){

            if(clickedBlock instanceof EnergyNode currentNode){
                event.setCancelled(true);
                UUID uuid = player.getUniqueId();

                if(!linkingState.containsKey(uuid)){
                    // Lần click đầu tiên
                    linkingState.put(uuid, clickedLoc);
                    player.sendMessage("§aĐã chọn nút điện thứ nhất. Hãy click vào nút tiếp theo để nối dây!");
                }
                else{
                    // Lần click thứ hai
                    Location firstLoc = linkingState.get(uuid);
                    linkingState.remove(uuid); // Làm sạch trạng thái ngay lập tức

                    if(firstLoc.equals(clickedLoc)){
                        player.sendMessage("§cBạn không thể nối một nút điện với chính nó!");
                        return;
                    }

                    if(firstLoc.getWorld() != clickedLoc.getWorld() || firstLoc.distance(clickedLoc) > 15.0){
                        player.sendMessage("§cKhoảng cách quá xa! Khoảng cách tối đa giữa 2 nút là 15 block.");
                        return;
                    }

                    EnergyNode firstNode = (EnergyNode) manager.getBlock(firstLoc);
                    if(firstNode != null){
                        // Kiểm tra xem nút 1 đã chứa tọa độ nút 2 chưa (hoặc ngược lại)
                        if (firstNode.getConnections().contains(clickedLoc) || currentNode.getConnections().contains(firstLoc)) {
                            player.sendMessage("§eHai nút điện này đã được kết nối với nhau từ trước rồi!");
                            return;
                        }

                        // Nối 2 chiều để dòng điện đi được qua lại
                        firstNode.addConnection(clickedLoc);
                        currentNode.addConnection(firstLoc);
                        player.sendMessage("§aNối 2 nút thành công!");
                    }
                }
            }
        }
    }
}
