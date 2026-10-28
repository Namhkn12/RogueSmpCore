package com.roguesmp.effect.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.constant.DamageOperation;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CurseOfDeathEffect extends SmpEffect {

    public static final String ID = "curse_of_death";
    public static final int MAX_STACKS = 3;
    public static final int DURATION = 20 * 60 * 10;
    public static final double DAMAGE_TAKEN_PER_STACK = 0.2;
    public static final double DAMAGE_DEALT_PER_STACK = 0.2;

    public static final Codec<CurseOfDeathEffect> CODEC = Codec.composite(
            SmpEffect.BASE_CODEC.forGetter(SmpEffect::getBaseProperties),
            Codec.INT.fieldOf("stacks").forGetter(CurseOfDeathEffect::getStacks),
            CurseOfDeathEffect::new
    );

    private final int stacks;

    public CurseOfDeathEffect(BaseProperties base, int stacks) {
        super(ID, base);
        this.stacks = Math.clamp(stacks, 1, MAX_STACKS);
    }

    public CurseOfDeathEffect(int stacks) {
        this(new BaseProperties(DURATION, DeathBehavior.KEEP_ON_DEATH, DisplayMode.WITH_TIME), stacks);
    }

    public static int addStack(Entity entity) {
        int current = EffectManager.getInstance().getActiveEffects(entity).get(ID) instanceof CurseOfDeathEffect curse ? curse.stacks : 0;
        int stacks = Math.min(current + 1, MAX_STACKS);
        EffectManager.getInstance().addEffect(entity, ID, new CurseOfDeathEffect(stacks));
        return stacks;
    }

    public static boolean clear(Entity entity) {
        return EffectManager.getInstance().clearEffects(entity, ID) != null;
    }

    public int getStacks() {
        return stacks;
    }

    @Override
    public double getMagnitude() {
        return stacks;
    }

    @Override
    public StackPolicy getStackPolicy() {
        return StackPolicy.REPLACE;
    }

    @Override
    protected boolean isPersistentByDefault() {
        return true;
    }

    @Override
    public @Nullable Component getDisplayComponent() {
        return Utils.text("Lời Nguyền Tử Thần " + stacks + "/" + MAX_STACKS, NamedTextColor.DARK_RED);
    }

    @Override
    public List<Component> getDisplayLines() {
        Component header = getDisplayWithTime();
        if (header == null) return List.of();
        return List.of(
                header,
                indent(Utils.text("+" + Utils.formatDecimal(stacks * DAMAGE_TAKEN_PER_STACK * 100) + "% sát thương phải nhận", NamedTextColor.RED)),
                indent(Utils.text("-" + Utils.formatDecimal(stacks * DAMAGE_DEALT_PER_STACK * 100) + "% sát thương gây ra", NamedTextColor.RED))
        );
    }

    private static Component indent(Component line) {
        return Component.space().append(line).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    @Override
    public void onHurt(DamageEvent event) {
        event.addDamageModifier(1 + stacks * DAMAGE_TAKEN_PER_STACK, DamageOperation.MORE_FINAL);
    }

    @Override
    public void onDamageEntity(DamageEvent event) {
        event.addDamageModifier(Math.max(0, 1 - stacks * DAMAGE_DEALT_PER_STACK), DamageOperation.MORE_FINAL);
    }
}
