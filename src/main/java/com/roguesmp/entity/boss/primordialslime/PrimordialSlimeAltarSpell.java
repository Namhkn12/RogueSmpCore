package com.roguesmp.entity.boss.primordialslime;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.entity.EntityManager;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.utils.ItemStackUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;

import java.util.Collection;

/**
 * Specifically for the boss altar, to check for key item being dropped on the altar
 */
public class PrimordialSlimeAltarSpell extends Spell {

    public static final String TYPE_KEY = "primordial_slime_altar_spell";

    public record Params() implements SpellParams {
        public static final Codec<Params> CODEC = MapCodec.unit(Params::new).codec();

        @Override
        public String getTypeId() {
            return TYPE_KEY;
        }
    }

    private final LivingEntity altarEntity;

    public PrimordialSlimeAltarSpell(LivingEntity altarEntity) {
        this.altarEntity = altarEntity;
    }


    @Override
    public void run(int interval) {

         Collection<Entity> items = altarEntity.getWorld().getNearbyEntities(altarEntity.getLocation(),1, 0.5, 1, e -> e instanceof Item);
         for (Entity entity : items) {
             Item item = (Item) entity;
             String itemId = ItemStackUtils.getBaseId(item.getItemStack());
             if (itemId != null && itemId.equals(PrimordialSlime.KEY_ITEM_ID)) {
                 EntityManager.getInstance().spawnEntity(PrimordialSlime.ID, item.getLocation());
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

    public static PrimordialSlimeAltarSpell create(Params params, LivingEntity owner) {
        return new PrimordialSlimeAltarSpell(owner);
    }
}
