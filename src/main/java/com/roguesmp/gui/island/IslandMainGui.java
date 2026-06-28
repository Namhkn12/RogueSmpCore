package com.roguesmp.gui.island;

import com.roguesmp.gui.BaseGui;
import com.roguesmp.island.IslandData;
import com.roguesmp.island.IslandManager;
import com.roguesmp.player.PlayerData;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class IslandMainGui extends BaseGui {

    private final Player player;

    public IslandMainGui(Player player) {
        super(Component.text("Skyblock Menu", NamedTextColor.DARK_AQUA).decoration(TextDecoration.BOLD, true), 5);
        this.player = player;
    }

    @Override
    public void setup() {
        clearUi();

        // 1. Draw frame borders around our inventory layout using fillers
        for (int col = 0; col < 9; col++) {
            addItem(0, col, FILLER_BLACK);
            addItem(4, col, FILLER_BLACK);
        }
        for (int row = 1; row < 4; row++) {
            addItem(row, 0, FILLER_BLACK);
            addItem(row, 8, FILLER_BLACK);
        }

        // 2. Access variables globally using the static IslandManager instance
        IslandManager islandManager = IslandManager.getInstance();

        PlayerData playerData = islandManager.getPlayerManager().getDataManager().getData(player.getUniqueId());
        UUID islandId = (playerData != null) ? playerData.getIslandId() : null;
        IslandData islandData = (islandId != null) ? islandManager.getIslandDataManager().getCachedData(islandId) : null;

        // 3. Render contextual nodes based on island state
        if (islandData == null) {
            setupUninhabitedView(islandManager);
        } else {
            setupActiveIslandView(islandManager, islandData);
            setupTeamManagementView(islandManager, islandData);
        }

        // 4. Paint remaining open background cells smoothly
        fillEmpty(FILLER);
    }

    /**
     * Renders controls for a player who doesn't currently own or belong to any active skyblock instances.
     */
    private void setupUninhabitedView(IslandManager islandManager) {
        ItemStack createItem = ItemStack.of(Material.GRASS_BLOCK);
        createItem.setData(DataComponentTypes.ITEM_NAME, Component.text("» Tạo Đảo Mới «", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true));

        List<Component> lore = List.of(
                Component.empty(),
                Utils.text("Bắt đầu cuộc hành trình sinh tồn", NamedTextColor.GRAY),
                Utils.text("trên một hòn đảo lơ lửng của riêng bạn!", NamedTextColor.GRAY),
                Component.empty(),
                Utils.text("Nhấp chuột để khởi tạo ngay lập tức.", NamedTextColor.YELLOW)
        );
        createItem.setData(DataComponentTypes.LORE, ItemLore.lore(lore));

        addButton(2, 4, createItem, event -> {
            event.setCancelled(true);
            player.closeInventory();
            islandManager.createIsland(player);
        });
    }

    /**
     * Renders controls for active island coops including warps, spawn settings, and member rosters.
     */
    private void setupActiveIslandView(IslandManager islandManager, IslandData islandData) {
        // --- BUTTON 1: WARP HOME ---
        ItemStack teleportItem = ItemStack.of(Material.BEACON);
        teleportItem.setData(DataComponentTypes.ITEM_NAME, Component.text("Dịch Chuyển Về Đảo", NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true));
        teleportItem.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.text("Quay trở về spawn trên đảo", NamedTextColor.GRAY),
                Component.empty(),
                Utils.text("Click chuột để biến về.", NamedTextColor.YELLOW)
        )));

        addButton(2, 2, teleportItem, event -> {
            event.setCancelled(true);
            player.closeInventory();
            islandManager.teleportToHomeIsland(player);
        });

        // --- BUTTON 2: SET SPAWN POINT ---
        ItemStack setSpawnItem = ItemStack.of(Material.COMPASS);
        setSpawnItem.setData(DataComponentTypes.ITEM_NAME, Component.text("Đặt Điểm Spawn", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
        setSpawnItem.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.text("Đặt vị trí đứng hiện tại của bạn", NamedTextColor.GRAY),
                Utils.text("làm spawn mặc định cho đảo.", NamedTextColor.GRAY),
                Component.empty(),
                Utils.text("Click chuột để lưu tọa độ.", NamedTextColor.YELLOW)
        )));

        addButton(2, 4, setSpawnItem, event -> {
            event.setCancelled(true);
            boolean success = islandManager.setIslandSpawn(player, player.getLocation());
            if (success) {
                player.sendMessage(Utils.fromString("<green>Cập nhật vị trí spawn của đảo thành công!"));
                setup(); // Hot-refresh lore metrics instantly
            }
        });

        // --- BUTTON 3: TEAM MEMBERS LIST ---
        ItemStack teamInfoItem = ItemStack.of(Material.PLAYER_HEAD);
        teamInfoItem.setData(DataComponentTypes.CUSTOM_NAME, Utils.text("Thành Viên Đội Nhóm", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.BOLD, true));

        List<Component> teamLore = new ArrayList<>();
        teamLore.add(Utils.text("Danh sách thành viên chung tổ đội:", NamedTextColor.GRAY));
        teamLore.add(Component.empty());

        Set<UUID> members = islandData.getMembers();
        for (UUID memberUuid : members) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(memberUuid);
            String nameString = (op.getName() != null) ? op.getName() : memberUuid.toString();

            if (op.isOnline()) {
                teamLore.add(Utils.text(" • " + nameString, NamedTextColor.GREEN).append(Utils.text(" (Online)", NamedTextColor.DARK_GREEN)));
            } else {
                teamLore.add(Utils.text(" • " + nameString, NamedTextColor.RED).append(Utils.text(" (Offline)", NamedTextColor.DARK_RED)));
            }
        }

        teamLore.add(Component.empty());
        teamLore.add(Utils.text("Tổng: " + members.size() + " người chơi.", NamedTextColor.DARK_GRAY));
        teamInfoItem.setData(DataComponentTypes.LORE, ItemLore.lore(teamLore));

        // Fetch user context skin profile natively using modern Paper APIs
        ResolvableProfile profile = ResolvableProfile.resolvableProfile(player.getPlayerProfile());
        teamInfoItem.setData(DataComponentTypes.PROFILE, profile);

        addButton(2, 6, teamInfoItem, event -> event.setCancelled(true));
    }

    /*
     * Contextual row additions addressing Invites, Leaving, and Disbanding networks securely.
     */
    private void setupTeamManagementView(IslandManager islandManager, IslandData islandData) {

        // --- BUTTON: INVITE NEW PLAYER ---
        ItemStack inviteItem = ItemStack.of(Material.WRITABLE_BOOK);
        inviteItem.setData(DataComponentTypes.ITEM_NAME, Component.text("Mời Thành Viên", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true));
        inviteItem.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.text("Thêm thành viên mới vào đảo của bạn.", NamedTextColor.GRAY),
                Component.empty(),
                Utils.text("Sử dụng lệnh: ", NamedTextColor.DARK_GRAY).append(Component.text("/is invite <tên>", NamedTextColor.YELLOW))
        )));
        addButton(3, 4, inviteItem, event -> {
            event.setCancelled(true);
            player.closeInventory();
            player.sendMessage("§eSử dụng lệnh §b/is invite <tên> §eđể gửi lời mời tham gia tổ đội!");
        });

        // --- BUTTON: LEAVE OR KICK SYSTEM ---
        ItemStack leaveItem = ItemStack.of(Material.IRON_DOOR);
        leaveItem.setData(DataComponentTypes.ITEM_NAME, Component.text("Rời Tổ Đội", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
        leaveItem.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.text("Từ bỏ quyền sở hữu và rời khỏi", NamedTextColor.GRAY),
                Utils.text("hòn đảo hiện tại của bạn.", NamedTextColor.GRAY),
                Component.empty(),
                Utils.text("Lưu ý: Hành động này không thể hoàn tác!", NamedTextColor.RED),
                Component.empty(),
                Utils.text("Nhấp chuột để rời đi.", NamedTextColor.YELLOW)
        )));
        addButton(3, 8, leaveItem, ClickHandler.openGui(new IslandLeaveConfirmGui(islandData.getIslandId(), player)));
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }

}
