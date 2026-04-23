package com.roguesmp.entity.boss.primordialslime;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageType;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.impl.SpeedEffect;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class AcidSludgeSpell extends Spell {
    private final LivingEntity owner;
    private final double damageTick;

    private int tick;

    public AcidSludgeSpell(LivingEntity owner, double damageTick) {
        this.owner = owner;
        this.damageTick = damageTick;
    }

    @Override
    public void run(int interval) {
        if (owner == null || !owner.isValid()) return;
        tick++;
        if (tick > cooldownTicks()) return;
        tick = 0;
        Location dropLoc = owner.getLocation();

        // Tạo một "vũng lầy" bằng hạt trong 5 giây
        new BukkitRunnable() {
            int duration = 100; // 5 seconds

            @Override
            public void run() {
                if (duration <= 0) {
                    this.cancel();
                    return;
                }

                // Visual: Dịch chảy dưới đất
                dropLoc.getWorld().spawnParticle(Particle.SNEEZE, dropLoc, 5, 0.5, 0.1, 0.5, 0.02);
                dropLoc.getWorld().spawnParticle(Particle.SQUID_INK, dropLoc, 3, 0.3, 0.1, 0.3, 0.01);

                // Gây sát thương nếu người chơi đứng trong vũng
                for (Player p : dropLoc.getNearbyPlayers(1.5)) {
                    DamageUtils.damage(p, owner, damageTick, new DamageEvent.Metadata(DamageType.AILMENT));
                    EffectManager.getInstance().addEffect(p, "acid_sludge", new SpeedEffect(40, -0.5, "acid_sludge"));
                }
                duration -= 5;
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0L, 5L);
    }

    @Override
    public int cooldownTicks() { return 20; } // Gần như liên tục khi Boss di chuyển
}
