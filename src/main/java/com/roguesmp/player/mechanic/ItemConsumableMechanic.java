package com.roguesmp.player.mechanic;

import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.impl.ConsumableComponent;
import com.roguesmp.player.SmpPlayer;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ItemConsumableMechanic implements PlayerMechanic {
    @Override public int getPriority() { return 510; }

    @Override
    public void onConsume(PlayerItemConsumeEvent event, SmpPlayer player) {
        if (event.isCancelled()) return;

        ItemStack consumed = event.getItem();
        SmpItem smpItem = SmpItem.wrap(consumed, player);
        smpItem.applyModifiers(player);

        ConsumableComponent consumableComponent = smpItem.getComponent(ComponentKeys.CONSUMABLE);
        if (consumableComponent != null) {
            List<SmpEffect> effectList = consumableComponent.getEffects();
            for (SmpEffect smpEffect : effectList) {
                String source = "consumable_" + smpEffect.getEffectID();
                EffectManager.getInstance().addEffect(player.getBukkitPlayer(), source, smpEffect); //Use clone
            }
        }
    }
}
