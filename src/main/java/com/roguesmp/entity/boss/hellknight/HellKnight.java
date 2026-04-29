package com.roguesmp.entity.boss.hellknight;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageOperation;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.impl.SpeedEffect;
import com.roguesmp.entity.BaseEntity;
import com.roguesmp.entity.BossBarManager;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.entity.boss.hellknight.minion.LineChargeSpell;
import com.roguesmp.entity.boss.hellknight.minion.companion.HellKnightCompanion;
import com.roguesmp.entity.boss.hellknight.minion.companion.InfernalTremor;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellManager;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.registry.entity.EntityRegistry;
import com.roguesmp.utils.*;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 *
 */
public class HellKnight extends SmpEntity {

    public static String ID = "hell_knight";
    public static final double ARENA_SIZE = 100;

    private final Map<Integer, BossBarManager.BossHealthAction> phaseEvents = new HashMap<>();
    private final Location altarLocation;

    private final List<Spell> phase1Passives;
    private final List<Spell> phase1Actives;

    private final List<Spell> phase2Passive;
    private final List<Spell> phase2Active;

    private final List<Spell> phase3Passive;
    private final List<Spell> phase3Active;

    private final List<Spell> phase4Passive;
    private final List<Spell> phase4Active;

    private HellKnightCompanion companion;
    private double defenseScaling;

    public HellKnight(BaseEntity base, LivingEntity entity) {
        super(base, entity);
        altarLocation = entity.getLocation();
        setupPhaseTrigger();

        phase1Passives = Arrays.asList(
                new SummonMinionSpell(entity, this, altarLocation, List.of("hell_knight_minion_melee", "hell_knight_minion_ranged"), 2, 2, 15),
                new TeleportBehindSpell(this, 1.5, 10)
        );

        phase1Actives = Arrays.asList(
                new MultiTargetBoltSpell(this, 12, 1.4, 1.1, 1, 12)
        );

        phase2Passive = Arrays.asList(
                new SummonMinionSpell(entity, this, altarLocation, List.of("hell_knight_minion_melee", "hell_knight_minion_ranged"), 2, 3, 15),
                new PeriodicLightningStrike(this, 15, 160, 25, 3)
        );
        phase2Active = Arrays.asList(
                new ShadowCloneSpell(this)
        );

        phase3Passive = Arrays.asList(
                new SummonMinionSpell(entity, this, altarLocation, List.of("hell_knight_minion_melee", "hell_knight_minion_ranged"), 3, 3, 15),
                new PeriodicLightningStrike(this, 15, 160, 25, 3)
        );
        phase3Active = Arrays.asList(
                new InfernalCleave(this, 25, 26, 220, 40),
                new MultiTargetBoltSpell(this, 12, 1.3, 1, 3, 15),
                new LineChargeSpell(this, 15, 2.0, 30, 3,20, 100)
        );

        phase4Passive = Arrays.asList(
                new SummonMinionSpell(entity, this, altarLocation, List.of("hell_knight_minion_melee", "hell_knight_minion_ranged"), 2, 3, 15),
                new PeriodicLightningStrike(this, 15, 100, 20, 3),
                new WatchfulEye(this, 2, 200, 200)
        );
        phase4Active = Arrays.asList(
                new PeriodicLightningStrike(this, 15, 160, 25, 3.5),
                new MultiTargetBoltSpell(this, 12, 1.3, 1, 3, 15)
        );

        defenseScaling = EntityUtils.healthScalingCoef(getParticipants().size(), 0.7, 0.65);
    }

    @Override
    public void initialize() {
        base.processEntity(entity);

        initialized = true;

        playIntroduction();
    }

    private void setupPhaseTrigger() {
        phaseEvents.put(80, boss -> {
            this.changePhase(SpellManager.EMPTY, Collections.emptyList(), null);

            getParticipants().forEach(player -> {
                player.playSound(Sound.sound(SoundEventKeys.ENTITY_WITHER_AMBIENT, Sound.Source.HOSTILE, 0.5f, 1.2f));
                player.playSound(Sound.sound(SoundEventKeys.ENTITY_ENDER_DRAGON_GROWL, Sound.Source.HOSTILE, 1.2f, 0.8f));

                EffectManager.getInstance().addEffect(player, "HkTransitionSlowness", new SpeedEffect(40, 0.2, "hk_transition_slowness"));
            });

            PlayerUtils.playersInRange(boss.getLocation(), 6, true).forEach(player -> {
                MovementUtils.knockAway(boss, player, 0.4f);
            });

            setAi(false);
            entity.getWorld().spawnParticle(Particle.FLAME, entity.getLocation().add(0, 1.5, 0), 30, 0.5, 0.5, 0.5);


            Utils.runLater(() -> {
                setAi(true);
                this.changePhase(new SpellManager(phase2Active), phase2Passive, living -> dialogue("<red><b>Để xem các ngươi xử lí thế nào..."));
                this.forceCastSpell(ShadowCloneSpell.class); // After this spell ends, boss lose 10% hp, which will transition him to 70% phase
            }, 20);
        });

        phaseEvents.put(70, boss -> {

            this.changePhase(new SpellManager(phase3Active), phase3Passive, null);

        });

        phaseEvents.put(50, boss -> {

            this.changePhase(SpellManager.EMPTY, Collections.emptyList(), living -> {
                dialogue(60, List.of("<b><red>Không tồi chút nào...",
                        "<b><red>Chiến mã hãy tới đây!"));
            });

            getParticipants().forEach(player -> {
                player.playSound(Sound.sound(SoundEventKeys.ENTITY_WITHER_AMBIENT, Sound.Source.HOSTILE, 0.5f, 1.2f));
                player.playSound(Sound.sound(SoundEventKeys.ENTITY_ENDER_DRAGON_GROWL, Sound.Source.HOSTILE, 1.2f, 0.8f));

                EffectManager.getInstance().addEffect(player, "HkTransitionSlowness", new SpeedEffect(40, 0.2, "hk_transition_slowness"));
            });

            PlayerUtils.playersInRange(boss.getLocation(), 6, true).forEach(player -> {
                MovementUtils.knockAway(boss, player, 0.4f);
            });

            setAi(false);
            entity.getWorld().spawnParticle(Particle.FLAME, entity.getLocation().add(0, 1.5, 0), 30, 0.5, 0.5, 0.5);

            Utils.runLater(() -> {
                entity.teleport(altarLocation);
                summonCompanion();
                this.changePhase(new SpellManager(phase3Active), phase3Passive, null);
            }, 30);

        });


        phaseEvents.put(30, boss -> {

            companion.changePhase(SpellManager.EMPTY, Collections.emptyList(), null);
            this.changePhase(SpellManager.EMPTY, Collections.emptyList(), living -> {
                dialogue("<b><red>Các ngươi thực sự rất mạnh...");
            });

            this.changePhase(SpellManager.EMPTY, Collections.emptyList(), null);

            getParticipants().forEach(player -> {
                player.playSound(Sound.sound(SoundEventKeys.ENTITY_WITHER_AMBIENT, Sound.Source.HOSTILE, 0.5f, 1.2f));
                player.playSound(Sound.sound(SoundEventKeys.ENTITY_ENDER_DRAGON_GROWL, Sound.Source.HOSTILE, 1.2f, 0.8f));

                EffectManager.getInstance().addEffect(player, "HkTransitionSlowness", new SpeedEffect(40, 0.2, "hk_transition_slowness"));
            });

            PlayerUtils.playersInRange(boss.getLocation(), 6, true).forEach(player -> {
                MovementUtils.knockAway(boss, player, 0.4f);
            });

            setAi(false);
            entity.getWorld().spawnParticle(Particle.FLAME, entity.getLocation().add(0, 1.5, 0), 30, 0.5, 0.5, 0.5);

            Utils.runLater(() -> {
                companion.getEntity().teleport(altarLocation);

                // Slight offset for the rider to ensure they don't clip
                entity.teleport(altarLocation.clone().add(1.5, 1, 1.5));

                // 4. Visual/Audio Flair for the TP
                entity.getWorld().playSound(Sound.sound(SoundEventKeys.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE,2f, 0.8f));
                entity.getWorld().spawnParticle(Particle.REVERSE_PORTAL, altarLocation.clone().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.1);

                // 5. The Mounting
                // We use a 1-tick delay to ensure the teleportation packets have
                // been processed by the client before mounting occurs.
                Utils.runLater(() -> {
                    if (!entity.isValid() || !companion.getEntity().isValid()) return;

                    companion.getEntity().addPassenger(entity);

                    // Re-enable AI or start the next phase movement logic
                    setAi(true);

                    companion.changePhase(new SpellManager(List.of(new InfernalTremor(companion.getEntity(), 14, 3, 30,40, 1.2, 30))), Collections.emptyList(), null);
                    HellKnight.this.changePhase(new SpellManager(phase4Active), phase4Passive, null);
                });

            }, 30);
        });
    }

    private void playIntroduction() {
        entity.setInvisible(true);
        setAi(false);

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
                        world.playSound(Sound.sound(SoundEventKeys.ENTITY_BLAZE_SHOOT, Sound.Source.HOSTILE, 1.5f, (float) (0.5 + progress)), Sound.Emitter.self());
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

                    PlayerUtils.playersInRange(entity.getLocation(), 6, true).forEach(player -> {
                        // 1. Get the direction vector
                        MovementUtils.knockAway(entity, player, 1f);
                    });

                    dialogue("<dark_red><bold>CÁC NGƯƠI ĐÃ TÌM ĐẾN CÁI CHẾT!");
                }

                if (ticks > REVEAL_TICK) {
                    startCombat();
                    this.cancel();
                }
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0L, 1L);
    }

    private void startCombat() {
        entity.setInvisible(false);
        setAi(true);

        getParticipants().forEach(player -> {
            player.showTitle(Title.title(Utils.fromString("<b><dark_red>Kị sĩ hỏa ngục"), Component.text("......", NamedTextColor.WHITE, TextDecoration.OBFUSCATED)));
        });

        BossBarManager bb = new BossBarManager(this.entity, 60, BossBar.Color.RED, BossBar.Overlay.NOTCHED_10, phaseEvents);

        startSpell(new SpellManager(phase1Actives), phase1Passives, 60, bb, 40, 2, true);
    }

    private void summonCompanion() {
        Location summonLoc = entity.getLocation().add(entity.getLocation().getDirection().multiply(4));
        summonLoc.setY(altarLocation.getY() + 1);

        new BukkitRunnable() {
            int timer = 0;

            @Override
            public void run() {
                if (timer >= 60) { // After 3 seconds

                    companion = (HellKnightCompanion) EntityRegistry.getInstance().spawnEntity(HellKnightCompanion.ID, summonLoc);
                    if (companion == null) {
                        RogueSmpCore.LOGGER.warn("Cannot find files for HellKnightCompanion!");
                        return;
                    }
                    companion.setMainBoss(HellKnight.this);

                    setAi(true);
                    this.cancel();
                    return;
                }

                if (timer % 5 == 0) {
                    summonLoc.getWorld().spawnParticle(Particle.BLOCK, summonLoc, 10, 0.5, 0.1, 0.5, 0, Material.DIRT.createBlockData());
                    summonLoc.getWorld().playSound(Sound.sound(SoundEventKeys.BLOCK_GRAVEL_BREAK, Sound.Source.HOSTILE,1.2f, 0.5f));
                }

                double angle = timer * 0.5;
                double x = Math.cos(angle) * 1.5;
                double z = Math.sin(angle) * 1.5;
                Location particleLoc = summonLoc.clone().add(x, (timer * 0.05), z);

                summonLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 2, 0, 0, 0, 0);
                summonLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, particleLoc, 1, 0, 0, 0, 0.02);

                // 3. Audio: Increasing pitch roar
                if (timer == 40) {
                    summonLoc.getWorld().playSound(Sound.sound(SoundEventKeys.ENTITY_RAVAGER_ROAR, Sound.Source.HOSTILE, 1.5f, 0.5f));
                }

                timer++;
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 20L, 1L);
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
                if (current > lines - 1) {
                    cancel();
                    return;
                }
                participants.forEach(player -> player.sendMessage(Utils.fromString(messages.get(current))));
                current++;
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 1, delayPerLine);
    }

    public void dialogue(String message) {
        dialogue(1, Collections.singletonList(message));
    }

    public void setAi(boolean state) {
        entity.setAI(state);
        entity.setInvulnerable(!state);
        if (companion != null) {
            companion.setAi(state);
        }
    }

    public Location getRandomLocationAroundAltar(double radius) {
        Random random = Utils.RANDOM;
        double angle = random.nextDouble() * 2 * Math.PI;
        double r = random.nextDouble() * radius;

        double x = altarLocation.getX() + r * Math.cos(angle);
        double z = altarLocation.getZ() + r * Math.sin(angle);

        Location loc = new Location(altarLocation.getWorld(), x, altarLocation.getY(), z);
        return loc.add(0, 1, 0);
    }

    @Override
    public boolean hasPlayerDeathTrigger() {
        return true;
    }

    @Override
    public void onNearbyPlayerDeath(PlayerDeathEvent event) {
        defenseScaling = EntityUtils.healthScalingCoef(getParticipants().size(), 0.7, 0.65);
    }

    @Override
    public void onDeath(EntityDeathEvent event) {
        super.onDeath(event);
        if (companion != null) companion.getEntity().setHealth(0);
        event.setDeathSound(org.bukkit.Sound.ENTITY_WITHER_DEATH);
        dialogue("<red><b>Ta sẽ... quay trở lại...");
    }

    @Override
    public void onHurt(DamageEvent event) {
        event.addDamageModifier(1 / defenseScaling, DamageOperation.MORE_FINAL);
        super.onHurt(event);
    }

    public Location getAltarLocation() {
        return altarLocation;
    }
}
