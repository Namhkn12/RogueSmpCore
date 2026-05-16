package com.roguesmp.entity.boss.hellknight.minion;

import com.roguesmp.entity.spell.Spell;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Random;

public class TpAwaySpell extends Spell {

    private final HellKnightMinion smpEntity;
    private final double damageThreshold; // Ngưỡng sát thương để kích hoạt TP
    private double accumulatedDamage = 0;
    private final double tpRadius; // Bán kính ngẫu nhiên quanh Altar

    public TpAwaySpell(HellKnightMinion minion, double damageThreshold, double tpRadius) {
        this.smpEntity = minion;
        this.damageThreshold = damageThreshold;
        this.tpRadius = tpRadius;
    }

    @Override
    public int cooldownTicks() {
        return 40; // 2 giây cooldown giữa mỗi lần TP để tránh bị rối loạn
    }

    /**
     * Hook này sẽ được gọi từ hàm onHurt trong lớp HellKnightStray
     */
    @Override
    public void onHurt(DamageEvent event) {

        accumulatedDamage += event.getFinalDamage();

        if (accumulatedDamage >= damageThreshold) {
            executeTeleport();
            accumulatedDamage = 0; // Reset tích lũy
        }
    }

    private void executeTeleport() {
        LivingEntity entity = smpEntity.getEntity();
        if (entity == null || !entity.isValid()) return;

        // 1. Hiệu ứng tại vị trí cũ
        Location oldLoc = entity.getLocation();
        oldLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, oldLoc.add(0, 1, 0), 20, 0.2, 0.5, 0.2, 0.05);
        oldLoc.getWorld().playSound(oldLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);

        // 2. Tính toán vị trí ngẫu nhiên quanh Altar
        Location newLoc = getRandomLocationAroundAltar();

        // 3. Dịch chuyển
        entity.teleport(newLoc);

        // 4. Hiệu ứng tại vị trí mới
        newLoc.getWorld().spawnParticle(Particle.SOUL, newLoc.add(0, 1, 0), 15, 0.2, 0.5, 0.2, 0.02);
        newLoc.getWorld().playSound(newLoc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.2f);

        Player random = PlayerUtils.playersInRange(newLoc, 20, true).stream()
                .findAny().orElse(null);
        if (random != null) {
            Location direction = entity.getLocation().setDirection(random.getLocation().subtract(entity.getLocation()).toVector());
            entity.teleport(direction);
        }
    }

    private Location getRandomLocationAroundAltar() {
        Random random = Utils.RANDOM;
        Location altarLocation;
        if (smpEntity.getMainBoss() == null){
            LivingEntity entity = smpEntity.getEntity();
            altarLocation = entity.getLocation();
        } else {
            altarLocation = smpEntity.getMainBoss().getAltarLocation();
        }
        double angle = random.nextDouble() * 2 * Math.PI;
        double r = random.nextDouble() * tpRadius;

        double x = altarLocation.getX() + r * Math.cos(angle);
        double z = altarLocation.getZ() + r * Math.sin(angle);

        Location loc = new Location(altarLocation.getWorld(), x, altarLocation.getY(), z);
        return loc.add(0, 1, 0);
    }

    @Override
    public void run(int interval) {
        // Spell này kích hoạt dựa trên Event, không cần chạy loop
    }
}
