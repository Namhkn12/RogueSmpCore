package com.roguesmp.dungeon.service.impl;

import com.roguesmp.dungeon.data.Party;
import com.roguesmp.dungeon.instance.DungeonInstance;
import com.roguesmp.dungeon.service.IRewardService;
import com.roguesmp.dungeon.utils.NameSpaceKeys;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

public class RewardService implements IRewardService {

    public static final NamespacedKey REWARD_OWNER_KEY = new NamespacedKey("roguesmp", "reward_owner");

    // Track các chest đang trong trạng thái animation để tránh click nhiều lần
    private final Set<Location> activeChests = new HashSet<>();
    // Track ItemDisplay entity của từng chest để cleanup
    private final Map<Location, ItemDisplay> chestDisplays = new HashMap<>();

    private final JavaPlugin plugin;
//    private final RewardManager rewardManager;

    public RewardService(JavaPlugin plugin) {
        this.plugin = plugin;
//        this.rewardManager = rewardManager;
    }

    // -------------------------------------------------------------------------
    // Tag block là reward chest khi paste schematic
    // -------------------------------------------------------------------------

    /**
     * Gọi hàm này sau khi paste schematic reward room để đánh dấu block là reward chest.
     * @param block   Block cần tag (thường là CHEST)
     * @param ownerId UUID của player sở hữu chest này
     */
    public void tagRewardChest(Block block, UUID ownerId) {
        if (!(block.getState() instanceof TileState tileState)) return;

        PersistentDataContainer pdc = tileState.getPersistentDataContainer();
        pdc.set(NameSpaceKeys.REWARD_CID_KEY, PersistentDataType.BYTE, (byte) 1);
        pdc.set(REWARD_OWNER_KEY, PersistentDataType.STRING, ownerId.toString());
        tileState.update();

        // Spawn ItemDisplay animation ngay khi tag
        spawnChestAnimation(block, getRewardPreviewItem(ownerId));
    }

    // -------------------------------------------------------------------------
    // Event: chặn mở chest và xử lý animation + bắn item
    // -------------------------------------------------------------------------



    // -------------------------------------------------------------------------
    // Core logic: animation rồi bắn item
    // -------------------------------------------------------------------------

    private void openRewardChest(Player player, Block block, UUID ownerId) {
        Location loc = block.getLocation();

        // Thay block thành AIR sau delay ngắn (hiệu ứng "vỡ" chest)
        // Sound mở chest
        loc.getWorld().playSound(loc, Sound.BLOCK_CHEST_OPEN, 1f, 1f);

        // Sau 1.5s: spin nhanh hơn rồi bắn item
        new BukkitRunnable() {
            int tick = 0;
            final int SPIN_TICKS = 30; // 1.5 giây spin

            @Override
            public void run() {
                if (tick >= SPIN_TICKS) {
                    // Kết thúc animation
                    cleanupChestAnimation(loc);

                    // Xóa block chest
                    block.setType(Material.AIR);

                    // Bắn item ra
                    List<ItemStack> rewards = getTemporaryRewards();
                    spawnRewardItems(loc.clone().add(0.5, 0.5, 0.5), rewards);

                    // Sound và particle
                    loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                    loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0.5, 0.5, 0.5), 30, 0.3, 0.3, 0.3, 0.1);

                    activeChests.remove(loc);
                    this.cancel();
                    return;
                }

                // Tăng tốc độ quay theo thời gian
                rotateChestDisplay(loc, tick / (float) SPIN_TICKS);
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // -------------------------------------------------------------------------
    // ItemDisplay animation
    // -------------------------------------------------------------------------

    private void spawnChestAnimation(Block block, ItemStack displayItem) {
        Location spawnLoc = block.getLocation().clone().add(0.5, 1.4, 0.5);
        World world = spawnLoc.getWorld();

        ItemDisplay display = world.spawn(spawnLoc, ItemDisplay.class, entity -> {
            entity.setItemStack(displayItem);
            entity.setBillboard(ItemDisplay.Billboard.VERTICAL); // Chỉ rotate theo trục Y
            entity.setTransformation(new Transformation(
                    new org.joml.Vector3f(0, 0, 0),           // translation
                    new AxisAngle4f(0, 0, 1, 0),              // left rotation
                    new Vector3f(0.6f, 0.6f, 0.6f),           // scale
                    new AxisAngle4f(0, 0, 1, 0)               // right rotation
            ));
            entity.setGlowing(true);
        });

        chestDisplays.put(block.getLocation(), display);
    }

    private void rotateChestDisplay(Location chestLoc, float progress) {
        ItemDisplay display = chestDisplays.get(chestLoc);
        if (display == null || !display.isValid()) return;

        // Tăng tốc độ quay + bounce lên xuống
        float angle = (float) (progress * Math.PI * 8); // 4 vòng trong 1.5s, tăng dần
        float bobY = (float) Math.sin(progress * Math.PI * 6) * 0.1f;

        display.setTransformation(new Transformation(
                new Vector3f(0, bobY, 0),
                new AxisAngle4f(angle, 0, 1, 0),   // rotate Y
                new Vector3f(0.6f, 0.6f, 0.6f),
                new AxisAngle4f(0, 0, 1, 0)
        ));
    }

    private void cleanupChestAnimation(Location chestLoc) {
        ItemDisplay display = chestDisplays.remove(chestLoc);
        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    // -------------------------------------------------------------------------
    // Bắn item ra theo hướng ngẫu nhiên
    // -------------------------------------------------------------------------

    private void spawnRewardItems(Location center, List<ItemStack> items) {
        World world = center.getWorld();
        Random random = new Random();

        for (ItemStack item : items) {
            org.bukkit.entity.Item dropped = world.dropItem(center, item);
            // Bắn item theo hướng ngẫu nhiên nhưng hướng lên
            dropped.setVelocity(new org.bukkit.util.Vector(
                    (random.nextDouble() - 0.5) * 0.4,
                    0.3 + random.nextDouble() * 0.3,
                    (random.nextDouble() - 0.5) * 0.4
            ));
            // Không cho item merge với nhau
            dropped.setPickupDelay(40); // 2s delay trước khi nhặt được
        }
    }

    // -------------------------------------------------------------------------
    // Helper: item hiển thị trên đầu chest (preview reward hoặc icon dungeon)
    // -------------------------------------------------------------------------

    private ItemStack getRewardPreviewItem(UUID ownerId) {
        // Có thể customize theo loại reward của player
        // Tạm thời dùng NETHER_STAR làm icon mặc định
        return new ItemStack(Material.NETHER_STAR);
    }

    // -------------------------------------------------------------------------
    // IRewardService
    // -------------------------------------------------------------------------

    @Override
    public void sendToRewardRoom(Party party, DungeonInstance instance) {
        // từ instance lấy ra vị trí gốc của region qua InstanceRegion
        // gọi schematic service paste reward room theo logic
        // dựa theo số player trong party paste ra số reward room tương ứng chồng lên nhau cùng 1 location chỉ khác Y
        // loop party và teleport đến từng reward room
    }

    private List<ItemStack> getTemporaryRewards() {
        return List.of(
                new ItemStack(Material.NETHERITE_INGOT, 1),
                new ItemStack(Material.DIAMOND, 3),
                new ItemStack(Material.GOLD_INGOT, 5),
                new ItemStack(Material.EMERALD, 4),
                new ItemStack(Material.IRON_INGOT, 8)
        );
    }
}