package com.roguesmp.item.component.impl;

import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BrokenComponent implements ItemComponent {

    public BrokenComponent() {
    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        context.builder().putLines(999, List.of(Utils.text("Vật phẩm này đã hỏng, hãy tìm thợ sửa chữa!", NamedTextColor.RED, TextDecoration.BOLD)));
    }

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }
}
