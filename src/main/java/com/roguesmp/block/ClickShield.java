package com.roguesmp.block;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.Keys;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClickShield {

    private static final float SIZE = 1.02f;
    private static final int RELEASE_TICKS = 8;
    private static final int CHECK_PERIOD = 4;

    private final SmpBlock block;
    private final Map<UUID, Shield> shields = new HashMap<>();
    private @Nullable BukkitTask task;

    public ClickShield(SmpBlock block) {
        this.block = block;
    }

    public static @Nullable BlockPos ownerOf(Entity entity) {
        if (!(entity instanceof Interaction)) return null;
        String serialized = entity.getPersistentDataContainer().get(Keys.BLOCK_POS, PersistentDataType.STRING);
        return serialized == null ? null : BlockPos.parse(serialized).result();
    }

    public void refresh(Player player) {
        int now = Bukkit.getCurrentTick();
        Shield shield = shields.get(player.getUniqueId());
        if (shield != null && shield.entity.isValid()) {
            shield.lastClick = now;
            return;
        }

        shields.put(player.getUniqueId(), new Shield(spawn(player), now));
        if (task == null) task = Bukkit.getScheduler().runTaskTimer(RogueSmpCore.getInstance(), this::expire, CHECK_PERIOD, CHECK_PERIOD);
    }

    public void release(Player player) {
        Shield shield = shields.remove(player.getUniqueId());
        if (shield != null) shield.entity.remove();
        stopIfEmpty();
    }

    public void clear() {
        for (Shield shield : shields.values()) shield.entity.remove();
        shields.clear();
        stopIfEmpty();
    }

    private Interaction spawn(Player player) {
        Location spot = block.getLocation().toBlockLocation().add(0.5, -(SIZE - 1) / 2, 0.5);
        Interaction entity = spot.getWorld().spawn(spot, Interaction.class, interaction -> {
            interaction.setPersistent(false);
            interaction.setVisibleByDefault(false);
            interaction.setInteractionWidth(SIZE);
            interaction.setInteractionHeight(SIZE);
            interaction.getPersistentDataContainer().set(Keys.BLOCK_POS, PersistentDataType.STRING, block.getPos().serialize());
        });
        player.showEntity(RogueSmpCore.getInstance(), entity);
        return entity;
    }

    private void expire() {
        int now = Bukkit.getCurrentTick();
        shields.values().removeIf(shield -> {
            if (shield.entity.isValid() && now - shield.lastClick <= RELEASE_TICKS) return false;
            shield.entity.remove();
            return true;
        });
        stopIfEmpty();
    }

    private void stopIfEmpty() {
        if (!shields.isEmpty() || task == null) return;
        task.cancel();
        task = null;
    }

    private static final class Shield {
        private final Interaction entity;
        private int lastClick;

        private Shield(Interaction entity, int lastClick) {
            this.entity = entity;
            this.lastClick = lastClick;
        }
    }
}
