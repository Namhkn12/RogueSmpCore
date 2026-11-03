package com.roguesmp.entity.boss.goblinking;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ActionStep extends ComboStep {

    @FunctionalInterface
    public interface Hook {
        void run(KingContext ctx, ActionStep step, int elapsed);
    }

    private static final Hook NONE = (ctx, step, elapsed) -> {
    };

    private final int linger;
    private Hook start = NONE;
    private Hook windupTick = NONE;
    private Hook fire = NONE;
    private @Nullable Player subject;

    public ActionStep(String title, StepKind kind, int windup, int linger) {
        super(title, kind, windup);
        this.linger = linger;
    }

    public ActionStep(String title, StepKind kind, int windup, int linger, Consumer<KingContext> action) {
        this(title, kind, windup, linger);
        fire = (ctx, step, elapsed) -> action.accept(ctx);
    }

    public ActionStep onStart(Hook hook) {
        start = hook;
        return this;
    }

    public ActionStep onWindup(Hook hook) {
        windupTick = hook;
        return this;
    }

    public ActionStep onFire(Hook hook) {
        fire = hook;
        return this;
    }

    public @Nullable Player subject() {
        return subject != null && subject.isValid() ? subject : null;
    }

    public void subject(@Nullable Player subject) {
        this.subject = subject;
    }

    @Override
    public void onWindupStart(KingContext ctx) {
        start.run(ctx, this, 0);
    }

    @Override
    public void onWindupTick(KingContext ctx, int elapsed) {
        windupTick.run(ctx, this, elapsed);
    }

    @Override
    public void onExecute(KingContext ctx) {
        fire.run(ctx, this, 0);
    }

    @Override
    public boolean tickActive(KingContext ctx, int elapsed) {
        return elapsed >= linger;
    }
}
