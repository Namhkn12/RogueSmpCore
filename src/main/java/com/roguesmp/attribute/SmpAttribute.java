package com.roguesmp.attribute;

import com.roguesmp.constant.Attributes;
import com.roguesmp.context.DamageContext;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface SmpAttribute {
    @NotNull String getId();

    @NotNull Attributes getEnumConstant();

    @NotNull String getSimpleName();

    @NotNull List<Component> getDisplayText(double value, SmpPlayer player, PersistentDataContainerView pdc);

    /**
     * Add attribute on equip
     */
    default void addVanillaAttribute(Player player, double value) {
//        Example:
//        player.getAttribute(Attribute.ATTACK_DAMAGE).addTransientModifier(new AttributeModifier(new NamespacedKey("smp", "attack"), 10d, AttributeModifier.Operation.ADD_NUMBER));
    }

    /**
     * Remove attribute on unequip
     */
    default void removeVanillaAttribute(Player player) {
//        Example
//        player.getAttribute(Attribute.ATTACK_DAMAGE).removeModifier(MODIFIER_ID);
    }

    default void onAttackEntity(DamageContext context, double value) {

    }

    default void onKillEntity(EntityDeathEvent event, double value) {

    }
}
