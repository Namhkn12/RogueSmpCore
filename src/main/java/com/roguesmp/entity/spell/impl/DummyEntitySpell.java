package com.roguesmp.entity.spell.impl;

import com.roguesmp.entity.spell.Spell;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class DummyEntitySpell extends Spell {

    @Override
    public void run() {

    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    @Override
    public void onHurt(DamageEvent event) {
        Utils.runLater(() -> {
            if (event.getVictim() instanceof LivingEntity living) {
                AttributeInstance maxHp = living.getAttribute(Attribute.MAX_HEALTH);
                if (maxHp != null) {
                    living.setHealth(maxHp.getBaseValue());
                }
            }
            if (event.getDamager() instanceof Player player) {
                EntityEquipment equipment = player.getEquipment();
                if (player.isSneaking() && equipment.getItemInMainHand().getType() == Material.AIR) {
                    event.getVictim().remove();
                }
                player.sendMessage(Component.text("Damage dealt: " + event.getFinalDamage(), NamedTextColor.RED));
            }
        });
    }

    public static DummyEntitySpell factory(@Nullable Map<String, Object> param, LivingEntity owner) {
        return new DummyEntitySpell();
    }
}
