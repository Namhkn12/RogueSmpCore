package com.roguesmp.item.component.impl;

import com.roguesmp.annotation.GsonIgnore;
import com.roguesmp.constant.Keys;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.WalletUtils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Item with this component will become a wallet which allow storing currency items inside
 */
public class WalletComponent implements ItemComponent {

    private final String currencyType;

    @GsonIgnore
    private int storedAmount;

    public WalletComponent(String currencyType) {
        this.currencyType = currencyType;
    }

    @Override
    public void load(PersistentDataContainerView pdc) {
        Integer amount = pdc.get(WalletUtils.DATA_ID, PersistentDataType.INTEGER);
        if (amount == null) return;
        storedAmount = amount;
    }

    @Override
    public void save(PersistentDataContainer pdc) {
        pdc.set(WalletUtils.DATA_ID, PersistentDataType.INTEGER, storedAmount);
    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        List<Component> lores = new ArrayList<>();
        lores.add(Utils.text("Ví tiền", NamedTextColor.GREEN)
                .append(Utils.text(" (Hiện có: " + storedAmount + " đồng)", NamedTextColor.GRAY)));
        context.builder().putLines(1, lores);
    }

    @Override
    public @NotNull ItemComponent copy() {
        return new WalletComponent(currencyType);
    }

    public String getCurrencyType() {
        return currencyType;
    }
}
