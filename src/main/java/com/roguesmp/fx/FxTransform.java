package com.roguesmp.fx;

import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * An immutable position + rotation + scale, used to place shapes in the world and to
 * compose parent/child movement (root effect transform combined with a part's local transform).
 */
public final class FxTransform {

    public static final FxTransform IDENTITY = new FxTransform(new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1));

    private final Vector3f position;
    private final Quaternionf rotation;
    private final Vector3f scale;

    public FxTransform(Vector3f position, Quaternionf rotation, Vector3f scale) {
        this.position = position;
        this.rotation = rotation;
        this.scale = scale;
    }

    public static FxTransform ofPosition(double x, double y, double z) {
        return new FxTransform(new Vector3f((float) x, (float) y, (float) z), new Quaternionf(), new Vector3f(1, 1, 1));
    }

    /** Yaw-then-pitch, both negated — same convention as {@code ParticleUtils.spawnShape}, so a directional shape (e.g. {@code LineShape}) points exactly where {@code loc} is facing, tilt included. */
    public static FxTransform at(Location loc) {
        Quaternionf rotation = new Quaternionf()
                .rotateY((float) FastMath.toRadians(-loc.getYaw()))
                .rotateX((float) FastMath.toRadians(-loc.getPitch()));
        return new FxTransform(new Vector3f((float) loc.getX(), (float) loc.getY(), (float) loc.getZ()), rotation, new Vector3f(1, 1, 1));
    }

    public Vector3f position() {
        return new Vector3f(position);
    }

    public Quaternionf rotation() {
        return new Quaternionf(rotation);
    }

    public Vector3f scale() {
        return new Vector3f(scale);
    }

    /** Transforms a shape-local offset into this transform's space (scale, then rotate, then translate). */
    public Vector3f apply(Vector local) {
        Vector3f v = new Vector3f((float) local.getX(), (float) local.getY(), (float) local.getZ());
        v.mul(scale);
        rotation.transform(v);
        v.add(position);
        return v;
    }

    /** Composes this transform with a child transform expressed relative to it. */
    public FxTransform combine(FxTransform child) {
        Vector3f childPos = new Vector3f(child.position).mul(scale);
        rotation.transform(childPos);
        childPos.add(position);

        Quaternionf newRotation = new Quaternionf(rotation).mul(child.rotation);
        Vector3f newScale = new Vector3f(scale).mul(child.scale);
        return new FxTransform(childPos, newRotation, newScale);
    }

    public Location toLocation(World world) {
        return new Location(world, position.x(), position.y(), position.z());
    }

    public static FxTransform lerp(FxTransform a, FxTransform b, float t) {
        Vector3f position = new Vector3f(a.position).lerp(b.position, t);
        Quaternionf rotation = new Quaternionf(a.rotation).slerp(b.rotation, t);
        Vector3f scale = new Vector3f(a.scale).lerp(b.scale, t);
        return new FxTransform(position, rotation, scale);
    }
}
