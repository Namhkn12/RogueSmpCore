package com.roguesmp.entity.spell.common;

import com.roguesmp.codec.Codec;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A lingering vanilla {@link AreaEffectCloud} declared in spell JSON.
 */
public record CloudSpec(double radius, int duration, List<PotionEffectSpec> effects, Color color) {

    public static final Codec<CloudSpec> CODEC = Codec.composite(
            Codec.DOUBLE.optionalFieldOf("radius", 3.0).forGetter(CloudSpec::radius),
            Codec.INT.optionalFieldOf("duration", 100).forGetter(CloudSpec::duration),
            PotionEffectSpec.LIST_CODEC.fieldOf("effects").forGetter(CloudSpec::effects),
            Codec.HEX_COLOR.optionalFieldOf("color", Color.fromRGB(0x5A8F29)).forGetter(CloudSpec::color),
            CloudSpec::new
    );

    public AreaEffectCloud spawn(Location location, @Nullable LivingEntity source) {
        return location.getWorld().spawn(location, AreaEffectCloud.class, cloud -> {
            cloud.setRadius((float) radius);
            cloud.setDuration(duration);
            cloud.setWaitTime(0);
            cloud.setReapplicationDelay(20);
            cloud.setColor(color);
            cloud.setSource(source);
            effects.forEach(effect -> cloud.addCustomEffect(effect.toPotionEffect(), true));
        });
    }
}
