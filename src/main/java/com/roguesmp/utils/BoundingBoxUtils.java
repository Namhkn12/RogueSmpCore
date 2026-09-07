package com.roguesmp.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BoundingBoxUtils {

    public static List<Block> getBlocksInBoundingBox(World world, BoundingBox box) {
        List<Block> blocks = new ArrayList<>();

        int minX = box.getMin().getBlockX();
        int minY = box.getMin().getBlockY();
        int minZ = box.getMin().getBlockZ();

        int maxX = box.getMax().getBlockX();
        int maxY = box.getMax().getBlockY();
        int maxZ = box.getMax().getBlockZ();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.isEmpty()) continue;
                    blocks.add(block);
                }
            }
        }

        return blocks;
    }

    /**
     * Retrieves a list of blocks that have an exact collision shape intersecting with the given bounding box. Note that a block without collision will not be in the list.
     *
     * @param world the world containing the blocks
     * @param box the bounding box to check for intersections
     * @return a list of blocks whose collision shapes overlap with the specified bounding box
     */
    public static List<Block> getExactCollidingBlocks(World world, BoundingBox box) {
        List<Block> blocks = new ArrayList<>();

        int minX = box.getMin().getBlockX();
        int minY = box.getMin().getBlockY();
        int minZ = box.getMin().getBlockZ();

        int maxX = box.getMax().getBlockX();
        int maxY = box.getMax().getBlockY();
        int maxZ = box.getMax().getBlockZ();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);

                    if (block.isEmpty()) continue;

                    for (BoundingBox collisionBox : block.getCollisionShape().getBoundingBoxes()) {
                        BoundingBox worldCollisionBox = collisionBox.shift(x, y, z);
                        if (worldCollisionBox.overlaps(box)) {
                            blocks.add(block);
                            break;
                        }
                    }
                }
            }
        }

        return blocks;
    }

    /**
     * Checks if any block with a collision shape overlaps with the given bounding box.
     * Exits immediately upon finding the first collision.
     *
     * @param world the world containing the blocks
     * @param box the bounding box to check for intersections
     * @return true if there is at least one colliding block, false otherwise
     */
    public static boolean hasCollisionExact(World world, BoundingBox box) {
        int minX = box.getMin().getBlockX();
        int minY = box.getMin().getBlockY();
        int minZ = box.getMin().getBlockZ();

        int maxX = box.getMax().getBlockX();
        int maxY = box.getMax().getBlockY();
        int maxZ = box.getMax().getBlockZ();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);

                    if (block.isEmpty()) continue;

                    for (BoundingBox collisionBox : block.getCollisionShape().getBoundingBoxes()) {
                        BoundingBox worldCollisionBox = collisionBox.shift(x, y, z);
                        if (worldCollisionBox.overlaps(box)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Incrementally travels a bounding box along a vector direction until an obstruction is hit,
     * chunk boundary is reached, or max distance is exceeded.
     *
     * @param world the world in which the movement occurs
     * @param movingBoundingBox the target bounding box that will be mutated to the final location
     * @param maxDistance maximum distance the box can travel
     * @param vector direction of travel
     * @param increment step size per iteration
     * @param wiggleY whether to scan Y-offsets up/down half the box height to bypass small obstacles
     * @param travelAction optional action executed at step intervals along the valid trajectory
     * @return true if blocked by terrain or unloaded chunk; false if traveled max distance without obstruction
     */
    public static boolean travelTillObstructed(
            World world,
            BoundingBox movingBoundingBox,
            double maxDistance,
            Vector vector,
            double increment,
            boolean wiggleY,
            @Nullable Consumer<Location> travelAction
    ) {
        Vector start = movingBoundingBox.getCenter();
        Vector vectorIncrement = vector.clone().normalize().multiply(increment);

        BoundingBox testBox = movingBoundingBox.clone();
        double maxIterations = (maxDistance / increment) * 1.1;

        for (int i = 0; i < maxIterations; i++) {
            testBox.shift(vectorIncrement);
            Vector testBoxCentre = testBox.getCenter();

            if (!testBox.getMin().toLocation(world).isChunkLoaded() || !testBox.getMax().toLocation(world).isChunkLoaded()) {
                return true;
            }

            if (start.distanceSquared(testBoxCentre) > maxDistance * maxDistance) {
                return false;
            }

            if (hasCollisionExact(world, testBox)) {
                if (wiggleY) {
                    boolean blocked = true;
                    BoundingBox wiggleBox = testBox.clone();
                    double step = 0.1;
                    int steps = (int) (wiggleBox.getHeight() / step);
                    wiggleBox.shift(0, -wiggleBox.getHeight() / 2.0 + step / 2.0, 0);

                    for (int dy = 0; dy < steps; dy++) {
                        if (!hasCollisionExact(world, wiggleBox)) {
                            blocked = false;
                            break;
                        }
                        wiggleBox.shift(0, step, 0);
                    }

                    if (blocked) {
                        return true;
                    }
                    movingBoundingBox.copy(wiggleBox);
                } else {
                    return true;
                }
            } else {
                movingBoundingBox.copy(testBox);
            }

            if (travelAction != null) {
                travelAction.accept(movingBoundingBox.getCenter().toLocation(world));
            }
        }

        return false;
    }
}
