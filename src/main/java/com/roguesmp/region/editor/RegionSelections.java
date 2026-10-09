package com.roguesmp.region.editor;

import com.roguesmp.region.RegionSelection;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RegionSelections {

    private static RegionSelections INSTANCE;

    private final Map<UUID, RegionSelection> selections = new HashMap<>();
    private final Set<UUID> fullHeight = new HashSet<>();

    private RegionSelections() {
    }

    public static void init() {
        INSTANCE = new RegionSelections();
    }

    public static RegionSelections getInstance() {
        return INSTANCE;
    }

    public void addPoint(Player player, BlockVector3 point) {
        RegionSelection current = raw(player);
        selections.put(player.getUniqueId(), current == null ? RegionSelection.of(player.getWorld(), point) : current.withPoint(point));
    }

    public void undo(Player player) {
        RegionSelection current = raw(player);
        if (current == null) return;

        RegionSelection previous = current.withoutLast();
        if (previous == null) selections.remove(player.getUniqueId());
        else selections.put(player.getUniqueId(), previous);
    }

    public void clear(Player player) {
        selections.remove(player.getUniqueId());
    }

    public boolean toggleFullHeight(Player player) {
        if (!fullHeight.remove(player.getUniqueId())) {
            fullHeight.add(player.getUniqueId());
            return true;
        }
        return false;
    }

    public boolean isFullHeight(Player player) {
        return fullHeight.contains(player.getUniqueId());
    }

    public @Nullable RegionSelection current(Player player) {
        RegionSelection wand = raw(player);
        if (wand != null) return isFullHeight(player) ? wand.withFullHeight() : wand;
        return worldEditSelection(player);
    }

    private @Nullable RegionSelection raw(Player player) {
        RegionSelection selection = selections.get(player.getUniqueId());
        if (selection != null && !selection.world().equals(player.getWorld())) {
            selections.remove(player.getUniqueId());
            return null;
        }
        return selection;
    }

    private @Nullable RegionSelection worldEditSelection(Player player) {
        try {
            Region selection = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player))
                    .getSelection(BukkitAdapter.adapt(player.getWorld()));
            int minY = selection.getMinimumPoint().y();
            int maxY = selection.getMaximumPoint().y();

            List<BlockVector3> points;
            if (selection instanceof Polygonal2DRegion polygon) {
                points = polygon.getPoints().stream().map(point -> BlockVector3.at(point.x(), minY, point.z())).toList();
            } else {
                BlockVector3 min = selection.getMinimumPoint();
                BlockVector3 max = selection.getMaximumPoint();
                points = List.of(min, max);
            }
            return new RegionSelection(player.getWorld(), points, minY, maxY);
        } catch (IncompleteRegionException e) {
            return null;
        }
    }
}
