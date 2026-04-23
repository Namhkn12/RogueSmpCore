package com.roguesmp.entity.boss.hellknight;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.entity.BaseEntity;
import com.roguesmp.entity.BossBarManager;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.utils.Hitbox;
import com.roguesmp.utils.MovementUtils;
import com.roguesmp.utils.Utils;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 *
 */
public class HellKnight extends SmpEntity {

    public static String ID = "hell_knight";
    public static final double ARENA_SIZE = 100;

    private final Map<Integer, BossBarManager.BossHealthAction> phaseEvents = new HashMap<>();
    private final Location altarLocation;

//    private final List<Spell> phase1Passives;
//    private final List<Spell> phase1Actives;
//
//    private final List<Spell> phase2Passive;
//    private final List<Spell> phase2Active;
//
//    private final List<Spell> phase3Passive;
//    private final List<Spell> phase3Active;

    private double defenseScaling;

    public HellKnight(BaseEntity base, LivingEntity entity) {
        super(base, entity);
        altarLocation = entity.getLocation();
        setupPhaseTrigger();
    }

    @Override
    public void initialize() {
        base.processEntity(entity);

        initialized = true;

        playIntroduction();
    }

    private void setupPhaseTrigger() {

    }

    private void playIntroduction() {
        entity.setInvisible(true);
        entity.setAI(false);
        entity.setInvulnerable(true);

        Location startLoc = altarLocation.clone().add(0, 15, 0);
        entity.teleport(startLoc);

        new BukkitRunnable() {
            int ticks = 0;
            final int COLLAPSE_START = 10;
            final int COLLAPSE_DURATION = 60; // 3 giây để co lại
            final int REVEAL_TICK = COLLAPSE_START + COLLAPSE_DURATION;

            @Override
            public void run() {
                if (!entity.isValid()) { this.cancel(); return; }
                ticks++;
                World world = entity.getWorld();
                Location center = entity.getLocation().add(0, 1, 0);

                // --- GIAI ĐOẠN 1: CÁC HẠT CO LẠI (COLLAPSING) ---
                if (ticks >= COLLAPSE_START && ticks < REVEAL_TICK) {
                    double progress = (double) (ticks - COLLAPSE_START) / COLLAPSE_DURATION;
                    double currentRadius = 8.0 * (1.0 - progress); // Bán kính giảm dần từ 8 về 0

                    // Hiệu ứng âm thanh tụ năng lượng (tăng pitch dần)
                    if (ticks % 5 == 0) {
                        world.playSound(Sound.sound(SoundEventKeys.BLOCK_BREWING_STAND_BREW, Sound.Source.HOSTILE, 1.5f, (float) (0.5 + progress)), Sound.Emitter.self());
                    }

                    // Tạo nhiều vòng xoáy hạt khác nhau
                    for (int i = 0; i < 5; i++) {
                        double angle = (ticks * 0.5) + (i * (Math.PI * 2 / 5));

                        // Vòng xoáy ngang (Soul Fire)
                        double x = Math.cos(angle) * currentRadius;
                        double z = Math.sin(angle) * currentRadius;
                        world.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(x, Math.sin(ticks * 0.2) * 2, z), 1, 0, 0, 0, 0);

                        // Vòng xoáy dọc (Smoke)
                        double y = Math.cos(angle) * currentRadius;
                        double x2 = Math.sin(angle) * currentRadius;
                        world.spawnParticle(Particle.LARGE_SMOKE, center.clone().add(x2, y, 0), 1, 0, 0, 0, 0);
                    }

                    // Bụi ma thuật hút vào tâm
                    world.spawnParticle(Particle.PORTAL, center, 10, currentRadius, currentRadius, currentRadius, 0);
                }

                // --- GIAI ĐOẠN 2: BÙNG NỔ & XUẤT HIỆN ---
                if (ticks == REVEAL_TICK) {
                    entity.setInvisible(false);
                    world.strikeLightningEffect(center);
                    world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 3);
                    world.playSound(Sound.sound(SoundEventKeys.ENTITY_WITHER_SPAWN, Sound.Source.HOSTILE, 2.0f, 0.5f));

                    dialogue("<dark_red><bold>Hỏa Ngục:</bold> CÁC NGƯƠI ĐÃ TRIỆU HỒI CÁI CHẾT!");

                    // Hiệu ứng hất tung nhẹ khi bùng nổ
                    getParticipants().forEach(p -> {
                        if (p.getLocation().distance(center) < 10) {
                            p.setVelocity(new Vector(0, 0.5, 0));
                        }
                    });

                    entity.setAI(true);
                }

                // --- GIAI ĐOẠN 3: LAO XUỐNG ---
//                if (ticks == REVEAL_TICK + 20) {
//                    Player target = getNearestParticipant();
//                    if (target != null) {
//                        privateMessage(target, "Ngươi sẽ là kẻ đầu tiên tan thành tro bụi!");
//
//                        Vector slamVec = target.getLocation().toVector()
//                                .subtract(entity.getLocation().toVector())
//                                .normalize().multiply(2.5);
//
//                        entity.setAI(true);
//                        entity.setVelocity(slamVec);
//                        world.playSound(center, Sound.ENTITY_GHAST_SCREAM, 2.0f, 0.5f);
//                    }
//                }

                // Impact check
                if (ticks > REVEAL_TICK + 20 && (entity.isOnGround() || ticks > 200)) {
//                    applyShockwave(entity.getLocation(), 8.0, 40);
                    startCombat();
                    this.cancel();
                }
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0L, 1L);
    }

    private void startCombat() {
        entity.setInvisible(false);
        entity.setAI(true);
        entity.setInvulnerable(false);


    }

    public List<Player> getParticipants() {
        if (altarLocation == null) {
            return Collections.emptyList();
        }
        Location center = altarLocation.clone();
        center.setY(altarLocation.getY() - 5);
        Hitbox hb = new Hitbox.UprightCylinderHitbox(center, 62, ARENA_SIZE / 2d);
        return hb.getHitPlayers(true);
    }

    public void dialogue(int delayPerLine, List<String> messages) {
        if (messages.isEmpty()) return;
        new BukkitRunnable() {
            final List<Player> participants = getParticipants();
            int current = 0;
            final int lines = messages.size();

            @Override
            public void run() {
                if (current > lines - 1) cancel();
                participants.forEach(player -> player.sendMessage(Utils.fromString(messages.get(current))));
                current++;
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 1, delayPerLine);
    }

    public void dialogue(String message) {
        dialogue(1, Collections.singletonList(message));
    }

    @Override
    public void onNearbyPlayerDeath(PlayerDeathEvent event) {
        super.onNearbyPlayerDeath(event);
    }
}
