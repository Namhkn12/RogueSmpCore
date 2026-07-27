package com.roguesmp.item.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DurabilityRepairComponent implements ItemComponent {

    public static final Codec<DurabilityRepairComponent> CODEC = Codec.INT.xmap(DurabilityRepairComponent::new, DurabilityRepairComponent::getAmount);

    private final int amount;

    public DurabilityRepairComponent(int amount) {
        this.amount = amount;
    }

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        context.builder().putLines(900, List.of(Utils.text("Có thể sử dụng để sửa chữa vật phẩm", NamedTextColor.GRAY),
                Utils.text("Hồi phục " + amount + " độ bền", NamedTextColor.GRAY)));
    }

    public int getAmount() {
        return amount;
    }
}
