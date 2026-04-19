package com.roguesmp.player.ability.impl.passive;

import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.trigger.AbilityResponse;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Sharpshooter extends Ability {
    public static final String ID = "sharpshooter";

    // Cached Attributes
    private final double passiveDmg;
    private final double stackDmg;
    private final int decayTicks;
    private static final int MAX_STACKS = 4;

    // State
    private int stacks = 0;
    private int ticksUntilDecay = 0;

    public static final AbilityInfo<Sharpshooter> INFO = new AbilityInfo<>(
            ID,
            Sharpshooter.class,
            Sharpshooter::new
    ).registerAction("execute", (ability) -> AbilityResponse.continueChain());

    public Sharpshooter(SmpPlayer player, int level) {
        super(player, level);
        // Cache attributes from JSON for high-frequency access
        this.passiveDmg = getAbilityInfo().getAttributeForLevel("passive_dmg", level);
        this.stackDmg = getAbilityInfo().getAttributeForLevel("stack_dmg", level);
        this.decayTicks = (int) getAbilityInfo().getAttributeForLevel("cooldown_decay", level);
    }

    @Override
    public void onDamageEntity(DamageEvent event) {
        // Only apply to projectile damage
        if (event.getDamageType() == DamageType.PROJECTILE) {

            // 1. Calculate and Apply Multiplier using cached values
            double multiplier = passiveDmg + (stacks * stackDmg);
            event.addDamageModifier(multiplier, DamageOperation.INCREASE_BASE);

            // 2. Add/Refresh stacks
            addStack();
        }
    }

    private void addStack() {
        this.ticksUntilDecay = decayTicks;

        if (stacks < MAX_STACKS) {
            stacks++;
            updateActionBar();
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
            updateActionBar();
        }
    }

    private void updateActionBar() {
        Player p = smpPlayer.getBukkitPlayer();

        // Clear action bar if stacks are gone
        if (stacks <= 0) {
            p.sendActionBar(Component.empty());
            return;
        }

        TextColor color = stacks >= MAX_STACKS ? NamedTextColor.RED : NamedTextColor.GOLD;
        p.sendActionBar(
                Component.text("Sharpshooter Stacks: ", NamedTextColor.GRAY)
                        .append(Component.text(stacks, color, TextDecoration.BOLD))
                        .append(Component.text("/" + MAX_STACKS, NamedTextColor.DARK_GRAY))
        );
    }

    @Override public @NotNull AbilityInfo<?> getAbilityInfo() { return INFO; }
}