package com.roguesmp.dungeon_v2.data.definition.spawner.behavior.impl;

import com.roguesmp.dungeon_v2.data.definition.spawner.behavior.BaseBehavior;
import com.roguesmp.dungeon_v2.data.definition.spawner.behavior.BehaviorData;
import com.roguesmp.dungeon_v2.utils.DungeonEcho;
import com.roguesmp.dungeon_v2.utils.NameSpaceKeys;
import com.roguesmp.dungeon_v2.utils.Razdon;
import com.roguesmp.registry.EntityRegistry;
import org.bukkit.*;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class TeleportSpawner extends BaseBehavior {

    private final int maxTeleports;
    private final double teleportRadius;     // bán kính tìm player
    private final double safeCheckRadius;    // bán kính check vị trí an toàn quanh player
    private int teleportCount = 0;

    private final JavaPlugin plugin;

    protected TeleportSpawner(BehaviorData data, JavaPlugin plugin) {
        super(data);
        this.maxTeleports = data.getInt("max_teleports", 3);
        this.teleportRadius = data.getDouble("teleport_radius", 20.0);
        this.safeCheckRadius = data.getDouble("safe_check_radius", 3.0);
        this.plugin = plugin;
    }

    // =========================================================
    // onBreak: chưa hết lần tele → chặn vỡ, tele spawner đi
    // =========================================================
    @Override
    public boolean onBreak(Player player, Location location) {
        if (completed) return true;

        if (teleportCount < maxTeleports) {
            int remaining = maxTeleports - teleportCount;
            DungeonEcho.info(player,
                    "The spawner phases away! (" + remaining + " escapes remaining)");

            Location dest = findTeleportDestination(location, player.getWorld());
            if (dest != null) {
                playTeleportTrail(location, dest);
                teleportSpawner(location, dest);
                teleportCount++;
            } else {
                // Không tìm được vị trí → coi như dùng 1 lần nhưng không di chuyển
                teleportCount++;
                DungeonEcho.info(player, "The spawner trembles but holds its ground.");
            }

            if (teleportCount >= maxTeleports) {
                completed = true;
                DungeonEcho.success(player, "The spawner has nowhere left to run!");
            }

            return false;
        }

        DungeonEcho.success(player, "The spawner is destroyed!");
        return true;
    }

    // =========================================================
    // onSpawn: tìm player gần nhất trong vùng, tele mob đến đó
    // =========================================================
    @Override
    public boolean onSpawn(LivingEntity entity, List<Player> players, Location location) {
        if (completed) return true;
        if (players.isEmpty()) return true;

        World world = location.getWorld();
        if (world == null) return true;

        EntityType type = entity.getType();

        // Mỗi player spawn 2-4 mob tele đến họ
        for (Player player : players) {
            if (!player.getWorld().equals(world)) continue;

            int count = Razdon.getInstance().nextIntInRange(2, 4);
            for (int i = 0; i < count; i++) {
                Location dest = findSafeLocationNearPlayer(player);
                if (dest == null) continue;

                LivingEntity spawned = (LivingEntity) world.spawnEntity(dest, type);
                playTeleportTrail(location, dest);
            }
        }

        // Cancel entity gốc vì đã tự spawn thay thế
        entity.remove();
        return false;
    }

    // =========================================================
    // Helpers
    // =========================================================

    /** Tìm vị trí an toàn ngẫu nhiên quanh player */
    private Location findSafeLocationNearPlayer(Player player) {
        Razdon rng = Razdon.getInstance();
        Location playerLoc = player.getLocation();

        for (int i = 0; i < 15; i++) {
            Location candidate = rng.nextLocationXZ(playerLoc, 1.5, safeCheckRadius);
            if (isSafe(candidate)) return candidate;
        }
        return null;
    }

    /** Tìm vị trí ngẫu nhiên trong world để spawner tele đến (tránh xa player) */
    private Location findTeleportDestination(Location current, World world) {
        Razdon rng = Razdon.getInstance();

        for (int i = 0; i < 20; i++) {
            Location candidate = rng.nextLocationXZ(current, 10.0, teleportRadius);
            if (isSafe(candidate)) return candidate;
        }
        return null;
    }

    private boolean isSafe(Location loc) {
        return loc.getBlock().isPassable()
                && loc.clone().add(0, 1, 0).getBlock().isPassable()
                && !loc.clone().subtract(0, 1, 0).getBlock().isPassable(); // có block phía dưới
    }

    /** Particle trail từ origin → dest để chỉ hướng tele */
    private void playTeleportTrail(Location origin, Location dest) {
        World world = origin.getWorld();
        if (world == null) return;

        world.spawnParticle(Particle.PORTAL, origin, 40, 0.3, 0.5, 0.3, 0.1);
        world.spawnParticle(Particle.WITCH, origin, 15, 0.2, 0.4, 0.2, 0);

        // Trail dọc theo đường đi
        drawTrail(origin, dest, world);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            world.spawnParticle(Particle.PORTAL, dest, 40, 0.3, 0.5, 0.3, 0.1);
            world.spawnParticle(Particle.FLASH, dest, 1, 0, 0, 0, 0);
            world.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        }, 1L);

        world.playSound(origin, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
    }

    private void drawTrail(Location from, Location to, World world) {
        double distance = from.distance(to);
        int steps = (int) (distance * 2);
        if (steps == 0) return;

        double dx = (to.getX() - from.getX()) / steps;
        double dy = (to.getY() - from.getY()) / steps;
        double dz = (to.getZ() - from.getZ()) / steps;

        for (int i = 0; i <= steps; i++) {
            Location point = from.clone().add(dx * i, dy * i + 0.5, dz * i);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, point, 1, 0, 0, 0, 0);
        }
    }

    private void teleportSpawner(Location from, Location to) {
        // Lấy meta cũ trước khi xóa
        CreatureSpawner oldSpawner = (CreatureSpawner) from.getBlock().getState();
        PersistentDataContainer oldPdc = oldSpawner.getPersistentDataContainer();
        String iid = oldPdc.get(NameSpaceKeys.SPAWNER_IID_KEY, PersistentDataType.STRING);

        to.getBlock().setType(Material.SPAWNER);

        // Copy IID sang spawner mới
        CreatureSpawner newSpawner = (CreatureSpawner) to.getBlock().getState();
        if (iid != null) {
            newSpawner.getPersistentDataContainer()
                    .set(NameSpaceKeys.SPAWNER_IID_KEY, PersistentDataType.STRING, iid);
        }
        newSpawner.update();

        from.getBlock().setType(Material.AIR);
    }

    @Override
    public void reset() {
        super.reset();
        teleportCount = 0;
    }
}