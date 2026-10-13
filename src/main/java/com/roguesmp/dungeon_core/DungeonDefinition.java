package com.roguesmp.dungeon_core;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.utils.Utils;
import com.roguesmp.world.PolarTemplateManager;
import com.roguesmp.world.PolarWorldKey;
import net.kyori.adventure.text.Component;

import java.util.Map;

/**
 * A dungeon: the template world each run is cloned from, and the features that make up its behavior, keyed by an id
 * unique inside the dungeon. The registry id is the dungeon id.
 */
public record DungeonDefinition(PolarWorldKey template, String displayName, Map<String, DungeonFeature<?>> features) {

    private static final Codec<PolarWorldKey> TEMPLATE_CODEC = Codec.STRING.comapFlatMap(
            name -> {
                try {
                    return DataResult.success(new PolarWorldKey(PolarTemplateManager.NAMESPACE, name));
                } catch (IllegalArgumentException e) {
                    return DataResult.error(e.getMessage());
                }
            },
            PolarWorldKey::name
    );

    public static final Codec<DungeonDefinition> CODEC = Codec.composite(
            TEMPLATE_CODEC.fieldOf("template").forGetter(DungeonDefinition::template),
            Codec.STRING.fieldOf("display_name").forGetter(DungeonDefinition::displayName),
            Codec.unboundedMap(DungeonFeature.CODEC).optionalFieldOf("features", Map.of()).forGetter(DungeonDefinition::features),
            DungeonDefinition::new
    );

    public Component name() {
        return Utils.fromString(displayName);
    }
}
