package com.roguesmp.entity.boss.primordialslime;

import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class ShieldStunSpell extends Spell {

    private final int tick;
    private final DamageType damageType;

    public ShieldStunSpell(int tick, DamageType damageType) {
        this.tick = tick;
        this.damageType = damageType;
    }

    @Override
    public void run() {

    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    @Override
    public void onDamage(DamageEvent event) {
        if (event.getDamageType() == damageType && event.getVictim() instanceof Player player) {
            if (player.isHandRaised() && player.getActiveItem().hasData(DataComponentTypes.BLOCKS_ATTACKS)) {
                player.setCooldown(player.getActiveItem().getType(), tick);
                player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 0.8f, 0.8f + Utils.RANDOM.nextFloat() * 0.4F);
            }
        }
    }
}
