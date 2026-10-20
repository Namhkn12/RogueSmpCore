package com.roguesmp.gate;

import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.component.impl.NameComponent;
import com.roguesmp.registry.Holder;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public final class GateDisplay {

    private GateDisplay() {
    }

    public static Component itemName(Holder<BaseItem> item) {
        if (!item.isBound()) return Component.text(item.getId(), NamedTextColor.RED);
        NameComponent name = item.value().getComponent(ItemComponentKeys.ITEM_NAME);
        if (name == null) return Component.text(item.getId(), NamedTextColor.WHITE);
        return Utils.fromString(name.value()).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static Component itemProgress(Holder<BaseItem> item, int amount, int current) {
        return Component.text(amount + "x ", NamedTextColor.YELLOW)
                .append(itemName(item))
                .append(Component.text(" (", NamedTextColor.GRAY))
                .append(Component.text(current, current >= amount ? NamedTextColor.GREEN : NamedTextColor.RED))
                .append(Component.text("/" + amount + ")", NamedTextColor.GRAY));
    }

    public static Component money(long amount, boolean met) {
        return Utils.fromString((met ? "<gold>" : "<red>") + Utils.formatMoney(amount) + " xu");
    }

    public static Component check(Component line, boolean met) {
        return Component.text(met ? "✔ " : "✘ ", met ? NamedTextColor.GREEN : NamedTextColor.RED).append(line);
    }
}
