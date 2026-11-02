package com.roguesmp.spawner;

import com.roguesmp.constant.Keys;
import com.roguesmp.utils.GlowUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
public final class SpawnerVisualizer {

    private static final double RANGE = 64;

    private final SpawnerInstanceManager instances;
    private final Set<UUID> enabled = new HashSet<>();
    private final Map<UUID, Set<Block>> glowing = new HashMap<>();

    SpawnerVisualizer(SpawnerInstanceManager instances) {
        this.instances = instances;
    }

    public boolean isEnabled(Player player) {
        return enabled.contains(player.getUniqueId());
    }

    public void toggle(Player player) {
        if (enabled.remove(player.getUniqueId())) {
            clear(player);
        } else {
            enabled.add(player.getUniqueId());
            update(player);
        }
    }

    void tick() {
        for (UUID id : List.copyOf(enabled)) {
            Player player = Bukkit.getPlayer(id);
            if (player == null) {
                forget(id);
                continue;
            }
            update(player);
        }
    }

    void forget(UUID id) {
        enabled.remove(id);
        glowing.remove(id);
    }

    private void update(Player player) {
        Set<Block> targets = targetsFor(player);
        Set<Block> current = glowing.computeIfAbsent(player.getUniqueId(), id -> new HashSet<>());
        current.removeIf(block -> {
            if (targets.contains(block)) return false;
            GlowUtils.unsetGlow(block, player);
            return true;
        });
        for (Block block : targets) {
            if (current.add(block)) GlowUtils.glow(block, player, ChatColor.GOLD);
        }
    }

    private Set<Block> targetsFor(Player player) {
        String templateId = player.getInventory().getItemInMainHand().getPersistentDataContainer().get(Keys.SPAWNER_TID, PersistentDataType.STRING);
        if (templateId == null) return Set.of();

        Location origin = player.getLocation();
        return instances.getByTemplate(templateId).stream()
                .map(SpawnerInstance::getLocation)
                .filter(location -> origin.getWorld().equals(location.getWorld()) && origin.distanceSquared(location) <= RANGE * RANGE)
                .filter(SpawnerInstanceManager::isLoadedSpawner)
                .map(Location::getBlock)
                .collect(Collectors.toSet());
    }

    private void clear(Player player) {
        Set<Block> blocks = glowing.remove(player.getUniqueId());
        if (blocks != null) blocks.forEach(block -> GlowUtils.unsetGlow(block, player));
    }
}
