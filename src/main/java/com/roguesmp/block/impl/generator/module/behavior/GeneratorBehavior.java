package com.roguesmp.block.impl.generator.module.behavior;

import com.roguesmp.block.StoredItem;
import com.roguesmp.block.impl.generator.ResourceGeneratorBlock;
import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;
import net.kyori.adventure.text.Component;

import java.util.List;

public interface GeneratorBehavior {

    Codec<GeneratorBehavior> CODEC = Codec.dispatch(GeneratorBehavior::getTypeId, Registries.GENERATOR_BEHAVIOR_CODEC::getOrThrow);

    String getTypeId();

    List<Component> getDisplay();

    default void onHarvest(ResourceGeneratorBlock generator, List<StoredItem> drops) {

    }
}
