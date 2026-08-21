package com.roguesmp.item.ability.impl;

import com.roguesmp.attribute.Attributes;
import com.roguesmp.codec.Codec;
import com.roguesmp.event.DurabilityChangedEvent;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.ability.ItemAbility;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.component.impl.DurabilityComponent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class UnyieldingEdge implements ItemAbility {

    public static final String TYPE_KEY = "unyielding_edge";

    public static final Codec<UnyieldingEdge> CODEC = Codec.composite(
            Codec.DOUBLE.optionalFieldOf("dmgPerUnitLoss", 5d).forGetter(UnyieldingEdge::getDmgPerUnitLoss),
            Codec.INT.optionalFieldOf("amountLossPerUnit", 200).forGetter(UnyieldingEdge::getAmountLossPerUnit),
            UnyieldingEdge::new
    );

    private final double dmgPerUnitLoss;
    private final int amountLossPerUnit;

    public UnyieldingEdge(double dmgPerUnitLoss, int amountLossPerUnit) {
        this.dmgPerUnitLoss = dmgPerUnitLoss;
        this.amountLossPerUnit = amountLossPerUnit;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public List<Component> getDisplay(SmpPlayer smpPlayer, SmpItem smpItem) {
        return List.of(Utils.fromString("<!i><red>Mỗi " + amountLossPerUnit + " độ bền mất sẽ tăng " + Utils.formatDecimal(dmgPerUnitLoss) + " sát thương cận chiến"));
    }

    @Override
    public String getSimpleDescription() {
        return "Mỗi x độ bền mất đi cộng thêm y melee_damage_base";
    }

    @Override
    public @NotNull Map<Attributes, Double> provideAttribute(SmpPlayer player, SmpItem smpItem) {
        DurabilityComponent durabilityComponent = smpItem.getComponent(ItemComponentKeys.DURABILITY);
        if (durabilityComponent == null) return ItemAbility.super.provideAttribute(player, smpItem);
        int unit = unitsLost(durabilityComponent.maxDurability(), durabilityComponent.currentDurability());
        if (unit <= 0) return ItemAbility.super.provideAttribute(player, smpItem);
        Map<Attributes, Double> map = new EnumMap<>(Attributes.class);
        map.put(Attributes.MELEE_DAMAGE_BASE, unit * dmgPerUnitLoss);
        return map;
    }

    @Override
    public void onDurabilityChange(SmpPlayer player, SmpItem item, DurabilityChangedEvent event) {
        DurabilityComponent durabilityComponent = item.getComponent(ItemComponentKeys.DURABILITY);
        if (durabilityComponent == null) return;

        int maxDurability = durabilityComponent.maxDurability();
        int oldDurability = durabilityComponent.currentDurability();
        int newDurability = Math.max(0, Math.min(maxDurability, oldDurability + event.getChangeAmount()));

        if (unitsLost(maxDurability, oldDurability) != unitsLost(maxDurability, newDurability)) {
            event.setShouldUpdateItem(true);
        }
    }

    private int unitsLost(int maxDurability, int currentDurability) {
        if (amountLossPerUnit <= 0) return 0;
        return (maxDurability - currentDurability) / amountLossPerUnit;
    }

    public double getDmgPerUnitLoss() {
        return dmgPerUnitLoss;
    }

    public int getAmountLossPerUnit() {
        return amountLossPerUnit;
    }
}
