package com.roguesmp.entity.boss.goblinking;

import com.roguesmp.entity.spell.ActiveSpell;
import com.roguesmp.entity.spell.ChargeUpManager;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellContext;
import com.roguesmp.event.DamageEvent;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ComboDirector extends Spell {

    public static final class ComboSpell extends ActiveSpell {

        private static final int MAJOR_COOLDOWN = 300;
        private static final int MINOR_COOLDOWN = 140;
        private static final int MAJOR_RECOVERY = 60;
        private static final int MINOR_RECOVERY = 30;
        private static final int MAJOR_WEIGHT = 3;
        private static final int MINOR_WEIGHT = 2;

        private final ComboDirector director;
        private final Supplier<List<ComboStep>> steps;
        private final boolean major;
        private final int recovery;

        private ComboSpell(ComboDirector director, boolean major, int recovery, Supplier<List<ComboStep>> steps) {
            super(major ? MAJOR_COOLDOWN : MINOR_COOLDOWN, 0, 1);
            this.director = director;
            this.major = major;
            this.recovery = recovery;
            this.steps = steps;
        }

        @Override
        public int weight() {
            return major ? MAJOR_WEIGHT : MINOR_WEIGHT;
        }

        @Override
        public boolean canCast(SpellContext context) {
            return !director.isBusy();
        }

        @Override
        public void onCast(SpellContext context) {
            director.start(steps.get(), recovery, context.target());
        }
    }

    private static final Particle.DustOptions CROWN = new Particle.DustOptions(Color.fromRGB(255, 205, 40), 0.9f);
    private static final int CROWN_POINTS = 6;
    private static final double CROWN_RADIUS = 0.3;

    private final LivingEntity owner;
    private final ChargeUpManager bar;
    private final List<Hazard> hazards = new ArrayList<>();
    private final List<Hazard> pending = new ArrayList<>();

    private @Nullable KingContext run;
    private List<ComboStep> steps = List.of();
    private int index;
    private int elapsed;
    private boolean executing;
    private int recovery;
    private long tick;
    private long busyUntil;

    public ComboDirector(LivingEntity owner) {
        this.owner = owner;
        this.bar = ChargeUpManager.around(owner, KingContext.ARENA_RANGE, 1, Component.empty(), BossBar.Color.RED, BossBar.Overlay.PROGRESS);
    }

    public ComboSpell major(Supplier<List<ComboStep>> steps) {
        return new ComboSpell(this, true, ComboSpell.MAJOR_RECOVERY, steps);
    }

    public ComboSpell major(int recovery, Supplier<List<ComboStep>> steps) {
        return new ComboSpell(this, true, recovery, steps);
    }

    public ComboSpell minor(Supplier<List<ComboStep>> steps) {
        return new ComboSpell(this, false, ComboSpell.MINOR_RECOVERY, steps);
    }

    public boolean isBusy() {
        return run != null || tick < busyUntil;
    }

    public void start(List<ComboStep> combo, int recoveryTicks, @Nullable Player target) {
        if (run != null) cancel();
        run = new KingContext(this, owner, target);
        steps = combo;
        recovery = recoveryTicks;
        index = 0;
        beginStep();
    }

    void addHazard(Hazard hazard) {
        pending.add(hazard);
    }

    @Override
    public void tick(SpellContext context, int interval) {
        tick = context.tick();
        drawCrown();
        hazards.addAll(pending);
        pending.clear();
        hazards.removeIf(hazard -> hazard.tick(interval));

        if (run == null) return;
        if (!owner.isValid() || owner.isDead()) {
            cancel();
            return;
        }
        if (run.isHovering()) owner.setVelocity(new Vector());
        if (run.isRooted()) owner.setVelocity(new Vector(0, Math.min(0, owner.getVelocity().getY()), 0));

        ComboStep step = steps.get(index);
        elapsed += interval;
        if (!executing) {
            if (elapsed >= step.windup()) {
                bar.reset();
                executing = true;
                elapsed = 0;
                step.onExecute(run);
            } else {
                bar.nextTick(interval);
                step.onWindupTick(run, elapsed);
            }
        } else if (step.tickActive(run, elapsed)) {
            index++;
            if (index >= steps.size()) {
                finish();
            } else {
                beginStep();
            }
        }
    }

    private void beginStep() {
        ComboStep step = steps.get(index);
        elapsed = 0;
        executing = false;
        step.onWindupStart(run);
        if (step.windup() > 0) {
            bar.setChargeTime(step.windup());
            bar.show(Component.text(step.title(), step.kind().textColor(), TextDecoration.BOLD), 0f, step.kind().barColor());
        }
    }

    private void finish() {
        bar.reset();
        run.end();
        run = null;
        busyUntil = tick + recovery;
    }

    private void cancel() {
        if (run == null) return;
        steps.get(index).onCancel(run);
        finish();
    }

    private void drawCrown() {
        if (!owner.isValid()) return;
        Location top = owner.getLocation().add(0, owner.getHeight() + 0.25, 0);
        double offset = tick * 0.15;
        for (int i = 0; i < CROWN_POINTS; i++) {
            double angle = offset + Math.PI * 2 * i / CROWN_POINTS;
            owner.getWorld().spawnParticle(Particle.DUST, top.clone().add(Math.cos(angle) * CROWN_RADIUS, 0, Math.sin(angle) * CROWN_RADIUS), 1, 0, 0, 0, 0, CROWN);
        }
        if (tick % 10 == 0) owner.getWorld().spawnParticle(Particle.WAX_ON, top, 1, 0.15, 0.05, 0.15, 0);
    }

    @Override
    public void onHurt(DamageEvent event) {
        if (run == null || !run.isImmune()) return;
        event.setCancelled(true);
        owner.getWorld().spawnParticle(Particle.DUST, owner.getLocation().add(0, owner.getHeight() * 0.6, 0), 8, 0.4, 0.5, 0.4, 0, CROWN);
        owner.getWorld().playSound(owner.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 0.8f);
    }

    @Override
    public void onDeath(EntityDeathEvent event) {
        onRemoved();
    }

    @Override
    public void onRemoved() {
        cancel();
        hazards.addAll(pending);
        pending.clear();
        hazards.forEach(Hazard::remove);
        hazards.clear();
        bar.hide();
    }
}
