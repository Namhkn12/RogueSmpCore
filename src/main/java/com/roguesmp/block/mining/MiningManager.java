package com.roguesmp.block.mining;

import com.roguesmp.block.BlockPos;
import com.roguesmp.block.BlockVisual;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.manager.BlockManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Barrier can't be broken in survival, so mining is simulated: the damage event opens a session, then
 * a per-tick task advances it via {@link MiningSpeedCalculator}. Damage events must not be cancelled, or the
 * matching abort event never fires.
 */
public class MiningManager {

    private static final double MINING_PARTICLE_REACH = 8.0;

    private final BlockManager manager;
    private final Map<UUID, BreakSession> sessions = new HashMap<>();
    private final Map<BlockPos, UUID> minerAt = new HashMap<>();

    public MiningManager(BlockManager manager, Plugin plugin) {
        this.manager = manager;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void onDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL) return;

        SmpBlock block = manager.get(BlockPos.of(event.getBlock()));
        if (block == null || !block.isHydrated() || block.getProperties().hardness() < 0) return;

        stop(player.getUniqueId());

        int ticks = MiningSpeedCalculator.ticksToBreak(player, block.getProperties());
        if (ticks == MiningSpeedCalculator.CANNOT_BREAK) return;
        if (ticks == 0) {
            manager.breakBlock(event.getBlock(), player, true);
            return;
        }

        UUID crackOverlayId = BlockVisual.spawnCrackOverlay(block.getPos(), 0, event.getPlayer()).getUniqueId();
        BreakSession session = new BreakSession(player.getUniqueId(), block, player.getInventory().getHeldItemSlot(), crackOverlayId);
        sessions.put(player.getUniqueId(), session);
        minerAt.put(block.getPos(), player.getUniqueId());
    }

    public void stop(UUID playerId) {
        BreakSession session = sessions.remove(playerId);
        if (session != null) finishSession(session);
    }

    private void finishSession(BreakSession session) {
        minerAt.remove(session.block().getPos(), session.playerId());
        removeCrackOverlay(session);
    }

    public void cancelAt(BlockPos pos) {
        UUID playerId = minerAt.remove(pos);
        if (playerId != null) stop(playerId);
    }

    public void stopAll() {
        sessions.values().forEach(this::removeCrackOverlay);
        sessions.clear();
        minerAt.clear();
    }

    private void tick() {
        Iterator<BreakSession> iterator = sessions.values().iterator();
        while (iterator.hasNext()) {
            BreakSession session = iterator.next();
            Player player = Bukkit.getPlayer(session.playerId());

            if (!isStillMining(player, session)) {
                iterator.remove();
                finishSession(session);
                continue;
            }

            int ticks = MiningSpeedCalculator.ticksToBreak(player, session.block().getProperties());
            if (ticks == MiningSpeedCalculator.CANNOT_BREAK) {
                iterator.remove();
                finishSession(session);
                continue;
            }

            if (ticks == 0 || session.advance(ticks)) {
                iterator.remove();
                finishSession(session);
                manager.breakBlock(session.block().getLocation().getBlock(), player, true);
                continue;
            }

            spawnMiningParticles(player, session.block());

            if (session.crackChanged(ticks)) {
                updateCrackOverlay(session);
            }
        }
    }

    private void spawnMiningParticles(Player player, SmpBlock block) {
        BlockFace face = player.getTargetBlockFace((int) MINING_PARTICLE_REACH);
        if (face != null) manager.spawnMiningParticles(block, face);
    }

    private void updateCrackOverlay(BreakSession session) {
        Entity overlay = crackOverlay(session);
        if (overlay != null) BlockVisual.updateCrackOverlay(overlay, session.shownStage());
    }

    private void removeCrackOverlay(BreakSession session) {
        Entity overlay = crackOverlay(session);
        if (overlay != null) overlay.remove();
    }

    private @Nullable Entity crackOverlay(BreakSession session) {
        return Bukkit.getEntity(session.crackOverlayId());
    }

    // BlockDamageAbortEvent alone isn't reliable enough to catch every case this checks for.
    private boolean isStillMining(Player player, BreakSession session) {
        if (player == null || !player.isOnline() || player.isDead() || player.getGameMode() != GameMode.SURVIVAL) return false;
        if (manager.get(session.block().getPos()) != session.block()) return false;
        if (player.getInventory().getHeldItemSlot() != session.heldSlot()) return false;

        Location center = session.block().getLocation().toCenterLocation();
        if (!center.getWorld().equals(player.getWorld())) return false;
        double reach = player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE).getValue();
        return player.getEyeLocation().distanceSquared(center) <= reach * reach;
    }
}
