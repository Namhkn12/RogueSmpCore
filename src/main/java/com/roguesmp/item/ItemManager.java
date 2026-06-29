package com.roguesmp.item;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.Keys;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.SmpItemUtils;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * This class is for caching SmpItem instance. Unique SmpItem is identified by their UUID.
 */
public class ItemManager {

    private static final ItemManager INSTANCE = new ItemManager();
    private final Cache<@NotNull UUID, SmpItem> trackedItems = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.SECONDS)
            .expireAfterWrite(40, TimeUnit.SECONDS) // Clock resets on every access
            .build();
    /**
     * Get the cached SmpItem corresponding to this itemStack, or return a transient SmpItem. Will also automatically cache the item if it is unique
     */
    public SmpItem wrapItem(@NotNull ItemStack itemStack, @Nullable SmpPlayer smpPlayer, @Nullable Consumer<SmpItem> onCacheMiss) {
        BaseItem baseItem = SmpItemUtils.getBaseItem(itemStack);
        if (baseItem == null) return new SmpItem(itemStack);

        UUID uuid = ItemStackUtils.getUUID(itemStack);

        if (baseItem.isUnique() && uuid == null) {
            UUID generatedId = UUID.randomUUID();
            itemStack.editPersistentDataContainer(pdc -> {
                pdc.set(Keys.ITEM_UUID, PersistentDataType.STRING, generatedId.toString());
            });
            uuid = generatedId;
        }

        if (uuid == null) {
            SmpItem smpItem = new SmpItem(itemStack);
            smpItem.applyModifiers(smpPlayer);
            if (onCacheMiss != null) {
                onCacheMiss.accept(smpItem);
            }
            return smpItem;
        }

        boolean[] missed = {false};
        SmpItem smpItem = trackedItems.get(uuid, id -> {
            missed[0] = true;
            SmpItem newItem = new SmpItem(itemStack);
            newItem.applyModifiers(smpPlayer);
            return newItem;
        });

        if (missed[0] && onCacheMiss != null) {
            onCacheMiss.accept(smpItem);
        }

        return smpItem;
    }

    public SmpItem wrapItem(ItemStack itemStack, @Nullable SmpPlayer smpPlayer) {
        return wrapItem(itemStack, smpPlayer, null);
    }

    public void removeTracking(UUID uuid) {
        trackedItems.invalidate(uuid);
    }

    public SmpItem wrapItem(ItemStack itemStack) {
        return wrapItem(itemStack, null);
    }

    public static ItemManager getInstance() {
        return INSTANCE;
    }
}
