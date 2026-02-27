package com.roguesmp.entity.spell;

import com.roguesmp.event.DamageEvent;
import com.roguesmp.event.SpellCastEvent;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class Spell implements Cloneable {
    protected final Set<BukkitRunnable> activeRunnables = new LinkedHashSet<>();
    /**
     * Used by some spells to indicate if they can be run now (true) or not (false)
     */
    public boolean canRun() {
        return true;
    }

    public abstract void run();

    /**
     * Cancels all currently running tasks (tasks in activeRunnables)
     * <p>
     * To use this functionality, user needs to add every BukkitRunnable created to activeRunnables,
     * and then remove them from activeRunnables when they are finished
     */
    public void cancel() {
        /*
         * Iterate over a copy of activeRunnables and cancel each task that isn't already cancelled.
         * Need to iterate over a copy because some runnables remove themselves from activeRunnables when cancelled
         */
        for (BukkitRunnable runnable : new ArrayList<>(activeRunnables)) {
            if (!runnable.isCancelled()) {
                runnable.cancel();
            }
        }
        activeRunnables.clear();
    }

    /**
     * How long entity must wait/cooldown in ticks between start (not end!) of this spell and start of next spell
     */
    public abstract int cooldownTicks();

    /**
     * How long entity takes in ticks to complete casting process of this spell
     */
    public int castTicks() {
        return 0;
    }

    public boolean onlyForceCasted() {
        return false;
    }

    /**
     * Whether this spell is currently active.
     * <p>
     * By default, this checks if there are active runnables, but may be overridden by spells to be more specific.
     */
    public boolean isRunning() {
        return activeRunnables.stream().anyMatch(r -> !r.isCancelled());
    }

    /**
     * Checks if the spell should persist after a phase change
     *
     * @return True if spell should not cancel on phase change
     */
    public boolean persistOnPhaseChange() {
        return false;
    }

    public void onDamage(DamageEvent event) {

    }

    /*
     * Entity was hurt
     */
    public void onHurt(DamageEvent event) {

    }

    public void onDeath(EntityDeathEvent event) {

    }

    /*
     * Entity shot a projectile
     */
    public void onProjectileLaunch(ProjectileLaunchEvent event) {

    }

    /*
     * Entity-shot projectile hit something
     */
    public void onProjectileHit(ProjectileHitEvent event) {

    }

    public void onCastSpell(SpellCastEvent event) {

    }

    public void nearbyPlayerDeath(PlayerDeathEvent event) {

    }

    @FunctionalInterface
    public interface GetSpellTargets<V extends Entity> {
        List<? extends V> getTargets();
    }

    @FunctionalInterface
    public interface SpellParamReader {
        @NotNull Spell fromParams(Map<String, Object> params);
    }

}
