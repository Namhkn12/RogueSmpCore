package com.roguesmp.utils.modification;

import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.enchant.Infusion;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.impl.EnchantComponent;
import com.roguesmp.registry.ItemRegistry;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.SmpItemUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class InfusionUtil {

    public static int getLevel(SmpItem smpItem, Infusion infusion) {
        EnchantComponent enchantComponent = smpItem.getComponent(ItemComponentKeys.ENCHANT);
        if (enchantComponent == null) return 0;
        return enchantComponent.getTotalLevel(infusion.getSmpEnchant());
    }

    public static @Nullable Infusion getActiveInfusion(SmpItem smpItem) {
        EnchantComponent enchantComponent = smpItem.getComponent(ItemComponentKeys.ENCHANT);
        if (enchantComponent == null) return null;

        for (Infusion inf : Infusion.values()) {
            if (enchantComponent.getTotalLevel(inf.getSmpEnchant()) > 0) {
                return inf;
            }
        }
        return null;
    }

    public static boolean hasAnyInfusion(SmpItem smpItem) {
        return getActiveInfusion(smpItem) != null;
    }

    public static boolean isEnchantable(SmpItem smpItem) {
        return smpItem.getComponent(ItemComponentKeys.ENCHANT) != null;
    }

    private static int getExpCostForLevel(Infusion infusion, int level) {
        if (level <= 0) return 0;
        int multiplier = (int) Math.pow(2, level - 1);
        return PlayerUtils.getExpFromLevel(infusion.getBaseExpLevelCost() * multiplier);
    }

    private static int getMaterialCostForLevel(Infusion infusion, int level) {
        if (level <= 0) return 0;
        int multiplier = (int) Math.pow(2, level - 1);
        return infusion.getBaseMaterialCost() * multiplier;
    }

    public static int getUpgradeExpCost(SmpItem smpItem, Infusion infusion) {
        return getExpCostForLevel(infusion, getLevel(smpItem, infusion) + 1);
    }

    public static int getUpgradeMaterialCost(SmpItem smpItem, Infusion infusion) {
        return getMaterialCostForLevel(infusion, getLevel(smpItem, infusion) + 1);
    }

    public static int getDowngradeExpRefund(SmpItem smpItem, Infusion infusion) {
        return getExpCostForLevel(infusion, getLevel(smpItem, infusion) - 1) / 2;
    }

    public static int getDowngradeMaterialRefund(SmpItem smpItem, Infusion infusion) {
        return getMaterialCostForLevel(infusion, getLevel(smpItem, infusion) - 1);
    }

    // =========================================================================
    // VALIDATION CHECKERS
    // =========================================================================

    public static boolean canAffordUpgrade(Player player, SmpItem smpItem, Infusion infusion) {
        boolean hasXp = PlayerUtils.getExp(player) >= getUpgradeExpCost(smpItem, infusion);
        boolean hasMaterials = PlayerUtils.countItemsInInventory(player, infusion.getCostItemId()) >= getUpgradeMaterialCost(smpItem, infusion);
        return hasXp && hasMaterials;
    }

    // =========================================================================
    // TRANSACTION MUTATIONS (UPGRADE / DOWNGRADE)
    // =========================================================================

    public static OperationStatus executeUpgrade(Player player, SmpItem smpItem, Infusion infusion, int maxLevelCap) {
        if (!isEnchantable(smpItem)) return OperationStatus.ERROR_NOT_ENCHANTABLE;

        int currentLevel = getLevel(smpItem, infusion);
        if (currentLevel >= maxLevelCap) return OperationStatus.ERROR_MAX_LEVEL;

        Infusion active = getActiveInfusion(smpItem);
        if (active != null && active != infusion) return OperationStatus.ERROR_OTHER_INFUSION_ACTIVE;

        int expCost = getUpgradeExpCost(smpItem, infusion);
        int materialCost = getUpgradeMaterialCost(smpItem, infusion);

        if (PlayerUtils.getExp(player) < expCost) return OperationStatus.ERROR_INSUFFICIENT_EXP;
        if (PlayerUtils.countItemsInInventory(player, infusion.getCostItemId()) < materialCost) {
            return OperationStatus.ERROR_INSUFFICIENT_MATERIALS;
        }

        // Charge
        PlayerUtils.changeExp(player, -expCost);
        PlayerUtils.removeItem(player, infusion.getCostItemId(), materialCost);

        // Mutate
        Map<Enchants, Integer> modification = Map.of(infusion.getSmpEnchant(), 1);
        SmpItemUtils.addEnchant(smpItem, modification);

        return OperationStatus.SUCCESS;
    }

    public static OperationStatus executeDowngrade(Player player, SmpItem smpItem, Infusion infusion) {
        int currentLevel = getLevel(smpItem, infusion);
        if (currentLevel <= 0) return OperationStatus.ERROR_NO_ACTIVE_INFUSION;

        int expRefund = getDowngradeExpRefund(smpItem, infusion);
        int materialRefund = getDowngradeMaterialRefund(smpItem, infusion);

        // Refund
        PlayerUtils.changeExp(player, expRefund);
        BaseItem currencyItem = ItemRegistry.getInstance().getBaseItem(infusion.getCostItemId());
        if (currencyItem != null && materialRefund > 0) {
            PlayerUtils.giveItem(player, currencyItem.generateItemStack(materialRefund));
        }

        // Mutate (reduce level by 1)
        Map<Enchants, Integer> modification = Map.of(infusion.getSmpEnchant(), -1);
        SmpItemUtils.addEnchant(smpItem, modification);

        return OperationStatus.SUCCESS;
    }

    public enum OperationStatus {
        SUCCESS,
        ERROR_NOT_ENCHANTABLE,
        ERROR_OTHER_INFUSION_ACTIVE,
        ERROR_INSUFFICIENT_EXP,
        ERROR_INSUFFICIENT_MATERIALS,
        ERROR_MAX_LEVEL,
        ERROR_NO_ACTIVE_INFUSION
    }
}
