package com.roguesmp.item.component.impl;

import com.roguesmp.annotation.GsonIgnore;
import com.roguesmp.context.ItemDataContext;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

public final class NameComponent implements ItemComponent {
    private final String value;

    @GsonIgnore
    private String prefix;

    public NameComponent(String value) {
        this.value = value;
        this.prefix = "";
    }

    @Override
    public @NotNull ItemComponent copy() {
        return new NameComponent(value);
    }

    @Override
    public void modifyStack(ItemDataContext context) {
        context.newStack().setData(DataComponentTypes.CUSTOM_NAME, Utils.fromString(prefix + value).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
    }

    public String value() {
        return value;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
