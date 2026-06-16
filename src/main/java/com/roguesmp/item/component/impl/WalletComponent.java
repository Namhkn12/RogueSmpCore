package com.roguesmp.item.component.impl;

import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.WalletUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * This component is used for lore mainly, for operation related to Wallet, see {@link WalletUtils}
 */
public class WalletComponent implements ItemComponent {

    private final int maxSlot;

    public WalletComponent(int maxSlot) {
        this.maxSlot = maxSlot;
    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        int storedAmount = context.data().getOrDefault(WalletUtils.DATA_ID, PersistentDataType.INTEGER, 0);
        List<Component> lores = new ArrayList<>();
        lores.add(Utils.text("Ví tiền", NamedTextColor.GREEN)
                .append(Utils.text(" (Hiện có: " + Utils.formatMoney(storedAmount) + " đồng)", NamedTextColor.GRAY)));
        lores.add(Utils.text("Dung tích: " + maxSlot + " ô (" + WalletUtils.VALUE_BLOCK * 64 * maxSlot + " đồng)", NamedTextColor.DARK_GRAY));
        context.builder().putLines(1, lores);
    }

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    public int getMaxSlot() {
        return maxSlot;
    }
}
