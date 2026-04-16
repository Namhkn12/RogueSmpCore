package com.roguesmp.player.ability.trigger;

public enum InputSignal {
    CONTINUE,        // I didn't use this; let the next ability try.
    CONSUME,         // I used this; stop the chain.
    CAPTURE,         // I am entering a state (Priming/Charging/Combo); lock inputs to me.
    RELEASE,          // I am exiting my state; unlock inputs.
    DENY            // Failed requirements, stop here and play 'fail' sound/visuals
}
