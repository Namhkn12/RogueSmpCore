package com.roguesmp.fishing;

import com.roguesmp.codec.Codec;
import com.roguesmp.utils.WorldPos;

import java.util.List;

public record FishingHotspotSpot(WorldPos position, List<String> hotspots) {

    public static final Codec<FishingHotspotSpot> CODEC = Codec.composite(
            Codec.WORLD_POS.fieldOf("position").forGetter(FishingHotspotSpot::position),
            Codec.listOf(Codec.STRING).optionalFieldOf("hotspots", List.of()).forGetter(FishingHotspotSpot::hotspots),
            FishingHotspotSpot::new
    );

    public boolean allows(String hotspotId) {
        return hotspots.isEmpty() || hotspots.contains(hotspotId);
    }
}
