package com.roguesmp.effect.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.constant.DamageType;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * A stacking damage-over-time debuff. Each application (see {@link #addStack()}) adds one stack
 * (capped at {@link #MAX_STACKS}) and refreshes the duration back to {@link #DURATION_TICKS} -
 * re-applying never creates a second instance, it always mutates the one already active for its
 * source, same as {@code Poisoning}'s enchant hook expects.
 */
public class PotentPoisonEffect extends SmpEffect {

    public static final String ID = "potent_poison";
    public static final int MAX_STACKS = 5;
    public static final int DURATION_TICKS = 40; // 2 seconds

    public static final Codec<PotentPoisonEffect> CODEC = Codec.composite(
            SmpEffect.BASE_CODEC.forGetter(SmpEffect::getBaseProperties),
            Codec.DOUBLE.fieldOf("damage_per_stack").forGetter(PotentPoisonEffect::getDamagePerStack),
            Codec.INT.optionalFieldOf("stacks", 1).forGetter(PotentPoisonEffect::getStacks),
            PotentPoisonEffect::new
    );

    private final double damagePerStack;
    private Entity applier;
    private int stacks;

    public PotentPoisonEffect(int duration, double damagePerStack, int stacks) {
        super(ID, duration);
        this.damagePerStack = damagePerStack;
        this.stacks = clampStacks(stacks);
    }

    public PotentPoisonEffect(BaseProperties base, double damagePerStack, int stacks) {
        super(ID, base);
        this.damagePerStack = damagePerStack;
        this.stacks = clampStacks(stacks);
    }

    private static int clampStacks(int stacks) {
        return Math.max(1, Math.min(MAX_STACKS, stacks));
    }

    /**
     * Adds one stack (capped at {@link #MAX_STACKS}) and resets the duration back to full - this
     * is what a re-application should call instead of adding a brand new effect instance.
     */
    public void addStack() {
        stacks = Math.min(MAX_STACKS, stacks + 1);
        setDuration(DURATION_TICKS);
    }

    @Override
    public double getMagnitude() {
        return damagePerStack * stacks;
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public @Nullable Component getDisplayComponent() {
        return Component.text("Kịch độc x" + stacks + " (-" + Utils.formatDecimal(getMagnitude()) + "/s)", NamedTextColor.DARK_GREEN);
    }

    @Override
    public void onTick(Entity entity, boolean oneHz, boolean twoHz) {
        if (!(entity instanceof LivingEntity livingEntity) || !livingEntity.isValid()) return;
        if (oneHz) {
            DamageEvent.Metadata metadata = new DamageEvent.Metadata(DamageType.AILMENT);
            metadata.setIgnoreIframe(true);
            metadata.setDoKnockback(false);
            DamageUtils.damage(livingEntity, applier, getMagnitude(), metadata);
        }
    }

    public double getDamagePerStack() {
        return damagePerStack;
    }

    public int getStacks() {
        return stacks;
    }

    public void setApplier(Entity applier) {
        this.applier = applier;
    }

    public static CommandAPICommand registerCommand() {
        return new CommandAPICommand("potentpoison")
                .withArguments(new IntegerArgument("duration"), new DoubleArgument("damagePerStack"), new IntegerArgument("stacks"), new StringArgument("source"))
                .executesPlayer((player, args) -> {
                    int duration = (Integer) args.get("duration");
                    double damagePerStack = (Double) args.get("damagePerStack");
                    int stacks = (Integer) args.get("stacks");
                    String source = (String) args.get("source");

                    EffectManager.getInstance().addEffect(player, source, new PotentPoisonEffect(duration, damagePerStack, stacks));
                });
    }
}
