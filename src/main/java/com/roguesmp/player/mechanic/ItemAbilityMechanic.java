package com.roguesmp.player.mechanic;

import com.roguesmp.constant.EquipSlot;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.event.DurabilityChangedEvent;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.ability.ItemAbility;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.component.impl.PassiveAbilityComponent;
import com.roguesmp.player.SmpPlayer;

import java.util.function.BiConsumer;

/**
 * Fans damage-dealt/damage-taken events and the regular player tick out to every equipped item's
 * {@link PassiveAbilityComponent} abilities - same instanceof-free component-lookup dispatch shape
 * {@code ItemComponentInteractionMechanic} uses for {@code InteractableComponent}, except this
 * checks every {@link EquipSlot} (not just the interacting hand), since abilities can live on
 * weapons (on-hit) or armor (on-hurt/tick) alike.
 */
public class ItemAbilityMechanic implements PlayerMechanic {

    @Override
    public int getPriority() {
        return 108;
    }

    @Override
    public void onDamageEntity(DamageEvent event, SmpPlayer player) {
        forEachAbility(player, (item, ability) -> ability.onDamageEntity(player, item, event));
    }

    @Override
    public void onHurt(DamageEvent event, SmpPlayer player) {
        forEachAbility(player, (item, ability) -> ability.onHurt(player, item, event));
    }

    @Override
    public void tick(int periodIncrement, SmpPlayer player) {
        forEachAbility(player, (item, ability) -> ability.onTick(player, item, periodIncrement));
    }

    @Override
    public void onDurabilityChange(DurabilityChangedEvent event, SmpPlayer player) {
        SmpItem item = event.getItem();
        PassiveAbilityComponent component = item.getComponent(ItemComponentKeys.PASSIVE_ABILITY);
        if (component == null) return;

        for (ItemAbility ability : component.getAbilities()) {
            ability.onDurabilityChange(player, item, event);
        }
    }

    private void forEachAbility(SmpPlayer player, BiConsumer<SmpItem, ItemAbility> consumer) {
        for (EquipSlot slot : EquipSlot.values()) {
            SmpItem item = player.getItemAtEquipSlot(slot);
            if (item == null) continue;

            PassiveAbilityComponent component = item.getComponent(ItemComponentKeys.PASSIVE_ABILITY);
            if (component == null) continue;

            for (ItemAbility ability : component.getAbilities()) {
                consumer.accept(item, ability);
            }
        }
    }
}
