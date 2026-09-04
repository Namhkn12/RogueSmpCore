package com.roguesmp.player.mechanic;

import com.roguesmp.constant.EquipSlot;
import com.roguesmp.event.AbilityCastEvent;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.SmpItem;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.classes.PlayerClass;
import com.roguesmp.registry.Registries;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Denies ability cast from a weapon that isn't allowed for the
 * player's current {@link PlayerClass}.
 * {@link #isRestrictedWeapon} is also called directly by
 * {@link SmpPlayer#updateSlotStat} to suppress a disallowed mainhand weapon's stat
 * bonus.
 */
public class ClassRestrictionMechanic implements PlayerMechanic {

    public static final String WEAPON_TAG_ID = "weapons";

    @Override public int getPriority() { return 10; } // High restriction priority

    private boolean removeOldBar = false; //Work around to action bar staying too long, by sending an empty text bar...

    @Override
    public void tick(int periodIncrement, SmpPlayer player) {
        SmpItem weapon = player.getItemAtEquipSlot(EquipSlot.MAINHAND);
        Player bukkitPlayer = player.getBukkitPlayer();
        if (!isRestrictedWeapon(player, weapon == null ? null : weapon.getBaseItem())) {
            if (removeOldBar) {
                bukkitPlayer.sendActionBar(Component.empty());
                removeOldBar = false;
            }
            return;
        }

        if (bukkitPlayer != null) {
            removeOldBar = true;
            bukkitPlayer.sendActionBar(Component.text("⚠ Vũ khí này không phù hợp với class của bạn!", NamedTextColor.RED));
        }
    }

    @Override
    public void onAbilityCast(AbilityCastEvent event, SmpPlayer player) {
        SmpItem weapon = player.getItemAtEquipSlot(EquipSlot.MAINHAND);
        if (!isRestrictedWeapon(player, weapon == null ? null : weapon.getBaseItem())) return;
        event.setCancelled(true);
    }

    /**
     * Whether {@code baseItem} is a weapon (tagged {@value #WEAPON_TAG_ID}) that {@code player}'s
     * current class does NOT allow - always {@code false} for an item not tagged
     * {@value #WEAPON_TAG_ID} at all, regardless of class.
     */
    public static boolean isRestrictedWeapon(SmpPlayer player, @Nullable BaseItem baseItem) {
        if (baseItem == null) return false;

        Set<String> itemTagIds = Registries.ITEM.getHolder(baseItem.getId()).getTagIds();
        if (!itemTagIds.contains(WEAPON_TAG_ID)) return false; // Not a weapon-type item at all

        PlayerClass playerClass = player.getPlayerClass();
        Set<String> allowed = playerClass == null ? Set.of() : playerClass.getAllowedWeapons();
        for (String tagId : itemTagIds) {
            if (allowed.contains(tagId)) return false; // Allowed by the player's own class
        }
        return true;
    }
}
