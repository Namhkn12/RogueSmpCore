package com.roguesmp.utils.modification;

import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.impl.EquipAttributeComponent;
import com.roguesmp.item.component.impl.GemDataComponent;
import com.roguesmp.item.component.impl.GemSocketComponent;
import com.roguesmp.registry.ItemRegistry;

import java.util.Collections;
import java.util.List;

public class GemSocketUtil {

    public enum SocketResult {
        SUCCESS,
        ERROR_NOT_SOCKETABLE,
        ERROR_NOT_A_GEM,
        ERROR_SLOT_INCOMPATIBLE,
        ERROR_NO_SLOTS_AVAILABLE,
        ERROR_FAILED_ROLL
    }

    // =========================================================================
    // STANDALONE VERIFYING & READING METHODS
    // =========================================================================

    /**
     * Checks if the item is capable of holding gems.
     */
    public static boolean isSocketable(SmpItem smpItem) {
        return smpItem.getComponent(ItemComponentKeys.GEM_SOCKET) != null;
    }

    /**
     * Checks if the item has at least one open socket left.
     */
    public static boolean hasAvailableSlots(SmpItem smpItem) {
        GemSocketComponent gemSocketComponent = smpItem.getComponent(ItemComponentKeys.GEM_SOCKET);
        return gemSocketComponent != null && gemSocketComponent.canFitGem();
    }

    /**
     * Verifies if the given item ID belongs to a valid registry gem.
     */
    public static boolean isValidGem(String gemId) {
        BaseItem baseItem = ItemRegistry.getInstance().getBaseItem(gemId);
        if (baseItem == null) return false;
        return baseItem.getComponent(ItemComponentKeys.GEM_DATA) != null;
    }

    /**
     * Verifies if the gem's allowed attribute slots match the equipment slot of the item.
     */
    public static boolean isGemCompatible(SmpItem smpItem, String gemId) {
        BaseItem baseItem = ItemRegistry.getInstance().getBaseItem(gemId);
        if (baseItem == null) return false;

        GemDataComponent gemData = baseItem.getComponent(ItemComponentKeys.GEM_DATA);
        if (gemData == null) return false;

        EquipAttributeComponent itemAttr = smpItem.getComponent(ItemComponentKeys.ATTRIBUTE);
        if (itemAttr == null) return false;

        return gemData.getAttributes().containsKey(itemAttr.getSlot());
    }

    /**
     * Performs a comprehensive check on whether a gem can theoretically be socketed.
     * Useful for greyed-out inventory slots or hover text in custom GUIs.
     */
    public static boolean canSocketGem(SmpItem smpItem, String gemId, boolean bypassLimit) {
        if (!isSocketable(smpItem)) return false;
        if (!isValidGem(gemId)) return false;
        if (!isGemCompatible(smpItem, gemId)) return false;
        if (!bypassLimit && !hasAvailableSlots(smpItem)) return false;

        return true;
    }

    /**
     * Obtains the list of currently socketed gem IDs inside the item.
     */
    public static List<String> getSocketedGems(SmpItem smpItem) {
        GemSocketComponent gemSocketComponent = smpItem.getComponent(ItemComponentKeys.GEM_SOCKET);
        if (gemSocketComponent == null) {
            return Collections.emptyList();
        }
        // Assuming your GemSocketComponent has a getGems() list getter
        return gemSocketComponent.getAppliedItem();
    }

    /**
     * Gets the maximum capacity of gems this item can hold.
     */
    public static int getMaxSlots(SmpItem smpItem) {
        GemSocketComponent gemSocketComponent = smpItem.getComponent(ItemComponentKeys.GEM_SOCKET);
        if (gemSocketComponent == null) {
            return 0;
        }
        // Assuming your component tracks max sockets (e.g. 3 or 5 slots)
        return gemSocketComponent.getSocketCount();
    }

    // =========================================================================
    // TRANSACTION MUTATIONS
    // =========================================================================

    /**
     * Attempts to socket a gem. Mutates the item components directly.
     */
    public static SocketResult addGem(SmpItem smpItem, String gemId, boolean checkSuccessChance, boolean bypassLimit) {
        if (!isSocketable(smpItem)) {
            return SocketResult.ERROR_NOT_SOCKETABLE;
        }

        if (!isValidGem(gemId)) {
            return SocketResult.ERROR_NOT_A_GEM;
        }

        if (!isGemCompatible(smpItem, gemId)) {
            return SocketResult.ERROR_SLOT_INCOMPATIBLE;
        }

        if (!bypassLimit && !hasAvailableSlots(smpItem)) {
            return SocketResult.ERROR_NO_SLOTS_AVAILABLE;
        }

        if (checkSuccessChance) {
            BaseItem baseItem = ItemRegistry.getInstance().getBaseItem(gemId);
            // Safe to assume non-null here due to the isValidGem check above
            GemDataComponent gemData = baseItem.getComponent(ItemComponentKeys.GEM_DATA);

            if (Math.random() > gemData.getSuccessChance()) {
                return SocketResult.ERROR_FAILED_ROLL;
            }
        }

        GemSocketComponent gemSocketComponent = smpItem.getComponent(ItemComponentKeys.GEM_SOCKET);
        gemSocketComponent.addGem(gemId);

        return SocketResult.SUCCESS;
    }

    /**
     * Removes a gem. Returns true if successfully removed, false otherwise.
     */
    public static boolean removeGem(SmpItem smpItem, String gemId) {
        GemSocketComponent gemSocketComponent = smpItem.getComponent(ItemComponentKeys.GEM_SOCKET);
        if (gemSocketComponent == null) {
            return false;
        }

        gemSocketComponent.removeGem(gemId);
        return true;
    }
}
