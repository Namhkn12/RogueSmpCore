package com.roguesmp.block.data;

import com.roguesmp.codec.Codec;

public record PaletteMarkerData(String palette) {

    public static final PaletteMarkerData DEFAULT = new PaletteMarkerData("default");

    public static final Codec<PaletteMarkerData> CODEC = Codec.composite(
            Codec.STRING.optionalFieldOf("palette", DEFAULT.palette()).forGetter(PaletteMarkerData::palette),
            PaletteMarkerData::new
    );
}
