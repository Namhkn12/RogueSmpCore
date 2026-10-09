package com.roguesmp.region;

import com.roguesmp.RogueSmpCore;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.FlagContext;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ServerRegionManager {

    private static final String SHAPE_INFIX = "_shape_";
    private static final Pattern SHAPE_SUFFIX = Pattern.compile(SHAPE_INFIX + "(\\d+)$");

    private static ServerRegionManager INSTANCE;

    private ServerRegionManager() {
    }

    public static void init() {
        INSTANCE = new ServerRegionManager();
    }

    public static ServerRegionManager getInstance() {
        return INSTANCE;
    }

    public static String logicalId(ProtectedRegion region) {
        ProtectedRegion parent = region.getParent();
        return isGroupTemplate(parent) ? parent.getId() : region.getId();
    }

    private static boolean isGroupTemplate(@Nullable ProtectedRegion region) {
        return region != null
                && region.getType() == RegionType.GLOBAL
                && region.getFlag(SmpRegionFlags.GROUPED) == StateFlag.State.ALLOW;
    }

    private static String shapeId(String regionId, int index) {
        return regionId + SHAPE_INFIX + index;
    }

    private static int shapeIndex(String worldGuardId) {
        Matcher matcher = SHAPE_SUFFIX.matcher(worldGuardId);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    public Map<String, List<ProtectedRegion>> regions(World world) {
        Map<String, List<ProtectedRegion>> groups = new TreeMap<>();
        RegionManager regionManager = getRegionManager(world);
        if (regionManager == null) return groups;

        for (ProtectedRegion region : regionManager.getRegions().values()) {
            if (region.getType() == RegionType.GLOBAL) continue;
            groups.computeIfAbsent(logicalId(region), id -> new ArrayList<>()).add(region);
        }
        groups.values().forEach(shapes -> shapes.sort(
                Comparator.comparingInt((ProtectedRegion shape) -> shapeIndex(shape.getId())).thenComparing(ProtectedRegion::getId)));
        return groups;
    }

    public List<ProtectedRegion> pieces(World world, String id) {
        RegionManager regionManager = getRegionManager(world);
        if (regionManager == null) return List.of();

        ProtectedRegion direct = regionManager.getRegion(id);
        if (direct == null) return List.of();
        if (direct.getType() != RegionType.GLOBAL) return List.of(direct);
        if (!isGroupTemplate(direct)) return List.of();

        List<ProtectedRegion> shapes = new ArrayList<>();
        for (ProtectedRegion region : regionManager.getRegions().values()) {
            if (region.getType() != RegionType.GLOBAL && direct.equals(region.getParent())) shapes.add(region);
        }
        shapes.sort(Comparator.comparingInt((ProtectedRegion shape) -> shapeIndex(shape.getId())).thenComparing(ProtectedRegion::getId));
        return shapes;
    }

    public @Nullable ProtectedRegion get(World world, String id) {
        RegionManager regionManager = getRegionManager(world);
        if (regionManager == null) return null;

        ProtectedRegion region = regionManager.getRegion(id);
        if (region == null) return null;
        if (region.getType() == RegionType.GLOBAL) return isGroupTemplate(region) ? region : null;
        return region;
    }

    public record Bounds(BlockVector3 min, BlockVector3 max) {

        public double centerX() {
            return (min.x() + max.x() + 1) / 2.0;
        }

        public double centerZ() {
            return (min.z() + max.z() + 1) / 2.0;
        }
    }

    public static @Nullable Bounds boundsOf(List<ProtectedRegion> shapes) {
        if (shapes.isEmpty()) return null;

        BlockVector3 min = shapes.getFirst().getMinimumPoint();
        BlockVector3 max = shapes.getFirst().getMaximumPoint();
        for (ProtectedRegion shape : shapes) {
            min = min.getMinimum(shape.getMinimumPoint());
            max = max.getMaximum(shape.getMaximumPoint());
        }
        return new Bounds(min, max);
    }

    public static boolean contains(List<ProtectedRegion> shapes, int x, int y, int z) {
        for (ProtectedRegion shape : shapes) {
            if (shape.contains(x, y, z)) return true;
        }
        return false;
    }

    public @Nullable Bounds bounds(World world, String id) {
        return boundsOf(pieces(world, id));
    }

    public String displayName(World world, String id) {
        ProtectedRegion settings = get(world, id);
        String name = settings == null ? null : settings.getFlag(SmpRegionFlags.DISPLAY_NAME);
        return name == null || name.isBlank() ? id : name;
    }

    public void add(World world, ProtectedRegion region) {
        RegionManager regionManager = getRegionManager(world);
        
        if (regionManager == null) {
            RogueSmpCore.LOGGER.error("WorldGuard region manager could not be resolved for world: {}", world.getName());
            return;
        }

        regionManager.addRegion(region);
    }

    public void merge(World world, String id, RegionSelection selection) {
        ProtectedRegion settings = get(world, id);
        if (settings == null) {
            add(world, build(id, selection));
            return;
        }

        ProtectedRegion template = settings.getType() == RegionType.GLOBAL ? settings : group(world, settings);
        ProtectedRegion shape = build(nextShapeId(world, id), selection, template.isTransient());
        attach(template, shape);
        add(world, shape);
    }

    public void replaceShape(World world, String id, RegionSelection selection) {
        ProtectedRegion settings = get(world, id);
        if (settings == null) {
            add(world, build(id, selection));
            return;
        }

        if (settings.getType() != RegionType.GLOBAL) {
            ProtectedRegion replacement = build(id, selection, settings.isTransient());
            replacement.copyFrom(settings);
            add(world, replacement);
            return;
        }

        pieces(world, id).forEach(shape -> removeRegion(world, shape.getId()));
        ProtectedRegion shape = build(shapeId(id, 1), selection, settings.isTransient());
        attach(settings, shape);
        add(world, shape);
    }

    public boolean removePiece(World world, String id, int index) {
        ProtectedRegion settings = get(world, id);
        List<ProtectedRegion> shapes = pieces(world, id);
        if (settings == null || settings.getType() != RegionType.GLOBAL || shapes.size() <= 1 || index < 0 || index >= shapes.size()) return false;

        removeRegion(world, shapes.get(index).getId());
        return true;
    }

    public void remove(World world, String id) {
        ProtectedRegion settings = get(world, id);
        if (settings == null) return;

        if (settings.getType() == RegionType.GLOBAL) pieces(world, id).forEach(shape -> removeRegion(world, shape.getId()));
        removeRegion(world, id);
    }

    public void editMembers(World world, String id, Consumer<DefaultDomain> edit) {
        ProtectedRegion settings = get(world, id);
        if (settings == null) return;

        DefaultDomain members = settings.getMembers();
        edit.accept(members);
        settings.setMembers(members);
    }

    public void setPriority(World world, String id, int priority) {
        ProtectedRegion settings = get(world, id);
        if (settings == null) return;

        settings.setPriority(priority);
        pieces(world, id).forEach(shape -> shape.setPriority(priority));
    }

    public void setFlag(World world, String id, String name, String value, RegionGroup group) throws InvalidFlagFormat {
        Flag<?> flag = WorldGuard.getInstance().getFlagRegistry().get(name);
        if (flag == null) throw new InvalidFlagFormat("Unknown flag '" + name + "'.");

        ProtectedRegion settings = get(world, id);
        if (settings == null) return;

        putFlag(settings, flag, value, group);
    }

    public void clearFlag(World world, String id, Flag<?> flag) {
        ProtectedRegion settings = get(world, id);
        if (settings == null) return;

        settings.setFlag(flag, null);
        if (flag.getRegionGroupFlag() != null) settings.setFlag(flag.getRegionGroupFlag(), null);
    }

    public @Nullable String flagError(String name, String value) {
        Flag<?> flag = WorldGuard.getInstance().getFlagRegistry().get(name);
        if (flag == null) return "Unknown flag '" + name + "'.";

        try {
            flag.parseInput(FlagContext.create().setInput(value).build());
            return null;
        } catch (InvalidFlagFormat e) {
            return e.getMessage();
        }
    }

    /**
     * This is not blocking
     */
    public boolean saveChanges(World world) {
        RegionManager regionManager = getRegionManager(world);
        if (regionManager == null) {
            RogueSmpCore.LOGGER.error("WorldGuard region manager could not be resolved for world: {} during save!", world.getName());
            return false;
        }
        try {
            regionManager.saveChanges();
            return true;
        } catch (StorageException e) {
            RogueSmpCore.LOGGER.error("WorldGuard region manager could not save changes for world: {}", regionManager.getName());
        }
        return false;
    }

    public ProtectedRegion addTransientCuboid(World world, String id, BlockVector3 min, BlockVector3 max) {
        ProtectedRegion region = new ProtectedCuboidRegion(id, true, min, max);
        add(world, region);
        return region;
    }

    public ProtectedRegion addTransientPolygon(World world, String id, List<BlockVector2> corners, int minY, int maxY) {
        ProtectedRegion region = new ProtectedPolygonalRegion(id, true, corners, minY, maxY);
        add(world, region);
        return region;
    }

    public void removeTransient(World world, String idPrefix) {
        RegionManager regionManager = getRegionManager(world);
        if (regionManager == null) return;

        regionManager.getRegions().values().stream()
                .filter(region -> region.isTransient() && region.getId().startsWith(idPrefix))
                .map(ProtectedRegion::getId)
                .toList()
                .forEach(regionManager::removeRegion);
    }

    public ProtectedRegion build(String id, RegionSelection selection) {
        return build(id, selection, false);
    }

    public ProtectedRegion build(String id, RegionSelection selection, boolean transientRegion) {
        if (!selection.isPolygon()) {
            BlockVector3 first = selection.points().get(0);
            BlockVector3 second = selection.points().get(1);
            return new ProtectedCuboidRegion(id, transientRegion,
                    BlockVector3.at(Math.min(first.x(), second.x()), selection.minY(), Math.min(first.z(), second.z())),
                    BlockVector3.at(Math.max(first.x(), second.x()), selection.maxY(), Math.max(first.z(), second.z())));
        }

        List<BlockVector2> corners = selection.points().stream().map(point -> BlockVector2.at(point.x(), point.z())).toList();
        return new ProtectedPolygonalRegion(id, transientRegion, corners, selection.minY(), selection.maxY());
    }

    public Set<String> regionIdsAt(Location location) {
        Set<String> ids = new HashSet<>();
        var query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        for (ProtectedRegion region : query.getApplicableRegions(BukkitAdapter.adapt(location))) ids.add(logicalId(region));
        return ids;
    }

    public List<String> sortedRegionIdsAt(Location location) {
        return regionIdsAt(location).stream().sorted().toList();
    }

    public boolean teleport(Player player, String id) {
        List<ProtectedRegion> shapes = pieces(player.getWorld(), id);
        if (shapes.isEmpty()) return false;

        ProtectedRegion primary = shapes.getFirst();

        BlockVector3 min = primary.getMinimumPoint();
        BlockVector3 max = primary.getMaximumPoint();
        double x = (min.x() + max.x() + 1) / 2.0;
        double z = (min.z() + max.z() + 1) / 2.0;
        World world = player.getWorld();
        int y = Math.max(min.y(), Math.min(max.y(), world.getHighestBlockYAt((int) x, (int) z))) + 1;
        player.teleportAsync(new Location(world, x, y, z));
        return true;
    }

    public void deleteStoredData(String worldName) {
        Plugin worldGuard = Bukkit.getPluginManager().getPlugin("WorldGuard");
        if (worldGuard == null) return;

        Path worldFolder = worldGuard.getDataFolder().toPath().resolve("worlds").resolve(worldName);
        if (!Files.exists(worldFolder)) return;

        try (var paths = Files.walk(worldFolder)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException e) {
            RogueSmpCore.LOGGER.error("Failed to delete WorldGuard data for world {}", worldName, e);
        }
    }

    private ProtectedRegion group(World world, ProtectedRegion plain) {
        RegionManager regionManager = getRegionManager(world);
        String id = plain.getId();
        List<ProtectedRegion> children = regionManager.getRegions().values().stream()
                .filter(region -> plain.equals(region.getParent()))
                .toList();

        ProtectedRegion template = new GlobalProtectedRegion(id, plain.isTransient());
        template.copyFrom(plain);
        template.setFlag(SmpRegionFlags.GROUPED, StateFlag.State.ALLOW);

        ProtectedRegion shape = cloneShape(plain, shapeId(id, 1));
        regionManager.removeRegion(id);
        regionManager.addRegion(template);
        attach(template, shape);
        regionManager.addRegion(shape);
        children.forEach(child -> setParent(child, template));
        return template;
    }

    private void attach(ProtectedRegion template, ProtectedRegion shape) {
        shape.setPriority(template.getPriority());
        setParent(shape, template);
    }

    private void setParent(ProtectedRegion child, ProtectedRegion parent) {
        try {
            child.setParent(parent);
        } catch (ProtectedRegion.CircularInheritanceException e) {
            RogueSmpCore.LOGGER.error("Circular region inheritance between {} and {}", child.getId(), parent.getId(), e);
        }
    }

    private ProtectedRegion cloneShape(ProtectedRegion source, String newId) {
        BlockVector3 min = source.getMinimumPoint();
        BlockVector3 max = source.getMaximumPoint();
        ProtectedRegion copy = source instanceof ProtectedPolygonalRegion polygon
                ? new ProtectedPolygonalRegion(newId, source.isTransient(), polygon.getPoints(), min.y(), max.y())
                : new ProtectedCuboidRegion(newId, source.isTransient(), min, max);
        copy.setPriority(source.getPriority());
        return copy;
    }

    private String nextShapeId(World world, String id) {
        RegionManager regionManager = getRegionManager(world);
        int next = pieces(world, id).stream().mapToInt(shape -> shapeIndex(shape.getId())).max().orElse(0) + 1;
        while (regionManager != null && regionManager.hasRegion(shapeId(id, next))) next++;
        return shapeId(id, next);
    }

    private void removeRegion(World world, String worldGuardId) {
        RegionManager regionManager = getRegionManager(world);
        if (regionManager != null) regionManager.removeRegion(worldGuardId);
    }

    private <V> void putFlag(ProtectedRegion region, Flag<V> flag, String value, RegionGroup group) throws InvalidFlagFormat {
        region.setFlag(flag, flag.parseInput(FlagContext.create().setInput(value).build()));
        if (flag.getRegionGroupFlag() == null) return;

        region.setFlag(flag.getRegionGroupFlag(), group == RegionGroup.ALL ? null : group);
    }

    private @Nullable RegionManager getRegionManager(World world) {
        return WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
    }
}
