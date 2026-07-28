package com.roguesmp.entity.spell.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellParams;
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

public class DummyEntitySpell extends Spell {

    public static final String TYPE_KEY = "dummy_entity_spell";

    public record Params() implements SpellParams {
        public static final Codec<Params> CODEC = MapCodec.unit(Params::new).codec();

        @Override
        public String getTypeId() {
            return TYPE_KEY;
        }
    }

    @Override
    public void run(int interval) {

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

    public static DummyEntitySpell create(Params params, LivingEntity owner) {
        return new DummyEntitySpell();
    }
}
