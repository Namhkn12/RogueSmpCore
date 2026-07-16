package com.roguesmp.constant;

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

    public Enchants getSmpEnchant() {
        return enchant;
    }

    public String getCostItemId() {
        return costItemId;
    }

    public int getBaseExpLevelCost() {
        return baseExpCost;
    }

    public int getBaseMaterialCost() {
        return baseMaterialCost;
    }
}
