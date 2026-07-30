package com.roguesmp.entity.boss.primordialslime;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageOperation;
import com.roguesmp.entity.component.EntityComponentKeys;
import com.roguesmp.entity.BaseEntity;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.entity.component.impl.BossBarComponent;
import com.roguesmp.entity.component.impl.SpellComponent;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellManager;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.EntityUtils;
import com.roguesmp.utils.Hitbox;
import com.roguesmp.utils.MovementUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class PrimordialSlime extends SmpEntity {

    public static final String KEY_ITEM_ID = "primordial_slime_key";
    public static final String ID = "primordial_slime";
    public static final double ARENA_SIZE = 100;

    private final Map<Integer, SpellComponent.PhaseManager.BossHealthAction> phaseEvents = new HashMap<>();
    private final Location altarLocation;

    private double defenseScaling;

    private final Spell globalPassive1 = new AccumulateDamage(entity, 10, 50, 40);

    private final List<Spell> phase1Passives;
    private final List<Spell> phase1Actives;

    private final List<Spell> phase2Passive;
    private final List<Spell> phase2Active;

    private final List<Spell> phase3Passive;
    private final List<Spell> phase3Active;

    private int currentPhase = 1;

    private boolean introFinished;

    private final SpellComponent spellCasting;

    public PrimordialSlime(BaseEntity base, LivingEntity entity) {
        super(base, entity);
        // Preserve a JSON-declared SpellComponent if one exists; only attach an empty one otherwise.
        spellCasting = getOrCreate(EntityComponentKeys.SPELLS, () -> new SpellComponent(this));
        altarLocation = entity.getLocation();
        setupPhaseTriggers();
        defenseScaling = EntityUtils.healthScalingCoef(getParticipants().size(), 0.7, 0.65);
        phase1Passives = Arrays.asList(
                globalPassive1
        );

        phase1Actives = Arrays.asList(
                new MagmaShockwave(entity, 20, 30, ARENA_SIZE / 4, 0.6, 0.9, 0.7),
                new MagmaTendrils(entity, 20, 3, 30, 15, 50, 2.5)
        );

        phase2Passive = Arrays.asList(
                globalPassive1,
                new SummonMinionSpell(entity, this,altarLocation, List.of("tmd_ashen_executioner", "tmd_ashen_executioner_2"), 1, 2, 5)
        );
        phase2Active = Arrays.asList(
                new MagmaShockwave(entity, 20, 30, ARENA_SIZE / 4, 0.6, 0.9, 0.7),
                new MagmaTendrils(entity, 20, 3, 30, 15, 50, 2.5),
                new DoomTotemSpell(entity, this, altarLocation.clone().add(0, 1, 0), 4,240, 100),
                new MeteorRainSpell(entity, this, altarLocation, ARENA_SIZE / 2, 40, 300)
        );

        phase3Passive = Arrays.asList(
                globalPassive1,
                new SummonMinionSpell(entity, this,altarLocation, List.of("tmd_ashen_executioner", "tmd_ashen_executioner_2"), 2, 3, 8),
                new AcidSludgeSpell(entity, 5)
        );
        phase3Active = Arrays.asList(
                new DoomTotemSpell(entity, this, altarLocation.clone().add(0, 1, 0), 4,200, 100),
                new MeteorRainSpell(entity, this, altarLocation, ARENA_SIZE / 2, 40, 300),
                new UnstableFissionSpell(entity, this, "!!! PHÂN BÀO !!!", 40, 4, 0.8, 40, 8.0, 2.5)
        );
    }

    private void setupPhaseTriggers() {
        // Trigger Phase 2 at 70%
        phaseEvents.put(70, boss -> enterPhaseTwo());
        // Trigger Phase 3 at 40%
        phaseEvents.put(40, boss -> enterPhaseThree());
    }

    @Override
    protected void onInitialized() {
        if (this.entity instanceof MagmaCube magmaCube) {
            magmaCube.setSize(6);
        }
        // Real spells don't start here - the intro sequence calls finishIntroduction(),
        // which starts them once the reveal animation ends.
        playIntroduction();
    }

    private void playIntroduction() {
        entity.setAI(false);
        entity.setInvulnerable(true);
        entity.setGravity(false);

        entity.teleport(altarLocation.clone().add(0, 15, 0));

        new BukkitRunnable() {
            int ticks = 0;
            double rotation = 0;

            @Override
            public void run() {
                ticks++;
                Location loc = entity.getLocation();

                // 1. Revolving Particle Effect (Double Helix/Vortex)
                // We calculate two opposite points moving in a circle
                for (int i = 0; i < 2; i++) {
                    double angle = rotation + (i * Math.PI);
                    double x = Math.cos(angle) * 2.5; // Radius of 2.5
                    double z = Math.sin(angle) * 2.5;

                    loc.getWorld().spawnParticle(Particle.ITEM_SLIME, loc.clone().add(x, 0, z), 5, 0.1, 0.1, 0.1, 0.05);
                    loc.getWorld().spawnParticle(Particle.GLOW, loc.clone().add(x, 1, z), 2, 0, 0, 0, 0);
                }
                rotation += 0.2; // Speed of rotation

                // 2. Thematic Dialogue Sequence
                if (ticks == 1) {
                    entity.customName(Component.text("???", NamedTextColor.DARK_RED));
                    broadcastBossMessage(Component.text("Mặt đất bắt đầu rung chuyển, hơi nóng bốc lên dữ dội...", NamedTextColor.RED));
                }
                if (ticks == 40) {
                    broadcastBossMessage(Component.text("Lõi Dung Nham Địa Ngục đang thành hình!", NamedTextColor.RED));
                    // Thay đổi hạt thành Lửa và Khói lớn
                    loc.getWorld().spawnParticle(Particle.FLAME, loc, 50, 1, 1, 1, 0.1);
                    loc.getWorld().spawnParticle(Particle.LAVA, loc, 20, 1, 1, 1, 0);
                    loc.getWorld().playSound(loc, Sound.BLOCK_LAVA_AMBIENT, 2f, 0.5f);
                }

                // End of intro (60 ticks / 3 seconds)
                if (ticks >= 60) {
                    entity.setGravity(true);
                    finishIntroduction();
                    this.cancel();
                }
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0L, 1L);
    }

    private void finishIntroduction() {
        entity.setAI(true);
        entity.setInvulnerable(false);
        entity.setGravity(true);
        entity.customName(Component.text("Ma chất nguyên thủy", NamedTextColor.DARK_RED));

        getParticipants().forEach(player -> player.showTitle(Title.title(Component.text("Ma chất nguyên thủy", NamedTextColor.DARK_RED), Component.text(".......", Style.style(TextDecoration.OBFUSCATED)))));

        // Now we initialize the spells and the BossBar
        spellCasting.setBossBar(new BossBarComponent(entity, 25, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS, true));
        spellCasting.setPhaseManager(new SpellComponent.PhaseManager(phaseEvents, true));

        // Sync the BossBar to the existing tasks
        spellCasting.startSpell(new SpellManager(phase1Actives), phase1Passives, 30, 1, 1);

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SLIME_JUMP, 2f, 0.5f);
        broadcastBossMessage(Component.text("Ma chất nguyên thủy đã thức tỉnh", NamedTextColor.GREEN));

        this.introFinished = true;
    }

    @Override
    public void onHurt(DamageEvent event) {
        // Prevent damage logic during intro
        if (!introFinished) {
            event.setCancelled(true);
            return;
        }
        event.addDamageModifier(1 / defenseScaling, DamageOperation.MORE_FINAL);
        super.onDamage(event);
    }

    public Collection<Player> getParticipants() {
        if (altarLocation == null) {
            return Collections.emptyList();
        }
        Location center = altarLocation.clone();
        center.setY(altarLocation.getY() - 5);
        Hitbox hb = new Hitbox.UprightCylinderHitbox(center, 62, ARENA_SIZE / 2d);
        List<Player> players = hb.getHitPlayers(true);
        defenseScaling = EntityUtils.healthScalingCoef(players.size(), 0.7, 0.65);
        return players;
    }

    private void enterPhaseTwo() {
        this.currentPhase = 2;

        // 1. Khóa Boss tại chỗ
        spellCasting.changePhase(SpellManager.EMPTY, Collections.emptyList(), null);
        entity.setAI(false);
        entity.setInvulnerable(true);
        entity.setGravity(false);

        // 2. Bay lên tâm bệ thờ
        Location transitionLoc = altarLocation.clone().add(0, 15, 0);
        entity.teleport(transitionLoc);

        new BukkitRunnable() {
            int ticks = 0;
            double rotation = 0;

            @Override
            public void run() {
                ticks++;
                Location loc = entity.getLocation();

                // Hiệu ứng hạt xoáy co lại và nén năng lượng
                double radius = Math.max(0.5, 4.0 - (ticks * 0.05));
                for (int i = 0; i < 4; i++) {
                    double angle = rotation + (i * (Math.PI / 2));
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(x, 0.5, z), 3, 0, 0, 0, 0);
                    loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(-x, 1, -z), 3, 0, 0, 0, 0);
                }
                rotation += 0.4;

                // 3. Rung lắc màn hình và âm thanh tăng dần (Build-up)
                if (ticks % 5 == 0 && ticks < 80) {
                    float pitch = 0.5f + (ticks * 0.01f);
                    loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 2f, pitch);

                    // Rung lắc nhẹ cho người chơi xung quanh
                    for (Player p : getParticipants()) {
                        p.playNote(p.getLocation(), Instrument.BASS_DRUM, Note.flat(1, Note.Tone.A));
                    }
                }

                // Chuỗi hội thoại thức tỉnh
                if (ticks == 1) {
                    broadcastBossMessage(Component.text("Năng lượng ma chất đang đạt tới điểm tới hạn...", NamedTextColor.DARK_AQUA, TextDecoration.ITALIC));
                }

                if (ticks == 40) {
                    entity.setGlowing(true); // Bắt đầu phát sáng
                    broadcastBossMessage(Component.text("TA CẢM THẤY... SỰ HUỶ DIỆT ĐANG TRÀO DÂNG!", NamedTextColor.RED, TextDecoration.BOLD));
                    loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.8f);
                }

                // 4. CÚ NỔ THỨC TỈNH (The Awakening Moment)
                if (ticks >= 100) {
                    // Hiệu ứng Sóng xung kích (Shockwave)
                    loc.getWorld().spawnParticle(Particle.FLASH, loc, 5, 0, 0, 0, Color.RED);
                    loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
                    loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.5f);
                    loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SPAWN, 2f, 0.7f);
                    loc.getWorld().playSound(loc, Sound.ITEM_TOTEM_USE, 1f, 0.5f);

                    // Hiệu ứng hạt nổ tung diện rộng
                    loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 200, 2, 2, 2, 0.5);
                    loc.getWorld().spawnParticle(Particle.LAVA, loc, 50, 1, 1, 1, 0.2);

                    // Đẩy lùi người chơi đứng quá gần
                    for (Player p : getParticipants()) {
                        if (p.getLocation().distance(loc) < 15) {
                            Vector knockback = p.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(2).setY(0.8);
                            MovementUtils.knockAwayDirection(knockback, p, 0, true);
                            p.sendMessage(Component.text("Sóng xung kích từ sự thức tỉnh hất văng bạn!", NamedTextColor.GRAY));
                        }
                    }

                    // Trả lại trạng thái chiến đấu
                    entity.setAI(true);
                    entity.setInvulnerable(false);
                    entity.setGravity(true);
                    entity.setGlowing(false);

                    entity.customName(Component.text("Ma chất nguyên thủy - THỨC TỈNH", NamedTextColor.DARK_RED, TextDecoration.BOLD));

                    applyPhaseTwoSpells();
                    this.cancel();
                }
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0L, 1L);
    }

    private void applyPhaseTwoSpells() {
        // Cập nhật SpellManager mới cho Phase 2 tại đây
        // Ví dụ: Tăng tốc độ hoặc thêm kỹ năng mới
        spellCasting.changePhase(new SpellManager(phase2Active), phase2Passive, null);
        spellCasting.forceCastSpell(DoomTotemSpell.class);
    }

    private void enterPhaseThree() {
        this.currentPhase = 3;

        // 1. Khóa Boss hoàn toàn
        spellCasting.changePhase(SpellManager.EMPTY, Collections.emptyList(), null);
        entity.setAI(false);
        entity.setInvulnerable(true);
        entity.setGravity(false);

        // 2. Kéo Boss về tâm bệ thờ và hạ thấp xuống sát đất (như đang tan chảy)
        Location midLoc = altarLocation.clone().add(0, 1, 0);
        entity.teleport(midLoc);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;
                Location loc = entity.getLocation();

                // --- HIỆU ỨNG TAN CHẢY (Melting Effect) ---
                if (ticks < 60) {
                    // Tạo các hạt ma chất và khói đen bốc lên từ chân Boss
                    loc.getWorld().spawnParticle(Particle.DRIPPING_LAVA, loc, 10, 1, 0.5, 1, 0.1);
                    loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 5, 0.8, 0.2, 0.8, 0.05);

                    if (ticks % 10 == 0) {
                        loc.getWorld().playSound(loc, Sound.BLOCK_LAVA_EXTINGUISH, 2f, 0.5f);
                        broadcastBossMessage(Component.text("Cơ thể của Ma Chất đang tan rã...", NamedTextColor.DARK_PURPLE));
                    }
                }

                // --- TRẠNG THÁI QUÁ TẢI (Overload) ---
                if (ticks >= 60 && ticks < 100) {
                    // Boss bắt đầu rung lắc dữ dội
                    double offset = (ticks % 2 == 0) ? 0.1 : -0.1;
                    entity.teleport(loc.clone().add(offset, 0, offset));

                    loc.getWorld().spawnParticle(Particle.WITCH, loc, 20, 1.5, 1.5, 1.5, 0.2);
                    loc.getWorld().spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, Color.RED);

                    if (ticks == 60) {
                        broadcastBossMessage(Component.text("TA... KHÔNG THỂ... KIỀM CHẾ NÓ THÊM NỮA!", NamedTextColor.DARK_RED, TextDecoration.BOLD));
                        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_HURT, 2f, 0.5f);
                    }
                }

                // --- CÚ NỔ GIẢI PHÓNG (The Final Release) ---
                if (ticks == 100) {
                    // Hiệu ứng Visual cực đại
                    loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 10, 3, 1, 3, 0);
                    loc.getWorld().spawnParticle(Particle.SCULK_SOUL, loc, 100, 5, 2, 5, 0.2);

                    // Âm thanh chấn động
                    loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 3f, 0.5f);
                    loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_DEATH, 2f, 0.5f);
                    loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 2f, 0.5f);

                    // Gây hiệu ứng mù tạm thời và đẩy lùi cho tất cả người chơi
                    for (Player p : getParticipants()) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1));
                        p.sendMessage(Component.text("MA CHẤT ĐÃ CHIẾM HỮU TOÀN BỘ ĐẤU TRƯỜNG!", NamedTextColor.RED, TextDecoration.BOLD));

                        Vector push = p.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(2.5).setY(0.5);
                        MovementUtils.knockAwayDirection(push, p, 0);
                    }

                    // Cập nhật trạng thái Boss
                    entity.setAI(true);
                    entity.setInvulnerable(false);
                    entity.setGravity(true);
                    entity.setGlowing(true); // Phase 3 luôn phát sáng

                    entity.customName(Component.text("KỊCH ĐỘC - MA CHẤT VÔ DẠNG", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));

                    applyPhaseThreeSpells();
                    this.cancel();
                }
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0L, 1L);
    }

    private void applyPhaseThreeSpells() {

        spellCasting.changePhase(new SpellManager(phase3Active), phase3Passive, null);
        spellCasting.forceCastSpell(MeteorRainSpell.class);
    }

    public void broadcastBossMessage(Component text) {
        for (Player p : getParticipants()) {
            p.sendMessage(text);
        }
    }

    private void spawnMinion() {
        // Logic to spawn small slimes via EntityManager
    }
}
