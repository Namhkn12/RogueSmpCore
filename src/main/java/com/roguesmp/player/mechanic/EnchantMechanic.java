package com.roguesmp.player.mechanic;

import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.event.AbilityCastEvent;
import com.roguesmp.event.ArrowConsumeEvent;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.event.DurabilityChangedEvent;
import com.roguesmp.item.SmpItem;
import com.roguesmp.player.SmpPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EnchantMechanic implements PlayerMechanic {

    // Owned per-player (one EnchantMechanic per SmpPlayer) - the level each enchant was resolved
    // at as of the last onEquipmentChange call, so a fresh diff against player.getActiveEnchants()
    // tells us exactly which enchants actually changed level this time.
    private final Map<Enchants, Integer> lastKnownLevels = new EnumMap<>(Enchants.class);

    @Override
    public int getPriority() {
        return 90;
    }

    @Override
    public void tick(int periodIncrement, SmpPlayer player) {
        player.getActiveEnchants().forEach((enchants, integer) ->
                enchants.getEnchant().tick(player, periodIncrement, integer)
        );
    }

    @Override
    public void onDamageEntity(DamageEvent event, SmpPlayer player) {
        if (event.isCancelled() || !(event.getDamager() instanceof Player)) return;
        player.getActiveEnchants().forEach((enchants, integer) ->
                enchants.getEnchant().onDamageEntity(event, integer, player)
        );
    }

    @Override
    public void onDamageEntityFinal(DamageEvent event, SmpPlayer player) {
        if (event.isCancelled() || !(event.getDamager() instanceof Player)) return;
        player.getActiveEnchants().forEach((enchants, integer) ->
                enchants.getEnchant().onDamageEntityFinal(event, integer, player)
        );
    }

    @Override
    public void onKillEntity(EntityDeathEvent event, SmpPlayer player) {
        player.getActiveEnchants().forEach((enchants, integer) ->
                enchants.getEnchant().onKillEntity(event, integer, player)
        );
    }

    @Override
    public void onHurt(DamageEvent event, SmpPlayer player) {
        if (event.isCancelled()) return;
        player.getActiveEnchants().forEach((enchants, integer) ->
                enchants.getEnchant().onHurt(event, integer, player)
        );
    }

    @Override
    public void onHurtFinal(DamageEvent event, SmpPlayer player) {
        if (event.isCancelled()) return;
        player.getActiveEnchants().forEach((enchants, integer) ->
                enchants.getEnchant().onHurtFinal(event, integer, player)
        );
    }

    @Override
    public void onHurtFatal(DamageEvent event, SmpPlayer player) {
        player.getActiveEnchants().forEach((enchants, integer) ->
                enchants.getEnchant().onHurtFatal(event, integer, player)
        );
    }

    @Override
    public void onAbilityCast(AbilityCastEvent event, SmpPlayer player) {
        player.getActiveEnchants().forEach((enchants, integer) ->
                enchants.getEnchant().onAbilityCast(event, integer, player)
        );
    }

    @Override
    public void onConsume(PlayerItemConsumeEvent event, SmpPlayer player) {
        if (event.isCancelled()) return;
        player.getActiveEnchants().forEach((enchants, integer) ->
                enchants.getEnchant().onConsume(event, integer, player)
        );
    }

    @Override
    public void onExpChange(PlayerExpChangeEvent event, SmpPlayer player) {
        player.getActiveEnchants().forEach((enchants, integer) ->
                enchants.getEnchant().onExpChange(event, integer, player)
        );
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event, SmpPlayer player) {
        if (event.isCancelled()) return;
        player.getActiveEnchants().forEach((enchants, integer) ->
                enchants.getEnchant().onBlockBreak(event, integer, player)
        );
    }

    @Override
    public void onCombust(EntityCombustEvent event, SmpPlayer player) {
        if (event.isCancelled()) return;
        player.getActiveEnchants().forEach((enchants, integer) ->
                enchants.getEnchant().onCombust(event, integer, player)
        );
    }

    @Override
    public void onCombustEntity(EntityCombustByEntityEvent event, SmpPlayer player) {
        if (event.isCancelled() || !(event.getCombuster() instanceof Player)) return;
        player.getActiveEnchants().forEach((enchants, integer) ->
                enchants.getEnchant().onCombustEntity(event, integer, player)
        );
    }

    @Override
    public void onConsumeArrow(ArrowConsumeEvent event, SmpPlayer player) {
        if (event.isCancelled()) return;
        player.getActiveEnchants().forEach((enchants, integer) ->
                enchants.getEnchant().onConsumeArrow(event, integer, player)
        );
    }

    @Override
    public void onDurabilityChange(DurabilityChangedEvent event, SmpPlayer player) {
        player.getActiveEnchants().forEach((enchants, integer) ->
                enchants.getEnchant().onDurabilityChange(event, integer, player)
        );
    }

    @Override
    public void onEquipmentChange(EquipSlot slot, @Nullable SmpItem newItem, SmpPlayer player) {
        Map<Enchants, Integer> current = player.getActiveEnchants();

        Set<Enchants> touched = new HashSet<>(lastKnownLevels.keySet());
        touched.addAll(current.keySet());
        for (Enchants enchant : touched) {
            int before = lastKnownLevels.getOrDefault(enchant, 0);
            int after = current.getOrDefault(enchant, 0);
            if (before != after) {
                enchant.getEnchant().onEquipmentChange(player, after);
            }
        }

        lastKnownLevels.clear();
        lastKnownLevels.putAll(current);
    }
}
