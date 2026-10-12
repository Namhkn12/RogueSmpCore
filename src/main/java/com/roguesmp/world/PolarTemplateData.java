package com.roguesmp.world;

import com.roguesmp.codec.Codec;
import org.bukkit.Location;

public record PolarTemplateData(
        double spawnX,
        double spawnY,
        double spawnZ,
        float spawnYaw,
        float spawnPitch,
        double borderDiameter
) {

    public static final PolarTemplateData DEFAULT = new PolarTemplateData(0.5, 64, 0.5, 0, 0, 0);

    public static final Codec<PolarTemplateData> CODEC = Codec.composite(
            Codec.DOUBLE.optionalFieldOf("spawn_x", DEFAULT.spawnX).forGetter(PolarTemplateData::spawnX),
            Codec.DOUBLE.optionalFieldOf("spawn_y", DEFAULT.spawnY).forGetter(PolarTemplateData::spawnY),
            Codec.DOUBLE.optionalFieldOf("spawn_z", DEFAULT.spawnZ).forGetter(PolarTemplateData::spawnZ),
            Codec.FLOAT.optionalFieldOf("spawn_yaw", DEFAULT.spawnYaw).forGetter(PolarTemplateData::spawnYaw),
            Codec.FLOAT.optionalFieldOf("spawn_pitch", DEFAULT.spawnPitch).forGetter(PolarTemplateData::spawnPitch),
            Codec.DOUBLE.optionalFieldOf("border_diameter", DEFAULT.borderDiameter).forGetter(PolarTemplateData::borderDiameter),
            PolarTemplateData::new
    );

    public Location spawn() {
        return new Location(null, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);
    }

    public PolarTemplateData withSpawn(Location spawn) {
        return new PolarTemplateData(spawn.getX(), spawn.getY(), spawn.getZ(), spawn.getYaw(), spawn.getPitch(), borderDiameter);
    }

    public PolarTemplateData withBorderDiameter(double borderDiameter) {
        return new PolarTemplateData(spawnX, spawnY, spawnZ, spawnYaw, spawnPitch, borderDiameter);
    }
}
