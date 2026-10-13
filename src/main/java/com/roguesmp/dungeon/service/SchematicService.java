package com.roguesmp.dungeon.service;

import com.roguesmp.dungeon.data.definition.Schemeta;
import com.roguesmp.dungeon.manager.SchemetaManager;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

public class SchematicService {

    private final SchemetaManager schemetaManager;

    public SchematicService(SchemetaManager schemetaManager) {
        this.schemetaManager = schemetaManager;
    }

    // ── SchematicService ────────────────────────────────────

    public BoundingBox paste(String schemetaId, Location location) {
        return pasteInternal(schemetaId, location);
    }

    private BoundingBox pasteInternal(String schemetaId, Location location) {
        // 1. Lấy metadata — fail fast nếu không tồn tại
        Schemeta schemeta = schemetaManager.get(schemetaId)
                .orElseThrow(() -> new IllegalArgumentException("Schemeta not found: " + schemetaId));

        // 2. Lấy clipboard qua lazy cache — dùng id, không dùng name
        Clipboard clipboard = schemetaManager.getClipboard(schemeta.getId())
                .orElseThrow(() -> new IllegalStateException("Schematic file missing for: " + schemetaId));

        try (var editSession = WorldEdit.getInstance()
                .newEditSession(BukkitAdapter.adapt(location.getWorld()))) {

            ClipboardHolder holder = new ClipboardHolder(clipboard);

            holder.createPaste(editSession)
                    .to(BlockVector3.at(location.getX(), location.getY(), location.getZ()))
                    .copyEntities(true)
                    .copyBiomes(true)
                    .build()
                    .resume(null);

            return calcBoundingBox(clipboard, location);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to paste schematic: " + schemetaId, e);
        }
    }

    private BoundingBox calcBoundingBox(Clipboard clipboard, Location pasteLocation) {
        Region region = clipboard.getRegion();
        BlockVector3 min    = region.getMinimumPoint();
        BlockVector3 max    = region.getMaximumPoint();
        BlockVector3 origin = clipboard.getOrigin();

        double ox = pasteLocation.getX() - origin.x();
        double oy = pasteLocation.getY() - origin.y();
        double oz = pasteLocation.getZ() - origin.z();

        return new BoundingBox(
                min.x() + ox, min.y() + oy, min.z() + oz,
                max.x() + ox, max.y() + oy, max.z() + oz
        );
    }
}