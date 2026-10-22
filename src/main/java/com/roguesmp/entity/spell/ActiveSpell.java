package com.roguesmp.entity.spell;

public abstract class ActiveSpell extends Spell {

    private static final int DEFAULT_RECOVERY_TICKS = 20;

    private final int cooldown;
    private final int windup;
    private final int recovery;

    protected ActiveSpell(int cooldown) {
        this(cooldown, 0, DEFAULT_RECOVERY_TICKS);
    }

    protected ActiveSpell(int cooldown, int windup, int recovery) {
        this.cooldown = cooldown;
        this.windup = windup;
        this.recovery = recovery;
    }

    public final int cooldown() {
        return cooldown;
    }

    public final int windup() {
        return windup;
    }

    public final int recovery() {
        return recovery;
    }

    public int weight() {
        return 1;
    }

    public boolean canCast(SpellContext context) {
        return true;
    }

    public void onWindupStart(SpellContext context) {

    }

    public void onWindupTick(SpellContext context, int elapsed) {

    }

    public abstract void onCast(SpellContext context);

    public void onInterrupted(SpellContext context) {

    }
}
