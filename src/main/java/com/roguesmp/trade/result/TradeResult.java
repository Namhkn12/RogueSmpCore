package com.roguesmp.trade.result;

import com.roguesmp.codec.Codec;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.Registries;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface TradeResult {

    Codec<TradeResult> CODEC = Codec.dispatch(
            TradeResult::getTypeId,
            Registries.TRADE_RESULT_CODEC::getOrThrow
    );

    String getTypeId();

    default void give(SmpPlayer player) {

    }

    Component getDisplay(SmpPlayer player);

    default List<ItemStack> createItems(SmpPlayer player) {
        return List.of();
    }

    default @Nullable ItemStack getIcon(SmpPlayer player) {
        return null;
    }
}
