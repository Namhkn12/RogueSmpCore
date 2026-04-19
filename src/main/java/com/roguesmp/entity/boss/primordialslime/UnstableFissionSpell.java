package com.roguesmp.entity.boss.primordialslime;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.boss.PrimordialSlime;
import com.roguesmp.entity.spell.ChargeUpManager;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class UnstableFissionSpell extends Spell {
    private final LivingEntity owner;
    private final PrimordialSlime boss;

    // --- Configuration Fields ---
    private final int chargeUpTicks;      // Thời gian gồng
    private final int projectileCount;    // Số lượng mảnh phân thân
    private final double projectileSpeed; // Tốc độ bay
    private final int projectileLife;     // Thời gian tồn tại (ticks)
    private final double damage;          // Sát thương khi nổ
    private final double explosionRadius; // Bán kính nổ
    private final String spellDisplayName;// Tên hiển thị trên BossBar

    public UnstableFissionSpell(LivingEntity owner, PrimordialSlime boss,
                                String spellDisplayName, int chargeUpTicks,
                                int projectileCount, double projectileSpeed,
                                int projectileLife, double damage, double explosionRadius) {
        this.owner = owner;
        this.boss = boss;
        this.spellDisplayName = spellDisplayName;
        this.chargeUpTicks = chargeUpTicks;
        this.projectileCount = projectileCount;
        this.projectileSpeed = projectileSpeed;
        this.projectileLife = projectileLife;
        this.damage = damage;
        this.explosionRadius = explosionRadius;
    }

    @Override
    public void run() {
        if (owner == null || !owner.isValid()) return;

        // BossBar sử dụng tên hiển thị từ config
        ChargeUpManager chargeUp = new ChargeUpManager(
                owner,
                chargeUpTicks,
                Component.text(spellDisplayName, NamedTextColor.DARK_PURPLE, TextDecoration.BOLD),
                BossBar.Color.PURPLE,
                BossBar.Overlay.NOTCHED_6,
                40
        );

        owner.getWorld().playSound(owner.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 1.5f, 0.5f);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (owner.isDead() || !owner.isValid()) {
                    chargeUp.remove();
                    this.cancel();
                    return;
                }

                owner.getWorld().spawnParticle(Particle.WITCH, owner.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.1);

                // Rung lắc nhẹ tỷ lệ thuận với cường độ gồng
                owner.setVelocity(new Vector((Math.random() - 0.5) * 0.08, 0, (Math.random() - 0.5) * 0.08));

                if (chargeUp.nextTick()) {
                    chargeUp.remove();
                    launchFissionProjectiles();
                    this.cancel();
                }
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0L, 1L);
    }

    private void launchFissionProjectiles() {
        owner.getWorld().playSound(owner.getLocation(), Sound.ENTITY_SLIME_ATTACK, 2f, 0.5f);

        // Góc bắn tự động chia đều theo số lượng projectiles trong config
        double angleStep = (Math.PI * 2) / projectileCount;

        for (int i = 0; i < projectileCount; i++) {
            double angle = i * angleStep;
            Vector direction = new Vector(Math.cos(angle), 0, Math.sin(angle)).multiply(projectileSpeed);

            MagmaCube minion = owner.getWorld().spawn(owner.getLocation().add(0, 0.5, 0), MagmaCube.class, m -> {
                m.setSize(4);
                m.setAI(false);
                m.setInvulnerable(true);
                m.setGravity(false);
                m.setPersistent(false); // Tránh việc quái không biến mất khi server restart
            });

            new BukkitRunnable() {
                int life = projectileLife;

                @Override
                public void run() {
                    if (life <= 0 || !minion.isValid()) {
                        triggerExplosion(minion);
                        this.cancel();
                        return;
                    }

                    minion.teleport(minion.getLocation().add(direction));

                    // Va chạm logic
                    BoundingBox collisionBox = minion.getBoundingBox().expand(0.2);

                    // Check Participants
                    boolean hitParticipant = boss.getParticipants().stream()
                            .anyMatch(p -> collisionBox.overlaps(p.getBoundingBox()));

                    if (hitParticipant || isHittingBlock(minion)) {
                        triggerExplosion(minion);
                        this.cancel();
                        return;
                    }

                    minion.getWorld().spawnParticle(Particle.SNEEZE, minion.getLocation(), 3, 0.1, 0.1, 0.1, 0.02);
                    life--;
                }
            }.runTaskTimer(RogueSmpCore.getInstance(), 0L, 1L);
        }
    }

    private boolean isHittingBlock(Entity entity) {
        BoundingBox box = entity.getBoundingBox();
        World world = entity.getWorld();
        for (int x = (int)Math.floor(box.getMinX()); x <= (int)Math.floor(box.getMaxX()); x++) {
            for (int y = (int)Math.floor(box.getMinY()); y <= (int)Math.floor(box.getMaxY()); y++) {
                for (int z = (int)Math.floor(box.getMinZ()); z <= (int)Math.floor(box.getMaxZ()); z++) {
                    Block b = world.getBlockAt(x, y, z);
                    if (b.getType().isSolid() && box.overlaps(b.getBoundingBox())) return true;
                }
            }
        }
        return false;
    }

    private void triggerExplosion(MagmaCube minion) {
        Location loc = minion.getLocation();
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
        loc.getWorld().spawnParticle(Particle.ITEM_SLIME, loc, 15, 0.3, 0.3, 0.3, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_SLIME_SQUISH, 1.2f, 0.6f);

        for (Player p : loc.getNearbyPlayers(explosionRadius)) {
            DamageUtils.damage(p, owner, damage, new DamageEvent.Metadata(DamageType.MAGIC));
            p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));
        }
        minion.remove();
    }

    @Override
    public int cooldownTicks() { return 200; }
}
