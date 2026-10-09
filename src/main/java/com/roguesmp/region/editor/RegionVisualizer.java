package com.roguesmp.region.editor;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.region.RegionSelection;
import com.roguesmp.region.ServerRegionManager;
import com.roguesmp.utils.Utils;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RegionVisualizer {

    private static final long TICK_INTERVAL = 10L;
    private static final double VIEW_DISTANCE = 64;
    private static final int MAX_POINTS_PER_EDGE = 32;

    private static final Particle.DustOptions REGION = new Particle.DustOptions(Color.AQUA, 1.0f);
    private static final Particle.DustOptions FOCUSED = new Particle.DustOptions(Color.ORANGE, 1.3f);
    private static final Particle.DustOptions FOCUSED_SHAPE = new Particle.DustOptions(Color.RED, 1.4f);
    private static final Particle.DustOptions SELECTION = new Particle.DustOptions(Color.LIME, 1.2f);

    private static RegionVisualizer INSTANCE;

    public record Focus(String regionId, int pieceIndex) { }

    private final Map<UUID, Focus> focuses = new HashMap<>();

    private RegionVisualizer(RogueSmpCore plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, TICK_INTERVAL, TICK_INTERVAL);
    }

    public static void init(RogueSmpCore plugin) {
        INSTANCE = new RegionVisualizer(plugin);
    }

    public static RegionVisualizer getInstance() {
        return INSTANCE;
    }

    public void focus(Player player, String regionId) {
        focus(player, regionId, -1);
    }

    public void focus(Player player, String regionId, int pieceIndex) {
        focuses.put(player.getUniqueId(), new Focus(regionId, pieceIndex));
    }

    public void clearFocus(Player player) {
        focuses.remove(player.getUniqueId());
    }

    public @Nullable Focus focusOf(Player player) {
        return focuses.get(player.getUniqueId());
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (RegionWand.isHolding(player)) render(player);
        }
    }

    private void render(Player player) {
        Focus focus = focuses.get(player.getUniqueId());

        for (Map.Entry<String, List<ProtectedRegion>> entry : ServerRegionManager.getInstance().regions(player.getWorld()).entrySet()) {
            boolean focusedRegion = focus != null && focus.regionId().equals(entry.getKey());
            List<ProtectedRegion> pieces = entry.getValue();
            for (int i = 0; i < pieces.size(); i++) {
                RegionOutline outline = RegionOutline.of(pieces.get(i));
                if (outline == null) continue;

                Particle.DustOptions dust = !focusedRegion ? REGION : focus.pieceIndex() == i ? FOCUSED_SHAPE : FOCUSED;
                draw(player, outline, dust);
            }
        }

        RegionSelection selection = RegionSelections.getInstance().current(player);
        if (selection != null) draw(player, RegionOutline.of(selection), SELECTION);

        List<String> here = ServerRegionManager.getInstance().sortedRegionIdsAt(player.getLocation());
        player.sendActionBar(Utils.fromString("<gray>Regions here: <white>" + (here.isEmpty() ? "none" : String.join(", ", here))));
    }

    private void draw(Player player, RegionOutline outline, Particle.DustOptions dust) {
        if (distanceTo(player, outline) > VIEW_DISTANCE) return;

        List<double[]> corners = outline.corners();
        for (int i = 0; i < corners.size(); i++) {
            double[] from = corners.get(i);
            double[] to = corners.get((i + 1) % corners.size());
            if (corners.size() > 1) {
                drawLine(player, from[0], outline.bottom(), from[1], to[0], outline.bottom(), to[1], dust);
                drawLine(player, from[0], outline.top(), from[1], to[0], outline.top(), to[1], dust);
            }
            drawLine(player, from[0], outline.bottom(), from[1], from[0], outline.top(), from[1], dust);
        }
    }

    private void drawLine(Player player, double ax, double ay, double az, double bx, double by, double bz, Particle.DustOptions dust) {
        double length = Math.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay) + (bz - az) * (bz - az));
        int points = Math.min(MAX_POINTS_PER_EDGE, Math.max(2, (int) length + 1));
        for (int i = 0; i < points; i++) {
            double t = i / (double) (points - 1);
            player.spawnParticle(Particle.DUST, ax + (bx - ax) * t, ay + (by - ay) * t, az + (bz - az) * t, 1, 0, 0, 0, 0, dust);
        }
    }

    private double distanceTo(Player player, RegionOutline outline) {
        Location location = player.getLocation();
        double dx = Math.max(Math.max(outline.minX() - location.getX(), 0), location.getX() - outline.maxX());
        double dy = Math.max(Math.max(outline.bottom() - location.getY(), 0), location.getY() - outline.top());
        double dz = Math.max(Math.max(outline.minZ() - location.getZ(), 0), location.getZ() - outline.maxZ());
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
