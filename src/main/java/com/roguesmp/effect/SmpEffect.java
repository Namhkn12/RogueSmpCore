package com.roguesmp.effect;

import com.google.gson.annotations.SerializedName;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;

public abstract class SmpEffect implements Comparable<SmpEffect> {

    protected int duration;
    private final String effectID;
    private final DeathBehavior deathBehavior;

    public SmpEffect(int duration, String effectID, DeathBehavior deathBehavior) {
        this.duration = duration;
        this.effectID = effectID;
        this.deathBehavior = deathBehavior;
    }

    public SmpEffect(int duration, String effectID) {
        this.duration = duration;
        this.effectID = effectID;
        this.deathBehavior = DeathBehavior.HALVES_ON_DEATH;
    }

    /**
     * Used to compare how this effect is stronger (more potent) than another effect of the same type
     * @return The magnitude of this effect instance
     */
    public abstract double getMagnitude();

    /**
     * Whether the effect stay (written to files) after player logged out
     * @return Whether the effect stay after player logged out
     */
    public abstract boolean isPersistent();

    /**
     * Ticks the effect, called regularly
     *
     * @param ticks Ticks passed since the last time this method was called to check duration expiry
     * @return Returns true if effect has expired and should be removed by the EffectManager
     */
    public boolean tickDuration(int ticks) {
        duration -= ticks;
        return duration <= 0;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getEffectID() {
        return effectID;
    }

    public DeathBehavior getDeathBehavior() {
        return deathBehavior;
    }

    @Override
    public int compareTo(@NotNull SmpEffect o) {
        return Double.compare(this.getMagnitude(), o.getMagnitude());
    }

    public enum DeathBehavior {
        @SerializedName("halves_on_death")
        HALVES_ON_DEATH,
        @SerializedName("remove_on_death")
        REMOVE_ON_DEATH,
        @SerializedName("keep_on_death")
        KEEP_ON_DEATH,
    }

    public void onTick(Entity entity, boolean oneHz, boolean twoHz, boolean fourHz) {

    }

    public void onGainEffect(Entity entity) {

    }

    public void onLoseEffect(Entity entity) {

    }

    public void onDeath(EntityDeathEvent event) {

    }
}
