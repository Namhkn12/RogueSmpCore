package com.roguesmp.player.ability.trigger;

public record AbilityResponse(InputSignal signal, int timeoutTicks) {
    /**
     * I didn't use this; let the next ability try.
     */
    public static AbilityResponse continueChain() { return new AbilityResponse(InputSignal.CONTINUE, 0); }

    /**
     * I used this; stop the chain.
     */
    public static AbilityResponse consume() { return new AbilityResponse(InputSignal.CONSUME, 0); }

    /**
     * I am entering a state (Priming/Charging/Combo/...); lock inputs to me.
     * @param ticks ticks to lock input for
     */
    public static AbilityResponse capture(int ticks) { return new AbilityResponse(InputSignal.CAPTURE, ticks); }

    /**
     * I am exiting my state; unlock inputs.
     */
    public static AbilityResponse release() { return new AbilityResponse(InputSignal.RELEASE, 0); }

    /**
     * I was the intended ability, but requirements (cooldown/mana/...) failed.
     * Stop the chain so no other ability triggers.
     */
    public static AbilityResponse deny() {
        return new AbilityResponse(InputSignal.DENY, 0);
    }
}
