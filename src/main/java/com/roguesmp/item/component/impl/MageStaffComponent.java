package com.roguesmp.item.component.impl;

import com.roguesmp.attribute.Attributes;
import com.roguesmp.codec.Codec;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.player.mechanic.DurabilityLossMechanic;
import com.roguesmp.item.beam.MageBeam;
import com.roguesmp.item.component.InteractableComponent;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.Color;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

public record MageStaffComponent(Color color) implements InteractableComponent {

    private static final double DEFAULT_RANGE = 20;

    public static final Codec<MageStaffComponent> CODEC = Codec.composite(
            Codec.HEX_COLOR.optionalFieldOf("color", Color.fromRGB(0xB04BFF)).forGetter(MageStaffComponent::color),
            MageStaffComponent::new
    ).codec();

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    @Override
    public void onItemInteract(SmpPlayer smpPlayer, PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        fire(smpPlayer, event.getPlayer(), true);
    }

    @Override
    public void onPreAttack(SmpPlayer smpPlayer, PrePlayerAttackEntityEvent event) {
        fire(smpPlayer, event.getPlayer(), false);
    }

    private void fire(SmpPlayer smpPlayer, Player player, boolean resetCooldown) {
        SmpItem staff = smpPlayer.getItemAtEquipSlot(EquipSlot.MAINHAND);
        if (staff == null || staff.hasComponent(ItemComponentKeys.BROKEN)) return;
        if (player.getAttackCooldown() < 0.95f) return;
        if (resetCooldown) player.resetCooldown();
        DurabilityLossMechanic.damageItem(smpPlayer, staff, 1, EquipSlot.MAINHAND);

        MageBeam beam = new MageBeam(color);
        smpPlayer.getActiveEnchants().forEach((enchant, level) -> enchant.getEnchant().onMageBeamFire(beam, level, smpPlayer));

        double range = smpPlayer.getActiveAttributes().getOrDefault(Attributes.STAFF_RANGE_BASE, DEFAULT_RANGE);
        beam.launch(player, range);
        player.getWorld().playSound(player.getEyeLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.4f, 1.6f);
    }
}
