package com.roguesmp.item.component.impl;

import com.roguesmp.action.ActionContext;
import com.roguesmp.action.ActionEntry;
import com.roguesmp.codec.Codec;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.InteractableComponent;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.Location;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record InteractActionComponent(List<ActionEntry> actions, String hint) implements InteractableComponent {

    public static final Codec<InteractActionComponent> CODEC = Codec.composite(
            Codec.listOf(ActionEntry.CODEC).optionalFieldOf("actions", List.of()).forGetter(InteractActionComponent::actions),
            Codec.STRING.optionalFieldOf("hint", "").forGetter(InteractActionComponent::hint),
            InteractActionComponent::new
    );

    private static final int LORE_PRIORITY = 950;

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        if (hint.isBlank()) return;
        context.builder().putLines(LORE_PRIORITY, List.of(Utils.fromString(hint)));
    }

    @Override
    public void onItemInteract(SmpPlayer smpPlayer, PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL) return;
        SmpItem item = smpPlayer.getItemAtEquipSlot(EquipSlot.fromVanilla(event.getHand()));
        Location location = event.getClickedBlock() == null ? null : event.getClickedBlock().getLocation();
        ActionContext context = new ActionContext(event.getPlayer(), event.getAction().isRightClick(), item, null, location, event);
        if (ActionEntry.runAll(actions, context)) event.setCancelled(true);
    }

    @Override
    public void onEntityInteract(SmpPlayer smpPlayer, PlayerInteractEntityEvent event) {
        SmpItem item = smpPlayer.getItemAtEquipSlot(EquipSlot.fromVanilla(event.getHand()));
        if (ActionEntry.runAll(actions, ActionContext.ofEntity(event.getPlayer(), true, item, event.getRightClicked(), event))) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onPreAttack(SmpPlayer smpPlayer, PrePlayerAttackEntityEvent event) {
        SmpItem item = smpPlayer.getItemAtEquipSlot(EquipSlot.MAINHAND);
        if (ActionEntry.runAll(actions, ActionContext.ofEntity(event.getPlayer(), false, item, event.getAttacked(), event))) {
            event.setCancelled(true);
        }
    }
}
