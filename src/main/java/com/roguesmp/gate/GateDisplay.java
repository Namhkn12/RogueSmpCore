package com.roguesmp.gate;

import com.roguesmp.utils.ItemRef;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public final class GateDisplay {

    private GateDisplay() {
    }

    public static Component itemName(String itemId) {
        boolean exists = ItemRef.isVanilla(itemId) ? ItemRef.vanillaMaterial(itemId) != null : ItemRef.baseItem(itemId) != null;
        if (!exists) return Component.text(itemId, NamedTextColor.RED);
        return ItemRef.displayName(itemId).colorIfAbsent(NamedTextColor.WHITE).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static Component itemProgress(String item, int amount, int current) {
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
