package com.roguesmp.dungeon.room;

import com.roguesmp.RogueSmpCore;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RoomSchematics {

    private static final String FOLDER = "dungeon/schematics";
    private static final Map<String, Clipboard> CACHE = new ConcurrentHashMap<>();

    private RoomSchematics() {
    }

    // Main thread only.
    public static BoundingBox paste(String name, Location location) {
        Clipboard clipboard = CACHE.computeIfAbsent(name, RoomSchematics::load);
        try (var editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location.getWorld()))) {
            new ClipboardHolder(clipboard).createPaste(editSession)
                    .to(BlockVector3.at(location.getX(), location.getY(), location.getZ()))
                    .copyEntities(true)
                    .copyBiomes(true)
                    .build()
                    .resume(null);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to paste schematic " + name, e);
        }

        Region region = clipboard.getRegion();
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        BlockVector3 origin = clipboard.getOrigin();
        double ox = location.getX() - origin.x();
        double oy = location.getY() - origin.y();
        double oz = location.getZ() - origin.z();
        return new BoundingBox(min.x() + ox, min.y() + oy, min.z() + oz, max.x() + ox + 1, max.y() + oy + 1, max.z() + oz + 1);
    }

    public static void save(Player player, String name) {
        Region region;
        try {
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
            region = session.getSelection(session.getSelectionWorld());
        } catch (Exception e) {
            throw new IllegalStateException("Không tìm thấy vùng chọn WorldEdit.", e);
        }

        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(BukkitAdapter.asBlockVector(player.getLocation()));
        try (var editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(player.getWorld()))) {
            ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
            copy.setCopyingEntities(true);
            copy.setCopyingBiomes(true);
            Operations.complete(copy);
        } catch (Exception e) {
            throw new IllegalStateException("Không thể sao chép vùng chọn.", e);
        }

        File file = file(name);
        file.getParentFile().mkdirs();
        try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(new FileOutputStream(file))) {
            writer.write(clipboard);
        } catch (IOException e) {
            throw new IllegalStateException("Không thể lưu schematic " + name, e);
        }
        CACHE.put(name, clipboard);
    }

    public static List<String> names() {
        String[] files = new File(RogueSmpCore.getInstance().getDataFolder(), FOLDER).list((dir, file) -> file.endsWith(".schem"));
        return files == null ? List.of() : Arrays.stream(files).map(file -> file.substring(0, file.length() - ".schem".length())).sorted().toList();
    }

    public static void clearCache() {
        CACHE.clear();
    }

    private static File file(String name) {
        return new File(new File(RogueSmpCore.getInstance().getDataFolder(), FOLDER), name + ".schem");
    }

    private static Clipboard load(String name) {
        File file = file(name);
        if (!file.exists()) throw new IllegalStateException("Schematic not found: " + file.getPath());
        try (ClipboardReader reader = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getReader(new FileInputStream(file))) {
            return reader.read();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read schematic " + name, e);
        }
    }
}
