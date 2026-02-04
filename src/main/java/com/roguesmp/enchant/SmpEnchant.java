package com.roguesmp.enchant;

import com.roguesmp.constant.Enchants;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.context.DamageContext;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public interface SmpEnchant {

    @NotNull String getId();

    @NotNull Enchants getEnumConstant();

    @NotNull String getSimpleName();

    @NotNull List<Component> getDisplayText(int level, SmpPlayer player, PersistentDataContainerView pdc);

    @NotNull Set<EquipSlot> getActiveSlots();

    default void onAttackEntity(DamageContext context, int level) {

    }

    default void onKillEntity(EntityDeathEvent event, int level) {

    }
}
