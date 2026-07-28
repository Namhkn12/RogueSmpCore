package com.roguesmp.entity.boss.hellknight;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.EntityManager;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.entity.boss.hellknight.minion.HellKnightMinion;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.EntityUtils;
import com.roguesmp.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ShadowCloneSpell extends Spell {

    private final HellKnight boss;
    private final List<LivingEntity> activeClones = new ArrayList<>();
    private final List<Location> deathLocations = new ArrayList<>(); // Lưu vị trí chết
    private Location altarLocation;
    private boolean isConverging = false;

    private final int convergeDura = 50;

    private final String[] potentialClones = {
            "hell_knight_stray",
            "hell_knight_golem",
            "hell_knight_evoker",
            "hell_knight_blaze"
    };

    public ShadowCloneSpell(HellKnight boss) {
        this.boss = boss;
        this.altarLocation = boss.getAltarLocation();
    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    @Override
    public boolean onlyForceCasted() {
        return true;
    }

    @Override
    public void run(int interval) {
        LivingEntity caster = boss.getEntity();
        this.deathLocations.clear();
        this.isConverging = false;

        // 1. Ẩn Boss
        caster.setInvisible(true);
        caster.setAI(false);
        caster.setInvulnerable(true);
        caster.teleport(altarLocation.clone().add(0, -30, 0));

        List<Player> targets = boss.getParticipants();
        if (targets.isEmpty()) return;

        int cloneCount = Math.min(4, targets.size());
        if (cloneCount < 2) cloneCount = 2;

        // 2. Triệu hồi phân thân
        for (int i = 0; i < cloneCount; i++) {
            String randomId = potentialClones[(int) (Math.random() * potentialClones.length)];
            spawnDifferentClone(getRandomLocationAroundAltar(altarLocation), randomId);
        }

        // 3. Task theo dõi: Chỉ khi TẤT CẢ đã chết mới bắt đầu hiệu ứng bay
        new BukkitRunnable() {
            @Override
            public void run() {
                for (LivingEntity clone : activeClones) {
                    if (!clone.isValid() || clone.isDead()) {
                        recordDeathLocation(clone.getLocation());
                    }
                }
                activeClones.removeIf(clone -> !clone.isValid() || clone.isDead());

                // Nếu tất cả đã chết và chưa bắt đầu hội tụ
                if (activeClones.isEmpty() && !isConverging) {
                    isConverging = true;
                    startMassConvergence();
                    this.cancel();
                }
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0L, 5L);
    }

    private void spawnDifferentClone(Location spawnAt, String id) {
        SmpEntity entity = EntityManager.getInstance().spawnEntity(id, spawnAt);
        if (entity == null) return;
        if (entity instanceof HellKnightMinion minion) {
            minion.setMainBoss(this.boss);
        }
        activeClones.add(entity.getEntity());
    }

    /**
     * Gọi hàm này mỗi khi 1 phân thân chết để lưu vị trí
     */
    public void recordDeathLocation(Location loc) {
        this.deathLocations.add(loc.clone());
    }

    /**
     * Kích hoạt hiệu ứng bay đồng loạt từ tất cả các vị trí chết
     */
    private void startMassConvergence() {
        boss.dialogue("<dark_gray>Linh hồn trở về với cội nguồn...");

        final int[] soulsArrived = {0};

        for (Location deathLoc : deathLocations) {
            new BukkitRunnable() {
                final Location current = deathLoc.clone().add(0, 1, 0);
                final double distance = current.distance(altarLocation);
                // Tính toán tốc độ cố định dựa trên quãng đường / 40 ticks
                final double fixedSpeed = distance / convergeDura;
                int ticks = 0;

                @Override
                public void run() {
                    if (ticks > convergeDura + 10 || current.distanceSquared(altarLocation) < 1.5) {
                        soulsArrived[0]++;
                        altarLocation.getWorld().playSound(altarLocation, Sound.BLOCK_SOUL_SAND_BREAK, 1f, 0.5f);

                        // Nếu linh hồn cuối cùng đã bay tới nơi
                        if (soulsArrived[0] >= deathLocations.size()) {
                            convergeBoss();
                        }
                        this.cancel();
                        return;
                    }

                    Vector dir = altarLocation.toVector().subtract(current.toVector()).normalize().multiply(fixedSpeed);
                    current.add(dir);

                    current.getWorld().spawnParticle(Particle.SOUL, current, 3, 0.1, 0.1, 0.1, 0.02);
                    current.getWorld().spawnParticle(Particle.LARGE_SMOKE, current, 1, 0, 0, 0, 0);
                    current.getWorld().playSound(current, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f - (ticks * 0.015f));
                    ticks++;
                }
            }.runTaskTimer(RogueSmpCore.getInstance(), 0L, 1L);
        }
    }

    private void convergeBoss() {
        LivingEntity caster = boss.getEntity();
        caster.teleport(altarLocation);
        caster.setInvisible(false);
        caster.setAI(true);
        caster.setInvulnerable(false);

        altarLocation.getWorld().playSound(altarLocation, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 2f, 0.5f);
        altarLocation.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, altarLocation, 1);
        altarLocation.getWorld().strikeLightningEffect(altarLocation);

        DamageUtils.damagePercent(caster, null, 0.1, new DamageEvent.Metadata(DamageType.TRUE));
    }

    private Location getRandomLocationAroundAltar(Location altarLocation) {
        Random random = Utils.RANDOM;
        double angle = random.nextDouble() * 2 * Math.PI;
        double r = random.nextDouble() * 15;

        double x = altarLocation.getX() + r * Math.cos(angle);
        double z = altarLocation.getZ() + r * Math.sin(angle);

        Location loc = new Location(altarLocation.getWorld(), x, altarLocation.getY(), z);
        return loc.add(0, 1, 0);
    }
}
