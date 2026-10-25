package com.roguesmp.entity.spell.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.entity.spell.common.HitCondition;
import com.roguesmp.entity.spell.common.OnHitSpell;
import com.roguesmp.entity.spell.common.SpellTargets;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.mechanic.DurabilityLossMechanic;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;

/**
 * Extra armor durability loss on hit. Custom items go through {@link DurabilityLossMechanic} (custom
 * durability + {@code DurabilityChangedEvent}); vanilla armor uses vanilla item damage.
 */
public class ArmorShredOnHitSpell extends OnHitSpell {

    private static final List<EquipSlot> ARMOR_SLOTS = List.of(EquipSlot.HEAD, EquipSlot.CHEST, EquipSlot.LEGS, EquipSlot.FEET);

    public record Params(HitCondition condition, int durabilityDamage) implements SpellParams {
        public static final Codec<Params> CODEC = Codec.composite(
                HitCondition.MAP_CODEC.forGetter(Params::condition),
                Codec.INT.optionalFieldOf("durabilityDamage", 3).forGetter(Params::durabilityDamage),
                Params::new
        );
    }

    private final int durabilityDamage;

    public ArmorShredOnHitSpell(LivingEntity owner, Params params) {
        super(owner, params.condition());
        this.durabilityDamage = params.durabilityDamage();
    }

    @Override
    protected void onHit(LivingEntity victim, DamageEvent event) {
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(victim.getUniqueId());
        EntityEquipment equipment = victim.getEquipment();

        for (EquipSlot slot : ARMOR_SLOTS) {
            EquipmentSlot vanillaSlot = slot.getVanillaSlot();
            if (equipment == null || vanillaSlot == null || equipment.getItem(vanillaSlot).isEmpty()) continue;

            if (smpPlayer != null) {
                DurabilityLossMechanic.damageItem(smpPlayer, smpPlayer.getItemAtEquipSlot(slot), durabilityDamage, slot);
            }
            victim.damageItemStack(vanillaSlot, durabilityDamage);
        }

        victim.getWorld().spawnParticle(Particle.CRIT, SpellTargets.centerOf(victim), 10, 0.3, 0.4, 0.3, 0.1);
        victim.getWorld().playSound(victim.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1f, 0.5f);
    }
}
