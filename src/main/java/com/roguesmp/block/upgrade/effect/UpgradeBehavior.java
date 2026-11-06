package com.roguesmp.block.upgrade.effect;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;
import net.kyori.adventure.text.Component;

import java.util.List;

public interface UpgradeBehavior {

    Codec<UpgradeBehavior> CODEC = Codec.dispatch(UpgradeBehavior::getTypeId, Registries.UPGRADE_BEHAVIOR_CODEC::getOrThrow);

    String getTypeId();

    List<Component> getDisplay();

    default void onOutput(OutputContext context) {

    }
}
