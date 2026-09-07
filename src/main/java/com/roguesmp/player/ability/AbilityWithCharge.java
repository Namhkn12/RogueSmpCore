package com.roguesmp.player.ability;

import com.roguesmp.player.SmpPlayer;

public abstract class AbilityWithCharge extends Ability {

    protected int maxCharges;
    protected int charges;

    protected int rechargeTicks;

    public AbilityWithCharge(SmpPlayer smpPlayer, int level) {
        super(smpPlayer, level);
    }

    protected void setMaxCharges(int maxCharges, int rechargeTicks) {
        this.maxCharges = maxCharges;
        this.charges = maxCharges;
        this.rechargeTicks = rechargeTicks;
    }

    /** Spends one charge; only (re)starts the recharge timer if it isn't already counting down. */
    @Override
    public void setCooldownTick(int cooldownTick) {
        if (charges > 0) charges--;
        if (this.cooldownTick <= 0) this.cooldownTick = cooldownTick;
    }

    @Override
    public boolean tickCooldown(int reduction) {
        if (cooldownTick <= 0) return false;
        cooldownTick -= reduction;
        if (cooldownTick <= 0) {
            cooldownTick = 0;
            boolean wasDepleted = charges <= 0;
            charges = Math.min(maxCharges, charges + 1);
            if (charges < maxCharges) cooldownTick = rechargeTicks;
            return wasDepleted; // "JUST usable again" only when we went from 0 -> 1
        }
        return false;
    }


    @Override
    public boolean isOnCooldown() {
        return charges <= 0;
    }

    public int getCharges() {
        return charges;
    }

    public void setCharges(int charges) {
        this.charges = charges;
    }

    public int getMaxCharges() {
        return maxCharges;
    }
}
