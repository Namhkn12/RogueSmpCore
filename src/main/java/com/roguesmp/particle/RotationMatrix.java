package com.roguesmp.particle;

import org.apache.commons.math3.util.FastMath;
import org.bukkit.util.Vector;

public class RotationMatrix {

    private final double m00, m01, m02;
    private final double m10, m11, m12;
    private final double m20, m21, m22;

    public RotationMatrix(Vector axis, double angle) {

        axis = axis.clone().normalize();

        double x = axis.getX();
        double y = axis.getY();
        double z = axis.getZ();

        double cos = FastMath.cos(angle);
        double sin = FastMath.sin(angle);
        double t = 1 - cos;

        m00 = t*x*x + cos;
        m01 = t*x*y - sin*z;
        m02 = t*x*z + sin*y;

        m10 = t*x*y + sin*z;
        m11 = t*y*y + cos;
        m12 = t*y*z - sin*x;

        m20 = t*x*z - sin*y;
        m21 = t*y*z + sin*x;
        m22 = t*z*z + cos;
    }

    public Vector apply(Vector v) {

        double x = v.getX();
        double y = v.getY();
        double z = v.getZ();

        return new Vector(m00*x + m01*y + m02*z, m10*x + m11*y + m12*z, m20*x + m21*y + m22*z);
    }
}
