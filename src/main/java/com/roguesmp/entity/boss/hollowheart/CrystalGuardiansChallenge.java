package com.roguesmp.entity.boss.hollowheart;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;

public class CrystalGuardiansChallenge implements CrystalChallenge {

    private static final double HEAL_PERCENT_PER_GUARDIAN = 0.03;

    private final CrystalContext context;
    private final GuardianPack guardians;
    private int spawned;
    private long endsAt;
    private int timerTicks;

    public CrystalGuardiansChallenge(CrystalContext context) {
        this.context = context;
        this.guardians = new GuardianPack(context.crystal(), context.arena());
    }

    @Override
    public void start(long now) {
        timerTicks = context.timer(context.phase().guardianTimerTicks());
        endsAt = now + timerTicks;
        guardians.summon(context.phase().guardianCount());
        spawned = guardians.alive().size();
        context.crystal().getWorld().playSound(context.crystal().getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 2f, 0.8f);
    }

    @Override
    public Outcome tick(long now, int interval) {
        if (guardians.alive().isEmpty()) return Outcome.SUCCESS;
        if (now >= endsAt) return Outcome.FAILURE;
        guardians.tick(now, context.healMultiplier());
        return Outcome.RUNNING;
    }

    @Override
    public BarState status(long now) {
        int remaining = guardians.alive().size();
        return BarState.countdown(Component.text(remaining + "/" + spawned, NamedTextColor.GRAY),
                endsAt - now, timerTicks, BossBar.Color.BLUE);
    }

    @Override
    public void punish() {
        Absorption.absorb(context.crystal(), guardians.alive(), HEAL_PERCENT_PER_GUARDIAN, context.healMultiplier());
    }

    @Override
    public void cleanup() {
        guardians.removeAll();
    }
}
