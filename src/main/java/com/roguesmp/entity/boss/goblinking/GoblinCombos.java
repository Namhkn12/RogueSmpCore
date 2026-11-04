package com.roguesmp.entity.boss.goblinking;

import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.boss.goblinking.ComboDirector.ComboSpell;
import com.roguesmp.entity.boss.goblinking.ComboStep.Aim;
import com.roguesmp.entity.boss.goblinking.ComboStep.Impact;
import com.roguesmp.entity.boss.goblinking.ComboStep.StepKind;
import com.roguesmp.entity.boss.goblinking.ComboStep.Volley;
import com.roguesmp.entity.spell.common.SpellTargets;
import com.roguesmp.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public final class GoblinCombos {

    private static final String ID = "goblin_king_";
    private static final double BLOOD_BURST_RADIUS = 4.0;
    private static final double DECREE_RADIUS = 20;
    private static final double DECREE_MULTIPLIER = 1.5;
    private static final int DECREE_ROOT_TICKS = 30;
    private static final int DECREE_LINGER = 6;
    private static final int HURL_FOLLOW_THROUGH = 12;
    private static final double HURL_TELEGRAPH = 14;

    private final ComboDirector director;

    public GoblinCombos(ComboDirector director) {
        this.director = director;
    }

    public List<ComboSpell> phaseOne() {
        return List.of(crimsonPursuit(), kingsHunt(), axeAndEmber(), buriedAlive(), wickedReturn());
    }

    public List<ComboSpell> phaseTwo() {
        return List.of(infernalWhirlwind(), earthbreaker(), arcaneGuillotine(), moltenReign(), kingsDecree(),
                lavaDance(), flameReversal(), shatteringPursuit());
    }

    public List<ComboSpell> phaseThree() {
        return List.of(royalExecution(), thunderousCrown(), cataclysmicReign(), finalDecree(), crownBreaker(),
                kingsDecree(), moltenReign());
    }

    private ComboSpell crimsonPursuit() {
        return director.major(() -> List.of(
                markPlayer("Huyết Ấn", 30, KingContext::aimTarget),
                new ChargeStep("Vương Xung", 20, Aim.MARKED, 0.9, 16, 1.4, 1.0, ID + "royal_charge"),
                new ActionStep("Huyết Bạo", StepKind.MAGIC, 25, 10, GoblinCombos::bloodBurst)
        ));
    }

    private ComboSpell infernalWhirlwind() {
        return director.major(() -> List.of(
                new AxeWhirlStep("Toàn Phong Phủ", 20, 50, 4.5, 0.9, ID + "axe_whirl"),
                new AscendStep("Hỏa Thăng Thiên", StepKind.AERIAL, 10, 6, 12, 40, Volley.fireSpiral(4, 0.35, ID + "flame_ascension")),
                new PlungeStep("Dung Nham Giáng", 12, Aim.TARGET, 0, 14,
                        Impact.slam(3, 1.4, ID + "lava_crash").and(Impact.fireRing(2.5, 6, 80, ID + "lava_crash")))
        ));
    }

    private ComboSpell earthbreaker() {
        return director.major(() -> List.of(
                new MarkGroundStep("Địa Chấn Ấn", StepKind.MAGIC, 30, GroundRune.Style.ROCK, 8, false, ID + "rockfall"),
                new ChargeStep("Vương Giả Xung Phong", 20, Aim.TARGET, 1.0, 22, 1.4, 1.2, ID + "kings_charge"),
                erupt("Thạch Thứ Bộc Phát", 15)
        ));
    }

    private ComboSpell royalExecution() {
        return director.major(() -> List.of(
                royalCleave(),
                arcaneAscension(),
                meteorFist(Aim.TARGET, 8, 10)
        ));
    }

    private ComboSpell kingsDecree() {
        return director.major(() -> List.of(
                royalDecree(40),
                hurl("Phi Phủ", 20, Aim.MARKED, false, 1.2, ID + "royal_hurl"),
                new ShadowLeapStep("Ảnh Bộ", 25, 1.5, ID + "shadow_leap")
        ));
    }

    private ComboSpell moltenReign() {
        return director.major(() -> List.of(
                magmaCracks(7),
                royalDive(Impact.slam(3, 1.4, ID + "royal_dive")),
                erupt("Dung Nham Bộc Phát", 0)
        ));
    }

    private ComboSpell arcaneGuillotine() {
        return director.major(() -> List.of(
                runePrison(25, 140),
                new CleaveStep("Đao Phủ Trảm", 30, Aim.PRISON, 8, 14, 2.6, ID + "executioners_swing"),
                new ActionStep("Huyền Thuật Bạo Phá", StepKind.MAGIC, 15, 10, ctx -> {
                    RunePrison prison = ctx.prison();
                    if (prison != null) prison.detonate();
                })
        ));
    }

    private ComboSpell thunderousCrown() {
        return director.major(() -> List.of(
                new ChargeStep("Vương Miện Húc", 15, Aim.TARGET, 0.8, 9, 1.2, 1.6, ID + "crown_bash"),
                new MarkGroundStep("Lôi Phù", StepKind.MAGIC, 30, GroundRune.Style.LIGHTNING, 6, true, ID + "thunder_rune"),
                new PlungeStep("Thiên Giáng", 10, Aim.TARGET, 6, 16,
                        Impact.slam(3, 1.3, ID + "skyfall").and(Impact.shockwave(10, 1.0, Fx.STORM, ID + "skyfall")))
        ));
    }

    private ComboSpell kingsHunt() {
        return director.major(90, () -> List.of(
                markPlayer("Liệp Ấn", 30, KingContext::farthest),
                new PlungeStep("Sao Chổi Yêu Tinh", 12, Aim.MARKED, 3, 14, Impact.slam(2.5, 1.2, ID + "goblin_comet")),
                new CleaveStep("Cuồng Phủ Liên Trảm", 10, Aim.MARKED, 4, 55, 0.9, 3, 8, 5, ID + "savage_flurry")
        ));
    }

    private ComboSpell cataclysmicReign() {
        return director.major(80, () -> List.of(
                new AscendStep("Hắc Ám Thăng Thiên", StepKind.MAGIC, 15, 7, 14, 30, Volley.gather()),
                hurl("Phi Phủ", 15, Aim.TARGET, false, 1.2, ID + "royal_hurl"),
                new MarkGroundStep("Dung Nham Phún Trào", StepKind.AREA, 30, GroundRune.Style.LAVA, 0, true, ID + "lava_eruption"),
                meteorFist(Aim.HOME, 20, 18)
        ));
    }

    private ComboSpell axeAndEmber() {
        return director.minor(() -> List.of(
                royalCleave(),
                AscendStep.grounded("Hỏa Cầu", 15, 10, Volley.fireball(ID + "fireball")),
                new PlungeStep("Vương Giả Phi Thân", 12, Aim.TARGET, 4, 14,
                        Impact.slam(3, 1.1, ID + "kings_leap").and(Impact.shockwave(6, 0.7, Fx.WARNING, ID + "kings_leap")))
        ));
    }

    private ComboSpell buriedAlive() {
        return director.minor(() -> List.of(
                new MarkGroundStep("Địa Chấn Ấn", StepKind.MAGIC, 25, GroundRune.Style.ROCK, 6, false, ID + "rockfall"),
                new ActionStep("Địa Chấn Kích", StepKind.AREA, 15, 10, ctx -> {
                    ctx.owner().swingMainHand();
                    Impact.slam(3.5, 1.2, ID + "ground_slam")
                            .and(Impact.shockwave(8, 0.8, Fx.EARTH, ID + "ground_slam"))
                            .land(ctx, ctx.owner().getLocation());
                }),
                erupt("Thạch Thứ Bộc Phát", 12)
        ));
    }

    private ComboSpell wickedReturn() {
        return director.minor(() -> List.of(
                hurl("Hồi Phủ", 18, Aim.TARGET, true, 1.0, ID + "axe_boomerang"),
                new ShadowLeapStep("Liệt Không Bộ", 22, 1.2, ID + "riftstep"),
                royalCleave()
        ));
    }

    private ComboSpell flameReversal() {
        return director.minor(() -> List.of(
                flameSpiral(15),
                gust("Vương Phong", 15, 7, 50),
                new ChargeStep("Vương Giả Xung Phong", 18, Aim.TARGET, 1.0, 20, 1.3, 1.2, ID + "kings_charge")
        ));
    }

    private ComboSpell crownBreaker() {
        return director.minor(() -> List.of(
                hurl("Phi Phủ", 18, Aim.TARGET, false, 1.2, ID + "royal_hurl"),
                arcaneAscension(),
                new PlungeStep("Bổ Nhào", 10, Aim.TARGET, 0, 10, Impact.slam(3, 1.3, ID + "dive_attack"))
        ));
    }

    private ComboSpell lavaDance() {
        return director.minor(() -> List.of(
                magmaCracks(6),
                new AxeWhirlStep("Toàn Phong Phủ", 15, 40, 4.5, 0.9, ID + "axe_whirl"),
                royalDive(Impact.slam(3, 1.3, ID + "royal_dive").and(Impact.eruptRunes()))
        ));
    }

    private ComboSpell shatteringPursuit() {
        return director.minor(() -> List.of(
                new ChargeStep("Vương Giả Xung Phong", 18, Aim.TARGET, 1.0, 20, 1.3, 1.2, ID + "kings_charge"),
                runePrison(20, 60),
                new MarkGroundStep("Địa Chấn Ấn", StepKind.MAGIC, 20, GroundRune.Style.ROCK, 6, false, ID + "rockfall"),
                erupt("Thạch Thứ Bộc Phát", 25)
        ));
    }

    private ComboSpell finalDecree() {
        return director.minor(() -> List.of(
                royalCleave(),
                meteorFist(Aim.TARGET, 10, 12),
                flameSpiral(12)
        ));
    }

    private static CleaveStep royalCleave() {
        return new CleaveStep("Vương Phủ Trảm", 22, Aim.TARGET, 5.5, 60, 1.8, ID + "royal_cleave");
    }

    private static AscendStep arcaneAscension() {
        return new AscendStep("Huyền Thuật Thăng Không", StepKind.MAGIC, 10, 5, 10, 40, Volley.arcaneBolts(3, 12, ID + "arcane_bolt"));
    }

    private static AscendStep flameSpiral(int windup) {
        return AscendStep.grounded("Hỏa Xoáy", windup, 30, Volley.fireSpiral(4, 0.3, ID + "flame_spiral"));
    }

    private static MarkGroundStep magmaCracks(int count) {
        return new MarkGroundStep("Dung Nham Liệt Địa", StepKind.MAGIC, 25, GroundRune.Style.MAGMA, count, false, ID + "magma_burst");
    }

    private static PlungeStep royalDive(Impact impact) {
        return new PlungeStep("Vương Giả Bổ Nhào", 15, Aim.TARGET, 5, 16, impact);
    }

    private static PlungeStep meteorFist(Aim aim, int windup, double shockwaveRadius) {
        return new PlungeStep("Lưu Tinh Quyền", windup, aim, 6, 16,
                Impact.slam(3.5, 1.6, ID + "meteor_fist")
                        .and(Impact.shockwave(shockwaveRadius, 1.1, Fx.ARCANE, ID + "meteor_fist"))
                        .and(Impact.shockwave(shockwaveRadius, 0.8, Fx.WARNING, ID + "meteor_fist", 12)));
    }

    private static ActionStep erupt(String title, int windup) {
        return new ActionStep(title, StepKind.AREA, windup, 10, ctx -> Impact.eruptRunes().land(ctx, ctx.owner().getLocation()));
    }

    private static ActionStep markPlayer(String title, int windup, Function<KingContext, @Nullable Player> selector) {
        return new ActionStep(title, StepKind.MAGIC, windup, 0)
                .onStart((ctx, step, elapsed) -> {
                    step.subject(selector.apply(ctx));
                    ctx.owner().swingOffHand();
                    ctx.owner().getWorld().playSound(ctx.owner().getLocation(), Sound.ENTITY_EVOKER_PREPARE_ATTACK, 1.5f, 0.8f);
                })
                .onWindup((ctx, step, elapsed) -> {
                    ctx.owner().getWorld().spawnParticle(Particle.DUST, ctx.owner().getLocation().add(0, 2.2, 0), 6, 0.3, 0.3, 0.3, 0, Fx.BLOOD);
                    Player chosen = step.subject();
                    if (chosen == null) return;
                    ctx.face(chosen.getLocation());
                    Fx.ring(chosen.getLocation(), 3.0 - 1.8 * elapsed / Math.max(1, step.windup()), Fx.BLOOD);
                })
                .onFire((ctx, step, elapsed) -> {
                    Player chosen = step.subject() != null ? step.subject() : selector.apply(ctx);
                    if (chosen == null) return;
                    ctx.mark(chosen, Fx.BLOOD);
                    chosen.getWorld().playSound(chosen.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 1.6f);
                    chosen.getWorld().spawnParticle(Particle.DUST, chosen.getLocation().add(0, 1, 0), 30, 0.4, 0.8, 0.4, 0, Fx.BLOOD);
                });
    }

    private static ActionStep royalDecree(int windup) {
        return new ActionStep("Vương Lệnh: Quỳ Xuống!", StepKind.MAGIC, windup, DECREE_LINGER)
                .onStart((ctx, step, elapsed) -> {
                    ctx.brace(true);
                    ctx.owner().swingMainHand();
                    ctx.owner().getWorld().playSound(ctx.owner().getLocation(), Sound.ENTITY_RAVAGER_ROAR, 2f, 0.8f);
                    ctx.owner().getWorld().playSound(ctx.owner().getLocation(), Sound.BLOCK_BELL_USE, 2f, 0.6f);
                })
                .onWindup((ctx, step, elapsed) -> {
                    Location center = ctx.onFloor(ctx.owner().getLocation());
                    double progress = (double) elapsed / Math.max(1, step.windup());
                    Fx.ring(center, DECREE_RADIUS * progress, Fx.ROYAL);
                    ctx.owner().getWorld().spawnParticle(Particle.DUST, ctx.owner().getLocation().add(0, ctx.owner().getHeight() + 0.8, 0),
                            6, 0.3, 0.3, 0.3, 0, Fx.ROYAL);
                    if (elapsed % 10 == 0) center.getWorld().playSound(center, Sound.BLOCK_BELL_RESONATE, 1.5f, 0.6f + (float) progress);
                })
                .onFire((ctx, step, elapsed) -> {
                    ctx.brace(false);
                    Location center = ctx.onFloor(ctx.owner().getLocation());
                    center.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 1.4f);
                    center.getWorld().playSound(center, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.5f);
                    Fx.ring(center, DECREE_RADIUS, Fx.ROYAL);

                    List<Player> defiant = ctx.players().stream()
                            .filter(player -> !player.isSneaking())
                            .filter(player -> player.getLocation().distanceSquared(center) <= DECREE_RADIUS * DECREE_RADIUS)
                            .toList();
                    for (Player player : defiant) {
                        ctx.damage(player, DECREE_MULTIPLIER, DamageType.MAGIC, ID + "royal_decree");
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, DECREE_ROOT_TICKS, 6, false, true));
                        player.setVelocity(new Vector(0, -0.5, 0));
                        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 25, 0.3, 0.6, 0.3, 0, Fx.ROYAL);
                    }
                    if (!defiant.isEmpty()) ctx.mark(defiant.get(Utils.RANDOM.nextInt(defiant.size())), Fx.ROYAL);
                });
    }

    private static ActionStep hurl(String title, int windup, Aim aim, boolean curved, double multiplier, String id) {
        return new ActionStep(title, StepKind.MELEE, windup, HURL_FOLLOW_THROUGH)
                .onStart((ctx, step, elapsed) ->
                        ctx.owner().getWorld().playSound(ctx.owner().getLocation(), Sound.ITEM_TRIDENT_THROW, 1.2f, 0.5f))
                .onWindup((ctx, step, elapsed) -> {
                    Location target = aim.resolve(ctx);
                    if (target == null) return;
                    ctx.face(target);
                    Fx.line(ctx.onFloor(ctx.owner().getLocation()), SpellTargets.horizontalDirection(ctx.owner().getLocation(), target),
                            HURL_TELEGRAPH, Fx.WARNING);
                })
                .onFire((ctx, step, elapsed) -> {
                    Location target = aim.resolve(ctx);
                    if (target == null) return;
                    ctx.owner().swingMainHand();
                    ctx.addHazard(new ThrownAxe(ctx, target, curved, multiplier, id));
                });
    }

    private static ActionStep runePrison(int windup, int fuse) {
        return new ActionStep("Phù Văn Lao Ngục", StepKind.MAGIC, windup, 0)
                .onStart((ctx, step, elapsed) -> {
                    step.subject(ctx.aimTarget());
                    ctx.owner().getWorld().playSound(ctx.owner().getLocation(), Sound.ENTITY_EVOKER_PREPARE_WOLOLO, 1.5f, 0.7f);
                })
                .onWindup((ctx, step, elapsed) -> {
                    Player victim = step.subject();
                    if (victim == null) return;
                    ctx.face(victim.getLocation());
                    Fx.ring(victim.getLocation(), 4.0 - 1.5 * elapsed / Math.max(1, step.windup()), Fx.ARCANE);
                })
                .onFire((ctx, step, elapsed) -> {
                    Player victim = step.subject() != null ? step.subject() : ctx.aimTarget();
                    if (victim == null) return;
                    ctx.owner().swingOffHand();
                    ctx.setPrison(new RunePrison(ctx, ctx.onFloor(victim.getLocation()), fuse, ID + "arcane_detonation"));
                });
    }

    private static ActionStep gust(String title, int windup, double range, double halfAngleDegrees) {
        double halfAngle = Math.toRadians(halfAngleDegrees);
        Vector[] facing = {new Vector(1, 0, 0)};
        return new ActionStep(title, StepKind.MAGIC, windup, 0)
                .onWindup((ctx, step, elapsed) -> {
                    Location target = Aim.TARGET.resolve(ctx);
                    if (target != null) {
                        facing[0] = SpellTargets.horizontalDirection(ctx.owner().getLocation(), target);
                        ctx.face(target);
                    }
                    Fx.cone(ctx.owner().getLocation(), facing[0], range, halfAngle, Fx.STORM);
                })
                .onFire((ctx, step, elapsed) -> {
                    Location origin = ctx.owner().getLocation();
                    ctx.owner().swingMainHand();
                    origin.getWorld().playSound(origin, Sound.ENTITY_BREEZE_WIND_BURST, 2f, 0.6f);
                    for (double r = 1; r <= range; r += 1) {
                        origin.getWorld().spawnParticle(Particle.GUST, origin.clone().add(facing[0].clone().multiply(r)).add(0, 1, 0), 1, 0.3, 0.3, 0.3, 0);
                    }
                    for (Player player : ctx.players()) {
                        Vector offset = player.getLocation().toVector().subtract(origin.toVector()).setY(0);
                        if (offset.lengthSquared() > range * range) continue;
                        if (offset.lengthSquared() > 1 && offset.angle(facing[0]) > halfAngle) continue;
                        ctx.damage(player, 0.4, DamageType.MAGIC, ID + "royal_gust");
                        ctx.push(player, origin, 1.6, 0.4);
                    }
                });
    }

    private static void bloodBurst(KingContext ctx) {
        Player marked = ctx.consumeMark();
        if (marked == null) return;
        Location center = marked.getLocation();

        center.getWorld().spawnParticle(Particle.DUST, center.clone().add(0, 1, 0), 80, BLOOD_BURST_RADIUS * 0.4, 0.8, BLOOD_BURST_RADIUS * 0.4, 0, Fx.BLOOD);
        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 2, 0.5, 0.3, 0.5, 0);
        center.getWorld().playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 2f, 0.6f);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.2f);
        Fx.ring(center, BLOOD_BURST_RADIUS, Fx.BLOOD);

        for (Player player : ctx.players()) {
            if (player.getLocation().distanceSquared(center) > BLOOD_BURST_RADIUS * BLOOD_BURST_RADIUS) continue;
            ctx.damage(player, player == marked ? 0.6 : 1.6, DamageType.MAGIC, ID + "blood_burst");
            if (player != marked) ctx.push(player, center, 0.6, 0.3);
        }
    }
}
