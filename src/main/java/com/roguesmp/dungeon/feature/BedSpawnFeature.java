package com.roguesmp.dungeon.feature;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon.DungeonEvent;
import com.roguesmp.dungeon.DungeonFeature;
import com.roguesmp.dungeon.DungeonInstance;
import com.roguesmp.dungeon.FeatureContext;
import com.roguesmp.dungeon.NoState;
import com.roguesmp.region.ServerRegionManager;
import com.roguesmp.region.SmpRegionFlags;
import com.roguesmp.utils.Utils;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.flags.StateFlag;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public record BedSpawnFeature() implements DungeonFeature<NoState> {

    public static final String TYPE_KEY = "bed_spawn";

    public static final Codec<BedSpawnFeature> CODEC = Codec.unit(BedSpawnFeature::new);

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public NoState createState() {
        return NoState.INSTANCE;
    }

    @Override
    public Codec<NoState> stateCodec() {
        return NoState.CODEC;
    }

    @Override
    public void onStart(FeatureContext<NoState> context) {
        World world = context.world();
        if (world == null) return;
        ServerRegionManager.getInstance().editGlobal(world, region -> region.setFlag(SmpRegionFlags.BED_SPAWN, StateFlag.State.ALLOW));
    }

    @Override
    public void onEvent(FeatureContext<NoState> context, DungeonEvent event) {
        switch (event) {
            case DungeonEvent.BlockInteracted interacted when Tag.BEDS.isTagged(interacted.block().getType()) -> {
                interacted.cancel();
                setSpawn(context, interacted.player(), interacted.block());
            }
            case DungeonEvent.PlayerRespawned respawned -> {
                Location bed = bedSpawn(context, respawned.player());
                if (bed != null) respawned.event().setRespawnLocation(bed);
            }
            default -> {
            }
        }
    }

    private static void setSpawn(FeatureContext<NoState> context, Player player, Block bed) {
        DungeonInstance instance = context.instance();
        if (!allowsBedSpawn(bed.getLocation())) {
            player.sendActionBar(Utils.fromString("<red>Không thể đặt điểm hồi sinh ở đây"));
            return;
        }
        BlockVector3 position = BlockVector3.at(bed.getX(), bed.getY(), bed.getZ());
        instance.setBedSpawn(player.getUniqueId(), position);
        context.save();
        player.sendActionBar(Utils.fromString("<green>Đã đặt điểm hồi sinh"));
    }

    private static @Nullable Location bedSpawn(FeatureContext<NoState> context, Player player) {
        DungeonInstance instance = context.instance();
        BlockVector3 position = instance.bedSpawn(player.getUniqueId());
        if (position == null) return null;

        Block bed = player.getWorld().getBlockAt(position.x(), position.y(), position.z());
        if (!allowsBedSpawn(bed.getLocation())) return null;
        if (!Tag.BEDS.isTagged(bed.getType())) {
            instance.setBedSpawn(player.getUniqueId(), null);
            context.save();
            player.sendMessage(Utils.fromString("<red>Giường của bạn đã bị phá"));
            return null;
        }
        Location spot = standingSpot(bed);
        if (spot == null) player.sendMessage(Utils.fromString("<red>Giường của bạn trong hầm ngục đang bị chặn"));
        return spot;
    }

    private static @Nullable Location standingSpot(Block bed) {
        if (fitsPlayer(bed.getRelative(BlockFace.UP))) return bed.getLocation().add(0.5, 0.5625, 0.5);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy >= -1; dy--) {
                    Block feet = bed.getRelative(dx, dy, dz);
                    if (fitsPlayer(feet) && feet.getRelative(BlockFace.DOWN).isSolid()) return feet.getLocation().add(0.5, 0, 0.5);
                }
            }
        }
        return null;
    }

    private static boolean fitsPlayer(Block feet) {
        return feet.isPassable() && !feet.isLiquid() && feet.getRelative(BlockFace.UP).isPassable();
    }

    private static boolean allowsBedSpawn(Location location) {
        return ServerRegionManager.getInstance().regionsAt(location).testState(null, SmpRegionFlags.BED_SPAWN);
    }
}
