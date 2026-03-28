package com.roguesmp.player.ability.impl.passive;

import com.roguesmp.constant.AbilityTrigger;
import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Sharpshooter extends Ability {
    public static final String ID = "sharpshooter";

    // Level Scaling
    private static final List<Double> PASSIVE_DMG = List.of(0.10, 0.15, 0.20, 0.25, 0.40);
    private static final List<Double> STACK_DMG = List.of(0.03, 0.04, 0.05, 0.06, 0.10);
    private static final List<Integer> DECAY_TICKS = List.of(80, 80, 80, 80, 120); // 4s, Lvl 5 is 6s
    private static final int MAX_STACKS = 4;

    private int stacks = 0;
    private int ticksUntilDecay = 0;

    public static final AbilityInfo<Sharpshooter> INFO = new AbilityInfo.Builder<Sharpshooter>()
            .id(ID)
            .displayText(Component.text("Sharpshooter", NamedTextColor.GOLD, TextDecoration.BOLD))
            .descriptionProvider((p, l) -> List.of(
                    Utils.text("Sát thương tầm xa tăng ", NamedTextColor.GRAY).append(Utils.text(Utils.formatDecimal(PASSIVE_DMG.get(l-1) * 100) + "%", NamedTextColor.GREEN)),
                    Utils.text("Khi bắn trúng địch sẽ nhận một cộng dồn", NamedTextColor.GRAY),
                    Utils.text("Mỗi cộng dồn (Tối đa 4): ", NamedTextColor.GRAY)
                            .append(Utils.text("+" + Utils.formatDecimal(STACK_DMG.get(l-1) * 100) + "% sát thương tầm xa", NamedTextColor.YELLOW)),
                    Utils.text("Cộng dồn sẽ mất sau vài giây nếu không đánh trúng.", NamedTextColor.DARK_GRAY)
            ))
            .displayIcon(Material.TARGET)
            .factory(Sharpshooter::new)
            .trigger(AbilityTrigger.PASSIVE)
            .build();

    public Sharpshooter(SmpPlayer player, int level) {
        super(player, level);
    }

    @Override
    public void onDamageEntity(DamageEvent event) {
        if (event.getDamageType() == DamageType.PROJECTILE) {

            // 1. Apply Multiplier
            double multiplier = PASSIVE_DMG.get(level - 1) + (stacks * STACK_DMG.get(level - 1));
            event.addDamageModifier(multiplier, DamageOperation.INCREASE_BASE);

            // 2. Stack Logic: Check if it's a "Critical" or valid projectile hit
            addStack();

        }
    }

    private void addStack() {
        this.ticksUntilDecay = DECAY_TICKS.get(level - 1);

        if (stacks < MAX_STACKS) {
            stacks++;
            updateActionBar();
        }
    }

    @Override
    public void tick(int periodIncrement) {
        if (stacks <= 0) {
            stacks = 0;
            return;
        }

        ticksUntilDecay -= periodIncrement; // Check every 10 ticks (0.5s)

        if (ticksUntilDecay <= 0) {
            stacks--;
            ticksUntilDecay = DECAY_TICKS.get(level - 1);
            updateActionBar();
        }
    }

    private void updateActionBar() {
        if (stacks <= 0) return;

        TextColor color = stacks >= MAX_STACKS ? NamedTextColor.RED : NamedTextColor.GOLD;
        smpPlayer.getBukkitPlayer().sendActionBar(
                Component.text("Sharpshooter Stacks: ", NamedTextColor.GRAY)
                        .append(Component.text(stacks, color, TextDecoration.BOLD))
                        .append(Component.text("/" + MAX_STACKS, NamedTextColor.DARK_GRAY))
        );
    }

    @Override public void cast() {} // Passive

    @Override public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() { return INFO; }
}