package com.roguesmp.constant;

import com.roguesmp.item.SmpItem;
import com.roguesmp.player.SmpPlayer;
import org.bukkit.Material;

/**
 * Basic enchants that player will add onto their item
 */
public enum Infusion {
    VIGOR(Enchants.VIGOR, "copper_ingot_currency", 30, 5),
    PERSPICACITY(Enchants.PERSPICACITY, "copper_ingot_currency", 30, 5),
    FORTITUDE(Enchants.FORTITUDE, "copper_ingot_currency", 30, 5),
    CELERITY(Enchants.CELERITY, "copper_ingot_currency", 30, 5),
    ;

    private final Enchants enchant;
    private final String costItemId;
    private final int baseExpCost;
    private final int baseMaterialCost;

    Infusion(Enchants enchant, String costItemId, int baseExpCost, int baseMaterialCost) {
        this.enchant = enchant;
        this.costItemId = costItemId;
        this.baseExpCost = baseExpCost;
        this.baseMaterialCost = baseMaterialCost;
    }

    /**
     * Check if a player can apply the infusion onto this item, currently only check cost
     */
    public static boolean canInfuse(SmpPlayer smpPlayer, SmpItem smpItem, Infusion infusion, int level) {
        return false;
    }

    public static void infuseItem(SmpPlayer smpPlayer, SmpItem smpItem, Infusion infusion, int level) {

    }
}
