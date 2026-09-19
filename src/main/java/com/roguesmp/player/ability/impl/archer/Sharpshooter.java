package com.roguesmp.player.ability.impl.archer;

import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.trigger.AbilityResponse;
import org.jetbrains.annotations.NotNull;

public class Sharpshooter extends Ability {
    public static final String ID = "sharpshooter";

    private final double passiveDmg;
    private final double stackDmg;
    private final int decayTicks;
    private static final int MAX_STACKS = 4;

    private int stacks = 0;
    private int ticksUntilDecay = 0;

    public static final AbilityInfo<Sharpshooter> INFO = new AbilityInfo<>(
            ID,
            Sharpshooter.class,
            Sharpshooter::new
    ).registerAction("execute", (ability) -> AbilityResponse.continueChain());

    public Sharpshooter(SmpPlayer player, int level) {
        super(player, level);
        this.passiveDmg = getAbilityInfo().getAttributeForLevel("passive_dmg", level);
        this.stackDmg = getAbilityInfo().getAttributeForLevel("stack_dmg", level);
        this.decayTicks = (int) getAbilityInfo().getAttributeForLevel("cooldown_decay", level);
    }

    @Override
    public void onDamageEntity(DamageEvent event) {
        if (event.getDamageType() == DamageType.PROJECTILE) {

            double multiplier = passiveDmg + (stacks * stackDmg);
            event.addDamageModifier(multiplier, DamageOperation.INCREASE_BASE);

            addStack();
        }
    }

    private void addStack() {
        this.ticksUntilDecay = decayTicks;

        if (stacks < MAX_STACKS) {
            stacks++;
        }
    }

    @Override
    public void tick(int periodIncrement) {
        if (stacks <= 0) return;

        ticksUntilDecay -= periodIncrement;

        if (ticksUntilDecay <= 0) {
            stacks--;
            // If stacks remain, reset timer for the next decay step
            if (stacks > 0) {
                ticksUntilDecay = decayTicks;
            }
        }
    }

    @Override public @NotNull AbilityInfo<?> getAbilityInfo() { return INFO; }
}