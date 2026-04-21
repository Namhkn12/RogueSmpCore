package com.roguesmp.dungeon.utils;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Razdon {
    private static final Razdon INSTANCE = new Razdon();
    private final Random random;

    private Razdon() {
        this.random = new Random();
    }

    public static Razdon getInstance() {
        return INSTANCE;
    }

    public Random getRandom() {
        return random;
    }

    public int nextInt(int bound) {
        return random.nextInt(bound);
    }

    public int nextIntInRange(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    public double nextDouble() {
        return random.nextDouble();
    }

    public boolean nextBoolean() {
        return random.nextBoolean();
    }

    public double nextAngle() {
        return random.nextDouble() * Math.PI * 2;
    }

    /** Random điểm trên vòng tròn 2D với radius cố định */
    public double[] nextCirclePoint(double radius) {
        double angle = nextAngle();
        return new double[]{ Math.cos(angle) * radius, Math.sin(angle) * radius };
    }

    /** Random điểm trong vòng tròn 2D với radius [minRadius, maxRadius] */
    public double[] nextRingPoint(double minRadius, double maxRadius) {
        double angle = nextAngle();
        double distance = nextDoubleInRange(minRadius, maxRadius);
        return new double[]{ Math.cos(angle) * distance, Math.sin(angle) * distance };
    }

    public double nextDoubleInRange(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    public float nextFloatInRange(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    /** Random offset XZ quanh một điểm, Y giữ nguyên */
    public Location nextLocationXZ(Location center, double minRadius, double maxRadius) {
        double[] point = nextRingPoint(minRadius, maxRadius);
        return center.clone().add(point[0], 0, point[1]);
    }

    /** Random offset XYZ với Y offset riêng */
    public Location nextLocationXYZ(Location center, double minRadius, double maxRadius,
                                    double minY, double maxY) {
        double[] point = nextRingPoint(minRadius, maxRadius);
        double yOffset = nextDoubleInRange(minY, maxY);
        return center.clone().add(point[0], yOffset, point[1]);
    }

    /** Random 1 phần tử từ list, null nếu rỗng */
    public <T> T nextElement(List<T> list) {
        if (list == null || list.isEmpty()) return null;
        return list.get(random.nextInt(list.size()));
    }

    /** Random 1 phần tử từ array, null nếu rỗng */
    public <T> T nextElement(T[] array) {
        if (array == null || array.length == 0) return null;
        return array[random.nextInt(array.length)];
    }

    /** Shuffle list và trả về bản copy */
    public <T> List<T> shuffled(List<T> list) {
        List<T> copy = new ArrayList<>(list);
        Collections.shuffle(copy, random);
        return copy;
    }

    /** true với xác suất percent% (0-100) */
    public boolean chance(double percent) {
        return random.nextDouble() * 100 < percent;
    }
}
