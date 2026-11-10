package com.roguesmp.utils.modification;

import com.roguesmp.enchant.infusion.InfusionDefinition;
import com.roguesmp.enchant.infusion.InfusionLevel;
import com.roguesmp.enchant.infusion.InfusionType;
import com.roguesmp.gate.cost.CostPayment;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.component.impl.EnchantComponent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.SmpItemUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class InfusionUtil {

    public static int getLevel(SmpItem smpItem, InfusionDefinition infusion) {
        EnchantComponent enchantComponent = smpItem.getComponent(ItemComponentKeys.ENCHANT);
        if (enchantComponent == null) return 0;
        return enchantComponent.getPersistentLevel(infusion.enchant());
    }

    public static @Nullable InfusionDefinition getActiveInfusion(SmpItem smpItem, InfusionType type) {
        EnchantComponent enchantComponent = smpItem.getComponent(ItemComponentKeys.ENCHANT);
        if (enchantComponent == null) return null;

        for (InfusionDefinition infusion : InfusionDefinition.sorted(type)) {
            if (enchantComponent.getPersistentLevel(infusion.enchant()) > 0) return infusion;
        }
        return null;
    }

    public static boolean isEnchantable(SmpItem smpItem) {
        return smpItem.getComponent(ItemComponentKeys.ENCHANT) != null;
    }

    public static @Nullable InfusionLevel getNextLevel(SmpItem smpItem, InfusionDefinition infusion) {
        return infusion.level(getLevel(smpItem, infusion) + 1);
    }

    public static @Nullable InfusionLevel getCurrentLevel(SmpItem smpItem, InfusionDefinition infusion) {
        return infusion.level(Math.min(getLevel(smpItem, infusion), infusion.maxLevel()));
    }

    public static Result upgrade(SmpPlayer player, SmpItem smpItem, InfusionDefinition infusion) {
        if (!isEnchantable(smpItem)) return Result.NOT_ENCHANTABLE;

        InfusionDefinition active = getActiveInfusion(smpItem, infusion.type());
        if (active != null && active != infusion) return Result.OTHER_INFUSION_ACTIVE;

        InfusionLevel next = getNextLevel(smpItem, infusion);
        if (next == null) return Result.MAX_LEVEL;

        if (!CostPayment.pay(player, next.costs())) return Result.CANNOT_AFFORD;
        if (ThreadLocalRandom.current().nextDouble() >= next.successChance()) return Result.FAILED_ROLL;

        SmpItemUtils.addEnchant(smpItem, Map.of(infusion.enchant(), 1));
        return Result.SUCCESS;
    }

    public static Result downgrade(SmpPlayer player, SmpItem smpItem, InfusionDefinition infusion) {
        InfusionLevel current = getCurrentLevel(smpItem, infusion);
        if (current == null) return Result.NO_ACTIVE_INFUSION;

        SmpItemUtils.addEnchant(smpItem, Map.of(infusion.enchant(), -1));
        CostPayment.refund(player, current.costs());
        return Result.SUCCESS;
    }

    public enum Result {
        SUCCESS,
        FAILED_ROLL,
        NOT_ENCHANTABLE,
        OTHER_INFUSION_ACTIVE,
        CANNOT_AFFORD,
        MAX_LEVEL,
        NO_ACTIVE_INFUSION
    }
}
