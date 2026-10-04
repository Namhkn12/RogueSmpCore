package com.roguesmp.block;

import com.roguesmp.codec.Codec;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public interface Directional {

    Codec<BlockFace> CODEC = Codec.enumOf(BlockFace.class);

    BlockFace getFaceDirection();

    default BlockFace getPlacementFacing(Player player) {
        Vector direction = player.getLocation().getDirection();

        double x = direction.getX();
        double y = direction.getY();
        double z = direction.getZ();

        double ax = Math.abs(x);
        double ay = Math.abs(y);
        double az = Math.abs(z);

        if (ay > ax && ay > az) {
            return y > 0 ? BlockFace.DOWN : BlockFace.UP;
        }

        if (az > ax) {
            return z > 0 ? BlockFace.NORTH : BlockFace.SOUTH;
        }

        return x > 0 ? BlockFace.WEST : BlockFace.EAST;
    }

    default void rotateDisplay(Entity display, BlockFace blockFace) {
        if (display instanceof ItemDisplay itemDisplay) {
            Vector direction = blockFace.getDirection();

            Quaternionf rotation = new Quaternionf().rotationTo(
                    new Vector3f(0, 0, 1),
                    new Vector3f((float) direction.getX(), (float) direction.getY(), (float) direction.getZ())
            );

            Transformation transformation = itemDisplay.getTransformation();
            transformation.getLeftRotation().set(rotation);
            itemDisplay.setTransformation(transformation);
        }
    }
}
