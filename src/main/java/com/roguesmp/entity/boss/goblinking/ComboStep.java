package com.roguesmp.entity.boss.goblinking;

import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.spell.common.SpellTargets;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class ComboStep {

    public enum StepKind {
        MAGIC(BossBar.Color.PURPLE, NamedTextColor.LIGHT_PURPLE),
        MELEE(BossBar.Color.RED, NamedTextColor.RED),
        AERIAL(BossBar.Color.YELLOW, NamedTextColor.GOLD),
        AREA(BossBar.Color.GREEN, NamedTextColor.GREEN);

        private final BossBar.Color barColor;
        private final NamedTextColor textColor;

        StepKind(BossBar.Color barColor, NamedTextColor textColor) {
            this.barColor = barColor;
            this.textColor = textColor;
        }

        public BossBar.Color barColor() {
            return barColor;
        }

        public NamedTextColor textColor() {
            return textColor;
        }
    }

    @FunctionalInterface
    public interface Aim {

        Aim TARGET = ctx -> location(ctx.aimTarget());
        Aim MARKED = ctx -> location(ctx.marked());
        Aim HOME = KingContext::home;
        Aim PRISON = ctx -> {
            RunePrison prison = ctx.prison();
            return prison != null ? prison.center() : location(ctx.aimTarget());
        };

        @Nullable Location resolve(KingContext ctx);

        private static @Nullable Location location(@Nullable Player player) {
            return player == null ? null : player.getLocation();
        }
    }

    @FunctionalInterface
    public interface Volley {

        void tick(KingContext ctx, int elapsed);

        static Volley none() {
            return (ctx, elapsed) -> {
            };
        }

        static Volley fireSpiral(int interval, double speed, String id) {
            return (ctx, elapsed) -> {
                if (elapsed % interval != 0) return;
                Location from = ctx.owner().getLocation().add(0, 1.2, 0);
                double angle = elapsed * 0.5;
                for (int arm = 0; arm < 2; arm++) {
                    double a = angle + Math.PI * arm;
                    Vector velocity = new Vector(Math.cos(a), 0, Math.sin(a)).multiply(speed).setY(0.05);
                    ctx.addHazard(new Bolt(ctx, from, velocity, null, 0, 0.012, 80, 0.6, 1.4, Particle.FLAME, DamageType.FIRE, id));
                }
                ctx.owner().getWorld().playSound(from, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.2f);
            };
        }

        static Volley arcaneBolts(int count, int interval, String id) {
            return (ctx, elapsed) -> {
                if (elapsed % interval != 0 || elapsed / interval >= count) return;
                Player target = ctx.aimTarget();
                if (target == null) return;
                Location from = ctx.owner().getEyeLocation();
                Vector velocity = target.getLocation().add(0, 1, 0).toVector().subtract(from.toVector()).normalize().multiply(0.45).setY(0.35);
                ctx.addHazard(new Bolt(ctx, from, velocity, target, 0.08, 0, 120, 0.9, 1.2, Particle.WITCH, DamageType.MAGIC, id));
                ctx.owner().getWorld().playSound(from, Sound.ENTITY_EVOKER_CAST_SPELL, 1.2f, 1.4f);
            };
        }

        static Volley fireball(String id) {
            return (ctx, elapsed) -> {
                if (elapsed != 0) return;
                Player target = ctx.aimTarget();
                if (target == null) return;
                Location from = ctx.owner().getEyeLocation();
                Vector velocity = target.getLocation().add(0, 0.5, 0).toVector().subtract(from.toVector()).normalize().multiply(0.7);
                ctx.addHazard(new Bolt(ctx, from, velocity, null, 0, 0, 80, 1.3, 3.0, Particle.FLAME, DamageType.FIRE, id));
                ctx.owner().getWorld().playSound(from, Sound.ENTITY_GHAST_SHOOT, 1.5f, 0.7f);
            };
        }

        static Volley gather() {
            return (ctx, elapsed) -> {
                Location center = ctx.owner().getLocation().add(0, 1, 0);
                for (int i = 0; i < 6; i++) {
                    double angle = elapsed * 0.3 + Math.PI * 2 * i / 6;
                    Location point = center.clone().add(Math.cos(angle) * 2.5, Math.sin(elapsed * 0.2 + i) * 0.5, Math.sin(angle) * 2.5);
                    center.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, Fx.FEL);
                }
                center.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, center, 3, 0.4, 0.4, 0.4, 0);
                if (elapsed % 10 == 0) center.getWorld().playSound(center, Sound.BLOCK_BEACON_AMBIENT, 2f, 0.5f + elapsed * 0.01f);
            };
        }
    }

    @FunctionalInterface
    public interface Impact {

        int FIRE_DAMAGE_INTERVAL = 10;
        double FIRE_DAMAGE_MULTIPLIER = 0.3;

        void land(KingContext ctx, Location at);

        default Impact and(Impact next) {
            return (ctx, at) -> {
                land(ctx, at);
                next.land(ctx, at);
            };
        }

        static Impact slam(double radius, double multiplier, String id) {
            return (ctx, at) -> {
                at.getWorld().spawnParticle(Particle.EXPLOSION, at, 3, 1, 0.1, 1, 0);
                at.getWorld().spawnParticle(Particle.BLOCK, at, 80, radius * 0.4, 0.2, radius * 0.4, 0.2, Material.STONE.createBlockData());
                at.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
                at.getWorld().playSound(at, Sound.BLOCK_ANVIL_LAND, 1.2f, 0.5f);
                for (Player player : ctx.players()) {
                    if (player.getLocation().distanceSquared(at) > radius * radius) continue;
                    ctx.damage(player, multiplier, DamageType.BLAST, id);
                    ctx.push(player, at, 0.7, 0.5);
                }
            };
        }

        static Impact shockwave(double radius, double multiplier, Particle.DustOptions dust, String id) {
            return shockwave(radius, multiplier, dust, id, 0);
        }

        static Impact shockwave(double radius, double multiplier, Particle.DustOptions dust, String id, int delay) {
            return (ctx, at) -> ctx.addHazard(new Shockwave(ctx, at, radius, multiplier, dust, id, delay));
        }

        static Impact fireRing(double inner, double outer, int duration, String id) {
            return (ctx, at) -> {
                Location center = at.clone();
                at.getWorld().playSound(at, Sound.ITEM_FIRECHARGE_USE, 1.5f, 0.6f);
                ctx.addHazard(Hazard.timed(duration, age -> {
                    for (int i = 0; i < 20; i++) {
                        double angle = Utils.RANDOM.nextDouble() * Math.PI * 2;
                        double radius = inner + Utils.RANDOM.nextDouble() * (outer - inner);
                        center.getWorld().spawnParticle(Particle.FLAME, center.getX() + Math.cos(angle) * radius, center.getY() + 0.15,
                                center.getZ() + Math.sin(angle) * radius, 1, 0, 0.05, 0, 0.01);
                    }
                    if (age % FIRE_DAMAGE_INTERVAL != 0) return false;
                    for (Player player : ctx.players()) {
                        double distance = SpellTargets.horizontalDistance(player.getLocation(), center);
                        if (distance < inner || distance > outer || Math.abs(player.getY() - center.getY()) > 1.5) continue;
                        ctx.damage(player, FIRE_DAMAGE_MULTIPLIER, DamageType.FIRE, id);
                        player.setFireTicks(Math.max(player.getFireTicks(), 40));
                    }
                    return false;
                }));
            };
        }

        static Impact eruptRunes() {
            return (ctx, at) -> List.copyOf(ctx.runes()).forEach(GroundRune::detonate);
        }
    }

    private final String title;
    private final StepKind kind;
    private final int windup;

    protected ComboStep(String title, StepKind kind, int windup) {
        this.title = title;
        this.kind = kind;
        this.windup = windup;
    }

    public final String title() {
        return title;
    }

    public final StepKind kind() {
        return kind;
    }

    public final int windup() {
        return windup;
    }

    public void onWindupStart(KingContext ctx) {

    }

    public void onWindupTick(KingContext ctx, int elapsed) {

    }

    public abstract void onExecute(KingContext ctx);

    public boolean tickActive(KingContext ctx, int elapsed) {
        return true;
    }

    public void onCancel(KingContext ctx) {

    }
}
