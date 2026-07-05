package com.roguesmp.player.mechanic;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.ItemStackUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class OffhandBlockerMechanic implements PlayerMechanic {
    @Override public int getPriority() { return 10; } // High Restriction priority

    @Override
    public void onProjectileLaunch(PlayerLaunchProjectileEvent event, SmpPlayer player) {
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null) return;

        ItemStack offhand = bukkitPlayer.getEquipment().getItemInOffHand();
        if (ItemStackUtils.isShootableItem(offhand)) {
            bukkitPlayer.sendMessage(Component.text("Bạn không thể ném/bắn khi có vũ khí ở tay phụ!", NamedTextColor.RED));
            event.setCancelled(true);
        }
    }

    @Override
    public void onShootArrow(EntityShootBowEvent event, SmpPlayer player) {
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null) return;

        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            bukkitPlayer.sendMessage(Component.text("Bạn không thể bắn tên từ tay phụ.", NamedTextColor.RED));
            event.setCancelled(true);
        }
    }
}
