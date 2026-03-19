package com.roguesmp.utils;

import org.apache.commons.math3.util.FastMath;
import org.bukkit.util.Vector;

public class VectorUtils {

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

    public static Vector rotateAroundAxis(Vector vector, Vector axis, double angle) {

        // Normalize axis
        axis = axis.clone().normalize();

        double x = vector.getX();
        double y = vector.getY();
        double z = vector.getZ();

        double u = axis.getX();
        double v = axis.getY();
        double w = axis.getZ();

        double cos = FastMath.cos(angle);
        double sin = FastMath.sin(angle);

        double dot = u * x + v * y + w * z;

        double newX = u * dot * (1 - cos) + x * cos + (-w * y + v * z) * sin;

        double newY = v * dot * (1 - cos) + y * cos + (w * x - u * z) * sin;

        double newZ = w * dot * (1 - cos) + z * cos + (-v * x + u * y) * sin;

        return new Vector(newX, newY, newZ);
    }
}
