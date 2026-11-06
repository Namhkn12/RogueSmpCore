package com.roguesmp.block.upgrade;

import com.roguesmp.block.upgrade.effect.UpgradeEffect;
import com.roguesmp.codec.Codec;
import com.roguesmp.item.ItemType;
import com.roguesmp.registry.Holder;
import com.roguesmp.registry.Registries;
import com.roguesmp.registry.Registry;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public record UpgradeSynergy(Holder<ItemType> type, UpgradeEffect effect) {

    public static final Codec<UpgradeSynergy> CODEC = Codec.composite(
            Registry.referenceCodec(() -> Registries.ITEM_TYPE).fieldOf("type").forGetter(UpgradeSynergy::type),
            UpgradeEffect.CODEC.forGetter(UpgradeSynergy::effect),
            UpgradeSynergy::new
    );

    public boolean appliesTo(String consumableItemId) {
        return type.isBound() && type.value().matches(consumableItemId);
    }

    public List<Component> describe() {
        List<Component> lines = new ArrayList<>();
        String typeName = type.isBound() ? type.value().displayText() : type.getId();
        lines.add(Utils.text("Với chất xúc tác ", NamedTextColor.GOLD).append(Utils.fromString(typeName)).append(Component.text(":", NamedTextColor.GOLD)));
        for (Component line : effect.describe()) lines.add(Utils.text(" ").append(line));
        return lines;
    }
}
