package com.roguesmp.gate.cost;

import com.roguesmp.codec.Codec;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.Registries;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.Inventory;

public interface Cost {

    Codec<Cost> CODEC = Codec.dispatch(
            Cost::getTypeId,
            Registries.COST_CODEC::getOrThrow
    );

    String getTypeId();

    boolean canAfford(SmpPlayer player);

    default void take(SmpPlayer player) {

    }

    Component getDisplay(SmpPlayer player);

    default void takeFrom(Inventory inventory) {

    }
}
