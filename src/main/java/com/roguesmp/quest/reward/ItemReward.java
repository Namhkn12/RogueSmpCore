package com.roguesmp.quest.reward;

import com.roguesmp.codec.Codec;
import com.roguesmp.item.BaseItem;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.ItemRef;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ItemReward implements QuestReward {

    public static final String TYPE_KEY = "item";
    public static final Codec<ItemReward> CODEC = Codec.composite(
            Codec.unboundedMap(Codec.INT).fieldOf("item").forGetter(ItemReward::getItems),
            ItemReward::new
    );

    private final Map<String, Integer> items;

    public ItemReward(Map<String, Integer> items) {
        this.items = items;
    }

    public @Unmodifiable Map<String, Integer> getItems() {
        return Collections.unmodifiableMap(items);
    }

    @Override
    public List<Component> getDisplay(SmpPlayer smpPlayer) {
        List<Component> displayList = new ArrayList<>();

        items.forEach((itemId, amount) -> displayList.add(Utils.fromString("<yellow>" + amount + "x</yellow> <aqua>")
                .append(ItemRef.displayName(itemId))));

        return displayList;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public void giveReward(SmpPlayer smpPlayer) {
        List<ItemStack> rewards = new ArrayList<>();
        items.forEach((s, integer) -> {
            BaseItem baseItem = ItemRef.baseItem(s);
            ItemStack itemStack = baseItem != null ? baseItem.generateItemStack(smpPlayer, integer) : ItemRef.toItemStack(s, integer);
            if (itemStack != null) rewards.add(itemStack);
        });
        PlayerUtils.giveItem(smpPlayer.getBukkitPlayer(), rewards);
    }
}
