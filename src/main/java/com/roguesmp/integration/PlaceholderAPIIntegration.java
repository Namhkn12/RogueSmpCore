package com.roguesmp.integration;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.effect.DisplayableEffect;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PlaceholderAPIIntegration extends PlaceholderExpansion {

    private final RogueSmpCore plugin;

    public PlaceholderAPIIntegration(RogueSmpCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "roguesmp";
    }

    @Override
    public @NotNull String getAuthor() {
        return "RogueSmpCore";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (params.startsWith("effect_")) {
            List<Component> effectDisplays = DisplayableEffect.getSortedEffectDisplays(player);
            if (params.startsWith("effect_more")) {
                int extra = effectDisplays.size() - 10;
                if (extra == 1) {
                    //Show 11th if there are exactly 11
                    return Utils.toString(effectDisplays.get(10));
                } else if (extra > 0) {
                    return Utils.toString(Component.text("... and " + extra + " more effects", NamedTextColor.GRAY));
                } else {
                    return "";
                }
            } else {
                try {
                    int index = Integer.parseInt(params.substring("effect_".length())) - 1;
                    if (index >= 0 && effectDisplays.size() > index) {
                        return Utils.toString(effectDisplays.get(index));
                    } else {
                        return "";
                    }
                } catch (NumberFormatException numberFormatException) {
                    RogueSmpCore.LOGGER.warn("Failed to find integer after 'effect_' on tab list");
                }
            }
            return null;
        }

        if (params.startsWith("player_")) {
            if (params.startsWith("player_level")) {
                SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player.getUniqueId());
                if (smpPlayer == null) return null;
                int level = smpPlayer.getPlayerData().getLevel();
                return "["+level+"] ";
            }
        }

//        if (params.startsWith("ability_")) {
//            if (params.startsWith("ability_cooldown_")) {
//                SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player.getUniqueId());
//                if (smpPlayer == null) return "";
//
//                String triggerName = params.substring("ability_cooldown_".length()).toUpperCase();
//                AbilityTrigger trigger;
//                try {
//                    trigger = AbilityTrigger.valueOf(triggerName);
//                } catch (IllegalArgumentException ex) {
//                    return "";
//                }
//
//                Ability ability = smpPlayer.getAbilityLoadout().getActiveAbilities().get(trigger);
//                if (ability == null) return "";
//
//                if (!ability.isOnCooldown()) {
//                    return Utils.toString(ability.getAbilityInfo().displayText().append(Component.text(": Ready", NamedTextColor.GREEN)));
//                }
//
//                double seconds = ability.getCooldownTick() / 20.0;
//                return Utils.toString(ability.getAbilityInfo().displayText().append(Component.text(": " + String.format("%.1fs", seconds), NamedTextColor.GREEN)));
//            }
//        }
        return null;
    }
}
