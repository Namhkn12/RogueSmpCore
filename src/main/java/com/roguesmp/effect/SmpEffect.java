package com.roguesmp.effect;

import com.google.gson.annotations.SerializedName;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class SmpEffect implements Comparable<SmpEffect>, DisplayableEffect {

    protected int duration;
    private final String effectID;
    private final DeathBehavior deathBehavior;

    private boolean display = true;
    private boolean displayTime = true;

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
     * Effect name display, return null for no display
     * @return Component
     */
    @Override
    public abstract @Nullable Component getDisplay();

    @Override
    public int getDisplayPriority() {
        if (!displayTime) {
            return -1;
        }
        return duration;
    }

    /**
     * Display effect with remaining time generally used in tab list, return null to not display
     * @return Component
     */
    @Override
    public @Nullable Component getDisplayWithTime() {
        if (display) {
            Component displayWithoutTime = getDisplay();
            if (displayWithoutTime != null) {
                Component display = displayWithoutTime;
                if (displayTime) {
                    display = display.append(Component.text(" " + Utils.intToMinuteAndSeconds(duration / 20), NamedTextColor.GRAY));
                }
                return display;
            }
        }
        return null;
    }

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

    public void setDisplay(boolean display) {
        this.display = display;
    }

    public void setDisplayTime(boolean displayTime) {
        this.displayTime = displayTime;
    }

    public boolean isDisplay() {
        return display;
    }

    public boolean isDisplayTime() {
        return displayTime;
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

    public void onTick(Entity entity, boolean oneHz, boolean twoHz) {

    }

    public void onGainEffect(Entity entity) {

    }

    public void onLoseEffect(Entity entity) {

    }

    public void onDeath(EntityDeathEvent event) {

    }

    public void onDamage(DamageEvent event) {

    }
}
