package com.roguesmp.player.mechanic;

import com.roguesmp.event.ArrowConsumeEvent;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class EnchantMechanic implements PlayerMechanic {
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
    public void onHurtFatal(DamageEvent event, SmpPlayer player) {
        player.getActiveEnchants().forEach((enchants, integer) ->
                enchants.getEnchant().onHurtFatal(event, integer, player)
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
}
