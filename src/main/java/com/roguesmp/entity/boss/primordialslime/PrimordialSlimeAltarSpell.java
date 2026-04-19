package com.roguesmp.entity.boss.primordialslime;

import com.roguesmp.entity.boss.PrimordialSlime;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.registry.entity.EntityRegistry;
import com.roguesmp.utils.ItemStackUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;

import java.util.Collection;
import java.util.Map;

/**
 * Specifically for the boss altar, to check for key item being dropped on the altar
 */
public class PrimordialSlimeAltarSpell extends Spell {

    private final LivingEntity altarEntity;

    public PrimordialSlimeAltarSpell(LivingEntity altarEntity) {
        this.altarEntity = altarEntity;
    }


    @Override
    public void run() {

         Collection<Entity> items = altarEntity.getWorld().getNearbyEntities(altarEntity.getLocation(),1, 0.5, 1, e -> e instanceof Item);
         for (Entity entity : items) {
             Item item = (Item) entity;
             String itemId = ItemStackUtils.getId(item.getItemStack());
             if (itemId != null && itemId.equals(PrimordialSlime.KEY_ITEM_ID)) {
                 EntityRegistry.getInstance().spawnEntity(PrimordialSlime.ID, item.getLocation());
                 altarEntity.getWorld().strikeLightningEffect(item.getLocation());
                 item.remove();
                 altarEntity.remove();
             }
         }

    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    public static PrimordialSlimeAltarSpell readParam(Map<String, Object> param, LivingEntity owner) {
        return new PrimordialSlimeAltarSpell(owner);
    }
}
