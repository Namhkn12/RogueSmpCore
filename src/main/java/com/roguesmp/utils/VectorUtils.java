package com.roguesmp.utils;

import org.apache.commons.math3.util.FastMath;
import org.bukkit.util.Vector;

public class VectorUtils {

    /**
     * Rotates a vector using Euler angles (Yaw, Pitch, and Roll) in a specific sequence.
     * <p>
     * The rotation follows a fixed-axis sequence:
     * <ol>
     * <li><b>Yaw (Y-axis):</b> Rotates the vector in the horizontal plane.</li>
     * <li><b>Pitch (X-axis):</b> Rotates the vector in the vertical plane.</li>
     * <li><b>Roll (Z-axis):</b> Rotates the vector around its forward-facing axis.</li>
     * </ol>
     *
     * @param v     The original {@link Vector} to be rotated.
     * @param yaw   The rotation angle around the Y-axis in degrees.
     * @param pitch The rotation angle around the X-axis in degrees.
     * @param roll  The rotation angle around the Z-axis in degrees.
     * @return A new {@link Vector} representing the coordinates after all three rotations.
     */
    public static Vector rotate(Vector v, double yaw, double pitch, double roll) {

        double yawRad = FastMath.toRadians(yaw);
        double pitchRad = FastMath.toRadians(pitch);
        double rollRad = FastMath.toRadians(roll);

        double cosYaw = FastMath.cos(yawRad);
        double sinYaw = FastMath.sin(yawRad);

        double cosPitch = FastMath.cos(pitchRad);
        double sinPitch = FastMath.sin(pitchRad);

        double cosRoll = FastMath.cos(rollRad);
        double sinRoll = FastMath.sin(rollRad);

        double x = v.getX();
        double y = v.getY();
        double z = v.getZ();

        // Y rotation (yaw)
        double x1 = x * cosYaw - z * sinYaw;
        double z1 = x * sinYaw + z * cosYaw;

        // X rotation (pitch)
        double y1 = y * cosPitch - z1 * sinPitch;
        double z2 = y * sinPitch + z1 * cosPitch;

        // Z rotation (roll)
        double x2 = x1 * cosRoll - y1 * sinRoll;
        double y2 = x1 * sinRoll + y1 * cosRoll;

        return new Vector(x2, y2, z2);
    }

    /**
     * Rotates a vector around a custom arbitrary axis using Rodrigues' Rotation Formula.
     * <p>
     * This method allows for precise rotation around any direction vector in 3D space,
     * rather than being limited to the world X, Y, or Z axes.
     *
     * @param vector The original {@link Vector} to be rotated.
     * @param axis   The direction {@link Vector} defining the axis of rotation.
     * This vector will be normalized internally.
     * @param degree  The amount of rotation in <b>radians</b>.
     * @return A new {@link Vector} representing the rotated position.
     * @see <a href="https://en.wikipedia.org/wiki/Rodrigues%27_rotation_formula">Rodrigues' Rotation Formula</a>
     */
    public static Vector rotateAroundAxis(Vector vector, Vector axis, double degree) {

        // Normalize axis
        axis = axis.clone().normalize();

        double x = vector.getX();
        double y = vector.getY();
        double z = vector.getZ();

        double u = axis.getX();
        double v = axis.getY();
        double w = axis.getZ();

        double cos = FastMath.cos(Math.toRadians(degree));
        double sin = FastMath.sin(Math.toRadians(degree));

        double dot = u * x + v * y + w * z;

        double newX = u * dot * (1 - cos) + x * cos + (-w * y + v * z) * sin;

        double newY = v * dot * (1 - cos) + y * cos + (w * x - u * z) * sin;

        double newZ = w * dot * (1 - cos) + z * cos + (-v * x + u * y) * sin;

        return new Vector(newX, newY, newZ);
    }

    public static Vector rotateYAxis(Vector vector, double degree) {
        // Standard rotation uses radians
        double sin = FastMath.sin(Math.toRadians(degree));
        double cos = FastMath.cos(Math.toRadians(degree));

        double x = vector.getX() * cos + vector.getZ() * sin;
        double z = vector.getZ() * cos - vector.getX() * sin;

        return vector.clone().setX(x).setZ(z);
    }
}
