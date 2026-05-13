package com.roguesmp.attribute.impl;

import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.constant.Attributes;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

//This doesn't do anything by itself, it is handled in throw rate base
public class ThrowRatePercent implements SmpAttribute {
    @Override
    public @NotNull String getId() {
        return "throw_rate_percent";
    }

    @Override
    public @NotNull Attributes getEnumConstant() {
        return Attributes.THROW_RATE_PERCENT;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Tốc ném";
    }

    @Override
    public @Nullable List<Component> getDisplayText(double value, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultPercentLoreProvider(value);
    }
}
