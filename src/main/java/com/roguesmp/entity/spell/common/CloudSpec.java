package com.roguesmp.entity.spell.common;

import com.roguesmp.codec.Codec;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A lingering vanilla {@link AreaEffectCloud} declared in spell JSON.
 */
public record CloudSpec(double radius, int duration, List<PotionEffectSpec> effects, Color color) {

    private static final List<PotionEffectSpec> DEFAULT_EFFECTS = List.of(new PotionEffectSpec(PotionEffectType.POISON, 100, 0));
    private static final Color DEFAULT_COLOR = Color.fromRGB(0x5A8F29);

    public static final Codec<CloudSpec> CODEC = Codec.composite(
            Codec.DOUBLE.optionalFieldOf("radius", 3.0).forGetter(CloudSpec::radius),
            Codec.INT.optionalFieldOf("duration", 100).forGetter(CloudSpec::duration),
            PotionEffectSpec.LIST_CODEC.optionalFieldOf("effects", DEFAULT_EFFECTS).forGetter(CloudSpec::effects),
            Codec.HEX_COLOR.optionalFieldOf("color", DEFAULT_COLOR).forGetter(CloudSpec::color),
            CloudSpec::new
    );

    public static final CloudSpec DEFAULT = new CloudSpec(3.0, 100, DEFAULT_EFFECTS, DEFAULT_COLOR);

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
